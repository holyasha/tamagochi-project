package com.example.tamagochirest.controllers;

import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import com.example.tamagochi_api_contract.api.TamagochiApi;
import com.example.tamagochi_api_contract.dto.PagedResponse;
import com.example.tamagochi_api_contract.dto.PatchTamagochiRequest;
import com.example.tamagochi_api_contract.dto.TamagochiRequest;
import com.example.tamagochi_api_contract.dto.TamagochiResponse;
import com.example.tamagochi_api_contract.dto.UpdateTamagochiRequest;
import com.example.tamagochirest.assemblers.TamagochiModelAssembler;
import com.example.tamagochirest.service.TamagochiService;

import jakarta.validation.Valid;

@RestController
public class TamagochiController implements TamagochiApi{
    
    private final TamagochiService tamagochiService;
    private final TamagochiModelAssembler tamagochiModelAssembler;
    private final PagedResourcesAssembler<TamagochiResponse> pagedResourcesAssembler;
    
    public TamagochiController(TamagochiService tamagochiService, TamagochiModelAssembler tamagochiModelAssembler,
            PagedResourcesAssembler<TamagochiResponse> pagedResourcesAssembler) {
        this.tamagochiService = tamagochiService;
        this.tamagochiModelAssembler = tamagochiModelAssembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Override
    public EntityModel<TamagochiResponse> getTamagochiById(Long id) {
        return tamagochiModelAssembler.toModel(tamagochiService.findTamagochiById(id));
    }

    @Override
    public PagedModel<EntityModel<TamagochiResponse>> getAllTamagochis(Long ownerId, String color, String species,
            String nameSearch, LocalDate birthDate, int page, int size) {
        PagedResponse<TamagochiResponse> paged = tamagochiService.findAllTamagochis(ownerId, species, color, nameSearch, birthDate, page, size);
        Page<TamagochiResponse> springPage = new PageImpl<>(
                paged.content(),
                PageRequest.of(paged.pageNumber(), paged.pageSize()),
                paged.totalElements()
        );
        return pagedResourcesAssembler.toModel(springPage, tamagochiModelAssembler);
    }

    @Override
    public ResponseEntity<EntityModel<TamagochiResponse>> createTamagochi(@Valid TamagochiRequest request) {
        TamagochiResponse created = tamagochiService.createTamagochi(request);
        EntityModel<TamagochiResponse> model = tamagochiModelAssembler.toModel(created);
        return ResponseEntity
                .created(model.getRequiredLink("self").toUri())
                .body(model);
    }

    @Override
    public EntityModel<TamagochiResponse> updateTamagochi(Long id, @Valid UpdateTamagochiRequest request) {
        return tamagochiModelAssembler.toModel(tamagochiService.updaTamagochi(id, request));
    }

    @Override
    public EntityModel<TamagochiResponse> patchTamagochi(Long id, @Valid PatchTamagochiRequest request) {
        return tamagochiModelAssembler.toModel(tamagochiService.patcTamagochi(id, request));
    }

    @Override
    public void deleteTamagochi(Long id) {
        tamagochiService.deleteTamagochi(id);        
    }

    
    
    
}
