package com.likelionknu.dojang.auth.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.likelionknu.dojang.auth.exception.AuthException;
import com.likelionknu.dojang.common.response.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;

/** 인가 코드로 카카오 회원번호만 받는다. 동의항목(닉네임, 이메일 등)은 요청하지 않는다. */
@Slf4j
@Component
public class KakaoClient {
    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USER_URL = "https://kapi.kakao.com/v2/user/me";

    private final RestClient restClient;
    private final String restKey;
    private final String clientSecret;

    public KakaoClient(@Value("${kakao.rest-key}") String restKey, @Value("${kakao.client-secret}") String clientSecret) {
        this.restKey = restKey;
        this.clientSecret = clientSecret;
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    public long fetchKakaoId(String code, String redirectUri) {
        if (restKey.isBlank()) {
            log.error("KAKAO_REST_KEY가 설정되지 않았습니다.");
            throw new AuthException(ErrorCode.KAKAO_FAILED);
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", restKey);
        form.add("redirect_uri", redirectUri);
        form.add("code", code);
        if (!clientSecret.isBlank()) {
            form.add("client_secret", clientSecret);
        }

        try {
            KakaoToken token = restClient.post().uri(TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(KakaoToken.class);
            if (token == null || token.accessToken() == null) {
                throw new AuthException(ErrorCode.KAKAO_FAILED);
            }
            KakaoUser user = restClient.get().uri(USER_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.accessToken())
                    .retrieve()
                    .body(KakaoUser.class);
            if (user == null || user.id() == null) {
                throw new AuthException(ErrorCode.KAKAO_FAILED);
            }
            return user.id();
        } catch (HttpClientErrorException e) {
            // 코드 만료·재사용, redirect_uri 불일치가 대부분이다
            log.warn("카카오 인증 실패: {}", e.getResponseBodyAsString());
            throw new AuthException(ErrorCode.UNAUTHORIZED, "카카오 인증에 실패했습니다. 다시 로그인해 주세요.");
        } catch (RestClientException e) {
            log.error("카카오 서버 호출 실패", e);
            throw new AuthException(ErrorCode.KAKAO_FAILED);
        }
    }

    private record KakaoToken(@JsonProperty("access_token") String accessToken) {
    }

    private record KakaoUser(Long id) {
    }
}
