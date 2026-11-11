package com.likelionknu.dojang.stt.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import java.time.Duration;

@Component
public class SttClient {
    private static final String ENDPOINT = "https://api.openai.com/v1/audio/transcriptions";
    private final RestClient client;
    private final String key;
    private final String model;

    @Autowired
    public SttClient(
            @Value("${STT_API_KEY:}") String sttKey,
            @Value("${LLM_API_KEY:}") String llmKey,
            @Value("${STT_MODEL:gpt-transcribe}") String model) {
        this(createClient(), sttKey, llmKey, model);
    }

    private static RestClient createClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(60));
        return RestClient.builder().requestFactory(factory).build();
    }

    SttClient(RestClient client, String key, String model) {
        this(client, key, "", model);
    }

    SttClient(RestClient client, String sttKey, String llmKey, String model) {
        this.client = client;
        this.key = sttKey == null || sttKey.isBlank() ? llmKey : sttKey;
        this.model = model == null || model.isBlank() ? "gpt-transcribe" : model;
    }

    public String transcribe(byte[] audio, String filename) {
        if (key == null || key.isBlank()) throw new IllegalStateException("음성 변환을 사용할 수 없습니다.");
        var body = new LinkedMultiValueMap<String, Object>();
        body.add("file", new ByteArrayResource(audio) {
            @Override public String getFilename() { return filename; }
        });
        body.add("model", model);
        body.add("language", "ko");
        body.add("response_format", "json");
        JsonNode response = client.post().uri(ENDPOINT)
                .headers(headers -> headers.setBearerAuth(key))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body).retrieve().body(JsonNode.class);
        if (response == null || !response.path("text").isString() || response.path("text").asString().isBlank()) {
            throw new IllegalStateException("음성 변환 응답을 확인할 수 없습니다.");
        }
        return response.path("text").asString();
    }
}
