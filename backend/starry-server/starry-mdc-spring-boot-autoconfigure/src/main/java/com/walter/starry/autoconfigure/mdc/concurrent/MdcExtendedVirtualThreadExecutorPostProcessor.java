package com.walter.starry.autoconfigure.mdc.concurrent;

import com.walter.starry.common.core.concurrent.ExtendedVirtualThreadExecutorPostProcessor;
import com.walter.starry.common.util.MdcUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.core.Ordered;

/**
 * 虚拟线程池的MDC后处理器
 * @author walter.tan
 */
public class MdcExtendedVirtualThreadExecutorPostProcessor implements ExtendedVirtualThreadExecutorPostProcessor<MdcExtendedVirtualThreadExecutorPostProcessor.MdcContext> {

    @Override
    public void preHandle(MdcContext context) {
        MdcUtil.setTraceId(MdcUtil.genSubThreadTraceId(context.getParentTraceId()));
    }

    @Override
    public void postHandle(MdcContext context) {
        MdcUtil.removeTraceId();
    }

    @Override
    public MdcContext getContext() {
        return new MdcContext(MdcUtil.getTraceId());
    }

    @Getter
    @AllArgsConstructor
    public static class MdcContext {
        private String parentTraceId;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
