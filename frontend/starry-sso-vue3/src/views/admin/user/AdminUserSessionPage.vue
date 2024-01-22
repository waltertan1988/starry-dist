<template>
  <div>
    <el-row justify="center">
      <el-col :span="10"><el-text type="primary" size="large" tag="b">账号：{{selectedRowRef.username}}</el-text></el-col>
      <el-col :span="10"><el-text type="primary" size="large" tag="b">昵称：{{selectedRowRef.nickname}}</el-text></el-col>
    </el-row>

    <div class="blank20"></div>

    <template v-if="sessionListRef !== null && sessionListRef.length > 0">
      <el-table :data="sessionListRef" :stripe="true" style="width: 100%"
                @selection-change="onTableSelectionChange">
        <el-table-column type="selection" width="30"/>
        <el-table-column property="sessionId" label="会话ID"/>
        <el-table-column property="createTime" label="创建时间" :formatter="(row, column, cellValue) => DateUtil.dateToString(cellValue)" width="160"/>
        <el-table-column property="lastAccessedTime" label="上次访问时间" :formatter="(row, column, cellValue) => DateUtil.dateToString(cellValue)" width="160"/>
        <el-table-column property="maxInactiveInterval" label="失效间隔毫秒" width="120"/>
        <el-table-column property="expired" label="失效" :formatter="(row, column, cellValue) => cellValue ? '是':'否'" width="60"/>
      </el-table>
      <div class="blank10"></div>
      <el-button type="primary" :disabled="selectedSessionListRef.length === 0" @click="removeSessions">踢出</el-button>
    </template>
    <template v-else>
      <el-alert title="该用户未有任何会话" type="info" effect="dark" center show-icon :closable="false"/>
    </template>
  </div>
</template>

<script setup>
import {ref} from "vue";
import {defineProps, defineEmits} from '@vue/runtime-core';
import HttpApi from "@/util/api";
import {ElMessage, ElMessageBox} from "element-plus";
import DateUtil from "@/util/date";

/** 组件定义段 */
// 定义组件用于接收外部调用方的参数列表
const props = defineProps(['selectedRow'])
// 自定义关闭close事件
const emit = defineEmits(['submitSuccess'])

/** 数据模型段 */
// 当前操作行数据
const selectedRowRef = ref(props.selectedRow)
// 可选择的会话列表
const sessionListRef = ref(null)
// 选中的会话列表
const selectedSessionListRef = ref([])

/** setup执行段 */
// 加载Session列表
loadSessions()



/** 方法定义段 */
/**
 * 加载Session列表
 */
function loadSessions(){
  HttpApi.get(`/api/auth/admin/user/getSessions?username=${selectedRowRef.value.username}`,
      response => {
        sessionListRef.value = response.data
      },
      response => {
        ElMessage.error(response.errMsg)
      },
      err => {
        ElMessage.error(err)
      }
  )
}

/**
 * 当表格选择项发生变化后触发的回调方法（主要用于多选情况）
 * @param selectedRows
 */
function onTableSelectionChange(selectedRows){
  selectedSessionListRef.value = selectedRows;
}

/**
 * 踢出Session
 */
function removeSessions(){
  ElMessageBox
    .confirm(`你确定要踢出所选的${selectedSessionListRef.value.length}项吗？`, '提示', {confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'})
    .then(() => {
      const config = {
        headers:{
          'Content-Type': 'application/json'
        }
      }

      const data = {
        username: selectedRowRef.value.username,
        sessionIds: []
      }
      selectedSessionListRef.value.forEach(row => data.sessionIds.push(row.sessionId))

      HttpApi.post('/api/auth/admin/user/removeSession', data, config,
          () => {
            ElMessage.success("会话踢出成功")

            // 提交成功触发事件
            emit('submitSuccess')
          },
          response => {
            ElMessage.error(response.errMsg)
          },
          err => {
            ElMessage.error(err)
          }
      )
    }).catch(err => {
      console.log(err)
    })
}

</script>

<style scoped>

</style>