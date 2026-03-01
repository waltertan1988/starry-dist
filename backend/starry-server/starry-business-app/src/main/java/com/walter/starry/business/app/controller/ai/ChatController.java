package com.walter.starry.business.app.controller.ai;

import com.walter.starry.autoconfigure.ai.core.tool.ExtSyncMcpToolCallbackProvider;
import com.walter.starry.autoconfigure.mdc.ai.advisor.MdcMcpAdvisor;
import com.walter.starry.business.app.entity.UserAiConversation;
import com.walter.starry.business.app.mapper.UserAiConversationMapper;
import com.walter.starry.business.app.vo.ai.ChatCallReq;
import com.walter.starry.common.util.IdUtil;
import com.walter.starry.common.util.JsonUtil;
import com.walter.starry.common.vo.ApiResponse;
import io.modelcontextprotocol.client.McpSyncClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.CurrentSecurityContext;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * AI聊天功能的控制器
 * @author walter.tan
 */
@Slf4j
@RestController
@RequestMapping("/ai/chat")
public class ChatController implements InitializingBean {
    @Autowired
    private OpenAiChatModel openAiChatModel;
    @Autowired
    private JdbcChatMemoryRepository jdbcChatMemoryRepository;
    @Autowired
    private UserAiConversationMapper userAiConversationMapper;
    @Autowired
    private List<McpSyncClient> mcpSyncClients;

    private Advisor promptChatMemoryAdvisor;

    @Override
    public void afterPropertiesSet() {
        // 聊天记忆的advisor
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(jdbcChatMemoryRepository)
                .maxMessages(20)
                .build();
        this.promptChatMemoryAdvisor = PromptChatMemoryAdvisor.builder(chatMemory).build();
    }

    /**
     * 用户获取一个新的对话ID
     * @return 新的对话ID
     */
    @GetMapping("/newConversationId")
    public ApiResponse<Long> getNewConversationId(@CurrentSecurityContext SecurityContext context){
        Date now = new Date();
        Long conversationId = IdUtil.genNextGlobalId();
        UserAiConversation conversation = new UserAiConversation();
        conversation.setUsername(context.getAuthentication().getName());
        conversation.setConversationId(conversationId);
        conversation.setCreateTime(now);
        conversation.setUpdateTime(now);
        userAiConversationMapper.insertSelective(conversation);

        return ApiResponse.success(conversationId);
    }

    /**
     * 调用AI的聊天
     * @param req 聊天请求参数
     * @return AI的回复
     */
    @PostMapping(value = "/call", produces = "text/plain;charset=UTF-8")
    public Flux<String> call(@RequestBody ChatCallReq req) {
        String requestId = UUID.randomUUID().toString();
        log.info("chat call start. requestId: {}, req: {}", requestId, JsonUtil.toJson(req));

        if(StringUtils.isBlank(req.getConversationId())){
            throw new IllegalArgumentException("对话ID不能为空");
        }

        Flux<String> resultFlux;
        if(StringUtils.isBlank(req.getContent())){
            resultFlux = Flux.just("请先输入您的内容");
        }else{
            ChatClient.ChatClientRequestSpec spec = ChatClient
                    .create(openAiChatModel)
                    .prompt(req.getContent())
                    .advisors(promptChatMemoryAdvisor) // 聊天记忆功能
                    .advisors(a -> a.params(Map.of(
                            ChatMemory.CONVERSATION_ID, req.getConversationId() // 聊天记忆的对话ID
                    )));

            if("no".equals(req.getMcpToolId())){
                resultFlux = spec.stream().content();
            }else{
                spec = spec.advisors(new MdcMcpAdvisor()); // 为MCP请求添加MDC信息
                if ("starry".equals(req.getMcpToolId())){
                    // 使用第一个MCP服务(starry)
                    spec = spec.toolCallbacks(new ExtSyncMcpToolCallbackProvider(mcpSyncClients.getFirst()));
                }else{
                    throw new IllegalArgumentException("不支持的MCP工具ID");
                }
                resultFlux = spec.stream().content();
            }
        }

        return resultFlux.doOnComplete(() -> log.info("chat call finished. requestId: {}", requestId));
    }
}
