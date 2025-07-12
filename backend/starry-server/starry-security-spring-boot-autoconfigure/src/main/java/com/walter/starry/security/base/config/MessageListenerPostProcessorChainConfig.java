package com.walter.starry.security.base.config;

import com.walter.starry.common.core.MessageListenerPostProcessor;
import com.walter.starry.common.core.MessageListenerPostProcessorChain;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author walter.tan
 */
@Configuration
public class MessageListenerPostProcessorChainConfig {
    @Bean
    public MessageListenerPostProcessorChain messageListenerPostProcessorChain(List<MessageListenerPostProcessor> processors) {
        return new MessageListenerPostProcessorChain(processors);
    }
}
