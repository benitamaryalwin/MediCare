package com.medicarepro;


import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Arrays;

public final class SetupAdminFrame extends JFrame {
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
