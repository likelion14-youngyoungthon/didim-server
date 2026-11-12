package com.likelionknu.dojang.auth.service;

import com.likelionknu.dojang.auth.client.KakaoClient;
import com.likelionknu.dojang.auth.client.KakaoClient.KakaoProfile;
import com.likelionknu.dojang.auth.data.dto.request.KakaoLoginRequestDto;
import com.likelionknu.dojang.auth.data.dto.response.LoginResponseDto;
import com.likelionknu.dojang.auth.exception.AuthException;
import com.likelionknu.dojang.common.response.ErrorCode;
import com.likelionknu.dojang.member.entity.Member;
import com.likelionknu.dojang.member.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Service
public class AuthService {
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

    public LoginResponseDto loginWithKakao(KakaoLoginRequestDto request) {
        String redirectUri = request.redirectUri() == null || request.redirectUri().isBlank()
                ? defaultRedirectUri : request.redirectUri();
        KakaoProfile profile = kakaoClient.fetchProfile(request.code(), redirectUri);

        Member member = memberRepository.findByKakaoId(profile.id()).orElse(null);
        boolean isNew = false;
        if (member == null) {
            try {
                member = memberRepository.saveAndFlush(Member.join(profile.id()));
                isNew = true;
            } catch (DataIntegrityViolationException e) {
                // 같은 계정의 첫 로그인이 동시에 들어오면 먼저 저장된 회원을 쓴다
                member = memberRepository.findByKakaoId(profile.id()).orElseThrow();
            }
        }
        return issue(member, isNew, profile.nickname(), false);
    }

    /** 체험할 때마다 임시 회원을 새로 만들어 저장 내용이 섞이지 않게 한다. 카카오 회원번호는 양수라 음수로 구분한다. */
    public LoginResponseDto loginAsDemo() {
        if (!demoLoginEnabled) {
            throw new AuthException(ErrorCode.NOT_FOUND, "테스트 로그인이 꺼져 있습니다.");
        }
        long demoId = -ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE);
        return issue(memberRepository.save(Member.join(demoId)), false, null, true);
    }

    private LoginResponseDto issue(Member member, boolean isNew, String nickname, boolean isDemo) {
        return new LoginResponseDto(jwtProvider.issue(member.getId()), "Bearer", jwtProvider.expiresInSeconds(),
                isNew, nickname, isDemo);
    }
}
