package com.likelionknu.dojang.income.controller;

import com.likelionknu.dojang.common.response.GlobalResponse;
import com.likelionknu.dojang.income.data.dto.response.IncomeBandsResponseDto;
import com.likelionknu.dojang.income.service.IncomeBandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/income-bands")
@RequiredArgsConstructor
public class IncomeBandController {
    private final IncomeBandService incomeBandService;

    @GetMapping
    @Operation(summary = "가구원 수별 소득 구간 금액")
    public GlobalResponse<IncomeBandsResponseDto> getIncomeBands(
            @Parameter(description = "가구원 수", example = "4") @RequestParam @Min(value = 1, message = "1 이상이어야 합니다.") int household) {
        return GlobalResponse.ok(incomeBandService.getIncomeBands(household));
    }
}
