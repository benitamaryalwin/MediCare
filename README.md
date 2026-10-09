# MediCare Pro — Java Swing Hospital Patient Information System

This is a Java 17 desktop application for Eclipse. It is **not a browser website**. The project uses Swing for the interface, JDBC for database access, and PostgreSQL for persistent records.

## Project layout (Maven modules)

The project is a multi-module Maven build. In Eclipse every module appears as its own project, so the Java files are grouped by responsibility:

| Module | Packages / contents | Depends on |
|---|---|---|
| `medicare-model` | `com.medicarepro.model` — records, enums (`Patient`, `User`, `Role`, …) | — |
| `medicare-security` | `com.medicarepro.security` — PBKDF2 password hashing | — |
| `medicare-data` | `com.medicarepro.data` — JDBC connection, config, `schema.sql` | PostgreSQL driver |
| `medicare-service` | `com.medicarepro.service` — `AuthService`, `ClinicRepository`, events, preferences | model, security, data |
| `medicare-ui` | `com.medicarepro.ui` — all Swing screens | model, service |
| `medicare-app` | `MediCareProApp` (the `main` class) and the runnable jar | all of the above |

`medicare-pro-swing` (the root `pom.xml`) is the parent that ties the modules together.

## Open and run in Eclipse

1. Install Java 17 or newer, Eclipse IDE for Java Developers, and PostgreSQL.
2. Extract `MediCare_Pro_Java.zip`.
3. In Eclipse choose **File → Import → Maven → Existing Maven Projects**.
4. Browse to the extracted `medicare-pro-swing` folder (the one containing the parent `pom.xml`). Eclipse lists the parent and the six `medicare-*` modules; keep them all ticked and click **Finish**.
5. Let Eclipse download Maven dependencies. If it does not, right-click a project and choose **Maven → Update Project** (tick all projects).
6. Configure the database connection in the run configuration (steps below).
7. Open `medicare-app/src/main/java/com/medicarepro/MediCareProApp.java` and select **Run As → Java Application**.

If the Package Explorer still shows one flat list, use the view menu (⋮) → **Project Presentation → Hierarchical**.

## PostgreSQL setup

Create an empty database first:

```sql
CREATE DATABASE medicare_pro;
```

The application creates its tables when it starts. The complete SQL is also provided at `database/schema.sql` and `medicare-data/src/main/resources/database/schema.sql`; to apply it manually:

```sh
psql -U postgres -d medicare_pro -f database/schema.sql
```

The default connection URL is:

```text
jdbc:postgresql://localhost:5432/medicare_pro
```

In Eclipse open **Run → Run Configurations → Java Application → Environment** and set:

| Variable | Example |
|---|---|
| `MEDICARE_DB_URL` | `jdbc:postgresql://localhost:5432/medicare_pro` |
| `MEDICARE_DB_USER` | Your PostgreSQL username, often `postgres` |
| `MEDICARE_DB_PASSWORD` | Your local PostgreSQL password |

The password is not saved in this project. If the variables are omitted, the app uses the URL above and the local PostgreSQL environment's configured user/password.

## First login and account privacy

- On the first launch, the setup screen creates the initial Head Doctor account.
- The Head Doctor signs in and creates separate user accounts in **User accounts**.
- Regular accounts only see and change records owned by that account.
- The Head Doctor can view/search records across accounts and manage user accounts.
- Passwords are stored as salted PBKDF2 hashes, not plain text.

This is a learning project. Do not enter real patient or other sensitive medical information.

## Working sections

- Dashboard and summary counts
- Patient create, view, search, update, delete, and CSV export
- Doctor directory, availability, create, update, delete, and CSV export
- Appointment booking and changes, appointment queue, and missed appointments
- Consultation start/complete, consultation history, and undo of the latest consultation
- Head Doctor user creation, password reset, and enable/disable controls
- Sign out and role-specific navigation

The appointment flows use database records; the buttons are not visual placeholders. Create a patient and an available doctor before booking an appointment.

## Java topics shown in the source

- Objects, records, enums, encapsulation, and validation
- Inheritance and polymorphism through reusable Swing record panels
- Interfaces and the observer/event pattern for updating related views
- A panel factory and table adapter pattern
- Collections including `ArrayList`, `HashMap`, `TreeSet`, and `ArrayDeque`
- Background work with `SwingWorker` so database actions do not freeze the Swing interface
- Exception handling, JDBC prepared statements, and transactional appointment creation
- File input/output for CSV export and filtered Java serialization for non-sensitive window-size settings

## Build and tests

From the root `medicare-pro-swing` folder:

```sh
mvn clean test package
```

This builds all modules in dependency order, runs the tests, and produces one runnable jar:

```sh
java -jar medicare-app/target/medicare-app-1.0.0.jar
```

The Maven project downloads the PostgreSQL JDBC driver and test libraries as needed. No database password or pre-populated medical data is bundled.

## Signing out and closing the window

- **Sign out** is available in two places: the bottom of the left sidebar and the top-right corner of the page, so it stays reachable even on small laptop screens.
- The window is sized to fit your screen (minus the taskbar) and remembers its size between sessions.
- The window's close (X) button asks whether to **Sign out**, **Exit application**, or **Cancel**.

## Fixes in this version

- Opening **Patients** (or clicking a patient card on the dashboard) threw a `NullPointerException`; fixed.
- Page titles and subtitles on Patients, Appointments, Doctors and User accounts are visible in the main workspace.
- The main window could be taller than the screen (1360×850 on a 1366×768 laptop), pushing **Sign out** off-screen; it now fits the screen and has a second Sign out button.
- The close button no longer traps you in a sign-out loop; it offers Sign out / Exit / Cancel.
- Each sign-in now gets its own event bus, so screens from a previous session no longer keep refreshing after sign-out.
- Saving a consultation now updates the exact patient on the appointment (it used to match by name, which broke when two patients shared a name) and records the appointment's Online/Offline source.
- The dashboard's "Upcoming appointments" list no longer shows completed or missed appointments.
