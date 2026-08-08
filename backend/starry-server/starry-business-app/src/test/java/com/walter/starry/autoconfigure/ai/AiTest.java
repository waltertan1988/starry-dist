package com.walter.starry.autoconfigure.ai;

import com.google.common.collect.Lists;
import com.openai.models.audio.AudioResponseFormat;
import com.walter.starry.ai.mcp.server.remote.AclAuthorityItemRes;
import com.walter.starry.ai.mcp.server.remote.StarryInfoRes;
import com.walter.starry.autoconfigure.ai.core.rag.ChineseTokenTextSplitter;
import com.walter.starry.autoconfigure.ai.core.tool.ExtSyncMcpToolCallbackProvider;
import com.walter.starry.autoconfigure.mdc.ai.advisor.MdcMcpAdvisor;
import com.walter.starry.business.app.BusinessApplication;
import com.walter.starry.common.util.JsonUtil;
import com.walter.starry.common.util.MdcUtil;
import io.modelcontextprotocol.client.McpSyncClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.*;
import org.springframework.ai.openai.metadata.OpenAiAudioSpeechResponseMetadata;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.UrlResource;
import org.springframework.util.MimeTypeUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@SpringBootTest(classes = BusinessApplication.class)
public class AiTest {
    @Autowired
    private OpenAiChatModel openAiChatModel;
    @Autowired
    private OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel;
    @Autowired
    private OpenAiAudioSpeechModel openAiAudioSpeechModel;
    @Autowired
    private JdbcChatMemoryRepository jdbcChatMemoryRepository;
    @Autowired
    private VectorStore vectorStore;

    @Nested
    class ModelTest{
        @Nested
        class AudioModelTest {

            @Test
            void asr(){
                OpenAiAudioTranscriptionOptions transcriptionOptions = OpenAiAudioTranscriptionOptions.builder()
                        .responseFormat(AudioResponseFormat.TEXT)
                        .temperature(0f)
                        .build();
                FileSystemResource audioFile = new FileSystemResource("E:/Download/《海滨仲夏夜》示范朗读.mp3");
                AudioTranscriptionPrompt transcriptionRequest = new AudioTranscriptionPrompt(audioFile, transcriptionOptions);
                AudioTranscriptionResponse response = openAiAudioTranscriptionModel.call(transcriptionRequest);
                System.out.println(response.getResult().getOutput());
            }

            @Test
            void tts() throws IOException {
                File outputFile = new File("E:/Download/tts.mp3");
                if(outputFile.exists()){
                    outputFile.delete();
                }

                var speechPrompt = new TextToSpeechPrompt("你站在桥上看风景，看风景的人在楼上看你。明月装饰了你的窗子，你装饰了别人的梦");
                TextToSpeechResponse response = openAiAudioSpeechModel.call(speechPrompt);

                byte[] responseAsBytes = response.getResult().getOutput();
                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    fos.write(responseAsBytes);
                }

                OpenAiAudioSpeechResponseMetadata metadata = (OpenAiAudioSpeechResponseMetadata) response.getMetadata();
                System.out.println(metadata);
            }
        }

        @Nested
        class ChatModelTest {
            private static final String PROMPT_TMPL_2 = "{subject}的{property}是？";

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
            final String mcpProgressToken = "wxyz";

            MdcUtil.setTraceId(MdcUtil.genNewTraceId());

            OpenAiChatOptions openAiChatOptions = OpenAiChatOptions.builder()
                    .customHeaders(Collections.emptyMap())
                    .build();

            Prompt prompt = Prompt.builder()
                    .content("请为我介绍Starry系统的基本信息")
                    .chatOptions(openAiChatOptions)
                    .build();

            log.info("开始执行.");
            String content = ChatClient.create(openAiChatModel)
                    .prompt(prompt)
                    .toolContext(Map.of(
                            "progressToken", mcpProgressToken // 要使用McpProgress能力，必须传递progressToken
                    ))
                    .advisors(new MdcMcpAdvisor())
                    .toolCallbacks(new ExtSyncMcpToolCallbackProvider(mcpSyncClients.getFirst(), Set.of("getStarryInfo")))
                    .call()
                    .content();
            System.out.println(content);
        }

        @Test
        void mdcStream(){
            final String mcpProgressToken = "abcd";
            final String mcpTraceId = "12345678";
            final String uid = "PC9527";

            ChatClient.create(openAiChatModel)
                    .prompt("请提供ID为%s的用户的个人简介".formatted(uid))
                    .advisors(a -> a.advisors(new MdcMcpAdvisor()).param(MdcUtil.ATTR_TRACE_ID, mcpTraceId))
                    .toolContext(Map.of(
                            "progressToken", mcpProgressToken, // 要使用McpProgress能力，必须传递progressToken
                            MdcUtil.ATTR_TRACE_ID, mcpTraceId
                    ))
                    .toolCallbacks(new ExtSyncMcpToolCallbackProvider(mcpSyncClients.getFirst(), Set.of("getUserInfo")))
                    .stream()
                    .content()
                    .doOnNext(System.out::print)
                    .doOnComplete(() -> System.out.println("\n~~~~~~~~~~~~~~~~~~~"))
                    .blockLast();
        }

        @Test
        void pageQueryAclAuthorityItem(){
            final String mcpTraceId = "12345678";
            final String searchName = "管理员";

            List<AclAuthorityItemRes> list = ChatClient.create(openAiChatModel)
                    .prompt("请查询名字中包含“%s”这%s个字的权限项配置记录".formatted(searchName, searchName.length()))
                    .advisors(a -> a.advisors(new MdcMcpAdvisor()).param(MdcUtil.ATTR_TRACE_ID, mcpTraceId))
                    .toolContext(Map.of(MdcUtil.ATTR_TRACE_ID, mcpTraceId))
                    .toolCallbacks(new ExtSyncMcpToolCallbackProvider(mcpSyncClients.getFirst(), Set.of("pageQueryAclAuthorityItem")))
                    .call()
                    .entity(new ParameterizedTypeReference<List<AclAuthorityItemRes>>() {
                    });

            System.out.println(JsonUtil.toJson(list));
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
        @Autowired
        private OpenAiEmbeddingModel openAiEmbeddingModel;

        @Nested
        class EtlTest{
            @Test
            void textReader(){
                // 读取文本文档
                final String fileName = "C:/Users/walter.tan/Desktop/1.txt";
                TextReader reader = new TextReader(new FileSystemResource(fileName));

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

                MarkdownDocumentReader reader = new MarkdownDocumentReader(new FileSystemResource(fileName), config);
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
                PagePdfDocumentReader reader = new PagePdfDocumentReader(new FileSystemResource(fileName),
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
                TikaDocumentReader reader = new TikaDocumentReader(new FileSystemResource(fileName));
                List<Document> documents = reader.read();
                System.out.println("documents.size(): " + documents.size());
                for (Document document : documents) {
                    System.out.println(document.getText());
                }
            }
        }

        @Nested
        class RagSearchTest{
            private static final String TAG_JIN_YONG = "金庸武侠小说";
            private static final String TAG_STARRY = "Starry系统";

            @Test
            void embedding(){
                float[] embeds = openAiEmbeddingModel.embed("你好");
                System.out.println(embeds.length);
                System.out.println(Arrays.toString(embeds));
            }

            @Test
            void addMarkdown(){
                // 读取Markdown文档
                final String fileName = "C:/projects/mine/starry-dist/backend/starry-server/README.md";
                MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                        .withHorizontalRuleCreateDocument(true)
                        .withIncludeCodeBlock(false)
                        .withIncludeBlockquote(false)
                        .withAdditionalMetadata("tag", TAG_STARRY)
                        .build();

                MarkdownDocumentReader reader = new MarkdownDocumentReader(new FileSystemResource(fileName), config);
                List<Document> documents = new ChineseTokenTextSplitter().apply(reader.read());
                int remainDoc = documents.size();
                for (List<Document> documentSubList : Lists.partition(documents, 10)) {
                    vectorStore.add(documentSubList);
                    remainDoc -= documentSubList.size();
                    System.out.println("remain docs: " + remainDoc);
                }
            }

            @Test
            void addText(){
                // 读取文本文档
                final String fileName = "C:/Users/think/Downloads/天龙八部.txt";
                TextReader reader = new TextReader(new FileSystemResource(fileName));
                reader.getCustomMetadata().put("tag", TAG_JIN_YONG);
                List<Document> documentList = new ChineseTokenTextSplitter().apply(reader.read());
                int remainDoc = documentList.size();
                for (List<Document> documentSubList : Lists.partition(documentList, 10)) {
                    vectorStore.add(documentSubList);
                    remainDoc -= documentSubList.size();
                    System.out.println("remain docs: " + remainDoc);
                }
            }

            @Test
            void similaritySearch(){
                List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder()
                        .filterExpression(String.format("tag == '%s'", TAG_JIN_YONG))
                        .query("神木王鼎有什么作用？")
                        .topK(3)
                        .build());
                for (Document document : documents) {
                    System.out.println(document.getText());
                    System.out.println("---------------------------");
                }
            }

            @Test
            void delete(){
                vectorStore.delete(String.format("source == '%s'", "天龙八部.txt"));
            }

            @Test
            void ragSearchWithJdbcChatMemory(){
                // 聊天记忆的advisor
                ChatMemory chatMemory = MessageWindowChatMemory.builder().chatMemoryRepository(jdbcChatMemoryRepository).maxMessages(5).build();
                Advisor messageChatMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();

                // RAG的advisor
                Advisor retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
                        .documentRetriever(VectorStoreDocumentRetriever.builder()
                                .similarityThreshold(0.50)
                                .vectorStore(vectorStore)
                                .build())
                        .queryAugmenter(ContextualQueryAugmenter.builder()
                                .allowEmptyContext(false)
                                .emptyContextPromptTemplate(PromptTemplate.builder().template("直接用以下句子的原文回复用户，且不要补充任何文字：“抱歉，这个问题已超出我的能力范围了。”").build())
                                .build())
                        .build();

                ChatClient.builder(openAiChatModel).build()
                        .prompt()
                        .advisors(retrievalAugmentationAdvisor, messageChatMemoryAdvisor)
                        .advisors(a -> a.params(Map.of(
                                VectorStoreDocumentRetriever.FILTER_EXPRESSION, String.format("source == '%s'", "天龙八部.txt"),
                                ChatMemory.CONVERSATION_ID, 1759996851659L
                        )))
                        .user("神木王鼎有什么作用？")
                        .stream()
                        .content()
                        .doOnNext(System.out::print)
                        .doOnComplete(() -> System.out.println("\n~~~~~~~~~~~~~~~~~~~"))
                        .blockLast();
            }
        }
    }
}
