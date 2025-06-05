package com.walter.starry.security.base.vo.request.function;

import com.walter.starry.security.base.vo.response.resource.ResourceGroupVo;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @Author: walter.tan
 * @DateTime: 2023-10-18 11:46:48
 */
@Data
public class ListFunctionRequest {

    /** 资源树所在的根分组编码 */
    @NotBlank(message = "资源树所在的根分组不能为空")
    private String rootResourceGroupCode;

    /** 功能项或功能组的编码 */
    private String code;

    /** 功能项或功能组的名称 */
    private String name;

    /** 功能行类型，参考：{@link ResourceGroupVo.RowTypeEnum} */
    private Integer rowType;
}
