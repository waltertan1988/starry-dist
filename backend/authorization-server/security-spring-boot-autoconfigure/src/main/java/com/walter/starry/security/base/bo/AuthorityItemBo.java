package com.walter.starry.security.base.bo;

import com.walter.starry.security.base.entity.AclAuthorityItem;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author walter.tan
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AuthorityItemBo extends AclAuthorityItem {

    private List<AuthorityItemBo> children;
}
