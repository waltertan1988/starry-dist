package com.walter.starry.security.base.listener.subscription;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;
import com.walter.starry.security.base.listener.annotation.RedisSubscribeTopic;
import com.walter.starry.security.base.common.enums.RedisTopicEnum;
import com.walter.starry.security.base.common.message.ResourceChangeMessage;
import com.walter.starry.security.base.component.security.OpenPolicyAgentAuthorizationManager;
import com.walter.starry.security.base.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * 资源变更的广播消息订阅
 * @Author: walter.tan
 * @DateTime: 2023-10-14 13:43:50
 */
@Slf4j
@Component
@RedisSubscribeTopic(RedisTopicEnum.RESOURCE_CHANGE_BROADCAST)
public class ResourceChangeListener implements MessageListener {
    @Autowired
    private OpenPolicyAgentAuthorizationManager openPolicyAgentAuthorizationManager;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String body = new String(message.getBody());
        log.info("ResourceChangeMessage body: {}", body);
        List<ResourceChangeMessage> messageList = JsonUtil.toList(body, new TypeReference<>() {});

        if(CollectionUtils.isEmpty(messageList)){
            return;
        }

        // 检查并尝试刷新本地缓存（资源与权限的关联关系）
        this.tryRefreshLocalCaches(messageList);
    }

    private void tryRefreshLocalCaches(List<ResourceChangeMessage> messageList) {
        boolean needRefreshRequestMatcherEntryHolder = false;

        for (ResourceChangeMessage changeMessage : messageList) {
            if(Objects.nonNull(changeMessage.getChangeAuthorityData())){
                // 资源权限变更
                needRefreshRequestMatcherEntryHolder = true;
                break;
            }else if(Objects.nonNull(changeMessage.getBefore()) && Objects.nonNull(changeMessage.getAfter())){
                // 修改资源

                if(!Objects.equals(changeMessage.getAfter().getCode(), changeMessage.getBefore().getCode())){
                    // 修改资源编码
                    needRefreshRequestMatcherEntryHolder = true;
                    break;
                }

                if(!Objects.equals(changeMessage.getAfter().getPattern(), changeMessage.getBefore().getPattern())){
                    // 修改路径模式
                    needRefreshRequestMatcherEntryHolder = true;
                    break;
                }

                if(StringUtils.isNotBlank(changeMessage.getAfter().getHttpMethodList()) && StringUtils.isNotBlank(changeMessage.getBefore().getHttpMethodList())){
                    Collection<String> beforeHttpMethods = Lists.newArrayList(changeMessage.getBefore().getHttpMethodList().split(OpenPolicyAgentAuthorizationManager.HTTP_METHOD_LIST_DELIMITER));
                    Collection<String> afterHttpMethods = Lists.newArrayList(changeMessage.getAfter().getHttpMethodList().split(OpenPolicyAgentAuthorizationManager.HTTP_METHOD_LIST_DELIMITER));
                    if(!CollectionUtils.isEqualCollection(beforeHttpMethods, afterHttpMethods)){
                        // 修改http请求方法类型
                        needRefreshRequestMatcherEntryHolder = true;
                        break;
                    }
                }

                if(!Objects.equals(changeMessage.getAfter().getSeq(), changeMessage.getBefore().getSeq())){
                    // 修改顺序
                    needRefreshRequestMatcherEntryHolder = true;
                    break;
                }
            }else if(Objects.isNull(changeMessage.getBefore())){
                // 新增资源
                needRefreshRequestMatcherEntryHolder = true;
                break;
            }else {
                // 删除资源
                needRefreshRequestMatcherEntryHolder = true;
                break;
            }
        }

        if(needRefreshRequestMatcherEntryHolder){
            openPolicyAgentAuthorizationManager.refreshRequestMatcherEntryHolder();
        }
    }
}
