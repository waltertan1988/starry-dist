package com.walter.starry.security.base.vo.request.function;

import com.walter.starry.security.base.vo.response.resource.ResourceGroupVo;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * @Author: walter.tan
 * @DateTime: 2023-10-13 23:26:29
 */
@Data
public class MoveFunctionRequest {

    /**
     * 待移动的功能行编码
     */
    @NotBlank(message = "待移动功能行编码不能为空")
    private String code;

    /** 待移动的功能行类型，参考：{@link ResourceGroupVo.RowTypeEnum} */
    @NotNull(message = "功能行类型不能为空")
    @Min(value = 0, message = "待移动功能行类型非法")
    @Max(value = 1, message = "待移动功能行类型非法")
    private Integer rowType;

    /**
     * 移动到功能分组编码
     */
    @NotBlank(message = "移动到功能分组编码不能为空")
    private String moveToGroupCode;
}
