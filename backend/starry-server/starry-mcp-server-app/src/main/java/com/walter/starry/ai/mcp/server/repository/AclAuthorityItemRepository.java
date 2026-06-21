package com.walter.starry.ai.mcp.server.repository;

import com.walter.starry.ai.mcp.server.entity.AclAuthorityItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * @author: walter.tan
 * @datetime: 2026/6/13 10:13
 */
public interface AclAuthorityItemRepository extends JpaRepository<AclAuthorityItem, Long>, JpaSpecificationExecutor<AclAuthorityItem> {

}
