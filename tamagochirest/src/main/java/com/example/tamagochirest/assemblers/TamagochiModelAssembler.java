package com.example.tamagochirest.assemblers;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import com.example.tamagochi_api_contract.dto.TamagochiResponse;
import com.example.tamagochirest.controllers.OwnerContoller;
import com.example.tamagochirest.controllers.TamagochiController;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class TamagochiModelAssembler implements RepresentationModelAssembler<TamagochiResponse, EntityModel<TamagochiResponse>>{
    
    @Override
    public EntityModel<TamagochiResponse> toModel(TamagochiResponse tamagochi) {
        EntityModel<TamagochiResponse> model = EntityModel.of(tamagochi,
            linkTo(methodOn(TamagochiController.class).getTamagochiById(tamagochi.getId())).withSelfRel(),
            linkTo(methodOn(TamagochiController.class).getAllTamagochis(null, null, null, null, null, 0, 20)).withRel("collection")
        );
        if(tamagochi.getOwner() != null) {
            model.add(linkTo(methodOn(OwnerContoller.class)
                    .getOwnerById(tamagochi.getOwner().getId())).withRel("owner"));
        }
        return model;
    }
}
