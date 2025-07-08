package com.walter.starry.common.util;

import lombok.experimental.UtilityClass;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;

import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * @Author: walter.tan
 * @DateTime: 2025-06-25 16:28:53
 */
@UtilityClass
public class MdcUtil {
    public static final String ATTR_TRACE_ID = "starry-traceId";

    public static String getTraceId(){
        return MDC.get(ATTR_TRACE_ID);
    }

    public static void setTraceId(String traceId){
        MDC.put(ATTR_TRACE_ID, traceId);
    }

    public static void removeTraceId(){
        MDC.remove(ATTR_TRACE_ID);
    }

    public static void clear(){
        MDC.clear();
    }

    public static String genNewTraceId(){
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static <T> Callable<T> toMdcCallable(String parentTraceId, Callable<T> callable){
        return () -> {
            try{
                MdcUtil.setTraceId(MdcUtil.genSubThreadTraceId(parentTraceId));
                return callable.call();
            }finally {
                MdcUtil.removeTraceId();
            }
        };
    }

    public static Runnable toMdcRunnable(String parentTraceId, Runnable runnable){
        return () -> {
            try{
                MdcUtil.setTraceId(MdcUtil.genSubThreadTraceId(parentTraceId));
                runnable.run();
            }finally {
                MdcUtil.removeTraceId();
            }
        };
    }

    private static String genSubThreadTraceId(String parentThreadTraceId){
        if(StringUtils.hasText(parentThreadTraceId)){
            return String.format("%s:%s", parentThreadTraceId, MdcUtil.genNewTraceId());
        }else{
            return String.format(":%s", MdcUtil.genNewTraceId());
        }
    }
}
