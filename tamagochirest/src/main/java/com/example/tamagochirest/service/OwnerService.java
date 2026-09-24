package com.example.tamagochirest.service;

import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.tamagochi_api_contract.dto.OwnerRequest;
import com.example.tamagochi_api_contract.dto.OwnerResponse;
import com.example.tamagochi_api_contract.dto.PagedResponse;
import com.example.tamagochi_api_contract.dto.PatchOwnerRequest;
import com.example.tamagochi_api_contract.exeption.ResourceNotFoundException;
import com.example.tamagochirest.domain.OwnerEntity;
import com.example.tamagochirest.repository.OwnerRepository;
import com.example.tamagochirest.event.OwnerEventPublisher;

import java.util.List;
import java.util.UUID;

@Service
public class OwnerService {

    private final OwnerRepository ownerRepository;
    private final TamagochiService tamagotchiService;
    private final OwnerEventPublisher eventPublisher;

    public OwnerService(OwnerRepository ownerRepository,
                       @Lazy TamagochiService tamagotchiService,
                       OwnerEventPublisher eventPublisher) {
        this.ownerRepository = ownerRepository;
        this.tamagotchiService = tamagotchiService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public PagedResponse<OwnerResponse> findAll(int page, int size) {
        Page<OwnerEntity> pageResult = ownerRepository.findAll(
            PageRequest.of(page, size, Sort.by("id"))
        );
        List<OwnerResponse> content = pageResult.getContent().stream()
            .map(this::toResponse)
            .toList();
        return new PagedResponse<>(content, page, size,
            (int) pageResult.getTotalElements(),
            pageResult.getTotalPages(),
            pageResult.isLast());
    }

    @Transactional(readOnly = true)
    public OwnerResponse findById(UUID id) {
        OwnerEntity entity = ownerRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Owner", id));
        return toResponse(entity);
    }

    @Transactional
    public OwnerResponse create(OwnerRequest request) {
        OwnerEntity entity = new OwnerEntity(
            UUID.randomUUID(),
            request.name(),
            request.birthDate(),
            0
        );
        OwnerEntity saved = ownerRepository.save(entity);
        OwnerResponse response = toResponse(saved);
        eventPublisher.publishCreated(response);
        return response;
    }

    @Transactional
    public OwnerResponse update(UUID id, OwnerRequest request) {
        OwnerEntity existing = ownerRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Owner", id));
        existing.setName(request.name());
        existing.setBirthDate(request.birthDate());
        OwnerEntity saved = ownerRepository.save(existing);
        return toResponse(saved);
    }

    @Transactional
    public OwnerResponse patchOwner(UUID id, PatchOwnerRequest request) {
        OwnerEntity existing = ownerRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Owner", id));
        if (request.name() != null) {
            existing.setName(request.name());
        }
        if (request.birthDate() != null) {
            existing.setBirthDate(request.birthDate());
        }
        OwnerEntity saved = ownerRepository.save(existing);
        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {
        OwnerEntity owner = ownerRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Owner", id));
        int deletedTamagochisCount = tamagotchiService.deleteTamagochisByOwnerId(id);
        ownerRepository.delete(owner);
        eventPublisher.publishDeleted(toResponse(owner), deletedTamagochisCount);
    }

    private OwnerResponse toResponse(OwnerEntity entity) {
        return OwnerResponse.builder()
            .id(entity.getId())
            .name(entity.getName())
            .birthDate(entity.getBirthDate())
            .tamagochisCount(entity.getTamagochisCount())
            .build();
    }
}
