package com.likelionknu.dojang.basis.service;

import com.likelionknu.dojang.judge.data.dto.request.JudgeRequestDto;
import com.likelionknu.dojang.judge.data.dto.response.JudgeResponseDto;
import com.likelionknu.dojang.judge.service.JudgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 운영 서버가 뜨면 체험 인물 4명(data/demo_personas.json)의 카드 근거를 미리 만들어 캐시에 넣는다.
 * 시연에서 근거를 처음 열 때 AI를 기다리지 않게 하려는 것. 서버 시작은 막지 않고 뒤에서 돈다.
 */
@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class BasisWarmup {
    private final JudgeService judgeService;
    private final BasisService basisService;
    private final JsonMapper jsonMapper;

    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() throws IOException {
        JsonNode personas = jsonMapper.readTree(new ClassPathResource("data/demo_personas.json").getInputStream()).path("personas");
        // ponytail: 동시 4개. OpenAI 속도 제한에 걸리면 줄인다
        ExecutorService pool = Executors.newFixedThreadPool(4);
        for (JsonNode persona : personas) {
            JudgeRequestDto request = jsonMapper.treeToValue(persona.path("request"), JudgeRequestDto.class);
            Map<String, Object> facts = facts(request);
            for (JudgeResponseDto service : judgeService.judge(request).values()) {
                if (service.hidden() || "not_eligible".equals(service.verdict())) {
                    continue;
                }
                pool.submit(() -> {
                    try {
                        basisService.basis(service.id(), facts);
                    } catch (RuntimeException e) {
                        log.warn("근거 미리 만들기 실패: {} {}", service.id(), e.getClass().getSimpleName());
                    }
                });
            }
        }
        pool.shutdown();
    }

    // BasisController가 만드는 값과 같은 모양이어야 캐시가 맞는다
    private static Map<String, Object> facts(JudgeRequestDto request) {
        Map<String, Object> facts = new LinkedHashMap<>();
        putIfPresent(facts, "age", request.age());
        putIfPresent(facts, "disability_type", request.disabilityType());
        putIfPresent(facts, "severity", request.severity());
        putIfPresent(facts, "status", request.status());
        putIfPresent(facts, "income_band", request.incomeBand());
        if (request.tags() != null && !request.tags().isEmpty()) {
            facts.put("tags", new ArrayList<>(request.tags()));
        }
        return facts;
    }

    private static void putIfPresent(Map<String, Object> facts, String key, Object value) {
        if (value != null) {
            facts.put(key, value);
        }
    }
}
