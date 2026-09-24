package com.example.tamagochirest.event;

import com.example.tamagochi_api_contract.dto.OwnerResponse;
import com.example.tamagochi.events.OwnerEvent;
import com.example.tamagochi.events.EventEnvelope;
import com.example.tamagochi.events.RoutingKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Публикация доменных событий владельцев в RabbitMQ.
 *
 * Аналогичен TamagochiEventPublisher — тот же fire-and-forget паттерн.
 */
@Component
public class OwnerEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OwnerEventPublisher.class);
    private static final String SOURCE = "tamagochirest";

    private final RabbitTemplate rabbitTemplate;

    public OwnerEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Публикует событие «владелец создан».
     */
    public void publishCreated(OwnerResponse owner) {
        var event = new OwnerEvent.Created(
                owner.getId(),
                owner.getName()
        );
        send(RoutingKeys.OWNER_CREATED, event);
    }

    /**
     * Публикует событие «владелец удалён» с количеством каскадно удалённых тамагочи.
     */
    public void publishDeleted(OwnerResponse owner, int deletedTamagochisCount) {
        var event = new OwnerEvent.Deleted(
                owner.getId(),
                owner.getName(),
                deletedTamagochisCount
        );
        send(RoutingKeys.OWNER_DELETED, event);
    }

    private void send(String routingKey, OwnerEvent event) {
        try {
            EventEnvelope<OwnerEvent> envelope = EventEnvelope.wrap(event, SOURCE, routingKey);
            rabbitTemplate.convertAndSend(RoutingKeys.EXCHANGE, routingKey, envelope);
            log.info("Событие отправлено: {} [eventId={}]", routingKey, envelope.metadata().eventId());
        } catch (Exception e) {
            log.error("Не удалось отправить событие {}: {}", routingKey, e.getMessage());
        }
    }
}
