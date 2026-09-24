package com.example.tamagochirest.repository;

import com.example.tamagochirest.domain.OwnerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OwnerRepository extends JpaRepository<OwnerEntity, UUID> {
}
