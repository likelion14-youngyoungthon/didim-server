package com.likelionknu.dojang.steps.data.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record ServiceStepsResponseDto(String serviceId, List<Step> steps) {
    public record Step(String key, String label,
            @JsonInclude(JsonInclude.Include.ALWAYS)
            @Schema(description = "신청 단계 명세에 따라 모든 단계에서 null을 반환합니다.", nullable = true)
            Integer days,
            @Schema(description = "다른 단계와 병행할 수 있는지 여부. 완료 상태가 아닙니다.")
            boolean parallel) {}
}
