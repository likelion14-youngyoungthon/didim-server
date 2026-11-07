package com.likelionknu.dojang;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class DojangApplicationTests {

    @Autowired
    MockMvc mvc;

    @Test
    void 성공_응답은_code_OK() throws Exception {
        mvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data").value("ok"))
                .andExpect(jsonPath("$.message").isEmpty());
    }

    @Test
    void 실패_응답은_E_상태코드() throws Exception {
        mvc.perform(get("/api/v1/nothing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("E404"))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void 지원하지_않는_메서드는_E405() throws Exception {
        mvc.perform(post("/api/v1/dongs"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("E405"));
    }
}
