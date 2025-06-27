package com.walter.starry.security.base.service.msg;

import com.walter.starry.security.base.common.enums.MessageTopicEnum;
import com.walter.starry.security.base.common.message.MdcMessageWrapper;
import com.walter.starry.security.base.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
     * 发送本应用的广播消息消息，消息体内包含MDC信息
     * @param messageTopicEnum
     * @param msgObj
     * @return
     * @param <T>
     * @throws Exception
     */
    public <T> String sendBroadcastMessage(MessageTopicEnum messageTopicEnum, T msgObj) throws Exception {
        return this.sendBroadcastMessage(messageTopicEnum, msgObj, true);
    }

    /**
     * 发送本应用的广播消息消息
     * @param messageTopicEnum
     * @param msgObj
     * @param useMdcWrapper
     * @return
     * @param <T>
     * @throws Exception
     */
    public <T> String sendBroadcastMessage(MessageTopicEnum messageTopicEnum, T msgObj, boolean useMdcWrapper) throws Exception {
        String message;
        if(useMdcWrapper){
            message = JsonUtil.toJson(new MdcMessageWrapper(JsonUtil.toJson(msgObj)));
        }else{
            message = JsonUtil.toJson(msgObj);
        }

        try{
            return infraMessageServiceList
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("sendBroadcastMessage not support"))
                    .sendBroadcastMessage(messageTopicEnum, message);
        }catch (Throwable t){
            throw new Exception(String.format("sendBroadcastMessage fail. topic: %s, message: %s", messageTopicEnum.name(), message), t);
        }
    }
}
