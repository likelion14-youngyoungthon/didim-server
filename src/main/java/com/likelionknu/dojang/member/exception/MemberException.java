package com.likelionknu.dojang.member.exception;

import com.likelionknu.dojang.common.response.ErrorCode;
import com.likelionknu.dojang.common.response.GlobalException;

public class MemberException extends GlobalException {
    public MemberException(String message) {
        super(ErrorCode.BAD_REQUEST, message);
    }
}
