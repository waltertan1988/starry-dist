package com.walter.starry.autoconfigure.ai.conf;

import com.walter.starry.autoconfigure.ai.core.prop.AppAiVectorStoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RestClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStoreOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 配置各种向量数据库
 */
@Slf4j
@Configuration
public class VectorStoreConf {

    /**
     * ElasticSearch向量数据库
     * @param restClient
     * @param embeddingModel
     * @param props
     * @return
     */
    @Bean
    @ConditionalOnProperty("app.ai.vectorstore.elasticsearch.index-name")
    @ConditionalOnClass(ElasticsearchVectorStore.class)
    public VectorStore elasticsearchVectorStore(RestClient restClient, EmbeddingModel embeddingModel, AppAiVectorStoreProperties props) {
        ElasticsearchVectorStoreOptions options = new ElasticsearchVectorStoreOptions();
        options.setIndexName(props.getElasticsearch().getIndexName());
        options.setSimilarity(props.getElasticsearch().getSimilarity());
        options.setDimensions(props.getElasticsearch().getDimensions());
        options.setEmbeddingFieldName(props.getElasticsearch().getEmbeddingFieldName());

        return ElasticsearchVectorStore.builder(restClient, embeddingModel)
                .options(options)
                .initializeSchema(props.getElasticsearch().isInitializeSchema())
                .batchingStrategy(new TokenCountBatchingStrategy()) // Optional: defaults to TokenCountBatchingStrategy
                .build();
    }
}
