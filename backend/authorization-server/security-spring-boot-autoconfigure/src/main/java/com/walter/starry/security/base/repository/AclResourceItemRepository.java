package com.walter.starry.security.base.repository;

import com.walter.starry.security.base.entity.AclResourceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Date;
import java.util.List;

/**
 * @author: walter.tan
 * @datetime: 2023/9/18 17:46
 */
public interface AclResourceItemRepository extends JpaRepository<AclResourceItem, Long>, JpaSpecificationExecutor<AclResourceItem> {

    /**
     * 查找用户权限所拥有的菜单项
     * @param type
     * @param authorityItemCodes
     * @return
     */
    @Query("SELECT ri FROM AclResourceItem ri, AclResourceGroup rg, AclAuthorityResource ar WHERE ri.parentGroupCode = rg.code AND ri.code = ar.resourceItemCode AND rg.type=?1 AND ar.authorityItemCode IN ?2")
    List<AclResourceItem> findAuthorizedResourceItemByRoleAndResourceGroupType(Integer type, List<String> authorityItemCodes);

    @Modifying
    @Query("DELETE FROM AclResourceItem i WHERE i.code IN ?1")
    void deleteByCodes(List<String> codes);

    @Modifying
    @Query("UPDATE AclResourceItem i set i.parentGroupCode = ?2, i.updateTime = ?3 where i.parentGroupCode = ?1")
    void updateParentGroupCode(String oldCode, String newCode, Date updateTime);
}
