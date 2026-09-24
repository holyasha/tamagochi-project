package com.example.tamagochirest.graphql.types;

import java.util.List;

import com.example.tamagochi_api_contract.dto.OwnerResponse;

/**
 * Тип-обёртка для постраничного ответа со списком владельцев.
 * Соответствует типу OwnerConnection в GraphQL-схеме.
 */
public record OwnerConnectionGql(
    List<OwnerResponse> content,
    PageInfoGql pageinfo,
    int totalElements
) {}
