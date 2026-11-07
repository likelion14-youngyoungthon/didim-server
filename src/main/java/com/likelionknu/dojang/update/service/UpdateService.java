package com.likelionknu.dojang.update.service;

import com.likelionknu.dojang.analyze.service.AnalyzeService;
import com.likelionknu.dojang.analyze.service.LlmClient;
import com.likelionknu.dojang.common.response.ErrorCode;
import com.likelionknu.dojang.dong.data.dto.response.DongResponseDto;
import com.likelionknu.dojang.dong.service.DongService;
import com.likelionknu.dojang.update.data.dto.request.UpdateRequestDto;
import com.likelionknu.dojang.update.data.dto.request.UpdateRequestDto.ServiceStateDto;
import com.likelionknu.dojang.update.data.dto.response.UpdateResponseDto;
import com.likelionknu.dojang.update.data.dto.response.UpdateResponseDto.ChangeDto;
import com.likelionknu.dojang.update.exception.UpdateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * "신청서 냈어요" 같은 말을 상태 변경안으로 바꾼다 (요구사항 8, 보완 요청 7.1.2).
 * 서버는 상태를 저장하지 않는다. AI가 낸 서비스 id, 상태 값, 근거는 코드로 다시 확인하고 맞지 않으면 버린다.
 */
@Slf4j
@Service
public class UpdateService {
    private static final Set<String> STATUS_TO = Set.of("submitted", "approved", "in_use");
    private static final Set<String> PERSONS = Set.of("self", "child", "spouse", "parent", "other");
    // 판정에 큰 영향을 주는 값. 항상 needs_confirm
    private static final Set<String> FACT_KEYS = Set.of("dong", "disability_type", "severity", "status", "income_band");
    // ponytail: 보완 요청은 늘 서류 준비 단계부터 다시 연다. 신청 단계 API가 생기면 단계 id를 맞춘다
    private static final String REOPEN_STEP = "prepare";

    private static final String SYSTEM = """
            너는 장애인 복지 안내 서비스에서 사용자 말을 신청 상태 변경안으로 바꾸는 역할이다.
            사용자 글에 분명히 적힌 변경만 changes에 기록한다. 없으면 빈 목록이다. 추측하지 않는다.
            type:
            - status: 신청서를 냈다(submitted), 승인 안내를 받았다(approved), 이용 중이다(in_use). service_id, to
            - supplement: 서류 보완이나 추가 제출 요청을 받았다. service_id
            - correct_service: 앞에서 말한 서비스가 아니라 다른 서비스라고 고쳤다. from_service, to_service
            - correct_person: 그 서비스의 대상자가 다른 사람이라고 고쳤다. service_id, person(self, child, spouse, parent, other)
            - fact: 거주지, 장애 유형, 장애 정도, 수급 여부, 소득 구간이 바뀌었다. key, to
              key와 to 값: dong(용인시 읍면동 이름, 예: 백암면), %s
            service_id는 아래 서비스 목록의 id만 쓴다.
            evidence는 사용자 글의 일부를 한 글자도 바꾸지 않고 그대로 복사한다.
            """;

    private static final Map<String, Object> SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of("changes", Map.of(
                    "type", "array",
                    "items", Map.of(
                            "type", "object",
                            "properties", Map.of(
                                    "type", Map.of("type", "string", "enum", List.of("status", "supplement", "correct_service", "correct_person", "fact")),
                                    "service_id", Map.of("type", List.of("string", "null")),
                                    "to", Map.of("type", List.of("string", "null")),
                                    "from_service", Map.of("type", List.of("string", "null")),
                                    "to_service", Map.of("type", List.of("string", "null")),
                                    "person", Map.of("type", List.of("string", "null")),
                                    "key", Map.of("type", List.of("string", "null")),
                                    "evidence", Map.of("type", "string")),
                            "required", List.of("type", "evidence")))),
            "required", List.of("changes"));

    private final LlmClient llmClient;
    private final Set<String> dongs;
    private final Map<String, String> serviceNames = new LinkedHashMap<>();
    private final String system;

    public UpdateService(LlmClient llmClient, DongService dongService, JsonMapper jsonMapper) throws IOException {
        this.llmClient = llmClient;
        this.dongs = dongService.getDongs().stream().map(DongResponseDto::dong).collect(Collectors.toSet());
        for (String path : List.of("data/rules_child_v2.json", "data/rules_adult_v2.json")) {
            jsonMapper.readTree(new ClassPathResource(path).getInputStream()).path("services")
                    .forEach(service -> serviceNames.putIfAbsent(service.path("id").asString(), service.path("name").asString()));
        }
        StringBuilder codes = new StringBuilder();
        for (String key : List.of("disability_type", "severity", "status", "income_band")) {
            codes.append(key).append('(').append(String.join(", ", AnalyzeService.ALLOWED.get(key).keySet())).append(") ");
        }
        this.system = SYSTEM.formatted(codes.toString().trim());
    }

    public UpdateResponseDto extract(UpdateRequestDto request) {
        // 화면이 보낸 카드가 있으면 그 안에서만 찾고 현재 상태를 from으로 쓴다
        Map<String, String> current = new HashMap<>();
        if (request.services() != null) {
            for (ServiceStateDto state : request.services()) {
                if (serviceNames.containsKey(state.id())) {
                    current.put(state.id(), state.status());
                }
            }
        }
        Set<String> candidates = current.isEmpty() ? serviceNames.keySet() : current.keySet();

        StringBuilder prompt = new StringBuilder("서비스 목록:\n");
        candidates.forEach(id -> prompt.append(id).append(": ").append(serviceNames.get(id)).append('\n'));
        prompt.append("\n사용자 글:\n").append(request.text());

        JsonNode result;
        try {
            result = llmClient.extractJson(system, prompt.toString(), "changes", SCHEMA);
        } catch (RuntimeException e) {
            log.warn("상태 변경 추출 AI 호출 실패: {}", e.getClass().getSimpleName());
            throw new UpdateException(ErrorCode.AI_FAILED);
        }

        List<ChangeDto> changes = new ArrayList<>();
        for (JsonNode change : result.path("changes")) {
            ChangeDto dto = validate(change, request.text(), candidates, current);
            if (dto != null) {
                changes.add(dto);
            }
        }
        return new UpdateResponseDto(changes);
    }

    private ChangeDto validate(JsonNode change, String text, Set<String> candidates, Map<String, String> current) {
        String evidence = change.path("evidence").asString("");
        if (evidence.isBlank() || !text.contains(evidence)) {
            return null;
        }
        String serviceId = change.path("service_id").asString(null);
        String to = change.path("to").asString(null);
        boolean knownService = serviceId != null && candidates.contains(serviceId);

        return switch (change.path("type").asString("")) {
            case "status" -> knownService && STATUS_TO.contains(to)
                    ? new ChangeDto("status", serviceId, current.get(serviceId), to, null, null, null, null, null, false, evidence) : null;
            case "supplement" -> knownService
                    ? new ChangeDto("supplement", serviceId, null, null, REOPEN_STEP, null, null, null, null, false, evidence) : null;
            case "correct_service" -> {
                String from = change.path("from_service").asString(null);
                String target = change.path("to_service").asString(null);
                // 바꿀 서비스는 지금 카드에 없을 수도 있어서 전체 목록에서 확인
                yield from != null && candidates.contains(from) && serviceNames.containsKey(target) && !from.equals(target)
                        ? new ChangeDto("correct_service", null, null, null, null, from, target, null, null, false, evidence) : null;
            }
            case "correct_person" -> {
                String person = change.path("person").asString(null);
                yield knownService && PERSONS.contains(person)
                        ? new ChangeDto("correct_person", serviceId, null, null, null, null, null, person, null, false, evidence) : null;
            }
            case "fact" -> {
                String key = change.path("key").asString(null);
                boolean valid = key != null && FACT_KEYS.contains(key) && to != null
                        && (key.equals("dong") ? dongs.contains(to) : AnalyzeService.ALLOWED.get(key).containsKey(to));
                yield valid ? new ChangeDto("fact", null, null, to, null, null, null, null, key, true, evidence) : null;
            }
            default -> null;
        };
    }
}
