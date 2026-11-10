package com.likelionknu.dojang.steps.service;

import com.likelionknu.dojang.steps.data.dto.response.ServiceStepsResponseDto;
import com.likelionknu.dojang.steps.data.dto.response.ServiceStepsResponseDto.Step;
import com.likelionknu.dojang.steps.exception.ServiceStepsException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ServiceStepsService {
    private final Map<String, ServiceStepsResponseDto> services;

    public ServiceStepsService(JsonMapper mapper) throws IOException {
        Map<String, String> offices = new HashMap<>();
        try (var input = new ClassPathResource("data/offices.json").getInputStream()) {
            for (JsonNode office : mapper.readTree(input).path("offices")) {
                offices.put(text(office, "id"), text(office, "name"));
            }
        }
        Map<String, ServiceStepsResponseDto> indexed = new LinkedHashMap<>();
        // Adult entries contain the detailed common-service rules; child-only services supplement them.
        for (String path : List.of("data/rules_adult_v2.json", "data/rules_child_v2.json")) {
            try (var input = new ClassPathResource(path).getInputStream()) {
                for (JsonNode rule : mapper.readTree(input).path("services")) {
                    String id = text(rule, "id");
                    indexed.putIfAbsent(id, response(rule, offices));
                }
            }
        }
        services = Map.copyOf(indexed);
    }

    public ServiceStepsResponseDto getSteps(String serviceId) {
        ServiceStepsResponseDto response = services.get(serviceId);
        if (response == null) throw new ServiceStepsException();
        return response;
    }

    private ServiceStepsResponseDto response(JsonNode rule, Map<String, String> offices) {
        JsonNode processingDays = rule.path("processing_days");
        Integer days = processingDays.isNumber() ? processingDays.asInt() : null;
        return new ServiceStepsResponseDto(text(rule, "id"), List.of(
                new Step("prepare", "서류 준비", null, true),
                new Step("submit", submissionLabel(rule, offices), null, false),
                new Step("review", "자격 심사", days, false),
                new Step("result", "결과 안내", null, false)));
    }

    private String submissionLabel(JsonNode rule, Map<String, String> offices) {
        if (!"none".equals(text(rule.path("auto"), "type"))) return "별도 신청·적용 경로 확인";
        String officeId = text(rule, "office_id");
        if ("dong_center".equals(officeId)) return "행정복지센터 신청";
        String officeName = offices.get(officeId);
        if (officeName != null && !officeName.isBlank()) return officeName + " 신청";
        String officeNote = text(rule, "office_note");
        return officeNote == null || officeNote.isBlank() ? "신청 경로 확인" : officeNote + " (신청 경로 확인)";
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isString() ? value.asString() : null;
    }
}
