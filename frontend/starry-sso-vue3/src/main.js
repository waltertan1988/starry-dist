import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import '@/assets/style/global.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import App from './App.vue'
import './registerServiceWorker'
import router from './router'
import store from './store'
import PermissionDirectives from "@/directives/permission"
import axios from "axios";

const app = createApp(App);

// 注册element plus的所有Icon图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component);
}

// 使用vuex、route、element plus
app.use(store).use(router).use(ElementPlus, {locale: zhCn}).mount('#app')

// 全局注册自定义功能权限指令
for(let key in PermissionDirectives){
    app.directive(key, PermissionDirectives[key])
}

// 设置axios全局的AJAX请求头
axios.defaults.headers.common['X-Requested-With'] = 'XMLHttpRequest'

// 打印环境变量
console.log("running mode: " + process.env.NODE_ENV)
console.log("base url: " + process.env.BASE_URL)
console.log("authorization server url: " + process.env.VUE_APP_API_AUTH_SERVER_PATH)