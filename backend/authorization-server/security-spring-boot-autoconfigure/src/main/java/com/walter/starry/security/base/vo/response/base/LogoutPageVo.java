package com.walter.starry.security.base.vo.response.base;

import lombok.Data;

import java.util.Map;

/**
 * @author: walter.tan
 * @datetime: 2023/9/24 16:26
 */
@Data
public class LogoutPageVo {
    /**
     * 注销表单的提交url
     */
    private String logoutUrl;

    /**
     * 表单中的隐藏域
     */
    private Map<String, String> hiddenInputs;
}
