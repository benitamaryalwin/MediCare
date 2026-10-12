package com.medicarepro;


import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Arrays;

/** Sign-in screen for administrators, receptionists, and patients. */
public final class LoginFrame extends JFrame {
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
