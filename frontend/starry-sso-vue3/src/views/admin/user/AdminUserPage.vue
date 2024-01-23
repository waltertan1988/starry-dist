<template>
  <AdminLayout>
    <div class="blank5"></div>
    <el-row justify="start">
      <el-col :span="6">
        <el-text tag="b">账号：</el-text>
        <el-input name="username" v-model="usernameRef" style="width: 200px;" placeholder="请输入账号" clearable/>
      </el-col>

      <el-col :span="6">
        <el-text tag="b">昵称：</el-text>
        <el-input name="nickname" v-model="nicknameRef" style="width: 200px;" placeholder="请输入昵称" clearable/>
      </el-col>

      <el-col :span="4">
        <el-text tag="b">启用：</el-text>
        <el-select v-model="enabledRef" style="width: 100px;" placeholder="请选择">
          <el-option v-for="item in [{value: '', label: '全部'}, {value: true, label: '是'}, {value: false, label: '否'}]"
                     :key="item.value" :label="item.label" :value="item.value"/>
        </el-select>
      </el-col>
    </el-row>

    <div class="blank5"></div>
    <el-row justify="start">
      <el-col :span="12">
        <el-text tag="b">创建时间：</el-text>
        <el-date-picker type="datetime" v-model="createTimeBeginRef" placeholder="开始时间" format="YYYY-MM-DD HH:mm:ss" clearable
                        :disabled-date="d => d.getTime() > Date.now()"/>
        <el-date-picker type="datetime" v-model="createTimeEndRef" placeholder="结束时间" format="YYYY-MM-DD HH:mm:ss" clearable
                        :disabled-date="d => d.getTime() > Date.now()"/>
      </el-col>

      <el-col :span="2">
        <el-button type="primary" :icon="Search" @click="searchUserList">查询</el-button>
      </el-col>
    </el-row>

    <div class="blank10"></div>
    <el-row justify="start">
      <el-col :span="2">
        <el-button type="success" :icon="Plus" plain bg @click="openOpDialog('create')">新增</el-button>
      </el-col>

      <el-col :span="2">
        <el-button type="danger" :icon="Delete" plain bg @click="deleteUser(selectedRowDataListRef)"
                   :disabled="selectedRowDataListRef.length === 0">删除</el-button>
      </el-col>
    </el-row>

    <div class="blank10"></div>

    <div>
      <!-- 通过给组件绑定并更新:key属性值，可实现触发重新渲染组件（包括执行相应的生命周期函数，计算属性，watch等） -->
      <PaginationTable ref="userPaginationTableRef" :page="userDataPageRef" :key="userDataPageRef.componentKey" :loading="userDataLoadingRef"
                       :doSearch="searchUserList" :on-selection-change="onTableSelectionChange">
        <el-table-column type="selection" width="40"/>
        <el-table-column prop="username" label="账号" width="280"/>
        <el-table-column prop="nickname" label="昵称" width="180"/>
        <el-table-column prop="enabled" label="启用" width="60" :formatter="(row, column, cellValue) => cellValue ? '是':'否'"/>
        <el-table-column prop="accountExpired" label="已过期" width="70" :formatter="(row, column, cellValue) => cellValue ? '是':'否'"/>
        <el-table-column prop="accountLocked" label="已锁定" width="70" :formatter="(row, column, cellValue) => cellValue ? '是':'否'"/>
        <el-table-column prop="credentialsExpired" label="密码过期" width="85" :formatter="(row, column, cellValue) => cellValue ? '是':'否'"/>
        <el-table-column prop="oauth2RegistrationId" label="OAuth2注册ID" width="120"/>
        <el-table-column prop="openId" label="OpenId" width="120"/>
        <el-table-column prop="createTime" label="创建时间" width="160" :formatter="(row, column, cellValue) => DateUtil.dateToString(cellValue)"/>
        <el-table-column prop="updateTime" label="修改时间" width="160" :formatter="(row, column, cellValue) => DateUtil.dateToString(cellValue)"/>
        <el-table-column label="操作" width="80" fixed="right">
          <template v-slot="scope"><!--定义作用域插槽scope，用于访问子组件的数据属性-->
            <el-dropdown>
            <span class="el-dropdown-link">
              更多
              <el-icon class="el-icon--right">
                <MoreFilled />
              </el-icon>
            </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item :icon="Edit" @click="openOpDialog('update', scope.row)">修改</el-dropdown-item>
                  <el-dropdown-item :icon="UserFilled" @click="openOpDialog('role', scope.row)">分配角色</el-dropdown-item>
                  <el-dropdown-item :icon="Remove" @click="openOpDialog('removeSession', scope.row)">踢出会话</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </PaginationTable>
    </div>
  </AdminLayout>

  <!-- 操作对话框 -->
  <el-dialog v-model="opDialogAttrRef.visible" :title="opDialogAttrRef.title" :width="opDialogAttrRef.width"
             :close-on-click-modal="false" :destroy-on-close="true">
    <component :is="opDialogAttrRef.component" :selectedRow="opRowDataRef"
               @submitSuccess="opDialogAttrRef.visible = false; searchUserList();"></component>
  </el-dialog>
</template>

<script setup>
import {h, ref, shallowRef} from "vue";
import {Search, Plus, Edit, Delete, UserFilled, MoreFilled, Remove} from '@element-plus/icons-vue';
import HttpApi from "@/util/api";
import DateUtil from "@/util/date";
import {ElMessage, ElMessageBox} from "element-plus";
import Pagination from "@/util/pagination";
import AdminLayout from "@/components/AdminLayout";
import PaginationTable from "@/components/PaginationTable";
import AdminUserSavePage from "@/views/admin/user/AdminUserSavePage.vue";
import AdminUserRolePage from "@/views/admin/user/AdminUserRolePage.vue";
import AdminUserSessionPage from "@/views/admin/user/AdminUserSessionPage.vue";

/** 组件定义段 */

/** 数据模型段 */
// 查询条件参数
const usernameRef = ref()
const nicknameRef = ref()
const enabledRef = ref("")
const createTimeBeginRef = ref()
const createTimeEndRef = ref()

// PaginationTable组件自身的引用，用于调用其内部方法
const userPaginationTableRef = ref()
// 用户分页数据
const userDataPageRef = ref(Pagination.toElPagination())
// 用户表格数据是否加载中
const userDataLoadingRef = ref(false)
// 当前操作目标行的数据
const opRowDataRef = ref(null)
// 表格当前的选中行数据集合
const selectedRowDataListRef = ref([])

// 操作对话框属性
const opDialogAttrRef = ref({visible: false, title: null, width: null ,component: null})

/** setup执行段 */
// 立即触发查询
searchUserList()

/** 方法定义段 */
/**
 * 查询用户列表
 * @param currentPage 由PaginationTable触发回调的分页页码
 * @param pageSize 由PaginationTable触发回调的分页每页显示条数
 */
function searchUserList(currentPage, pageSize) {
  if(currentPage && Number.isInteger(currentPage)){
    userDataPageRef.value.currentPage = currentPage
  }
  if(pageSize && Number.isInteger(pageSize)){
    userDataPageRef.value.pageSize = pageSize
  }

  let data = {
    username: usernameRef.value,
    nickname: nicknameRef.value,
    enabled: enabledRef.value === '' ? null : enabledRef.value,
    createTimeBegin: createTimeBeginRef.value,
    createTimeEnd: createTimeEndRef.value,
    pageNumber: userDataPageRef.value.currentPage ? userDataPageRef.value.currentPage - 1 : 0,
    pageSize: userDataPageRef.value.pageSize ? userDataPageRef.value.pageSize : Pagination.DEFAULT_EL_PAGE_SIZE,
  }

  const config = {
    headers:{
      'Content-Type': 'application/json'
    }
  }

  HttpApi.post('/api/auth/admin/user/list', data, config,
      response => {
        userDataPageRef.value = Pagination.toElPagination(response.data)
        // 还原当前操作行和选中行
        opRowDataRef.value = null
        selectedRowDataListRef.value = []
      },
      response => {
        ElMessage.error(response.errMsg)
      },
      err => {
        ElMessage.error(err)
      },
      () => {
        userDataLoadingRef.value = true
      },
      () => {
        userDataLoadingRef.value = false
      })
}

/**
 * 当表格选择项发生变化后触发的回调方法（主要用于多选情况）
 * @param selectedRows
 */
function onTableSelectionChange(selectedRows){
  selectedRowDataListRef.value = selectedRows;
}

/**
 * 打开操作对话框（只针对单行数据）
 * @param opType
 * @param row 操作的表格行
 */
function openOpDialog(opType, row) {
  if(opType === 'create'){
    // 新增用户
    opRowDataRef.value = null
    opDialogAttrRef.value.title = "新增用户"
    opDialogAttrRef.value.width = "50%"
    opDialogAttrRef.value.component = shallowRef(AdminUserSavePage)

  }else if(opType === 'update'){
    // 修改用户
    opRowDataRef.value = row
    opDialogAttrRef.value.title = "修改用户"
    opDialogAttrRef.value.width = "50%"
    opDialogAttrRef.value.component = shallowRef(AdminUserSavePage)

  }else if(opType === 'role'){
    opRowDataRef.value = row
    opDialogAttrRef.value.title = "分配角色"
    opDialogAttrRef.value.width = "50%"
    opDialogAttrRef.value.component = shallowRef(AdminUserRolePage)

  }else if(opType === 'removeSession'){
    // 踢出会话
    opRowDataRef.value = row
    opDialogAttrRef.value.title = "踢出会话"
    opDialogAttrRef.value.width = "70%"
    opDialogAttrRef.value.component = shallowRef(AdminUserSessionPage)
  }

  // 显示对话框
  opDialogAttrRef.value.visible = true
}

/**
 * 删除用户
 * @param rows
 */
function deleteUser(rows){
  if(!rows || rows.length <= 0){
    return
  }

  let deleteUsernames = []
  let usernameMessageList = []
  rows.forEach(r => {
    deleteUsernames.push(r.username)

    let userMsg = {}
    userMsg[r.username] = r.nickname
    usernameMessageList.push(userMsg)
  })

  ElMessageBox({
    title: '你确定要删除以下用户吗？',
    type: 'warning',
    closeOnClickModal: false,
    message: h('span', {style: 'color: red'}, JSON.stringify(usernameMessageList)),
    showCancelButton: true,
    confirmButtonText: '确定',
    cancelButtonText: '取消',
  }).then(() => {
    const config = {
      headers:{
        'Content-Type': 'application/json'
      }
    }

    HttpApi.post("/api/auth/admin/user/delete", {usernameList: deleteUsernames}, config,
        () => {
          ElMessage.success("删除成功")
          searchUserList()
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
.el-dropdown-link {
  cursor: pointer;
  color: var(--el-color-primary);
  display: flex;
  align-items: center;
}
</style>