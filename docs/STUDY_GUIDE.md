# Four-part Java study guide

For the presentation, study the project in these four parts. The supporting files are small and only need a short explanation.

## 1. Application flow — `MediCareProApp.java`

Explain that this is the entry point. It starts Swing, connects to PostgreSQL, initializes the schema, opens first-time administrator setup, and then opens the login screen.

## 2. Authentication and roles — `AuthService.java`

Explain that this file handles administrator passwords, receptionist login, online patient self-registration, account creation, and role checks. Passwords are salted and hashed before storage.

## 3. Database operations — `Database.java` and `ClinicRepository.java`

Explain that `Database.java` creates PostgreSQL connections and loads `schema.sql`. `ClinicRepository.java` performs patient create, search, update, delete, and dashboard queries.

The important access rule is in the repository query scope:

- Head doctor: sees all records.
- Receptionist: sees and manages offline records.
- Online patient: sees and changes only records belonging to that patient.

## 4. User interface — `ClinicScreens.java`

Explain that this file contains the dashboard, patient table, account screen, navigation, and role-specific buttons. The UI calls the service/repository classes instead of writing SQL directly.

## Supporting files

- `PatientData.java`: simple records for patients, users, roles, and dashboard totals.
- `UiSupport.java`: reusable forms, tables, styling, and refresh events.
- `src/main/resources/schema.sql`: PostgreSQL tables, constraints, and indexes.
- `src/test/java/com/medicarepro/PasswordHasherTest.java`: password hashing tests.

## One-minute explanation

“The application starts in `MediCareProApp`. `AuthService` authenticates users and applies roles. `ClinicRepository` performs validated database operations. `ClinicScreens` presents the data in Swing. `Database` provides the PostgreSQL connection, while the records and UI helpers keep the code organized.”
