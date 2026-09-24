package com.example.tamagochi.grpcanalytics.service;

import com.example.tamagochi.grpc.AnalyzeTamagochiRequest;
import com.example.tamagochi.grpc.TamagochiAnalysisResponse;
import com.example.tamagochi.grpc.TamagochiAnalyticsGrpc;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Реализация gRPC-сервиса TamagochiAnalytics.
 *
 * Наследует сгенерированный базовый класс TamagochiAnalyticsImplBase —
 * аналог того, как REST-контроллер реализует интерфейс контракта:
 *
 *   REST:    TamagochiController
 *   GraphQL: TamagochiDataFetcher с @DgsQuery
 *   gRPC:    TamagochiAnalyticsServiceImpl extends TamagochiAnalyticsGrpc.TamagochiAnalyticsImplBase
 *
 * Ключевые отличия от REST/GraphQL:
 * - Бинарный протокол (protobuf) вместо JSON — компактнее и быстрее
 * - Строго типизированный контракт (.proto) — несовместимость обнаруживается при компиляции
 * - HTTP/2 с мультиплексированием — несколько запросов в одном TCP-соединении
 * - Поддержка streaming (server, client, bidirectional) — здесь используем unary (простой запрос-ответ)
 */
public class TamagochiAnalyticsServiceImpl extends TamagochiAnalyticsGrpc.TamagochiAnalyticsImplBase {

    private static final Logger log = LoggerFactory.getLogger(TamagochiAnalyticsServiceImpl.class);

    /**
     * Обрабатывает запрос на анализ тамагочи.
     *
     * Паттерн gRPC: метод получает request и StreamObserver для ответа.
     * StreamObserver — это callback-интерфейс:
     *   - onNext(response) — отправить ответ (для unary RPC вызывается один раз)
     *   - onCompleted()    — завершить RPC
     *   - onError(t)       — сообщить об ошибке
     *
     * Для unary RPC (один запрос → один ответ) всегда:
     *   responseObserver.onNext(response);
     *   responseObserver.onCompleted();
     */
    @Override
    public void analyzeTamagochi(AnalyzeTamagochiRequest request,
                                  StreamObserver<TamagochiAnalysisResponse> responseObserver) {

        log.info("gRPC запрос: анализ тамагочи id={} «{}» (вид: {}, здоровье: {}, голод: {}, счастье: {})",
                request.getTamagochiId(), request.getName(),
                request.getSpecies(), request.getHealth(), request.getHunger(), request.getHappiness());

        // ─── Вычисление метрик (демонстрационная логика) ─────────────
        String condition = assessOverallCondition(
                request.getHealth(), request.getHunger(),
                request.getHappiness(), request.getCleanliness(), request.getEnergy()
        );
        int wellbeingScore = calculateWellbeingScore(
                request.getHealth(), request.getHunger(),
                request.getHappiness(), request.getCleanliness(), request.getEnergy()
        );
        String careLevel = assessCareLevel(wellbeingScore, request.getDaysAlive());
        String lifestage = classifyLifestage(request.getDaysAlive());

        // ─── Формируем ответ ─────────────────────────────────────────
        TamagochiAnalysisResponse response = TamagochiAnalysisResponse.newBuilder()
                .setTamagochiId(request.getTamagochiId())
                .setOverallCondition(condition)
                .setWellbeingScore(wellbeingScore)
                .setCareLevel(careLevel)
                .setDaysAlive(request.getDaysAlive())
                .setLifestageClassification(lifestage)
                .build();

        log.info("gRPC ответ: тамагочи id={}, состояние={}, благополучие={}, уход={}, стадия={}",
                response.getTamagochiId(), condition, wellbeingScore, careLevel, lifestage);

        // Отправляем ответ клиенту и завершаем RPC
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    // ─── Демонстрационная бизнес-логика ──────────────────────────────

    /**
     * Оценка общего состояния тамагочи на основе всех показателей.
     */
    private String assessOverallCondition(int health, int hunger, int happiness, int cleanliness, int energy) {
        int avgScore = (health + (100 - hunger) + happiness + cleanliness + energy) / 5;

        if (avgScore >= 80) return "EXCELLENT";
        if (avgScore >= 60) return "GOOD";
        if (avgScore >= 40) return "FAIR";
        if (avgScore >= 20) return "POOR";
        return "CRITICAL";
    }

    /**
     * Рассчитывает единый балл благополучия (0-100).
     */
    private int calculateWellbeingScore(int health, int hunger, int happiness, int cleanliness, int energy) {
        // Здоровье важнее всего
        double score = health * 0.3 +
                       (100 - hunger) * 0.2 +
                       happiness * 0.25 +
                       cleanliness * 0.15 +
                       energy * 0.1;
        return (int) Math.round(score);
    }

    /**
     * Оценка уровня ухода на основе благополучия и продолжительности жизни.
     */
    private String assessCareLevel(int wellbeingScore, int daysAlive) {
        // Если тамагочи прожил долго и в хорошем состоянии — отличный уход
        if (wellbeingScore >= 70 && daysAlive >= 30) return "EXCELLENT";
        if (wellbeingScore >= 60 && daysAlive >= 14) return "GOOD";
        if (wellbeingScore >= 40) return "BASIC";
        return "NEGLECTED";
    }

    /**
     * Классификация стадии жизни по возрасту.
     */
    private String classifyLifestage(int daysAlive) {
        if (daysAlive < 7) return "BABY";
        if (daysAlive < 14) return "CHILD";
        if (daysAlive < 30) return "TEEN";
        if (daysAlive < 90) return "ADULT";
        return "ELDER";
    }
}
