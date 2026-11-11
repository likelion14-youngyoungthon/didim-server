package com.likelionknu.dojang.common.response;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(GlobalException.class)
    public ResponseEntity<GlobalResponse<?>> handleGlobalException(GlobalException e) {
        return respond(e.getErrorCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GlobalResponse<?>> handleValidationException(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse(ErrorCode.BAD_REQUEST.getMessage());
        log.warn("요청 값 검증 실패: {}", detail);
        return respond(ErrorCode.BAD_REQUEST, detail);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<GlobalResponse<?>> handleParameterValidation(HandlerMethodValidationException e) {
        String detail = e.getParameterValidationResults().stream()
                .findFirst()
                .map(result -> result.getMethodParameter().getParameterName() + ": " + result.getResolvableErrors().getFirst().getDefaultMessage())
                .orElse(ErrorCode.BAD_REQUEST.getMessage());
        log.warn("요청 파라미터 검증 실패: {}", detail);
        return respond(ErrorCode.BAD_REQUEST, detail);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<GlobalResponse<?>> handleMissingParameter(MissingServletRequestParameterException e) {
        return respond(ErrorCode.BAD_REQUEST, e.getParameterName() + ": 값이 필요합니다.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<GlobalResponse<?>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return respond(ErrorCode.BAD_REQUEST, e.getName() + ": 형식이 올바르지 않습니다.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<GlobalResponse<?>> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("요청 본문을 읽을 수 없음: {}", e.getMessage());
        return respond(ErrorCode.BAD_REQUEST, ErrorCode.BAD_REQUEST.getMessage());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<GlobalResponse<?>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        return respond(ErrorCode.BAD_REQUEST, "Content-Type이 API에서 지원하는 형식과 다릅니다.");
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<GlobalResponse<?>> handleMissingPart(MissingServletRequestPartException e) {
        return respond(ErrorCode.BAD_REQUEST, e.getRequestPartName() + ": 파일이 필요합니다.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<GlobalResponse<?>> handleUploadSize(MaxUploadSizeExceededException e) {
        return respond(ErrorCode.BAD_REQUEST, "업로드 용량을 초과했습니다. 음성은 10MiB 이하로 보내주세요.");
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<GlobalResponse<?>> handleMultipart(MultipartException e) {
        return respond(ErrorCode.BAD_REQUEST, "multipart/form-data 업로드 형식이 올바르지 않습니다.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<GlobalResponse<?>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return respond(ErrorCode.METHOD_NOT_ALLOWED, ErrorCode.METHOD_NOT_ALLOWED.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<GlobalResponse<?>> handleNotFound(NoResourceFoundException e) {
        return respond(ErrorCode.NOT_FOUND, ErrorCode.NOT_FOUND.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GlobalResponse<?>> handleException(Exception e) {
        log.error("처리되지 않은 예외", e);
        return respond(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getMessage());
    }

    private ResponseEntity<GlobalResponse<?>> respond(ErrorCode errorCode, String message) {
        return ResponseEntity.status(errorCode.getHttpStatus()).body(GlobalResponse.error(errorCode, message));
    }
}
