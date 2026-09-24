package com.example.tamagochi.auditservice.listener;

import com.example.tamagochi.auditservice.model.AuditEntry;
import com.example.tamagochi.auditservice.storage.AuditStorage;
import com.example.tamagochi.events.OwnerEvent;
import com.example.tamagochi.events.TamagochiEvent;
import com.example.tamagochi.events.EventMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;

/**
 * Единый слушатель всех доменных событий из RabbitMQ.
 *
 * Принимает «сырое» AMQP-сообщение (Message) и десериализует его вручную.
 * Это необходимо, потому что EventEnvelope<T> — generic тип, и Jackson
 * не может определить конкретный подтип T при автоматической десериализации.
 *
 * Промышленная альтернатива:
 * - отдельные очереди для разных типов событий (не generic listener),
 * - Spring Cloud Stream с content-type routing,
 * - Apache Avro/Protobuf с Schema Registry.
 */
@Component
public class AuditEventListener {

    private static final Logger log = LoggerFactory.getLogger(AuditEventListener.class);

    private final AuditStorage auditStorage;
    private final JsonMapper jsonMapper;

    public AuditEventListener(AuditStorage auditStorage, JsonMapper jsonMapper) {
        this.auditStorage = auditStorage;
        this.jsonMapper = jsonMapper;
    }

    /**
     * Принимает все события из очереди q.audit.events.
     *
     * Десериализация выполняется в два этапа:
     * 1. Парсим JSON в дерево узлов (JsonNode) — быстро и безопасно.
     * 2. Извлекаем metadata и определяем тип payload по полю eventType.
     * 3. Десериализуем payload в конкретный record по выявленному типу.
     */
    @RabbitListener(queues = "q.audit.events", messageConverter = "")
    public void handleEvent(Message message) {
        try {
            byte[] body = message.getBody();
            JsonNode root = jsonMapper.readTree(body);

            // Извлекаем метаданные из JSON-конверта
            JsonNode metaNode = root.get("metadata");
            EventMetadata metadata = jsonMapper.treeToValue(metaNode, EventMetadata.class);

            // Дедупликация — если событие уже обработано, пропускаем
            if (auditStorage.isDuplicate(metadata.eventId())) {
                log.warn("Дубликат события пропущен: eventId={}", metadata.eventId());
                return;
            }

            // Определяем тип события и формируем описание
            JsonNode payloadNode = root.get("payload");
            String description = buildDescription(metadata.eventType(), payloadNode);

            AuditEntry entry = auditStorage.save(new AuditEntry(
                    0,
                    metadata.eventId(),
                    metadata.eventType(),
                    metadata.source(),
                    metadata.timestamp(),
                    Instant.now(),
                    description
            ));

            log.info("[AUDIT #{}] {} | {}", entry.sequenceNumber(), metadata.eventType(), description);

        } catch (Exception e) {
            log.error("Ошибка обработки события: {}", e.getMessage(), e);
            // Исключение пробросится, сообщение уйдёт в DLQ после исчерпания retries
            throw new RuntimeException("Не удалось обработать событие", e);
        }
    }

    /**
     * Формирует человекочитаемое описание события для аудит-лога.
     *
     * Десериализует payload в конкретный тип на основе eventType,
     * затем формирует описание через pattern matching по sealed interface.
     */
    private String buildDescription(String eventType, JsonNode payloadNode) throws Exception {
        return switch (eventType) {
            case "tamagochi.created" -> {
                TamagochiEvent.Created e = jsonMapper.treeToValue(payloadNode, TamagochiEvent.Created.class);
                yield String.format("Создан тамагочи «%s» (вид: %s, цвет: %s), владелец: %s",
                        e.name(), e.species(), e.color(), e.ownerName());
            }
            case "tamagochi.updated" -> {
                TamagochiEvent.Updated e = jsonMapper.treeToValue(payloadNode, TamagochiEvent.Updated.class);
                yield String.format("Обновлён тамагочи id=%d «%s» (здоровье: %d, голод: %d, счастье: %d)",
                        e.tamagochiId(), e.name(), e.health(), e.hunger(), e.happiness());
            }
            case "tamagochi.deleted" -> {
                TamagochiEvent.Deleted e = jsonMapper.treeToValue(payloadNode, TamagochiEvent.Deleted.class);
                yield String.format("Удалён тамагочи id=%d «%s» (причина: %s)",
                        e.tamagochiId(), e.name(), e.reason());
            }
            case "tamagochi.fed" -> {
                TamagochiEvent.Fed e = jsonMapper.treeToValue(payloadNode, TamagochiEvent.Fed.class);
                yield String.format("Покормлен тамагочи «%s» (голод: %d → %d)",
                        e.name(), e.hungerBefore(), e.hungerAfter());
            }
            case "tamagochi.played" -> {
                TamagochiEvent.Played e = jsonMapper.treeToValue(payloadNode, TamagochiEvent.Played.class);
                yield String.format("С тамагочи «%s» поиграли (счастье: %d → %d)",
                        e.name(), e.happinessBefore(), e.happinessAfter());
            }
            case "tamagochi.healed" -> {
                TamagochiEvent.Healed e = jsonMapper.treeToValue(payloadNode, TamagochiEvent.Healed.class);
                yield String.format("Вылечен тамагочи «%s» (здоровье: %d → %d)",
                        e.name(), e.healthBefore(), e.healthAfter());
            }
            case "tamagochi.cleaned" -> {
                TamagochiEvent.Cleaned e = jsonMapper.treeToValue(payloadNode, TamagochiEvent.Cleaned.class);
                yield String.format("Очищен тамагочи «%s» (чистота: %d → %d)",
                        e.name(), e.cleanlinessBefore(), e.cleanlinessAfter());
            }
            case "tamagochi.rested" -> {
                TamagochiEvent.Rested e = jsonMapper.treeToValue(payloadNode, TamagochiEvent.Rested.class);
                yield String.format("Тамагочи «%s» отдохнул (энергия: %d → %d)",
                        e.name(), e.energyBefore(), e.energyAfter());
            }
            case "tamagochi.enriched" -> {
                TamagochiEvent.Enriched e = jsonMapper.treeToValue(payloadNode, TamagochiEvent.Enriched.class);
                yield String.format("Тамагочи обогащён id=%d «%s» (состояние: %s, благополучие: %d, уход: %s, дней жизни: %d, стадия: %s)",
                        e.tamagochiId(), e.name(), e.overallCondition(),
                        e.wellbeingScore(), e.careLevel(), e.daysAlive(), e.lifestageClassification());
            }
            case "owner.deleted" -> {
                OwnerEvent.Deleted e = jsonMapper.treeToValue(payloadNode, OwnerEvent.Deleted.class);
                yield String.format("Удалён владелец «%s» (удалено тамагочи: %d)",
                        e.name(), e.deletedTamagochisCount());
            }
            default -> "Неизвестное событие: " + eventType;
        };
    }
}
