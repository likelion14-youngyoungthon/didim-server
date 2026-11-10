package com.likelionknu.dojang.analyze.data.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AnalyzeRequestDto(
        @Schema(example = "백암면") @NotBlank String dong,
        @Schema(description = "self(본인) 또는 guardian(보조 이용)", example = "guardian")
        @NotBlank @Pattern(regexp = "self|guardian", message = "self 또는 guardian이어야 합니다.") String actor,
        @Schema(example = "아이가 네 살이고 자폐 진단을 받아서 이번에 장애 등록했어요. 어린이집 다니고 있어요.")
        @NotBlank @Size(max = 2000, message = "2000자 이하로 입력해 주세요.") String text) {
}
