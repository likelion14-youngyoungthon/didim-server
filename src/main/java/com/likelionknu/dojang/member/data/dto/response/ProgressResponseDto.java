package com.likelionknu.dojang.member.data.dto.response;

import com.likelionknu.dojang.judge.data.dto.request.JudgeRequestDto;
import com.likelionknu.dojang.member.data.dto.request.ProgressRequestDto.ServiceStateDto;

import java.time.Instant;
import java.util.List;

/** 저장한 적이 없으면 profile·updated_at은 null, services는 빈 목록 */
public record ProgressResponseDto(JudgeRequestDto profile, List<ServiceStateDto> services, Instant updatedAt) {
}
