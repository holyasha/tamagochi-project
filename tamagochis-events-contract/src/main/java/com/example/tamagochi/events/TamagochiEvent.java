package com.example.tamagochi.events;

import java.time.LocalDate;

/**
 * Семейство событий, связанных с тамагочи.
 *
 * Sealed interface (Java 17+) ограничивает набор наследников — компилятор гарантирует,
 * что TamagochiEvent может быть ТОЛЬКО одним из перечисленных типов.
 * Это делает switch/pattern matching исчерпывающим (exhaustive).
 *
 * Полиморфная десериализация выполняется не через Jackson-аннотации,
 * а через поле eventType в EventMetadata — consumer определяет конкретный тип
 * по routing key и десериализует payload в нужный record напрямую.
 */
public sealed interface TamagochiEvent {

    /**
     * Тамагочи создан. Содержит все ключевые атрибуты нового питомца.
     */
    record Created(
            Long tamagochiId,
            String name,
            String species,
            String color,
            LocalDate birthDate,
            Long ownerId,
            String ownerName
    ) implements TamagochiEvent {}

    /**
     * Тамагочи обновлён. Содержит актуальное состояние после обновления.
     */
    record Updated(
            Long tamagochiId,
            String name,
            String species,
            String color,
            Integer health,
            Integer hunger,
            Integer happiness,
            Integer cleanliness,
            Integer energy,
            Boolean isAlive
    ) implements TamagochiEvent {}

    /**
     * Тамагочи удалён (умер или удалён владельцем).
     */
    record Deleted(
            Long tamagochiId,
            String name,
            String reason
    ) implements TamagochiEvent {}

    /**
     * Тамагочи покормлен. Событие фиксирует действие кормления.
     */
    record Fed(
            Long tamagochiId,
            String name,
            Integer hungerBefore,
            Integer hungerAfter,
            Integer healthAfter
    ) implements TamagochiEvent {}

    /**
     * С тамагочи поиграли. Событие фиксирует игровую активность.
     */
    record Played(
            Long tamagochiId,
            String name,
            Integer happinessBefore,
            Integer happinessAfter,
            Integer energyAfter
    ) implements TamagochiEvent {}

    /**
     * Тамагочи вылечен. Событие фиксирует лечение питомца.
     */
    record Healed(
            Long tamagochiId,
            String name,
            Integer healthBefore,
            Integer healthAfter
    ) implements TamagochiEvent {}

    /**
     * Тамагочи искупан/очищен.
     */
    record Cleaned(
            Long tamagochiId,
            String name,
            Integer cleanlinessBefore,
            Integer cleanlinessAfter
    ) implements TamagochiEvent {}

    /**
     * Тамагочи спит/отдыхает.
     */
    record Rested(
            Long tamagochiId,
            String name,
            Integer energyBefore,
            Integer energyAfter
    ) implements TamagochiEvent {}

    /**
     * Тамагочи обогащён аналитикой — результат gRPC-вызова к analytics-серверу.
     *
     * Событие публикуется grpc-enrichment-client после получения ответа
     * от grpc-analytics-server. Содержит вычисленные метрики тамагочи.
     */
    record Enriched(
            Long tamagochiId,
            String name,
            String overallCondition,
            Integer wellbeingScore,
            String careLevel,
            Integer daysAlive,
            String lifestageClassification
    ) implements TamagochiEvent {}
}
