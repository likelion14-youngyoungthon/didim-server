package com.likelionknu.dojang.judge;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class JudgeControllerTest {
    @Autowired MockMvc mvc;

    private static final String CHILD = """
            {"age":4,"disability_type":"autism","severity":"severe","dong":"백암면",
             "household":4,"status":"neither","income_band":"le120",
             "reg_items":["fee_reductions_7","parking_sticker"],"reg_items_unknown":false,"tags":["daycare"]}
            """;

    private ResultActions judge(String body) throws Exception {
        return mvc.perform(post("/api/v1/judge").contentType(MediaType.APPLICATION_JSON).content(body));
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
}
