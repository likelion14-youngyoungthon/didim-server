package com.likelionknu.dojang.dong.service;

import com.likelionknu.dojang.dong.data.dto.response.DongResponseDto;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.List;

/** 용인시 읍면동 목록. 출처: 용인시 등록장애인 현황(2025-12-31) CSV의 행정지역. 양지면은 2026-01-02 양지읍으로 승격. */
@Service
public class DongService {
    private final List<DongResponseDto> dongs;

    public DongService(JsonMapper jsonMapper) throws IOException {
        this.dongs = jsonMapper.readValue(new ClassPathResource("data/dongs.json").getInputStream(), new TypeReference<>() {});
    }

    public List<DongResponseDto> getDongs() {
        return dongs;
    }
}
