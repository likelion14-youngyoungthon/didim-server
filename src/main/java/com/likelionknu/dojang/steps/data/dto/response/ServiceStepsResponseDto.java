package com.likelionknu.dojang.steps.data.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record ServiceStepsResponseDto(String serviceId, List<Step> steps) {
    public record Step(String key, String label,
            @JsonInclude(JsonInclude.Include.ALWAYS)
            @Schema(description = "규칙표의 전체 처리기간. review 단계만 표시하며 심사만의 소요기간이나 확정 기한이 아닙니다.", nullable = true)
            Integer days,
            @Schema(description = "다른 단계와 병행할 수 있는지 여부. 완료 상태가 아닙니다.")
            boolean parallel) {}
}
