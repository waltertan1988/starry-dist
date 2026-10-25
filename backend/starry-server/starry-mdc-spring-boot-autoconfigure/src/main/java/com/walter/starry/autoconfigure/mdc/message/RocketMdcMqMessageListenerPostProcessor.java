package com.walter.starry.autoconfigure.mdc.message;

import com.walter.starry.common.constant.MqConstant;
import com.walter.starry.common.util.MdcUtil;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @author walter.tan
 */
@Component
@ConditionalOnProperty(name = MqConstant.RocketMq.CONDITIONAL_ON_PROP)
public class RocketMdcMqMessageListenerPostProcessor implements MdcMessageListenerPostProcessor {

    @Override
    public void preHandle(Object... object) {
        MessageExt messageExt = (MessageExt) object[0];
        MdcUtil.setTraceId(messageExt.getUserProperty(MdcUtil.ATTR_TRACE_ID));
    }
}
