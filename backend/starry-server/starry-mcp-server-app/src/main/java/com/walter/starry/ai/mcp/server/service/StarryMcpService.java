package com.walter.starry.ai.mcp.server.service;

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
public class StarryMcpService {

    @Tool(description = "提供Starry系统的介绍")
    public String getStarryInfo(@ToolParam(description = "调用本方法的用户姓名") String guest) {
        log.info("getStarryInfo start, guest: {}", guest);
        return String.format("您好，%s！Starry系统是由Walter.Tan开发的基于Java和Vue3的综合性系统基座", StringUtils.hasText(guest) ? guest : "用户");
    }
}
