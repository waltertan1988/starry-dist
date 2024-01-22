package com.walter.starry.security.base.repository;

import com.walter.starry.security.base.entity.AclAuthority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Date;
import java.util.List;

/**
 * @author: walter.tan
 * @datetime: 2023/9/18 17:53
 */
public interface AclAuthorityRepository extends JpaRepository<AclAuthority, Long>, JpaSpecificationExecutor<AclAuthority> {

    @Query("select distinct a.username from AclAuthority a where a.username > ?1 and a.authority in ?2")
    Page<String> scanDistinctUsernameByAuthorityIn(String lastUsername, List<String> authorityList, Pageable pageable);

    @Modifying
    void deleteByUsername(String username);

    @Modifying
    @Query("delete AclAuthority o where o.username = ?1 and o.authority in ?2")
    void deleteByUsernameAndAuthorityIn(String username, List<String> authorityList);

    @Modifying
    @Query("delete AclAuthority o where o.authority in ?1")
    void deleteByAuthorities(List<String> authorityList);

    @Modifying
    @Query("update AclAuthority o set o.authority = ?2, o.updateTime = ?3 where o.authority = ?1")
    void updateAuthority(String oldCode, String newCode, Date updateTime);
}
