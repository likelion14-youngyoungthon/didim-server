package com.likelionknu.dojang.office.controller;

import com.likelionknu.dojang.common.response.GlobalResponse;
import com.likelionknu.dojang.office.data.dto.response.OfficeResponseDto;
import com.likelionknu.dojang.office.service.OfficeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/offices")
@RequiredArgsConstructor
public class OfficeController {
    private final OfficeService officeService;

    @GetMapping
    @Operation(summary = "동별 접수처 목록")
    public GlobalResponse<List<OfficeResponseDto>> getOffices(
            @Parameter(description = "읍면동", example = "백암면") @RequestParam String dong,
            @Parameter(description = "서비스 id (규칙표)", example = "dev_rehab_voucher") @RequestParam(required = false) String service,
            @Parameter(description = "접수처 또는 이용기관") @RequestParam(required = false) String kind) {
        return GlobalResponse.ok(officeService.getOffices(dong, service, kind));
    }
}
