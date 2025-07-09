package com.walter.starry.autoconfigure.mdc.web;

import com.walter.starry.common.util.MdcUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * MDC的Web过滤器，用于设置每个请求的traceId
 * @Author: walter.tan
 * @DateTime: 2025-06-26 16:28:33
 */
public class MdcWebFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try{
            String traceId = StringUtils.firstNonBlank(MdcUtil.getTraceId(), request.getParameter(MdcUtil.ATTR_TRACE_ID), request.getHeader(MdcUtil.ATTR_TRACE_ID), MdcUtil.genNewTraceId());
            MdcUtil.setTraceId(traceId);
            response.addHeader(MdcUtil.ATTR_TRACE_ID, traceId);
            filterChain.doFilter(request, response);
        }finally {
            MdcUtil.removeTraceId();
        }
    }
}
