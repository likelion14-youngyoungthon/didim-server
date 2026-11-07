package com.likelionknu.dojang.dong;

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
class DongControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void 읍면동_39개를_돌려준다() throws Exception {
        mvc.perform(get("/api/v1/dongs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.length()").value(39))
                .andExpect(jsonPath("$.data[0].gu").value("처인구"))
                .andExpect(jsonPath("$.data[0].dong").value("포곡읍"));
    }
}
