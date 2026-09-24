package com.example.tamagochirest.graphql.types;

import java.util.List;

import com.example.tamagochi_api_contract.dto.TamagochiResponse;
/**
 * Тип-обёртка для постраничного ответа со списком питомцев.
 * Соответствует типу TamagochiConnection в GraphQL-схеме.
 */
public record TamagochiConnectionGql(
    List<TamagochiResponse> content,
    PageInfoGql pageInfo,
    int totalElements
) {}
