package com.walter.starry.common.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.util.Objects;

/**
 * 标准响应体Vo
 * @author walter.tan
 * @param <T>
 */
@Data
public class ApiResponse<T> {
    /** 是否成功 */
    private Boolean success;
    /** 错误编码 */
    private String errCode;
    /** 错误信息 */
    private String errMsg;
    /** 响应业务数据 */
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
