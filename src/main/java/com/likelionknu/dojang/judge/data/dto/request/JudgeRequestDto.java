package com.likelionknu.dojang.judge.data.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record JudgeRequestDto(
        @NotNull(message = "널이어서는 안됩니다") @Min(value = 0, message = "0 이상이어야 합니다") Integer age,
        @Schema(description = "장애 유형. AI가 확인하지 못한 경우 unknown 또는 null") String disabilityType,
        String severity, String dong, @Min(value = 1, message = "1 이상이어야 합니다") Integer household,
        String status, String incomeBand, List<String> regItems,
        Boolean regItemsUnknown, List<String> tags) {
}
