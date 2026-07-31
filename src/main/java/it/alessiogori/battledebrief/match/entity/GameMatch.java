package it.alessiogori.battledebrief.match.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Entity
@Table(
        name = "game_matches",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_matches_external_id",
                columnNames = "external_match_id"
        )
)
public class GameMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(name = "external_match_id", nullable = false, length = 100)
    private String externalMatchId;

    @NotBlank
    @Size(max = 150)
    @Column(name = "map_name", nullable = false, length = 150)
    private String mapName;

    @NotBlank
    @Size(max = 100)
    @Column(name = "game_mode", nullable = false, length = 100)
    private String gameMode;

    @NotNull
    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @NotNull
    @Positive
    @Column(name = "duration_seconds", nullable = false)
    private Integer durationSeconds;

    @Enumerated(EnumType.STRING)
    @Column(name = "winner_team", length = 20)
    private TeamSide winnerTeam;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MatchSource source;

    @Column(name = "imported_at", nullable = false, updatable = false)
    private Instant importedAt;

    @OneToMany(
            mappedBy = "gameMatch",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<MatchPerformance> performances = new ArrayList<>();

    protected GameMatch() {
    }

    public GameMatch(
            String externalMatchId,
            String mapName,
            String gameMode,
            Instant startedAt,
            Integer durationSeconds,
            TeamSide winnerTeam,
            MatchSource source
    ) {
        this.externalMatchId = Objects.requireNonNull(externalMatchId);
        this.mapName = Objects.requireNonNull(mapName);
        this.gameMode = Objects.requireNonNull(gameMode);
        this.startedAt = Objects.requireNonNull(startedAt);
        this.durationSeconds = Objects.requireNonNull(durationSeconds);
        this.winnerTeam = winnerTeam;
        this.source = Objects.requireNonNull(source);
    }

    @PrePersist
    void initializeImportedAt() {
        if (importedAt == null) {
            importedAt = Instant.now();
        }
    }

    public void addPerformance(MatchPerformance performance) {
        Objects.requireNonNull(performance);
        performance.assignTo(this);
        performances.add(performance);
    }

    public Long getId() {
        return id;
    }

    public String getExternalMatchId() {
        return externalMatchId;
    }

    public String getMapName() {
        return mapName;
    }

    public String getGameMode() {
        return gameMode;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public TeamSide getWinnerTeam() {
        return winnerTeam;
    }

    public MatchSource getSource() {
        return source;
    }

    public Instant getImportedAt() {
        return importedAt;
    }

    public List<MatchPerformance> getPerformances() {
        return Collections.unmodifiableList(performances);
    }
}
