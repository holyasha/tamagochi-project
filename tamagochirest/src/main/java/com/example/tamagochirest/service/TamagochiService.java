package com.example.tamagochirest.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.tamagochi_api_contract.dto.OwnerResponse;
import com.example.tamagochi_api_contract.dto.PagedResponse;
import com.example.tamagochi_api_contract.dto.PatchTamagochiRequest;
import com.example.tamagochi_api_contract.dto.TamagochiRequest;
import com.example.tamagochi_api_contract.dto.TamagochiResponse;
import com.example.tamagochi_api_contract.dto.UpdateTamagochiRequest;
import com.example.tamagochi_api_contract.exeption.ResourceNotFoundException;
import com.example.tamagochirest.domain.OwnerEntity;
import com.example.tamagochirest.domain.TamagochiEntity;
import com.example.tamagochirest.repository.OwnerRepository;
import com.example.tamagochirest.repository.TamagochiRepository;
import com.example.tamagochirest.event.TamagochiEventPublisher;

@Service
public class TamagochiService {

    private final TamagochiRepository tamagochiRepository;
    private final OwnerRepository ownerRepository;
    private final TamagochiEventPublisher eventPublisher;

    public TamagochiService(TamagochiRepository tamagochiRepository,
                           OwnerRepository ownerRepository,
                           TamagochiEventPublisher eventPublisher) {
        this.tamagochiRepository = tamagochiRepository;
        this.ownerRepository = ownerRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public TamagochiResponse findTamagochiById(UUID id) {
        TamagochiEntity entity = tamagochiRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Tamagochi", id));
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public PagedResponse<TamagochiResponse> findAllTamagochis(UUID ownerId,
                                              String species,
                                              String color,
                                              String nameSearch,
                                              LocalDate birthDate,
                                              int page,
                                              int size) {
        // ponytail: simplified — full filtering via repository methods when query complexity grows
        Page<TamagochiEntity> pageResult = tamagochiRepository.findAll(
            PageRequest.of(page, size, Sort.by("id"))
        );

        List<TamagochiResponse> content = pageResult.getContent().stream()
            .filter(e -> ownerId == null || e.getOwner().getId().equals(ownerId))
            .filter(e -> species == null || species.isBlank() || species.equalsIgnoreCase(e.getSpecies()))
            .filter(e -> color == null || color.isBlank() || color.equalsIgnoreCase(e.getColor()))
            .filter(e -> birthDate == null || birthDate.equals(e.getBirthDate()))
            .filter(e -> nameSearch == null || nameSearch.isBlank() ||
                   e.getName().toLowerCase().contains(nameSearch.toLowerCase()))
            .map(this::toResponse)
            .toList();

        return new PagedResponse<>(content, page, size,
            (int) pageResult.getTotalElements(),
            pageResult.getTotalPages(),
            pageResult.isLast());
    }

    @Transactional
    public TamagochiResponse createTamagochi(TamagochiRequest request) {
        OwnerEntity owner = ownerRepository.findById(request.ownerId())
            .orElseThrow(() -> new ResourceNotFoundException("Owner", request.ownerId()));

        TamagochiEntity entity = new TamagochiEntity(
            UUID.randomUUID(),
            request.name(),
            request.species(),
            request.color(),
            true,
            100, 100, 100, 100, 100,
            request.birthDate(),
            owner,
            OffsetDateTime.now()
        );
        TamagochiEntity saved = tamagochiRepository.save(entity);
        TamagochiResponse response = toResponse(saved);
        eventPublisher.publishCreated(response);
        return response;
    }

    @Transactional
    public TamagochiResponse updaTamagochi(UUID id, UpdateTamagochiRequest request) {
        TamagochiEntity existing = tamagochiRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Tamagochi", id));
        existing.setName(request.name());
        existing.setSpecies(request.species());
        existing.setColor(request.color());
        existing.setBirthDate(request.birthDate());
        TamagochiEntity saved = tamagochiRepository.save(existing);
        TamagochiResponse response = toResponse(saved);
        eventPublisher.publishUpdated(response);
        return response;
    }

    @Transactional
    public TamagochiResponse patcTamagochi(UUID id, PatchTamagochiRequest request) {
        TamagochiEntity existing = tamagochiRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Tamagochi", id));
        if (request.name() != null) existing.setName(request.name());
        if (request.species() != null) existing.setSpecies(request.species());
        if (request.color() != null) existing.setColor(request.color());
        if (request.birthDate() != null) existing.setBirthDate(request.birthDate());
        TamagochiEntity saved = tamagochiRepository.save(existing);
        TamagochiResponse response = toResponse(saved);
        eventPublisher.publishUpdated(response);
        return response;
    }

    @Transactional
    public void deleteTamagochi(UUID id) {
        TamagochiEntity entity = tamagochiRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Tamagochi", id));
        tamagochiRepository.delete(entity);
        eventPublisher.publishDeleted(id, entity.getName(), "Удалён владельцем");
    }

    @Transactional
    public int deleteTamagochisByOwnerId(UUID ownerId) {
        List<TamagochiEntity> toDelete = tamagochiRepository.findByOwnerId(ownerId);
        tamagochiRepository.deleteAll(toDelete);
        return toDelete.size();
    }

    private TamagochiResponse toResponse(TamagochiEntity entity) {
        OwnerEntity ownerEntity = entity.getOwner();
        OwnerResponse ownerResponse = OwnerResponse.builder()
            .id(ownerEntity.getId())
            .name(ownerEntity.getName())
            .birthDate(ownerEntity.getBirthDate())
            .tamagochisCount(ownerEntity.getTamagochisCount())
            .build();

        return TamagochiResponse.builder()
            .id(entity.getId())
            .name(entity.getName())
            .species(entity.getSpecies())
            .color(entity.getColor())
            .isAlive(entity.getIsAlive())
            .health(entity.getHealth())
            .hunger(entity.getHunger())
            .happiness(entity.getHappiness())
            .energy(entity.getEnergy())
            .clearliness(entity.getClearliness())
            .birthDate(entity.getBirthDate())
            .owner(ownerResponse)
            .createdAt(entity.getCreatedAt())
            .build();
    }
}