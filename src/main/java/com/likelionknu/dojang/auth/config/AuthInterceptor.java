package com.likelionknu.dojang.auth.config;

import com.likelionknu.dojang.auth.exception.AuthException;
import com.likelionknu.dojang.auth.service.JwtProvider;
import com.likelionknu.dojang.common.response.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/** Bearer 토큰을 확인하고 회원 id를 요청 속성 memberId로 넘긴다. */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {
    public static final String MEMBER_ID = "memberId";
    private static final String BEARER = "Bearer ";

    private final JwtProvider jwtProvider;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (CorsUtils.isPreFlightRequest(request)) {
            return true;
        }
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER)) {
            throw new AuthException(ErrorCode.UNAUTHORIZED);
        }
        request.setAttribute(MEMBER_ID, jwtProvider.parseMemberId(header.substring(BEARER.length())));
        return true;
    }
}
