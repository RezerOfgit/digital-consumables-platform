package com.dcp.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置：声明交换机、队列和绑定关系。
 * AI 风控使用 Direct 交换机，精确匹配 routing key。
 * @author Re-zero
 * @version 1.0
 */
@Configuration
public class RabbitMQConfig {

    /** 交换机名称 */
    public static final String EXCHANGE_NAME = "dcp.ai.risk.exchange";

    /** 单品风控队列 */
    public static final String QUEUE_SINGLE = "dcp.ai.risk.single";

    /** 批量风控队列 */
    public static final String QUEUE_BATCH = "dcp.ai.risk.batch";

    /** 单品路由键 */
    public static final String ROUTING_KEY_SINGLE = "ai.risk.single";

    /** 批量路由键 */
    public static final String ROUTING_KEY_BATCH = "ai.risk.batch";

    @Bean
    public DirectExchange aiRiskExchange() {
        // durable=true 重启不丢失
        return new DirectExchange(EXCHANGE_NAME, true, false);
    }

    @Bean
    public Queue singleRiskQueue() {
        return QueueBuilder.durable(QUEUE_SINGLE).build();
    }

    @Bean
    public Queue batchRiskQueue() {
        return QueueBuilder.durable(QUEUE_BATCH).build();
    }

    @Bean
    public Binding singleBinding(Queue singleRiskQueue, DirectExchange aiRiskExchange) {
        return BindingBuilder.bind(singleRiskQueue).to(aiRiskExchange).with(ROUTING_KEY_SINGLE);
    }

    @Bean
    public Binding batchBinding(Queue batchRiskQueue, DirectExchange aiRiskExchange) {
        return BindingBuilder.bind(batchRiskQueue).to(aiRiskExchange).with(ROUTING_KEY_BATCH);
    }
}