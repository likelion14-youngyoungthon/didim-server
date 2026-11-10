package com.likelionknu.dojang.judge.controller;

import com.likelionknu.dojang.common.response.GlobalResponse;
import com.likelionknu.dojang.judge.data.dto.request.JudgeRequestDto;
import com.likelionknu.dojang.judge.data.dto.response.JudgeResponseDto;
import com.likelionknu.dojang.judge.service.JudgeService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/judge")
@RequiredArgsConstructor
public class JudgeController {
    private final JudgeService judgeService;

    @PostMapping
    @Operation(summary = "남은 신청 판정", description = "규칙표 기반 지원 후보 안내. 정보가 부족하면 확인 필요로 반환하며 수급 자격 확정이 아닙니다.")
    public GlobalResponse<Map<String, JudgeResponseDto>> judge(@Valid @RequestBody JudgeRequestDto request) {
        return GlobalResponse.ok(judgeService.judge(request));
    }
}
