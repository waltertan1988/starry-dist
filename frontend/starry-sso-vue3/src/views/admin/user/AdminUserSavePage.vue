<template>
  <el-form ref="ruleFormRef" :model="ruleForm" :rules="rules" status-icon>
    <el-row v-if="selectedRowRef">
      <el-col :offset="4" :span="2">
        <el-text tag="b">ID：</el-text>
      </el-col>
      <el-col :span="12">
        <el-form-item>
          <el-input name="id" v-model="selectedRowRef.id" style="width: 300px;" disabled clearable/>
        </el-form-item>
      </el-col>
    </el-row>

    <el-row v-if="selectedRowRef">
      <el-col :offset="4" :span="2">
        <el-text tag="b">账号：</el-text>
      </el-col>
      <el-col :span="12">
        <el-form-item>
          <el-input name="username" v-model="selectedRowRef.username" style="width: 300px;" disabled clearable/>
        </el-form-item>
      </el-col>
    </el-row>

    <el-row>
      <el-col :offset="4" :span="2">
        <el-text tag="b">昵称：</el-text>
      </el-col>
      <el-col :span="12">
        <el-form-item prop="nickname">
          <el-input name="nickname" v-model="ruleForm.nickname" style="width: 300px;" placeholder="请输入昵称" clearable/>
        </el-form-item>
      </el-col>
    </el-row>

    <el-row>
      <el-col :offset="4" :span="2">
        <el-text tag="b">启用：</el-text>
      </el-col>
      <el-col :span="8">
        <el-form-item>
          <el-select v-model="ruleForm.enabled" style="width: 100px;" placeholder="请选择">
            <el-option v-for="item in [{value: true, label: '是'}, {value: false, label: '否'}]"
                       :key="item.value" :label="item.label" :value="item.value"/>
          </el-select>
        </el-form-item>
      </el-col>
    </el-row>

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
const props = defineProps(['selectedRow'])
// 自定义关闭close事件
const emit = defineEmits(['submitSuccess'])

/** 数据模型段 */
// el-form表单引用
const ruleFormRef = ref()
// 当前操作行数据
const selectedRowRef = ref(props.selectedRow)
// 表单数据
const ruleForm = reactive({
  username: selectedRowRef.value ? selectedRowRef.value.username : null,
  nickname: selectedRowRef.value ? selectedRowRef.value.nickname : null,
  enabled: selectedRowRef.value ? selectedRowRef.value.enabled : true
})
// 表单校验规则
const rules = reactive({
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur', pattern: /\S+/ },
    { min: 1, max: 255, message: '不能超过255字符', trigger: 'blur' },
  ],
})

/** setup执行段 */

/** 方法定义段 */
// 保存
function save(){
  if(!ruleFormRef.value){
    return
  }

  ruleFormRef.value.validate(valid => {
    if (valid) {
      const config = {
        headers:{
          'Content-Type': 'application/json'
        }
      }
      HttpApi.post("/api/auth/admin/user/save", ruleForm, config,
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