package com.likelionknu.dojang.stt.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class SttClientTest {
    private MockRestServiceServer server;
    private RestClient restClient;
    private SttClient client;
    private static final String ENDPOINT="https://api.openai.com/v1/audio/transcriptions";

    @BeforeEach void setup() {
        RestClient.Builder builder=RestClient.builder();
        server=MockRestServiceServer.bindTo(builder).build();
        restClient=builder.build();
        client=new SttClient(restClient,"test-key-only","gpt-transcribe");
    }

    @Test void 파일과_모델_한국어옵션을_multipart로_전송한다() {
        server.expect(requestTo(ENDPOINT)).andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization","Bearer test-key-only"))
                .andExpect(header("Content-Type",containsString("multipart/form-data")))
                .andExpect(content().string(containsString("name=\"file\"; filename=\"recording.wav\"")))
                .andExpect(content().string(containsString("gpt-transcribe")))
                .andExpect(content().string(containsString("name=\"language\"")))
                .andExpect(content().string(containsString("ko")))
                .andRespond(withSuccess("{\"text\":\"백암면에 살아요.\"}",MediaType.APPLICATION_JSON));
        assertThat(client.transcribe(new byte[]{1,2,3},"recording.wav")).isEqualTo("백암면에 살아요.");
        server.verify();
    }

    @Test void 누락_빈값_타입오류_깨진응답을_실패로_취급한다() {
        for(String body:new String[]{"{}","{\"text\":\" \"}","{\"text\":3}","not-json"}) {
            setup();
            server.expect(requestTo(ENDPOINT)).andRespond(withSuccess(body,MediaType.APPLICATION_JSON));
            assertThatThrownBy(()->client.transcribe(new byte[]{1},"recording.wav")).isInstanceOf(RuntimeException.class);
            server.verify();
        }
    }

    @Test void 외부오류와_타임아웃은_호출자에게_실패로_전달한다() {
        server.expect(requestTo(ENDPOINT)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        assertThatThrownBy(()->client.transcribe(new byte[]{1},"recording.wav")).isInstanceOf(RuntimeException.class);
        server.verify();
        setup();
        server.expect(requestTo(ENDPOINT)).andRespond(withException(new java.net.SocketTimeoutException("test timeout")));
        assertThatThrownBy(()->client.transcribe(new byte[]{1},"recording.wav")).isInstanceOf(RuntimeException.class);
        server.verify();
    }

    @Test void 키가_없으면_외부호출하지_않는다() {
        SttClient missing=new SttClient(restClient,"","gpt-transcribe");
        assertThatThrownBy(()->missing.transcribe(new byte[]{1},"recording.wav")).isInstanceOf(IllegalStateException.class);
        server.verify();
    }

    @Test void 전용키를_우선하고_공백전용키는_기존키를_사용한다() {
        for (String sttKey : new String[]{"stt-test-key", " "}) {
            setup();
            String expected = sttKey.isBlank() ? "llm-test-key" : sttKey;
            var configured = new SttClient(restClient, sttKey, "llm-test-key", " ");
            server.expect(requestTo(ENDPOINT))
                    .andExpect(header("Authorization", "Bearer " + expected))
                    .andExpect(content().string(containsString("gpt-transcribe")))
                    .andRespond(withSuccess("{\"text\":\"설정 확인\"}", MediaType.APPLICATION_JSON));
            assertThat(configured.transcribe(new byte[]{1}, "recording.wav")).isEqualTo("설정 확인");
            server.verify();
        }
    }
}
