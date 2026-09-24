package com.example.tamagochirest.graphql.types;

import java.time.LocalDate;

/**
 * Входной тип для создания владельца.
 * Соответствует input CreateOwnerInput в GraphQL-схеме.
 */
public record CreateOwnerInputGql(
    String name,
    LocalDate birthDate
) {}
