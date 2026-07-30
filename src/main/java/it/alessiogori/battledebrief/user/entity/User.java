package it.alessiogori.battledebrief.user.entity;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
        name = "app_users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_username", columnNames = "username"),
                @UniqueConstraint(name = "uk_users_email", columnNames = "email")
        }
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 3, max = 50)
    @Column(nullable = false, length = 50)
    private String username;

    @NotBlank
    @Email
    @Size(max = 254)
    @Column(nullable = false, length = 254)
    private String email;

    @NotBlank
    @Size(max = 255)
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false, length = 20)
    private AuthProvider authProvider = AuthProvider.LOCAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.USER;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToOne(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private PlayerProfile playerProfile;

    protected User() {
    }

    public User(String username, String email, String passwordHash) {
        this.username = Objects.requireNonNull(username);
        this.email = Objects.requireNonNull(email);
        this.passwordHash = Objects.requireNonNull(passwordHash);
    }

    @PrePersist
    void initializeCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public void linkPlayerProfile(PlayerProfile profile) {
        Objects.requireNonNull(profile);

        if (profile.getUser() != null && profile.getUser() != this) {
            throw new IllegalArgumentException(
                    "Player profile is already linked to another user"
            );
        }

        playerProfile = profile;
        profile.assignTo(this);
    }

    public void changeEmail(String email) {
        this.email = Objects.requireNonNull(email);
    }

    public void changePasswordHash(String passwordHash) {
        this.passwordHash = Objects.requireNonNull(passwordHash);
    }

    public void changeRole(Role role) {
        this.role = Objects.requireNonNull(role);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public AuthProvider getAuthProvider() {
        return authProvider;
    }

    public Role getRole() {
        return role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public PlayerProfile getPlayerProfile() {
        return playerProfile;
    }
}
