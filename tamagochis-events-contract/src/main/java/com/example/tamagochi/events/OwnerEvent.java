package com.example.tamagochi.events;

/**
 * Семейство событий, связанных с владельцами тамагочи.
 *
 * Аналогично TamagochiEvent — sealed interface гарантирует полный перечень вариантов.
 * Десериализация по eventType, а не через Jackson-аннотации.
 */
public sealed interface OwnerEvent {

    /**
     * Владелец создан. Содержит основные атрибуты нового владельца.
     */
    record Created(
            Long ownerId,
            String name
    ) implements OwnerEvent {}

    /**
     * Владелец удалён. В системе удаление каскадное — вместе с тамагочи.
     */
    record Deleted(
            Long ownerId,
            String name,
            int deletedTamagochisCount
    ) implements OwnerEvent {}
}
