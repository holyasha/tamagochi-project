package com.example.tamagochirest.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import com.example.tamagochi_api_contract.dto.OwnerResponse;
import com.example.tamagochi_api_contract.dto.PagedResponse;
import com.example.tamagochi_api_contract.dto.PatchTamagochiRequest;
import com.example.tamagochi_api_contract.dto.TamagochiRequest;
import com.example.tamagochi_api_contract.dto.TamagochiResponse;
import com.example.tamagochi_api_contract.dto.UpdateTamagochiRequest;
import com.example.tamagochi_api_contract.exeption.ResourceNotFoundException;
import com.example.tamagochirest.storage.InMemoryStorage;
import com.example.tamagochirest.event.TamagochiEventPublisher;

@Component
public class TamagochiService {

    private final InMemoryStorage storage;
    private final OwnerService ownerService;
    private final TamagochiEventPublisher eventPublisher;

    public TamagochiService(InMemoryStorage storage,
                           OwnerService ownerService,
                           TamagochiEventPublisher eventPublisher) {
        this.storage = storage;
        this.ownerService = ownerService;
        this.eventPublisher = eventPublisher;
    }

    public TamagochiResponse findTamagochiById(Long id) {
        return Optional.ofNullable(storage.tamagochis.get(id))
                .orElseThrow(() -> new ResourceNotFoundException("Tamagochi", id));
    }

    public PagedResponse<TamagochiResponse> findAllTamagochis(Long ownerId,
                                              String species,
                                              String color,
                                              String nameSearch,
                                              LocalDate birthDate,
                                              int page,
                                              int size) {

        Stream<TamagochiResponse> stream = storage.tamagochis.values().stream()
            .sorted((p1, p2) -> p1.getId().compareTo(p2.getId()));
        if (ownerId != null) {
            stream = stream.filter(p ->
                p.getOwner() != null &&
                p.getOwner().getId().equals(ownerId)
            );
        }
        if (species != null && !species.isBlank()) {
            stream = stream.filter(p ->
                species.equalsIgnoreCase(p.getSpecies())
                );
        }
        if (color != null && !color.isBlank()) {
            stream = stream.filter(p ->
                color.equalsIgnoreCase(p.getColor())
                );
        }
        if (birthDate != null) {
            stream = stream.filter(p ->
                birthDate.equals(p.getBirthDate())
            );
        }
        if (nameSearch != null && !nameSearch.isBlank()) {
            String q = nameSearch.toLowerCase();
            stream = stream.filter(p ->
                p.getName() != null &&
                p.getName().toLowerCase().contains(q)
            );
        }
        List<TamagochiResponse> allTamagochis = stream.toList();
        int totalElements = allTamagochis.size();
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 1;
        int from = page * size;
        int to = Math.min(from + size, totalElements);
        List<TamagochiResponse> content = (from >= totalElements) ? List.of() : allTamagochis.subList(from, to);
        return new PagedResponse<>(content, page, size, totalElements, totalPages, page >= totalPages - 1);
    }

    public TamagochiResponse createTamagochi(TamagochiRequest request) {
        OwnerResponse owner = ownerService.findById(request.ownerId());

        long id = storage.tamagochiSequence.incrementAndGet();
        TamagochiResponse tamagochi = TamagochiResponse.builder()
            .id(id)
            .name(request.name())
            .species(request.species())
            .color(request.color())
            .owner(owner)
            .happiness(100)
            .health(100)
            .hunger(100)
            .energy(100)
            .clearliness(100)
            .isAlive(true)
            .birthDate(request.birthDate())
            .createdAt(OffsetDateTime.now())
            .build();
        storage.tamagochis.put(id, tamagochi);

        // Публикуем доменное событие ПОСЛЕ успешного сохранения.
        // Если RabbitMQ недоступен — тамагочи всё равно создан, событие просто потеряется.
        eventPublisher.publishCreated(tamagochi);

        return tamagochi;
    }

    public TamagochiResponse updaTamagochi(Long id, UpdateTamagochiRequest request) {
        TamagochiResponse existing = findTamagochiById(id);
        TamagochiResponse updated = TamagochiResponse.builder()
            .id(id)
            .name(request.name())
            .species(request.species())
            .color(request.color())
            .owner(existing.getOwner())
            .birthDate(request.birthDate())
            .happiness(existing.getHappiness())
            .health(existing.getHealth())
            .hunger(existing.getHunger())
            .energy(existing.getEnergy())
            .clearliness(existing.getClearliness())
            .isAlive(existing.getIsAlive())
            .createdAt(existing.getCreatedAt())
            .updatedAt(OffsetDateTime.now())
            .build();
        storage.tamagochis.put(id, updated);
        eventPublisher.publishUpdated(updated);
        return updated;
    }

    public TamagochiResponse patcTamagochi(Long id, PatchTamagochiRequest request) {
        TamagochiResponse existing = findTamagochiById(id);
        TamagochiResponse updated = TamagochiResponse.builder()
            .id(id)
            .name(request.name() != null ? request.name() : existing.getName())
            .species(request.species() != null ? request.species() : existing.getSpecies())
            .color(request.color() != null ? request.color() : existing.getColor())
            .owner(existing.getOwner())
            .birthDate(request.birthDate() != null ? request.birthDate() : existing.getBirthDate())
            .happiness(existing.getHappiness())
            .health(existing.getHealth())
            .hunger(existing.getHunger())
            .energy(existing.getEnergy())
            .clearliness(existing.getClearliness())
            .isAlive(existing.getIsAlive())
            .createdAt(existing.getCreatedAt())
            .updatedAt(OffsetDateTime.now())
            .build();
        storage.tamagochis.put(id, updated);
        eventPublisher.publishUpdated(updated);
        return updated;
    }

    public void deleteTamagochi(Long id) {
        TamagochiResponse tamagochi = findTamagochiById(id);
        storage.tamagochis.remove(id);
        eventPublisher.publishDeleted(id, tamagochi.getName(), "Удалён владельцем");
    }

    public void deleteTamagochisByOwnerId(Long ownerId) {
        List<Long> toDelete = storage.tamagochis.values().stream()
                .filter(b -> b.getOwner() != null && b.getOwner().getId().equals(ownerId))
                .map(TamagochiResponse::getId)
                .toList();
        toDelete.forEach(storage.tamagochis::remove);
    }
}