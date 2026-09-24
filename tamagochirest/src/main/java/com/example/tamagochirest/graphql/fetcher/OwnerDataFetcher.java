package com.example.tamagochirest.graphql.fetcher;

import com.example.tamagochi_api_contract.dto.OwnerRequest;
import com.example.tamagochi_api_contract.dto.OwnerResponse;
import com.example.tamagochi_api_contract.dto.PagedResponse;
import com.example.tamagochirest.graphql.types.CreateOwnerInputGql;
import com.example.tamagochirest.graphql.types.OwnerConnectionGql;
import com.example.tamagochirest.graphql.types.PageInfoGql;
import com.example.tamagochirest.service.OwnerService;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;
/**
 * DataFetcher для операций с владельцами.
 *
 * Обрабатывает корневые поля Query и Mutation, связанные с авторами.
 * Вложенные поля (Owner.tamagochis) обрабатываются в OwnerTamagochisDataFetcher.
 *
 * Принцип разделения: один DataFetcher — одна группа связанных операций.
 * Это делает код более читаемым и тестируемым.
 */
@DgsComponent
public class OwnerDataFetcher {
    private final OwnerService ownerService;

    public OwnerDataFetcher(OwnerService ownerService) {
        this.ownerService = ownerService;
    }

     /**
     * Получение владельца по идентификатору.
     * Соответствует полю Query.owner(id: ID!) в схеме.
     */
    @DgsQuery
    public OwnerResponse owner(@InputArgument String id) {
        return ownerService.findById(Long.parseLong(id));
    }

    /**
     * Список владельцев с пагинацией.
     * Соответствует полю Query.owners(page, size) в схеме.
     */
    @DgsQuery
    public OwnerConnectionGql owners(
            @InputArgument Integer page,
            @InputArgument Integer size) {

        int pageNum = page != null ? page : 0;
        int pageSize = size != null ? size : 20;

        PagedResponse<OwnerResponse> paged = ownerService.findAll(pageNum, pageSize);

        return new OwnerConnectionGql(
                paged.content(),
                new PageInfoGql(paged.pageNumber(), paged.pageSize(), paged.totalPages(), paged.last()),
                (int) paged.totalElements());
    }

    /**
     * Создание владельца.
     * Соответствует полю Mutation.createOwner(input) в схеме.
     */
    @DgsMutation
    public OwnerResponse createOwner(@InputArgument CreateOwnerInputGql input) {
        OwnerRequest request = new OwnerRequest(
                input.name(),
                input.birthDate()
        );
        return ownerService.create(request);
    }

}
