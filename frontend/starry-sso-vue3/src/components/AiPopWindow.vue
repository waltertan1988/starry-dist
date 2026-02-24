<template>
    <!-- 可拖拽弹窗 -->
    <div v-if="popupVisible" 
         id="draggablePopup"
         :style="{ left: popupPosition.left + 'px', top: popupPosition.top + 'px', width: popupWidth + 'px', height: popupHeight + 'px' }"
         @mousedown="startDrag"
         @touchstart="startDrag">
        <div class="popup-header">
            <span>智能问答</span>
            <el-icon class="close-icon" @click.stop="closePopup"><Close/></el-icon>
        </div>
        <div class="popup-content">
            hello world
        </div>
    </div>

    <!-- 智能客服图标 -->
    <div v-else 
         id="smartServiceIcon"
         :style="{ left: popupPosition.left + 'px', top: popupPosition.top + 'px' }"
         @click="showPopup"
         @mousedown="startDrag"
         @touchstart="startDrag">
        <div class="icon-content">
            <el-icon class="service-icon"><ChatDotRound/></el-icon>
            <span class="icon-text">智能问答</span>
        </div>
    </div>
</template>

<script setup>
    import { ref } from 'vue'
    import {Close, ChatDotRound} from "@element-plus/icons-vue";
    import {defineProps} from '@vue/runtime-core';

    // 定义组件用于接收外部调用方的参数列表
    const props = defineProps(['popupWidth','popupHeight','iconWidth'])

    // 弹窗尺寸常量
    const popupWidth = ref(props.popupWidth); // 弹窗宽度
    const popupHeight = ref(props.popupHeight); // 弹窗高度
    const iconWidth = ref(props.iconWidth); // 图标宽度

    // 弹窗相关数据
    const popupVisible = ref(false); // 弹窗是否显示，默认不显示，显示智能客服图标
    const initPopupPosition = ref({
        left: window.innerWidth - iconWidth.value - 10,
        top: window.innerHeight - iconWidth.value - 80
    }); // 弹窗的默认位置，距离页面右侧10px，距离底部80px
    const popupPosition = ref({ left: initPopupPosition.value.left, top: initPopupPosition.value.top }); // 弹窗位置
    const isDragging = ref(false); // 是否正在拖拽
    const startX = ref(0); // 拖拽开始的X坐标
    const startY = ref(0); // 拖拽开始的Y坐标
    const hasMoved = ref(false); // 是否有移动，用于区分点击和拖拽
    const dragThreshold = 5; // 拖拽阈值，超过此值视为拖拽

    // 开始拖拽
    function startDrag(event) {
        isDragging.value = true;
        hasMoved.value = false;
        // 记录鼠标开始位置
        if (event.type === 'mousedown') {
            startX.value = event.clientX - popupPosition.value.left;
            startY.value = event.clientY - popupPosition.value.top;
            // 添加鼠标移动和释放事件监听
            document.addEventListener('mousemove', drag);
            document.addEventListener('mouseup', stopDrag);
        } else if (event.type === 'touchstart') {
            startX.value = event.touches[0].clientX - popupPosition.value.left;
            startY.value = event.touches[0].clientY - popupPosition.value.top;
            // 添加触摸移动和结束事件监听
            document.addEventListener('touchmove', drag);
            document.addEventListener('touchend', stopDrag);
        }
    }

    // 拖拽中
    function drag(event) {
        if (!isDragging.value) return;
        // 计算新位置
        let newLeft, newTop;
        if (event.type === 'mousemove') {
            newLeft = event.clientX - startX.value;
            newTop = event.clientY - startY.value;
        } else if (event.type === 'touchmove') {
            newLeft = event.touches[0].clientX - startX.value;
            newTop = event.touches[0].clientY - startY.value;
        }
        
        // 检测是否有实际移动
        const distance = Math.sqrt(
            Math.pow(newLeft - popupPosition.value.left, 2) + 
            Math.pow(newTop - popupPosition.value.top, 2)
        );
        if (distance > dragThreshold) {
            hasMoved.value = true;
        }
        
        // 更新位置
        popupPosition.value = {
            left: newLeft,
            top: newTop
        };
    }

    // 结束拖拽
    function stopDrag() {
        isDragging.value = false;
        // 移除事件监听
        document.removeEventListener('mousemove', drag);
        document.removeEventListener('mouseup', stopDrag);
        document.removeEventListener('touchmove', drag);
        document.removeEventListener('touchend', stopDrag);
    }

    // 关闭弹窗
    function closePopup() {
        popupPosition.value = {
            left: initPopupPosition.value.left,
            top: initPopupPosition.value.top
        };
        popupVisible.value = false;
    }

    // 显示弹窗
    function showPopup() {
        // 只有在没有移动（纯粹的点击）的情况下才显示弹窗
        if (!hasMoved.value) {
            // 调整弹窗位置，确保弹窗完整显示
            adjustPopupPosition();
            popupVisible.value = true;
        }
        // 重置hasMoved，确保下一次点击或拖拽能够正确判断
        setTimeout(() => {
            hasMoved.value = false;
        }, 100);
    }

    // 调整弹窗位置，确保弹窗完整显示
    function adjustPopupPosition() {
        const windowWidth = window.innerWidth;
        const windowHeight = window.innerHeight;
        const minMargin = 5; // 最小边距，确保弹窗边界距离页面视界至少5px
        
        // 计算弹窗的理想位置（基于图标位置）
        let newLeft = popupPosition.value.left;
        let newTop = popupPosition.value.top;
        
        // 确保弹窗不会超出右侧边界
        if (newLeft + popupWidth.value > windowWidth) {
            newLeft = windowWidth - popupWidth.value - minMargin;
        }
        
        // 确保弹窗不会超出左侧边界
        if (newLeft < minMargin) {
            newLeft = minMargin;
        }
        
        // 确保弹窗不会超出底部边界
        if (newTop + popupHeight.value > windowHeight) {
            newTop = windowHeight - popupHeight.value - minMargin;
        }
        
        // 确保弹窗不会超出顶部边界
        if (newTop < minMargin) {
            newTop = minMargin;
        }
        
        // 强制确保弹窗完全在视口内
        // 再次检查并修正，确保计算正确
        newLeft = Math.max(minMargin, Math.min(newLeft, windowWidth - popupWidth.value - minMargin));
        newTop = Math.max(minMargin, Math.min(newTop, windowHeight - popupHeight.value - minMargin));
        
        // 更新弹窗位置
        popupPosition.value = {
            left: newLeft,
            top: newTop
        };
    }
</script>

<style scoped>
    /* 可拖拽弹窗样式 */
    #draggablePopup {
        position: fixed;
        background-color: white;
        border: 1px solid #e0e0e0;
        border-radius: 4px;
        box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
        z-index: 9999;
        cursor: move;
        overflow: hidden;
    }

    .popup-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 10px 15px;
        background-color: var(--el-color-primary-light-7);
        border-bottom: 1px solid #e0e0e0;
        cursor: move;
    }

    .popup-header span {
        font-size: 14px;
        font-weight: 500;
        color: #303133;
    }

    .close-icon {
        font-size: 16px;
        color: #909399;
        cursor: pointer;
        transition: color 0.3s;
    }

    .close-icon:hover {
        color: #606266;
    }

    .popup-content {
        padding: 20px;
        font-size: 14px;
        color: #303133;
        text-align: center;
    }

    /* 智能客服图标样式 */
    #smartServiceIcon {
        position: fixed;
        width: 60px;
        height: 60px;
        background-color: #409eff;
        border-radius: 50%;
        box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.15);
        z-index: 9999;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: all 0.3s ease;
    }

    #smartServiceIcon:hover {
        transform: scale(1.05);
        box-shadow: 0 4px 16px 0 rgba(0, 0, 0, 0.2);
    }

    .icon-content {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        color: white;
    }

    .service-icon {
        font-size: 24px;
        margin-bottom: 4px;
    }

    .icon-text {
        font-size: 10px;
        font-weight: 500;
    }
</style>