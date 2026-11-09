package com.likelionknu.dojang.meta;

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
class MetaControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void 데이터_기준일을_돌려준다() throws Exception {
        mvc.perform(get("/api/v1/meta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.rules_updated").value("2026-11-02"))
                .andExpect(jsonPath("$.data.offices_updated").value("2026-11-07"))
                .andExpect(jsonPath("$.data.welfare_api_fetched").isEmpty());
    }
}
