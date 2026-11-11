package com.likelionknu.dojang.stt;

import com.likelionknu.dojang.stt.service.SttClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SttControllerTest {
    private static final byte[] WAV = {'R','I','F','F',0,0,0,0,'W','A','V','E',1};
    private static final byte[] WEBM = {0x1a,0x45,(byte)0xdf,(byte)0xa3,0};
    @Autowired MockMvc mvc;
    @MockitoBean SttClient client;

    @Test void 명세의_audio_필드와_text_응답() throws Exception {
        given(client.transcribe(any(byte[].class),eq("recording.wav"))).willReturn("아이가 네 살이고 백암면에 살아요.");
        mvc.perform(multipart("/api/v1/stt").file(new MockMultipartFile("audio","record.wav","audio/wav",WAV)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.text").value("아이가 네 살이고 백암면에 살아요."))
                .andExpect(jsonPath("$.message").isEmpty());
        verify(client).transcribe(WAV,"recording.wav");
    }

    @Test void 브라우저_blob_파일명과_코덱포함_MIME() throws Exception {
        given(client.transcribe(any(byte[].class),eq("recording.webm"))).willReturn("이동 지원을 알아보고 싶어요.");
        mvc.perform(multipart("/api/v1/stt").file(new MockMultipartFile("audio","blob","audio/webm;codecs=opus",WEBM)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.text").isString());
        verify(client).transcribe(WEBM,"recording.webm");
    }

    @Test void 누락_다른필드명_빈파일은_E400() throws Exception {
        mvc.perform(multipart("/api/v1/stt")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E400"));
        mvc.perform(multipart("/api/v1/stt").file(new MockMultipartFile("file","record.wav","audio/wav",WAV)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E400"));
        mvc.perform(multipart("/api/v1/stt").file(new MockMultipartFile("audio","empty.wav","audio/wav",new byte[0])))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(client);
    }

    @Test void 파일_헤더_MIME_확장자를_검증한다() throws Exception {
        for (MockMultipartFile file : new MockMultipartFile[]{
                new MockMultipartFile("audio","bad.wav","audio/wav","not audio".getBytes()),
                new MockMultipartFile("audio","record.wav","text/plain",WAV),
                new MockMultipartFile("audio","wrong.webm","audio/webm",WAV),
                new MockMultipartFile("audio","wrong.mp3","application/octet-stream",WAV)}) {
            mvc.perform(multipart("/api/v1/stt").file(file))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E400"));
        }
        verifyNoInteractions(client);
    }

    @Test void 파일_10MiB_경계() throws Exception {
        byte[] accepted = java.util.Arrays.copyOf(WAV,10*1024*1024);
        given(client.transcribe(any(byte[].class),anyString())).willReturn("경계 파일");
        mvc.perform(multipart("/api/v1/stt").file(new MockMultipartFile("audio","boundary.wav","audio/wav",accepted)))
                .andExpect(status().isOk());
        clearInvocations(client);
        byte[] rejected = java.util.Arrays.copyOf(WAV,10*1024*1024+1);
        mvc.perform(multipart("/api/v1/stt").file(new MockMultipartFile("audio","large.wav","audio/wav",rejected)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E400"));
        verifyNoInteractions(client);
    }

    @Test void 공급자_실패는_상세내용없이_E502() throws Exception {
        given(client.transcribe(any(byte[].class),anyString())).willThrow(new IllegalStateException("DO_NOT_LEAK_PROVIDER_DETAILS"));
        mvc.perform(multipart("/api/v1/stt").file(new MockMultipartFile("audio","voice.wav","audio/wav",WAV)))
                .andExpect(status().isBadGateway()).andExpect(jsonPath("$.code").value("E502"))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("DO_NOT_LEAK"))));
    }

    @Test void 잘못된_요청형식과_메서드() throws Exception {
        mvc.perform(post("/api/v1/stt").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E400"));
        mvc.perform(get("/api/v1/stt")).andExpect(status().isMethodNotAllowed());
    }
}
