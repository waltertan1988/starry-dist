
/** 分页组件默认的分页每页显示条数 */
const DEFAULT_EL_PAGE_SIZE = 10

/** 分页组件默认的分页页码 */
const DEFAULT_EL_PAGE_NO = 1

/**
 * 把服务端的返回分页，转换为ElementPlus的分页对象
 * @param serverPage
 */
function toElPagination(serverPage) {
    return {
        componentKey: Date.now(), //通过给组件绑定并更新:key属性值，可实现触发重新渲染组件（包括执行相应的生命周期函数，计算属性，watch等）
        content: serverPage ? serverPage.content : [],
        currentPage: serverPage ? serverPage.number + 1 : DEFAULT_EL_PAGE_NO,
        pageSize: serverPage ? serverPage.size : DEFAULT_EL_PAGE_SIZE,
        total: serverPage ? serverPage.totalElements : 0,
    }
}

const Pagination = {
    DEFAULT_EL_PAGE_SIZE,
    DEFAULT_EL_PAGE_NO,
    toElPagination,
}

export default Pagination