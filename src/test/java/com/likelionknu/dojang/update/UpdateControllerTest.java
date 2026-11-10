package com.likelionknu.dojang.update;

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
class UpdateControllerTest {
    private static final String BODY = """
            {"text": "어제 발달재활 신청서 냈어요. 서류 더 내라고 연락 왔어요. 그리고 기흥동으로 이사했어요.",
             "services": [{"id": "dev_rehab_voucher", "status": "apply"}, {"id": "activity_support", "status": "apply"}]}
            """;

    @Autowired
    MockMvc mvc;

    @MockitoBean
    LlmClient llmClient;

    @Test
    void 확인된_변경만_남긴다() throws Exception {
        // 뒤의 네 개는 버려야 한다: 없는 상태 값, 카드에 없는 서비스, 원문에 없는 근거, 용인시 동이 아님
        given(llmClient.extractJson(anyString(), anyString(), anyString(), any())).willReturn(JsonMapper.builder().build().readTree("""
                {"changes": [
                  {"type": "status", "service_id": "dev_rehab_voucher", "to": "submitted", "evidence": "어제 발달재활 신청서 냈어요"},
                  {"type": "supplement", "service_id": "activity_support", "evidence": "서류 더 내라고 연락 왔어요"},
                  {"type": "fact", "key": "dong", "to": "기흥동", "evidence": "기흥동으로 이사했어요"},
                  {"type": "status", "service_id": "dev_rehab_voucher", "to": "done", "evidence": "신청서 냈어요"},
                  {"type": "status", "service_id": "pension_basic", "to": "submitted", "evidence": "신청서 냈어요"},
                  {"type": "status", "service_id": "dev_rehab_voucher", "to": "approved", "evidence": "승인됐어요"},
                  {"type": "fact", "key": "dong", "to": "강남동", "evidence": "이사했어요"}
                ]}
                """));

        mvc.perform(post("/api/v1/updates").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.changes.length()").value(3))
                .andExpect(jsonPath("$.data.changes[0].type").value("status"))
                .andExpect(jsonPath("$.data.changes[0].from").value("apply"))
                .andExpect(jsonPath("$.data.changes[0].to").value("submitted"))
                .andExpect(jsonPath("$.data.changes[0].needs_confirm").value(false))
                .andExpect(jsonPath("$.data.changes[0].person").doesNotExist())
                .andExpect(jsonPath("$.data.changes[1].reopen_step").value("prepare"))
                .andExpect(jsonPath("$.data.changes[2].key").value("dong"))
                .andExpect(jsonPath("$.data.changes[2].needs_confirm").value(true));
    }

    @Test
    void AI_실패는_E502() throws Exception {
        given(llmClient.extractJson(anyString(), anyString(), anyString(), any())).willThrow(new IllegalStateException());

        mvc.perform(post("/api/v1/updates").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("E502"));
    }

    @Test
    void 빈_글은_E400() throws Exception {
        mvc.perform(post("/api/v1/updates").contentType(MediaType.APPLICATION_JSON).content("{\"text\": \" \"}"))
                .andExpect(status().isBadRequest());
    }
}
