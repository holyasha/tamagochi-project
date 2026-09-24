package com.example.tamagochirest.graphql.fetcher;

import java.time.LocalDate;

import com.example.tamagochi_api_contract.dto.PagedResponse;
import com.example.tamagochi_api_contract.dto.TamagochiRequest;
import com.example.tamagochi_api_contract.dto.TamagochiResponse;
import com.example.tamagochi_api_contract.dto.UpdateTamagochiRequest;
import com.example.tamagochirest.graphql.types.CreateTamagochiInputGql;
import com.example.tamagochirest.graphql.types.PageInfoGql;
import com.example.tamagochirest.graphql.types.TamagochiConnectionGql;
import com.example.tamagochirest.graphql.types.TamagochiFilterGql;
import com.example.tamagochirest.graphql.types.UpdateTamagochiInputGql;
import com.example.tamagochirest.service.TamagochiService;
import com.netflix.graphql.dgs.DgsComponent;
import com.netflix.graphql.dgs.DgsMutation;
import com.netflix.graphql.dgs.DgsQuery;
import com.netflix.graphql.dgs.InputArgument;

/**
 * DataFetcher для операций с питомцами.
 *
 * Аннотация @DgsComponent регистрирует этот класс как компонент DGS-фреймворка.
 * Каждый метод с @DgsQuery или @DgsMutation привязывается к соответствующему полю
 * в GraphQL-схеме. DGS находит их по имени метода (или по явному параметру field).
 *
 * Этот DataFetcher обрабатывает корневые поля Query и Mutation для питомцев.
 * Вложенные поля (Tamagochi.owner) обрабатываются в отдельном резолвере.
 */
@DgsComponent
public class TamagochiDataFetcher {
    
    private final TamagochiService tamagochiService;

    public TamagochiDataFetcher(TamagochiService tamagochiService) {
        this.tamagochiService = tamagochiService;
    }

    /**
     * Получение питомца по идентификатору.
     * Соответствует полю Query.tamagochi(id: ID!) в схеме.
     * Возвращает null если питомец не найден (вместо исключения, как принято в GraphQL).
     */
    @DgsQuery
    public TamagochiResponse tamagochi(@InputArgument String id) {
        return tamagochiService.findTamagochiById(Long.parseLong(id));
    }

    /**
     * Список питомцев с фильтрацией и пагинацией.
     * Соответствует полю Query.tamagochis(filter, page, size) в схеме.
     *
     * @InputArgument автоматически маппит GraphQL-аргументы на Java-параметры.
     * Для сложных типов (input TamagochiFilter) DGS сам десериализует JSON в объект.
     */
    @DgsQuery
    public TamagochiConnectionGql tamagochis(
            @InputArgument TamagochiFilterGql filter,
            @InputArgument Integer page,
            @InputArgument Integer size) {

        // Подставляем значения по умолчанию, если клиент не передал аргументы
        int pageNum = page != null ? page : 0;
        int pageSize = size != null ? size : 20;

        // Извлекаем параметры фильтрации
        Long ownerId = null;
        String species = null;
        String color = null;
        String nameSearch = null;
        LocalDate birthDate = null;

        if (filter != null) {
            ownerId = filter.ownerId() != null ? Long.parseLong(filter.ownerId()) : null;
            species = filter.species();
            color = filter.color();
            birthDate = filter.birthDate();
            nameSearch = filter.nameSearch();
        }

        // Переиспользуем существующий сервисный слой — GraphQL не дублирует бизнес-логику
        PagedResponse<TamagochiResponse> paged = tamagochiService.findAllTamagochis(
                ownerId, species, color, nameSearch, birthDate, pageNum, pageSize);

        return new TamagochiConnectionGql(
                paged.content(),
                new PageInfoGql(paged.pageNumber(), paged.pageSize(), paged.totalPages(), paged.last()),
                (int) paged.totalElements());
    }

    /**
     * Создание питомца.
     * Соответствует полю Mutation.createTamagochi(input: CreateTamagochiInput!) в схеме.
     */
    @DgsMutation
    public TamagochiResponse createTamagochi(@InputArgument CreateTamagochiInputGql input) {
        TamagochiRequest request = new TamagochiRequest(
                input.name(),
                input.species(),
                input.color(),
                Long.parseLong(input.ownerId()),
                input.birthDate()
        );
        return tamagochiService.createTamagochi(request);
    }

    /**
     * Обновление питомца.
     * Соответствует полю Mutation.updateTamagochi(id, input) в схеме.
     */
    @DgsMutation
    public TamagochiResponse updateTamagochi(@InputArgument String id, @InputArgument UpdateTamagochiInputGql input) {
        UpdateTamagochiRequest request = new UpdateTamagochiRequest(
                input.name(),
                input.species(),
                input.color(),
                input.birthDate()
        );
        return tamagochiService.updaTamagochi(Long.parseLong(id), request);
    }

    /**
     * Удаление питомца.
     * Соответствует полю Mutation.deleteTamagochi(id) в схеме.
     * Возвращает true при успешном удалении.
     */
    @DgsMutation
    public boolean deleteTamagochi(@InputArgument String id) {
        tamagochiService.deleteTamagochi(Long.parseLong(id));
        return true;
    }
}
