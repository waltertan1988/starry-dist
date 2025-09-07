package com.walter.starry.autoconfigure.mdc.ai.mcp.server;

import com.walter.starry.common.util.MdcUtil;
import lombok.Getter;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.Serializable;

/**
 * 支持MDC的MCP工具请求体
 * @author walter.tan
 */
@Getter
public class MdcMcpToolParam implements Serializable {
    @ToolParam(description = "调用本方法的starryTraceId", required = false)
    private String starryTraceId;

    public void setStarryTraceId(String starryTraceId) {
        this.starryTraceId = starryTraceId;
        MdcUtil.setTraceId(starryTraceId);
    }
}
