package com.walter.starry.security.base.vo.response.base;

import lombok.Data;

import java.util.Map;

/**
 * 填充登录页的数据
 * @author: walter.tan
 * @datetime: 2023/8/27 18:09
 */
@Data
public class LoginPageVo {
    /**
     * 登录是否出现错误
     */
    private Boolean loginError;
    /**
     * 登录错误消息
     */
    private String loginErrorMessage;
    /**
     * 登录注销是否成功
     */
    private Boolean isLogoutSuccess;
    /**
     * 表单登录页数据的Vo，null表示不启用表单登录功能
     */
    private FormLoginPageVo formLoginPageVo;
    /**
     * OAuth2登录页数据的Vo，null表示不启用OAuth2登录功能
     */
    private OAuth2LoginPageVo oAuth2LoginPageVo;
    /**
     * Saml2登录页数据的Vo，null表示不启用Saml2登录功能
     */
    private Saml2LoginPageVo saml2LoginPageVo;

    @Data
    public static class FormLoginPageVo {
        /**
         * 账号输入框在登录表单中的name
         */
        private String usernameParameter;
        /**
         * 密码输入框在登录表单中的name
         */
        private String passwordParameter;
        /**
         * rememberMe复选框在登录表单中的name，空表示不启用rememberMe功能
         */
        private String rememberMeParameter;
        /**
         * 表单中的隐藏域
         */
        private Map<String, String> hiddenInputs;
        /**
         * 登录表单的提交url
         */
        private String authenticationUrl;
    }

    @Data
    public static class OAuth2LoginPageVo{
        /**
         * 客户端授权url映射集，格式为[url,clientName]
         */
        private Map<String, String> clientAuthenticationUrlToClientNameMap;
    }

    @Data
    public static class Saml2LoginPageVo {
        /**
         * 客户端授权url映射集，格式为[url,name]
         */
        private Map<String, String>  relyingPartyUrlToNameMap;
    }
}
