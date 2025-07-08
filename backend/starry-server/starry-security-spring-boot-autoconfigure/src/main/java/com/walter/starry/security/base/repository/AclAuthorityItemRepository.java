package com.walter.starry.security.base.repository;

import com.walter.starry.security.base.entity.AclAuthorityItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Date;
import java.util.List;

/**
 * @author walter.tan
 */
public interface AclAuthorityItemRepository extends JpaRepository<AclAuthorityItem, Long>, JpaSpecificationExecutor<AclAuthorityItem> {

    @Modifying
    @Query("delete AclAuthorityItem o where o.code in ?1")
    void deleteByCodes(List<String> codes);

    @Modifying
    @Query("update AclAuthorityItem o set o.parentCode = ?2, o.updateTime = ?3 where o.parentCode = ?1")
    void updateParentCode(String oldCode, String newCode, Date updateTime);
}
