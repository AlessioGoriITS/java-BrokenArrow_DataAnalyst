package it.alessiogori.battledebrief.unit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(
        name = "specializations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_specializations_name_faction",
                columnNames = {"name", "faction"}
        )
)
public class Specialization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String faction;

    @Size(max = 2000)
    @Column(length = 2000)
    private String description;

    @ManyToMany(mappedBy = "specializations")
    private Set<Unit> units = new LinkedHashSet<>();

    protected Specialization() {
    }

    public Specialization(String name, String faction, String description) {
        this.name = Objects.requireNonNull(name);
        this.faction = Objects.requireNonNull(faction);
        this.description = description;
    }

    void registerUnit(Unit unit) {
        units.add(Objects.requireNonNull(unit));
    }

    void unregisterUnit(Unit unit) {
        units.remove(unit);
    }

    public void updateDetails(
            String name,
            String faction,
            String description
    ) {
        this.name = Objects.requireNonNull(name);
        this.faction = Objects.requireNonNull(faction);
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getFaction() {
        return faction;
    }

    public String getDescription() {
        return description;
    }

    public Set<Unit> getUnits() {
        return Collections.unmodifiableSet(units);
    }
}
