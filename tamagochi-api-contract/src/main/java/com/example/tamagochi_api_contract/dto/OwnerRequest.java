package com.example.tamagochi_api_contract.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Запрос на создание или полное обновление пользователя")
public record OwnerRequest(
    
    @Schema(description = "Имя пользователя", example = "Мегакрут", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Имя пользователя не может быть пустым")
    @Size(max = 100, message = "Имя не может превышать 100 символов")
    String name,

    @Schema(description = "Дата рождения пользователя", example = "2000-04-18", requiredMode = Schema.RequiredMode.REQUIRED)
    @Past(message = "Дата рождения должна быть в прошлом")
    LocalDate birthDate


) {}
