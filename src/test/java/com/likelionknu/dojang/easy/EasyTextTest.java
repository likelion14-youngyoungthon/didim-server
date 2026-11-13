package com.likelionknu.dojang.easy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EasyTextTest {
    private static final String CHILD_4 = """
            {"age": 4, "disability_type": "autism", "severity": "severe", "dong": "백암면", "household": 3,
             "status": "neither", "income_band": "le180", "reg_items": ["fee_reductions_7", "parking_sticker"], "tags": ["daycare"]}
            """;

    @Autowired
    MockMvc mvc;

    @Test
    void 판정_결과에_쉬운_말이_붙는다() throws Exception {
        mvc.perform(post("/api/v1/judge").contentType(MediaType.APPLICATION_JSON).content(CHILD_4))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dev_rehab_voucher.easy.summary").isString())
                .andExpect(jsonPath("$.data.dev_rehab_voucher.easy.groups[0].title").value("무엇을 해요"))
                .andExpect(jsonPath("$.data.dev_rehab_voucher.easy.groups[0].items[0].icon").isString())
                .andExpect(jsonPath("$.data.dev_rehab_voucher.easy.steps").isArray())
                // 검수용 필드는 내보내지 않는다
                .andExpect(jsonPath("$.data.dev_rehab_voucher.easy.reviewed").doesNotExist())
                .andExpect(jsonPath("$.data.dev_rehab_voucher.easy.problems").doesNotExist())
                // 아직 만들지 않은 서비스는 null
                .andExpect(jsonPath("$.data.child_allowance.easy").isEmpty());
    }
}
