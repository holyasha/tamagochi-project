package com.example.tamagochi.grpcenrichment.publisher;

import com.example.tamagochi.events.TamagochiEvent;
import com.example.tamagochi.events.EventEnvelope;
import com.example.tamagochi.events.RoutingKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Публикация событий обогащения (tamagochi.enriched) в RabbitMQ.
 *
 * Аналогичен TamagochiEventPublisher в tamagochirest, но публикует другой тип события.
 * Паттерн fire-and-forget: если RabbitMQ недоступен, ошибка логируется,
 * но gRPC-вызов уже выполнен — результат не теряется полностью.
 */
@Component
public class EnrichmentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EnrichmentEventPublisher.class);
    private static final String SOURCE = "grpc-enrichment-client";

    private final RabbitTemplate rabbitTemplate;

    public EnrichmentEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Публикует событие tamagochi.enriched с результатами gRPC-аналитики.
     */
    public void publishEnriched(TamagochiEvent.Enriched enrichedEvent) {
        try {
            EventEnvelope<TamagochiEvent> envelope = EventEnvelope.wrap(
                    enrichedEvent, SOURCE, RoutingKeys.TAMAGOCHI_ENRICHED);

            rabbitTemplate.convertAndSend(
                    RoutingKeys.EXCHANGE,
                    RoutingKeys.TAMAGOCHI_ENRICHED,
                    envelope);

            log.info("Событие отправлено: {} [tamagochiId={}, eventId={}]",
                    RoutingKeys.TAMAGOCHI_ENRICHED,
                    enrichedEvent.tamagochiId(),
                    envelope.metadata().eventId());

        } catch (Exception e) {
            log.error("Не удалось отправить событие {}: {}",
                    RoutingKeys.TAMAGOCHI_ENRICHED, e.getMessage());
        }
    }
}
