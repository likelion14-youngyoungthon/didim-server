package com.likelionknu.dojang.income;

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
class IncomeBandControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void 사인_가구_금액() throws Exception {
        mvc.perform(get("/api/v1/income-bands").param("household", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.year").value(2026))
                .andExpect(jsonPath("$.data.household").value(4))
                .andExpect(jsonPath("$.data.bands.length()").value(4))
                .andExpect(jsonPath("$.data.bands[0].id").value("le120"))
                .andExpect(jsonPath("$.data.bands[0].max_won").value(7793685))
                .andExpect(jsonPath("$.data.bands[0].label").value("월 7,793,685원 이하"))
                .andExpect(jsonPath("$.data.bands[1].max_won").value(11690528))
                .andExpect(jsonPath("$.data.bands[2].max_won").isEmpty())
                .andExpect(jsonPath("$.data.checked_at").value("2026-11-07"));
    }

    @Test
    void 팔인_이상은_칠인에_일인당_금액을_더한다() throws Exception {
        mvc.perform(get("/api/v1/income-bands").param("household", "8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bands[0].max_won").value(12569217));
    }

    @Test
    void 가구원_수가_잘못되면_E400() throws Exception {
        mvc.perform(get("/api/v1/income-bands").param("household", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E400"))
                .andExpect(jsonPath("$.message").value("household: 1 이상이어야 합니다."));
        mvc.perform(get("/api/v1/income-bands").param("household", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E400"));
        mvc.perform(get("/api/v1/income-bands"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E400"));
    }
}
