package com.example.tamagochirest.assemblers;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import com.example.tamagochi_api_contract.dto.OwnerResponse;
import com.example.tamagochirest.controllers.OwnerContoller;
import com.example.tamagochirest.controllers.TamagochiController;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class OwnerModelAssembler implements RepresentationModelAssembler<OwnerResponse, EntityModel<OwnerResponse>> {
    
    @Override
    public EntityModel<OwnerResponse> toModel(OwnerResponse owner) {
        return EntityModel.of(owner,
                linkTo(methodOn(OwnerContoller.class).getOwnerById(owner.getId())).withSelfRel(),
                linkTo(methodOn(OwnerContoller.class).getAllOwners(0, 20)).withRel("collection"),
                linkTo(methodOn(TamagochiController.class).getAllTamagochis(owner.getId(), null, null, null, null, 0, 20)).withRel("tamagochis")
        );
    }
}
