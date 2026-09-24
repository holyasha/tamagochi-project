package com.example.tamagochi_api_contract.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

@Getter
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Relation(collectionRelation = "tamagochis", itemRelation = "tamagochi")
@Schema(description = "Информация о питомце")
public class TamagochiResponse extends RepresentationModel<TamagochiResponse> {

    @Schema(description = "Уникальный идентификатор питомца", example = "1")
    private final Long id;

    @Schema(description = "Имя питомца", example = "Чупеп")
    private final String name;

    @Schema(description = "Вид питомца", example = "Кошка")
    private final String species;

    @Schema(description = "Цвет питомца", example = "Серый")
    private final String color;

    @Schema(description = "Статус жизни питомца", example = "true")
    private final Boolean isAlive;

    @Schema(description = "Здоровье питомца", example = "100")
    @Min(0)
    @Max(100)
    private final Integer health;

    @Schema(description = "Голод питомца", example = "50")
    @Min(0)
    @Max(100)
    private final Integer hunger;

    @Schema(description = "Счастье питомца", example = "50")
    @Min(0)
    @Max(100)
    private final Integer happiness;

    @Schema(description = "Чистота питомца", example = "50")
    @Min(0)
    @Max(100)
    private final Integer clearliness;

    @Schema(description = "Энергия питомца", example = "50")
    @Min(0)
    @Max(100)
    private final Integer energy;

    @Schema(description = "Дата рождения питомца", example = "2026-03-15")
    private final LocalDate birthDate;

    @Schema(description = "Владелец питомца")
    private final OwnerResponse owner;

    @Schema(description = "Момент создания питомца")
    private final OffsetDateTime createdAt;

    @Schema(description = "Момент последнего обновления питомца")
    private final OffsetDateTime updatedAt;
}