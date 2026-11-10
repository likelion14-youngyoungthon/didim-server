package com.likelionknu.dojang.member.controller;

import com.likelionknu.dojang.auth.config.AuthInterceptor;
import com.likelionknu.dojang.common.response.GlobalResponse;
import com.likelionknu.dojang.member.data.dto.request.ProgressRequestDto;
import com.likelionknu.dojang.member.data.dto.response.ProgressResponseDto;
import com.likelionknu.dojang.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

    @GetMapping("/progress")
    @Operation(summary = "내 신청 현황 불러오기")
    public GlobalResponse<ProgressResponseDto> getProgress(@Parameter(hidden = true) @RequestAttribute(AuthInterceptor.MEMBER_ID) Long memberId) {
        return GlobalResponse.ok(memberService.getProgress(memberId));
    }

    @PutMapping("/progress")
    @Operation(summary = "내 신청 현황 저장", description = "보낸 내용으로 통째로 덮어씁니다.")
    public GlobalResponse<ProgressResponseDto> saveProgress(@Parameter(hidden = true) @RequestAttribute(AuthInterceptor.MEMBER_ID) Long memberId,
                                                            @Valid @RequestBody ProgressRequestDto request) {
        return GlobalResponse.ok(memberService.saveProgress(memberId, request));
    }

    @DeleteMapping
    @Operation(summary = "탈퇴", description = "회원과 저장한 신청 현황을 바로 지웁니다.")
    public GlobalResponse<Void> withdraw(@Parameter(hidden = true) @RequestAttribute(AuthInterceptor.MEMBER_ID) Long memberId) {
        memberService.withdraw(memberId);
        return GlobalResponse.ok();
    }
}
