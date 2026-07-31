package it.alessiogori.battledebrief.match.entity;

import it.alessiogori.battledebrief.player.entity.PlayerProfile;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Entity
@Table(
        name = "match_performances",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_match_performance_player",
                columnNames = {"game_match_id", "player_profile_id"}
        )
)
public class MatchPerformance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TeamSide team;

    @Column(nullable = false)
    private boolean won;

    @PositiveOrZero
    @Column(name = "old_rating")
    private Integer oldRating;

    @PositiveOrZero
    @Column(name = "new_rating")
    private Integer newRating;

    @PositiveOrZero
    @Column(name = "destruction_score")
    private Long destructionScore;

    @PositiveOrZero
    @Column(name = "losses_score")
    private Long lossesScore;

    @PositiveOrZero
    @Column(name = "damage_dealt")
    private Long damageDealt;

    @PositiveOrZero
    @Column(name = "damage_received")
    private Long damageReceived;

    @PositiveOrZero
    @Column(name = "objectives_captured")
    private Integer objectivesCaptured;

    @PositiveOrZero
    @Column(name = "spawned_unit_score")
    private Long spawnedUnitScore;

    @PositiveOrZero
    @Column(name = "refunded_unit_score")
    private Long refundedUnitScore;

    @PositiveOrZero
    @Column(name = "supply_consumed")
    private Long supplyConsumed;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_profile_id", nullable = false)
    private PlayerProfile playerProfile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_match_id", nullable = false)
    private GameMatch gameMatch;

    @OneToMany(
            mappedBy = "matchPerformance",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<UnitMatchPerformance> unitPerformances = new ArrayList<>();

    protected MatchPerformance() {
    }

    public MatchPerformance(
            PlayerProfile playerProfile,
            TeamSide team,
            boolean won
    ) {
        this.playerProfile = Objects.requireNonNull(playerProfile);
        this.team = Objects.requireNonNull(team);
        this.won = won;
    }

    void assignTo(GameMatch gameMatch) {
        if (this.gameMatch != null && this.gameMatch != gameMatch) {
            throw new IllegalStateException(
                    "Performance is already linked to another match"
            );
        }
        this.gameMatch = Objects.requireNonNull(gameMatch);
    }

    public void addUnitPerformance(UnitMatchPerformance performance) {
        Objects.requireNonNull(performance);
        performance.assignTo(this);
        unitPerformances.add(performance);
    }

    public void updateMetrics(
            Integer oldRating,
            Integer newRating,
            Long destructionScore,
            Long lossesScore,
            Long damageDealt,
            Long damageReceived,
            Integer objectivesCaptured,
            Long spawnedUnitScore,
            Long refundedUnitScore,
            Long supplyConsumed
    ) {
        this.oldRating = oldRating;
        this.newRating = newRating;
        this.destructionScore = destructionScore;
        this.lossesScore = lossesScore;
        this.damageDealt = damageDealt;
        this.damageReceived = damageReceived;
        this.objectivesCaptured = objectivesCaptured;
        this.spawnedUnitScore = spawnedUnitScore;
        this.refundedUnitScore = refundedUnitScore;
        this.supplyConsumed = supplyConsumed;
    }

    public Long getId() {
        return id;
    }

    public TeamSide getTeam() {
        return team;
    }

    public boolean isWon() {
        return won;
    }

    public Integer getOldRating() {
        return oldRating;
    }

    public Integer getNewRating() {
        return newRating;
    }

    public Long getDestructionScore() {
        return destructionScore;
    }

    public Long getLossesScore() {
        return lossesScore;
    }

    public Long getDamageDealt() {
        return damageDealt;
    }

    public Long getDamageReceived() {
        return damageReceived;
    }

    public Integer getObjectivesCaptured() {
        return objectivesCaptured;
    }

    public Long getSpawnedUnitScore() {
        return spawnedUnitScore;
    }

    public Long getRefundedUnitScore() {
        return refundedUnitScore;
    }

    public Long getSupplyConsumed() {
        return supplyConsumed;
    }

    public PlayerProfile getPlayerProfile() {
        return playerProfile;
    }

    public GameMatch getGameMatch() {
        return gameMatch;
    }

    public List<UnitMatchPerformance> getUnitPerformances() {
        return Collections.unmodifiableList(unitPerformances);
    }
}
