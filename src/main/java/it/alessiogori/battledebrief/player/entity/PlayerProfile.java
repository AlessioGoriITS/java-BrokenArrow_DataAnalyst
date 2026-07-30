package it.alessiogori.battledebrief.player.entity;

import it.alessiogori.battledebrief.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
        name = "player_profiles",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_profiles_user", columnNames = "user_id"),
                @UniqueConstraint(name = "uk_profiles_steam", columnNames = "steam_id"),
                @UniqueConstraint(
                        name = "uk_profiles_commander",
                        columnNames = "external_commander_id"
                )
        }
)
public class PlayerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Pattern(regexp = "\\d{17}")
    @Column(name = "steam_id", length = 17)
    private String steamId;

    @Size(max = 100)
    @Column(name = "external_commander_id", length = 100)
    private String externalCommanderId;

    @Size(max = 2048)
    @Column(name = "avatar_url", length = 2048)
    private String avatarUrl;

    @PositiveOrZero
    @Column(name = "current_elo")
    private Integer currentElo;

    @PositiveOrZero
    @Column(name = "peak_elo")
    private Integer peakElo;

    @Column(name = "last_sync_at")
    private Instant lastSyncAt;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    protected PlayerProfile() {
    }

    public PlayerProfile(String displayName) {
        this.displayName = Objects.requireNonNull(displayName);
    }

    public void assignTo(User user) {
        if (this.user != null && this.user != user) {
            throw new IllegalStateException(
                    "Player profile is already linked to another user"
            );
        }
        this.user = Objects.requireNonNull(user);
    }

    public void updateExternalIdentity(
            String displayName,
            String steamId,
            String externalCommanderId,
            String avatarUrl
    ) {
        this.displayName = Objects.requireNonNull(displayName);
        this.steamId = steamId;
        this.externalCommanderId = externalCommanderId;
        this.avatarUrl = avatarUrl;
    }

    public void updateElo(Integer currentElo, Integer peakElo) {
        this.currentElo = currentElo;
        this.peakElo = peakElo;
    }

    public void markSynchronized(Instant synchronizedAt) {
        lastSyncAt = Objects.requireNonNull(synchronizedAt);
    }

    public Long getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSteamId() {
        return steamId;
    }

    public String getExternalCommanderId() {
        return externalCommanderId;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public Integer getCurrentElo() {
        return currentElo;
    }

    public Integer getPeakElo() {
        return peakElo;
    }

    public Instant getLastSyncAt() {
        return lastSyncAt;
    }

    public User getUser() {
        return user;
    }
}
