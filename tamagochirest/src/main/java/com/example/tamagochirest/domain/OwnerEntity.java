package com.example.tamagochirest.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "owners")
public class OwnerEntity {

    @Id
    private UUID id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "tamagochis_count", nullable = false)
    private Integer tamagochisCount = 0;

    @Version
    private Long version;

    public OwnerEntity() {
    }

    public OwnerEntity(UUID id, String name, LocalDate birthDate, Integer tamagochisCount) {
        this.id = id;
        this.name = name;
        this.birthDate = birthDate;
        this.tamagochisCount = tamagochisCount;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public Integer getTamagochisCount() {
        return tamagochisCount;
    }

    public void setTamagochisCount(Integer tamagochisCount) {
        this.tamagochisCount = tamagochisCount;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
