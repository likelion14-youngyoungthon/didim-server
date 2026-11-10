package com.likelionknu.dojang.analyze.service;

import com.likelionknu.dojang.analyze.data.dto.request.AnalyzeRequestDto;
import com.likelionknu.dojang.analyze.data.dto.response.AnalyzeResponseDto;
import com.likelionknu.dojang.analyze.data.dto.response.AnalyzeResponseDto.FactDto;
import com.likelionknu.dojang.analyze.data.dto.response.AnalyzeResponseDto.OptionDto;
import com.likelionknu.dojang.analyze.data.dto.response.AnalyzeResponseDto.QuestionDto;
import com.likelionknu.dojang.analyze.exception.AnalyzeException;
import com.likelionknu.dojang.common.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 사용자 글에서 판정(/judge)에 쓸 사실만 뽑는다.
 * AI 결과는 그대로 믿지 않는다. 허용값 밖의 값과 원문에 없는 근거는 버리고 "모름"으로 둔다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyzeService {
    private static final String UNKNOWN = "unknown";
    private static final String SCHEMA_NAME = "facts";

    // /judge 요청 필드 허용값. 라벨은 되묻기 선택지에 쓴다
    public static final Map<String, Map<String, String>> ALLOWED = Map.of(
            "disability_type", linked(
                    "physical", "지체", "brain_lesion", "뇌병변", "visual", "시각", "hearing", "청각",
                    "speech", "언어", "intellectual", "지적", "autism", "자폐성", "mental", "정신",
                    "kidney", "신장", "heart", "심장", "respiratory", "호흡기", "liver", "간",
                    "facial", "안면", "ostomy", "장루요루", "epilepsy", "뇌전증"),
            "severity", linked("severe", "심한 장애", "mild", "심하지 않은 장애"),
            "status", linked("basic", "기초생활수급", "near_poor", "차상위", "neither", "둘 다 아님"),
            "income_band", linked("le120", "기준 중위소득 120% 이하", "le180", "기준 중위소득 180% 이하", "over180", "그보다 많음"),
            "tags", linked("daycare", "어린이집 다님", "has_car", "차 있음", "wheelchair", "휠체어 사용", "newborn", "출산"));

    private static final List<String> FACT_KEYS = List.of("age", "disability_type", "severity", "household", "status", "income_band", "tags");
    // 판정에 꼭 필요한 값. 이 순서대로 되묻는다
    private static final List<String> REQUIRED = List.of("age", "disability_type", "severity", "status", "income_band");
    private static final Map<String, String> QUESTION_TEXT = Map.of(
            "age", "지원받을 분의 나이는 몇 살인가요?",
            "disability_type", "어떤 장애로 등록하셨나요?",
            "severity", "장애 정도가 '심한 장애'인가요, '심하지 않은 장애'인가요?",
            "status", "기초생활수급자나 차상위계층에 해당하나요?",
            "income_band", "가구의 한 달 소득은 어느 구간인가요?");
    private static final int MAX_QUESTIONS = 2;

    private static final String SYSTEM = """
            너는 장애인 복지 안내 서비스에서 사용자 글을 정리하는 역할이다.
            글에서 지원받을 사람(지원대상자)에 대한 사실만 뽑아 JSON으로 기록한다.
            - 글에 분명히 적힌 것만 기록한다. 추측하지 않는다. 없으면 기록하지 않는다.
            - evidence는 사용자 글의 일부를 한 글자도 바꾸지 않고 그대로 복사한다.
            - age: 만 나이 정수 (예: "네 살" -> 4)
            - household: 함께 사는 가구원 수 정수
            - disability_type: %s
            - severity: %s
            - status: %s
            - income_band: 기준 중위소득 대비. %s
            - tags: 하나당 사실 하나로 기록한다. %s
            - 자격이나 받을 수 있는 서비스는 판단하지 않는다.
            """.formatted(codes("disability_type"), codes("severity"), codes("status"), codes("income_band"), codes("tags"));

    private static final Map<String, Object> SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of("facts", Map.of(
                    "type", "array",
                    "items", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                    "key", Map.of("type", "string", "enum", FACT_KEYS),
                                    "value", Map.of("type", List.of("string", "integer")),
                                    "evidence", Map.of("type", "string")),
                            "required", List.of("key", "value", "evidence")))),
            "required", List.of("facts"));

    private final LlmClient llmClient;

    public AnalyzeResponseDto analyze(AnalyzeRequestDto request) {
        JsonNode result;
        try {
            String who = request.actor().equals("guardian") ? "보호자가 지원대상자에 대해 쓴 글" : "지원대상자 본인이 쓴 글";
            result = llmClient.extractJson(SYSTEM, who + ":\n" + request.text(), SCHEMA_NAME, SCHEMA);
        } catch (RuntimeException e) {
            // 사용자 글은 로그에 남기지 않는다
            log.warn("대화 정리 AI 호출 실패: {}", e.getClass().getSimpleName());
            throw new AnalyzeException(ErrorCode.AI_FAILED);
        }
        return toResponse(request.text(), result);
    }

    AnalyzeResponseDto toResponse(String text, JsonNode result) {
        Map<String, FactDto> found = new HashMap<>();
        List<String> tags = new ArrayList<>();
        List<String> tagEvidence = new ArrayList<>();

        for (JsonNode fact : result.path("facts")) {
            String key = fact.path("key").asString("");
            String evidence = fact.path("evidence").asString("");
            Object value = normalize(key, fact.path("value"));
            // 원문에 없는 근거나 허용값 밖의 값은 버린다
            if (value == null || evidence.isBlank() || !text.contains(evidence)) {
                continue;
            }
            if (key.equals("tags")) {
                if (!tags.contains(value)) {
                    tags.add((String) value);
                    tagEvidence.add(evidence);
                }
            } else {
                found.putIfAbsent(key, new FactDto(key, value, evidence));
            }
        }
        if (!tags.isEmpty()) {
            found.put("tags", new FactDto("tags", tags, String.join(" / ", tagEvidence)));
        }

        List<FactDto> facts = FACT_KEYS.stream()
                .map(key -> found.getOrDefault(key, new FactDto(key, emptyValue(key), null)))
                .toList();
        List<String> missing = REQUIRED.stream().filter(key -> !found.containsKey(key)).toList();
        List<QuestionDto> questions = missing.stream().limit(MAX_QUESTIONS).map(AnalyzeService::question).toList();
        return new AnalyzeResponseDto(facts, missing, questions);
    }

    private static Object normalize(String key, JsonNode value) {
        return switch (key) {
            case "age" -> intIn(value, 0, 120);
            case "household" -> intIn(value, 1, 20);
            case "disability_type", "severity", "status", "income_band", "tags" -> {
                String code = value.asString("");
                yield ALLOWED.get(key).containsKey(code) ? code : null;
            }
            default -> null;
        };
    }

    private static Integer intIn(JsonNode value, int min, int max) {
        int n;
        if (value.isIntegralNumber()) {
            n = value.asInt();
        } else {
            try {
                n = Integer.parseInt(value.asString("").trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return n >= min && n <= max ? n : null;
    }

    private static Object emptyValue(String key) {
        return switch (key) {
            case "age", "household" -> null;
            case "tags" -> List.of();
            default -> UNKNOWN;
        };
    }

    private static QuestionDto question(String key) {
        List<OptionDto> options = new ArrayList<>();
        if (ALLOWED.containsKey(key)) {
            ALLOWED.get(key).forEach((value, label) -> options.add(new OptionDto(value, label)));
            options.add(new OptionDto(UNKNOWN, "모름"));
        }
        return new QuestionDto(key, QUESTION_TEXT.get(key), options);
    }

    private static String codes(String key) {
        StringBuilder sb = new StringBuilder();
        ALLOWED.get(key).forEach((value, label) -> sb.append(value).append("(").append(label).append(") "));
        return sb.toString().trim();
    }

    private static Map<String, String> linked(String... pairs) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return map;
    }
}
