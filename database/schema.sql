CREATE TABLE IF NOT EXISTS app_users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(60) NOT NULL UNIQUE,
    display_name VARCHAR(120) NOT NULL,
    password_hash BYTEA NOT NULL,
    password_salt BYTEA NOT NULL,
    role VARCHAR(12) NOT NULL CHECK (role IN ('ADMIN', 'USER')),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

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
    billed BOOLEAN NOT NULL DEFAULT FALSE,
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

CREATE TABLE IF NOT EXISTS invoices (
    id BIGSERIAL PRIMARY KEY,
    owner_user_id BIGINT NOT NULL REFERENCES app_users(id) ON DELETE RESTRICT,
    appointment_id BIGINT REFERENCES appointments(id) ON DELETE SET NULL,
    patient_name VARCHAR(120) NOT NULL,
    doctor_name VARCHAR(120) NOT NULL,
    consultation_fee NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (consultation_fee >= 0),
    medication_amount NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (medication_amount >= 0),
    other_amount NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (other_amount >= 0),
    payment_status VARCHAR(12) NOT NULL DEFAULT 'UNPAID' CHECK (payment_status IN ('PAID', 'UNPAID')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS invoices_owner_created_idx ON invoices(owner_user_id, created_at DESC);
