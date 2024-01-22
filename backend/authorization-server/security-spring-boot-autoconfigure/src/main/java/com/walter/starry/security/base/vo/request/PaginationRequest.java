package com.walter.starry.security.base.vo.request;

import lombok.Data;

/**
 * @author: walter.tan
 * @datetime: 2023/9/27 22:00
 */
@Data
public abstract class PaginationRequest {

    /** 页码，从0开始 */
    private int pageNumber = 0;

    /** 分页大小 */
    private int pageSize = 10;
}
