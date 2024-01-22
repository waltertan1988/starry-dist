package com.walter.starry.security.base.common.exception;

import com.walter.starry.security.base.vo.response.ApiResponse;
import lombok.Getter;

/**
 * @author walter.tan
 */
@Getter
public class BizException extends RuntimeException {
    /**
     * 异常码
     */
    private ApiResponse.ErrCode code;

    public BizException() {
        super();
    }

    public BizException(ApiResponse.ErrCode code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(ApiResponse.ErrCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public BizException(Throwable cause) {
        super(cause);
    }

    protected BizException(ApiResponse.ErrCode code, String message, Throwable cause,
                               boolean enableSuppression,
                               boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.code = code;
    }
}
