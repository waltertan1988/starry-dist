<template>
    <el-container id="starryContainer">
        <el-header id="starryHeader">
            <div class="logo">
              <span style="font-weight: bold">{{vueAppLoginPageTitleRef}}</span>
            </div>
            <div class="toolbar">
                <div v-if="userPrincipal.nickname">
                    <el-dropdown style="cursor: pointer;" @command="handleHeaderUserPrincipalCommand">
                        <div>
                          {{decodeURI(userPrincipal.nickname)}}
                          <el-icon style="margin-right: 8px; margin-top: 1px"><Setting/></el-icon>
                        </div>
                        <template #dropdown>
                            <el-dropdown-menu>
                                <el-dropdown-item command="logout"><el-icon><Expand /></el-icon>退出</el-dropdown-item>
                            </el-dropdown-menu>
                        </template>
                    </el-dropdown>
                </div>
                <div v-else><a href="javascript:void(0)" @click="openAjaxLoginFormDialog">登录</a></div>
            </div>
        </el-header>

        <el-main id="starryMain">
            <el-container>
                <el-aside id="starryAside">
                    <el-scrollbar>
                        <el-menu :router="true" :default-active="$route.path" :default-openeds="menuGroupDefaultOpeneds" :key="elMenuKeyRef">
                            <MenuGroup :menu-list="menuTreeList"></MenuGroup>
                        </el-menu>
                    </el-scrollbar>
                </el-aside>

                <el-main id="starryContent" style="margin-left: 5px;">
                    <el-scrollbar>
                        <div style="margin-right: 8px;">
                          <slot></slot>
                        </div>
                    </el-scrollbar>
                </el-main>
            </el-container>
        </el-main>

        <el-footer id="starryFooter">
            <hr style="margin: 0">
            <span style="font-size:10px; color: grey;">© 2023 Walter Tan</span>
        </el-footer>
    </el-container>

    <!--ajax请求无权限时的登录框-->
    <el-dialog v-model="store.state.ajaxLoginFormDialog.visible" title="统一认证平台"
               center align-center width="300" :close-on-click-modal="false" :show-close="false">
        <div class="alert alert-danger" role="alert">你的会话已过期，请重新登录</div>

        <el-form method="post" :action="'/api/auth' + store.state.ajaxLoginFormDialog.loginPageVo.formLoginPageVo.authenticationUrl">
            <div class="alert alert-danger" role="alert" v-if="store.state.ajaxLoginFormDialog.loginPageVo.loginError">
                {{store.state.ajaxLoginFormDialog.loginPageVo.loginErrorMessage}}
            </div>

            <el-form-item>
                <el-input :name="store.state.ajaxLoginFormDialog.loginPageVo.formLoginPageVo.usernameParameter"
                          v-model="store.state.ajaxLoginFormDialog.inputUsername"
                          placeholder="账号" clearable/>
            </el-form-item>
            <el-form-item>
                <el-input :name="store.state.ajaxLoginFormDialog.loginPageVo.formLoginPageVo.passwordParameter"
                          v-model="store.state.ajaxLoginFormDialog.inputPassword"
                          type="password" placeholder="密码" clearable/>
            </el-form-item>

            <template v-for="(value, key) in store.state.ajaxLoginFormDialog.loginPageVo.formLoginPageVo.hiddenInputs" :key="key">
                <input type="hidden" :name="key" :value="value"/>
            </template>
        </el-form>

        <template #footer>
          <span class="dialog-footer">
            <el-button type="primary" @click="loginSubmit">登录</el-button>
            <el-button @click="store.commit('ajaxLoginFormDialog/setVisible', false)">取消</el-button>
          </span>
        </template>
    </el-dialog>

    <!--ajax退出登录框-->
    <el-dialog v-model="store.state.ajaxLogoutFormDialog.visible" title="统一认证平台"
               center align-center width="300" :close-on-click-modal="false" :show-close="false">
        <div class="alert alert-warning" role="alert">您确定要退出登录吗？</div>

        <el-form method="post" :action="'/api/auth' + store.state.ajaxLogoutFormDialog.logoutPageVo.logoutUrl">
            <template v-for="(value, key) in store.state.ajaxLogoutFormDialog.logoutPageVo.hiddenInputs" :key="key">
                <input type="hidden" :name="key" :value="value"/>
            </template>
        </el-form>

        <template #footer>
          <span class="dialog-footer">
            <el-button type="primary" @click="logoutSubmit">继续退出</el-button>
            <el-button @click="store.commit('ajaxLogoutFormDialog/setVisible', false)">取消</el-button>
          </span>
        </template>
    </el-dialog>
</template>

<script setup>
    import { ref } from 'vue'
    import HttpApi from "../util/api";
    import {ElMessage} from "element-plus";
    import MenuGroup from "./MenuGroup";
    import Cookie from "../util/cookie";
    import Principal from "../util/principal";
    import Login from "../util/login";
    import SessionStorage from "../util/sessionStorage";
    import {Expand, Setting} from "@element-plus/icons-vue";
    import {useStore} from "vuex";

    const store = useStore()

    const vueAppLoginPageTitleRef = ref(process.env.VUE_APP_LOGIN_PAGE_TITLE)

    // 使用非Ajax方式登录后再重定向到页面时，尝试从查询串中解析nickname并设置到cookie
    const nicknameFromSearchParam = new URLSearchParams(window.location.search).get("nickname")
    if(nicknameFromSearchParam){
      Principal.setCookie({
        nickname: nicknameFromSearchParam
      })
    }

    // 用户principal的响应式数据
    const userPrincipal = ref({
        nickname: Cookie.get(Cookie.KEYS.PRINCIPAL.NICKNAME)
    })

    // 菜单树
    const menuTreeList = ref([])
    // 菜单树中默认打开的子菜单组
    const menuGroupDefaultOpeneds = ref([])

    const elMenuKeyRef = ref(Date.now())

    // 加载菜单
    loadMenuTreeListUsingSessionStorage();

    /**
     * 点击顶栏用户的下拉选项
     */
    function handleHeaderUserPrincipalCommand(command) {
        if(command === 'logout'){
            HttpApi.get("/api/auth/logout",
                response => {
                    let logoutPageVo = response.data
                    // 填充响应式数据
                    store.commit("ajaxLogoutFormDialog/setLogoutPageVo", logoutPageVo)
                    // 弹出退出登录框
                    store.commit("ajaxLogoutFormDialog/setVisible", true)
                }
            )
        }
    }

    /**
     * 加载菜单树
     */
    function loadMenuTreeListUsingSessionStorage() {
        if(sessionStorage.getItem(SessionStorage.KEYS.PRINCIPAL.MENU_TREE_NODES) == null){
            HttpApi.get(`/api/auth/admin/menu/loadForAllUser?rootResourceGroupCode=${process.env.VUE_APP_ROOT_MENU_GROUP}`,
                response => {
                    sessionStorage.setItem(SessionStorage.KEYS.PRINCIPAL.MENU_TREE_NODES, JSON.stringify(response.data.resourceGroupVoList));
                    mountMenuTree(response.data.resourceGroupVoList)
                },
                response => {
                    ElMessage.error(response.errMsg)
                },
                err => {
                    ElMessage.error(err)
                }
            )
        }else{
            mountMenuTree(JSON.parse(sessionStorage.getItem(SessionStorage.KEYS.PRINCIPAL.MENU_TREE_NODES)))
        }
    }

    /**
     * 挂载菜单树
     * @param menuTreeNodes
     */
    function mountMenuTree(menuTreeNodes){
      // 挂载菜单
      menuTreeList.value = menuTreeNodes

      const defaultOpeneds = []
      dfsTravelDefaultOpenedMenuGroups(menuTreeNodes, defaultOpeneds)
      menuGroupDefaultOpeneds.value = defaultOpeneds

      // 刷新左侧菜单栏组件
      elMenuKeyRef.value = Date.now()
    }

    function dfsTravelDefaultOpenedMenuGroups(menuGroupList, defaultOpenedList){
        if(menuGroupList){
            menuGroupList.forEach(group => {
                if(group.rowType === 1 && group.config.defaultOpen){
                    defaultOpenedList.push(group.rowType + '-' + group.code)
                }

                dfsTravelDefaultOpenedMenuGroups(group.resourceGroupVoList, defaultOpenedList)
            })
        }
    }

    /**
     * 弹出登录对话框
     */
    function openAjaxLoginFormDialog() {
        Login.openAjaxLoginFormDialog()
    }

    /**
     * ajax登录提交
     */
    function loginSubmit() {
        const ajaxLoginFormDialog = store.state.ajaxLoginFormDialog;

        let url = "/api/auth" + ajaxLoginFormDialog.loginPageVo.formLoginPageVo.authenticationUrl

        let data = {}
        data[ajaxLoginFormDialog.loginPageVo.formLoginPageVo.usernameParameter] = ajaxLoginFormDialog.inputUsername
        data[ajaxLoginFormDialog.loginPageVo.formLoginPageVo.passwordParameter] = ajaxLoginFormDialog.inputPassword
        for(let key in ajaxLoginFormDialog.loginPageVo.formLoginPageVo.hiddenInputs){
            data[key] = ajaxLoginFormDialog.loginPageVo.formLoginPageVo.hiddenInputs[key]
        }

        const config = {
            headers:{
                'Content-Type': 'application/x-www-form-urlencoded'
            }
        }

        // 提交登录
        HttpApi.post(url, data, config,
            response => {
                ajaxLoginFormDialog.loginPageVo.loginError = false
                ajaxLoginFormDialog.loginPageVo.loginErrorMessage = null
                store.commit("ajaxLoginFormDialog/setLoginPageVo", ajaxLoginFormDialog.loginPageVo)

                // 把登录用户的信息添加到Cookie
                Principal.setCookie(response.data.principal)
                // 清空旧的SessionStorage信息
                Principal.clearSessionStorage();

                userPrincipal.value.nickname = Cookie.get(Cookie.KEYS.PRINCIPAL.NICKNAME)

                // 关闭对话框
                store.commit('ajaxLoginFormDialog/setVisible', false)
                ElMessage.success("登录成功")

                // 加载菜单
                // loadMenuTreeListUsingSessionStorage();

                // 刷新当前页面
                window.location.reload()
            },
            response => {
                ajaxLoginFormDialog.loginPageVo.loginError = true
                ajaxLoginFormDialog.loginPageVo.loginErrorMessage = response.errMsg
                store.commit("ajaxLoginFormDialog/setLoginPageVo", ajaxLoginFormDialog.loginPageVo)
            },
            err => {
                ajaxLoginFormDialog.loginPageVo.loginError = true
                ajaxLoginFormDialog.loginPageVo.loginErrorMessage = "系统错误"
                store.commit("ajaxLoginFormDialog/setLoginPageVo", ajaxLoginFormDialog.loginPageVo)
                console.log(err)
            }
        )
    }

    /**
     * ajax退出登录提交
     */
    function logoutSubmit() {
        const ajaxLogoutFormDialog = store.state.ajaxLogoutFormDialog;

        let url = "/api/auth" + ajaxLogoutFormDialog.logoutPageVo.logoutUrl

        let data = {}
        for(let key in ajaxLogoutFormDialog.logoutPageVo.hiddenInputs){
            data[key] = ajaxLogoutFormDialog.logoutPageVo.hiddenInputs[key]
        }

        const config = {
            headers:{
                'Content-Type': 'application/x-www-form-urlencoded'
            }
        }

        // 提交退出登录
        HttpApi.post(url, data, config,
            () => {
                processAfterLogout()
            },
            response => {
                processAfterLogout()
                console.log(JSON.stringify(response))
            },
            err => {
                processAfterLogout()
                console.log(err)
            }
        )
    }

    /**
     * 退出登录后的处理，主要是执行一些清理工作
     */
    function processAfterLogout(){
        const ajaxLogoutFormDialog = store.state.ajaxLogoutFormDialog;

        // 清空退出登录页面的信息
        ajaxLogoutFormDialog.logoutUrl = null;
        ajaxLogoutFormDialog.hiddenInputs = null;
        store.commit("ajaxLogoutFormDialog/setLogoutPageVo", ajaxLogoutFormDialog)

        // 清空所有登录用户的Cookie信息
        Principal.clearAllCookie()

        // 清空所有登录用户的SessionStorage信息
        Principal.clearSessionStorage()

        // 清空用户principal的响应式数据
        userPrincipal.value = {}

        // 关闭对话框
        store.commit('ajaxLogoutFormDialog/setVisible', false)

        // 重定向到登录页
        ElMessage({
            message: "退出成功",
            type: "success",
            duration: 1000,
            onClose: () => window.location.href = "/login"
        })
    }
</script>

<style scoped>
    #starryHeader {
        position: relative;
        background-color: var(--el-color-primary-light-7);
        color: var(--el-text-color-primary);
        width: 100%;
        height: var(--starry-header-height);
    }
    #starryHeader .logo {
      float: left;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      height: 100%;
      left: 5px;
    }
    #starryHeader .toolbar {
      float: right;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      height: 100%;
      right: 20px;
      font-size: 12px;
    }

    #starryMain{
        padding: 0;
        background: var(--el-bg-color);
    }

    #starryAside {
        width: var(--starry-aside-width);
        color: var(--el-text-color-primary);
        background: var(--el-color-primary-light-8);
    }

    #starryAside .el-scrollbar {
        height: var(--starry-main-height);
    }

    #starryContent {
        padding: 0;
    }

    #starryContent .el-scrollbar {
        height: var(--starry-main-height);
    }

    #starryFooter {
        position: absolute;
        bottom: 0;
        width: 100%;
        height: var(--starry-footer-height);
        text-align: center;
    }
</style>