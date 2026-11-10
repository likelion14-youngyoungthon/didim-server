package com.likelionknu.dojang.auth.data.dto.response;

public record LoginResponseDto(String accessToken, String tokenType, long expiresIn, boolean isNew) {
}
