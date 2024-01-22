<template>
  <div>
    <el-row justify="center">
      <el-col :span="12"><el-text type="primary" size="large" tag="b">功能项编码：{{selectedRowRef.code}}</el-text></el-col>
      <el-col :span="12"><el-text type="primary" size="large" tag="b">功能项名称：{{selectedRowRef.name}}</el-text></el-col>
    </el-row>

    <div class="blank20"></div>
    <el-row justify="start">
      <el-col>
        <el-tree ref="elTreeRef" :data="rootRoleWrapperListRef.rootRoleList" :props="defaultProps" show-checkbox check-strictly node-key="code"
                 :default-checked-keys="rootRoleWrapperListRef.selectedCodeList" :default-expanded-keys="rootRoleWrapperListRef.selectedCodeList"
                 @check="onCheck"/>
      </el-col>
    </el-row>

    <div class="blank20"></div>
    <el-row>
      <el-col>
        <el-button type="primary" @click="save">保存</el-button>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import {h, reactive, ref} from "vue";
import {defineEmits, defineProps} from '@vue/runtime-core';
import {ElMessage, ElMessageBox} from "element-plus";
import HttpApi from "@/util/api";

/** 组件定义段 */
// 定义组件用于接收外部调用方的参数列表
const props = defineProps(['selectedRow', 'componentParam'])
// 自定义关闭close事件
const emit = defineEmits(['submitSuccess'])

/** 数据模型段 */
// el-tree引用
const elTreeRef = ref()
// 当前操作行数据
const selectedRowRef = ref(props.selectedRow)

// 树形控件属性到数据模型字段的映射
const defaultProps = {
  children: 'children',
  label: (data) => `${data.name} [${data.code}]`
}

// 可选择的根角色数据列表
const rootRoleWrapperListRef = reactive({rootRoleList:[], selectedCodeList:[]})
// 目标用户原来已存在的角色列表
const originalCheckedRoleList = []
// 目标用户待添加的角色列表
let newRoleList = []
// 目标用户待剔除的角色列表
let removeRoleList = []

/** setup执行段 */

// 查询权限树数据
HttpApi.post(`/api/auth/admin/function/authority/listTree?functionItemCode=${selectedRowRef.value.code}`, null, null,
    response => {
      rootRoleWrapperListRef.rootRoleList = response.data
      rootRoleWrapperListRef.selectedCodeList = findGrantedAuthorityCodes(response.data, [])

      // 记录目标用户原来已存在的角色集合
      originalCheckedRoleList.push(...rootRoleWrapperListRef.selectedCodeList)
    },
    response => {
      ElMessage.error(response.errMsg)
    },
    err => {
      ElMessage.error(err)
    }
)

/** 方法定义段 */

/**
 * 递归查找已拥有的权限编码
 * @param roleList
 * @param grantedCodes
 * @returns
 */
function findGrantedAuthorityCodes(roleList, grantedCodes){
  if(roleList && roleList.length > 0){
    roleList.forEach(role => {
      if(role.granted){
        grantedCodes.push(role.code)
      }

      if(role.children.length > 0){
        findGrantedAuthorityCodes(role.children, grantedCodes)
      }
    })
  }

  return grantedCodes
}

/**
 * 点击节点复选框之后触发
 * @param currNode 点击的节点
 * @param checkedNodes 选中的节点集合
 */
function onCheck(currNode, checkedNodes){
  const latestCheckedRoleList = checkedNodes.checkedKeys

  const originalCheckedRoleSet = new Set(originalCheckedRoleList)
  const latestCheckedRoleSet = new Set(latestCheckedRoleList)

  // 待删除列表
  removeRoleList = originalCheckedRoleList.filter(o => !latestCheckedRoleSet.has(o))
  // 待添加列表
  newRoleList = latestCheckedRoleList.filter(o => !originalCheckedRoleSet.has(o))
}

/**
 * 保存提交
 */
function save(){
  ElMessageBox({
    title: '你确定要保存变更吗？',
    type: 'warning',
    closeOnClickModal: false,
    message: h('p', null, [
      h('p', null, [
        h('span', null, "待删除："),
        h('i', {style: 'color: red'}, JSON.stringify(removeRoleList))
      ]),
      h('p', null, [
        h('span', null, "待添加："),
        h('i', {style: 'color: green'}, JSON.stringify(newRoleList))
      ])
    ]),
    showCancelButton: true,
    confirmButtonText: '确定',
    cancelButtonText: '取消',
  }).then(() => {
    const config = {
      headers:{
        'Content-Type': 'application/json'
      }
    }
    const data = {
      functionItemCode: selectedRowRef.value.code,
      newRoleCodeList: newRoleList,
      removeRoleCodeList: removeRoleList
    }
    HttpApi.post("/api/auth/admin/function/authority/grant", data, config,
        () => {
          ElMessage.success("保存成功")

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