package com.likelionknu.dojang.meta.service;

import com.likelionknu.dojang.meta.data.dto.response.MetaResponseDto;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.LocalDate;

@Service
public class MetaService {
    private final MetaResponseDto meta;

    public MetaService(JsonMapper jsonMapper) throws IOException {
        LocalDate child = rulesUpdated(jsonMapper, "data/rules_child_v2.json");
        LocalDate adult = rulesUpdated(jsonMapper, "data/rules_adult_v2.json");
        LocalDate rulesUpdated = child.isAfter(adult) ? child : adult;
        // 기관 데이터와 복지 API 수집이 아직 없어서 null
        this.meta = new MetaResponseDto(rulesUpdated, null, null);
    }

    public MetaResponseDto getMeta() {
        return meta;
    }

    private LocalDate rulesUpdated(JsonMapper jsonMapper, String path) throws IOException {
        String updated = jsonMapper.readTree(new ClassPathResource(path).getInputStream()).path("meta").path("updated").asString();
        return LocalDate.parse(updated);
    }
}
