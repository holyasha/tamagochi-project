package com.example.tamagochirest.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import com.example.tamagochi_api_contract.api.OwnerApi;
import com.example.tamagochi_api_contract.dto.OwnerRequest;
import com.example.tamagochi_api_contract.dto.OwnerResponse;
import com.example.tamagochi_api_contract.dto.PagedResponse;
import com.example.tamagochi_api_contract.dto.PatchOwnerRequest;
import com.example.tamagochi_api_contract.dto.TamagochiResponse;
import com.example.tamagochirest.assemblers.OwnerModelAssembler;
import com.example.tamagochirest.assemblers.TamagochiModelAssembler;
import com.example.tamagochirest.service.OwnerService;
import com.example.tamagochirest.service.TamagochiService;

import java.util.UUID;

@RestController
public class OwnerContoller implements OwnerApi {

    private final OwnerService ownerService;
    private final TamagochiService tamagotchiService;
    private final OwnerModelAssembler ownerModelAssembler;
    private final TamagochiModelAssembler tamagochiModelAssembler;
    private final PagedResourcesAssembler<OwnerResponse> pagedOwnersAssembler;
    private final PagedResourcesAssembler<TamagochiResponse> pagedTamagochisAssembler;
    public OwnerContoller(OwnerService ownerService, TamagochiService tamagotchiService,
            OwnerModelAssembler ownerModelAssembler, TamagochiModelAssembler tamagochiModelAssembler,
            PagedResourcesAssembler<OwnerResponse> pagedOwnersAssembler,
            PagedResourcesAssembler<TamagochiResponse> pagedTamagochisAssembler) {
        this.ownerService = ownerService;
        this.tamagotchiService = tamagotchiService;
        this.ownerModelAssembler = ownerModelAssembler;
        this.tamagochiModelAssembler = tamagochiModelAssembler;
        this.pagedOwnersAssembler = pagedOwnersAssembler;
        this.pagedTamagochisAssembler = pagedTamagochisAssembler;
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public PagedModel<EntityModel<OwnerResponse>> getAllOwners(int page, int size) {
        PagedResponse<OwnerResponse> paged = ownerService.findAll(page, size);
        Page<OwnerResponse> springPage = new PageImpl<>(
            paged.content(),
            PageRequest.of(paged.pageNumber(), paged.pageSize()),
            paged.totalElements()
        );
        return pagedOwnersAssembler.toModel(springPage, ownerModelAssembler);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public EntityModel<OwnerResponse> getOwnerById(UUID id) {
        return ownerModelAssembler.toModel(ownerService.findById(id));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EntityModel<OwnerResponse>> createOwner(OwnerRequest request) {
        OwnerResponse created = ownerService.create(request);
        EntityModel<OwnerResponse> model = ownerModelAssembler.toModel(created);
        return ResponseEntity
                .created(model.getRequiredLink("self").toUri())
                .body(model);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public EntityModel<OwnerResponse> updateOwner(UUID id, OwnerRequest request) {
        return ownerModelAssembler.toModel(ownerService.update(id, request));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public EntityModel<OwnerResponse> patchOwner(UUID id, PatchOwnerRequest request) {
        return ownerModelAssembler.toModel(ownerService.patchOwner(id, request));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteOwner(UUID id) {
        ownerService.delete(id);
    }

    @Override
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public PagedModel<EntityModel<TamagochiResponse>> getTamagochisByOwner(UUID id, int page, int size) {
        ownerService.findById(id);
        PagedResponse<TamagochiResponse> paged = tamagotchiService.findAllTamagochis(id, null, null, null, null, page, size);
        Page<TamagochiResponse> springPage = new PageImpl<>(
            paged.content(),
            PageRequest.of(paged.pageNumber(), paged.pageSize()),
            paged.totalElements()
        );
        return pagedTamagochisAssembler.toModel(springPage, tamagochiModelAssembler);
    }



}
