package com.walter.starry.autoconfigure.ai;

import com.walter.starry.common.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.UrlResource;
import org.springframework.util.MimeTypeUtils;

import java.util.List;

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

    record AreaResponse(List<Area> areas){}
    record Area(String areaName, List<String> provinceNames) {}
}
