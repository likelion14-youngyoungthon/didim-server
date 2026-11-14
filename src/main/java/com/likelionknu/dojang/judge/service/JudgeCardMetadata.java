package com.likelionknu.dojang.judge.service;

import com.likelionknu.dojang.judge.data.dto.request.JudgeRequestDto;
import com.likelionknu.dojang.judge.data.dto.response.JudgeResponseDto.Benefit;
import com.likelionknu.dojang.judge.data.dto.response.JudgeResponseDto.Condition;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Presentation metadata comes from the rule table; display sentences are never parsed as policy. */
final class JudgeCardMetadata {
    private static final Map<String, String> TYPES = Map.ofEntries(
            Map.entry("physical", "지체"), Map.entry("brain_lesion", "뇌병변"), Map.entry("visual", "시각"),
            Map.entry("hearing", "청각"), Map.entry("speech", "언어"), Map.entry("intellectual", "지적"),
            Map.entry("autism", "자폐성"), Map.entry("mental", "정신"), Map.entry("kidney", "신장"),
            Map.entry("heart", "심장"), Map.entry("respiratory", "호흡기"), Map.entry("liver", "간"),
            Map.entry("facial", "안면"), Map.entry("ostomy", "장루·요루"), Map.entry("epilepsy", "뇌전증"));
    private JudgeCardMetadata() {}

    static String shortDescription(JsonNode rule, Benefit benefit) {
        String fallback = text(rule, "short_desc");
        if (!rule.path("short_desc_from_benefit").asBoolean(false) || benefit == null || benefit.amount() == null) return fallback;
        return benefit.amount() + (benefit.note() == null || benefit.note().isBlank() ? "" : " · " + benefit.note());
    }

    static Benefit benefit(JudgeRequestDto request, JsonNode rule) {
        JsonNode benefits = rule.path("benefit");
        if (!benefits.isArray()) return null;
        for (String when : new String[]{request.status(), request.incomeBand(), "any"}) {
            if (when == null) continue;
            for (JsonNode candidate : benefits) {
                if (when.equals(text(candidate, "when"))) {
                    return new Benefit(text(candidate, "amount"), text(candidate, "note"));
                }
            }
        }
        return null;
    }

    static List<Condition> conditions(JudgeRequestDto request, JsonNode rule) {
        return conditions(request, rule, java.time.Year.now(java.time.ZoneId.of("Asia/Seoul")).getValue());
    }

    static List<Condition> conditions(JudgeRequestDto request, JsonNode rule, int currentYear) {
        JsonNode definitions = rule.path("conditions");
        if (!definitions.isArray()) return null;
        List<Condition> result = new ArrayList<>();
        for (JsonNode definition : definitions) {
            String key = text(definition, "key");
            String value = null;
            Boolean met = null;
            if ("age".equals(key)) {
                value = "만 " + request.age() + "세";
                Integer min = number(rule, "age_min"), max = number(rule, "age_max");
                if (BirthYearEligibility.applies(rule)) {
                    int match = BirthYearEligibility.match(request.age(), rule, currentYear);
                    met = match == 0 ? null : match > 0;
                } else if (min != null || max != null) {
                    if (min != null && request.age() < min) met = false;
                    else if (max != null && request.age() > max) {
                        // These services have exceptions that the request cannot resolve.
                        met = List.of("activity_support", "yongin_activity_extra", "dd_day_activity", "employment_package")
                                .contains(text(rule, "id")) ? null : false;
                    } else met = true;
                }
            } else if ("disability_type".equals(key)) {
                value = label(request.disabilityType(), TYPES);
                JsonNode types = rule.path("disability_types");
                if ("all".equals(types.isString() ? types.asString() : null)) met = true;
                else if (!unknown(request.disabilityType()) && types.isArray()) {
                    if (contains(types, request.disabilityType())) met = true;
                    else if (contains(rule.path("disability_types_conditional"), request.disabilityType())
                            || wheelchairRoute(request, rule)) met = null;
                    else met = false;
                }
            } else if ("severity".equals(key)) {
                value = label(request.severity(), Map.of("severe", "심한 장애", "mild", "심하지 않은 장애"));
                String severity = text(rule, "severity");
                if ("any".equals(severity)) met = true;
                else if (severity != null && !unknown(request.severity())) {
                    if (severity.equals(request.severity())) met = true;
                    else if (wheelchairRoute(request, rule)) met = null;
                    else met = false;
                }
            } else if ("income_band".equals(key) || "status".equals(key)) {
                value = "income_band".equals(key)
                        ? label(request.incomeBand(), Map.of("le120", "기준 중위소득 120% 이하", "le180", "기준 중위소득 180% 이하", "over180", "그보다 많아요"))
                        : label(request.status(), Map.of("basic", "기초생활수급", "near_poor", "차상위", "neither", "해당 없어요"));
                int match = JudgeService.income(request, rule.path("income"));
                met = match == 0 ? null : match > 0;
            }
            result.add(new Condition(key, text(definition, "label"), text(definition, "rule"), value, met));
        }
        return List.copyOf(result);
    }

    private static boolean wheelchairRoute(JudgeRequestDto request, JsonNode rule) {
        return "special_transport".equals(text(rule, "id")) && request.tags() != null && request.tags().contains("wheelchair");
    }

    private static boolean contains(JsonNode values, String expected) {
        if (values.isArray()) for (JsonNode value : values) if (expected.equals(value.asString())) return true;
        return false;
    }

    private static String label(String value, Map<String, String> labels) {
        return unknown(value) ? "잘 모르겠어요" : labels.get(value);
    }

    private static boolean unknown(String value) { return value == null || "unknown".equals(value); }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asString();
    }

    private static Integer number(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isNumber() ? value.asInt() : null;
    }
}
