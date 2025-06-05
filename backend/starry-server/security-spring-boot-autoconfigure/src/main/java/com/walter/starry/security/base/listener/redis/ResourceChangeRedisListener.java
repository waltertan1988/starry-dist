package com.walter.starry.security.base.listener.redis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.walter.starry.security.base.common.enums.MessageTopicEnum;
import com.walter.starry.security.base.common.message.ResourceChangeMessage;
import com.walter.starry.security.base.listener.annotation.RedisSubscribeTopic;
import com.walter.starry.security.base.service.ResourceGroupService;
import com.walter.starry.security.base.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

import java.util.List;

/**
 * 资源变更的广播消息订阅（已废弃，由Pulsar代替）
 * @Author: walter.tan
 * @DateTime: 2023-10-14 13:43:50
 */
@Slf4j
@Deprecated
//@Component
@RedisSubscribeTopic(MessageTopicEnum.RESOURCE_CHANGE_BROADCAST)
public class ResourceChangeRedisListener implements MessageListener {
    @Autowired
    private ResourceGroupService resourceGroupService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String body = new String(message.getBody());
        log.info("ResourceChangeMessage body: {}", body);
        List<ResourceChangeMessage> messageList = JsonUtil.toList(body, new TypeReference<>() {});

        if(CollectionUtils.isEmpty(messageList)){
            return;
        }

        // 检查并尝试刷新本地缓存（资源与权限的关联关系）
        resourceGroupService.tryRefreshLocalCaches(messageList);
    }
}
