package com.likelionknu.dojang.basis.exception;

import com.likelionknu.dojang.common.response.ErrorCode;
import com.likelionknu.dojang.common.response.GlobalException;

public class BasisException extends GlobalException {

    public BasisException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
