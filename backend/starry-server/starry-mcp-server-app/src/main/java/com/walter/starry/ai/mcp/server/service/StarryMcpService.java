package com.walter.starry.ai.mcp.server.service;

import com.walter.starry.ai.mcp.server.remote.StarryInfoRes;
import com.walter.starry.autoconfigure.mdc.ai.mcp.server.MdcMcpToolRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springaicommunity.mcp.context.McpSyncRequestContext;
import org.springframework.stereotype.Service;

/**
 * @author walter.tan
 */
@Slf4j
@Service
public class StarryMcpService {

    private static final String STARRY_AUTHOR_UID = "123456";

    @McpTool(description = "提供Starry系统的基本信息")
    public StarryInfoRes getStarryInfo(McpSyncRequestContext context, MdcMcpToolRequest req) {
        log.info("getStarryInfo start");
        return new StarryInfoRes(STARRY_AUTHOR_UID, "Starry系统是一个基于Java和Vue3的综合性系统基座");
    }

    @McpTool(description = "根据用户ID获取该用户的简介信息")
    public String getUserInfo(McpSyncRequestContext context, UserInfoReq req){
        log.info("getUserInfo start. uid: {}", req.getUid());

        if(STARRY_AUTHOR_UID.equals(req.getUid())){
            return "Walter.Tan，是一名资深的专业软件开发从业者，特别擅长互联网行业的软件架构和技术方案的设计";
        }else{
            return "暂未找到该用户的个人简介";
        }
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class UserInfoReq extends MdcMcpToolRequest {
        @McpToolParam(description = "用户ID")
        private String uid;
    }
}
