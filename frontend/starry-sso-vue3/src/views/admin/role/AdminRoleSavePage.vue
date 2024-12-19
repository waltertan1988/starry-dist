<template>
  <el-form ref="ruleFormRef" :model="ruleForm" :rules="rules" status-icon>
    <el-row>
      <el-col :offset="4" :span="4">
        <el-text tag="b">角色编码：</el-text>
      </el-col>
      <el-col :span="12">
        <el-form-item prop="code">
          <el-input name="code" v-model="ruleForm.code" style="width: 300px;" placeholder="请输入角色编码，以ROLE_开头" clearable/>
        </el-form-item>
      </el-col>
    </el-row>

    <el-row>
      <el-col :offset="4" :span="4">
        <el-text tag="b">角色名称：</el-text>
      </el-col>
      <el-col :span="12">
        <el-form-item prop="name">
          <el-input name="name" v-model="ruleForm.name" style="width: 300px;" placeholder="请输入角色名称" clearable/>
        </el-form-item>
      </el-col>
    </el-row>

    <el-row>
      <el-col :offset="4" :span="4">
        <el-text tag="b">顺序：</el-text>
      </el-col>
      <el-col :span="12">
        <el-form-item prop="priority">
          <el-input-number v-model="ruleForm.priority" controls-position="right"/>
        </el-form-item>
      </el-col>
    </el-row>

    <el-row>
      <el-col :offset="4" :span="4">
        <el-text tag="b">系统角色：</el-text>
      </el-col>
      <el-col :span="1">
        <el-text>否</el-text>
      </el-col>
    </el-row>

    <div class="blank20"></div>
    <el-row>
      <el-col :offset="4" :span="4">
        <el-text tag="b">上级角色编码：</el-text>
      </el-col>
      <el-col :span="12" style="text-align: left;">
        <template v-if="componentParamRef.opType === 'create'">{{selectedRowRef.code}}</template>
        <template v-else>{{selectedRowRef.parentCode}}</template>
      </el-col>
    </el-row>

    <div class="blank20"></div>

    <el-row>
      <el-col>
        <el-button type="primary" @click="save">保存</el-button>
      </el-col>
    </el-row>
  </el-form>
</template>

<script setup>
import {reactive, ref} from "vue";
import {defineEmits, defineProps} from '@vue/runtime-core';
import {ElMessage} from "element-plus";
import HttpApi from "@/util/api";

/** 组件定义段 */
// 定义组件用于接收外部调用方的参数列表
const props = defineProps(['selectedRow', 'componentParam'])
// 自定义关闭close事件
const emit = defineEmits(['submitSuccess'])

/** 数据模型段 */
// el-form表单引用
const ruleFormRef = ref()
// 当前操作行数据
const selectedRowRef = ref(props.selectedRow)
// 组件参数
const componentParamRef = ref(props.componentParam)
// 表单数据
const ruleForm = reactive({
  id: componentParamRef.value.opType === 'create' ? null : selectedRowRef.value.id,
  code: componentParamRef.value.opType === 'create' ? null : selectedRowRef.value.code,
  name: componentParamRef.value.opType === 'create' ? null : selectedRowRef.value.name,
  parentCode: componentParamRef.value.opType === 'create' ? selectedRowRef.value.code : selectedRowRef.value.parentCode,
  priority: componentParamRef.value.opType === 'create' ? 1000 : selectedRowRef.value.priority,
  systemAuthority: false
})
// 表单校验规则
const rules = reactive({
  code: [
    { required: true, message: '请输入角色编码', trigger: 'blur', pattern: /\S+/ },
    { min: 1, max: 128, message: '不能超过128字符', trigger: 'blur' },
  ],
  name: [
    { required: true, message: '请输入角色名称', trigger: 'blur', pattern: /\S+/ },
    { min: 1, max: 255, message: '不能超过255字符', trigger: 'blur' },
  ],
  priority: [
    { required: true, message: '请输入顺序', trigger: 'blur'},
  ],
})

/** setup执行段 */

/** 方法定义段 */
// 保存
function save(){
  if(!ruleFormRef.value){
    return
  }

  // TODO 不知为何这里会报错：children must be keyed
  ruleFormRef.value.validate(valid => {
    if (valid) {
      const config = {
        headers:{
          'Content-Type': 'application/json'
        }
      }

      HttpApi.post("/api/auth/admin/role/save", ruleForm, config,
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
    } else {
      return false
    }
  })
}
</script>

<style scoped>

</style>