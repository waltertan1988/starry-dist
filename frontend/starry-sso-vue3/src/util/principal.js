import Cookie from "./cookie";
import SessionStorage from "./sessionStorage";

/**
 * 把用户的principal信息添加到Cookie
 * @param principal
 */
function setCookie(principal){
    Cookie.set(Cookie.KEYS.PRINCIPAL.NICKNAME, encodeURI(principal.nickname), {path:"/"})
}

/**
 * 清空当前登录用户principal的所有Cookie信息
 */
function clearAllCookie() {
    Cookie.del(Cookie.KEYS.PRINCIPAL.NICKNAME)
}

/**
 * 清空所有登录用户的SessionStorage信息
 */
function clearSessionStorage() {
    sessionStorage.removeItem(SessionStorage.KEYS.PRINCIPAL.MENU_TREE_NODES);
}

const Principal = {
    setCookie,
    clearAllCookie,
    clearSessionStorage
}

export default Principal