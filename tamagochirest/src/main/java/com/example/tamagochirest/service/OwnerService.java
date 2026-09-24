package com.example.tamagochirest.service;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.example.tamagochi_api_contract.dto.OwnerRequest;
import com.example.tamagochi_api_contract.dto.OwnerResponse;
import com.example.tamagochi_api_contract.dto.PagedResponse;
import com.example.tamagochi_api_contract.dto.PatchOwnerRequest;
import com.example.tamagochi_api_contract.exeption.ResourceNotFoundException;
import com.example.tamagochirest.storage.InMemoryStorage;
import com.example.tamagochirest.event.OwnerEventPublisher;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class OwnerService {

    private final InMemoryStorage storage;
    private final TamagochiService tamagotchiService;
    private final OwnerEventPublisher eventPublisher;

    public OwnerService(InMemoryStorage storage,
                       @Lazy TamagochiService tamagotchiService,
                       OwnerEventPublisher eventPublisher) {
        this.storage = storage;
        this.tamagotchiService = tamagotchiService;
        this.eventPublisher = eventPublisher;
    }

    public PagedResponse<OwnerResponse> findAll(int page, int size) {
        List <OwnerResponse> all = storage.owners.values().stream()
                .sorted(Comparator.comparingLong(OwnerResponse::getId)).toList();
        int totalElements = all.size();
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 1;
        int from = page * size;
        int to = Math.min(from + size, totalElements);
        List<OwnerResponse> content = (from >= totalElements) ? List.of() : all.subList(from, to);
        return new PagedResponse<>(content, page, size, totalElements, totalPages, page >= totalPages - 1);
    }

    public OwnerResponse findById(Long id) {
        return Optional.ofNullable(storage.owners.get(id))
                .orElseThrow(() -> new ResourceNotFoundException("Owner", id));
    }

    public OwnerResponse create(OwnerRequest request) {
        long id = storage.ownerSequence.incrementAndGet();
        OwnerResponse owner = OwnerResponse.builder()
            .id(id)
            .name(request.name())
            .birthDate(request.birthDate())
            .tamagochisCount(0)
            .build();
        storage.owners.put(id, owner);
        eventPublisher.publishCreated(owner);
        return owner;
    }

    public OwnerResponse update(Long id, OwnerRequest request) {
        OwnerResponse existing = findById(id); // Проверяем, что владелец существует
        OwnerResponse updatedOwner = OwnerResponse.builder()
                .id(id)
                .name(request.name())
                .birthDate(request.birthDate())
                .tamagochisCount(existing.getTamagochisCount())
                .build();
        storage.owners.put(id, updatedOwner);
        return updatedOwner;
    }

    public OwnerResponse patchOwner(Long id, PatchOwnerRequest request) {
        OwnerResponse existing = findById(id);
        String newName = request.name() != null ? request.name() : existing.getName();
        OwnerResponse updated = OwnerResponse.builder()
                .id(id)
                .name(newName)
                .birthDate(request.birthDate() != null ? request.birthDate() : existing.getBirthDate())
                .tamagochisCount(existing.getTamagochisCount())
                .build();
        storage.owners.put(id, updated);
        return updated;
    }

    public void delete(Long id) {
        OwnerResponse owner = findById(id);
        int deletedTamagochisCount = (int) storage.tamagochis.values().stream()
                .filter(t -> t.getOwner() != null && t.getOwner().getId().equals(id))
                .count();
        tamagotchiService.deleteTamagochisByOwnerId(id);
        storage.owners.remove(id);
        eventPublisher.publishDeleted(owner, deletedTamagochisCount);
    }
}
