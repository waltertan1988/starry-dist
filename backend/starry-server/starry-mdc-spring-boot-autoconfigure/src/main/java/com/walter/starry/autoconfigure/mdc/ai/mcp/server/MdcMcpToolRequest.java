package com.walter.starry.autoconfigure.mdc.ai.mcp.server;

import com.walter.starry.common.util.MdcUtil;
import lombok.Getter;
import org.springframework.ai.mcp.annotation.McpToolParam;

import java.io.Serializable;

/**
 * 支持MDC的MCP工具请求体
 * @author walter.tan
 */
@Getter
public class MdcMcpToolRequest implements Serializable {

    public static final String TRACE_ID_DESC = "the starryTraceId for this MCP tool calling";

    @McpToolParam(description = TRACE_ID_DESC, required = false)
    private String starryTraceId;

    public void setStarryTraceId(String starryTraceId) {
        this.starryTraceId = starryTraceId;
        MdcUtil.setTraceId(starryTraceId);
    }
}
