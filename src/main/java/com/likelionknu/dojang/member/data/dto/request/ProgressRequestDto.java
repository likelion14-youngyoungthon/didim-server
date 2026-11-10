package com.likelionknu.dojang.member.data.dto.request;

import com.likelionknu.dojang.judge.data.dto.request.JudgeRequestDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 화면의 지금 상태를 통째로 덮어쓴다. */
public record ProgressRequestDto(
        @Schema(description = "/judge에 보낸 요청 그대로. 아직 없으면 null") JudgeRequestDto profile,
        @Schema(description = "카드 목록. /judge 결과 또는 /updates로 바뀐 상태") @NotNull @Size(max = 100) List<@Valid ServiceStateDto> services) {

    public record ServiceStateDto(
            @Schema(example = "dev_rehab_voucher") @NotBlank @Pattern(regexp = "[a-z0-9_]+", message = "허용값이 아닙니다.") String id,
            @Schema(example = "apply") @Pattern(regexp = "[a-z0-9_]+", message = "허용값이 아닙니다.") String status) {
    }
}
