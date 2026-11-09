package com.likelionknu.dojang.common.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 응답 code는 E + HTTP 상태코드. 기능별 에러는 여기에 추가하고, 기능 패키지 exception에서 GlobalException을 상속해 쓴다. */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    BAD_REQUEST(400, "잘못된 요청입니다."),
    NOT_FOUND(404, "정보를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(405, "지원하지 않는 요청 방식입니다."),
    INTERNAL_ERROR(500, "서버 오류가 발생했습니다."),
    AI_FAILED(502, "AI 호출에 실패했습니다.");

    private final int httpStatus;
    private final String message;

    public String getCode() {
        return "E" + httpStatus;
    }
}
