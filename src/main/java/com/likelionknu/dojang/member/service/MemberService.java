package com.likelionknu.dojang.member.service;

import com.likelionknu.dojang.auth.exception.AuthException;
import com.likelionknu.dojang.common.response.ErrorCode;
import com.likelionknu.dojang.judge.data.dto.request.JudgeRequestDto;
import com.likelionknu.dojang.member.data.dto.request.ProgressRequestDto;
import com.likelionknu.dojang.member.data.dto.request.ProgressRequestDto.ServiceStateDto;
import com.likelionknu.dojang.member.data.dto.response.ProgressResponseDto;
import com.likelionknu.dojang.member.entity.Member;
import com.likelionknu.dojang.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberService {
    private static final TypeReference<List<ServiceStateDto>> SERVICE_LIST = new TypeReference<>() {
    };

    private final MemberRepository memberRepository;
    private final JsonMapper jsonMapper;

    @Transactional(readOnly = true)
    public ProgressResponseDto getProgress(Long memberId) {
        Member member = find(memberId);
        JudgeRequestDto profile = member.getProfile() == null ? null : jsonMapper.readValue(member.getProfile(), JudgeRequestDto.class);
        List<ServiceStateDto> services = member.getServices() == null ? List.of() : jsonMapper.readValue(member.getServices(), SERVICE_LIST);
        return new ProgressResponseDto(profile, services, member.getProgressUpdatedAt());
    }

    @Transactional
    public ProgressResponseDto saveProgress(Long memberId, ProgressRequestDto request) {
        Member member = find(memberId);
        String profile = request.profile() == null ? null : jsonMapper.writeValueAsString(request.profile());
        member.saveProgress(profile, jsonMapper.writeValueAsString(request.services()));
        return new ProgressResponseDto(request.profile(), request.services(), member.getProgressUpdatedAt());
    }

    @Transactional
    public void withdraw(Long memberId) {
        memberRepository.delete(find(memberId));
    }

    // 탈퇴 뒤에도 남은 토큰으로 들어오는 경우
    private Member find(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new AuthException(ErrorCode.UNAUTHORIZED, "회원 정보가 없습니다. 다시 로그인해 주세요."));
    }
}
