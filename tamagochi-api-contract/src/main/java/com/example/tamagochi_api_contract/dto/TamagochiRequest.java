package com.example.tamagochi_api_contract.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Запрос на создание или полное обновление питомца")
public record TamagochiRequest(
    @Schema(description = "Имя питомца", example = "Чупеп", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Имя питомца не может быть пустым")
    @Size(max = 100, message = "Имя не может превышать 100 символов")
    String name,

    @Schema(description = "Вид питомца", example = "Кошка", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Вид питомца не может быть пустым")
    @Pattern(regexp = "Кошка|Собака|Робот|Пришелец|Дракон", message = "Вид должен быть одним из: Кошка, Собака, Робот, Пришелец, Дракон")
    String species,

    @Schema(description = "Цвет питомца", example = "Серый", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Цвет питомца не может быть пустым")
    @Size(max = 100, message = "Цвет не может превышать 100 символов")
    String color,

    @Schema(description = "ID владельца", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "ID владельца не может быть пустым")
    Long ownerId,

    @Schema(description = "Дата рождения питомца", example = "2026-04-18", requiredMode = Schema.RequiredMode.REQUIRED)
    @Past(message = "Дата рождения должна быть в прошлом")
    LocalDate birthDate
) {}
