<template>
    <AdminLayout>
      <div style="text-align: left; margin: 10px 10px 0 10px">
        <div class="blank5"></div>
        <el-row justify="start">
          <el-col :span="7">
            <el-text tag="b">角色名称：</el-text>
            <el-input v-model="nameRef" style="width: 200px;" placeholder="请输入角色名称" clearable/>
          </el-col>

          <el-col :span="7">
            <el-text tag="b">角色编码：</el-text>
            <el-input v-model="codeRef" style="width: 200px;" placeholder="请输入角色编码" clearable/>
          </el-col>

          <el-col :span="4">
            <el-text tag="b">系统角色：</el-text>
            <el-select v-model="systemAuthorityRef" style="width: 100px;" placeholder="请选择">
              <el-option v-for="item in [{value: '', label: '全部'}, {value: true, label: '是'}, {value: false, label: '否'}]"
                         :key="item.value" :label="item.label" :value="item.value"/>
            </el-select>
          </el-col>

          <el-col :span="4" :push="2">
            <el-button type="primary" :icon="Search" @click="searchRoleList">查询</el-button>
          </el-col>
        </el-row>
      </div>

      <div class="blank10"></div>
      <el-row justify="start">
        <el-col :span="3">
          <el-button type="success" :icon="Plus" :disabled="selectedRowRef == null" plain bg @click="openOpDialog('create')">新增角色</el-button>
        </el-col>
        <el-col :span="3">
          <el-button type="info" :icon="Sort" :disabled="selectedRowRef == null" plain bg @click="toggleSubTreeExpansion()">展开/折叠</el-button>
        </el-col>
      </el-row>

      <div class="blank10"></div>
      <div>
        <el-table ref="tableRef" :data="roleDataListRef" style="width: 100%;" row-key="code" border highlight-current-row v-loading="roleDataLoadingRef" @row-click="onRowClick">
          <el-table-column prop="name" label="角色名称"/>
          <el-table-column prop="code" label="角色编码" width="250"/>
          <el-table-column prop="priority" label="顺序" width="65"/>
          <el-table-column prop="systemAuthority" label="系统角色" :formatter="(row, column, cellValue) => cellValue ? '是':'否'" width="85"/>
          <el-table-column prop="createTime" label="创建时间" :formatter="(row, column, cellValue) => DateUtil.dateToString(cellValue)" width="160"/>
          <el-table-column prop="updateTime" label="修改时间" :formatter="(row, column, cellValue) => DateUtil.dateToString(cellValue)" width="160"/>
          <el-table-column label="操作" width="75">
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
                    <el-dropdown-item :icon="Edit" @click="openOpDialog('update', scope.row)">编辑</el-dropdown-item>
                    <el-dropdown-item :icon="Scissor" @click="moveSubTree(scope.row)">移动</el-dropdown-item>
                    <el-dropdown-item :icon="Delete" @click="deleteSubTree(scope.row)">删除</el-dropdown-item>
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
                 @submitSuccess="opDialogAttrRef.visible = false; searchRoleList();"></component>
    </el-dialog>
</template>

<script setup>
import {ref, shallowRef} from "vue";
import {Delete, Edit, MoreFilled, Plus, Scissor, Search, Sort} from "@element-plus/icons-vue";
import HttpApi from "@/util/api";
import {ElMessage, ElMessageBox} from "element-plus";
import DateUtil from "@/util/date";
import AdminLayout from "@/components/AdminLayout";
import AdminRoleSavePage from "@/views/admin/role/AdminRoleSavePage.vue";


/** 组件定义段 */

/** 数据模型段 */
// 查询角色编码
const codeRef = ref()
// 查询角色名称
const nameRef = ref()
// 查询是否系统角色
const systemAuthorityRef = ref()

// el-table引用
const tableRef = ref()
// 角色列表数据
const roleDataListRef = ref([])
// 数据是否加载中
const roleDataLoadingRef = ref(false)

// 单选选中的角色行
const selectedRowRef = ref()
// 当前操作目标行的数据
const opRowDataRef = ref(null)
// 操作对话框属性
const opDialogAttrRef = ref({visible: false, title: null, width: null, component: null, componentParam: null})

/** setup执行段 */
searchRoleList()

/** 方法定义段 */

/**
 * 查询角色列表
 */
function searchRoleList(){
  const data = {
    code: codeRef.value,
    name: nameRef.value,
    systemAuthority: systemAuthorityRef.value === '' ? null : systemAuthorityRef.value,
  }

  const config = {
    headers:{
      'Content-Type': 'application/json'
    }
  }

  HttpApi.post('/api/auth/admin/role/listTree', data, config,
      response => {
        roleDataListRef.value = response.data
      },
      response => {
        ElMessage.error(response.errMsg)
      },
      err => {
        ElMessage.error(err)
      },
      () => {
        roleDataLoadingRef.value = true
      },
      () => {
        roleDataLoadingRef.value = false
      })
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

    if(toggleRow.children && toggleRow.children.length > 0){
      // 递归处理下级角色
      await toggleRow.children.forEach(child => toggleSubTreeExpansion(child, toggleRow.isExpended))
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
  if(opType === 'create'){
    // 新增角色
    opRowDataRef.value = selectedRowRef.value
    opDialogAttrRef.value.title = "新增角色"
    opDialogAttrRef.value.width = "50%"
    opDialogAttrRef.value.component = shallowRef(AdminRoleSavePage)

  }else if(opType === 'update'){
    // 修改角色
    opRowDataRef.value = row
    opDialogAttrRef.value.title = "编辑角色"
    opDialogAttrRef.value.width = "50%"
    opDialogAttrRef.value.component = shallowRef(AdminRoleSavePage)

  }

  opDialogAttrRef.value.componentParam = {opType}

  // 显示对话框
  opDialogAttrRef.value.visible = true
}

/**
 * 移动角色子树
 * @param row
 */
function moveSubTree(row){
  let selectedRow = selectedRowRef.value;

  if(!selectedRow){
    ElMessage.error("请先选择挂载角色")
    return
  }

  if(selectedRow.code === row.code){
    ElMessage.error("不能移动到自己下面")
    return
  }

  ElMessageBox.confirm(
      `你确定要把【${row.name}：${row.code}】及其下所有角色，移动到【${selectedRow.name}：${selectedRow.code}】下面吗？`,
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
      moveToCode: selectedRow.code,
    }

    HttpApi.post("/api/auth/admin/role/move", data, config,
        () => {
          ElMessage({type: 'success', message: '移动成功'})
          searchRoleList()
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
 * 删除角色子树
 * @param row
 */
function deleteSubTree(row){
  ElMessageBox.confirm(
    `删除当前角色，将会级联删除其所有的下级角色，同时也会解绑与这些角色关联的用户和资源。你确定要删除【${row.name}：${row.code}】吗？`,
    '提示',
    {confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'}
  ).then(() => {
    HttpApi.post(`/api/auth/admin/role/delete?code=${row.code}`, null, null,
        () => {
          ElMessage.success("删除成功")
          searchRoleList()
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