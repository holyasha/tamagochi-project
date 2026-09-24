package com.example.tamagochirest.graphql.types;

import java.time.LocalDate;

/**
 * Входной тип для создания питомца.
 * Соответствует input CreateTamagochiInput в GraphQL-схеме.
 */
public record CreateTamagochiInputGql(
    String name,
    String species,
    String color,
    String ownerId,
    LocalDate birthDate
) {
}
