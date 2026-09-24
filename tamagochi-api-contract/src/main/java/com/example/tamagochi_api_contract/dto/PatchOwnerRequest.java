package com.example.tamagochi_api_contract.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

@Schema(description = "Частичное обновление владельца (PATCH). Передайте только те поля, которые нужно изменить. "
        + "Непереданные поля остаются без изменений.")
public record PatchOwnerRequest(

    @Schema(description = "Новое имя владельца", example = "Мегакрут2",requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 100, message = "Имя не может превышать 100 символов")
    String name,

    @Schema(description = "Новая дата рождения владельца", example = "1828-09-09")
    @Past(message = "Дата рождения должна быть в прошлом")
    LocalDate birthDate


) {}

