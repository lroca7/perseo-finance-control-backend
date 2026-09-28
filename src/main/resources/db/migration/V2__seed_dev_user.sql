-- V2__seed_dev_user.sql
-- Mientras no exista login real (HU-T.8 pendiente), el frontend usa un
-- userId fijo (DEV_USER_ID en frontend/src/api/endpoints.ts) para poder
-- probar la app de punta a punta. Este usuario debe existir para que la
-- foreign key de `cards.user_id` no falle.
INSERT INTO users (id, email, password_hash)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'dev@perseo.local',
    'no-aplica-sin-auth'
)
ON CONFLICT (id) DO NOTHING;
