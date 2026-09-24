package com.example.tamagochirest.graphql.types;

import java.time.LocalDate;

/**
 * Входной тип для фильтрации книг.
 * Соответствует input TamagochiFilter в GraphQL-схеме.
 *
 * Все поля необязательны — клиент передаёт только нужные фильтры.
 */
public record TamagochiFilterGql(
    String ownerId,
    String species,
    String color,
    LocalDate birthDate,
    String nameSearch
) {}
