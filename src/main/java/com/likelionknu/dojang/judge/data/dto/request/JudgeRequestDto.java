package com.likelionknu.dojang.judge.data.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record JudgeRequestDto(
        @NotNull @Min(0) Integer age,
        @Schema(description = "장애 유형. AI가 확인하지 못한 경우 unknown 또는 null") String disabilityType,
        String severity, String dong, @Min(1) Integer household,
        String status, String incomeBand, List<String> regItems,
        Boolean regItemsUnknown, List<String> tags) {
}
