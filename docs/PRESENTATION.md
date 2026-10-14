# MediCare Pro presentation notes

## One-minute overview

MediCare Pro is a Java 17 Swing patient-information system backed by PostgreSQL. The application separates the user interface, business operations, database access, and data records so each responsibility is easy to explain and maintain.

## What to point out

### User experience

- The startup screen checks the database and initializes the schema.
- The first run guides the user through administrator setup.
- The dashboard gives staff a quick view of patient totals and recent registrations.
- Shared form and table helpers keep the screens consistent.

### Security and roles

- Administrator accounts use a username and password.
- Receptionists use a name plus their receptionist ID. Online patients use a name plus their patient ID.
- Passwords are stored as salted hashes, never as plain text.
- Repository queries scope patient records according to the signed-in user.

### Technical design

| Area | File | Responsibility |
| --- | --- | --- |
| Application startup | `MediCareProApp.java` | Starts Swing, checks the database, and opens login/setup |
| User interface | `ClinicScreens.java` | Dashboard, patients, users, and navigation |
| UI support | `UiSupport.java` | Forms, tables, theme, and refresh events |
| Authentication | `AuthService.java` | Login, account creation, and password hashing |
| Patient data | `ClinicRepository.java` | Validated CRUD queries and dashboard statistics |
| Database | `Database.java` | Connection configuration and schema initialization |
| Data model | `PatientData.java` | Records and role definitions |

## Suggested live demo

Use a prepared PostgreSQL database and sample values. Demonstrate one complete path rather than entering many records:

1. Create an administrator.
2. Open the dashboard.
3. Create a receptionist from the head doctor account.
4. Sign in as receptionist and add one offline patient with a diagnosis, visit type, and status.
5. Search for that patient and update the status.
6. Return to the dashboard to show refreshed totals.
7. Sign out and explain the separate patient/receptionist login fields.

## Important limitation

This is an academic desktop application, not a production medical-record system. It should be demonstrated with fictional records only and would need additional operational, audit, deployment, and compliance controls before real clinical use.
