package com.likelionknu.dojang.auth.controller;

import com.likelionknu.dojang.auth.data.dto.request.KakaoLoginRequestDto;
import com.likelionknu.dojang.auth.data.dto.response.LoginResponseDto;
import com.likelionknu.dojang.auth.service.AuthService;
import com.likelionknu.dojang.common.response.GlobalResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/kakao")
    @Operation(summary = "카카오 로그인", description = "인가 코드를 받아 JWT를 발급합니다. 처음이면 회원을 만들고 is_new가 true입니다.")
    public GlobalResponse<LoginResponseDto> loginWithKakao(@Valid @RequestBody KakaoLoginRequestDto request) {
        return GlobalResponse.ok(authService.loginWithKakao(request));
    }

    @PostMapping("/demo")
    @Operation(summary = "테스트 로그인", description = "카카오 없이 공용 테스트 계정 토큰을 발급합니다. 저장 내용은 모든 테스트 사용자가 공유합니다. 서버 설정 DEMO_LOGIN_ENABLED가 false면 404입니다.")
    public GlobalResponse<LoginResponseDto> loginAsDemo() {
        return GlobalResponse.ok(authService.loginAsDemo());
    }
}
