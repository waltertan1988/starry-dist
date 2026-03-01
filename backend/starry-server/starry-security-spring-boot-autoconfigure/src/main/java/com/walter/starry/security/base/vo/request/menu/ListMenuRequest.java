package com.walter.starry.security.base.vo.request.menu;

import com.walter.starry.security.base.vo.response.resource.ResourceGroupVo;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @Author: walter.tan
 * @DateTime: 2023-10-18 11:46:48
 */
@Data
public class ListMenuRequest {

    /** 资源树所在的根分组编码 */
    @NotBlank(message = "资源树所在的根分组不能为空")
    private String rootResourceGroupCode;

    /** 菜单项或菜单组的编码 */
    private String code;

    /** 菜单项或菜单组的名称 */
    private String name;

    /** 菜单行类型，参考：{@link ResourceGroupVo.RowTypeEnum} */
    private Integer rowType;
}
