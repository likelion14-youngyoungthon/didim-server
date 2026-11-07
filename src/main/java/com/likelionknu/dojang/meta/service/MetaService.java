package com.likelionknu.dojang.meta.service;

import com.likelionknu.dojang.meta.data.dto.response.MetaResponseDto;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
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
        LocalDate officesUpdated = LocalDate.parse(readTree(jsonMapper, "data/offices.json").path("checked_at").asString());
        // 복지 API 수집은 아직 없어서 null
        this.meta = new MetaResponseDto(rulesUpdated, officesUpdated, null);
    }

    public MetaResponseDto getMeta() {
        return meta;
    }

    private LocalDate rulesUpdated(JsonMapper jsonMapper, String path) throws IOException {
        return LocalDate.parse(readTree(jsonMapper, path).path("meta").path("updated").asString());
    }

    private JsonNode readTree(JsonMapper jsonMapper, String path) throws IOException {
        return jsonMapper.readTree(new ClassPathResource(path).getInputStream());
    }
}
