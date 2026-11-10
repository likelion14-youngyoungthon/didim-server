package com.likelionknu.dojang.steps.exception;

import com.likelionknu.dojang.common.response.ErrorCode;
import com.likelionknu.dojang.common.response.GlobalException;

public class ServiceStepsException extends GlobalException {
    public ServiceStepsException() {
        super(ErrorCode.NOT_FOUND, "service: 없는 서비스입니다.");
    }
}
