package com.likelionknu.dojang.regitem;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RegItemControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void 등록_신청서_항목_8개를_돌려준다() throws Exception {
        mvc.perform(get("/api/v1/reg-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.length()").value(8))
                .andExpect(jsonPath("$.data[0].id").value("fee_reductions_7"))
                .andExpect(jsonPath("$.data[0].label").value("감면서비스 대행 7종"));
    }
}
