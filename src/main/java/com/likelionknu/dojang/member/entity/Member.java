package com.likelionknu.dojang.member.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** 카카오 회원번호와 신청 현황만 둔다. 이름·연락처와 대화 내용은 저장하지 않는다. */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long kakaoId;

    /** /judge 요청 형식 JSON */
    @Column(columnDefinition = "text")
    private String profile;

    /** [{id, status}] JSON */
    @Column(columnDefinition = "text")
    private String services;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant progressUpdatedAt;

    public static Member join(long kakaoId) {
        Member member = new Member();
        member.kakaoId = kakaoId;
        member.createdAt = Instant.now();
        return member;
    }

    public void saveProgress(String profile, String services) {
        this.profile = profile;
        this.services = services;
        this.progressUpdatedAt = Instant.now();
    }
}
