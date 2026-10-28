# Лабораторная работа 06 - Distributed Tracing с OpenTelemetry + Tempo + Grafana

## Запущенные сервисы

- **Backend (tamagochirest)**: http://localhost:8080
- **Gateway (BFF)**: http://localhost:8088
- **Keycloak**: http://localhost:8085
- **Redis**: localhost:6379
- **PostgreSQL**: localhost:5432
- **Tempo**: http://localhost:3200
- **OTEL Collector**: http://localhost:4318
- **Grafana**: http://localhost:3000

## Что сделано

### 1. Инфраструктура для трейсинга
- Добавлены сервисы в `compose.yaml`:
  - **Tempo** (Grafana Tempo 2.8.2) — хранилище трейсов
  - **OpenTelemetry Collector** (0.132.0) — приём и обработка трейсов
  - **Grafana** (12.1.0) — визуализация трейсов

### 2. Конфигурация
- `infra/tempo/tempo.yaml` — конфигурация Tempo для приёма OTLP через gRPC
- `infra/otel/otel-collector.yaml` — OTLP HTTP receiver → Tempo exporter
- `infra/grafana/provisioning/datasources/tempo.yaml` — datasource для Grafana

### 3. Tracing в tamagochirest
Добавлены зависимости в `tamagochirest/pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>
```

Конфигурация в `application.properties`:
```properties
management.tracing.propagation.type=w3c
management.tracing.sampling.probability=1.0
management.opentelemetry.resource-attributes.deployment.environment.name=local
management.opentelemetry.tracing.export.otlp.endpoint=http://localhost:4318/v1/traces
management.otlp.metrics.export.enabled=false
management.observations.enable.spring.security=false

logging.pattern.correlation=[${spring.application.name:},trace_id=%X{traceId:-},span_id=%X{spanId:-}]
```

### 4. Tracing в gateway-service
Те же зависимости в `gateway-service/pom.xml` + конфигурация в `application.yml`:
```yaml
management:
  tracing:
    propagation:
      type: w3c
    sampling:
      probability: 1.0
  opentelemetry:
    resource-attributes:
      deployment.environment.name: local
    tracing:
      export:
        otlp:
          endpoint: http://localhost:4318/v1/traces
```

## Проверка трейсинга

### 1. Войти в систему через браузер
1. Открой http://localhost:8088
2. Авторизуйся через Keycloak (alice/alice или bob/bob)
3. Сделай несколько запросов к `/api/tamagochis` и `/api/owners`

### 2. Проверить трейсы в Grafana
1. Открой Grafana: http://localhost:3000
2. Логин/пароль: `admin` / `admin-dev-only`
3. Перейди в **Explore** (иконка компаса слева)
4. Выбери datasource **Tempo**
5. В **Query type** выбери **Search**
6. Фильтры:
   - Service Name: `tamagochirest` или `gateway-service`
   - Duration: например, > 0ms
7. Нажми **Run query**

Трейс покажет:
- Span от gateway-service (прокси запроса)
- Span от tamagochirest (обработка в backend)
- trace_id связывает оба span'а
- Время выполнения каждого компонента

### 3. Проверить логи с correlation ID
```bash
tail -f tamagochirest.log | grep trace_id
```

После авторизованного запроса через gateway увидишь:
```
[tamagochirest,trace_id=<hex>,span_id=<hex>] ...
```

### 4. Проверить API Tempo напрямую
```bash
# Список трейсов
curl -s http://localhost:3200/api/search

# Получить конкретный трейс
curl -s http://localhost:3200/api/traces/<trace_id>
```

## Архитектура трейсинга

```
Browser → Gateway (OAuth2 Client + Token Relay)
           ↓ W3C Trace Context (traceparent header)
          tamagochirest (OAuth2 Resource Server)
           ↓ spans export via OTLP HTTP
          OpenTelemetry Collector
           ↓ OTLP gRPC
          Tempo (storage)
           ↓ query
          Grafana (UI)
```

## Что показывает трейс

- **trace_id** — уникальный ID всего запроса (сквозной через все сервисы)
- **span_id** — ID конкретной операции в рамках трейса
- **parent_span_id** — связь между вызовами (gateway → backend)
- **duration** — время выполнения операции
- **attributes** — метаданные (http.method, http.status_code, service.name и т.д.)

## Troubleshooting

### Трейсы не появляются в Tempo
1. Проверь, что OTLP Collector доступен:
   ```bash
   curl http://localhost:4318/v1/traces
   # Должен вернуть 405 (Method Not Allowed) — endpoint живой
   ```

2. Проверь логи OTEL Collector:
   ```bash
   docker logs tamagochi-otel-collector
   ```

3. Проверь логи Tempo:
   ```bash
   docker logs tamagochi-tempo
   ```

### trace_id пустой в логах
Значит Spring Boot не экспортирует трейсы. Проверь:
- Зависимости `micrometer-tracing-bridge-otel` и `opentelemetry-exporter-otlp` в pom.xml
- Конфигурация `management.opentelemetry.tracing.export.otlp.endpoint`

### Grafana не показывает Tempo datasource
1. Открой Grafana → Configuration → Data Sources
2. Убедись, что Tempo datasource настроен на http://tempo:3200
3. Нажми "Test" — должен показать успех

## Завершение работы

```bash
# Остановить Java процессы
ps aux | grep spring-boot | awk '{print $2}' | xargs kill

# Остановить Docker контейнеры
docker compose down
```
