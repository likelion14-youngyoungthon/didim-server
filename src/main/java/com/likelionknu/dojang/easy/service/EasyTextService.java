package com.likelionknu.dojang.easy.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 쉬운 말 문장(data/easy_texts.json). 요청마다 AI를 부르지 않고 미리 만든 문장을 판정 결과에 붙인다.
 * 파일은 scripts/make_easy_texts.py가 만들고 사람이 검수한다. 검수용 필드(reviewed, problems)는 응답에서 뺀다.
 */
@Service
public class EasyTextService {
    private final Map<String, JsonNode> texts = new HashMap<>();

    public EasyTextService(JsonMapper jsonMapper) throws IOException {
        ClassPathResource file = new ClassPathResource("data/easy_texts.json");
        if (!file.exists()) {
            return;
        }
        jsonMapper.readTree(file.getInputStream()).path("services").properties().forEach(entry -> {
            ObjectNode easy = ((ObjectNode) entry.getValue()).deepCopy();
            easy.remove("reviewed");
            easy.remove("problems");
            texts.put(entry.getKey(), easy);
        });
    }

    /** 없으면 null (아직 만들지 않은 서비스) */
    public JsonNode get(String serviceId) {
        return texts.get(serviceId);
    }
}
