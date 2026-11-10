package com.likelionknu.dojang.analyze.exception;

import com.likelionknu.dojang.common.response.ErrorCode;
import com.likelionknu.dojang.common.response.GlobalException;

public class AnalyzeException extends GlobalException {

    public AnalyzeException(ErrorCode errorCode) {
        super(errorCode);
    }
}
