package com.walter.starry.autoconfigure.ai;

import com.walter.starry.ai.mcp.server.remote.StarryInfoRes;
import com.walter.starry.autoconfigure.ai.core.StarryMcpToolCallbackProvider;
import com.walter.starry.autoconfigure.mdc.ai.advisor.MdcMcpAdvisor;
import com.walter.starry.common.util.JsonUtil;
import com.walter.starry.common.util.MdcUtil;
import io.modelcontextprotocol.client.McpSyncClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.UrlResource;
import org.springframework.util.MimeTypeUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@SpringBootTest(classes = AiApplication.class)
public class AiTest {

    @Nested
    class AudioTest {
        @Autowired
        private OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel;

        @Test
        void asr(){
            OpenAiAudioTranscriptionOptions transcriptionOptions = OpenAiAudioTranscriptionOptions.builder()
                    .responseFormat(OpenAiAudioApi.TranscriptResponseFormat.TEXT)
                    .temperature(0f)
                    .build();
            FileSystemResource audioFile = new FileSystemResource("C:/Users/walter.tan/Downloads/普通话27_20s.mp3");
            AudioTranscriptionPrompt transcriptionRequest = new AudioTranscriptionPrompt(audioFile, transcriptionOptions);
            AudioTranscriptionResponse response = openAiAudioTranscriptionModel.call(transcriptionRequest);
            System.out.println(response.getResult().getOutput());
        }
    }

    @Nested
    class ChatTest {
        private static final String PROMPT_TMPL_2 = "{subject}的{property}是？";
        @Autowired
        private OpenAiChatModel openAiChatModel;

        @Test
        void call(){
            System.out.println(openAiChatModel.call("中国的首都是哪里？"));
        }

        @Test
        void callAndReturnEntity(){
            AreaResponse areaResponse = ChatClient.builder(openAiChatModel).build()
                    .prompt("中国各个地区分别包含了哪些省份？请输出中文内容。")
                    .call()
                    .entity(AreaResponse.class);
            System.out.println(JsonUtil.toJson(areaResponse));
        }

        @Test
        void promptTemplate(){
            String content = ChatClient.builder(openAiChatModel).build()
                    .prompt()
                    .user(u -> u.text(PROMPT_TMPL_2)
                            .param("subject", "中国")
                            .param("property", "首都")
                    ).call()
                    .content();
            System.out.println(content);
        }

        @Test
        void stream(){
            String last = ChatClient.builder(openAiChatModel).build()
                    .prompt("为婚礼挑选主持人时需要考察他哪些特质？请按重要性从高到低列出来")
                    .stream()
                    .content()
                    .doOnNext(System.out::print)
                    .doOnComplete(() -> System.out.println("\n~~~~~~~~~~~~~~~~~~~"))
                    .blockLast();
            System.out.println(">>>>>> last=" + last);
        }

        @Test
        void multiModality() {
            String picUrl = "https://docs.spring.io/spring-ai/reference/_images/multimodal.test.png";
            ChatClient.create(openAiChatModel)
                    .prompt()
                    .user(u -> u.text("描述一下在以下图片中看到了什么？")
                            .media(MimeTypeUtils.IMAGE_PNG, UrlResource.from(picUrl)))
                    .stream()
                    .content()
                    .doOnNext(System.out::print)
                    .doOnComplete(() -> System.out.println("\n~~~~~~~~~~~~~~~~~~~"))
                    .blockLast();
        }
    }

    @Nested
    class McpClientTest {
        @Autowired
        private OpenAiChatModel openAiChatModel;
        @Autowired
        private List<McpSyncClient> mcpSyncClients;
        @Autowired
        private ChatClient.Builder builder;

        @Test
        void mdcCall(){
            MdcUtil.setTraceId(MdcUtil.genNewTraceId());

            OpenAiChatOptions openAiChatOptions = OpenAiChatOptions.builder()
                    .httpHeaders(Collections.emptyMap())
                    .build();

            Prompt prompt = Prompt.builder()
                    .content("请为我介绍Starry系统的基本信息")
                    .chatOptions(openAiChatOptions)
                    .build();

            log.info("开始执行.");
            String content = ChatClient.create(openAiChatModel)
                    .prompt(prompt)
                    .advisors(new MdcMcpAdvisor())
                    .toolCallbacks(new StarryMcpToolCallbackProvider(mcpSyncClients, Set.of("getStarryInfo")))
                    .call()
                    .content();
            System.out.println(content);
        }

        @Test
        void mdcStream(){
            ChatClient.create(openAiChatModel)
                    .prompt("请提供ID为123456的用户的个人简介")
                    .advisors(a -> a.advisors(new MdcMcpAdvisor()).param(MdcUtil.ATTR_TRACE_ID, "12345678"))
                    .toolCallbacks(new StarryMcpToolCallbackProvider(mcpSyncClients, Set.of("getUserInfo")))
                    .stream()
                    .content()
                    .doOnNext(System.out::print)
                    .doOnComplete(() -> System.out.println("\n~~~~~~~~~~~~~~~~~~~"))
                    .blockLast();
        }
    }

    @Nested
    class Agent{
        @Autowired
        private OpenAiChatModel openAiChatModel;
        @Autowired
        private List<McpSyncClient> mcpSyncClients;

        @Test
        void getStarryAuthorInfoAgent1(){
            MdcUtil.setTraceId(MdcUtil.genNewTraceId());
            log.info("getStarryAuthorInfoAgent start.");

            ToolCallback[] toolCallBacks = new StarryMcpToolCallbackProvider(mcpSyncClients, Set.of("getStarryInfo", "getUserInfo")).getToolCallbacks();

            StarryInfoRes starryInfoRes = ChatClient.create(openAiChatModel)
                    .prompt("请提供Starry系统的基本信息。要求：不存在的数据用null填充，禁止胡乱编造。")
                    .advisors(new MdcMcpAdvisor())
                    .toolCallbacks(toolCallBacks)
                    .call()
                    .entity(StarryInfoRes.class);
            log.info("starryInfoRes: {}", JsonUtil.toJson(starryInfoRes));

            String content = ChatClient.create(openAiChatModel)
                    .prompt().user(u -> u.text("请提供ID为{uid}的用户的个人简介。")
                            .param("uid", Optional.ofNullable(starryInfoRes).map(StarryInfoRes::authorUid).orElse("")))
                    .advisors(new MdcMcpAdvisor())
                    .toolCallbacks(toolCallBacks)
                    .call()
                    .content();
            System.out.println(content);
        }
    }

    record AreaResponse(List<Area> areas){}
    record Area(String areaName, List<String> provinceNames) {}
}
