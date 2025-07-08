package com.walter.starry.security.base.vo.response.resource;

import com.walter.starry.security.base.entity.JsonConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.util.Date;
import java.util.List;

/**
 * @author: walter.tan
 * @datetime: 2023/9/19 18:44
 */
@Data
public class ResourceGroupVo {

    /** 资源行类型，参考：{@link RowTypeEnum} */
    private Integer rowType;

    /** 主键 */
    private Long id;

    /** 编码 */
    private String code;

    /** 名称 */
    private String name;

    /** 顺序，数值越小越靠前 */
    private Long seq;

    /** 上一层级分组的编码值 */
    private String parentGroupCode;

    /** JSON配置 */
    private JsonConfig config;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;

    /**
     * 子资源组/项
     */
    private List<ResourceGroupVo> resourceGroupVoList;

    /**
     * 资源行类型枚举
     */
    @Getter
    @AllArgsConstructor
    public enum RowTypeEnum {

        /** 资源项 */
        ITEM(0),

        /** 资源组 */
        GROUP(1);

        private final Integer code;
    }
}
