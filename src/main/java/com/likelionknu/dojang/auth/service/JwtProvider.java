package com.likelionknu.dojang.auth.service;

import com.likelionknu.dojang.auth.exception.AuthException;
import com.likelionknu.dojang.common.response.ErrorCode;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/** refresh 토큰 없이 access 토큰 하나만 쓴다. 만료되면 카카오 로그인을 다시 한다. */
@Component
public class JwtProvider {
    private final SecretKey key;
    private final Duration expiry;

    public JwtProvider(@Value("${jwt.secret}") String secret, @Value("${jwt.expire-days}") long expireDays) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiry = Duration.ofDays(expireDays);
    }

    public String issue(Long memberId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiry)))
                .signWith(key)
                .compact();
    }

    public Long parseMemberId(String token) {
        try {
            String subject = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
            return Long.valueOf(subject);
        } catch (JwtException | IllegalArgumentException e) {
            throw new AuthException(ErrorCode.UNAUTHORIZED, "로그인이 만료되었습니다. 다시 로그인해 주세요.");
        }
    }

    public long expiresInSeconds() {
        return expiry.toSeconds();
    }
}
