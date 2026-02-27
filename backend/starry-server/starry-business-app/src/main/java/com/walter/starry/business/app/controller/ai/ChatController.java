package com.walter.starry.business.app.controller.ai;

import com.walter.starry.business.app.vo.ai.ChatCallReq;
import com.walter.starry.common.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Random;
import java.util.UUID;

/**
 * AI聊天功能的控制器
 * @author walter.tan
 */
@Slf4j
@RestController
@RequestMapping("/ai/chat")
public class ChatController {
    @Autowired
    private OpenAiChatModel openAiChatModel;

    /**
     * 调用AI的聊天
     * @param req 聊天请求参数
     * @return AI的回复
     */
    @PostMapping(value = "/call", produces = "text/plain;charset=UTF-8")
    public Flux<String> call(@RequestBody ChatCallReq req) {
        String requestId = UUID.randomUUID().toString();
        log.info("chat call start. requestId: {}, req: {}", requestId, JsonUtil.toJson(req));

        return (StringUtils.isBlank(req.getContent()) ? Flux.just("请先输入您的内容") :
                ChatClient
                        .builder(openAiChatModel)
                        .build()
                        .prompt(req.getContent())
                        .stream()
                        .content()
        ).doOnComplete(() -> log.info("chat call finished. requestId: {}", requestId));
    }
}
