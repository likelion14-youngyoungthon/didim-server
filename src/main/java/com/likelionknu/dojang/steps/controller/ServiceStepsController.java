package com.likelionknu.dojang.steps.controller;

import com.likelionknu.dojang.common.response.GlobalResponse;
import com.likelionknu.dojang.steps.data.dto.response.ServiceStepsResponseDto;
import com.likelionknu.dojang.steps.service.ServiceStepsService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/services")
@RequiredArgsConstructor
public class ServiceStepsController {
    private final ServiceStepsService serviceStepsService;

    @GetMapping("/{id}/steps")
    @Operation(summary = "서비스 신청 단계 조회", description = "서류 준비·신청 경로·자격 심사·결과 안내의 기본 흐름입니다. "
            + "실제 상세 조사, 추가 서식, 연장 가능 여부는 해당 기관에 확인해야 합니다. "
            + "review.days는 규칙표의 전체 처리기간이며 심사만의 소요기간이나 확정 기한이 아닙니다. "
            + "null은 기간 미확인입니다. parallel은 병행 가능 여부이며 완료 상태가 아닙니다. "
            + "자동·직권 적용 서비스는 별도 신청·적용 경로 확인이 필요합니다.")
    public GlobalResponse<ServiceStepsResponseDto> getSteps(@PathVariable String id) {
        return GlobalResponse.ok(serviceStepsService.getSteps(id));
    }
}
