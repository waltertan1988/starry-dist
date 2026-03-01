package com.walter.starry.security.base.entity;

/**
 * @author: walter.tan
 * @datetime: 2023/9/19 12:05
 */
public interface AclHierarchyResource {

    String getCode();

    void setCode(String code);

    Long getSeq();

    void setSeq(Long seq);

    String getParentGroupCode();

    void setParentGroupCode(String parentGroupCode);
}
