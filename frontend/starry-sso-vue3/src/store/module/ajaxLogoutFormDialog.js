const state = () => ({
    visible: false,
    logoutPageVo: null
})

const getters = {}

const mutations = {
    setVisible(state, val){
        state.visible = val
    },
    setLogoutPageVo(state, val){
        state.logoutPageVo = val
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