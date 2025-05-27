package com.example.springaidemo.exception;

import com.example.springaidemo.common.BaseResponse;
import com.example.springaidemo.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public <T> BaseResponse<T> handleBusinessException(BusinessException e) {
        log.error("BusinessException occurred, code: {}, message: {}", e.getCode(), e.getMessage());
        log.error("Stack trace:", e);  // 这行会打印完整堆栈
        return ResultUtils.error(e.getCode(), e.getMessage());
    }
    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> businessExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR);
    }
}
