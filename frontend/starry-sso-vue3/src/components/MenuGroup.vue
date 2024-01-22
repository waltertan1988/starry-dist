<template>
    <template v-for="item in menuList" :key="item.rowType + '-' + item.code">
        <!-- 菜单分组 -->
        <el-sub-menu :index="item.rowType + '-' + item.code" v-if="item.rowType === 1">
            <template #title>
                <el-icon v-if="item.config">
                    <component :is="item.config.icon"></component>
                </el-icon>
                <span>{{ item.name }}</span>
            </template>

            <!-- 递归遍历子项列表-->
            <MenuGroup :menuList="item.resourceGroupVoList"></MenuGroup>
        </el-sub-menu>

        <!-- 菜单项 -->
        <el-menu-item :index="item.pattern" v-if="item.rowType === 0">
            <el-icon v-if="item.config">
                <component :is="item.config.icon"></component>
            </el-icon>
            <span>{{ item.name }}</span>
        </el-menu-item>
    </template>
</template>

<script>
    export default {
        name: "MenuGroup",
        props: {
            menuList: Array
        }
    }
</script>

<style scoped>

</style>