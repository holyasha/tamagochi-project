package com.example.tamagochirest.repository;

import com.example.tamagochirest.domain.TamagochiEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TamagochiRepository extends JpaRepository<TamagochiEntity, UUID> {
    List<TamagochiEntity> findByOwnerId(UUID ownerId);
}
