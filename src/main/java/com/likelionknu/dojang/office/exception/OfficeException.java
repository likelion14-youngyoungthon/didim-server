package com.likelionknu.dojang.office.exception;

import com.likelionknu.dojang.common.response.ErrorCode;
import com.likelionknu.dojang.common.response.GlobalException;

public class OfficeException extends GlobalException {

    public OfficeException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
