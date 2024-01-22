import { createRouter, createWebHistory } from 'vue-router'
import Login from "../views/Login";

const routes = [
  {
    path: '/admin',
    name: 'AdminPage',
    component: () => import(/* webpackChunkName: "AdminPage" */ '../views/admin/AdminPage.vue'),
  },
  {
    path: '/admin/user',
    name: 'AdminUserPage',
    component: () => import(/* webpackChunkName: "AdminUserPage" */ '../views/admin/user/AdminUserPage.vue'),
  },
  {
    path: '/admin/role',
    name: 'AdminRolePage',
    component: () => import(/* webpackChunkName: "AdminRolePage" */ '../views/admin/role/AdminRolePage.vue'),
  },
  {
    path: '/admin/menu',
    name: 'AdminMenuPage',
    component: () => import(/* webpackChunkName: "AdminMenuPage" */ '../views/admin/menu/AdminMenuPage.vue'),
  },
  {
    path: '/admin/function',
    name: 'AdminFunctionPage',
    component: () => import(/* webpackChunkName: "AdminFunctionPage" */ '../views/admin/function/AdminFunctionPage.vue'),
  },
  {
    path: '/login',
    name: 'Login',
    component: Login
  }
]

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes
})

export default router
