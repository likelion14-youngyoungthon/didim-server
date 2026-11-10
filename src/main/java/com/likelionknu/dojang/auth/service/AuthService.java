package com.likelionknu.dojang.auth.service;

import com.likelionknu.dojang.auth.client.KakaoClient;
import com.likelionknu.dojang.auth.data.dto.request.KakaoLoginRequestDto;
import com.likelionknu.dojang.auth.data.dto.response.LoginResponseDto;
import com.likelionknu.dojang.auth.exception.AuthException;
import com.likelionknu.dojang.common.response.ErrorCode;
import com.likelionknu.dojang.member.entity.Member;
import com.likelionknu.dojang.member.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthService {
    // 카카오 회원번호는 양수라 겹치지 않는다
    private static final long DEMO_KAKAO_ID = -1L;

    private final KakaoClient kakaoClient;
    private final JwtProvider jwtProvider;
    private final MemberRepository memberRepository;
    private final String defaultRedirectUri;
    private final boolean demoLoginEnabled;

    public AuthService(KakaoClient kakaoClient, JwtProvider jwtProvider, MemberRepository memberRepository,
                       @Value("${kakao.redirect-uri}") String defaultRedirectUri,
                       @Value("${demo-login.enabled}") boolean demoLoginEnabled) {
        this.kakaoClient = kakaoClient;
        this.jwtProvider = jwtProvider;
        this.memberRepository = memberRepository;
        this.defaultRedirectUri = defaultRedirectUri;
        this.demoLoginEnabled = demoLoginEnabled;
    }

    @Transactional
    public LoginResponseDto loginWithKakao(KakaoLoginRequestDto request) {
        String redirectUri = request.redirectUri() == null || request.redirectUri().isBlank()
                ? defaultRedirectUri : request.redirectUri();
        return issue(kakaoClient.fetchKakaoId(request.code(), redirectUri));
    }

    /** 팀 테스트·시연용 공용 계정. 모두 같은 계정을 쓰므로 저장 내용도 공유된다. */
    @Transactional
    public LoginResponseDto loginAsDemo() {
        if (!demoLoginEnabled) {
            throw new AuthException(ErrorCode.NOT_FOUND, "테스트 로그인이 꺼져 있습니다.");
        }
        return issue(DEMO_KAKAO_ID);
    }

    private LoginResponseDto issue(long kakaoId) {
        Optional<Member> found = memberRepository.findByKakaoId(kakaoId);
        Member member = found.orElseGet(() -> memberRepository.save(Member.join(kakaoId)));
        return new LoginResponseDto(jwtProvider.issue(member.getId()), "Bearer", jwtProvider.expiresInSeconds(), found.isEmpty());
    }
}
