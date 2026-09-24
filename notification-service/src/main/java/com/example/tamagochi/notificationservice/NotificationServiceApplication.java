package com.example.tamagochi.notificationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Notification Service — сервис уведомлений для тамагочи.
 *
 * Слушает все события из RabbitMQ и рассылает их через WebSocket
 * подключённым клиентам в реальном времени.
 *
 * Запуск: mvnw spring-boot:run -pl notification-service
 * WebSocket: ws://localhost:8084/ws/notifications
 * Web UI: http://localhost:8084/
 */
@SpringBootApplication
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
