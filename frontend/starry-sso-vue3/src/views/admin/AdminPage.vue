<template>
    <AdminLayout>
      <h1>管理台首页</h1>

      <div v-hasAllFunctions="['admin_menu_operation', 'admin_menutree_load_for_all_user']" v-show="false">
        当前角色满足：v-hasAllFunctions="['admin_menu_operation', 'admin_menutree_load_for_all_user']"
      </div>

      <div v-hasAnyFunctions="['admin_menu_operation', 'admin_menutree_load_for_all_user']" v-show="false">
        当前角色满足：v-hasAnyFunctions="['admin_menu_operation', 'admin_menutree_load_for_all_user']"
      </div>
    </AdminLayout>
</template>

<script setup>
import AdminLayout from "@/components/AdminLayout";

/** 组件定义段 */

/** 数据模型段 */

/** setup执行段 */

/** 方法定义段 */

</script>

<style scoped>

</style>