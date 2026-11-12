package com.likelionknu.dojang.basis.service;

import com.likelionknu.dojang.common.ai.LlmClient;
import com.likelionknu.dojang.basis.data.dto.response.BasisResponseDto;
import com.likelionknu.dojang.basis.data.dto.response.BasisResponseDto.QuoteDto;
import com.likelionknu.dojang.basis.exception.BasisException;
import com.likelionknu.dojang.common.response.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 카드 상세 "왜 해당되나요". 서비스 id로 원문(welfare_texts.json)을 찾고, AI가 사용자 값과 관련된 문장을 고른다.
 * 인용문은 원문 문장에 그대로 있는 것만 내보낸다. 원문은 scripts/fetch_welfare_texts.py로 받는다.
 */
@Slf4j
@Service
public class BasisService {
    private static final int MAX_QUOTES = 4;
    private static final int MAX_SUMMARY = 300;

    private static final String SYSTEM = """
            너는 장애인 복지 안내 서비스에서 복지 서비스 조건 원문을 사용자 상황과 연결하는 역할이다.
            번호가 붙은 원문 문장 중 지원 조건(나이, 장애 유형, 장애 정도, 소득, 수급 여부 등)을 말하는 문장을 최대 4개 고른다.
            - line: 고른 문장 번호
            - text: 그 문장에서 조건 부분만 한 글자도 바꾸지 않고 복사한다. 자르기 어려우면 문장 전체를 쓴다.
            - matches: 그 문장이 다루는 사용자 값의 key. 사용자 값 중에 없으면 null
            - summary: 사용자 값과 고른 조건이 어떻게 연결되는지 한국어 1~2문장. 원문에 없는 내용을 더하지 않는다.
              받을 수 있다고 단정하지 않는다. "~조건과 관련됩니다", "~를 확인해 보세요"처럼 쓴다.
              key 이름이나 영문을 쓰지 않는다. 예: "만 4세, 자폐성 장애로 나이와 장애 유형 조건과 관련됩니다."
            """;

    private static final Map<String, Object> SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "quotes", Map.of(
                            "type", "array",
                            "items", Map.of(
                                    "type", "object",
                                    "properties", Map.of(
                                            "line", Map.of("type", "integer"),
                                            "text", Map.of("type", "string"),
                                            "matches", Map.of("type", List.of("string", "null"))),
                                    "required", List.of("line", "text", "matches"))),
                    "summary", Map.of("type", "string")),
            "required", List.of("quotes", "summary"));

    private static final Map<String, Map<String, String>> LABELS = Map.of(
            "disability_type", linked(
                    "physical", "지체", "brain_lesion", "뇌병변", "visual", "시각", "hearing", "청각",
                    "speech", "언어", "intellectual", "지적", "autism", "자폐성", "mental", "정신",
                    "kidney", "신장", "heart", "심장", "respiratory", "호흡기", "liver", "간",
                    "facial", "안면", "ostomy", "장루요루", "epilepsy", "뇌전증"),
            "severity", linked("severe", "심한 장애", "mild", "심하지 않은 장애"),
            "status", linked("basic", "기초생활수급", "near_poor", "차상위", "neither", "둘 다 아님"),
            "income_band", linked("le120", "기준 중위소득 120% 이하", "le180", "기준 중위소득 180% 이하", "over180", "그보다 많음"),
            "tags", linked("daycare", "어린이집 다님", "has_car", "차 있음", "wheelchair", "휠체어 사용", "newborn", "출산"));

    private record Line(String text, String source) {
    }

    private final LlmClient llmClient;
    private final JsonMapper jsonMapper;
    private final LocalDate checkedAt;
    private final Map<String, List<Line>> linesByService = new HashMap<>();

    public BasisService(LlmClient llmClient, JsonMapper jsonMapper) throws IOException {
        this.llmClient = llmClient;
        this.jsonMapper = jsonMapper;
        JsonNode texts = readTree("data/welfare_texts.json");
        this.checkedAt = LocalDate.parse(texts.path("fetched_at").asString());

        // 규칙표 서비스 id -> 복지 id들 -> 원문 문장. 아동, 성인 규칙표에 같은 id가 있으면 합친다
        Map<String, Set<String>> welfareIds = new HashMap<>();
        for (String path : List.of("data/rules_child_v2.json", "data/rules_adult_v2.json")) {
            readTree(path).path("services").forEach(service -> {
                Set<String> ids = welfareIds.computeIfAbsent(service.path("id").asString(), key -> new LinkedHashSet<>());
                service.path("welfare_ids").forEach(id -> ids.add(id.asString()));
            });
        }
        welfareIds.forEach((serviceId, ids) -> {
            List<Line> lines = new ArrayList<>();
            for (String id : ids) {
                JsonNode text = texts.path("services").path(id);
                text.path("lines").forEach(line -> lines.add(new Line(line.path("text").asString(), text.path("source").asString())));
            }
            linesByService.put(serviceId, lines);
        });
    }

    public BasisResponseDto basis(String serviceId, Map<String, Object> facts) {
        List<Line> lines = linesByService.get(serviceId);
        if (lines == null) {
            throw new BasisException(ErrorCode.NOT_FOUND, "id: 없는 서비스입니다.");
        }
        // 주차표지, 7종 감면처럼 복지 API에 원문이 없는 서비스
        if (lines.isEmpty()) {
            return new BasisResponseDto(List.of(), null);
        }

        JsonNode result;
        try {
            result = llmClient.extractJson(SYSTEM, prompt(lines, facts), "basis", SCHEMA);
        } catch (RuntimeException e) {
            log.warn("카드 근거 AI 호출 실패: {}", e.getClass().getSimpleName());
            throw new BasisException(ErrorCode.AI_FAILED, ErrorCode.AI_FAILED.getMessage());
        }
        return toResponse(lines, facts, result);
    }

    private String prompt(List<Line> lines, Map<String, Object> facts) {
        // 요약에 영문 코드가 나오지 않게 한글 이름을 같이 넘긴다
        Map<String, Object> labeled = new LinkedHashMap<>();
        facts.forEach((key, value) -> {
            Map<String, String> labels = LABELS.getOrDefault(key, Map.of());
            labeled.put(key, value instanceof List<?> list
                    ? list.stream().map(item -> labels.getOrDefault(item, String.valueOf(item))).toList()
                    : labels.getOrDefault(value, String.valueOf(value)));
        });
        StringBuilder sb = new StringBuilder("사용자 값: ").append(jsonMapper.writeValueAsString(labeled)).append("\n\n원문 문장:\n");
        for (int i = 0; i < lines.size(); i++) {
            sb.append('[').append(i).append("] ").append(lines.get(i).text()).append('\n');
        }
        return sb.toString();
    }

    private BasisResponseDto toResponse(List<Line> lines, Map<String, Object> facts, JsonNode result) {
        List<QuoteDto> quotes = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (JsonNode quote : result.path("quotes")) {
            int index = quote.path("line").asInt(-1);
            if (index < 0 || index >= lines.size() || quotes.size() >= MAX_QUOTES) {
                continue;
            }
            Line line = lines.get(index);
            // AI가 고친 문장은 버리고 원문 문장 전체를 쓴다
            String text = quote.path("text").asString("").strip();
            if (text.isEmpty() || !line.text().contains(text)) {
                text = line.text();
            }
            String matches = quote.path("matches").asString(null);
            if (matches != null && !facts.containsKey(matches)) {
                matches = null;
            }
            if (seen.add(text)) {
                quotes.add(new QuoteDto(text, matches, line.source(), checkedAt));
            }
        }

        String summary = result.path("summary").asString("").strip();
        if (quotes.isEmpty() || summary.isEmpty()) {
            summary = null;
        } else if (summary.length() > MAX_SUMMARY) {
            summary = summary.substring(0, MAX_SUMMARY);
        }
        return new BasisResponseDto(quotes, summary);
    }

    private JsonNode readTree(String path) throws IOException {
        return jsonMapper.readTree(new ClassPathResource(path).getInputStream());
    }

    private static Map<String, String> linked(String... pairs) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return map;
    }
}
