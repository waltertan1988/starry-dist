package com.walter.starry.security.base.vo.response.base;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * @Author: walter.tan
 * @DateTime: 2024-12-26 10:47:46
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageVo<T> implements Serializable {

    /** 分页内的元素列表 */
    private List<T> content;

    /** 分页页码，从0开始 */
    private int number;

    /** 分页大小 */
    private int size;

    /** 总元素个数 */
    private long totalElements;
}
