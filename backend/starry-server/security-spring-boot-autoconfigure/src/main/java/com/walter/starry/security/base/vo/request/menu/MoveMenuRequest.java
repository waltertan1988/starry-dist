package com.walter.starry.security.base.vo.request.menu;

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
public class MoveMenuRequest {

    /**
     * 待移动的菜单行编码
     */
    @NotBlank(message = "待移动菜单行编码不能为空")
    private String code;

    /** 待移动的菜单行类型，参考：{@link ResourceGroupVo.RowTypeEnum} */
    @NotNull(message = "菜单行类型不能为空")
    @Min(value = 0, message = "待移动菜单行类型非法")
    @Max(value = 1, message = "待移动菜单行类型非法")
    private Integer rowType;

    /**
     * 移动到菜单分组编码
     */
    @NotBlank(message = "移动到菜单分组编码不能为空")
    private String moveToGroupCode;
}
