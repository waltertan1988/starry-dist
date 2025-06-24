package com.walter.starry.security.base.listener.redis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.walter.starry.security.base.common.enums.MessageTopicEnum;
import com.walter.starry.security.base.common.message.RoleChangeMessage;
import com.walter.starry.security.base.service.RoleService;
import com.walter.starry.security.base.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 角色变更的广播消息订阅
 * @Author: walter.tan
 * @DateTime: 2023-10-14 13:43:50
 */
@Slf4j
@Component
@RedisSubscribe(namespace = "${app.message.redis.namespace}", topic = MessageTopicEnum.ROLE_CHANGE_BROADCAST)
@ConditionalOnProperty(name = "app.message.redis.enabled", havingValue = "true")
public class RoleChangeRedisListener implements MessageListener {
    @Autowired
    private RoleService roleService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String body = new String(message.getBody());
        log.info("redis RoleChangeMessage body: {}", body);
        List<RoleChangeMessage> messageList = JsonUtil.toList(body, new TypeReference<>() {});

        // 检查并尝试刷新本地缓存（包括层次角色、权限与资源的关联关系）
        roleService.tryRefreshLocalCaches(messageList);
    }
}
