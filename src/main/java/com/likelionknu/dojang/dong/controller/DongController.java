package com.likelionknu.dojang.dong.controller;

import com.likelionknu.dojang.common.response.GlobalResponse;
import com.likelionknu.dojang.dong.data.dto.response.DongResponseDto;
import com.likelionknu.dojang.dong.service.DongService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dongs")
@RequiredArgsConstructor
public class DongController {
    private final DongService dongService;

    @GetMapping
    @Operation(summary = "용인시 읍면동 목록")
    public GlobalResponse<List<DongResponseDto>> getDongs() {
        return GlobalResponse.ok(dongService.getDongs());
    }
}
