package com.likelionknu.dojang.meta.controller;

import com.likelionknu.dojang.common.response.GlobalResponse;
import com.likelionknu.dojang.meta.data.dto.response.MetaResponseDto;
import com.likelionknu.dojang.meta.service.MetaService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/meta")
@RequiredArgsConstructor
public class MetaController {
    private final MetaService metaService;

    @GetMapping
    @Operation(summary = "데이터 기준일")
    public GlobalResponse<MetaResponseDto> getMeta() {
        return GlobalResponse.ok(metaService.getMeta());
    }
}
