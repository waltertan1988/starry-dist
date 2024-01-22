package com.walter.starry.security.base.repository;

import com.walter.starry.security.base.entity.AclUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * @author: walter.tan
 * @datetime: 2023/9/26 22:41
 */
public interface AclUserRepository extends JpaRepository<AclUser, String>, JpaSpecificationExecutor<AclUser> {
}
