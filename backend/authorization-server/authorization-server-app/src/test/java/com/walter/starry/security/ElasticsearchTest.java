package com.walter.starry.security;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.InfoResponse;
import com.walter.starry.authorization.server.app.AuthorizationServerApplication;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

/**
 * @Author: walter.tan
 * @DateTime: 2024-03-09 19:22:15
 */
@SpringBootTest(classes = AuthorizationServerApplication.class)
public class ElasticsearchTest {
    private final static Logger logger = LoggerFactory.getLogger(ElasticsearchTest.class);

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Nested
    class InfoTest {
        @Test
        void info() throws IOException {
            InfoResponse info = elasticsearchClient.info();
            logger.info("elasticsearch info. clusterName:{}, clusterUuid:{}", info.clusterName(), info.clusterUuid());
        }
    }
}
