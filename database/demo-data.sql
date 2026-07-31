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
