package com.walter.starry.security.base.vo.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.util.Objects;

/**
 * 标准Vo
 * @param <T>
 */
@Data
public class ApiResponse<T> {

    private Boolean success;

    private String errCode;

    private String errMsg;

    private String traceId;

    private T data;

    public static <T> ApiResponse<T> success(){
        return success(null);
    }

    public static <T> ApiResponse<T> success(T data){
        ApiResponse<T> resp = new ApiResponse<>();
        resp.success = true;
        resp.data = data;
        return resp;
    }

    public static <T> ApiResponse<T> fail(ErrCode errCode, String msgFmt, Object...msgFmtArgs){
        ApiResponse<T> resp = new ApiResponse<>();
        resp.success = false;
        resp.errCode = errCode.value;
        if(Objects.isNull(msgFmtArgs) || msgFmtArgs.length == 0){
            resp.errMsg = msgFmt;
        }else{
            resp.errMsg = String.format(msgFmt, msgFmtArgs);
        }

        return resp;
    }

    @Getter
    @AllArgsConstructor
    public enum ErrCode {
        /** 客户端错误的请求 */
        BAD_REQUEST("400"),
        /** 未认证 */
        UNAUTHORIZED("401"),
        /** 权限不足，拒绝访问 */
        FORBIDDEN("403"),
        /** 服务端内部错误 */
        INTERNAL_SERVER_ERROR("500");

        private final String value;
    }
}
