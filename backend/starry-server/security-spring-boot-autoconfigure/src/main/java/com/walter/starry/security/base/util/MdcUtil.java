package com.walter.starry.security.base.util;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * @Author: walter.tan
 * @DateTime: 2025-06-25 16:28:53
 */
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
}
