package com.likelionknu.dojang.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "demo-login.enabled=false")
@AutoConfigureMockMvc
class DemoLoginDisabledTest {
    @Autowired MockMvc mvc;

    @Test
    void 꺼져_있으면_404() throws Exception {
        mvc.perform(post("/api/v1/auth/demo"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("E404"));
    }
}
