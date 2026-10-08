package com.medicarepro.ui;

import com.medicarepro.model.User;
import com.medicarepro.service.AuthService;
import com.medicarepro.service.ClinicEvents;
import com.medicarepro.service.ClinicRepository;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Arrays;

public final class LoginFrame extends JFrame {
    private final AuthService authService;
    private final ClinicRepository repository;
    private final Runnable reopen;
    private final JTextField username = new JTextField(22);
    private final JPasswordField password = new JPasswordField(22);
    private final JButton signIn = new JButton("Sign in");

    public LoginFrame(AuthService authService, ClinicRepository repository, Runnable reopen) {
        super("Sign in · MediCare Pro");
        this.authService = authService;
        this.repository = repository;
        this.reopen = reopen;
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(475, 390);
        setLocationRelativeTo(null);
        setContentPane(buildContent());
        signIn.addActionListener(event -> authenticate());
        password.addActionListener(event -> authenticate());
    }

    private JComponent buildContent() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(SwingTheme.PAPER);
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SwingTheme.LINE),
                new EmptyBorder(28, 34, 28, 34)));
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
        c.insets = new Insets(0, 0, 20, 0);
        JLabel subtitle = new JLabel("Sign in to your private clinic workspace.");
        subtitle.setForeground(SwingTheme.MUTED);
        card.add(subtitle, c);
        c.gridwidth = 1;
        c.gridy++;
        c.insets = new Insets(8, 0, 8, 12);
        card.add(new JLabel("Username"), c);
        c.gridx = 1;
        c.insets = new Insets(8, 0, 8, 0);
        card.add(username, c);
        c.gridx = 0;
        c.gridy++;
        c.insets = new Insets(8, 0, 8, 12);
        card.add(new JLabel("Password"), c);
        c.gridx = 1;
        c.insets = new Insets(8, 0, 8, 0);
        card.add(password, c);
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        c.insets = new Insets(17, 0, 0, 0);
        signIn.setBackground(SwingTheme.ACCENT);
        signIn.setForeground(Color.WHITE);
        signIn.setPreferredSize(new Dimension(180, 39));
        card.add(signIn, c);
        c.gridy++;
        c.insets = new Insets(17, 0, 0, 0);
        JLabel note = new JLabel("Do not enter real patient or health information.");
        note.setForeground(SwingTheme.MUTED);
        note.setFont(new Font("SansSerif", Font.PLAIN, 10));
        card.add(note, c);
        outer.add(card);
        return outer;
    }

    private void authenticate() {
        char[] enteredPassword = password.getPassword();
        String enteredUsername = username.getText().trim();
        if (enteredUsername.isBlank() || enteredPassword.length == 0) {
            Arrays.fill(enteredPassword, '\0');
            JOptionPane.showMessageDialog(this, "Enter your username and password.",
                    "Sign in", JOptionPane.WARNING_MESSAGE);
            return;
        }
        signIn.setEnabled(false);
        new SwingWorker<User, Void>() {
            @Override protected User doInBackground() {
                return authService.authenticate(enteredUsername, enteredPassword);
            }
            @Override protected void done() {
                Arrays.fill(enteredPassword, '\0');
                password.setText("");
                signIn.setEnabled(true);
                try {
                    User user = get();
                    if (user == null) {
                        JOptionPane.showMessageDialog(LoginFrame.this,
                                "The username or password is incorrect, or the account is disabled.",
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
