package com.walter.starry.security.base.listener.subscription;

import com.fasterxml.jackson.core.type.TypeReference;
import com.walter.starry.security.base.listener.annotation.RedisSubscribeTopic;
import com.walter.starry.security.base.common.enums.RedisTopicEnum;
import com.walter.starry.security.base.common.message.RoleChangeMessage;
import com.walter.starry.security.base.component.security.OpenPolicyAgentAuthorizationManager;
import com.walter.starry.security.base.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * 角色变更的广播消息订阅
 * @Author: walter.tan
 * @DateTime: 2023-10-14 13:43:50
 */
@Slf4j
@Component
@RedisSubscribeTopic(RedisTopicEnum.ROLE_CHANGE_BROADCAST)
public class RoleChangeListener implements MessageListener {
    @Autowired
    private OpenPolicyAgentAuthorizationManager openPolicyAgentAuthorizationManager;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String body = new String(message.getBody());
        log.info("RoleChangeMessage body: {}", body);
        List<RoleChangeMessage> messageList = JsonUtil.toList(body, new TypeReference<>() {});

        if(CollectionUtils.isEmpty(messageList)){
            return;
        }

        // 检查并尝试刷新本地缓存（包括层次角色、权限与资源的关联关系）
        this.tryRefreshLocalCaches(messageList);
    }

    private void tryRefreshLocalCaches(List<RoleChangeMessage> messageList) {
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
