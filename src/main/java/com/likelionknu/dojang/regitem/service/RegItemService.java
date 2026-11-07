package com.likelionknu.dojang.regitem.service;

import com.likelionknu.dojang.regitem.data.dto.response.RegItemResponseDto;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.List;

/** 장애인 등록 및 서비스 신청서(시행규칙 별지 제1호의4서식)의 서비스 항목. id는 규칙표 reg_form_item과 맞춘다. */
@Service
public class RegItemService {
    private final List<RegItemResponseDto> regItems;

    public RegItemService(JsonMapper jsonMapper) throws IOException {
        this.regItems = jsonMapper.readValue(new ClassPathResource("data/reg_items.json").getInputStream(), new TypeReference<>() {});
    }

    public List<RegItemResponseDto> getRegItems() {
        return regItems;
    }
}
