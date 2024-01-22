# starry-sso-vue3

## Project setup
```
npm install
```

### Compiles and hot-reloads for development
```
npm run serve
```

### Compiles and minifies for production
```
npm run build
```

### Lints and fixes files
```
npm run lint
```

### Customize configuration
See [Configuration Reference](https://cli.vuejs.org/config/).

## 项目说明 
基于Vue3和Element Plus研发的后台管理系统

### 目录结构说明 
* public  
第三方默认的资源(css、图片等)
* src  
  * assets  
  自定义的资源(css、图片等)
  * components  
  自定义组件
  * directives  
  自定义指令
  * router  
  菜单路由
  * store  
  基于vuex的全局状态管理
  * util  
  自定义js工具库
  * views  
  自定义页面
* .env, .env.development, .env.production  
系统环境参数
* vue.config.js  
配置与后端交互的跨域代理

### 自定义组件 
基于Element Plus的常用组件进行系统级整合

#### 全局布局组件：components/AdminLayout.vue 
定义页面整体的布局、AJAX登录和登出、左侧菜单树的数据加载等

#### 系统菜单分组组件：components/MenuGroup.vue 
定义左侧菜单树的页面渲染逻辑

#### 分页表格组件：components/PaginationTable.vue 
对表格组件和分页组件进行整合

#### 图标选择器组件：components/IconSelector.vue 
支持快速选择图标

### 自定义指令 
#### v-hasAllFunctions与v-hasAnyFunctions，判断当前用户是否拥有指定的权限功能 
```html
<!--是否同时拥有admin_menu_operation与admin_menutree_load_for_all_user这两种功能权限-->
<div v-hasAllFunctions="['admin_menu_operation', 'admin_menutree_load_for_all_user']" v-show="false">
    当前角色满足：v-hasAllFunctions="['admin_menu_operation', 'admin_menutree_load_for_all_user']"
</div>

<!--是否拥有admin_menu_operation与admin_menutree_load_for_all_user中任意一个功能权限-->
<div v-hasAnyFunctions="['admin_menu_operation', 'admin_menutree_load_for_all_user']" v-show="false">
    当前角色满足：v-hasAnyFunctions="['admin_menu_operation', 'admin_menutree_load_for_all_user']"
</div>
```
> 建议同时加入v-show="false"，让页面在初始渲染时不显示html标签