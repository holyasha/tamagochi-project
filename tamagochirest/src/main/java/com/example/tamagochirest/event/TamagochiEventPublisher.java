package com.example.tamagochirest.event;

import com.example.tamagochi_api_contract.dto.TamagochiResponse;
import com.example.tamagochi.events.TamagochiEvent;
import com.example.tamagochi.events.EventEnvelope;
import com.example.tamagochi.events.RoutingKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Публикация доменных событий тамагочи в RabbitMQ.
 *
 * Паттерн: TamagochiService вызывает publish-метод ПОСЛЕ успешного завершения
 * бизнес-операции. Если RabbitMQ недоступен — событие логируется как ошибка,
 * но основная операция (создание/удаление тамагочи) НЕ откатывается.
 *
 * Это паттерн «fire-and-forget» — допустимая потеря события лучше,
 * чем отказ бизнес-операции из-за недоступности брокера.
 */
@Component
public class TamagochiEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(TamagochiEventPublisher.class);
    private static final String SOURCE = "tamagochirest";

    private final RabbitTemplate rabbitTemplate;

    public TamagochiEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Публикует событие «тамагочи создан».
     */
    public void publishCreated(TamagochiResponse tamagochi) {
        var event = new TamagochiEvent.Created(
                tamagochi.getId(),
                tamagochi.getName(),
                tamagochi.getSpecies(),
                tamagochi.getColor(),
                tamagochi.getBirthDate(),
                tamagochi.getOwner() != null ? tamagochi.getOwner().getId() : null,
                tamagochi.getOwner() != null ? tamagochi.getOwner().getName() : "Неизвестен"
        );
        send(RoutingKeys.TAMAGOCHI_CREATED, event);
    }

    /**
     * Публикует событие «тамагочи обновлён».
     */
    public void publishUpdated(TamagochiResponse tamagochi) {
        var event = new TamagochiEvent.Updated(
                tamagochi.getId(),
                tamagochi.getName(),
                tamagochi.getSpecies(),
                tamagochi.getColor(),
                tamagochi.getHealth(),
                tamagochi.getHunger(),
                tamagochi.getHappiness(),
                tamagochi.getClearliness(),
                tamagochi.getEnergy(),
                tamagochi.getIsAlive()
        );
        send(RoutingKeys.TAMAGOCHI_UPDATED, event);
    }

    /**
     * Публикует событие «тамагочи удалён».
     */
    public void publishDeleted(Long tamagochiId, String name, String reason) {
        var event = new TamagochiEvent.Deleted(tamagochiId, name, reason);
        send(RoutingKeys.TAMAGOCHI_DELETED, event);
    }

    /**
     * Публикует событие «тамагочи покормлен».
     */
    public void publishFed(Long tamagochiId, String name, int hungerBefore, int hungerAfter, int healthAfter) {
        var event = new TamagochiEvent.Fed(tamagochiId, name, hungerBefore, hungerAfter, healthAfter);
        send(RoutingKeys.TAMAGOCHI_FED, event);
    }

    /**
     * Публикует событие «с тамагочи поиграли».
     */
    public void publishPlayed(Long tamagochiId, String name, int happinessBefore, int happinessAfter, int energyAfter) {
        var event = new TamagochiEvent.Played(tamagochiId, name, happinessBefore, happinessAfter, energyAfter);
        send(RoutingKeys.TAMAGOCHI_PLAYED, event);
    }

    /**
     * Публикует событие «тамагочи вылечен».
     */
    public void publishHealed(Long tamagochiId, String name, int healthBefore, int healthAfter) {
        var event = new TamagochiEvent.Healed(tamagochiId, name, healthBefore, healthAfter);
        send(RoutingKeys.TAMAGOCHI_HEALED, event);
    }

    /**
     * Публикует событие «тамагочи очищен».
     */
    public void publishCleaned(Long tamagochiId, String name, int cleanlinessBefore, int cleanlinessAfter) {
        var event = new TamagochiEvent.Cleaned(tamagochiId, name, cleanlinessBefore, cleanlinessAfter);
        send(RoutingKeys.TAMAGOCHI_CLEANED, event);
    }

    /**
     * Публикует событие «тамагочи отдохнул».
     */
    public void publishRested(Long tamagochiId, String name, int energyBefore, int energyAfter) {
        var event = new TamagochiEvent.Rested(tamagochiId, name, energyBefore, energyAfter);
        send(RoutingKeys.TAMAGOCHI_RESTED, event);
    }

    /**
     * Отправляет событие в RabbitMQ, обёрнутое в EventEnvelope.
     */
    private void send(String routingKey, TamagochiEvent event) {
        try {
            EventEnvelope<TamagochiEvent> envelope = EventEnvelope.wrap(event, SOURCE, routingKey);
            rabbitTemplate.convertAndSend(RoutingKeys.EXCHANGE, routingKey, envelope);
            log.info("Событие отправлено: {} [eventId={}]", routingKey, envelope.metadata().eventId());
        } catch (Exception e) {
            log.error("Не удалось отправить событие {}: {}", routingKey, e.getMessage());
        }
    }
}
