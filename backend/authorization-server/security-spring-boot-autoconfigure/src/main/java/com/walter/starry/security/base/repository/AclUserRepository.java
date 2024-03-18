package com.walter.starry.security.base.repository;

import com.walter.starry.security.base.entity.AclUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * @author: walter.tan
 * @datetime: 2023/9/26 22:41
 */
public interface AclUserRepository extends JpaRepository<AclUser, String>, JpaSpecificationExecutor<AclUser> {

    @Modifying
    @Transactional(rollbackFor = Exception.class)
    @Query("UPDATE AclUser u SET u.expiredSessionsCleanTime = ?2 WHERE u.username IN ?1")
    void updateExpiredSessionsCleanTime(List<String> usernames, Date expiredSessionsCleanTime);

    @Modifying
    @Transactional(rollbackFor = Exception.class)
    @Query("DELETE FROM AclUser u WHERE u.username = ?1")
    void deleteByUsername(String username);
}
