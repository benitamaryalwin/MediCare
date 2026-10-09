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
    doctor VARCHAR(120) NOT NULL DEFAULT '',
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

CREATE TABLE IF NOT EXISTS doctors (
    id BIGSERIAL PRIMARY KEY,
    owner_user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE RESTRICT,
    name VARCHAR(120) NOT NULL,
    specialization VARCHAR(120) NOT NULL,
    qualifications VARCHAR(180) NOT NULL DEFAULT '',
    experience_years INTEGER NOT NULL DEFAULT 0 CHECK (experience_years BETWEEN 0 AND 80),
    phone VARCHAR(30) NOT NULL DEFAULT '',
    status VARCHAR(30) NOT NULL DEFAULT 'Available',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS doctors_owner_name_idx ON doctors(owner_user_id, name);
INSERT INTO doctors (owner_user_id, name, specialization, qualifications, experience_years, phone, status)
SELECT u.id, 'Dr. Ananya Rao', 'General Medicine', 'MBBS, MD', 12, '90000 10001', 'Available'
FROM app_users u
WHERE u.role = 'HEAD_DOCTOR'
  AND NOT EXISTS (SELECT 1 FROM doctors WHERE name = 'Dr. Ananya Rao');
INSERT INTO doctors (owner_user_id, name, specialization, qualifications, experience_years, phone, status)
SELECT u.id, 'Dr. Vikram Shah', 'Cardiology', 'MBBS, DM', 15, '90000 10002', 'Available'
FROM app_users u
WHERE u.role = 'HEAD_DOCTOR'
  AND NOT EXISTS (SELECT 1 FROM doctors WHERE name = 'Dr. Vikram Shah');
INSERT INTO doctors (owner_user_id, name, specialization, qualifications, experience_years, phone, status)
SELECT u.id, 'Dr. Meera Nair', 'Pediatrics', 'MBBS, DCH', 9, '90000 10003', 'Available'
FROM app_users u
WHERE u.role = 'HEAD_DOCTOR'
  AND NOT EXISTS (SELECT 1 FROM doctors WHERE name = 'Dr. Meera Nair');

CREATE TABLE IF NOT EXISTS appointments (
    id BIGSERIAL PRIMARY KEY,
    owner_user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE RESTRICT,
    patient_id BIGINT REFERENCES patients(id) ON DELETE SET NULL,
    doctor_id BIGINT REFERENCES doctors(id) ON DELETE SET NULL,
    patient_name VARCHAR(120) NOT NULL,
    doctor_name VARCHAR(120) NOT NULL,
    appointment_at TIMESTAMPTZ NOT NULL,
    visit_mode VARCHAR(20) NOT NULL DEFAULT 'Offline',
    status VARCHAR(32) NOT NULL DEFAULT 'Scheduled',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS patient_id BIGINT REFERENCES patients(id) ON DELETE SET NULL;
ALTER TABLE appointments ADD COLUMN IF NOT EXISTS doctor_id BIGINT REFERENCES doctors(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS appointments_owner_date_idx ON appointments(owner_user_id, appointment_at);
CREATE INDEX IF NOT EXISTS appointments_owner_status_idx ON appointments(owner_user_id, status);

CREATE TABLE IF NOT EXISTS consultations (
    id BIGSERIAL PRIMARY KEY,
    owner_user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE RESTRICT,
    appointment_id BIGINT REFERENCES appointments(id) ON DELETE SET NULL,
    patient_name VARCHAR(120) NOT NULL,
    doctor_name VARCHAR(120) NOT NULL,
    age INTEGER NOT NULL DEFAULT 0 CHECK (age BETWEEN 0 AND 120),
    gender VARCHAR(40) NOT NULL DEFAULT '',
    complaints TEXT NOT NULL DEFAULT '',
    diagnosis TEXT NOT NULL DEFAULT '',
    medication TEXT NOT NULL DEFAULT '',
    follow_up_date DATE,
    notes TEXT NOT NULL DEFAULT '',
    source VARCHAR(20) NOT NULL DEFAULT 'Offline',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS consultations_owner_created_idx ON consultations(owner_user_id, created_at DESC);
ALTER TABLE appointments DROP COLUMN IF EXISTS billed;
DROP TABLE IF EXISTS invoices;

