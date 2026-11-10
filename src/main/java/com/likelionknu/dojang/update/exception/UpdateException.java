package com.likelionknu.dojang.update.exception;

import com.likelionknu.dojang.common.response.ErrorCode;
import com.likelionknu.dojang.common.response.GlobalException;

public class UpdateException extends GlobalException {

    public UpdateException(ErrorCode errorCode) {
        super(errorCode);
    }
}
