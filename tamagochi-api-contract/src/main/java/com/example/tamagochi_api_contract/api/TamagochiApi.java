package com.example.tamagochi_api_contract.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.time.LocalDate;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;

import com.example.tamagochi_api_contract.config.TamagochisApiContractConfig;
import com.example.tamagochi_api_contract.dto.PatchTamagochiRequest;
import com.example.tamagochi_api_contract.dto.TamagochiRequest;
import com.example.tamagochi_api_contract.dto.TamagochiResponse;
import com.example.tamagochi_api_contract.dto.UpdateTamagochiRequest;

/**
 * Контракт API для управления питомцами.
 * Реализующий контроллер в сервисе должен имплементировать этот интерфейс.
 */
@Tag(name = "Tamagochis", description = "Управление питамцами")
@RequestMapping(
        value = "/api/tamagochis",
        produces = MediaType.APPLICATION_JSON_VALUE
)
public interface TamagochiApi {

    @Operation(
            summary = "Получить питомца по ID",
            security = @SecurityRequirement(name = TamagochisApiContractConfig.SECURITY_SCHEME_BEARER)
    )
    @ApiResponse(responseCode = "200", description = "Питомец найден")
    @ApiResponse(responseCode = "404", description = "Питомец не найден",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{id}")
    EntityModel<TamagochiResponse> getTamagochiById(
            @Parameter(description = "ID питомца", required = true, example = "1") @PathVariable Long id
    );

    @Operation(
            summary = "Список питомцев",
            description = """
                    Возвращает постраничный список питомцев с HATEOAS-ссылками.
                    Поддерживает комбинирование фильтров: name, color, species
                    можно передавать одновременно.
                    """,
            security = @SecurityRequirement(name = TamagochisApiContractConfig.SECURITY_SCHEME_BEARER)
    )
    @ApiResponse(responseCode = "200", description = "Постраничный список питомцев")
    @GetMapping
    PagedModel<EntityModel<TamagochiResponse>> getAllTamagochis(
            @Parameter(description = "Фильтр по ID владельца") @RequestParam(required = false) Long ownerId,
            @Parameter(description = "Фильтр по цвету", example = "Серый") @RequestParam(required = false) String color,
            @Parameter(description = "Фильтр по виду питомца", example = "Кошка") @RequestParam(required = false) String species,
            @Parameter(description = "Поиск по имени (substring, case-insensitive)", example = "Чупеп") @RequestParam(required = false) String nameSearch,
            @Parameter(description = "Поиск по дате рождения питомца", example = "2026-10-1")@RequestParam(required = false) LocalDate birthDate,
            @Parameter(description = "Номер страницы (0..N)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы", example = "20") @RequestParam(defaultValue = "20") int size
    );

    @Operation(
            summary = "Создать питомца",
            security = @SecurityRequirement(name = TamagochisApiContractConfig.SECURITY_SCHEME_BEARER)
    )
    @ApiResponse(responseCode = "201", description = "Питомец создан. Location header содержит URI нового ресурса.")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Владелец с указанным ownerId не найден",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    ResponseEntity<EntityModel<TamagochiResponse>> createTamagochi(@Valid @RequestBody TamagochiRequest request);

    @Operation(
            summary = "Полное обновление питомца (PUT)",
            description = "Заменяет все поля питомца. Владельца изменить нельзя. "
                    + "Для обновления отдельных полей используйте PATCH.",
            security = @SecurityRequirement(name = TamagochisApiContractConfig.SECURITY_SCHEME_BEARER)
    )
    @ApiResponse(responseCode = "200", description = "Питомец обновлен")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Питомец не найдена",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    EntityModel<TamagochiResponse> updateTamagochi(
            @Parameter(description = "ID питомца", required = true, example = "1") @PathVariable Long id,
            @Valid @RequestBody UpdateTamagochiRequest request
    );

    @Operation(
            summary = "Частичное обновление питомца (PATCH)",
            description = """
                    Обновляет только переданные поля (семантика JSON Merge Patch, RFC 7396).
                    Непереданные поля остаются без изменений. Владельца питомца изменить нельзя.
                    """,
            security = @SecurityRequirement(name = TamagochisApiContractConfig.SECURITY_SCHEME_BEARER)
    )
    @ApiResponse(responseCode = "200", description = "Питомец обновлен")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Питомец не найден",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    EntityModel<TamagochiResponse> patchTamagochi(
            @Parameter(description = "ID питомца", required = true, example = "1") @PathVariable Long id,
            @Valid @RequestBody PatchTamagochiRequest request
    );

    @Operation(
            summary = "Удалить питомца",
            security = @SecurityRequirement(name = TamagochisApiContractConfig.SECURITY_SCHEME_BEARER)
    )
    @ApiResponse(responseCode = "204", description = "Питомец удален")
    @ApiResponse(responseCode = "404", description = "Питомец не найден",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteTamagochi(
            @Parameter(description = "ID питомца", required = true, example = "1") @PathVariable Long id
    );
}

  
