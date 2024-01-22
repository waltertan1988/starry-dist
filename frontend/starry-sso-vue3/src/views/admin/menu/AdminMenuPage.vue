<template>
  <AdminLayout>
    <div style="text-align: left">
      <div class="blank5"></div>
      <el-row justify="start">
        <el-col :span="7">
          <el-text tag="b">菜单行名称：</el-text>
          <el-input v-model="nameRef" style="width: 200px;" placeholder="请输入菜单行名称" clearable/>
        </el-col>

        <el-col :span="7">
          <el-text tag="b">菜单行编码：</el-text>
          <el-input v-model="codeRef" style="width: 200px;" placeholder="请输入菜单行编码" clearable/>
        </el-col>

        <el-col :span="4">
          <el-text tag="b">类型：</el-text>
          <el-select v-model="rowTypeRef" style="width: 100px;" placeholder="请选择">
            <el-option v-for="item in [{value: '', label: '全部'}, {value: 0, label: '菜单项'}, {value: 1, label: '菜单组'}]"
                       :key="item.value" :label="item.label" :value="item.value"/>
          </el-select>
        </el-col>

        <el-col :span="4" :push="1">
          <el-button type="primary" :icon="Search" @click="searchMenuList">查询</el-button>
        </el-col>
      </el-row>
    </div>

    <div class="blank10"></div>
    <el-row justify="start">
      <el-col :span="3">
        <el-button type="success" :icon="FolderAdd" :disabled="selectedRowRef == null || selectedRowRef.rowType === 0" plain bg @click="openOpDialog('createGroup')">添加菜单组</el-button>
      </el-col>
      <el-col :span="3">
        <el-button type="primary" :icon="Plus" :disabled="selectedRowRef == null || selectedRowRef.rowType === 0" plain bg @click="openOpDialog('createItem')">添加菜单项</el-button>
      </el-col>
      <el-col :span="3">
        <el-button type="info" :icon="Sort" :disabled="selectedRowRef == null || selectedRowRef.rowType === 0" plain bg @click="toggleSubTreeExpansion()">展开/折叠</el-button>
      </el-col>
    </el-row>

    <div class="blank10"></div>
    <div>
      <el-table ref="tableRef" :data="menuDataListRef" :row-key="getTableRowKey" :tree-props="{children: 'resourceGroupVoList'}"
                border highlight-current-row v-loading="menuDataLoadingRef" @row-click="onRowClick">
        <el-table-column prop="name" label="菜单行名称" width="200" fixed/>
        <el-table-column prop="code" label="菜单行编码" width="250" fixed/>
        <el-table-column prop="seq" label="顺序" width="65"/>
        <el-table-column prop="rowType" label="菜单路径" :formatter="(row, column, cellValue) => cellValue === 1 ? null : row.pattern" width="200"/>
        <el-table-column prop="createTime" label="创建时间" :formatter="(row, column, cellValue) => DateUtil.dateToString(cellValue)" width="160"/>
        <el-table-column prop="updateTime" label="修改时间" :formatter="(row, column, cellValue) => DateUtil.dateToString(cellValue)" width="160"/>
        <el-table-column label="操作" width="75" fixed="right">
          <template v-slot="scope"><!--定义作用域插槽scope，用于访问子组件的数据属性-->
            <el-dropdown v-if="!scope.row.systemAuthority">
                <span class="el-dropdown-link">
                  更多
                  <el-icon class="el-icon--right">
                    <MoreFilled />
                  </el-icon>
                </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <template v-if="scope.row.rowType === 1">
                    <el-dropdown-item :icon="Edit" @click="openOpDialog('updateGroup', scope.row)">编辑</el-dropdown-item>
                    <el-dropdown-item :icon="Scissor" @click="moveSubTree(scope.row)">移动</el-dropdown-item>
                    <el-dropdown-item :icon="Delete" @click="deleteSubTree(scope.row)">删除</el-dropdown-item>
                  </template>
                  <template v-else>
                    <el-dropdown-item :icon="Edit" @click="openOpDialog('updateItem', scope.row)">编辑</el-dropdown-item>
                    <el-dropdown-item :icon="Key" @click="openOpDialog('setAuthority', scope.row)">设置权限</el-dropdown-item>
                    <el-dropdown-item :icon="Scissor" @click="moveSubTree(scope.row)">移动</el-dropdown-item>
                    <el-dropdown-item :icon="Delete" @click="deleteSubTree(scope.row)">删除</el-dropdown-item>
                  </template>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </AdminLayout>

  <!-- 操作对话框 -->
  <el-dialog v-model="opDialogAttrRef.visible" :title="opDialogAttrRef.title" :width="opDialogAttrRef.width"
             :close-on-click-modal="false" :destroy-on-close="true">
    <component :is="opDialogAttrRef.component" :selectedRow="opRowDataRef" :componentParam="opDialogAttrRef.componentParam"
               @submitSuccess="opDialogAttrRef.visible = false; searchMenuList();"></component>
  </el-dialog>
</template>

<script setup>
import {ref, shallowRef} from "vue";
import {Delete, Edit, FolderAdd, Key, MoreFilled, Plus, Scissor, Search, Sort} from "@element-plus/icons-vue";
import AdminLayout from "@/components/AdminLayout";
import AdminMenuGroupSavePage from "@/views/admin/menu/AdminMenuGroupSavePage.vue";
import AdminMenuItemSavePage from "@/views/admin/menu/AdminMenuItemSavePage.vue";
import AdminMenuSetAuthorityPage from "@/views/admin/menu/AdminMenuSetAuthorityPage.vue";
import DateUtil from "@/util/date";
import HttpApi from "@/util/api";
import {ElMessage, ElMessageBox} from "element-plus";


/** 组件定义段 */

/** 数据模型段 */
// 查询菜单行名称
const nameRef = ref()
// 查询菜单行编码
const codeRef = ref();
// 查询菜单行类型
const rowTypeRef = ref()

// el-table引用
const tableRef = ref()
// 菜单列表数据
const menuDataListRef = ref([])
// 数据是否加载中
const menuDataLoadingRef = ref(false)

// 单选选中的角色行
const selectedRowRef = ref()
// 当前操作目标行的数据
const opRowDataRef = ref(null)
// 操作对话框属性
const opDialogAttrRef = ref({visible: false, title: null, width: null, component: null, componentParam: null})

/** setup执行段 */
searchMenuList()

/** 方法定义段 */
function searchMenuList(){
  const data = {
    rootResourceGroupCode: process.env.VUE_APP_ROOT_MENU_GROUP,
    code: codeRef.value,
    name: nameRef.value,
    rowType: rowTypeRef.value === '' ? null : rowTypeRef.value,
  }

  const config = {
    headers:{
      'Content-Type': 'application/json'
    }
  }

  HttpApi.post('/api/auth/admin/menu/list', data, config,
      response => {
        menuDataListRef.value = response.data
      },
      response => {
        ElMessage.error(response.errMsg)
      },
      err => {
        ElMessage.error(err)
      },
      () => {
        menuDataLoadingRef.value = true
      },
      () => {
        menuDataLoadingRef.value = false
      })
}

/**
 * table计算rowKey方法
 * @param row
 */
function getTableRowKey(row){
  if(row.rowType === 0){
    return `item@${row.code}`
  }else{
    return `group@${row.code}`
  }
}

/**
 * 处理数据行的单击事件
 * @param row
 */
function onRowClick(row){
  if(selectedRowRef.value && row.code === selectedRowRef.value.code){
    // 取消选中
    selectedRowRef.value = null
    tableRef.value.setCurrentRow()
    return
  }
  selectedRowRef.value = row
}

/**
 * 切换表格行子树的折叠或展开
 * @param row 待切换的行数据
 * @param needExpend 是否需要设置为展开
 */
async function toggleSubTreeExpansion(row, needExpend){
  let toggleRow = row ? row : selectedRowRef.value

  if(toggleRow){
    toggleRow.isExpended = needExpend ? needExpend : !toggleRow.isExpended
    await tableRef.value.toggleRowExpansion(toggleRow, toggleRow.isExpended)

    if(toggleRow.resourceGroupVoList && toggleRow.resourceGroupVoList.length > 0){
      // 递归处理下级角色
      await toggleRow.resourceGroupVoList.forEach(child => toggleSubTreeExpansion(child, toggleRow.isExpended))
    }

    // 触发折叠/展开后，不知道为何当前选中行的高亮样式会消失，因此要重新设置发选中高亮样式
    tableRef.value.setCurrentRow(selectedRowRef.value)
  }
}

/**
 * 打开操作对话框（只针对单行数据）
 * @param opType 操作类型
 * @param row 操作的表格行
 */
function openOpDialog(opType, row) {
  if(opType === 'createGroup'){
    opRowDataRef.value = selectedRowRef.value
    opDialogAttrRef.value.title = "添加菜单组"
    opDialogAttrRef.value.width = "50%"
    opDialogAttrRef.value.component = shallowRef(AdminMenuGroupSavePage)

  }else if(opType === 'updateGroup'){
    opRowDataRef.value = row
    opDialogAttrRef.value.title = "编辑菜单组"
    opDialogAttrRef.value.width = "50%"
    opDialogAttrRef.value.component = shallowRef(AdminMenuGroupSavePage)

  }else if(opType === 'createItem'){
    opRowDataRef.value = selectedRowRef.value
    opDialogAttrRef.value.title = "添加菜单项"
    opDialogAttrRef.value.width = "50%"
    opDialogAttrRef.value.component = shallowRef(AdminMenuItemSavePage)

  }else if(opType === 'updateItem'){
    opRowDataRef.value = row
    opDialogAttrRef.value.title = "编辑菜单项"
    opDialogAttrRef.value.width = "50%"
    opDialogAttrRef.value.component = shallowRef(AdminMenuItemSavePage)

  }else if(opType === 'setAuthority'){
    opRowDataRef.value = row
    opDialogAttrRef.value.title = "设置权限"
    opDialogAttrRef.value.width = "50%"
    opDialogAttrRef.value.component = shallowRef(AdminMenuSetAuthorityPage)
  }

  opDialogAttrRef.value.componentParam = {opType}

  // 显示对话框
  opDialogAttrRef.value.visible = true
}

/**
 * 移动菜单子树（菜单组和(或)菜单项）
 * @param row
 */
function moveSubTree(row){
  let selectedRow = selectedRowRef.value;

  if(!selectedRow){
    ElMessage.error("请先选择挂载分组")
    return
  }

  if(selectedRow.rowType !== 1){
    ElMessage.error("选中的挂载项不是菜单分组")
    return
  }

  if(selectedRow.code === row.code){
    ElMessage.error("不能移动到自己下面")
    return
  }

  ElMessageBox.confirm(
      `你确定要把【${row.name}：${row.code}】${row.rowType === 1 ? '及其下所有菜单行' : ''}，移动到【${selectedRow.name}：${selectedRow.code}】下面吗？`,
      '提示',
      {confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'}
  ).then(() => {
    const config = {
      headers:{
        'Content-Type': 'application/json'
      }
    }

    const data = {
      code: row.code,
      rowType: row.rowType,
      moveToGroupCode: selectedRow.code
    }

    HttpApi.post("/api/auth/admin/menu/move", data, config,
        () => {
          ElMessage({type: 'success', message: '移动成功'})
          searchMenuList()
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
  }).catch(err => {
    console.log(err)
  })
}

/**
 * 删除菜单子树
 * @param row
 */
function deleteSubTree(row){
  ElMessageBox.confirm(
      `你确定要删除【${row.name}：${row.code}】${row.rowType === 1 ? '及其下所有菜单行' : ''}吗？`,
      '提示',
      {confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'}
  ).then(() => {
    const config = {
      headers:{
        'Content-Type': 'application/json'
      }
    }

    HttpApi.post(`/api/auth/admin/menu/delete?rowType=${row.rowType}&code=${row.code}`, null, config,
        () => {
          ElMessage({type: 'success', message: '删除成功'})
          searchMenuList()
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