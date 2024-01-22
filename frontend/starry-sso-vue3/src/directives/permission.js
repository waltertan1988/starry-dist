import HttpApi from "@/util/api";
import {ElMessage} from "element-plus";

/**
 * 自定义权限指令：当前用户是否拥有指定的全部功能
 */
const hasAllFunctions = {
    mounted(el, binding) {
        if(binding.value && Array.isArray(binding.value) && binding.value.length > 0){
            const data = {
                type: 0,
                functionItemCodeList: binding.value
            }

            const config = {
                headers:{
                    'Content-Type': 'application/json'
                }
            }

            HttpApi.post('/api/auth/admin/function/has', data, config,
                response => {
                    const hasAll = response.data
                    if(!hasAll){
                        el.remove()
                    }else{
                        el.style.display = ""
                    }
                },
                response => {
                    ElMessage.error(response.errMsg)
                },
                err => {
                    ElMessage.error(err)
                })
        }else{
            throw new TypeError("v-hasAllFunctions value should be array type and cannot be empty")
        }
    }
}

/**
 * 自定义权限指令：当前用户是否拥有指定的任一功能
 */
const hasAnyFunctions = {
    mounted(el, binding) {
        if(binding.value && Array.isArray(binding.value) && binding.value.length > 0){
            const data = {
                type: 1,
                functionItemCodeList: binding.value
            }

            const config = {
                headers:{
                    'Content-Type': 'application/json'
                }
            }

            HttpApi.post('/api/auth/admin/function/has', data, config,
                response => {
                    const hasAny = response.data
                    if(!hasAny){
                        el.remove()
                    }else{
                        el.style.display = ""
                    }
                },
                response => {
                    ElMessage.error(response.errMsg)
                },
                err => {
                    ElMessage.error(err)
                })
        }else{
            throw new TypeError("v-hasAnyFunctions value should be array type and cannot be empty")
        }
    }
}

const PermissionDirectives = {
    hasAllFunctions,
    hasAnyFunctions
}

export default PermissionDirectives