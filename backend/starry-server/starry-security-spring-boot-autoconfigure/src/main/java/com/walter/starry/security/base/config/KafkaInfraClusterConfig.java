package com.walter.starry.security.base.config;

import com.walter.starry.common.constant.MqConstant;
import com.walter.starry.security.base.component.kafka.KafkaSendResultListener;
import com.walter.starry.security.base.config.properties.AppMsgProps;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
@ConditionalOnProperty(name = MqConstant.Kafka.CONDITIONAL_ON_PROP)
public class KafkaInfraClusterConfig {
    @Autowired
    private AppMsgProps appMsgProps;

    @Bean("infraKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, String> infraKafkaListenerContainerFactory() {
        AppMsgProps.Kafka.Cfg cfg = appMsgProps.getKafka().getInfraClusterCfg();

        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(this.consumerFactory());
        factory.setConcurrency(cfg.getConsumer().getConcurrency());
        factory.getContainerProperties().setPollTimeout(cfg.getConsumer().getMaxPollTimeOut());
        return factory;
    }

    private ConsumerFactory<String, String> consumerFactory() {
        AppMsgProps.Kafka.Cfg cfg = appMsgProps.getKafka().getInfraClusterCfg();
        return new DefaultKafkaConsumerFactory<>(this.consumerProps(cfg));
    }

    private Map<String, Object> consumerProps(AppMsgProps.Kafka.Cfg cfg) {
        Map<String, Object> props = new HashMap<>();
        //连接地址
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, cfg.getBootstrapServers());
        //每次拉取最大条数
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, cfg.getConsumer().getMaxPollRecords());
        //是否自动提交
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, cfg.getConsumer().getAutoCommit());
        //自动提交的频率
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, cfg.getConsumer().getAutoCommitInterval());
        //
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, cfg.getConsumer().getAutoOffsetReset());
        //Session超时设置
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, cfg.getConsumer().getSessionTimeout());
        //键的反序列化方式
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        //值的反序列化方式
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        // ...
        return props;
    }

    @Bean
    public ProducerFactory<String, String> infraProducerFactory() {
        AppMsgProps.Kafka.Cfg cfg = appMsgProps.getKafka().getInfraClusterCfg();
        return new DefaultKafkaProducerFactory<>(this.senderProps(cfg));
    }

    private Map<String, Object> senderProps(AppMsgProps.Kafka.Cfg cfg) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, cfg.getBootstrapServers());
        //重试，0为不启用重试机制
        props.put(ProducerConfig.RETRIES_CONFIG, cfg.getProducer().getRetries());
        //控制批处理大小，单位为字节
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, cfg.getProducer().getBatchSize());
        //批量发送，延迟为1毫秒，启用该功能能有效减少生产者发送消息次数，从而提高并发量
        props.put(ProducerConfig.LINGER_MS_CONFIG, cfg.getProducer().getLingerMs());
        //生产者可以使用的总内存字节来缓冲等待发送到服务器的记录
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, cfg.getProducer().getBufferMemory());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        //...
        return props;
    }

    @Bean("infraKafkaTemplate")
    public KafkaTemplate<String, String> infraKafkaTemplate(KafkaSendResultListener<String, String> kafkaSendResultListener) {
        KafkaTemplate<String, String> template = new KafkaTemplate<>(this.infraProducerFactory());
        template.setProducerListener(kafkaSendResultListener);
        return template;
    }
}
