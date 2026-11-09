package com.likelionknu.dojang.judge.exception;

import com.likelionknu.dojang.common.response.ErrorCode;
import com.likelionknu.dojang.common.response.GlobalException;

public class JudgeException extends GlobalException {
    public JudgeException(String message) { super(ErrorCode.BAD_REQUEST, message); }
}
