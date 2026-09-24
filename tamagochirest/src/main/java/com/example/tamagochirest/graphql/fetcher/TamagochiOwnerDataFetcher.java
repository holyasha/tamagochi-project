package com.example.tamagochirest.graphql.fetcher;

import com.example.tamagochi_api_contract.dto.OwnerResponse;
import com.example.tamagochi_api_contract.dto.TamagochiResponse;
import com.example.tamagochirest.service.OwnerService;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsData;
import com.netflix.graphql.dgs.DgsDataFetchingEnvironment;

/**
 * Вложенный резолвер для поля Tamagochi.owner.
 *
 * В GraphQL каждое поле может иметь свой резолвер. Когда клиент запрашивает
 * книгу вместе с автором:
 *
 *   query {
 *     tamagochi(id: "1") {
 *       name
 *       owner {       ← этот резолвер срабатывает
 *         name
 *       }
 *     }
 *   }
 *
 * DGS вызывает этот метод для каждого питомца, передавая родительский объект
 * через DgsDataFetchingEnvironment. Если клиент НЕ запросил поле owner,
 * этот резолвер вообще не вызывается — экономия ресурсов.
 *
 * Аннотация @DgsData(parentType, field) привязывает метод к конкретному полю
 * конкретного типа в GraphQL-схеме.
 */
@DgsComponent
public class TamagochiOwnerDataFetcher {
    private final OwnerService ownerService;

    public TamagochiOwnerDataFetcher(OwnerService ownerService) {
        this.ownerService = ownerService;
    }

    /**
     * Загружает владельца для заданного питомца.
     *
     * Родительский объект (Tamagochi) извлекается из DgsDataFetchingEnvironment.
     * В нашем in-memory хранилище владелец уже вложен в TamagochiResponse,
     * поэтому мы просто его возвращаем. В реальном проекте здесь был бы
     * вызов к базе данных или внешнему сервису.
     */
    @DgsData(parentType = "Tamagochi", field = "owner")
    public OwnerResponse owner(DgsDataFetchingEnvironment dfe) {
        TamagochiResponse tamagochi = dfe.getSource();

        // Если владелец уже вложен в TamagochiResponse, возвращаем его напрямую.
        // В реальном приложении здесь мог бы быть вызов ownerService.findById().
        if (tamagochi.getOwner() != null) {
            return tamagochi.getOwner();
        }

        // Запасной вариант — загрузить владельца отдельно (для демонстрации)
        return null;
    }
}
