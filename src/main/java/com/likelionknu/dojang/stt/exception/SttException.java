package com.likelionknu.dojang.stt.exception;

import com.likelionknu.dojang.common.response.ErrorCode;
import com.likelionknu.dojang.common.response.GlobalException;

public class SttException extends GlobalException {
    public SttException(ErrorCode code, String message) {
        super(code, message);
    }
}
