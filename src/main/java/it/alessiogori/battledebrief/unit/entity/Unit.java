package it.alessiogori.battledebrief.unit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(
        name = "game_units",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_units_external_id",
                columnNames = "external_unit_id"
        )
)
public class Unit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(name = "external_unit_id", nullable = false, length = 100)
    private String externalUnitId;

    @NotBlank
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String name;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String faction;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String category;

    @NotNull
    @PositiveOrZero
    @Column(name = "base_cost", nullable = false)
    private Integer baseCost;

    @Size(max = 4000)
    @Column(length = 4000)
    private String description;

    @PositiveOrZero
    @Column(name = "hit_points")
    private Integer hitPoints;

    @PositiveOrZero
    @Column(precision = 8, scale = 2)
    private BigDecimal speed;

    @Size(max = 100)
    @Column(length = 100)
    private String armor;

    @Size(max = 200)
    @Column(name = "main_weapon", length = 200)
    private String mainWeapon;

    @Size(max = 2048)
    @Column(name = "image_url", length = 2048)
    private String imageUrl;

    @NotBlank
    @Size(max = 50)
    @Column(name = "dataset_version", nullable = false, length = 50)
    private String datasetVersion;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "unit_specializations",
            joinColumns = @JoinColumn(name = "unit_id"),
            inverseJoinColumns = @JoinColumn(name = "specialization_id"),
            uniqueConstraints = @UniqueConstraint(
                    name = "uk_unit_specialization",
                    columnNames = {"unit_id", "specialization_id"}
            )
    )
    private Set<Specialization> specializations = new LinkedHashSet<>();

    protected Unit() {
    }

    public Unit(
            String externalUnitId,
            String name,
            String faction,
            String category,
            Integer baseCost,
            String datasetVersion
    ) {
        this.externalUnitId = Objects.requireNonNull(externalUnitId);
        this.name = Objects.requireNonNull(name);
        this.faction = Objects.requireNonNull(faction);
        this.category = Objects.requireNonNull(category);
        this.baseCost = Objects.requireNonNull(baseCost);
        this.datasetVersion = Objects.requireNonNull(datasetVersion);
    }

    public void addSpecialization(Specialization specialization) {
        Objects.requireNonNull(specialization);
        if (specializations.add(specialization)) {
            specialization.registerUnit(this);
        }
    }

    public void replaceSpecializations(
            Collection<Specialization> newSpecializations
    ) {
        Objects.requireNonNull(newSpecializations);
        for (Specialization specialization : Set.copyOf(specializations)) {
            specialization.unregisterUnit(this);
        }
        specializations.clear();
        newSpecializations.forEach(this::addSpecialization);
    }

    public void updateDetails(
            String name,
            String faction,
            String category,
            Integer baseCost,
            String description,
            Integer hitPoints,
            BigDecimal speed,
            String armor,
            String mainWeapon,
            String imageUrl,
            String datasetVersion
    ) {
        this.name = Objects.requireNonNull(name);
        this.faction = Objects.requireNonNull(faction);
        this.category = Objects.requireNonNull(category);
        this.baseCost = Objects.requireNonNull(baseCost);
        this.description = description;
        this.hitPoints = hitPoints;
        this.speed = speed;
        this.armor = armor;
        this.mainWeapon = mainWeapon;
        this.imageUrl = imageUrl;
        this.datasetVersion = Objects.requireNonNull(datasetVersion);
    }

    public Long getId() {
        return id;
    }

    public String getExternalUnitId() {
        return externalUnitId;
    }

    public String getName() {
        return name;
    }

    public String getFaction() {
        return faction;
    }

    public String getCategory() {
        return category;
    }

    public Integer getBaseCost() {
        return baseCost;
    }

    public String getDescription() {
        return description;
    }

    public Integer getHitPoints() {
        return hitPoints;
    }

    public BigDecimal getSpeed() {
        return speed;
    }

    public String getArmor() {
        return armor;
    }

    public String getMainWeapon() {
        return mainWeapon;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getDatasetVersion() {
        return datasetVersion;
    }

    public Set<Specialization> getSpecializations() {
        return Collections.unmodifiableSet(specializations);
    }
}
