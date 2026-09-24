package com.example.tamagochi.grpcenrichment.listener;

import com.example.tamagochi.events.TamagochiEvent;
import com.example.tamagochi.events.EventMetadata;
import com.example.tamagochi.grpc.AnalyzeTamagochiRequest;
import com.example.tamagochi.grpc.TamagochiAnalysisResponse;
import com.example.tamagochi.grpc.TamagochiAnalyticsGrpc;
import com.example.tamagochi.grpcenrichment.publisher.EnrichmentEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Слушатель событий tamagochi.created из RabbitMQ.
 *
 * Десериализация — ручная (как в audit-service), потому что EventEnvelope<T>
 * является generic-типом, и Jackson не может определить конкретный подтип T.
 */
@Component
public class TamagochiCreatedListener {

    private static final Logger log = LoggerFactory.getLogger(TamagochiCreatedListener.class);

    private final TamagochiAnalyticsGrpc.TamagochiAnalyticsBlockingStub analyticsStub;
    private final EnrichmentEventPublisher enrichmentPublisher;
    private final JsonMapper jsonMapper;

    public TamagochiCreatedListener(TamagochiAnalyticsGrpc.TamagochiAnalyticsBlockingStub analyticsStub,
                                    EnrichmentEventPublisher enrichmentPublisher,
                                    JsonMapper jsonMapper) {
        this.analyticsStub = analyticsStub;
        this.enrichmentPublisher = enrichmentPublisher;
        this.jsonMapper = jsonMapper;
    }

    /**
     * Обрабатывает событие tamagochi.created:
     * 1. Десериализует событие из JSON
     * 2. Формирует gRPC-запрос
     * 3. Вызывает gRPC-сервер (синхронно)
     * 4. Публикует результат как событие tamagochi.enriched
     */
    @RabbitListener(queues = "q.enrichment.tamagochi-created", messageConverter = "")
    public void handleTamagochiCreated(Message message) {
        try {
            // 1. Парсим JSON-конверт
            byte[] body = message.getBody();
            JsonNode root = jsonMapper.readTree(body);

            JsonNode metaNode = root.get("metadata");
            EventMetadata metadata = jsonMapper.treeToValue(metaNode, EventMetadata.class);

            JsonNode payloadNode = root.get("payload");
            TamagochiEvent.Created tamagochiCreated = jsonMapper.treeToValue(payloadNode, TamagochiEvent.Created.class);

            log.info("Получено событие tamagochi.created: tamagochiId={}, «{}» [eventId={}]",
                    tamagochiCreated.tamagochiId(), tamagochiCreated.name(), metadata.eventId());

            // Вычисляем количество дней жизни
            int daysAlive = (int) ChronoUnit.DAYS.between(tamagochiCreated.birthDate(), LocalDate.now());

            // 2. Формируем gRPC-запрос
            AnalyzeTamagochiRequest grpcRequest = AnalyzeTamagochiRequest.newBuilder()
                    .setTamagochiId(tamagochiCreated.tamagochiId())
                    .setName(tamagochiCreated.name() != null ? tamagochiCreated.name() : "")
                    .setSpecies(tamagochiCreated.species() != null ? tamagochiCreated.species() : "")
                    .setHealth(100)        // Новый тамагочи начинает со 100% здоровья
                    .setHunger(50)         // Средний голод
                    .setHappiness(80)      // Счастливый новорожденный
                    .setCleanliness(100)   // Чистый
                    .setEnergy(100)        // Полная энергия
                    .setDaysAlive(daysAlive)
                    .setIsAlive(true)
                    .build();

            // 3. Вызываем gRPC-сервер (синхронно)
            log.info("Вызов gRPC: TamagochiAnalytics.AnalyzeTamagochi(tamagochiId={})", tamagochiCreated.tamagochiId());
            TamagochiAnalysisResponse grpcResponse = analyticsStub.analyzeTamagochi(grpcRequest);

            log.info("gRPC ответ получен: tamagochiId={}, состояние={}, благополучие={}, уход={}, стадия={}",
                    grpcResponse.getTamagochiId(),
                    grpcResponse.getOverallCondition(),
                    grpcResponse.getWellbeingScore(),
                    grpcResponse.getCareLevel(),
                    grpcResponse.getLifestageClassification());

            // 4. Публикуем событие tamagochi.enriched
            TamagochiEvent.Enriched enrichedEvent = new TamagochiEvent.Enriched(
                    grpcResponse.getTamagochiId(),
                    tamagochiCreated.name(),
                    grpcResponse.getOverallCondition(),
                    grpcResponse.getWellbeingScore(),
                    grpcResponse.getCareLevel(),
                    grpcResponse.getDaysAlive(),
                    grpcResponse.getLifestageClassification()
            );

            enrichmentPublisher.publishEnriched(enrichedEvent);

            log.info("Тамагочи обогащён: tamagochiId={}, «{}» → tamagochi.enriched отправлено",
                    tamagochiCreated.tamagochiId(), tamagochiCreated.name());

        } catch (io.grpc.StatusRuntimeException e) {
            log.error("gRPC ошибка при обогащении тамагочи: {} ({})",
                    e.getStatus().getDescription(), e.getStatus().getCode());
            throw new RuntimeException("gRPC-вызов завершился ошибкой", e);

        } catch (Exception e) {
            log.error("Ошибка обработки события tamagochi.created: {}", e.getMessage(), e);
            throw new RuntimeException("Не удалось обработать событие tamagochi.created", e);
        }
    }
}
