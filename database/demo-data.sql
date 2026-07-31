-- Development-only accounts. Change these credentials outside local demos.
-- admin / Admin123!
-- demo / Demo123!
-- analyst / Demo123!

INSERT INTO app_users (
    username,
    email,
    password_hash,
    auth_provider,
    role,
    enabled,
    created_at
)
SELECT
    'admin',
    'admin@battle-debrief.local',
    '$2a$10$3h9P2gtWguHkWRaAE/TAVOWuXCYduGFDdalWjI0MLer.M8CoFC/ZC',
    'LOCAL',
    'ADMIN',
    TRUE,
    CURRENT_TIMESTAMP(6)
WHERE NOT EXISTS (
    SELECT 1 FROM app_users WHERE username = 'admin'
);

INSERT INTO app_users (
    username,
    email,
    password_hash,
    auth_provider,
    role,
    enabled,
    created_at
)
SELECT
    'demo',
    'demo@battle-debrief.local',
    '$2a$10$CeIxldaBdwl.mlYRKOG8ieuLHuJUaeTzch8lcR7T3wmi1Xp7ESJ6W',
    'LOCAL',
    'USER',
    TRUE,
    CURRENT_TIMESTAMP(6)
WHERE NOT EXISTS (
    SELECT 1 FROM app_users WHERE username = 'demo'
);

INSERT INTO app_users (
    username,
    email,
    password_hash,
    auth_provider,
    role,
    enabled,
    created_at
)
SELECT
    'analyst',
    'analyst@battle-debrief.local',
    '$2a$10$CeIxldaBdwl.mlYRKOG8ieuLHuJUaeTzch8lcR7T3wmi1Xp7ESJ6W',
    'LOCAL',
    'USER',
    TRUE,
    CURRENT_TIMESTAMP(6)
WHERE NOT EXISTS (
    SELECT 1 FROM app_users WHERE username = 'analyst'
);

INSERT INTO player_profiles (
    display_name,
    external_commander_id,
    current_elo,
    peak_elo,
    user_id
)
SELECT
    'Battle Debrief Admin',
    'demo-admin',
    1600,
    1650,
    user_account.id
FROM app_users user_account
WHERE user_account.username = 'admin'
  AND NOT EXISTS (
      SELECT 1
      FROM player_profiles profile
      WHERE profile.user_id = user_account.id
  );

INSERT INTO player_profiles (
    display_name,
    steam_id,
    external_commander_id,
    current_elo,
    peak_elo,
    user_id
)
SELECT
    'Demo Commander',
    '76561198000000001',
    'demo-commander',
    1500,
    1575,
    user_account.id
FROM app_users user_account
WHERE user_account.username = 'demo'
  AND NOT EXISTS (
      SELECT 1
      FROM player_profiles profile
      WHERE profile.user_id = user_account.id
  );

INSERT INTO player_profiles (
    display_name,
    steam_id,
    external_commander_id,
    current_elo,
    peak_elo,
    user_id
)
SELECT
    'Dataset Analyst',
    '76561198000000002',
    'demo-analyst',
    1450,
    1520,
    user_account.id
FROM app_users user_account
WHERE user_account.username = 'analyst'
  AND NOT EXISTS (
      SELECT 1
      FROM player_profiles profile
      WHERE profile.user_id = user_account.id
  );

-- Minimal catalog subset used by the demo telemetry below. The application
-- initializer completes the catalog from the versioned JSON dataset.
INSERT INTO specializations (name, faction, description)
SELECT 'US Armored Brigade', 'USA',
       'Heavy combined-arms formation centered on Abrams tanks.'
WHERE NOT EXISTS (
    SELECT 1 FROM specializations
    WHERE name = 'US Armored Brigade' AND faction = 'USA'
);

INSERT INTO specializations (name, faction, description)
SELECT 'US Airborne Brigade', 'USA',
       'Rapid-deployment formation supported by helicopters.'
WHERE NOT EXISTS (
    SELECT 1 FROM specializations
    WHERE name = 'US Airborne Brigade' AND faction = 'USA'
);

INSERT INTO specializations (name, faction, description)
SELECT 'RU Guards Tank Brigade', 'RUS',
       'Armored formation built around main battle tanks.'
WHERE NOT EXISTS (
    SELECT 1 FROM specializations
    WHERE name = 'RU Guards Tank Brigade' AND faction = 'RUS'
);

INSERT INTO game_units (
    external_unit_id, name, faction, category, base_cost, description,
    hit_points, speed, armor, main_weapon, dataset_version
)
SELECT 'usa_m1a1_abrams', 'M1A1 Abrams', 'USA', 'TANK', 240,
       'Main battle tank with strong frontal protection.',
       1350, 52.00, 'HEAVY', '120 mm M256', 'demo-2026.1'
WHERE NOT EXISTS (
    SELECT 1 FROM game_units WHERE external_unit_id = 'usa_m1a1_abrams'
);

INSERT INTO game_units (
    external_unit_id, name, faction, category, base_cost, description,
    hit_points, speed, armor, main_weapon, dataset_version
)
SELECT 'usa_m2a2_bradley', 'M2A2 Bradley', 'USA', 'IFV', 150,
       'Mechanized infantry fighting vehicle.',
       850, 61.00, 'MEDIUM', '25 mm M242 Bushmaster', 'demo-2026.1'
WHERE NOT EXISTS (
    SELECT 1 FROM game_units WHERE external_unit_id = 'usa_m2a2_bradley'
);

INSERT INTO game_units (
    external_unit_id, name, faction, category, base_cost, description,
    hit_points, speed, armor, main_weapon, dataset_version
)
SELECT 'usa_ah64d_apache', 'AH-64D Apache', 'USA', 'HELICOPTER', 220,
       'Attack helicopter optimized for anti-armor missions.',
       620, 78.00, 'LIGHT', 'AGM-114 Hellfire', 'demo-2026.1'
WHERE NOT EXISTS (
    SELECT 1 FROM game_units WHERE external_unit_id = 'usa_ah64d_apache'
);

INSERT INTO game_units (
    external_unit_id, name, faction, category, base_cost, description,
    hit_points, speed, armor, main_weapon, dataset_version
)
SELECT 'rus_t90a', 'T-90A', 'RUS', 'TANK', 235,
       'Russian main battle tank with layered protection.',
       1320, 50.00, 'HEAVY', '125 mm 2A46M', 'demo-2026.1'
WHERE NOT EXISTS (
    SELECT 1 FROM game_units WHERE external_unit_id = 'rus_t90a'
);

INSERT INTO unit_specializations (unit_id, specialization_id)
SELECT unit.id, specialization.id
FROM game_units unit, specializations specialization
WHERE unit.external_unit_id IN ('usa_m1a1_abrams', 'usa_m2a2_bradley')
  AND specialization.name = 'US Armored Brigade'
  AND specialization.faction = 'USA'
  AND NOT EXISTS (
      SELECT 1 FROM unit_specializations relation
      WHERE relation.unit_id = unit.id
        AND relation.specialization_id = specialization.id
  );

INSERT INTO unit_specializations (unit_id, specialization_id)
SELECT unit.id, specialization.id
FROM game_units unit, specializations specialization
WHERE unit.external_unit_id = 'usa_ah64d_apache'
  AND specialization.name = 'US Airborne Brigade'
  AND specialization.faction = 'USA'
  AND NOT EXISTS (
      SELECT 1 FROM unit_specializations relation
      WHERE relation.unit_id = unit.id
        AND relation.specialization_id = specialization.id
  );

INSERT INTO unit_specializations (unit_id, specialization_id)
SELECT unit.id, specialization.id
FROM game_units unit, specializations specialization
WHERE unit.external_unit_id = 'rus_t90a'
  AND specialization.name = 'RU Guards Tank Brigade'
  AND specialization.faction = 'RUS'
  AND NOT EXISTS (
      SELECT 1 FROM unit_specializations relation
      WHERE relation.unit_id = unit.id
        AND relation.specialization_id = specialization.id
  );

-- Idempotent after-action reports provide visible analytics on first startup.
INSERT INTO game_matches (
    external_match_id, map_name, game_mode, started_at, duration_seconds,
    winner_team, source, imported_at
)
SELECT 'demo-aar-001', 'River Crossing', '5V5',
       '2026-07-20 18:00:00', 1840, 'TEAM_ONE', 'DEMO_DATA',
       CURRENT_TIMESTAMP(6)
WHERE NOT EXISTS (
    SELECT 1 FROM game_matches WHERE external_match_id = 'demo-aar-001'
);

INSERT INTO game_matches (
    external_match_id, map_name, game_mode, started_at, duration_seconds,
    winner_team, source, imported_at
)
SELECT 'demo-aar-002', 'Black Forest', '5V5',
       '2026-07-22 19:15:00', 2010, 'TEAM_TWO', 'DEMO_DATA',
       CURRENT_TIMESTAMP(6)
WHERE NOT EXISTS (
    SELECT 1 FROM game_matches WHERE external_match_id = 'demo-aar-002'
);

INSERT INTO game_matches (
    external_match_id, map_name, game_mode, started_at, duration_seconds,
    winner_team, source, imported_at
)
SELECT 'demo-aar-003', 'Baltiysk', '3V3',
       '2026-07-24 17:40:00', 1650, 'TEAM_ONE', 'DEMO_DATA',
       CURRENT_TIMESTAMP(6)
WHERE NOT EXISTS (
    SELECT 1 FROM game_matches WHERE external_match_id = 'demo-aar-003'
);

INSERT INTO game_matches (
    external_match_id, map_name, game_mode, started_at, duration_seconds,
    winner_team, source, imported_at
)
SELECT 'demo-aar-004', 'River Crossing', '5V5',
       '2026-07-26 20:05:00', 1920, 'TEAM_ONE', 'DEMO_DATA',
       CURRENT_TIMESTAMP(6)
WHERE NOT EXISTS (
    SELECT 1 FROM game_matches WHERE external_match_id = 'demo-aar-004'
);

INSERT INTO game_matches (
    external_match_id, map_name, game_mode, started_at, duration_seconds,
    winner_team, source, imported_at
)
SELECT 'demo-aar-005', 'Kaliningrad', '5V5',
       '2026-07-28 18:30:00', 2130, 'TEAM_TWO', 'DEMO_DATA',
       CURRENT_TIMESTAMP(6)
WHERE NOT EXISTS (
    SELECT 1 FROM game_matches WHERE external_match_id = 'demo-aar-005'
);

INSERT INTO game_matches (
    external_match_id, map_name, game_mode, started_at, duration_seconds,
    winner_team, source, imported_at
)
SELECT 'demo-aar-006', 'River Crossing', '5V5',
       '2026-07-30 21:10:00', 1760, 'TEAM_ONE', 'DEMO_DATA',
       CURRENT_TIMESTAMP(6)
WHERE NOT EXISTS (
    SELECT 1 FROM game_matches WHERE external_match_id = 'demo-aar-006'
);

INSERT INTO match_performances (
    team, won, old_rating, new_rating, destruction_score, losses_score,
    damage_dealt, damage_received, objectives_captured, spawned_unit_score,
    refunded_unit_score, supply_consumed, player_profile_id, game_match_id
)
SELECT 'TEAM_ONE', TRUE, 1470, 1484, 2650, 1510,
       6100, 3200, 3, 2050, 120, 680, profile.id, match_record.id
FROM player_profiles profile
JOIN app_users user_account ON user_account.id = profile.user_id
JOIN game_matches match_record ON match_record.external_match_id = 'demo-aar-001'
WHERE user_account.username = 'demo'
  AND NOT EXISTS (
      SELECT 1 FROM match_performances performance
      WHERE performance.player_profile_id = profile.id
        AND performance.game_match_id = match_record.id
  );

INSERT INTO match_performances (
    team, won, old_rating, new_rating, destruction_score, losses_score,
    damage_dealt, damage_received, objectives_captured, spawned_unit_score,
    refunded_unit_score, supply_consumed, player_profile_id, game_match_id
)
SELECT 'TEAM_ONE', FALSE, 1484, 1476, 1420, 2380,
       3900, 5700, 1, 1920, 80, 740, profile.id, match_record.id
FROM player_profiles profile
JOIN app_users user_account ON user_account.id = profile.user_id
JOIN game_matches match_record ON match_record.external_match_id = 'demo-aar-002'
WHERE user_account.username = 'demo'
  AND NOT EXISTS (
      SELECT 1 FROM match_performances performance
      WHERE performance.player_profile_id = profile.id
        AND performance.game_match_id = match_record.id
  );

INSERT INTO match_performances (
    team, won, old_rating, new_rating, destruction_score, losses_score,
    damage_dealt, damage_received, objectives_captured, spawned_unit_score,
    refunded_unit_score, supply_consumed, player_profile_id, game_match_id
)
SELECT 'TEAM_ONE', TRUE, 1476, 1491, 2940, 1280,
       6800, 2700, 4, 2140, 160, 610, profile.id, match_record.id
FROM player_profiles profile
JOIN app_users user_account ON user_account.id = profile.user_id
JOIN game_matches match_record ON match_record.external_match_id = 'demo-aar-003'
WHERE user_account.username = 'demo'
  AND NOT EXISTS (
      SELECT 1 FROM match_performances performance
      WHERE performance.player_profile_id = profile.id
        AND performance.game_match_id = match_record.id
  );

INSERT INTO match_performances (
    team, won, old_rating, new_rating, destruction_score, losses_score,
    damage_dealt, damage_received, objectives_captured, spawned_unit_score,
    refunded_unit_score, supply_consumed, player_profile_id, game_match_id
)
SELECT 'TEAM_ONE', TRUE, 1491, 1507, 3180, 1640,
       7200, 3500, 3, 2260, 90, 790, profile.id, match_record.id
FROM player_profiles profile
JOIN app_users user_account ON user_account.id = profile.user_id
JOIN game_matches match_record ON match_record.external_match_id = 'demo-aar-004'
WHERE user_account.username = 'demo'
  AND NOT EXISTS (
      SELECT 1 FROM match_performances performance
      WHERE performance.player_profile_id = profile.id
        AND performance.game_match_id = match_record.id
  );

INSERT INTO match_performances (
    team, won, old_rating, new_rating, destruction_score, losses_score,
    damage_dealt, damage_received, objectives_captured, spawned_unit_score,
    refunded_unit_score, supply_consumed, player_profile_id, game_match_id
)
SELECT 'TEAM_ONE', FALSE, 1507, 1498, 1710, 2510,
       4100, 6200, 1, 2010, 60, 820, profile.id, match_record.id
FROM player_profiles profile
JOIN app_users user_account ON user_account.id = profile.user_id
JOIN game_matches match_record ON match_record.external_match_id = 'demo-aar-005'
WHERE user_account.username = 'demo'
  AND NOT EXISTS (
      SELECT 1 FROM match_performances performance
      WHERE performance.player_profile_id = profile.id
        AND performance.game_match_id = match_record.id
  );

INSERT INTO match_performances (
    team, won, old_rating, new_rating, destruction_score, losses_score,
    damage_dealt, damage_received, objectives_captured, spawned_unit_score,
    refunded_unit_score, supply_consumed, player_profile_id, game_match_id
)
SELECT 'TEAM_ONE', TRUE, 1498, 1500, 2490, 1390,
       5900, 3000, 2, 1980, 110, 650, profile.id, match_record.id
FROM player_profiles profile
JOIN app_users user_account ON user_account.id = profile.user_id
JOIN game_matches match_record ON match_record.external_match_id = 'demo-aar-006'
WHERE user_account.username = 'demo'
  AND NOT EXISTS (
      SELECT 1 FROM match_performances performance
      WHERE performance.player_profile_id = profile.id
        AND performance.game_match_id = match_record.id
  );

-- One representative unit record per after-action report is enough to drive
-- unit and specialization analytics while keeping the demo dataset readable.
INSERT INTO unit_match_performances (
    unit_cost, spawned_count, lost_count, kills_count, destroyed_value,
    damage_dealt, damage_received, supply_consumed,
    match_performance_id, unit_id
)
SELECT 240, 4, 1, 5, 1650, 3500, 1200, 180,
       performance.id, unit.id
FROM match_performances performance
JOIN game_matches match_record ON match_record.id = performance.game_match_id
JOIN game_units unit ON unit.external_unit_id = 'usa_m1a1_abrams'
WHERE match_record.external_match_id IN (
    'demo-aar-001', 'demo-aar-004', 'demo-aar-006'
)
  AND NOT EXISTS (
      SELECT 1 FROM unit_match_performances unit_performance
      WHERE unit_performance.match_performance_id = performance.id
        AND unit_performance.unit_id = unit.id
  );

INSERT INTO unit_match_performances (
    unit_cost, spawned_count, lost_count, kills_count, destroyed_value,
    damage_dealt, damage_received, supply_consumed,
    match_performance_id, unit_id
)
SELECT 150, 5, 2, 3, 720, 1900, 1400, 130,
       performance.id, unit.id
FROM match_performances performance
JOIN game_matches match_record ON match_record.id = performance.game_match_id
JOIN game_units unit ON unit.external_unit_id = 'usa_m2a2_bradley'
WHERE match_record.external_match_id IN ('demo-aar-002', 'demo-aar-005')
  AND NOT EXISTS (
      SELECT 1 FROM unit_match_performances unit_performance
      WHERE unit_performance.match_performance_id = performance.id
        AND unit_performance.unit_id = unit.id
  );

INSERT INTO unit_match_performances (
    unit_cost, spawned_count, lost_count, kills_count, destroyed_value,
    damage_dealt, damage_received, supply_consumed,
    match_performance_id, unit_id
)
SELECT 220, 2, 0, 4, 1380, 2600, 500, 210,
       performance.id, unit.id
FROM match_performances performance
JOIN game_matches match_record ON match_record.id = performance.game_match_id
JOIN game_units unit ON unit.external_unit_id = 'usa_ah64d_apache'
WHERE match_record.external_match_id = 'demo-aar-003'
  AND NOT EXISTS (
      SELECT 1 FROM unit_match_performances unit_performance
      WHERE unit_performance.match_performance_id = performance.id
        AND unit_performance.unit_id = unit.id
  );
