package com.walter.starry.autoconfigure.ai;

import com.walter.starry.ai.mcp.server.remote.StarryInfoRes;
import com.walter.starry.autoconfigure.ai.core.rag.ChineseTokenTextSplitter;
import com.walter.starry.autoconfigure.ai.core.tool.ExtSyncMcpToolCallbackProvider;
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
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.UrlResource;
import org.springframework.util.MimeTypeUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@SpringBootTest(classes = AiApplication.class)
public class AiTest {

    @Nested
    class ModelTest{
        @Nested
        class AudioModelTest {
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
        class ChatModelTest {
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

            record AreaResponse(List<Area> areas){}
            record Area(String areaName, List<String> provinceNames) {}
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
                    .toolCallbacks(new ExtSyncMcpToolCallbackProvider(mcpSyncClients.getFirst(), Set.of("getStarryInfo")))
                    .call()
                    .content();
            System.out.println(content);
        }

        @Test
        void mdcStream(){
            ChatClient.create(openAiChatModel)
                    .prompt("请提供ID为123456的用户的个人简介")
                    .advisors(a -> a.advisors(new MdcMcpAdvisor()).param(MdcUtil.ATTR_TRACE_ID, "12345678"))
                    .toolCallbacks(new ExtSyncMcpToolCallbackProvider(mcpSyncClients.getFirst(), Set.of("getUserInfo")))
                    .stream()
                    .content()
                    .doOnNext(System.out::print)
                    .doOnComplete(() -> System.out.println("\n~~~~~~~~~~~~~~~~~~~"))
                    .blockLast();
        }
    }

    @Nested
    class AgentTest{
        @Autowired
        private OpenAiChatModel openAiChatModel;
        @Autowired
        private List<McpSyncClient> mcpSyncClients;

        /**
         * 手动编排
         */
        @Test
        void getStarryAuthorInfoAgent1(){
            MdcUtil.setTraceId(MdcUtil.genNewTraceId());
            log.info("getStarryAuthorInfoAgent start.");

            ToolCallback[] toolCallBacks = new ExtSyncMcpToolCallbackProvider(mcpSyncClients.getFirst(), Set.of("getStarryInfo", "getUserInfo")).getToolCallbacks();

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

        /**
         * 全自动编排
         */
        @Test
        void getStarryAuthorInfoAgent2(){
            MdcUtil.setTraceId(MdcUtil.genNewTraceId());
            log.info("getStarryAuthorInfoAgent2 start.");

            String result = ChatClient.create(openAiChatModel)
                    .prompt("我要获取Starry系统的作者的用户信息，请依次调用合适的工具方法给我返回最终答案。要求：调用工具的过程中，不存在的数据用null填充，严禁胡乱编造。")
                    .advisors(new MdcMcpAdvisor())
                    .toolCallbacks(new ExtSyncMcpToolCallbackProvider(mcpSyncClients.getFirst()))
                    .call()
                    .content();
            log.info("result: {}", result);
        }

        /**
         * 半自动编排
         */
        @Test
        void getStarryAuthorInfoAgent3(){
            MdcUtil.setTraceId(MdcUtil.genNewTraceId());
            log.info("getStarryAuthorInfoAgent3 start.");

            Map<String, ToolCallback> toolCallbackMap = Arrays.stream(new ExtSyncMcpToolCallbackProvider(mcpSyncClients.getFirst()).getToolCallbacks())
                    .collect(Collectors.toMap(tcb -> tcb.getToolDefinition().name(), Function.identity()));

            List<String> plannedToolCallbackList = ChatClient.create(openAiChatModel)
                    .prompt("我要获取Starry系统的作者的用户信息，需要依次调用哪些工具？最后请把工具的名称以数组形式按需要调用的顺序返回。")
                    .advisors(new MdcMcpAdvisor())
                    .toolCallbacks(toolCallbackMap.values().stream().toList())
                    .call()
                    .entity(new ParameterizedTypeReference<>() {});

            Objects.requireNonNull(plannedToolCallbackList).forEach(System.out::println);
            // TODO tyx 半自动编排
        }
    }

    @Nested
    class RagTest{
        @Nested
        class EtlTest{
            @Test
            void textReader(){
                // 读取文本文档
                final String fileName = "C:/Users/walter.tan/Desktop/1.txt";
                TextReader reader = new TextReader(new PathResource(fileName));

                List<Document> documents = reader.read();
                System.out.println("documents.size(): " + documents.size());
                reader.getCustomMetadata().put("filename", fileName);
                for (Document document : documents) {
                    System.out.println(document.getText());
                }

                List<Document> splitDocuments = new TokenTextSplitter().apply(reader.read());
                System.out.println("splitDocuments size: " + splitDocuments.size());
                for (Document document : splitDocuments) {
                    System.out.println(document.getText());
                }

                List<Document> splitChineseDocuments = new ChineseTokenTextSplitter().apply(reader.read());
                System.out.println("splitChineseDocuments size: " + splitChineseDocuments.size());
                for (Document document : splitChineseDocuments) {
                    System.out.println(document.getText());
                }
            }

            @Test
            void markdownDocumentReader(){
                // 读取Markdown文档
                final String fileName = "C:/projects/mine/starry-dist/backend/starry-server/README.md";
                MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                        .withHorizontalRuleCreateDocument(true)
                        .withIncludeCodeBlock(false)
                        .withIncludeBlockquote(false)
                        .withAdditionalMetadata("filename", "README.md")
                        .build();

                MarkdownDocumentReader reader = new MarkdownDocumentReader(new PathResource(fileName), config);
                List<Document> documents = reader.read();
                System.out.println("documents.size(): " + documents.size());
                for (Document document : documents) {
                    System.out.println(document.getText());
                }
            }

            @Test
            void pagePdfDocumentReader(){
                // 按页读取PDF文档
                final String fileName = "C:/公司资料/1.集团HR制度/3.《加班管理制度》-20110101.pdf";
                PagePdfDocumentReader reader = new PagePdfDocumentReader(new PathResource(fileName),
                        PdfDocumentReaderConfig.builder()
                                .withPageTopMargin(0)
                                .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                                        .withNumberOfTopTextLinesToDelete(0)
                                        .build())
                                .withPagesPerDocument(1)
                                .build());
                List<Document> documents = reader.read();
                System.out.println("documents.size(): " + documents.size());
                for (Document document : documents) {
                    System.out.println(document.getText());
                }
            }

            @Test
            void tikaDocumentReader(){
                final String fileName = "C:/projects/mine/starry-dist/doc/Starry系统使用说明.docx";
                TikaDocumentReader reader = new TikaDocumentReader(new PathResource(fileName));
                List<Document> documents = reader.read();
                System.out.println("documents.size(): " + documents.size());
                for (Document document : documents) {
                    System.out.println(document.getText());
                }
            }
        }
    }
}
