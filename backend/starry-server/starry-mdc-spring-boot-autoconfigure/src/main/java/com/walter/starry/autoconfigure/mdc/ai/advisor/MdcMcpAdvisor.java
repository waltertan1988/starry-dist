package com.walter.starry.autoconfigure.mdc.ai.advisor;

import com.walter.starry.autoconfigure.mdc.ai.mcp.server.MdcMcpToolParam;
import com.walter.starry.common.util.MdcUtil;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.Ordered;

import java.util.HashMap;
import java.util.Map;

/**
 * @author walter.tan
 */
public class MdcMcpAdvisor implements BaseAdvisor {

    private static final String DEFAULT_MDC_TEMPLATE = """
			{input_query}
			In addition, %s is {starryTraceId}.
			""".formatted(MdcMcpToolParam.TRACE_ID_DESC);

    private final String mdcTemplate;

    public MdcMcpAdvisor() {
        this(DEFAULT_MDC_TEMPLATE);
    }

    public MdcMcpAdvisor(String mdcTemplate) {
        this.mdcTemplate = mdcTemplate;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        Map<String, Object> varMap = new HashMap<>(2);
        varMap.put("input_query", chatClientRequest.prompt().getSystemMessage().getText());
        String traceId = ObjectUtils.firstNonNull(chatClientRequest.context().get(MdcUtil.ATTR_TRACE_ID), MdcUtil.getTraceId(), StringUtils.EMPTY).toString();
        varMap.put("starryTraceId", traceId);

        String augmentedText = PromptTemplate.builder()
                .template(this.mdcTemplate)
                .variables(varMap)
                .build()
                .render();
        return chatClientRequest.mutate()
                .prompt(chatClientRequest.prompt().augmentSystemMessage(augmentedText))
                .build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        return chatClientResponse;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
