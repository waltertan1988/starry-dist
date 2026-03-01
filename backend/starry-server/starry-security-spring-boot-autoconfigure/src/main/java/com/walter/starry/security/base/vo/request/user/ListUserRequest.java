package com.walter.starry.security.base.vo.request.user;

import com.walter.starry.common.vo.AbstractPaginationRequest;
import lombok.Data;

import java.util.Date;

/**
 * @author: walter.tan
 * @datetime: 2023/9/27 21:47
 */
@Data
public class ListUserRequest extends AbstractPaginationRequest {
    private String username;

    private String nickname;

    private Boolean enabled;

    private Date createTimeBegin;

    private Date createTimeEnd;
}
