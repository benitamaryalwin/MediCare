# Application architecture

MediCare Pro is a small Java 17 Swing application with a clear four-part flow:

```text
Swing interface → authentication and repository services → database connection → PostgreSQL
```

## Main components

| Component | File | Responsibility |
| --- | --- | --- |
| Application flow | `MediCareProApp.java` | Starts Swing, initializes the database, and opens authentication screens |
| Authentication | `AuthService.java` | Handles accounts, password hashing, sign-in, and role checks |
| Data access | `ClinicRepository.java` | Validates and persists patient records and dashboard data |
| User interface | `ClinicScreens.java` | Displays navigation, dashboards, tables, forms, and role-based actions |

## Supporting components

- `Database.java` manages PostgreSQL connections and loads the schema.
- `PatientData.java` contains the records and role definitions.
- `UiSupport.java` contains reusable Swing forms, tables, styling, and refresh events.
- `schema.sql` defines the application tables, constraints, and indexes.

## Access rules

- The head doctor can view all patient records and create receptionist accounts.
- Receptionists register and manage offline patient records.
- Online patients can create their own record and access only their own records.

The code intentionally keeps the application small and uses straightforward JDBC and Swing so each layer is easy to explain and maintain.
