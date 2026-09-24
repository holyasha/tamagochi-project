package com.example.tamagochirest.graphql.fetcher;

import com.example.tamagochi_api_contract.dto.OwnerResponse;
import com.example.tamagochi_api_contract.dto.PagedResponse;
import com.example.tamagochi_api_contract.dto.TamagochiResponse;
import com.example.tamagochirest.graphql.types.PageInfoGql;
import com.example.tamagochirest.graphql.types.TamagochiConnectionGql;
import com.example.tamagochirest.service.TamagochiService;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;
import com.netflix.graphql.dgs.InputArgument;

/**
 * Вложенный резолвер для поля Owner.tamagochis.
 *
 * Срабатывает когда клиент запрашивает питомцев владельца:
 *
 *   query {
 *     owner(id: "1") {
 *       name
 *       tamagochis(page: 0, size: 5) {    ← этот резолвер
 *         content {
 *           name
 *         }
 *         totalElements
 *       }
 *     }
 *   }
 *
 * Демонстрирует работу с аргументами вложенного поля (page, size)
 * и доступ к родительскому объекту (Owner).
 */
@DgsComponent
public class OwnerTamagochisDataFetcher {
    private final TamagochiService tamagochiService;

    public OwnerTamagochisDataFetcher(TamagochiService tamagochiService) {
        this.tamagochiService = tamagochiService;
    }

    /**
     * Загружает питомцев указанного владельца с пагинацией.
     *
     * Аргументы (page, size) берутся из GraphQL-запроса через @InputArgument.
     * Родительский объект (Owner) берётся из DgsDataFetchingEnvironment.
     */
    @DgsData(parentType = "Owner", field = "tamagochis")
    public TamagochiConnectionGql tamagochis(
            DgsDataFetchingEnvironment dfe,
            @InputArgument Integer page,
            @InputArgument Integer size) {

        OwnerResponse owner = dfe.getSource();

        int pageNum = page != null ? page : 0;
        int pageSize = size != null ? size : 20;

        // Фильтруем питомцев по ID владельца — переиспользуем сервис
        PagedResponse<TamagochiResponse> paged = tamagochiService.findAllTamagochis(
                owner.getId(), null, null, null, null, pageNum, pageSize);

        return new TamagochiConnectionGql(
                paged.content(),
                new PageInfoGql(paged.pageNumber(), paged.pageSize(), paged.totalPages(), paged.last()),
                (int) paged.totalElements());
    }
}
