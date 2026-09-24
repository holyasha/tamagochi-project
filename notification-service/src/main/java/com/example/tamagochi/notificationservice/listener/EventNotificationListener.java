package com.example.tamagochi.notificationservice.listener;

import com.example.tamagochi.events.*;
import com.example.tamagochi.notificationservice.websocket.NotificationWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Слушатель всех доменных событий из RabbitMQ.
 *
 * Получает события из очереди q.notifications.all (binding "#"),
 * формирует человекочитаемое JSON-уведомление и рассылает
 * всем подключённым WebSocket-клиентам через NotificationWebSocketHandler.
 *
 * Дедупликация — по eventId (на случай повторной доставки RabbitMQ).
 */
@Component
public class EventNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(EventNotificationListener.class);

    private final NotificationWebSocketHandler webSocketHandler;
    private final JsonMapper jsonMapper;

    /** Набор обработанных eventId для дедупликации. */
    private final Set<String> processedEventIds = ConcurrentHashMap.newKeySet();

    public EventNotificationListener(NotificationWebSocketHandler webSocketHandler,
                                     JsonMapper jsonMapper) {
        this.webSocketHandler = webSocketHandler;
        this.jsonMapper = jsonMapper;
    }

    @RabbitListener(queues = "q.notifications.all")
    public void handleEvent(Message message) {
        try {
            byte[] body = message.getBody();
            JsonNode root = jsonMapper.readTree(body);

            // Парсим метаданные
            JsonNode metaNode = root.get("metadata");
            EventMetadata metadata = jsonMapper.treeToValue(metaNode, EventMetadata.class);

            // Дедупликация по eventId
            if (!processedEventIds.add(metadata.eventId())) {
                log.warn("Дубликат уведомления пропущен: eventId={}", metadata.eventId());
                return;
            }

            // Формируем уведомление
            JsonNode payloadNode = root.get("payload");
            String title = buildTitle(metadata.eventType());
            String description = buildDescription(metadata.eventType(), payloadNode);
            String icon = resolveIcon(metadata.eventType());
            String level = resolveLevel(metadata.eventType());

            // JSON для WebSocket-клиента
            String notificationJson = jsonMapper.writeValueAsString(
                    new NotificationPayload(
                            "NOTIFICATION",
                            metadata.eventId(),
                            metadata.eventType(),
                            title,
                            description,
                            icon,
                            level,
                            metadata.source(),
                            metadata.timestamp().toString(),
                            Instant.now().toString()
                    )
            );

            // Broadcast в WebSocket
            webSocketHandler.broadcast(notificationJson);

            log.info("[NOTIFY] {} | {} (клиентов: {})",
                    metadata.eventType(), description, webSocketHandler.getActiveConnectionCount());

        } catch (Exception e) {
            log.error("Ошибка обработки события для уведомлений: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось обработать событие", e);
        }
    }

    // Формирование заголовка уведомления

    private String buildTitle(String eventType) {
        return switch (eventType) {
            case "tamagochi.created"  -> "Новый тамагочи";
            case "tamagochi.updated"  -> "Тамагочи обновлён";
            case "tamagochi.deleted"  -> "Тамагочи удалён";
            case "tamagochi.fed"      -> "Тамагочи покормлен";
            case "tamagochi.played"   -> "Игра с тамагочи";
            case "tamagochi.healed"   -> "Тамагочи вылечен";
            case "tamagochi.cleaned"  -> "Тамагочи очищен";
            case "tamagochi.rested"   -> "Тамагочи отдохнул";
            case "tamagochi.enriched" -> "Аналитика тамагочи";
            case "owner.created"      -> "Новый владелец";
            case "owner.deleted"      -> "Владелец удалён";
            default                   -> "Событие: " + eventType;
        };
    }

    // Формирование описания

    private String buildDescription(String eventType, JsonNode payload) {
        try {
            return switch (eventType) {
                case "tamagochi.created" -> {
                    TamagochiEvent.Created e = jsonMapper.treeToValue(payload, TamagochiEvent.Created.class);
                    yield "Создан тамагочи «%s» (вид: %s, цвет: %s), владелец: %s".formatted(
                            e.name(), e.species(), e.color(), e.ownerName());
                }
                case "tamagochi.updated" -> {
                    TamagochiEvent.Updated e = jsonMapper.treeToValue(payload, TamagochiEvent.Updated.class);
                    yield "Обновлён тамагочи «%s» (здоровье: %d, голод: %d, счастье: %d)".formatted(
                            e.name(), e.health(), e.hunger(), e.happiness());
                }
                case "tamagochi.deleted" -> {
                    TamagochiEvent.Deleted e = jsonMapper.treeToValue(payload, TamagochiEvent.Deleted.class);
                    yield "Удалён тамагочи «%s» (причина: %s)".formatted(e.name(), e.reason());
                }
                case "tamagochi.fed" -> {
                    TamagochiEvent.Fed e = jsonMapper.treeToValue(payload, TamagochiEvent.Fed.class);
                    yield "Покормлен «%s» (голод: %d → %d)".formatted(
                            e.name(), e.hungerBefore(), e.hungerAfter());
                }
                case "tamagochi.played" -> {
                    TamagochiEvent.Played e = jsonMapper.treeToValue(payload, TamagochiEvent.Played.class);
                    yield "С «%s» поиграли (счастье: %d → %d)".formatted(
                            e.name(), e.happinessBefore(), e.happinessAfter());
                }
                case "tamagochi.healed" -> {
                    TamagochiEvent.Healed e = jsonMapper.treeToValue(payload, TamagochiEvent.Healed.class);
                    yield "Вылечен «%s» (здоровье: %d → %d)".formatted(
                            e.name(), e.healthBefore(), e.healthAfter());
                }
                case "tamagochi.cleaned" -> {
                    TamagochiEvent.Cleaned e = jsonMapper.treeToValue(payload, TamagochiEvent.Cleaned.class);
                    yield "Очищен «%s» (чистота: %d → %d)".formatted(
                            e.name(), e.cleanlinessBefore(), e.cleanlinessAfter());
                }
                case "tamagochi.rested" -> {
                    TamagochiEvent.Rested e = jsonMapper.treeToValue(payload, TamagochiEvent.Rested.class);
                    yield "«%s» отдохнул (энергия: %d → %d)".formatted(
                            e.name(), e.energyBefore(), e.energyAfter());
                }
                case "tamagochi.enriched" -> {
                    TamagochiEvent.Enriched e = jsonMapper.treeToValue(payload, TamagochiEvent.Enriched.class);
                    yield "Тамагочи «%s» — состояние: %s, благополучие: %d, уход: %s, стадия: %s".formatted(
                            e.name(), e.overallCondition(), e.wellbeingScore(),
                            e.careLevel(), e.lifestageClassification());
                }
                case "owner.created" -> {
                    OwnerEvent.Created e = jsonMapper.treeToValue(payload, OwnerEvent.Created.class);
                    yield "Создан владелец «%s»".formatted(e.name());
                }
                case "owner.deleted" -> {
                    OwnerEvent.Deleted e = jsonMapper.treeToValue(payload, OwnerEvent.Deleted.class);
                    yield "Удалён владелец «%s» (удалено тамагочи: %d)".formatted(
                            e.name(), e.deletedTamagochisCount());
                }
                default -> "Неизвестное событие: " + eventType;
            };
        } catch (Exception e) {
            return "Событие " + eventType + " (ошибка парсинга)";
        }
    }

    // Иконка по типу события

    private String resolveIcon(String eventType) {
        return switch (eventType) {
            case "tamagochi.created"  -> "pet-plus";
            case "tamagochi.updated"  -> "pet-edit";
            case "tamagochi.deleted"  -> "pet-remove";
            case "tamagochi.fed"      -> "food";
            case "tamagochi.played"   -> "game";
            case "tamagochi.healed"   -> "medical";
            case "tamagochi.cleaned"  -> "clean";
            case "tamagochi.rested"   -> "sleep";
            case "tamagochi.enriched" -> "analytics";
            case "owner.created"      -> "user-plus";
            case "owner.deleted"      -> "user-remove";
            default                   -> "bell";
        };
    }

    // Уровень уведомления

    private String resolveLevel(String eventType) {
        return switch (eventType) {
            case "tamagochi.deleted", "owner.deleted" -> "warning";
            case "tamagochi.enriched"                 -> "info";
            case "tamagochi.healed"                   -> "success";
            default                                   -> "default";
        };
    }

    /**
     * Payload уведомления для WebSocket.
     */
    record NotificationPayload(
            String type,
            String eventId,
            String eventType,
            String title,
            String description,
            String icon,
            String level,
            String source,
            String eventTimestamp,
            String receivedAt
    ) {}
}
