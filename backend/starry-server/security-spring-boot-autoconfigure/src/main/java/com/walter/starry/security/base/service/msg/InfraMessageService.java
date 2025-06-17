package com.walter.starry.security.base.service.msg;

import com.walter.starry.security.base.common.enums.MessageTopicEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @Author: walter.tan
 * @DateTime: 2023-10-16 11:19:47
 */
@Slf4j
@Service
public class InfraMessageService {
    @Autowired
    private List<AbstractInfraMessageService> infraMessageServiceList;

    /**
     * 发送本应用的广播消息消息
     * @param messageTopicEnum
     * @param message
     * @return
     */
    @Nullable
    public String sendBroadcastMessage(MessageTopicEnum messageTopicEnum, String message) {
        return infraMessageServiceList
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("sendBroadcastMessage not support"))
                .sendBroadcastMessage(messageTopicEnum, message);
    }
}
