package com.example.tamagochirest.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.tamagochi_api_contract.dto.OwnerResponse;
import com.example.tamagochi_api_contract.dto.PagedResponse;
import com.example.tamagochi_api_contract.dto.PatchTamagochiRequest;
import com.example.tamagochi_api_contract.dto.TamagochiRequest;
import com.example.tamagochi_api_contract.dto.TamagochiResponse;
import com.example.tamagochi_api_contract.dto.UpdateTamagochiRequest;
import com.example.tamagochi_api_contract.exeption.ResourceNotFoundException;
import com.example.tamagochirest.config.DiagnosticMode;
import com.example.tamagochirest.domain.OwnerEntity;
import com.example.tamagochirest.domain.TamagochiEntity;
import com.example.tamagochirest.repository.OwnerRepository;
import com.example.tamagochirest.repository.TamagochiRepository;
import com.example.tamagochirest.event.TamagochiEventPublisher;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Service
public class TamagochiService {

    private static final Logger log = LoggerFactory.getLogger(TamagochiService.class);

    private final TamagochiRepository tamagochiRepository;
    private final OwnerRepository ownerRepository;
    private final TamagochiEventPublisher eventPublisher;
    private final DiagnosticMode diagnosticMode;
    private final MeterRegistry registry;
    private final AtomicInteger active = new AtomicInteger(0);

    public TamagochiService(TamagochiRepository tamagochiRepository,
                           OwnerRepository ownerRepository,
                           TamagochiEventPublisher eventPublisher,
                           DiagnosticMode diagnosticMode,
                           MeterRegistry registry) {
        this.tamagochiRepository = tamagochiRepository;
        this.ownerRepository = ownerRepository;
        this.eventPublisher = eventPublisher;
        this.diagnosticMode = diagnosticMode;
        this.registry = registry;

        registry.counter("tamagochi.requests", "result", "success");
        registry.counter("tamagochi.requests", "result", "failure");
        Timer.builder("tamagochi.processing")
                .tag("result", "success")
                .serviceLevelObjectives(Duration.ofMillis(500), Duration.ofSeconds(1))
                .publishPercentileHistogram()
                .register(registry);
        Timer.builder("tamagochi.processing")
                .tag("result", "failure")
                .serviceLevelObjectives(Duration.ofMillis(500), Duration.ofSeconds(1))
                .publishPercentileHistogram()
                .register(registry);
        Gauge.builder("tamagochi.active", active, AtomicInteger::get)
                .register(registry);
    }

    @Transactional(readOnly = true)
    public TamagochiResponse findTamagochiById(UUID id) {
        active.incrementAndGet();
        Timer.Sample sample = Timer.start(registry);
        String result = "success";

        try {
            applyDiagnosticMode();

            TamagochiEntity entity = tamagochiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tamagochi", id));
            return toResponse(entity);
        } catch (Exception e) {
            result = "failure";
            log.atWarn()
                .addKeyValue("event", "tamagochi.fetch.failed")
                .addKeyValue("outcome", "failure")
                .addKeyValue("failureType", e.getClass().getSimpleName())
                .log("Tamagochi fetch failed");
            throw e;
        } finally {
            sample.stop(registry.timer("tamagochi.processing", "result", result));
            registry.counter("tamagochi.requests", "result", result).increment();
            active.decrementAndGet();
        }
    }

    private void applyDiagnosticMode() {
        DiagnosticMode.Mode mode = diagnosticMode.getCurrentMode();
        if (mode == DiagnosticMode.Mode.SLOW) {
            try {
                Thread.sleep(750);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else if (mode == DiagnosticMode.Mode.FAIL) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Diagnostic failure mode active");
        }
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