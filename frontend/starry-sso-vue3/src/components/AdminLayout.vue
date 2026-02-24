<template>
    <el-container id="starryContainer">
        <el-header id="starryHeader">
            <div class="logo">
              <img src="../assets/star.svg" width="30" height="30">
              <span v-show="!isMemuCollapseRef" style="font-weight: bold; margin-left: 5px;">{{vueAppLoginPageTitleRef}}</span>
              <el-icon style="margin-left: 5px; cursor: pointer;" @click="toggleMenuCollapse">
                <Fold v-if="!isMemuCollapseRef"/><Expand v-else/>
              </el-icon>
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
                <el-aside id="starryAside" :class="[isMemuCollapseRef ? 'fold' : 'expand']">
                    <el-scrollbar>
                        <el-menu :collapse="isMemuCollapseRef" :router="true" :default-active="$route.path" :default-openeds="menuGroupDefaultOpeneds" :key="elMenuKeyRef">
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
            <span style="font-size:10px; color: grey;">© 2020 Walter Tan</span>
        </el-footer>
    </el-container>

    <!--ajax请求无权限时的登录框-->
    <el-dialog v-model="store.state.ajaxLoginFormDialog.visible" title="系统提示"
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
    <el-dialog v-model="store.state.ajaxLogoutFormDialog.visible" title="系统提示"
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

    <!-- 可拖拽弹窗 -->
    <div v-if="popupVisible" 
         id="draggablePopup"
         :style="{ left: popupPosition.left + 'px', top: popupPosition.top + 'px' }"
         @mousedown="startDrag"
         @touchstart="startDrag">
        <div class="popup-header">
            <span>智能问答</span>
            <el-icon class="close-icon" @click.stop="closePopup"><Close/></el-icon>
        </div>
        <div class="popup-content">
            hello world
        </div>
    </div>

    <!-- 智能客服图标 -->
    <div v-else 
         id="smartServiceIcon"
         :style="{ left: popupPosition.left + 'px', top: popupPosition.top + 'px' }"
         @click="showPopup"
         @mousedown="startDrag"
         @touchstart="startDrag">
        <div class="icon-content">
            <el-icon class="service-icon"><ChatDotRound/></el-icon>
            <span class="icon-text">智能问答</span>
        </div>
    </div>
</template>

<script setup>
    import { ref, onMounted } from 'vue'
    import HttpApi from "../util/api";
    import {ElMessage} from "element-plus";
    import MenuGroup from "./MenuGroup";
    import Cookie from "../util/cookie";
    import Principal from "../util/principal";
    import Login from "../util/login";
    import SessionStorage from "../util/sessionStorage";
    import {Expand, Fold, Setting, Close, ChatDotRound} from "@element-plus/icons-vue";
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

    // 左侧菜单是否折叠
    const isMemuCollapseRef = ref(sessionStorage.getItem(SessionStorage.KEYS.PRINCIPAL.IS_MENU_TREE_COLLAPSE) === "true")
    // 菜单树
    const menuTreeList = ref([])
    // 菜单树中默认打开的子菜单组
    const menuGroupDefaultOpeneds = ref([])

    const elMenuKeyRef = ref(Date.now())

    // 弹窗相关数据
    const popupVisible = ref(false); // 弹窗是否显示，默认不显示，显示智能客服图标
    const popupPosition = ref({ left: 0, top: 0 }); // 弹窗位置
    const isDragging = ref(false); // 是否正在拖拽
    const startX = ref(0); // 拖拽开始的X坐标
    const startY = ref(0); // 拖拽开始的Y坐标
    const hasMoved = ref(false); // 是否有移动，用于区分点击和拖拽
    const dragThreshold = 5; // 拖拽阈值，超过此值视为拖拽

    // 初始化弹窗位置
    onMounted(() => {
        // 设置弹窗初始位置为页面右下方，距离右侧边框10px
        const windowWidth = window.innerWidth;
        const windowHeight = window.innerHeight;
        const iconWidth = 60;
        const rightMargin = 10;
        popupPosition.value = {
            left: windowWidth - iconWidth - rightMargin,
            top: windowHeight - iconWidth - 80
        };
    });

    // 开始拖拽
    function startDrag(event) {
        isDragging.value = true;
        hasMoved.value = false;
        // 记录鼠标开始位置
        if (event.type === 'mousedown') {
            startX.value = event.clientX - popupPosition.value.left;
            startY.value = event.clientY - popupPosition.value.top;
            // 添加鼠标移动和释放事件监听
            document.addEventListener('mousemove', drag);
            document.addEventListener('mouseup', stopDrag);
        } else if (event.type === 'touchstart') {
            startX.value = event.touches[0].clientX - popupPosition.value.left;
            startY.value = event.touches[0].clientY - popupPosition.value.top;
            // 添加触摸移动和结束事件监听
            document.addEventListener('touchmove', drag);
            document.addEventListener('touchend', stopDrag);
        }
    }

    // 拖拽中
    function drag(event) {
        if (!isDragging.value) return;
        // 计算新位置
        let newLeft, newTop;
        if (event.type === 'mousemove') {
            newLeft = event.clientX - startX.value;
            newTop = event.clientY - startY.value;
        } else if (event.type === 'touchmove') {
            newLeft = event.touches[0].clientX - startX.value;
            newTop = event.touches[0].clientY - startY.value;
        }
        
        // 检测是否有实际移动
        const distance = Math.sqrt(
            Math.pow(newLeft - popupPosition.value.left, 2) + 
            Math.pow(newTop - popupPosition.value.top, 2)
        );
        if (distance > dragThreshold) {
            hasMoved.value = true;
        }
        
        // 更新位置
        popupPosition.value = {
            left: newLeft,
            top: newTop
        };
    }

    // 结束拖拽
    function stopDrag() {
        isDragging.value = false;
        // 移除事件监听
        document.removeEventListener('mousemove', drag);
        document.removeEventListener('mouseup', stopDrag);
        document.removeEventListener('touchmove', drag);
        document.removeEventListener('touchend', stopDrag);
    }

    // 关闭弹窗
    function closePopup() {
        popupVisible.value = false;
    }

    // 显示弹窗
    function showPopup() {
        // 只有在没有移动（纯粹的点击）的情况下才显示弹窗
        if (!hasMoved.value) {
            // 调整弹窗位置，确保弹窗完整显示
            adjustPopupPosition();
            popupVisible.value = true;
        }
        // 重置hasMoved，确保下一次点击或拖拽能够正确判断
        setTimeout(() => {
            hasMoved.value = false;
        }, 100);
    }

    // 调整弹窗位置，确保弹窗完整显示
    function adjustPopupPosition() {
        const windowWidth = window.innerWidth;
        const windowHeight = window.innerHeight;
        const popupWidth = 300;
        const popupHeight = 225;
        const minMargin = 5; // 最小边距，确保弹窗边界距离页面视界至少5px
        
        // 计算弹窗的理想位置（基于图标位置）
        let newLeft = popupPosition.value.left;
        let newTop = popupPosition.value.top;
        
        // 确保弹窗不会超出右侧边界
        if (newLeft + popupWidth > windowWidth) {
            newLeft = windowWidth - popupWidth - minMargin;
        }
        
        // 确保弹窗不会超出左侧边界
        if (newLeft < minMargin) {
            newLeft = minMargin;
        }
        
        // 确保弹窗不会超出底部边界
        if (newTop + popupHeight > windowHeight) {
            newTop = windowHeight - popupHeight - minMargin;
        }
        
        // 确保弹窗不会超出顶部边界
        if (newTop < minMargin) {
            newTop = minMargin;
        }
        
        // 强制确保弹窗完全在视口内
        // 再次检查并修正，确保计算正确
        newLeft = Math.max(minMargin, Math.min(newLeft, windowWidth - popupWidth - minMargin));
        newTop = Math.max(minMargin, Math.min(newTop, windowHeight - popupHeight - minMargin));
        
        // 更新弹窗位置
        popupPosition.value = {
            left: newLeft,
            top: newTop
        };
    }

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

    /** 切换左侧菜单的折叠/展开效果 */
    function toggleMenuCollapse(){
      isMemuCollapseRef.value = !isMemuCollapseRef.value
      sessionStorage.setItem(SessionStorage.KEYS.PRINCIPAL.IS_MENU_TREE_COLLAPSE, isMemuCollapseRef.value)
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
        color: var(--el-text-color-primary);
        background: var(--el-color-primary-light-8);
    }

    #starryAside.expand{
      width: var(--starry-aside-expend-width);
    }

    #starryAside.fold{
      width: var(--starry-aside-fold-width);
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

    /* 可拖拽弹窗样式 */
    #draggablePopup {
        position: fixed;
        width: 300px;
        height: 225px;
        background-color: white;
        border: 1px solid #e0e0e0;
        border-radius: 4px;
        box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
        z-index: 9999;
        cursor: move;
        overflow: hidden;
    }

    .popup-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 10px 15px;
        background-color: var(--el-color-primary-light-7);
        border-bottom: 1px solid #e0e0e0;
        cursor: move;
    }

    .popup-header span {
        font-size: 14px;
        font-weight: 500;
        color: #303133;
    }

    .close-icon {
        font-size: 16px;
        color: #909399;
        cursor: pointer;
        transition: color 0.3s;
    }

    .close-icon:hover {
        color: #606266;
    }

    .popup-content {
        padding: 20px;
        font-size: 14px;
        color: #303133;
        text-align: center;
    }

    /* 智能客服图标样式 */
    #smartServiceIcon {
        position: fixed;
        width: 60px;
        height: 60px;
        background-color: #409eff;
        border-radius: 50%;
        box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.15);
        z-index: 9999;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: all 0.3s ease;
    }

    #smartServiceIcon:hover {
        transform: scale(1.05);
        box-shadow: 0 4px 16px 0 rgba(0, 0, 0, 0.2);
    }

    .icon-content {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        color: white;
    }

    .service-icon {
        font-size: 24px;
        margin-bottom: 4px;
    }

    .icon-text {
        font-size: 10px;
        font-weight: 500;
    }
</style>