package com.likelionknu.dojang.judge.controller;

import com.likelionknu.dojang.common.response.GlobalResponse;
import com.likelionknu.dojang.judge.data.dto.response.ServiceListResponseDto;
import com.likelionknu.dojang.judge.service.JudgeService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/services")
@RequiredArgsConstructor
public class ServiceListController {
    private final JudgeService judgeService;

    @GetMapping
    @Operation(summary = "따로 신청한 복지 목록", description = "나이에 맞는 규칙표에서 등록 신청서 항목을 제외한 복지 목록")
    public GlobalResponse<List<ServiceListResponseDto>> getServices(@RequestParam Integer age) {
        return GlobalResponse.ok(judgeService.listServices(age));
    }
}
