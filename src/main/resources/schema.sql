CREATE TABLE IF NOT EXISTS app_users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(60) NOT NULL UNIQUE,
    display_name VARCHAR(120) NOT NULL,
    password_hash BYTEA,
    password_salt BYTEA,
    login_id VARCHAR(60),
    role VARCHAR(20) NOT NULL CHECK (role IN ('HEAD_DOCTOR', 'USER', 'RECEPTIONIST', 'PATIENT')),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE app_users DROP CONSTRAINT IF EXISTS app_users_role_check;
UPDATE app_users SET role = 'HEAD_DOCTOR' WHERE role = 'ADMIN';
ALTER TABLE app_users ADD COLUMN IF NOT EXISTS login_id VARCHAR(60);
ALTER TABLE app_users ALTER COLUMN password_hash DROP NOT NULL;
ALTER TABLE app_users ALTER COLUMN password_salt DROP NOT NULL;
ALTER TABLE app_users ADD CONSTRAINT app_users_role_check CHECK (role IN ('HEAD_DOCTOR', 'USER', 'RECEPTIONIST', 'PATIENT'));
CREATE UNIQUE INDEX IF NOT EXISTS app_users_login_id_unique ON app_users (LOWER(login_id)) WHERE login_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS patients (
    id BIGSERIAL PRIMARY KEY,
    owner_user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE RESTRICT,
    name VARCHAR(120) NOT NULL,
    age INTEGER NOT NULL CHECK (age BETWEEN 0 AND 120),
    gender VARCHAR(40) NOT NULL DEFAULT '',
    contact VARCHAR(30) NOT NULL,
    visit_type VARCHAR(30) NOT NULL DEFAULT 'First Visit',
    diagnosis TEXT NOT NULL DEFAULT '',
    status VARCHAR(30) NOT NULL DEFAULT 'Active',
    source VARCHAR(20) NOT NULL DEFAULT 'Offline',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS patients_owner_created_idx ON patients(owner_user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS patients_name_idx ON patients(name);
CREATE INDEX IF NOT EXISTS patients_contact_idx ON patients(contact);
