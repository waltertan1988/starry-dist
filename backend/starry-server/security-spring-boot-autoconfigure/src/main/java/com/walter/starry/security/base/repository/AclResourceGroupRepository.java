package com.walter.starry.security.base.repository;

import com.walter.starry.security.base.entity.AclResourceGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/**
 * @author: walter.tan
 * @datetime: 2023/9/18 17:46
 */
public interface AclResourceGroupRepository extends JpaRepository<AclResourceGroup, Long>, JpaSpecificationExecutor<AclResourceGroup> {

    @Modifying
    @Query("DELETE FROM AclResourceGroup g WHERE g.type = ?1 AND g.code IN ?2")
    void deleteByCodes(Integer type, List<String> codes);
}
