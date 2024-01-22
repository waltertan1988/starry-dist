import axios from "axios";
import Login from "./login";
import {ElMessage} from "element-plus";

/** 未认证 */
const UNAUTHORIZED = "401";
/** 权限不足，拒绝访问 */
const FORBIDDEN = "403";

/**
 * 发起Http Get请求
 * @param url
 * @param successCallback
 * @param failureCallback
 * @param errorCallback
 * @param beforeSendCallback
 * @param completeCallback
 */
function get(url, successCallback, failureCallback, errorCallback, beforeSendCallback, completeCallback) {
    if(beforeSendCallback){
        beforeSendCallback()
    }

    axios.get(url)
        .then(result => {
            if(completeCallback){
                completeCallback()
            }

            let response = result.data
            if(!response.success){
                if(response.errCode === UNAUTHORIZED){
                    // 未认证，弹出登录对话框
                    Login.openAjaxLoginFormDialog()
                }else if(response.errCode === FORBIDDEN){
                    // 无权限，弹出提示框
                    ElMessage.error("你的权限不足，请联系管理员")
                }else {
                    // 其他错误
                    if(failureCallback){
                        failureCallback(response);
                    }else{
                        alert(JSON.stringify(response))
                    }
                }

                return
            }

            if(successCallback){
                successCallback(response);
            }else{
                console.log(JSON.stringify(response))
            }
        })
        .catch(err => {
            if(completeCallback){
                completeCallback()
            }

            if(errorCallback){
                errorCallback(err)
            }else{
                console.log(err)
            }
        })
}

/**
 * 发起Http Post请求
 * @param url
 * @param data
 * @param config
 * @param successCallback
 * @param failureCallback
 * @param errorCallback
 * @param beforeSendCallback
 * @param completeCallback
 */
function post(url, data, config, successCallback, failureCallback, errorCallback, beforeSendCallback, completeCallback) {
    if(beforeSendCallback){
        beforeSendCallback()
    }

    axios
        .post(url, data, config)
        .then(result => {
            if(completeCallback){
                completeCallback()
            }

            let response = result.data
            if(!response.success){
                if(response.errCode === UNAUTHORIZED){
                    // 未认证，弹出登录对话框
                    Login.openAjaxLoginFormDialog()
                }else if(response.errCode === FORBIDDEN){
                    // 无权限，弹出提示框
                    ElMessage.error("你的权限不足，请联系管理员")
                }else {
                    // 其他错误
                    if(failureCallback){
                        failureCallback(response);
                    }else{
                        alert(JSON.stringify(response))
                    }
                }

                return
            }

            if(successCallback){
                successCallback(response);
            }else{
                console.log(JSON.stringify(response))
            }
        })
        .catch(err => {
            if(completeCallback){
                completeCallback()
            }

            if(errorCallback){
                errorCallback(err)
            }else{
                console.log(err)
            }
        })
}

const HttpApi = {
    get,
    post,
}

export default HttpApi