package com.walter.starry.business.app.controller;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Slf4j
public class MenuControllerTest{

    @Test
    @SneakyThrows
    void listAuthorityTree(){
        HttpRequest request = HttpRequest
                .newBuilder()
                .uri(URI.create("http://127.0.0.1:8080/admin/menu/authority/listTree?menuItemCode=AdminPage"))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .header("Cookie", "SESSION=MzQ0OGQxYjgtYjU1ZC00YTU3LWJhMzUtM2M2ZjJlOTI0N2Y1")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(500)).build()){
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.printf("Status %s \n", response.statusCode());
            System.out.printf("Response %s \n", response.body());
        }
    }
}
