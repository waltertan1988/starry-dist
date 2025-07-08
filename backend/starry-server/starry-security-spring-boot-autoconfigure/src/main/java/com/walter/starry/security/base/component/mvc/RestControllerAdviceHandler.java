package com.walter.starry.security.base.component.mvc;

import com.walter.starry.common.vo.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @author: walter.tan
 * @datetime: 2023/9/27 22:52
 */
@Slf4j
@RestControllerAdvice("com.walter.starry.security.base.controller")
public class RestControllerAdviceHandler {

    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> handleThrowable(Throwable throwable) {
        log.error("", throwable);
        return ApiResponse.fail(ApiResponse.ErrCode.INTERNAL_SERVER_ERROR, throwable.getMessage());
    }
}
