package com.walter.starry.security.base.vo.response.user;

import com.walter.starry.security.base.bo.AuthorityItemBo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserAvailableAuthorityResponse extends AuthorityItemBo {

    /**
     * 目标用户是否拥有该权限
     */
    private boolean granted;
    /**
     * 该权限的各级父权限的名称
     */
    private List<String> parentNameList;
}
