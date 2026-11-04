package com.likelionknu.dojang.common.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 모든 API 응답 형식 (노션 API 명세서 0번 공통 규칙).
 * 성공: {"code": "OK", "data": {...}, "message": null}
 * 실패: {"code": "E400", "data": null, "message": "사유"}
 */
@Getter
@AllArgsConstructor
public class GlobalResponse<T> {
    private final String code;
    private final T data;
    private final String message;

    public static <T> GlobalResponse<T> ok(T data) {
        return new GlobalResponse<>("OK", data, null);
    }

    public static GlobalResponse<Void> ok() {
        return new GlobalResponse<>("OK", null, null);
    }

    public static <T> GlobalResponse<T> error(ErrorCode errorCode, String message) {
        return new GlobalResponse<>(errorCode.getCode(), null, message);
    }

    public static <T> GlobalResponse<T> error(ErrorCode errorCode) {
        return error(errorCode, errorCode.getMessage());
    }
}
