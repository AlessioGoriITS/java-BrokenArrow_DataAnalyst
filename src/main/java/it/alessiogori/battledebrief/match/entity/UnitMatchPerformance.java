package it.alessiogori.battledebrief.match.entity;

import it.alessiogori.battledebrief.unit.entity.Unit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.Objects;

@Entity
@Table(
        name = "unit_match_performances",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_unit_match_performance_unit",
                columnNames = {"match_performance_id", "unit_id"}
        )
)
public class UnitMatchPerformance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @PositiveOrZero
    @Column(name = "unit_cost", nullable = false)
    private Integer unitCost;

    @NotNull
    @PositiveOrZero
    @Column(name = "spawned_count", nullable = false)
    private Integer spawnedCount;

    @NotNull
    @PositiveOrZero
    @Column(name = "lost_count", nullable = false)
    private Integer lostCount;

    @NotNull
    @PositiveOrZero
    @Column(name = "kills_count", nullable = false)
    private Integer killsCount;

    @NotNull
    @PositiveOrZero
    @Column(name = "destroyed_value", nullable = false)
    private Long destroyedValue;

    @PositiveOrZero
    @Column(name = "damage_dealt")
    private Long damageDealt;

    @PositiveOrZero
    @Column(name = "damage_received")
    private Long damageReceived;

    @PositiveOrZero
    @Column(name = "supply_consumed")
    private Long supplyConsumed;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_performance_id", nullable = false)
    private MatchPerformance matchPerformance;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    protected UnitMatchPerformance() {
    }

    public UnitMatchPerformance(
            Unit unit,
            Integer unitCost,
            Integer spawnedCount,
            Integer lostCount
    ) {
        this.unit = Objects.requireNonNull(unit);
        this.unitCost = Objects.requireNonNull(unitCost);
        this.spawnedCount = Objects.requireNonNull(spawnedCount);
        this.lostCount = Objects.requireNonNull(lostCount);
        this.killsCount = 0;
        this.destroyedValue = 0L;
    }

    void assignTo(MatchPerformance matchPerformance) {
        if (this.matchPerformance != null
                && this.matchPerformance != matchPerformance) {
            throw new IllegalStateException(
                    "Unit performance is already linked to another performance"
            );
        }
        this.matchPerformance = Objects.requireNonNull(matchPerformance);
    }

    public void updateMetrics(
            Integer killsCount,
            Long destroyedValue,
            Long damageDealt,
            Long damageReceived,
            Long supplyConsumed
    ) {
        this.killsCount = Objects.requireNonNull(killsCount);
        this.destroyedValue = Objects.requireNonNull(destroyedValue);
        this.damageDealt = damageDealt;
        this.damageReceived = damageReceived;
        this.supplyConsumed = supplyConsumed;
    }

    public Long getId() {
        return id;
    }

    public Integer getUnitCost() {
        return unitCost;
    }

    public Integer getSpawnedCount() {
        return spawnedCount;
    }

    public Integer getLostCount() {
        return lostCount;
    }

    public Integer getKillsCount() {
        return killsCount;
    }

    public Long getDestroyedValue() {
        return destroyedValue;
    }

    public Long getDamageDealt() {
        return damageDealt;
    }

    public Long getDamageReceived() {
        return damageReceived;
    }

    public Long getSupplyConsumed() {
        return supplyConsumed;
    }

    public MatchPerformance getMatchPerformance() {
        return matchPerformance;
    }

    public Unit getUnit() {
        return unit;
    }
}
