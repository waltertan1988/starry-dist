package com.walter.starry.security.base.service;

import com.walter.starry.security.base.common.message.RoleChangeMessage;
import com.walter.starry.security.base.component.security.OpenPolicyAgentAuthorizationManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * @Author: walter.tan
 * @DateTime: 2024-03-27 15:44:29
 */
@Service
public class RoleService {
    @Autowired
    private OpenPolicyAgentAuthorizationManager openPolicyAgentAuthorizationManager;

    /**
     * 检查并尝试刷新本地缓存（包括层次角色、权限与资源的关联关系）
     * @param messageList
     */
    public void tryRefreshLocalCaches(List<RoleChangeMessage> messageList) {
        boolean needRefreshRoleHierarchy = false;
        boolean needRefreshRequestMatcherEntryHolder = false;

        for (RoleChangeMessage roleChangeMessage : messageList) {
            if(Objects.isNull(roleChangeMessage.getBefore())){
                // 新增角色
                needRefreshRoleHierarchy = true;
            }else if(Objects.isNull(roleChangeMessage.getAfter())){
                // 删除角色
                needRefreshRoleHierarchy = true;
                needRefreshRequestMatcherEntryHolder = true;
            }else{
                // 修改角色

                if(!Objects.equals(roleChangeMessage.getAfter().getCode(), roleChangeMessage.getBefore().getCode())){
                    // 修改角色编码
                    needRefreshRoleHierarchy = true;
                    needRefreshRequestMatcherEntryHolder = true;
                }

                if(!Objects.equals(roleChangeMessage.getAfter().getParentCode(), roleChangeMessage.getBefore().getParentCode())){
                    // 移动角色
                    needRefreshRoleHierarchy = true;
                }
            }

            if(needRefreshRoleHierarchy && needRefreshRequestMatcherEntryHolder){
                break;
            }
        }

        if(needRefreshRoleHierarchy){
            openPolicyAgentAuthorizationManager.refreshRoleHierarchy();
        }
        
        if(needRefreshRequestMatcherEntryHolder){
            openPolicyAgentAuthorizationManager.refreshRequestMatcherEntryHolder();
        }
    }
}
