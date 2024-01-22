<template>
    <div v-if="formLoginPageVo">
        <div class="form-signin" method="post" :action="'/api/auth'+formLoginPageVo.authenticationUrl">
          <h2 class="form-signin-heading"><span style="font-weight: bold">{{loginSystemName}}</span></h2>
            <div class="alert alert-danger" role="alert" v-if="loginError">{{loginErrorMessage}}</div>
            <div class="alert alert-success" role="alert" v-if="isLogoutSuccess">You have been signed out</div>
            <p>
                <label for="username" class="sr-only">Username</label>
                <input type="text" id="username" :name="formLoginPageVo.usernameParameter" class="form-control" placeholder="账号" required autofocus v-model="inputUsername">
            </p>
            <p>
                <label for="password" class="sr-only">Password</label>
                <input type="password" id="password" :name="formLoginPageVo.passwordParameter" class="form-control" placeholder="密码" required v-model="inputPassword">
            </p>
            <p v-if="formLoginPageVo.rememberMeParameter">
                <input type='checkbox' :name="formLoginPageVo.rememberMeParameter"/> Remember me on this computer.
            </p>
            <template v-for="(value, key) in formLoginPageVo.hiddenInputs" :key="key">
                <input type="hidden" :name="key" :value="value"/>
            </template>
            <button class="btn btn-lg btn-primary btn-block" @click="submit">登录</button>
        </div>
    </div>
    <div v-if="oauth2LoginPageVo">
      <span>其它登录方式：</span>
      <template v-for="(value, key) in oauth2ClientNameToClientAuthenticationUrls" :key="key">
        <a :href="value" style="margin-right: 10px;">{{key}}</a>
      </template>
    </div>
</template>

<script>
    import {ElMessage} from "element-plus";
    import HttpApi from "../util/api";
    import Principal from "../util/principal";

    export default {
        name: 'Login',
        data() {
            return {
                loginSystemName: process.env.VUE_APP_LOGIN_PAGE_TITLE,
                loginError: null,
                loginErrorMessage: null,
                isLogoutSuccess: null,
                formLoginPageVo: null,
                oauth2LoginPageVo: null,
                saml2LoginPageVo: null,

                inputUsername: null,
                inputPassword: null
            }
        },
        computed: {
          oauth2ClientNameToClientAuthenticationUrls(){
            let result = {}
            for(let key in this.oauth2LoginPageVo.clientAuthenticationUrlToClientNameMap){
              result[this.oauth2LoginPageVo.clientAuthenticationUrlToClientNameMap[key]] = process.env.VUE_APP_API_AUTH_SERVER_PATH + key
            }
            return result
          }
        },
        created() {
            // 获取登录页的表单字段信息(如账/密字段的name, 各种隐藏域、表单提交的action路径)
            HttpApi.get('/api/auth/login',
                loginPageVo => {
                    this.loginError = loginPageVo.data.loginError
                    this.loginErrorMessage = loginPageVo.data.loginErrorMessage
                    this.isLogoutSuccess = loginPageVo.data.isLogoutSuccess
                    this.formLoginPageVo = loginPageVo.data.formLoginPageVo
                    this.oauth2LoginPageVo = loginPageVo.data.oauth2LoginPageVo
                    this.saml2LoginPageVo = loginPageVo.data.saml2LoginPageVo
                })
        },
        methods: {
            // 登录提交
            submit: function () {
                let url = "/api/auth" + this.formLoginPageVo.authenticationUrl

                let data = {}
                data[this.formLoginPageVo.usernameParameter] = this.inputUsername
                data[this.formLoginPageVo.passwordParameter] = this.inputPassword
                for(let key in this.formLoginPageVo.hiddenInputs){
                    data[key] = this.formLoginPageVo.hiddenInputs[key]
                }

                const config = {
                    headers:{
                        'Content-Type': 'application/x-www-form-urlencoded'
                    }
                }

                HttpApi.post(url, data, config,
                    // eslint-disable-next-line
                    response => {
                        this.loginError = false
                        this.loginErrorMessage = null

                        // 把登录用户的信息添加到Cookie
                        Principal.setCookie(response.data.principal)
                        // 清空旧的SessionStorage信息
                        Principal.clearSessionStorage();

                        // 重定向到管理后台首页
                        ElMessage({
                            message: "登录成功",
                            type: "success",
                            duration: 1000,
                            onClose: () => window.location.href = "/admin"
                        })
                    },
                    response => {
                        this.loginError = true
                        this.loginErrorMessage = response.errMsg
                    },
                    err => {
                        this.loginError = true
                        this.loginErrorMessage = "系统错误";
                        console.log(err)
                    }
                )
            }
        }
    }
</script>

<style scoped>

</style>