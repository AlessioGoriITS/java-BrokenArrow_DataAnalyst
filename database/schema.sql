-- Run this script while connected to the battle_debrief database.
-- Docker creates the database before executing initialization scripts.

CREATE TABLE IF NOT EXISTS app_users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    auth_provider ENUM ('LOCAL', 'STEAM') NOT NULL,
    role ENUM ('USER', 'ADMIN') NOT NULL,
    enabled BIT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS player_profiles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    display_name VARCHAR(100) NOT NULL,
    steam_id VARCHAR(17),
    external_commander_id VARCHAR(100),
    avatar_url VARCHAR(2048),
    current_elo INT,
    peak_elo INT,
    last_sync_at DATETIME(6),
    user_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_profiles_user UNIQUE (user_id),
    CONSTRAINT uk_profiles_steam UNIQUE (steam_id),
    CONSTRAINT uk_profiles_commander UNIQUE (external_commander_id),
    CONSTRAINT fk_profiles_user FOREIGN KEY (user_id)
        REFERENCES app_users (id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS specializations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    faction VARCHAR(100) NOT NULL,
    description VARCHAR(2000),
    PRIMARY KEY (id),
    CONSTRAINT uk_specializations_name_faction UNIQUE (name, faction)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS game_units (
    id BIGINT NOT NULL AUTO_INCREMENT,
    external_unit_id VARCHAR(100) NOT NULL,
    name VARCHAR(150) NOT NULL,
    faction VARCHAR(100) NOT NULL,
    category VARCHAR(100) NOT NULL,
    base_cost INT NOT NULL,
    description VARCHAR(4000),
    hit_points INT,
    speed DECIMAL(8, 2),
    armor VARCHAR(100),
    main_weapon VARCHAR(200),
    image_url VARCHAR(2048),
    dataset_version VARCHAR(50) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_units_external_id UNIQUE (external_unit_id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS unit_specializations (
    unit_id BIGINT NOT NULL,
    specialization_id BIGINT NOT NULL,
    PRIMARY KEY (unit_id, specialization_id),
    CONSTRAINT fk_unit_specializations_unit FOREIGN KEY (unit_id)
        REFERENCES game_units (id),
    CONSTRAINT fk_unit_specializations_specialization
        FOREIGN KEY (specialization_id)
        REFERENCES specializations (id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS game_matches (
    id BIGINT NOT NULL AUTO_INCREMENT,
    external_match_id VARCHAR(100) NOT NULL,
    map_name VARCHAR(150) NOT NULL,
    game_mode VARCHAR(100) NOT NULL,
    started_at DATETIME(6) NOT NULL,
    duration_seconds INT NOT NULL,
    winner_team ENUM ('TEAM_ONE', 'TEAM_TWO'),
    source ENUM ('JSON_IMPORT', 'EXTERNAL_PROVIDER', 'DEMO_DATA') NOT NULL,
    imported_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_matches_external_id UNIQUE (external_match_id),
    INDEX idx_matches_started_at (started_at),
    INDEX idx_matches_map_name (map_name)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS match_performances (
    id BIGINT NOT NULL AUTO_INCREMENT,
    team ENUM ('TEAM_ONE', 'TEAM_TWO') NOT NULL,
    won BIT NOT NULL,
    old_rating INT,
    new_rating INT,
    destruction_score BIGINT,
    losses_score BIGINT,
    damage_dealt BIGINT,
    damage_received BIGINT,
    objectives_captured INT,
    spawned_unit_score BIGINT,
    refunded_unit_score BIGINT,
    supply_consumed BIGINT,
    player_profile_id BIGINT NOT NULL,
    game_match_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_match_performance_player
        UNIQUE (game_match_id, player_profile_id),
    CONSTRAINT fk_match_performances_player FOREIGN KEY (player_profile_id)
        REFERENCES player_profiles (id),
    CONSTRAINT fk_match_performances_match FOREIGN KEY (game_match_id)
        REFERENCES game_matches (id),
    INDEX idx_match_performances_player (player_profile_id)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS unit_match_performances (
    id BIGINT NOT NULL AUTO_INCREMENT,
    unit_cost INT NOT NULL,
    spawned_count INT NOT NULL,
    lost_count INT NOT NULL,
    kills_count INT NOT NULL,
    destroyed_value BIGINT NOT NULL,
    damage_dealt BIGINT,
    damage_received BIGINT,
    supply_consumed BIGINT,
    match_performance_id BIGINT NOT NULL,
    unit_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_unit_match_performance_unit
        UNIQUE (match_performance_id, unit_id),
    CONSTRAINT fk_unit_performances_match_performance
        FOREIGN KEY (match_performance_id)
        REFERENCES match_performances (id),
    CONSTRAINT fk_unit_performances_unit FOREIGN KEY (unit_id)
        REFERENCES game_units (id),
    INDEX idx_unit_performances_unit (unit_id)
) ENGINE = InnoDB;
