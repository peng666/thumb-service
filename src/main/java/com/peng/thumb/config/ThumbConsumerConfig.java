package com.peng.thumb.config;

import lombok.Builder;
import org.apache.pulsar.client.api.BatchReceivePolicy;
import org.apache.pulsar.client.api.ConsumerBuilder;
import org.apache.pulsar.client.api.DeadLetterPolicy;
import org.apache.pulsar.client.api.RedeliveryBackoff;
import org.apache.pulsar.client.impl.MultiplierRedeliveryBackoff;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.pulsar.annotation.PulsarListenerConsumerBuilderCustomizer;

import java.util.concurrent.TimeUnit;

@Configuration
public class ThumbConsumerConfig<T> implements PulsarListenerConsumerBuilderCustomizer<T> {
    @Override
    public void customize(ConsumerBuilder<T> consumerBuilder) {
        consumerBuilder.batchReceivePolicy(
                BatchReceivePolicy.builder()
                        // 每次处理1000条消息
                        .maxNumMessages(1000)
                        // 设置超时时间为10秒
                        .timeout(10000, TimeUnit.MILLISECONDS)
                        .build()
        );
    }

    /**
     * 配置消费者的NACK重试策略
     *
     * @return
     */
    @Bean
    public RedeliveryBackoff negativeAckRedeliveryBackoff() {
        return MultiplierRedeliveryBackoff.builder()
                // 初始延迟时间为1秒
                .minDelayMs(1000)
                // 最大延迟时间为60秒
                .maxDelayMs(60_000)
                // 每次重试的延时倍数
                .multiplier(2)
                .build();
    }

    /**
     * 配置消费者的ACK超时重试策略
     *
     * @return
     */
    @Bean
    public RedeliveryBackoff ackTimeoutRedeliveryBackoff() {
        return MultiplierRedeliveryBackoff.builder()
                // 初始延迟时间为5秒
                .minDelayMs(5000)
                // 最大延迟时间为300秒
                .maxDelayMs(300_000)
                // 每次重试的延时倍数
                .multiplier(3)
                .build();
    }

    /**
     * 配置死信队列的策略
     *
     * @return
     */
    @Bean
    public DeadLetterPolicy deadLetterPolicy() {
        return DeadLetterPolicy.builder()
                // 最大重试次数
                .maxRedeliverCount(3)
                // 死信队列的主题名称
                .deadLetterTopic("thumb-dlq-topic")
                .build();
    }
}
