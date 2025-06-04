<template>
  <div>
    <el-row justify="center">
      <el-col :span="10"><el-text type="primary" size="large" tag="b">账号：{{selectedRowRef.username}}</el-text></el-col>
      <el-col :span="10"><el-text type="primary" size="large" tag="b">昵称：{{selectedRowRef.nickname}}</el-text></el-col>
    </el-row>

    <div class="blank20"></div>

    <template v-if="userOidcListRef !== null && userOidcListRef.length > 0">
      <el-table :data="userOidcListRef" :stripe="true" style="width: 100%">
        <el-table-column property="oidcRegistrationId" label="应用注册ID"/>
        <el-table-column property="openId" label="OpenID"/>
        <el-table-column property="createTimeTs" label="创建时间" :formatter="(row, column, cellValue) => DateUtil.dateToString(cellValue)" width="160"/>
        <el-table-column property="updateTimeTs" label="更新时间" :formatter="(row, column, cellValue) => DateUtil.dateToString(cellValue)" width="160"/>
        <el-table-column label="状态" width="60">
          <template #default="scope">
            <el-switch v-model="scope.row.enabled" inline-prompt active-text="启用" inactive-text="禁用" @click="changeUserOidcEnabled(scope.row)"/>
          </template>
        </el-table-column>
      </el-table>
      <div class="blank10"></div>
    </template>
    <template v-else>
      <el-alert title="该用户未配置OIDC" type="info" effect="dark" center show-icon :closable="false"/>
    </template>
  </div>
</template>

<script setup>
import {ref} from "vue";
import {defineProps} from '@vue/runtime-core';
import HttpApi from "@/util/api";
import {ElMessage} from "element-plus";
import DateUtil from "@/util/date";

/** 组件定义段 */
// 定义组件用于接收外部调用方的参数列表
const props = defineProps(['selectedRow'])

/** 数据模型段 */
// 当前操作行数据
const selectedRowRef = ref(props.selectedRow)
// 用户的OIDC列表
const userOidcListRef = ref(null)

/** setup执行段 */
// 获取用户的OIDC列表
loadUserOidcList()



/** 方法定义段 */
/**
 * 获取用户的OIDC列表
 */
function loadUserOidcList(){
  HttpApi.get(`/api/auth/admin/user/oidc/list?username=${selectedRowRef.value.username}`,
      response => {
        userOidcListRef.value = response.data
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
 * 切换启停用状态
 * @param row
 */
function changeUserOidcEnabled(row){
  if(row.username != selectedRowRef.value.username){
    throw new Error("OIDC记录的用户与所选用户不一致")
  }

  const config = {
    headers:{
      'Content-Type': 'application/json'
    }
  }

  const data = {
    oidcRegistrationId: row.oidcRegistrationId,
    openId: row.openId,
    enabled: row.enabled
  }

  HttpApi.post('/api/auth/admin/user/oidc/changeEnabled', data, config,
      null,
      response => {
        ElMessage.error(response.errMsg)
      },
      err => {
        ElMessage.error(err)
      }
  )
}

</script>

<style scoped>

</style>