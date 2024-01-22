package com.walter.starry.security.base.vo.request.function;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.util.List;

/**
 * @Author: walter.tan
 * @DateTime: 2023-11-09 09:33:52
 */
@Data
public class HasFunctionRequest {

    /**
     * 类型，参看：{@link Type}
     */
    @NotNull(message = "类型不能为空")
    @Min(value = 0, message = "类型值非法")
    @Max(value = 1, message = "类型值非法")
    private Integer type;

    @NotEmpty(message = "功能项编码不能为空集")
    private List<String> functionItemCodeList;

    @Getter
    @AllArgsConstructor
    public enum Type {
        /** 全部 */
        ALL(0),

        /** 任一 */
        ANY(1);

        private final Integer code;
    }
}
