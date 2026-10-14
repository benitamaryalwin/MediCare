# MediCare Pro

MediCare Pro is a Java 17 desktop application for managing clinic patient information. It uses Swing for the user interface, JDBC for persistence, and PostgreSQL as its database.

## Features

- First-run administrator setup.
- Administrator, receptionist, and patient sign-in flows.
- Role-based access to patient records.
- Patient create, search, view, update, and delete operations.
- Receptionist account management by the head doctor.
- Dashboard totals and recent registrations.
- Password hashing with per-password salts.

## Project structure

This is intentionally a small single-module Maven application. The source is grouped by responsibility without changing the existing public behavior:

```text
src/
├── main/
│   ├── java/com/medicarepro/
│   │   ├── MediCareProApp.java   # Startup, login, and first-run setup
│   │   ├── ClinicScreens.java    # Dashboard and record-management screens
│   │   ├── UiSupport.java        # Shared Swing forms, tables, and theme
│   │   ├── AuthService.java      # Account and password operations
│   │   ├── ClinicRepository.java # Patient queries and persistence
│   │   ├── Database.java         # PostgreSQL connection and schema setup
│   │   └── PatientData.java      # Records, roles, and application data
│   └── resources/schema.sql      # Database tables and indexes
└── test/java/com/medicarepro/    # Automated tests
```

The application follows a simple flow:

```text
Swing screens → services/repositories → Database → PostgreSQL
                     ↑
             records and roles
```

## Run locally

1. Install Java 17, Maven, and PostgreSQL.
2. Create a database, for example `medicare_pro`.
3. Set these environment variables:

   ```text
   MEDICARE_DB_URL=jdbc:postgresql://localhost:5432/medicare_pro
   MEDICARE_DB_USER=postgres
   MEDICARE_DB_PASSWORD=your-password
   ```

4. Build and test:

   ```sh
   mvn clean test
   mvn package
   ```

5. Start the packaged application:

   ```sh
   java -jar target/medicare-pro-swing-1.0.0.jar
   ```

The application creates or updates its tables from `src/main/resources/schema.sql` during startup. The first launch opens administrator setup; later launches open the sign-in screen.

## Presentation walkthrough

Use sample data only. A clear demo sequence is:

1. Explain the startup and automatic database initialization.
2. Create the first administrator account.
3. Sign in as administrator and show the dashboard.
4. Create a receptionist account and demonstrate role-based access.
5. Sign in as receptionist and register an offline patient.
6. Add, search, edit, and delete a sample patient record.
7. Show the dashboard totals and recent registrations.
8. Sign out and demonstrate the patient sign-in flow.

More detailed talking points are in [`docs/PRESENTATION.md`](docs/PRESENTATION.md).

## Notes

This is a learning project. Do not enter real patient information. PostgreSQL must be running before the application starts.
