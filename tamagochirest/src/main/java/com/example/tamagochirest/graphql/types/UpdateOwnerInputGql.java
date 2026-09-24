package com.example.tamagochirest.graphql.types;

import java.time.LocalDate;
/**
 * Входной тип для обновления владельца.
 * Соответствует input UpdateOwnerInput в GraphQL-схеме.
 */
public record UpdateOwnerInputGql(
    String name,
    LocalDate birthDate
) {}
