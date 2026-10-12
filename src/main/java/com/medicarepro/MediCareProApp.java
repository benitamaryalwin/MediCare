package com.medicarepro;


import javax.swing.*;
import java.awt.*;

public final class MediCareProApp {
    private MediCareProApp() { }

    public static void main(String[] args) {
        SwingTheme.install();
        SwingUtilities.invokeLater(() -> new StartupFrame());
    }

    private static final class StartupFrame extends JFrame {
        private final JLabel status = new JLabel("Connecting to PostgreSQL…", SwingConstants.CENTER);
        private final Database database = new Database(DatabaseConfig.fromEnvironment());
        private final AuthService authService = new AuthService(database);
        private final ClinicRepository repository = new ClinicRepository(database);

        private StartupFrame() {
            super("MediCare Pro");
            setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            setSize(420, 145);
            setLocationRelativeTo(null);
            status.setFont(new Font("SansSerif", Font.PLAIN, 14));
            add(status, BorderLayout.CENTER);
            setVisible(true);
            new SwingWorker<Boolean, Void>() {
                @Override protected Boolean doInBackground() throws Exception {
                    database.checkConnection();
                    database.initializeSchema();
                    return authService.hasAdminAccount();
                }
                @Override protected void done() {
                    try {
                        boolean hasAdmin = get();
                        dispose();
                        if (hasAdmin) {
                            openLogin();
                        } else {
                            new SetupAdminFrame(authService, StartupFrame.this::openLogin).setVisible(true);
                        }
                    } catch (Exception error) {
                        status.setText("Could not connect to the database.");
                        showDatabaseError(error);
                        dispose();
                    }
                }
            }.execute();
        }

        private void openLogin() {
            new LoginFrame(authService, repository, this::openLogin).setVisible(true);
        }

        private void showDatabaseError(Throwable error) {
            Throwable cause = error;
            while (cause.getCause() != null && cause.getCause() != cause) cause = cause.getCause();
            String details = cause.getMessage() == null ? "Unknown database connection error." : cause.getMessage();
            String message = """
                    PostgreSQL could not be reached.

                    Start PostgreSQL, create the medicare_pro database, and set MEDICARE_DB_URL,
                    MEDICARE_DB_USER, and MEDICARE_DB_PASSWORD if your local settings differ.
                    The complete SQL and Eclipse instructions are in README.md.

                    Details: %s
                    """.formatted(details);
            Object[] options = {"Retry", "Exit"};
            int choice = JOptionPane.showOptionDialog(null, message, "Database connection",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE, null, options, options[0]);
            if (choice == 0) new StartupFrame();
            else System.exit(1);
        }
    }
}
