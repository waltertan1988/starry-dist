package com.walter.starry.common.core.concurrent;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.map.HashedMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * 扩展虚拟线程执行的后处理器调用链
 * @author walter.tan
 */
@Slf4j
public class ExtendedVirtualThreadExecutorPostProcessorChain {

    private ExtendedVirtualThreadExecutorPostProcessorNode head;

    public ExtendedVirtualThreadExecutorPostProcessorChain(List<ExtendedVirtualThreadExecutorPostProcessor<Object>> processors) {
        if(Objects.isNull(processors)){
            return;
        }

        processors = new ArrayList<>(processors);

        ExtendedVirtualThreadExecutorPostProcessorNode lastNext = null;
        for (int i = processors.size() - 1; i >= 0 ; i--) {
            lastNext = new ExtendedVirtualThreadExecutorPostProcessorNode(processors.get(i).getClass().getName(), processors.get(i), lastNext);
        }

        this.head = lastNext;
    }

    public Runnable proxy(Runnable task) {
        if(Objects.isNull(this.head)){
            return task;
        }

        Map<String, Object> parentThreadContextMap = this.buildParentThreadContextMap(this.head);

        return () -> {
            String failProcessorId = null;
            try {
                this.head.preHandle(parentThreadContextMap);
                task.run();
            } catch (ExtendedVirtualThreadExecutorPostProcessorChainException e) {
                failProcessorId = e.getProcessorId();
                throw new RuntimeException(e);
            } finally {
                this.head.postHandle(failProcessorId, parentThreadContextMap);
            }
        };
    }

    public <T> Callable<T> proxy(Callable<T> task){
        if(Objects.isNull(this.head)){
            return task;
        }

        Map<String, Object> parentThreadContextMap = this.buildParentThreadContextMap(this.head);

        return () -> {
            String failProcessorId = null;
            try {
                this.head.preHandle(parentThreadContextMap);
                return task.call();
            } catch (ExtendedVirtualThreadExecutorPostProcessorChainException e) {
                failProcessorId = e.getProcessorId();
                throw e;
            } finally {
                this.head.postHandle(failProcessorId, parentThreadContextMap);
            }
        };
    }

    private Map<String, Object> buildParentThreadContextMap(ExtendedVirtualThreadExecutorPostProcessorNode head) {
        Map<String, Object> contextMap = new HashedMap<>();
        while(Objects.nonNull(head)){
            contextMap.put(head.id, head.curr.getContext());
            head = head.next;
        }
        return contextMap;
    }

    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ExtendedVirtualThreadExecutorPostProcessorNode {
        private String id;
        private ExtendedVirtualThreadExecutorPostProcessor<Object> curr;
        private ExtendedVirtualThreadExecutorPostProcessorNode next;

        private void preHandle(Map<String, Object> contextMap) throws ExtendedVirtualThreadExecutorPostProcessorChainException {
            try{
                curr.preHandle(contextMap.get(this.id));
            }catch (Throwable t){
                throw new ExtendedVirtualThreadExecutorPostProcessorChainException(this.id, "ExtendedVirtualThreadExecutorPostProcessor preHandle fail", t);
            }

            if(Objects.nonNull(next)){
                next.preHandle(contextMap);
            }
        }

        private void postHandle(String stopFromProcessorId, Map<String, Object> contextMap) {
            if(Objects.equals(this.id, stopFromProcessorId)){
                return;
            }

            if(Objects.nonNull(next)){
                next.postHandle(stopFromProcessorId, contextMap);
            }

            try{
                curr.postHandle(contextMap.get(this.id));
            }catch (Throwable t){
                log.error("ExtendedVirtualThreadExecutorPostProcessor postHandle fail for {}", curr.getClass().getName(), t);
            }
        }
    }

    @Getter
    public static class ExtendedVirtualThreadExecutorPostProcessorChainException extends Exception {
        private final String processorId;

        public ExtendedVirtualThreadExecutorPostProcessorChainException(String processorId, String message, Throwable throwable) {
            super(message, throwable);
            this.processorId = processorId;
        }
    }
}
