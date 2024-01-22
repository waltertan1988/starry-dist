import { createStore } from 'vuex';
import ajaxLoginFormDialog from './module/ajaxLoginFormDialog';
import ajaxLogoutFormDialog from "./module/ajaxLogoutFormDialog";

const store = createStore({
  // TODO tyx 设置vuex的严格模式
  // strict: process.env.NODE_ENV !== 'production',

  state: {
    productModule:{
      usaExchangeRate: 6.5
    }
  },

  getters:{
    usaExchangeRate(state){
      return state.productModule.usaExchangeRate;
    }
  },

  mutations: {
    updateUsaExchangeRate(state, obj){
      return state.productModule.usaExchangeRate = obj.newVal;
    }
  },

  actions: {
  },

  modules: {
    ajaxLoginFormDialog, // ajax登录对话框
    ajaxLogoutFormDialog, // ajax退出登录对话框
  }
})

export default store;
