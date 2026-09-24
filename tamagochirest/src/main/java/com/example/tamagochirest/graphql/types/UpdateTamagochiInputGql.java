package com.example.tamagochirest.graphql.types;

import java.time.LocalDate;

/**
 * Входной тип для обновления питомца.
 * Соответствует input UpdateTamagochiInput в GraphQL-схеме.
 * Владельца изменить нельзя.
 */
public record UpdateTamagochiInputGql(
    String name,
    String species,
    String color,
    LocalDate birthDate
) {}
