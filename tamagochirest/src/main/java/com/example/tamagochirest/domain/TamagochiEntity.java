package com.example.tamagochirest.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tamagochis")
public class TamagochiEntity {

    @Id
    private UUID id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "species", nullable = false, length = 100)
    private String species;

    @Column(name = "color", nullable = false, length = 50)
    private String color;

    @Column(name = "is_alive", nullable = false)
    private Boolean isAlive = true;

    @Column(name = "health", nullable = false)
    private Integer health;

    @Column(name = "hunger", nullable = false)
    private Integer hunger;

    @Column(name = "happiness", nullable = false)
    private Integer happiness;

    @Column(name = "energy", nullable = false)
    private Integer energy;

    @Column(name = "clearliness", nullable = false)
    private Integer clearliness;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private OwnerEntity owner;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Version
    private Long version;

    public TamagochiEntity() {
    }

    public TamagochiEntity(UUID id, String name, String species, String color, Boolean isAlive,
                          Integer health, Integer hunger, Integer happiness, Integer energy,
                          Integer clearliness, LocalDate birthDate, OwnerEntity owner,
                          OffsetDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.species = species;
        this.color = color;
        this.isAlive = isAlive;
        this.health = health;
        this.hunger = hunger;
        this.happiness = happiness;
        this.energy = energy;
        this.clearliness = clearliness;
        this.birthDate = birthDate;
        this.owner = owner;
        this.createdAt = createdAt;
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

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Boolean getIsAlive() {
        return isAlive;
    }

    public void setIsAlive(Boolean isAlive) {
        this.isAlive = isAlive;
    }

    public Integer getHealth() {
        return health;
    }

    public void setHealth(Integer health) {
        this.health = health;
    }

    public Integer getHunger() {
        return hunger;
    }

    public void setHunger(Integer hunger) {
        this.hunger = hunger;
    }

    public Integer getHappiness() {
        return happiness;
    }

    public void setHappiness(Integer happiness) {
        this.happiness = happiness;
    }

    public Integer getEnergy() {
        return energy;
    }

    public void setEnergy(Integer energy) {
        this.energy = energy;
    }

    public Integer getClearliness() {
        return clearliness;
    }

    public void setClearliness(Integer clearliness) {
        this.clearliness = clearliness;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public OwnerEntity getOwner() {
        return owner;
    }

    public void setOwner(OwnerEntity owner) {
        this.owner = owner;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
