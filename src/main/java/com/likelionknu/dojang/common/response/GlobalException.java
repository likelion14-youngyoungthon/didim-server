package com.likelionknu.dojang.common.response;

import lombok.Getter;

@Getter
public abstract class GlobalException extends RuntimeException {
    private final ErrorCode errorCode;

    public GlobalException(ErrorCode errorCode) {
        this(errorCode, errorCode.getMessage());
    }

    /** message는 응답 message에 그대로 나간다. 사용자에게 보여줄 사유를 적는다. */
    public GlobalException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
