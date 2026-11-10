package com.likelionknu.dojang.analyze.controller;

import com.likelionknu.dojang.analyze.data.dto.request.AnalyzeRequestDto;
import com.likelionknu.dojang.analyze.data.dto.response.AnalyzeResponseDto;
import com.likelionknu.dojang.analyze.service.AnalyzeService;
import com.likelionknu.dojang.common.response.GlobalResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analyze")
@RequiredArgsConstructor
public class AnalyzeController {
    private final AnalyzeService analyzeService;

    @PostMapping
    @Operation(summary = "대화 정리 (사실 카드)", description = "AI 호출에 실패하면 E502")
    public GlobalResponse<AnalyzeResponseDto> analyze(@Valid @RequestBody AnalyzeRequestDto request) {
        return GlobalResponse.ok(analyzeService.analyze(request));
    }
}
