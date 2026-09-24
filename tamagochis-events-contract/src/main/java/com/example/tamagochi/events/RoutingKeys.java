package com.example.tamagochi.events;

/**
 * Константы для маршрутизации событий в RabbitMQ.
 *
 * Routing key в topic exchange работает как почтовый индекс:
 * - "tamagochi.created" — конкретное событие
 * - "tamagochi.*"       — все события тамагочи
 * - "#"                 — все события вообще
 *
 * Вынесены в контракт, чтобы publisher и consumer использовали одни и те же строки.
 * Рассогласование routing key — частая ошибка, которую трудно отследить.
 */
public final class RoutingKeys {

    private RoutingKeys() {
        // утилитарный класс — экземпляры не создаём
    }

    // Имя общего topic exchange для доменных событий
    public static final String EXCHANGE = "tamagochis.events";

    // Routing keys для событий тамагочи
    public static final String TAMAGOCHI_CREATED = "tamagochi.created";
    public static final String TAMAGOCHI_UPDATED = "tamagochi.updated";
    public static final String TAMAGOCHI_DELETED = "tamagochi.deleted";
    public static final String TAMAGOCHI_FED = "tamagochi.fed";
    public static final String TAMAGOCHI_PLAYED = "tamagochi.played";
    public static final String TAMAGOCHI_HEALED = "tamagochi.healed";
    public static final String TAMAGOCHI_CLEANED = "tamagochi.cleaned";
    public static final String TAMAGOCHI_RESTED = "tamagochi.rested";
    public static final String TAMAGOCHI_ENRICHED = "tamagochi.enriched";

    // Routing keys для событий владельцев
    public static final String OWNER_CREATED = "owner.created";
    public static final String OWNER_DELETED = "owner.deleted";

    // Паттерны для подписки (wildcard)
    public static final String ALL_TAMAGOCHI_EVENTS = "tamagochi.*";
    public static final String ALL_OWNER_EVENTS = "owner.*";
    public static final String ALL_EVENTS = "#";
}
