package com.walter.starry.security.base.vo.response.resource;

import com.walter.starry.security.base.bo.AuthorityItemBo;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author walter.tan
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ResourceAuthorityResponse extends AuthorityItemBo {
    /**
     * 目标资源是否关联该权限
     */
    private boolean granted;
}
