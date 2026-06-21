package com.walter.starry.autoconfigure.ai.core.prop;

import lombok.Data;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStoreOptions;
import org.springframework.ai.vectorstore.elasticsearch.SimilarityFunction;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 应用的向量数据库配置
 */
@Data
@Component
@ConditionalOnClass(ElasticsearchVectorStore.class)
@ConfigurationProperties("app.ai.vectorstore")
public class AppAiVectorStoreProperties {
    /**
     * ElasticSearch向量数据库的配置字段及含义参照：{@link ElasticsearchVectorStoreOptions}
     */
    private Elasticsearch elasticsearch;

    @Data
    public static class Elasticsearch {
        boolean initializeSchema = false;
        private String indexName = "spring-ai-document-index";
        private int dimensions = 1536;
        private SimilarityFunction similarity = SimilarityFunction.cosine;
        private String embeddingFieldName = "embedding";
    }
}
