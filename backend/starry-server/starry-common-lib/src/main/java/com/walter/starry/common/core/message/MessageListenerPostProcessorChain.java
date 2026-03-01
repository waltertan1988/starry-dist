package com.walter.starry.common.core.message;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 消息队列监听器的后处理器调用链
 * @author walter.tan
 */
@Slf4j
public class MessageListenerPostProcessorChain {

    private MessageListenerPostProcessorNode head;

    public MessageListenerPostProcessorChain(List<MessageListenerPostProcessor> processors) {
        if(Objects.isNull(processors) || processors.isEmpty()){
            return;
        }

        processors = new ArrayList<>(processors);

        MessageListenerPostProcessorNode lastNext = null;
        for (int i = processors.size() - 1; i >= 0 ; i--) {
            lastNext = new MessageListenerPostProcessorNode(processors.get(i).getClass().getName(), processors.get(i), lastNext);
        }

        this.head = lastNext;
    }

    public <T> T handle(Supplier<T> supplier, Object... objects) throws MessageListenerPostProcessorChainException {
        if(Objects.isNull(this.head)){
            return supplier.get();
        }

        String failProcessorId = null;
        try {
            this.head.preHandle(objects);
            return supplier.get();
        } catch (MessageListenerPostProcessorChainException e) {
            failProcessorId = e.getProcessorId();
            throw e;
        } finally {
            this.head.postHandle(failProcessorId, objects);
        }
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class MessageListenerPostProcessorNode {
        private String id;
        private MessageListenerPostProcessor curr;
        private MessageListenerPostProcessorNode next;

        private void preHandle(Object... objects) throws MessageListenerPostProcessorChainException {
            try{
                curr.preHandle(objects);
            }catch (Throwable t){
                throw new MessageListenerPostProcessorChainException(this.id, "MessageListenerPostProcessor preHandle fail", t);
            }

            if(Objects.nonNull(next)){
                next.preHandle(objects);
            }
        }

        private void postHandle(String stopFromProcessorId, Object... objects) {
            if(Objects.equals(this.id, stopFromProcessorId)){
                return;
            }

            if(Objects.nonNull(next)){
                next.postHandle(stopFromProcessorId, objects);
            }

            try{
                curr.postHandle(objects);
            }catch (Throwable t){
                log.error("MessageListenerPostProcessor postHandle fail for {}", curr.getClass().getName(), t);
            }
        }
    }

    @Getter
    public static class MessageListenerPostProcessorChainException extends Exception {
        private final String processorId;

        public MessageListenerPostProcessorChainException(String processorId, String message, Throwable throwable) {
            super(message, throwable);
            this.processorId = processorId;
        }
    }
}
