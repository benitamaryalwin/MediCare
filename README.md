# MediCare Pro

Small Java 17 Swing clinic application using an embedded SQLite database.

## Run in VS Code

1. Install the Extension Pack for Java.
2. Open this folder in VS Code.
3. Run `mvn clean package` from the terminal, or use **Run and Debug** for `com.medicarepro.App`.
4. Start the application from the project root. It creates `medicare.db` automatically from `database/schema.sql`.

No PostgreSQL server, username, password, or environment variables are required.

## Features

- First-run Head Doctor account creation.
- Separate Head Doctor and user logins.
- Users can add, edit, update, search, and delete their own patient records.
- Head Doctor can search and manage all patient records and create user accounts.
- Logout returns to the login screen.
- Invoice and unused legacy modules have been removed.

The application stores passwords as salted PBKDF2 hashes. Do not enter real medical information into this learning project.

## Database

`database/schema.sql` is the only SQL file. The SQLite database file `medicare.db` is generated locally and is ignored by Git.
