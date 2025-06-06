package com.walter.starry.security.base.vo.response.resource;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author: walter.tan
 * @datetime: 2023/9/19 18:45
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ResourceItemVo extends ResourceGroupVo {

    /** http请求方法类型 */
    private List<String> httpMethodList;

    /** 路径 */
    private String pattern;
}
