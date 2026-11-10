package com.likelionknu.dojang.judge.service;

import com.likelionknu.dojang.judge.data.dto.request.JudgeRequestDto;
import com.likelionknu.dojang.judge.data.dto.response.JudgeResponseDto;
import com.likelionknu.dojang.judge.data.dto.response.JudgeResponseDto.*;
import com.likelionknu.dojang.judge.exception.JudgeException;
import com.likelionknu.dojang.office.service.OfficeService;
import com.likelionknu.dojang.regitem.service.RegItemService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.util.*;

@Service
public class JudgeService {
    private static final Set<String> TYPES = Set.of("physical", "brain_lesion", "visual", "hearing", "speech", "intellectual", "autism", "mental", "kidney", "heart", "respiratory", "liver", "facial", "ostomy", "epilepsy", "unknown");
    private static final Set<String> TAGS = Set.of("daycare", "has_car", "wheelchair", "newborn");
    // These requirements exist only in prose and cannot be resolved from the current request.
    private static final Map<String, String> EXTRA_CHECKS = Map.ofEntries(
        Map.entry("special_transport", "보행상 장애 또는 진단서 등 이용대상자 등록 요건 확인 필요"),
        Map.entry("special_education", "진단평가를 통한 특수교육 필요성 확인 필요"),
        Map.entry("assistive_device", "품목별 기준과 보조기기 적격성 확인 필요"),
        Map.entry("parking_sticker", "차량 명의·동거 요건 및 주차가능 표지의 보행상 장애 확인 필요"),
        Map.entry("fee_reductions_7", "감면 항목별 장애 정도·소득·동의 요건 확인 필요"),
        Map.entry("yongin_activity_extra", "장애인활동지원 결정 및 서비스 구간 확인 필요"),
        Map.entry("health_insurance_reduction", "건강보험 지역가입자 세대 여부와 자동 적용 확인 필요"),
        Map.entry("job_program", "미취업 여부 및 선발 조건 확인 필요"),
        Map.entry("yongin_birth_grant", "출산한 장애인 부모 여부·180일 거주·신청 기한 확인 필요"),
        Map.entry("car_excise_tax", "차량 구입·명의·가구 요건 확인 필요"),
        Map.entry("diagnosis_fee", "신규 등록·재판정 구분 및 세부 급여 자격 확인 필요"),
        Map.entry("sports_voucher", "지원 연도별 출생연도 기준과 모집·선정 여부 확인 필요"),
        Map.entry("disabled_childcare_fee", "미취학 여부 및 어린이집 이용·입소 예정 확인 필요")
    );
    private final List<JsonNode> childRules;
    private final List<JsonNode> adultRules;
    private final OfficeService officeService;
    private final Set<String> regItemIds;

    public JudgeService(JsonMapper mapper, OfficeService officeService, RegItemService regItemService) throws IOException {
        this.officeService = officeService;
        this.regItemIds = new HashSet<>();
        regItemService.getRegItems().forEach(item -> regItemIds.add(item.id()));
        childRules = load(mapper, "data/rules_child_v2.json");
        adultRules = load(mapper, "data/rules_adult_v2.json");
    }

    private List<JsonNode> load(JsonMapper mapper, String path) throws IOException {
        try (var stream = new ClassPathResource(path).getInputStream()) {
            List<JsonNode> result = new ArrayList<>();
            mapper.readTree(stream).path("services").forEach(result::add);
            return List.copyOf(result);
        }
    }

    public Map<String, JudgeResponseDto> judge(JudgeRequestDto request) {
        validate(request);
        var offices = officeService.getOffices(request.dong(), null, null);
        Map<String, Office> officeMap = new HashMap<>();
        offices.forEach(o -> officeMap.put(o.id(), new Office(o.id(), o.name(), o.phone())));
        List<JsonNode> rules = request.age() < 18 ? childRules : adultRules;
        Map<String, JudgeResponseDto> result = new LinkedHashMap<>();
        for (JsonNode rule : rules) {
            JudgeResponseDto item = evaluate(request, rule, officeMap);
            result.put(item.id(), item);
        }
        return result;
    }

    private JudgeResponseDto evaluate(JudgeRequestDto q, JsonNode r, Map<String, Office> offices) {
        String id = text(r, "id");
        List<String> notes = new ArrayList<>();
        for (String field : List.of("age_note", "severity_note", "office_note", "note", "exclusive_note", "amount_note")) add(notes, text(r, field));
        add(notes, text(r.path("auto"), "note"));
        boolean excluded = false;
        boolean check = false;
        Integer fromAge = null;
        Integer min = integer(r, "age_min"), max = integer(r, "age_max");
        if (min != null && q.age() < min) { excluded = true; fromAge = min; }
        if (max != null && q.age() > max) {
            if (Set.of("activity_support", "yongin_activity_extra", "dd_day_activity", "employment_package").contains(id)) {
                check = true; add(notes, "상한 연령의 예외 및 적용 가능 여부 확인 필요");
            } else excluded = true;
        }
        boolean wheelchairRoute = id.equals("special_transport") && list(q.tags()).contains("wheelchair");
        JsonNode types = r.path("disability_types");
        if (!(types.isString() && "all".equals(types.asString()))) {
            if (unknown(q.disabilityType())) check = true;
            else if (!strings(types).contains(q.disabilityType())) {
                if (strings(r.path("disability_types_conditional")).contains(q.disabilityType()) || wheelchairRoute) check = true;
                else excluded = true;
            }
        }
        String severity = text(r, "severity");
        if (!"any".equals(severity)) {
            if (unknown(q.severity())) check = true;
            else if (!severity.equals(q.severity())) {
                if (wheelchairRoute) check = true; else excluded = true;
            }
        }
        if (wheelchairRoute) { check = true; add(notes, "휠체어 필요·기간이 적힌 종합병원급 이상 진단서 확인 필요"); }
        int income = income(q, r.path("income"));
        // The prose permits near-poor reassessment and income-independent official reassessment.
        if (income < 0 && id.equals("diagnosis_fee")) {
            income = 0;
        }
        if (income < 0) excluded = true;
        if (income == 0) { check = true; add(notes, "소득 또는 세부 급여 요건 확인 필요"); }
        if (EXTRA_CHECKS.containsKey(id)) { check = true; add(notes, EXTRA_CHECKS.get(id)); }
        if (id.equals("disability_allowance") && q.age() <= 20) {
            check = true; add(notes, "18~20세 학교 재학·휴학에 따른 장애아동수당 적용 여부 확인 필요");
        }
        if (id.equals("child_allowance") && "basic".equals(q.status())) {
            check = true; add(notes, "생계·의료급여인지 확인 후 직권 지급 여부 판단 필요");
        }
        boolean hidden = false;
        for (JsonNode effect : r.path("situation_effects")) {
            String tag = text(effect, "tag");
            if ("show_only_if".equals(text(effect, "effect")) && !list(q.tags()).contains(tag)) hidden = true;
            if (list(q.tags()).contains(tag)) add(notes, text(effect, "note"));
        }
        String regForm = text(r, "reg_form"), regItem = text(r, "reg_form_item");
        boolean checked = regItem != null && list(q.regItems()).contains(regItem);
        String verdict = excluded ? "not_eligible" : check ? "check_needed" : "eligible";
        String status;
        if (excluded) status = null;
        else if (!"no".equals(regForm) && Boolean.TRUE.equals(q.regItemsUnknown())) status = "check";
        else if (checked && "yes".equals(regForm)) status = "already";
        else if (checked && "partial".equals(regForm)) status = "extra_form";
        else if (check || (!"no".equals(regForm) && (Boolean.TRUE.equals(q.regItemsUnknown()) || q.regItems() == null))) status = "check";
        else if ("auto".equals(text(r.path("auto"), "type")) && "always".equals(text(r.path("auto"), "when"))) status = "auto";
        else status = "apply";
        if (checked) add(notes, "등록 신청서 체크 내역이며 실제 접수·승인·이용 상태를 확인한 것은 아닙니다.");
        List<String> names = strings(r.path("form_names"));
        List<FormFile> files = names.stream().map(name -> new FormFile(name, null)).toList();
        String officeId = text(r, "office_id");
        Office office = offices.get(officeId);
        if (office == null) office = new Office(officeId, text(r, "office_note"), null);
        return new JudgeResponseDto(id, text(r, "name"), verdict, status, hidden, office, names, files,
            strings(r.path("documents")), integer(r, "processing_days"), text(r, "deadline"), List.copyOf(notes), fromAge,
            new Basis(text(r, "confidence"), strings(r.path("source_urls")), strings(r.path("unverified_fields")), text(r.path("income"), "note")));
    }

    // -1: definitely outside the supplied interval; 0: cannot decide; 1: matches.
    private int income(JudgeRequestDto q, JsonNode rule) {
        String type = text(rule, "type");
        if ("none".equals(type)) return 1;
        if ("basic_or_near_poor".equals(type)) return unknown(q.status()) ? 0 : Set.of("basic", "near_poor").contains(q.status()) ? 1 : -1;
        if ("basic".equals(type)) return unknown(q.status()) ? 0 : "basic".equals(q.status()) ? 1 : -1;
        if (!"median_pct".equals(type)) return 0;
        if (unknown(q.incomeBand())) return 0;
        Integer pct = integer(rule, "pct"), min = integer(rule, "pct_min");
        if (pct == null) return 0;
        int low = switch(q.incomeBand()) { case "le180" -> 120; case "over180" -> 180; default -> 0; };
        int high = switch(q.incomeBand()) { case "le120" -> 120; case "le180" -> 180; default -> Integer.MAX_VALUE; };
        if (low >= pct || (min != null && high <= min)) return -1;
        return high <= pct && (min == null || low >= min) ? 1 : 0;
    }

    private void validate(JudgeRequestDto q) {
        if (q == null) throw new JudgeException("요청 본문이 필요합니다.");
        if (q.age() == null) throw new JudgeException("age: 나이가 필요합니다.");
        allowed("disability_type", q.disabilityType(), TYPES);
        allowed("severity", q.severity(), Set.of("severe", "mild", "unknown"));
        allowed("status", q.status(), Set.of("basic", "near_poor", "neither", "unknown"));
        allowed("income_band", q.incomeBand(), Set.of("le120", "le180", "over180", "unknown"));
        for (String value : list(q.regItems())) if (value == null || !regItemIds.contains(value)) throw new JudgeException("reg_items: 허용되지 않은 항목입니다.");
        for (String value : list(q.tags())) if (value == null || !TAGS.contains(value)) throw new JudgeException("tags: 허용되지 않은 값입니다.");
        if (q.age() != null && q.age() < 0) throw new JudgeException("age: 0 이상이어야 합니다.");
        if (q.household() != null && q.household() < 1) throw new JudgeException("household: 1 이상이어야 합니다.");
    }
    private static void allowed(String field, String value, Set<String> allowed) {
        if (value != null && !allowed.contains(value)) throw new JudgeException(field + ": 허용되지 않은 값입니다.");
    }
    private static boolean unknown(String value) { return value == null || value.equals("unknown"); }
    private static List<String> list(List<String> values) { return values == null ? List.of() : values; }
    private static void add(List<String> notes, String value) { if (value != null && !notes.contains(value)) notes.add(value); }
    private static String text(JsonNode node, String field) { JsonNode v = node.path(field); return v.isMissingNode() || v.isNull() ? null : v.asString(); }
    private static Integer integer(JsonNode node, String field) { JsonNode v = node.path(field); return v.isNumber() ? v.asInt() : null; }
    private static List<String> strings(JsonNode node) { List<String> values = new ArrayList<>(); if (node.isArray()) node.forEach(v -> values.add(v.asString())); return List.copyOf(values); }
}
