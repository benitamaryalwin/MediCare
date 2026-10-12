# MediCare Pro

A small Java 17 Swing application for managing hospital patient information. It uses JDBC and PostgreSQL to save records.

## What it does

- Receptionists register patients in person.
- Patients use the online login option to access their records.
- Staff can add, search, view, update, and delete patient records.
- The administrator can create receptionist accounts.
- The dashboard shows patient totals and recent registrations.

## Run

Open this folder in VS Code or Eclipse as a Maven project. Set `MEDICARE_DB_URL`, `MEDICARE_DB_USER`, and `MEDICARE_DB_PASSWORD` for your PostgreSQL database, then run:

```sh
mvn -DskipTests package
java -jar target/medicare-pro-swing-1.0.0.jar
```

The database schema is in `src/main/resources/schema.sql`.

## Code layout

- `src/main/java/com/medicarepro` contains the app, Swing screens, database code, and data classes.
- `src/main/resources/schema.sql` creates the required database tables.

The Java files share one package to keep the folder structure simple. Each class still has one clear job, so the flow is easy to follow from the screen to the database.

This is a learning project. Use sample data only; do not enter real patient information.
