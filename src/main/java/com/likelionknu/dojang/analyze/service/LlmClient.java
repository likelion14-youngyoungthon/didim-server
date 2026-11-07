package com.likelionknu.dojang.analyze.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

/**
 * OpenAI Chat Completions 호출. response_format json_schema로 정해진 형식의 JSON만 받는다.
 * 키는 환경변수 LLM_API_KEY, 모델은 LLM_MODEL(기본 gpt-5-mini).
 */
@Component
public class LlmClient {
    private static final String URL = "https://api.openai.com/v1/chat/completions";

    private final RestClient restClient;
    private final JsonMapper jsonMapper;
    private final String apiKey;
    private final String model;

    public LlmClient(JsonMapper jsonMapper, @Value("${LLM_API_KEY:}") String apiKey, @Value("${LLM_MODEL:gpt-5-mini}") String model) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(30_000);
        this.restClient = RestClient.builder().requestFactory(factory).build();
        this.jsonMapper = jsonMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    /** 실패하면 예외를 던진다. 호출하는 쪽에서 E502로 바꾼다 */
    public JsonNode extractJson(String system, String userText, String schemaName, Map<String, Object> schema) {
        if (apiKey.isBlank()) {
            throw new IllegalStateException("LLM_API_KEY가 설정되지 않음");
        }
        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", system),
                        Map.of("role", "user", "content", userText)),
                "response_format", Map.of(
                        "type", "json_schema",
                        "json_schema", Map.of("name", schemaName, "schema", schema)));

        JsonNode response = restClient.post().uri(URL)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        JsonNode message = response.path("choices").path(0).path("message");
        String content = message.path("content").asString("");
        if (content.isBlank()) {
            throw new IllegalStateException("LLM 응답이 비었거나 거절됨");
        }
        return jsonMapper.readTree(content);
    }
}
