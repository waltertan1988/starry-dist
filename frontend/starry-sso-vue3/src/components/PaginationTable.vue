<template>
    <el-table ref="elTableRef" :data="pageRef.content" :stripe="true" border :highlight-current-row="true" v-loading="loadingRef"
              @row-click="onRowClick" @selection-change="onSelectionChange">
        <slot></slot>
    </el-table>
    <div class="blank10"></div>
    <el-pagination style="justify-content: center"
        layout="total, sizes, prev, pager, next, jumper"
        v-model:current-page="pageRef.currentPage"
        v-model:page-size="pageRef.pageSize"
        :page-sizes="[Pagination.DEFAULT_EL_PAGE_SIZE, 20, 50, 100]"
        :background="true"
        :highlight-current-row="true"
        :total="pageRef.total"
    />
    <div class="blank10"></div>
</template>

<script setup>
    import {ref} from "vue";
    import {defineExpose, defineProps, watch} from '@vue/runtime-core';
    import Pagination from "@/util/pagination";

    /**
     * 定义组件用于接收外部调用方的参数列表
     *  page：分页表格的数据
     *  doSearch：列表查询方法，用于点击分页控件时触发查询
     *  loading：表格是否显示加载中的遮罩
     *  onRowClick：表格单选触发的回调方法（主要用于表格单选的情况）
     *  onSelectionChange：当表格选择项发生变化后触发的回调方法（主要用于表格多选的情况）
     */
    const props = defineProps(['page', 'doSearch', 'loading', 'onRowClick', 'onSelectionChange'])

    /**
     * 定义组件向外暴露的方法
     */
    defineExpose({
        /**
         * 用于单选表格，设定某一行为选中行，如果调用时不加参数，则会取消目前高亮行的选中状态
         * @param row
         */
        setCurrentRow: function (row) {
            elTableRef.value.setCurrentRow(row)
        }
    })

    // el-table组件自身的引用，用于调用其内部方法
    const elTableRef = ref()
    // 分页数据
    const pageRef = ref(props.page)
    // 数据是否加载中
    const loadingRef = ref(props.loading)

    // 监听数据是否加载中
    watch(
        () => props.loading,
        newVal => {
            loadingRef.value = newVal
        }
    )

    // 监听页码的变化
    watch(
        () => pageRef.value.currentPage,
        (newCurrentPage, oldCurrentPage) => {
            if(Number.isInteger(newCurrentPage) && newCurrentPage !== oldCurrentPage){
                props.doSearch(newCurrentPage)
            }
        }
    )

    // 监听每页显示条数的变化
    watch(
        () => pageRef.value.pageSize,
        (newPageSize, oldPageSize) => {
            if(Number.isInteger(newPageSize) && newPageSize !== oldPageSize){
                props.doSearch(null, newPageSize)
            }
        }
    )

    function onRowClick(row, column, event) {
        if(props.onRowClick){
            props.onRowClick(row, column, event)
        }
    }

    function onSelectionChange(selectedRows){
      if(props.onSelectionChange){
        props.onSelectionChange(selectedRows)
      }
    }
</script>

<style scoped>

</style>