const state = () => ({
    visible: false,
    loginPageVo: null,

    inputUsername: null,
    inputPassword: null
})

const getters = {}

const mutations = {
    setVisible(state, val){
        state.visible = val
    },
    setLoginPageVo(state, val){
        state.loginPageVo = val
    },
    clearInputFields(state){
        state.inputUsername = null
        state.inputPassword = null
    }
}

const actions = {}

export default {
    namespaced: true,
    state,
    getters,
    mutations,
    actions
}