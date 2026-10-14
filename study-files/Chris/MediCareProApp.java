package com.medicarepro;

import java.awt.*;
import java.util.Arrays;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

final class MediCareProApp {
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

/** Sign-in screen for administrators, receptionists, and patients. */
final class LoginFrame extends JFrame {
    private final AuthService authService;
    private final ClinicRepository repository;
    private final Runnable reopen;
    private final JComboBox<String> mode = new JComboBox<>(new String[]{
            "Administrator", "Receptionist · offline", "Patient · online"});
    private final JLabel nameLabel = new JLabel("Username");
    private final JLabel idLabel = new JLabel("Password");
    private final JLabel passwordLabel = new JLabel("Password");
    private final JTextField name = new JTextField(22);
    private final JTextField identity = new JTextField(22);
    private final JPasswordField password = new JPasswordField(22);
    private final JButton signIn = new JButton("Sign in");

    public LoginFrame(AuthService authService, ClinicRepository repository, Runnable reopen) {
        super("Sign in · MediCare Pro");
        this.authService = authService;
        this.repository = repository;
        this.reopen = reopen;
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(520, 440);
        setLocationRelativeTo(null);
        setContentPane(buildContent());
        mode.addActionListener(event -> updateMode());
        signIn.addActionListener(event -> authenticate());
        password.addActionListener(event -> authenticate());
        identity.addActionListener(event -> authenticate());
        updateMode();
    }

    private JComponent buildContent() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(SwingTheme.PAPER);
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SwingTheme.LINE), new EmptyBorder(26, 34, 26, 34)));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 5, 0);
        JLabel brand = new JLabel("♥  MediCare Pro");
        brand.setFont(new Font("SansSerif", Font.BOLD, 21));
        brand.setForeground(SwingTheme.ACCENT);
        card.add(brand, c);

        c.gridy++;
        c.insets = new Insets(0, 0, 15, 0);
        JLabel subtitle = new JLabel("Choose how you are entering the clinic workspace.");
        subtitle.setForeground(SwingTheme.MUTED);
        card.add(subtitle, c);

        c.gridwidth = 1;
        c.gridy++;
        addRow(card, c, "Login type", mode);
        c.gridy++;
        addRow(card, c, nameLabel, name);
        c.gridy++;
        addRow(card, c, idLabel, identity);
        c.gridy++;
        addRow(card, c, passwordLabel, password);

        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        c.insets = new Insets(17, 0, 0, 0);
        signIn.setBackground(SwingTheme.ACCENT);
        signIn.setForeground(Color.WHITE);
        signIn.setPreferredSize(new Dimension(190, 39));
        card.add(signIn, c);

        c.gridy++;
        c.insets = new Insets(15, 0, 0, 0);
        JLabel note = new JLabel("Receptionists and patients sign in with their name and ID.");
        note.setForeground(SwingTheme.MUTED);
        note.setFont(new Font("SansSerif", Font.PLAIN, 10));
        card.add(note, c);
        outer.add(card);
        return outer;
    }

    private static void addRow(JPanel card, GridBagConstraints c, String label, JComponent field) {
        c.gridx = 0;
        c.insets = new Insets(7, 0, 7, 12);
        card.add(new JLabel(label), c);
        c.gridx = 1;
        c.insets = new Insets(7, 0, 7, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        card.add(field, c);
        c.fill = GridBagConstraints.NONE;
    }

    private static void addRow(JPanel card, GridBagConstraints c, JLabel label, JComponent field) {
        c.gridx = 0;
        c.insets = new Insets(7, 0, 7, 12);
        card.add(label, c);
        c.gridx = 1;
        c.insets = new Insets(7, 0, 7, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        card.add(field, c);
        c.fill = GridBagConstraints.NONE;
    }

    private void updateMode() {
        boolean administrator = mode.getSelectedIndex() == 0;
        boolean patient = mode.getSelectedIndex() == 2;
        nameLabel.setText(administrator ? "Username" : "Full name");
        idLabel.setText(administrator ? "Password" : (patient ? "Patient ID (PID)" : "Receptionist ID"));
        identity.setVisible(!administrator);
        idLabel.setVisible(!administrator);
        password.setVisible(administrator);
        passwordLabel.setVisible(administrator);
        revalidate();
        repaint();
    }

    private void authenticate() {
        int selectedMode = mode.getSelectedIndex();
        String enteredName = name.getText().trim();
        String enteredId = identity.getText().trim();
        char[] enteredPassword = password.getPassword();
        boolean missingDetails = enteredName.isBlank()
                || (selectedMode == 0 ? enteredPassword.length == 0 : enteredId.isBlank());
        if (missingDetails) {
            Arrays.fill(enteredPassword, '\0');
            JOptionPane.showMessageDialog(this, "Complete the login details.",
                    "Sign in", JOptionPane.WARNING_MESSAGE);
            return;
        }

        signIn.setEnabled(false);
        mode.setEnabled(false);
        new SwingWorker<User, Void>() {
            @Override protected User doInBackground() {
                if (selectedMode == 0) return authService.authenticate(enteredName, enteredPassword);
                if (selectedMode == 1) {
                    return authService.authenticateByNameAndId(enteredName, enteredId, Role.RECEPTIONIST);
                }
                return authService.registerOrAuthenticatePatient(enteredName, enteredId);
            }

            @Override protected void done() {
                Arrays.fill(enteredPassword, '\0');
                password.setText("");
                signIn.setEnabled(true);
                mode.setEnabled(true);
                try {
                    User user = get();
                    if (user == null) {
                        JOptionPane.showMessageDialog(LoginFrame.this,
                                "The name and ID do not match an active account.",
                                "Sign in failed", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    dispose();
                    new MainFrame(user, repository, authService, new ClinicEvents(), reopen).setVisible(true);
                } catch (Exception error) {
                    FormDialogs.showError(LoginFrame.this, error);
                }
            }
        }.execute();
    }
}

final class SetupAdminFrame extends JFrame {
    private final AuthService authService;
    private final Runnable continueToLogin;
    private final JTextField displayName = new JTextField(22);
    private final JTextField username = new JTextField(22);
    private final JPasswordField password = new JPasswordField(22);
    private final JPasswordField confirm = new JPasswordField(22);
    private final JButton createButton = new JButton("Create administrator");

    public SetupAdminFrame(AuthService authService, Runnable continueToLogin) {
        super("Administrator setup · MediCare Pro");
        this.authService = authService;
        this.continueToLogin = continueToLogin;
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(535, 470);
        setLocationRelativeTo(null);
        setContentPane(buildContent());
        createButton.addActionListener(event -> createAdmin());
    }

    private JComponent buildContent() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(SwingTheme.PAPER);
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SwingTheme.LINE),
                new EmptyBorder(26, 32, 26, 32)));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 7, 0);
        JLabel title = new JLabel("Create the administrator login");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        card.add(title, c);
        c.gridy++;
        c.insets = new Insets(0, 0, 16, 0);
        JLabel note = new JLabel("This account can create users and manage every patient's records.");
        note.setForeground(SwingTheme.MUTED);
        card.add(note, c);
        c.gridwidth = 1;
        addRow(card, c, "Display name", displayName);
        addRow(card, c, "Username", username);
        addRow(card, c, "Password (10+ characters)", password);
        addRow(card, c, "Confirm password", confirm);
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        c.insets = new Insets(15, 0, 0, 0);
        createButton.setBackground(SwingTheme.ACCENT);
        createButton.setForeground(Color.WHITE);
        card.add(createButton, c);
        outer.add(card);
        return outer;
    }

    private static void addRow(JPanel card, GridBagConstraints c, String label, JComponent input) {
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 1;
        c.insets = new Insets(7, 0, 7, 12);
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        card.add(new JLabel(label), c);
        c.gridx = 1;
        c.insets = new Insets(7, 0, 7, 0);
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        card.add(input, c);
    }

    private void createAdmin() {
        char[] first = password.getPassword();
        char[] second = confirm.getPassword();
        String enteredUsername = username.getText();
        String enteredDisplayName = displayName.getText();
        if (!Arrays.equals(first, second)) {
            Arrays.fill(first, '\0');
            Arrays.fill(second, '\0');
            JOptionPane.showMessageDialog(this, "The passwords do not match.",
                    "First-time setup", JOptionPane.WARNING_MESSAGE);
            return;
        }
        createButton.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() {
                authService.createInitialAdmin(enteredUsername, enteredDisplayName, first);
                return null;
            }
            @Override protected void done() {
                Arrays.fill(first, '\0');
                Arrays.fill(second, '\0');
                createButton.setEnabled(true);
                try {
                    get();
                    JOptionPane.showMessageDialog(SetupAdminFrame.this,
                            "Administrator account created. Sign in with the new account.",
                            "Setup complete", JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                    continueToLogin.run();
                } catch (Exception error) {
                    FormDialogs.showError(SetupAdminFrame.this, error);
                }
            }
        }.execute();
    }
}
