package com.walter.starry.ai.mcp.server.service;

import com.walter.starry.ai.mcp.server.remote.StarryInfoRes;
import com.walter.starry.autoconfigure.mdc.ai.mcp.server.MdcMcpToolRequest;
import com.walter.starry.common.util.MdcUtil;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springaicommunity.mcp.annotation.McpProgressToken;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springaicommunity.mcp.context.McpSyncRequestContext;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * @author walter.tan
 */
@Slf4j
@Service
public class StarryMcpService {

    private static final String STARRY_AUTHOR_UID = "PC9527";

    @McpTool(description = "提供Starry系统的基本信息")
    public StarryInfoRes getStarryInfo(McpSyncServerExchange exchange, @McpProgressToken String progressToken, MdcMcpToolRequest req) {
        // 向mcp客户端发送服务端的日志信息
        exchange.loggingNotification(McpSchema.LoggingMessageNotification.builder()
                .level(McpSchema.LoggingLevel.INFO)
                .data("【McpServer-getStarryInfo】开始处理，progressToken: " + progressToken)
                .meta(Map.of(MdcUtil.ATTR_TRACE_ID, MdcUtil.getTraceId()))
                .build());

        log.info("getStarryInfo start");

        try{
            return new StarryInfoRes(STARRY_AUTHOR_UID, "Starry系统是一个基于Java和Vue3的综合性系统基座");
        }finally {
            // 向mcp客户端发送执行进度信息
            if(StringUtils.isNotBlank(progressToken)){
                exchange.progressNotification(new McpSchema.ProgressNotification(
                        progressToken, 1.0, 1.0, "【McpServer-getStarryInfo】处理完成",
                        Map.of(MdcUtil.ATTR_TRACE_ID, MdcUtil.getTraceId())));
            }
        }
    }

    @McpTool(description = "根据用户ID获取该用户的简介信息")
    public String getUserInfo(McpSyncRequestContext context, UserInfoReq req){
        // 向mcp客户端发送执行进度信息
        context.progress(p -> p.progress(0).total(1.0)
                .meta(MdcUtil.ATTR_TRACE_ID, MdcUtil.getTraceId())
                .message("【McpServer-getUserInfo】开始处理"));

        log.info("getUserInfo start. uid: {}", req.getUid());

        // 向mcp客户端发送服务端的日志信息
        context.log(log -> log
                .level(McpSchema.LoggingLevel.INFO)
                .message("【McpServer-getUserInfo】处理中: uid: %s, progressToken: %s, traceId: %s"
                        .formatted(req.getUid(), context.request().progressToken(), context.requestMeta().get(MdcUtil.ATTR_TRACE_ID)))
                .meta(MdcUtil.ATTR_TRACE_ID, MdcUtil.getTraceId()));

        try{
            if(STARRY_AUTHOR_UID.equals(req.getUid())){
                return "Walter.Tan，是一名资深的专业软件开发从业者，特别擅长互联网行业的软件架构和技术方案的设计";
            }else{
                return "暂未找到该用户的个人简介";
            }
        }finally {
            context.progress(p -> p.progress(1.0).total(1.0)
                    .meta(MdcUtil.ATTR_TRACE_ID, MdcUtil.getTraceId())
                    .message("【McpServer-getUserInfo】处理完成"));
        }
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class UserInfoReq extends MdcMcpToolRequest {
        @McpToolParam(description = "用户ID")
        private String uid;
    }
}
