package com.likelionknu.dojang.stt;

import com.likelionknu.dojang.stt.service.SttClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.json.JsonMapper;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SttMultipartHttpTest {
    @LocalServerPort int port;
    @MockitoBean SttClient client;
    private static final byte[] WAV={'R','I','F','F',0,0,0,0,'W','A','V','E',1};
    private static final String BOUNDARY="stt-test-boundary";
    private final HttpClient http=HttpClient.newHttpClient();

    @BeforeEach void setup() { given(client.transcribe(any(byte[].class),anyString())).willReturn("음성 변환 결과"); }

    private HttpResponse<String> upload(byte[] audio,boolean withFilename) throws Exception {
        var bytes=new ByteArrayOutputStream();
        String disposition="Content-Disposition: form-data; name=\"audio\""+(withFilename?"; filename=\"voice.wav\"":"");
        bytes.write(("--"+BOUNDARY+"\r\n"+disposition+"\r\nContent-Type: audio/wav\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
        bytes.write(audio);
        bytes.write(("\r\n--"+BOUNDARY+"--\r\n").getBytes(StandardCharsets.US_ASCII));
        HttpRequest req=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v1/stt"))
                .header("Content-Type","multipart/form-data; boundary="+BOUNDARY)
                .header("Origin","http://localhost:5173")
                .timeout(Duration.ofSeconds(15)).POST(HttpRequest.BodyPublishers.ofByteArray(bytes.toByteArray())).build();
        return http.send(req,HttpResponse.BodyHandlers.ofString());
    }

    @Test void 실제_HTTP_multipart_성공() throws Exception {
        var response=upload(WAV,true);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(JsonMapper.builder().build().readTree(response.body()).path("data").path("text").asString())
                .isEqualTo("음성 변환 결과");
    }

    @Test void 허용되지_않은_출처는_변환을_호출하지_못한다() throws Exception {
        HttpRequest request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v1/stt"))
                .header("Origin","https://not-allowed.example")
                .header("Content-Type","multipart/form-data; boundary="+BOUNDARY)
                .POST(HttpRequest.BodyPublishers.noBody()).build();
        var response=http.send(request,HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(403);
        assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).isEmpty();
        verifyNoInteractions(client);
    }

    @Test void 서블릿이_거부하는_큰파일도_JSON_E400() throws Exception {
        var response=upload(java.util.Arrays.copyOf(WAV,10*1024*1024+1),true);
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.headers().firstValue("Access-Control-Allow-Origin")).contains("http://localhost:5173");
        assertThat(JsonMapper.builder().build().readTree(response.body()).path("code").asString()).isEqualTo("E400");
        verifyNoInteractions(client);
    }

    @Test void filename없는_일반파트는_파일누락_E400() throws Exception {
        var response=upload(WAV,false);
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(JsonMapper.builder().build().readTree(response.body()).path("code").asString()).isEqualTo("E400");
        verifyNoInteractions(client);
    }
}
