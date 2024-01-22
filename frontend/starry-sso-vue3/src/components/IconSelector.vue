<template>
  <el-popover ref="elPopoverRef" :width="600" trigger="click" placement="right">
    <template #reference>
      <div>
        <div style="cursor: pointer;" v-if="selectedIconKeyRef">
          <el-row>
            <el-col :span="1">
              <el-icon size="20">
                <component :is="selectedIconKeyRef"></component>
              </el-icon>
            </el-col>
            <el-col :push="22" :span="1">{{selectedIconKeyRef}}</el-col>
          </el-row>
        </div>
        <el-button type="primary" size="small" plain v-else>选择</el-button>
      </div>
    </template>
    <template #default>
      <div>
        <el-scrollbar height="200px">
          <template v-for="iconKey in iconListRef" :key="iconKey">
            <el-icon :size="20" style="cursor: pointer;" :title="iconKey" @click="choose(iconKey)">
              <component :is="iconKey"></component>
            </el-icon>
          </template>
        </el-scrollbar>
      </div>
      <div class="blank10"></div>
      <div style="text-align: center;">
        <el-button size="small" type="danger" plain @click="choose(null)">重置</el-button>
        <el-button size="small" type="info" plain @click="elPopoverRef.hide()">关闭</el-button>
      </div>
    </template>
  </el-popover>
</template>

<script setup>
import {ref} from "vue";
import {defineEmits, defineProps} from '@vue/runtime-core';
import * as ElementPlusIconsVue from "@element-plus/icons-vue";

/** 组件定义段 */
// 定义组件用于接收外部调用方的参数列表
const props = defineProps(['iconKey'])
// 自定义的选中事件
const emit = defineEmits(['onSelected'])

/** 数据模型段 */
// el-popover组件引用
const elPopoverRef = ref()
// 选中的图标key值
const selectedIconKeyRef = ref(props.iconKey)

/** setup执行段 */
const iconListRef = ref(Object.keys(ElementPlusIconsVue))

/** 方法定义段 */
/**
 * 点击选中图标项
 * @param iconKey
 */
function choose(iconKey){
  selectedIconKeyRef.value = iconKey
  elPopoverRef.value.hide()
  emit('onSelected', iconKey)
}
</script>

<style scoped>

</style>