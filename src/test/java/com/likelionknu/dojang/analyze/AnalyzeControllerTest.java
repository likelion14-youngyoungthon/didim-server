package com.likelionknu.dojang.analyze;

import com.likelionknu.dojang.analyze.service.LlmClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AnalyzeControllerTest {
    private static final String BODY = """
            {"dong": "백암면", "actor": "guardian", "text": "아이가 네 살이고 자폐 진단을 받아서 이번에 장애 등록했어요. 어린이집 다니고 있어요."}
            """;

    @Autowired
    MockMvc mvc;

    @MockitoBean
    LlmClient llmClient;

    @Test
    void 원문에_있는_사실만_남기고_빠진_값을_되묻는다() throws Exception {
        // severity는 원문에 없는 근거, tags의 pet은 허용값 밖이라 버려야 한다
        given(llmClient.extractJson(anyString(), anyString(), anyString(), any())).willReturn(JsonMapper.builder().build().readTree("""
                {"facts": [
                  {"key": "age", "value": 4, "evidence": "아이가 네 살이고"},
                  {"key": "disability_type", "value": "autism", "evidence": "자폐 진단을 받아서"},
                  {"key": "severity", "value": "severe", "evidence": "심한 장애래요"},
                  {"key": "tags", "value": "daycare", "evidence": "어린이집 다니고 있어요"},
                  {"key": "tags", "value": "pet", "evidence": "어린이집"}
                ]}
                """));

        mvc.perform(post("/api/v1/analyze").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.facts[0].key").value("age"))
                .andExpect(jsonPath("$.data.facts[0].value").value(4))
                .andExpect(jsonPath("$.data.facts[1].value").value("autism"))
                .andExpect(jsonPath("$.data.facts[2].value").value("unknown"))
                .andExpect(jsonPath("$.data.facts[2].evidence").doesNotExist())
                .andExpect(jsonPath("$.data.facts[6].value[0]").value("daycare"))
                .andExpect(jsonPath("$.data.facts[6].value.length()").value(1))
                .andExpect(jsonPath("$.data.missing[0]").value("severity"))
                .andExpect(jsonPath("$.data.missing.length()").value(3))
                .andExpect(jsonPath("$.data.questions.length()").value(2))
                .andExpect(jsonPath("$.data.questions[0].key").value("severity"))
                .andExpect(jsonPath("$.data.questions[0].options[2].value").value("unknown"));
    }

    @Test
    void AI_실패는_E502() throws Exception {
        given(llmClient.extractJson(anyString(), anyString(), anyString(), any())).willThrow(new IllegalStateException());

        mvc.perform(post("/api/v1/analyze").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("E502"));
    }

    @Test
    void 빈_글은_E400() throws Exception {
        mvc.perform(post("/api/v1/analyze").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dong\": \"백암면\", \"actor\": \"guardian\", \"text\": \"\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/analyze").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dong\": \"백암면\", \"actor\": \"friend\", \"text\": \"안녕\"}"))
                .andExpect(status().isBadRequest());
    }
}
