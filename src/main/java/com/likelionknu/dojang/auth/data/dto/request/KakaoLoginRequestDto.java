package com.likelionknu.dojang.auth.data.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record KakaoLoginRequestDto(
        @Schema(description = "카카오가 redirect_uri로 넘겨준 인가 코드") @NotBlank String code,
        @Schema(description = "인가 요청 때 쓴 redirect_uri. 비우면 서버 기본값", example = "http://localhost:5173/oauth/kakao") String redirectUri) {
}
