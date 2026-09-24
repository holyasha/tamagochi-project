package com.example.tamagochi_api_contract.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;

/**
 * Центральная OpenAPI-конфигурация контракта Tamagochis API.
 *
 * Это не Spring-бин — здесь нет @Component или @Configuration.
 * Библиотека springdoc-openapi сканирует classpath и автоматически подхватывает
 * аннотацию @OpenAPIDefinition, когда контракт подключён как зависимость.
 * Сервис-реализатор не дублирует описание API — он просто добавляет jar и получает
 * готовый Swagger UI.
 */
@OpenAPIDefinition(
        info = @Info(
                title = "Tamagochis API",
                version = "1.0.0",
                description = """
                        REST API для управления питомцами.
                        """
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Local development")
        })
@SecurityScheme(
        name = TamagochisApiContractConfig.SECURITY_SCHEME_BEARER,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER,
        description = "JWT Bearer token. Пример: `Authorization: Bearer eyJhbGci...`"
)
public final class TamagochisApiContractConfig {

    /**
     * Имя схемы безопасности. Используется в аннотациях @SecurityRequirement на методах API.
     */
    public static final String SECURITY_SCHEME_BEARER = "bearerAuth";

    private TamagochisApiContractConfig() {
        // utility class
    }
}

