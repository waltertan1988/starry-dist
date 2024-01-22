import axios from "axios";
import store from "../store";

/**
 * 弹出登录对话框
 */
function openAjaxLoginFormDialog(){
    // 清空以前填写过的表单数据
    store.commit("ajaxLoginFormDialog/clearInputFields")

    axios.get("/api/auth/login")
        .then(result => {
            let response = result.data;
            if(!response.success){
                console.log(JSON.stringify(response))
                return
            }

            let loginPageVo = response.data

            // 填充响应式数据
            store.commit("ajaxLoginFormDialog/setLoginPageVo", loginPageVo)
            // 显示对话框
            store.commit("ajaxLoginFormDialog/setVisible", true)
        })
        .catch(err =>{
            console.log(err)
        })
}

const Login = {
    openAjaxLoginFormDialog
}

export default Login