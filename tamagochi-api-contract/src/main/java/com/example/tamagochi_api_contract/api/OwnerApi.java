package com.example.tamagochi_api_contract.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.tamagochi_api_contract.config.TamagochisApiContractConfig;
import com.example.tamagochi_api_contract.dto.ErrorResponse;
import com.example.tamagochi_api_contract.dto.OwnerRequest;
import com.example.tamagochi_api_contract.dto.OwnerResponse;
import com.example.tamagochi_api_contract.dto.PatchOwnerRequest;
import com.example.tamagochi_api_contract.dto.TamagochiResponse;

/**
 * Контракт API для управления пользователями.
 * Реализующий контроллер в сервисе должен имплементировать этот интерфейс.
 */
@Tag(name = "Owners", description = "Управление владельцами питомцев")
@RequestMapping(
        value = "/api/owners",
        produces = MediaType.APPLICATION_JSON_VALUE
)
public interface OwnerApi {

    @Operation(
            summary = "Список владельцев",
            description = "Возвращает постраничный список владельцев с HATEOAS-ссылками. "
                    + "Ссылки prev/next позволяют клиенту навигировать по страницам без знания офсетов.",
            security = @SecurityRequirement(name = TamagochisApiContractConfig.SECURITY_SCHEME_BEARER)
    )
    @ApiResponse(responseCode = "200", description = "Список владельцев")
    @GetMapping
    PagedModel<EntityModel<OwnerResponse>> getAllOwners(
            @Parameter(description = "Номер страницы (0..N)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы", example = "20")
            @RequestParam(defaultValue = "20") int size
    );

    @Operation(
            summary = "Получить владельца по ID",
            security = @SecurityRequirement(name = TamagochisApiContractConfig.SECURITY_SCHEME_BEARER)
    )
    @ApiResponse(responseCode = "200", description = "Пользователь найден")
    @ApiResponse(responseCode = "404", description = "Пользователь не найден",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{id}")
    EntityModel<OwnerResponse> getOwnerById(
            @Parameter(description = "ID владельца", required = true, example = "1") @PathVariable Long id
    );

    @Operation(
            summary = "Создать владельца",
            security = @SecurityRequirement(name = TamagochisApiContractConfig.SECURITY_SCHEME_BEARER)
    )
    @ApiResponse(responseCode = "201", description = "Пользователь создан. Location header содержит URI нового ресурса.")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    ResponseEntity<EntityModel<OwnerResponse>> createOwner(@Valid @RequestBody OwnerRequest request);

    @Operation(
            summary = "Полное обновление владельца (PUT)",
            description = "Заменяет все поля владельца. Для обновления отдельных полей используйте PATCH.",
            security = @SecurityRequirement(name = TamagochisApiContractConfig.SECURITY_SCHEME_BEARER)
    )
    @ApiResponse(responseCode = "200", description = "Владелец обновлён")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Владелец не найден",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    EntityModel<OwnerResponse> updateOwner(
            @Parameter(description = "ID владельца", required = true, example = "1") @PathVariable Long id,
            @Valid @RequestBody OwnerRequest request
    );

    @Operation(
            summary = "Частичное обновление владельца (PATCH)",
            description = """
                    Обновляет только переданные поля (семантика JSON Merge Patch, RFC 7396).
                    Непереданные поля остаются без изменений.
                    """,
            security = @SecurityRequirement(name = TamagochisApiContractConfig.SECURITY_SCHEME_BEARER)
    )
    @ApiResponse(responseCode = "200", description = "Владелец обновлён")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Владелец не найден",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    EntityModel<OwnerResponse> patchOwner(
            @Parameter(description = "ID владельца", required = true, example = "1") @PathVariable Long id,
            @Valid @RequestBody PatchOwnerRequest request
    );

    @Operation(
            summary = "Удалить владельца",
            description = "Удаляет владельца и всех его питомцев(каскадное удаление).",
            security = @SecurityRequirement(name = TamagochisApiContractConfig.SECURITY_SCHEME_BEARER)
    )
    @ApiResponse(responseCode = "204", description = "Пользователь удалён")
    @ApiResponse(responseCode = "404", description = "Пользователь не найден",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteOwner(
            @Parameter(description = "ID владельца", required = true, example = "1") @PathVariable Long id
    );

    @Operation(
            summary = "Питомцы пользователя (суб-ресурс)",
            description = """
                    Возвращает постраничный список питомцев указанного пользователя.
                    Это суб-ресурс (концепция REST): /owners/{id}/tamagochis.
                    Эквивалентен GET /tamagochis?ownerId={id}, но точнее отражает иерархию.
                    """,
            security = @SecurityRequirement(name = TamagochisApiContractConfig.SECURITY_SCHEME_BEARER)
    )
    @ApiResponse(responseCode = "200", description = "Список питомцев пользователя")
    @ApiResponse(responseCode = "404", description = "Пользователь не найден",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{id}/tamagochis")
    PagedModel<EntityModel<TamagochiResponse>> getTamagochisByOwner(
            @Parameter(description = "ID владельца", required = true, example = "1") @PathVariable Long id,
            @Parameter(description = "Номер страницы (0..N)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы", example = "20") @RequestParam(defaultValue = "20") int size
    );
}

