package com.example.tamagochi.grpcenrichment.config;

import com.example.tamagochi.events.RoutingKeys;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/**
 * Конфигурация RabbitMQ для enrichment-клиента.
 *
 * Этот сервис одновременно и consumer (слушает tamagochi.created),
 * и publisher (публикует tamagochi.enriched). Поэтому конфигурация включает:
 * - Exchange (общий для всех сервисов)
 * - Очередь для приёма tamagochi.created
 * - DLQ для необработанных сообщений
 * - RabbitTemplate для публикации tamagochi.enriched
 *
 * Каждый consumer определяет свою очередь и привязку.
 * Enrichment-клиент слушает только tamagochi.created (не все события),
 * поэтому binding key = "tamagochi.created", а не "#".
 */
@Configuration
public class RabbitMQConfig {

    public static final String ENRICHMENT_QUEUE = "q.enrichment.tamagochi-created";
    public static final String ENRICHMENT_DLQ = "q.enrichment.tamagochi-created.dlq";

    // JSON-конвертер

    @Bean
    public MessageConverter jsonMessageConverter(JsonMapper jsonMapper) {
        return new JacksonJsonMessageConverter(jsonMapper);
    }

    // RabbitTemplate для публикации событий

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }

    // Listener Container Factory

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(3);
        factory.setDefaultRequeueRejected(false); // при ошибке → DLQ
        return factory;
    }

    // Exchange

    @Bean
    public TopicExchange eventsExchange() {
        return ExchangeBuilder
                .topicExchange(RoutingKeys.EXCHANGE)
                .durable(true)
                .build();
    }

    // Dead Letter Exchange

    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder
                .directExchange(RoutingKeys.EXCHANGE + ".dlx")
                .durable(true)
                .build();
    }

    // Очередь для tamagochi.created
    // Только события создания тамагочи — binding key "tamagochi.created" (не "#")

    @Bean
    public Queue enrichmentQueue() {
        return QueueBuilder
                .durable(ENRICHMENT_QUEUE)
                .deadLetterExchange(RoutingKeys.EXCHANGE + ".dlx")
                .deadLetterRoutingKey(ENRICHMENT_DLQ)
                .build();
    }

    // Dead Letter Queue

    @Bean
    public Queue enrichmentDlq() {
        return QueueBuilder.durable(ENRICHMENT_DLQ).build();
    }

    // Привязки

    /**
     * Привязка только tamagochi.created к очереди enrichment.
     * В отличие от audit-service (binding "#" — все события),
     * enrichment-клиент подписан только на создание тамагочи.
     */
    @Bean
    public Binding enrichmentBinding(Queue enrichmentQueue, TopicExchange eventsExchange) {
        return BindingBuilder
                .bind(enrichmentQueue)
                .to(eventsExchange)
                .with(RoutingKeys.TAMAGOCHI_CREATED); // только tamagochi.created
    }

    @Bean
    public Binding enrichmentDlqBinding(Queue enrichmentDlq, DirectExchange deadLetterExchange) {
        return BindingBuilder
                .bind(enrichmentDlq)
                .to(deadLetterExchange)
                .with(ENRICHMENT_DLQ);
    }
}
