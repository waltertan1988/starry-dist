package com.walter.starry.ai.mcp.server.service;

import com.walter.starry.autoconfigure.mdc.ai.mcp.server.MdcMcpToolParam;
import com.walter.starry.common.core.ai.AiTool;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * @author walter.tan
 */
@Slf4j
@Service
public class StarryMcpService implements AiTool {
    @Tool(description = "提供Starry系统的介绍")
    public String getStarryInfo(StarryInfoReq req) {
        log.info("getStarryInfo start, guest: {}", req.getGuest());
        return String.format("您好，%s！Starry系统是由Walter.Tan开发的基于Java和Vue3的综合性系统基座", StringUtils.hasText(req.getGuest()) ? req.getGuest() : "用户");
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class StarryInfoReq extends MdcMcpToolParam {
        @ToolParam(description = "调用本方法的用户姓名")
        private String guest;
    }
}
