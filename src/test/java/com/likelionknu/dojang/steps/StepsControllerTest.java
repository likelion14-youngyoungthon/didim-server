package com.likelionknu.dojang.steps;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class StepsControllerTest {
    @Autowired MockMvc mvc;

    private static String path(String id) { return "/api/v1/services/" + id + "/steps"; }

    @Test void 발달재활_네단계_계약과_미확인_처리일() throws Exception {
        mvc.perform(get(path("dev_rehab_voucher")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.message").isEmpty())
                .andExpect(jsonPath("$.data.service_id").value("dev_rehab_voucher"))
                .andExpect(jsonPath("$.data.steps.length()").value(4))
                .andExpect(jsonPath("$.data.steps[0].key").value("prepare"))
                .andExpect(jsonPath("$.data.steps[0].label").value("서류 준비"))
                .andExpect(jsonPath("$.data.steps[0].parallel").value(true))
                .andExpect(jsonPath("$.data.steps[1].key").value("submit"))
                .andExpect(jsonPath("$.data.steps[1].label").value("행정복지센터 신청"))
                .andExpect(jsonPath("$.data.steps[1].parallel").value(false))
                .andExpect(jsonPath("$.data.steps[2].key").value("review"))
                .andExpect(jsonPath("$.data.steps[2].label").value("자격 심사"))
                .andExpect(jsonPath("$.data.steps[2]").value(org.hamcrest.Matchers.hasKey("days")))
                .andExpect(jsonPath("$.data.steps[2].days").isEmpty())
                .andExpect(jsonPath("$.data.steps[2].parallel").value(false))
                .andExpect(jsonPath("$.data.steps[3].key").value("result"))
                .andExpect(jsonPath("$.data.steps[3].label").value("결과 안내"))
                .andExpect(jsonPath("$.data.steps[3].parallel").value(false));
    }

    @Test void 규칙표에_있는_처리일만_반환한다() throws Exception {
        for (String id : new String[]{"activity_support", "pension_basic"}) {
            mvc.perform(get(path(id)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.steps[2].days").value(30))
                    .andExpect(jsonPath("$.data.steps[0].days").doesNotExist())
                    .andExpect(jsonPath("$.data.steps[1].days").doesNotExist())
                    .andExpect(jsonPath("$.data.steps[3].days").doesNotExist());
        }
        mvc.perform(get(path("special_education")))
                .andExpect(jsonPath("$.data.steps[2].days").value(44));
    }

    @Test void 주민센터가_아닌_실제_접수처를_반환한다() throws Exception {
        mvc.perform(get(path("employment_package")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.steps[1].label").value("한국장애인고용공단 신청"));
        mvc.perform(get(path("special_education")))
                .andExpect(jsonPath("$.data.steps[1].label").value("용인교육지원청 신청"));
    }

    @Test void 자동과_직권_서비스는_직접신청을_단정하지_않는다() throws Exception {
        for (String id : new String[]{"pension_additional", "child_allowance", "health_insurance_reduction", "transport_discounts"}) {
            mvc.perform(get(path(id)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.steps[1].label").value("별도 신청·적용 경로 확인"));
        }
    }

    @Test void 장소_데이터가_없는_서비스는_확인_안내() throws Exception {
        mvc.perform(get(path("dd_family_rest")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.steps[1].label").value(org.hamcrest.Matchers.containsString("확인")));
    }

    @Test void 없는_서비스는_E404() throws Exception {
        mvc.perform(get(path("missing_service")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("E404"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test void 조회_전용이며_다시_조회해도_진행상태가_생기지_않는다() throws Exception {
        mvc.perform(post(path("dev_rehab_voucher")))
                .andExpect(status().isMethodNotAllowed()).andExpect(jsonPath("$.code").value("E405"));
        for (int i = 0; i < 2; i++) {
            mvc.perform(get(path("dev_rehab_voucher")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").doesNotExist())
                    .andExpect(jsonPath("$.data.steps[0].completed").doesNotExist());
        }
    }
}
