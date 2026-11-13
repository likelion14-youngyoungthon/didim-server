package com.likelionknu.dojang.judge;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class JudgeControllerTest {
    @Autowired MockMvc mvc;
    @Autowired JsonMapper mapper;

    private static final String CHILD = """
            {"age":4,"disability_type":"autism","severity":"severe","dong":"백암면",
             "household":4,"status":"neither","income_band":"le120",
             "reg_items":["fee_reductions_7","parking_sticker"],"reg_items_unknown":false,"tags":["daycare"]}
            """;

    private ResultActions judge(String body) throws Exception {
        return mvc.perform(post("/api/v1/judge").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test void 온라인_신청은_규칙표에_명시된_서비스만_안내한다() throws Exception {
        assertOnlineServices(CHILD, Set.of("dev_rehab_voucher"));
        String adult = CHILD.replace("\"age\":4", "\"age\":35").replace("autism", "hearing");
        assertOnlineServices(adult, Set.of("pension_basic", "pension_additional", "disability_allowance",
                "activity_support", "dd_day_activity", "assistive_device", "self_reliance_loan", "energy_voucher"));
    }

    private void assertOnlineServices(String request, Set<String> expected) throws Exception {
        String body = judge(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode data = mapper.readTree(body).path("data");
        Set<String> actual = new HashSet<>();
        data.properties().forEach(entry -> {
            JsonNode service = entry.getValue();
            assertTrue(service.path("online").isBoolean(), entry.getKey());
            if (service.path("online").asBoolean()) {
                actual.add(entry.getKey());
                assertTrue(service.path("basis").path("sources").path(0).asString().startsWith("https://www.bokjiro.go.kr/"));
            }
        });
        assertEquals(expected, actual);
    }

    @Test void 아동_시연_응답과_실제_창구_서식_근거() throws Exception {
        judge(CHILD).andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.message").isEmpty())
                .andExpect(jsonPath("$.data.dev_rehab_voucher.id").value("dev_rehab_voucher"))
                .andExpect(jsonPath("$.data.dev_rehab_voucher.verdict").value("eligible"))
                .andExpect(jsonPath("$.data.dev_rehab_voucher.status").value("apply"))
                .andExpect(jsonPath("$.data.dev_rehab_voucher.office.id").value("dong_center"))
                .andExpect(jsonPath("$.data.dev_rehab_voucher.office.name").value("백암면 행정복지센터"))
                .andExpect(jsonPath("$.data.dev_rehab_voucher.office.phone").value("031-6193-5792"))
                .andExpect(jsonPath("$.data.dev_rehab_voucher.form_files[0].name").value("사회보장급여(사회서비스이용권) 신청(변경)서"))
                .andExpect(jsonPath("$.data.dev_rehab_voucher.form_files[0].url").isEmpty())
                .andExpect(jsonPath("$.data.dev_rehab_voucher.basis.confidence").value("official"))
                .andExpect(jsonPath("$.data.dev_rehab_voucher.basis.sources").isNotEmpty());
    }

    @Test void 소득_재판정은_이전_결과를_저장하지_않는다() throws Exception {
        judge(CHILD.replace("le120", "over180"))
                .andExpect(jsonPath("$.data.dev_rehab_voucher.verdict").value("not_eligible"))
                .andExpect(jsonPath("$.data.dev_rehab_voucher.status").isEmpty());
        judge(CHILD.replace("le120", "le180"))
                .andExpect(jsonPath("$.data.dev_rehab_voucher.verdict").value("eligible"));
        judge(CHILD.replace("le120", "unknown"))
                .andExpect(jsonPath("$.data.dev_rehab_voucher.verdict").value("check_needed"));
    }

    @Test void 나이_하한과_성인_분기() throws Exception {
        judge(CHILD).andExpect(jsonPath("$.data.sports_voucher.verdict").value("not_eligible"))
                .andExpect(jsonPath("$.data.sports_voucher.eligible_from_age").value(5));
        judge(CHILD.replace("\"age\":4", "\"age\":18"))
                .andExpect(jsonPath("$.data.child_allowance").doesNotExist())
                .andExpect(jsonPath("$.data.pension_additional").exists());
    }

    @Test void 등록_체크는_서비스_id가_아닌_등록항목_id로_대조() throws Exception {
        judge(CHILD.replace("fee_reductions_7", "individual_plan"))
                .andExpect(jsonPath("$.data.dd_individual_plan.status").value("extra_form"))
                .andExpect(jsonPath("$.data.parking_sticker.status").value("already"));
    }

    @Test void 등록내역_모름은_등록서식_외_서비스에_영향없다() throws Exception {
        judge(CHILD.replace("\"reg_items_unknown\":false", "\"reg_items_unknown\":true"))
                .andExpect(jsonPath("$.data.dev_rehab_voucher.status").value("apply"))
                .andExpect(jsonPath("$.data.fee_reductions_7.status").value("check"));
    }

    @Test void 수급_종류를_모르면_아동수당_자동지급_단정하지_않는다() throws Exception {
        judge(CHILD.replace("neither", "basic"))
                .andExpect(jsonPath("$.data.child_allowance.status").value("check"));
    }

    @Test void 조건부_유형과_휠체어_대체경로를_확정_제외하지_않는다() throws Exception {
        judge(CHILD).andExpect(jsonPath("$.data.special_transport.verdict").value("check_needed"));
        judge(CHILD.replace("severe", "mild").replace("daycare", "wheelchair"))
                .andExpect(jsonPath("$.data.special_transport.verdict").value("check_needed"));
    }

    @Test void AI_미확인값과_선택_가구원수_누락을_받는다() throws Exception {
        judge(CHILD.replace("autism", "unknown").replace("\"household\":4,", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dev_rehab_voucher.verdict").value("check_needed"));
    }

    @Test void 태그_조건_미충족은_숨기되_불충족_카드도_반환한다() throws Exception {
        judge(CHILD.replace("[\"daycare\"]", "[]"))
                .andExpect(jsonPath("$.data.disabled_childcare_fee.hidden").value(true))
                .andExpect(jsonPath("$.data.sports_voucher.hidden").value(false));
    }

    @Test void 세부소득과_미검증_연령_경계는_확인필요() throws Exception {
        judge(CHILD.replace("\"age\":4", "\"age\":30"))
                .andExpect(jsonPath("$.data.self_reliance_loan.verdict").value("check_needed"));
        judge(CHILD.replace("\"age\":4", "\"age\":65"))
                .andExpect(jsonPath("$.data.dd_day_activity.verdict").value("check_needed"));
    }

    @Test void 진단서비_재판정_예외와_교통_추가조건은_확인필요() throws Exception {
        judge(CHILD.replace("\"age\":4", "\"age\":30").replace("neither", "near_poor"))
                .andExpect(jsonPath("$.data.diagnosis_fee.verdict").value("check_needed"));
        judge(CHILD.replace("autism", "visual"))
                .andExpect(jsonPath("$.data.special_transport.verdict").value("check_needed"));
    }

    @Test void 나이는_규칙표_선택에_필수() throws Exception {
        judge(CHILD.replace("\"age\":4,", ""))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E400"));
    }

    @Test void 잘못된_코드와_주소_음수는_E400() throws Exception {
        for (String body : new String[]{CHILD.replace("autism", "invalid"), CHILD.replace("백암면", "없는동"),
                CHILD.replace("\"age\":4", "\"age\":-1"), CHILD.replace("daycare", "bad_tag"),
                CHILD.replace("fee_reductions_7", "bad_item"), CHILD.replace("\"household\":4", "\"household\":0")}) {
            judge(body).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E400"))
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }
    private static final String LATEST_CHILD = CHILD.replace("\"household\":4", "\"household\":3")
            .replace("le120", "le180");

    @Test void 판정_오류_코드와_메시지가_명세와_일치한다() throws Exception {
        String[][] cases = {
                {LATEST_CHILD.replace("\"age\":4,", ""), "age: 널이어서는 안됩니다"},
                {LATEST_CHILD.replace("\"age\":4", "\"age\":-1"), "age: 0 이상이어야 합니다"},
                {LATEST_CHILD.replace("\"household\":3", "\"household\":0"), "household: 1 이상이어야 합니다"},
                {LATEST_CHILD.replace("autism", "invalid"), "disability_type: 허용되지 않은 값입니다."},
                {LATEST_CHILD.replace("severe", "invalid"), "severity: 허용되지 않은 값입니다."},
                {LATEST_CHILD.replace("neither", "invalid"), "status: 허용되지 않은 값입니다."},
                {LATEST_CHILD.replace("le180", "invalid"), "income_band: 허용되지 않은 값입니다."},
                {LATEST_CHILD.replace("fee_reductions_7", "invalid"), "reg_items: 허용되지 않은 항목입니다."},
                {LATEST_CHILD.replace("daycare", "invalid"), "tags: 허용되지 않은 값입니다."},
                {LATEST_CHILD.replace("백암면", "없는동"), "dong: 용인시 읍면동이 아닙니다."},
                {LATEST_CHILD.replace("\"dong\":\"백암면\",", ""), "dong: 용인시 읍면동이 아닙니다."},
                {"{", "잘못된 요청입니다."}
        };
        for (String[] item : cases) {
            judge(item[0]).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("E400"))
                    .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()))
                    .andExpect(jsonPath("$.message").value(item[1]));
        }
        mvc.perform(post("/api/v1/judge").contentType(MediaType.TEXT_PLAIN).content(LATEST_CHILD))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E400"))
                .andExpect(jsonPath("$.message").value("Content-Type은 application/json이어야 합니다."));
        mvc.perform(get("/api/v1/judge"))
                .andExpect(status().isMethodNotAllowed()).andExpect(jsonPath("$.code").value("E405"))
                .andExpect(jsonPath("$.message").value("지원하지 않는 요청 방식입니다."));
    }

    @Test void 판정_응답의_키와_JSON_타입을_검증한다() throws Exception {
        for (String request : new String[]{LATEST_CHILD, LATEST_CHILD.replace("\"age\":4", "\"age\":35").replace("autism", "hearing")}) {
            assertJudgeResponseTypes(mapper.readTree(judge(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()));
        }
    }

    private void assertJudgeResponseTypes(JsonNode root) {
        assertEquals(Set.of("code", "data", "message"), keys(root));
        assertTrue(root.path("data").isObject());
        Set<String> expected = Set.of("id", "name", "verdict", "status", "hidden", "office", "form_names", "form_files",
                "documents", "processing_days", "deadline", "notes", "eligible_from_age", "basis", "category", "short_desc",
                "description", "check_reason", "benefit", "conditions", "online", "easy");
        for (var item : root.path("data")) {
            assertEquals(expected, keys(item));
            assertTrue(item.path("easy").isNull() || item.path("easy").isObject(), "easy");
            for (String field : new String[]{"id", "name", "verdict"}) assertTrue(item.path(field).isString(), field);
            for (String field : new String[]{"status", "deadline", "category", "short_desc", "description", "check_reason"})
                assertTrue(item.path(field).isNull() || item.path(field).isString(), field);
            for (String field : new String[]{"hidden", "online"}) assertTrue(item.path(field).isBoolean(), field);
            for (String field : new String[]{"processing_days", "eligible_from_age"})
                assertTrue(item.path(field).isNull() || item.path(field).isIntegralNumber(), field);
            for (String field : new String[]{"form_names", "documents", "notes"}) assertStrings(item.path(field));
            assertEquals(Set.of("id", "name", "phone"), keys(item.path("office")));
            for (String field : new String[]{"id", "name", "phone"}) assertNullableString(item.path("office").path(field));
            assertEquals(Set.of("confidence", "sources", "unverified", "income_note"), keys(item.path("basis")));
            assertNullableString(item.path("basis").path("confidence"));
            assertNullableString(item.path("basis").path("income_note"));
            assertStrings(item.path("basis").path("sources"));
            assertStrings(item.path("basis").path("unverified"));
            assertTrue(item.path("form_files").isArray());
            for (var form : item.path("form_files")) {
                assertEquals(Set.of("name", "url"), keys(form));
                assertTrue(form.path("name").isString());
                assertTrue(form.path("url").isNull() || form.path("url").isString());
            }
            var benefit = item.path("benefit");
            assertTrue(benefit.isNull() || benefit.isObject());
            if (benefit.isObject()) {
                assertEquals(Set.of("amount", "note"), keys(benefit));
                assertNullableString(benefit.path("amount"));
                assertNullableString(benefit.path("note"));
            }
            var conditions = item.path("conditions");
            assertTrue(conditions.isNull() || conditions.isArray());
            for (var condition : conditions) {
                assertEquals(Set.of("key", "label", "rule", "value", "met"), keys(condition));
                for (String field : new String[]{"key", "label", "rule", "value"}) assertNullableString(condition.path(field));
                assertTrue(condition.path("met").isNull() || condition.path("met").isBoolean());
            }
        }
    }

    private void assertNullableString(JsonNode node) { assertTrue(node.isNull() || node.isString()); }

    private Set<String> keys(JsonNode node) {
        Set<String> result = new HashSet<>();
        node.properties().forEach(entry -> result.add(entry.getKey()));
        return result;
    }

    private void assertStrings(JsonNode node) {
        assertTrue(node.isArray());
        node.forEach(value -> assertTrue(value.isString()));
    }

    @Test void 예상하지_못한_서버오류도_명세의_E500으로_반환한다() throws Exception {
        var service = org.mockito.Mockito.mock(com.likelionknu.dojang.judge.service.JudgeService.class);
        org.mockito.Mockito.when(service.judge(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new IllegalStateException("contract test failure"));
        var isolatedMvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
                .standaloneSetup(new com.likelionknu.dojang.judge.controller.JudgeController(service))
                .setControllerAdvice(new com.likelionknu.dojang.common.response.GlobalExceptionHandler()).build();
        isolatedMvc.perform(post("/api/v1/judge").contentType(MediaType.APPLICATION_JSON).content(LATEST_CHILD))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("E500"))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.message").value("서버 오류가 발생했습니다."));
    }

    @Test void 어린이집_다니는_네다섯살은_신청후보이고_여섯살부터_취학확인() throws Exception {
        for (int age : new int[]{4, 5}) {
            judge(LATEST_CHILD.replace("\"age\":4", "\"age\":" + age))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.disabled_childcare_fee.verdict").value("eligible"))
                    .andExpect(jsonPath("$.data.disabled_childcare_fee.status").value("apply"))
                    .andExpect(jsonPath("$.data.disabled_childcare_fee.hidden").value(false));
        }
        judge(LATEST_CHILD.replace("\"age\":4", "\"age\":6"))
                .andExpect(jsonPath("$.data.disabled_childcare_fee.verdict").value("check_needed"))
                .andExpect(jsonPath("$.data.disabled_childcare_fee.status").value("check"));
        judge(LATEST_CHILD.replace("[\"daycare\"]", "[]"))
                .andExpect(jsonPath("$.data.disabled_childcare_fee.hidden").value(true))
                .andExpect(jsonPath("$.data.disabled_childcare_fee.verdict").value("check_needed"))
                .andExpect(jsonPath("$.data.disabled_childcare_fee.status").value("check"));
    }

    @Test void 최신_시연_발달재활_표시정보와_서식_서류를_구분한다() throws Exception {
        judge(LATEST_CHILD).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dev_rehab_voucher.category").value("therapy"))
                .andExpect(jsonPath("$.data.dev_rehab_voucher.short_desc").value("월 18만원 · 본인부담 8만원"))
                .andExpect(jsonPath("$.data.dev_rehab_voucher.description").value("언어·놀이·감각 등 발달재활 치료비 이용권"))
                .andExpect(jsonPath("$.data.dev_rehab_voucher.benefit.amount").value("월 18만원"))
                .andExpect(jsonPath("$.data.dev_rehab_voucher.benefit.note").value("본인부담 8만원"))
                .andExpect(jsonPath("$.data.dev_rehab_voucher.conditions.length()").value(3))
                .andExpect(jsonPath("$.data.dev_rehab_voucher.conditions[0].key").value("age"))
                .andExpect(jsonPath("$.data.dev_rehab_voucher.conditions[0].label").value("나이"))
                .andExpect(jsonPath("$.data.dev_rehab_voucher.conditions[0].rule").value("18세 미만"))
                .andExpect(jsonPath("$.data.dev_rehab_voucher.conditions[0].value").value("만 4세"))
                .andExpect(jsonPath("$.data.dev_rehab_voucher.conditions[0].met").value(true))
                .andExpect(jsonPath("$.data.dev_rehab_voucher.notes.length()").value(1))
                .andExpect(jsonPath("$.data.dev_rehab_voucher.notes[0]").value("9세 미만은 등록 전이라도 의뢰서와 검사자료로 신청 가능"))
                .andExpect(jsonPath("$.data.dev_rehab_voucher.form_names.length()").value(2))
                .andExpect(jsonPath("$.data.dev_rehab_voucher.form_names", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("발달재활서비스 의뢰서"))))
                .andExpect(jsonPath("$.data.dev_rehab_voucher.documents", org.hamcrest.Matchers.hasItem("발달재활서비스 의뢰서")));
    }

    @Test void 네살_남은신청은_신청전5_확인2_공고1이고_센터신청서는5종이다() throws Exception {
        String response = judge(LATEST_CHILD).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        var data = tools.jackson.databind.json.JsonMapper.builder().build().readTree(response).path("data");
        int apply = 0, check = 0, provider = 0, already = 0, later = 0, center = 0;
        java.util.Set<String> forms = new java.util.HashSet<>();
        for (var item : data) {
            if (item.path("hidden").asBoolean()) continue;
            String state = item.path("status").asString();
            if ("already".equals(state)) already++;
            if ("not_eligible".equals(item.path("verdict").asString()) && item.path("eligible_from_age").isNumber()) later++;
            if (state == null || !java.util.Set.of("apply", "check", "extra_form").contains(state)) continue;
            String office = item.path("office").path("id").asString();
            if ("provider".equals(office) && "apply".equals(state)) provider++;
            else if ("apply".equals(state)) apply++;
            if ("check".equals(state)) check++;
            if ("dong_center".equals(office)) {
                center++;
                item.path("form_names").forEach(name -> forms.add(name.asString()));
            }
        }
        org.junit.jupiter.api.Assertions.assertEquals(java.util.List.of(5, 2, 1, 2, 4, 5, 5),
                java.util.List.of(apply, check, provider, already, later, center, forms.size()));
    }

    @Test void 미제공_표시정보는_null_필드를_유지한다() throws Exception {
        ResultActions result = judge(LATEST_CHILD).andExpect(status().isOk());
        for (String field : new String[]{"category", "short_desc", "description", "check_reason", "benefit", "conditions"}) {
            result.andExpect(jsonPath("$.data.child_allowance", org.hamcrest.Matchers.hasKey(field)))
                    .andExpect(jsonPath("$.data.child_allowance." + field).value(org.hamcrest.Matchers.nullValue()));
        }
    }

    @Test void 발달재활_목록금액은_상세금액과_같고_모르면_금액없는_설명이다() throws Exception {
        judge(LATEST_CHILD.replace("neither", "basic"))
                .andExpect(jsonPath("$.data.dev_rehab_voucher.short_desc").value("월 26만원"))
                .andExpect(jsonPath("$.data.dev_rehab_voucher.benefit.amount").value("월 26만원"));
        judge(LATEST_CHILD.replace("neither", "near_poor"))
                .andExpect(jsonPath("$.data.dev_rehab_voucher.short_desc").value("월 24만원"))
                .andExpect(jsonPath("$.data.dev_rehab_voucher.benefit.amount").value("월 24만원"));
        for (String band : new String[]{"le120", "unknown", "over180"}) {
            judge(LATEST_CHILD.replace("le180", band))
                    .andExpect(jsonPath("$.data.dev_rehab_voucher.short_desc").value("발달재활 치료비 이용권"))
                    .andExpect(jsonPath("$.data.dev_rehab_voucher.benefit").value(org.hamcrest.Matchers.nullValue()));
        }
    }

    @Test void 다른_체험인물의_대표서비스도_전체판정과_함께_반환한다() throws Exception {
        String adult = LATEST_CHILD.replace("[\"daycare\"]", "[]");
        judge(adult.replace("\"age\":4", "\"age\":35").replace("autism", "hearing"))
                .andExpect(jsonPath("$.data.activity_support.category").value("care"))
                .andExpect(jsonPath("$.data.activity_support.conditions[0].value").value("만 35세"))
                .andExpect(jsonPath("$.data.activity_support.conditions[0].met").value(true));
        judge(adult.replace("\"age\":4", "\"age\":24"))
                .andExpect(jsonPath("$.data.dd_day_activity.category").value("care"))
                .andExpect(jsonPath("$.data.dd_day_activity.benefit.amount").value("기본형 월 132시간, 확장형 월 176시간 바우처"));
        String senior = adult.replace("\"age\":4", "\"age\":72").replace("autism", "physical")
                .replace("le180", "le120").replace("\"tags\":[]", "\"tags\":[\"wheelchair\"]");
        judge(senior).andExpect(jsonPath("$.data.yongin_mobility_repair.benefit.amount").value("수리비의 50%"))
                .andExpect(jsonPath("$.data.pension_additional").exists());
        judge(senior.replace("neither", "basic"))
                .andExpect(jsonPath("$.data.yongin_mobility_repair.benefit.amount").value("수리비 전액"));
        judge(senior.replace("neither", "unknown"))
                .andExpect(jsonPath("$.data.yongin_mobility_repair.benefit").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test void 확인사유는_확인상태에서만_표시하고_재판정시_남지않는다() throws Exception {
        judge(LATEST_CHILD).andExpect(jsonPath("$.data.special_education.check_reason").value("진단평가"))
                .andExpect(jsonPath("$.data.disabled_childcare_fee.check_reason").value(org.hamcrest.Matchers.nullValue()));
        judge(LATEST_CHILD.replace("\"age\":4", "\"age\":6"))
                .andExpect(jsonPath("$.data.disabled_childcare_fee.check_reason").value("미취학 여부 확인"));
        judge(LATEST_CHILD.replace("\"age\":4", "\"age\":13"))
                .andExpect(jsonPath("$.data.disabled_childcare_fee.status").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.data.disabled_childcare_fee.check_reason").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test void 등록항목_누락은_등록서식_서비스만_확인필요로_남긴다() throws Exception {
        String body = LATEST_CHILD.replace("\"reg_items\":[\"fee_reductions_7\",\"parking_sticker\"],", "")
                .replace("neither", "basic").replace("\"age\":4", "\"age\":6");
        ResultActions result = judge(body).andExpect(status().isOk());
        for (String id : new String[]{"fee_reductions_7", "parking_sticker", "activity_support", "assistive_device", "dd_individual_plan"}) {
            result.andExpect(jsonPath("$.data." + id + ".status").value("check"));
        }
        result.andExpect(jsonPath("$.data.dev_rehab_voucher.status").value("apply"));
    }

}
