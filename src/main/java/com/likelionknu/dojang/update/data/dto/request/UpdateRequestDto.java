package com.likelionknu.dojang.update.data.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateRequestDto(
        @Schema(example = "어제 발달재활 신청서 냈어요")
        @NotBlank @Size(max = 1000, message = "1000자 이하로 입력해 주세요.") String text,
        @Schema(description = "지금 화면의 카드 목록(/judge 결과의 id와 status 또는 바뀐 상태). 서버는 저장하지 않으므로 보내면 from을 채우고 이 서비스 안에서만 찾는다")
        List<@Valid ServiceStateDto> services) {

    public record ServiceStateDto(
            @Schema(example = "dev_rehab_voucher") @NotBlank @Pattern(regexp = "[a-z0-9_]+", message = "허용값이 아닙니다.") String id,
            @Schema(example = "apply") @Pattern(regexp = "[a-z0-9_]+", message = "허용값이 아닙니다.") String status) {
    }
}
