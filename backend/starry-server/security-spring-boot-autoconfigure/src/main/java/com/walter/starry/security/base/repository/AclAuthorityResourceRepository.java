package com.walter.starry.security.base.repository;

import com.walter.starry.security.base.entity.AclAuthorityResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Date;
import java.util.List;

/**
 * @author walter.tan
 */
public interface AclAuthorityResourceRepository extends JpaRepository<AclAuthorityResource, Long> {

    List<AclAuthorityResource> findByResourceItemCodeIn(List<String> resourceItemCodes);

    @Modifying
    @Query("delete AclAuthorityResource o where o.authorityItemCode in ?1")
    void deleteByAuthorityItemCodes(List<String> authorityItemCodeList);

    @Modifying
    @Query("delete AclAuthorityResource o where o.resourceItemCode in ?1")
    void deleteByResourceItemCode(List<String> resourceItemCodeList);

    @Modifying
    @Query("delete AclAuthorityResource o where o.resourceItemCode = ?1 and o.authorityItemCode in ?2")
    void deleteByResourceItemCodeAndAuthorityItemCodes(String resourceItemCode, List<String> authorityItemCodeList);

    @Modifying
    @Query("update AclAuthorityResource o set o.authorityItemCode = ?2, o.updateTime = ?3 where o.authorityItemCode = ?1")
    void updateAuthorityItemCode(String oldCode, String newCode, Date updateTime);
}
