package com.medicarepro.ui;

import com.medicarepro.model.Role;
import com.medicarepro.model.User;
import com.medicarepro.service.AuthService;
import com.medicarepro.service.ClinicEvents;
import com.medicarepro.service.ClinicRepository;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Arrays;

public final class LoginFrame extends JFrame {
    private final AuthService authService; private final ClinicRepository repository; private final Runnable reopen;
    private final JComboBox<String> mode = new JComboBox<>(new String[]{"Doctor / staff password", "Receptionist · offline", "Patient · online"});
    private final JLabel nameLabel = new JLabel("Username"), idLabel = new JLabel("Password"), passwordLabel = new JLabel("Password");
    private final JTextField name = new JTextField(), identity = new JTextField(); private final JPasswordField password = new JPasswordField();
    private final JButton signIn = new JButton("Continue  →"); private final JPanel identityBlock = new JPanel(), passwordBlock = new JPanel();

    public LoginFrame(AuthService authService, ClinicRepository repository, Runnable reopen) {
        super("Sign in · MediCare Pro"); this.authService = authService; this.repository = repository; this.reopen = reopen;
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE); setSize(900, 570); setLocationRelativeTo(null); setContentPane(buildContent());
        mode.addActionListener(event -> updateMode()); signIn.addActionListener(event -> authenticate()); password.addActionListener(event -> authenticate()); identity.addActionListener(event -> authenticate()); updateMode();
    }

    private JComponent buildContent() {
        JPanel root = new JPanel(new BorderLayout()); root.setBackground(SwingTheme.PAPER);
        UiKit.GradientPanel hero = new UiKit.GradientPanel(new BorderLayout(), new Color(18, 61, 72), new Color(22, 132, 113)); hero.setPreferredSize(new Dimension(390, 0)); hero.setBorder(new EmptyBorder(48, 44, 42, 40));
        JPanel heroText = new JPanel(); heroText.setOpaque(false); heroText.setLayout(new BoxLayout(heroText, BoxLayout.Y_AXIS));
        JLabel logo = new JLabel("♥  MEDICARE PRO"); logo.setForeground(Color.WHITE); logo.setFont(new Font("SansSerif", Font.BOLD, 21));
        JLabel kicker = new JLabel("SMART CLINIC WORKSPACE"); kicker.setForeground(new Color(183, 239, 219)); kicker.setFont(new Font("SansSerif", Font.BOLD, 10)); kicker.setBorder(new EmptyBorder(10, 2, 0, 0));
        JLabel title = new JLabel("Care, coordinated."); title.setForeground(Color.WHITE); title.setFont(new Font("SansSerif", Font.BOLD, 31)); title.setBorder(new EmptyBorder(60, 0, 8, 0));
        JLabel copy = new JLabel("<html><div style='width:270px'>A calm, connected workspace for your clinic team and patients.</div></html>"); copy.setForeground(new Color(220, 244, 235)); copy.setFont(new Font("SansSerif", Font.PLAIN, 14)); heroText.add(logo); heroText.add(kicker); heroText.add(title); heroText.add(copy); hero.add(heroText, BorderLayout.NORTH);
        JLabel footer = new JLabel("SECURE TRAINING ENVIRONMENT"); footer.setForeground(new Color(183, 239, 219)); footer.setFont(new Font("Monospaced", Font.BOLD, 10)); hero.add(footer, BorderLayout.SOUTH); root.add(hero, BorderLayout.WEST);

        JPanel right = new JPanel(new GridBagLayout()); right.setBackground(new Color(239, 244, 249));
        UiKit.RoundedPanel card = UiKit.surface(new BorderLayout(), 22, Color.WHITE); card.setBorder(new EmptyBorder(34, 44, 34, 44)); card.setPreferredSize(new Dimension(470, 470));
        JPanel form = new JPanel(); form.setOpaque(false); form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        JLabel welcome = new JLabel("Welcome back"); welcome.setFont(new Font("SansSerif", Font.BOLD, 27)); welcome.setForeground(SwingTheme.INK); welcome.setAlignmentX(Component.LEFT_ALIGNMENT); form.add(welcome);
        JLabel subtitle = new JLabel("Choose your access route to continue."); subtitle.setForeground(SwingTheme.MUTED); subtitle.setAlignmentX(Component.LEFT_ALIGNMENT); form.add(subtitle); form.add(Box.createVerticalStrut(24));
        form.add(fieldBlock("ACCESS MODE", mode)); form.add(Box.createVerticalStrut(13)); form.add(fieldBlockLabel(nameLabel, name)); form.add(Box.createVerticalStrut(13));
        UiKit.input(identity); UiKit.input(password);
        identityBlock.setOpaque(false); identityBlock.setLayout(new BorderLayout(0, 6)); identityBlock.setPreferredSize(new Dimension(390, 68)); identityBlock.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68)); identityBlock.add(idLabel, BorderLayout.NORTH); identityBlock.add(identity, BorderLayout.CENTER); form.add(identityBlock); form.add(Box.createVerticalStrut(13));
        passwordBlock.setOpaque(false); passwordBlock.setLayout(new BorderLayout(0, 6)); passwordBlock.setPreferredSize(new Dimension(390, 68)); passwordBlock.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68)); passwordBlock.add(passwordLabel, BorderLayout.NORTH); passwordBlock.add(password, BorderLayout.CENTER); form.add(passwordBlock); form.add(Box.createVerticalStrut(22));
        signIn.setBackground(SwingTheme.ACCENT); signIn.setForeground(Color.WHITE); signIn.setFocusPainted(false); signIn.setBorder(new EmptyBorder(12, 18, 12, 18)); signIn.setAlignmentX(Component.LEFT_ALIGNMENT); form.add(signIn);
        JLabel hint = new JLabel("Patients and receptionists use name + ID, without a password."); hint.setForeground(SwingTheme.MUTED); hint.setFont(new Font("SansSerif", Font.PLAIN, 10)); hint.setAlignmentX(Component.LEFT_ALIGNMENT); form.add(Box.createVerticalStrut(14)); form.add(hint);
        card.add(form, BorderLayout.CENTER); right.add(card); root.add(right, BorderLayout.CENTER); return root;
    }

    private static JPanel fieldBlock(String caption, JComponent input) { return fieldBlockLabel(new JLabel(caption), input); }
    private static JPanel fieldBlockLabel(JLabel label, JComponent input) { JPanel block = new JPanel(new BorderLayout(0, 6)); block.setOpaque(false); block.setPreferredSize(new Dimension(390, 68)); block.setMaximumSize(new Dimension(Integer.MAX_VALUE, 68)); label.setForeground(new Color(91, 112, 135)); label.setFont(new Font("SansSerif", Font.BOLD, 10)); block.add(label, BorderLayout.NORTH); UiKit.input(input); input.setPreferredSize(new Dimension(390, 42)); input.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42)); block.add(input, BorderLayout.CENTER); return block; }

    private void updateMode() { boolean doctor = mode.getSelectedIndex() == 0; boolean patient = mode.getSelectedIndex() == 2; nameLabel.setText(doctor ? "Username" : "Full name"); idLabel.setText(patient ? "Patient ID (PID)" : "Receptionist ID"); Color labelColor = new Color(91, 112, 135); nameLabel.setForeground(labelColor); idLabel.setForeground(labelColor); passwordLabel.setForeground(labelColor); nameLabel.setFont(new Font("SansSerif", Font.BOLD, 10)); idLabel.setFont(new Font("SansSerif", Font.BOLD, 10)); passwordLabel.setFont(new Font("SansSerif", Font.BOLD, 10)); identityBlock.setVisible(!doctor); passwordBlock.setVisible(doctor); revalidate(); repaint(); }

    private void authenticate() {
        String enteredName = name.getText().trim(), enteredId = identity.getText().trim(); char[] enteredPassword = password.getPassword();
        if (enteredName.isBlank() || (mode.getSelectedIndex() == 0 ? enteredPassword.length == 0 : enteredId.isBlank())) { Arrays.fill(enteredPassword, '\0'); JOptionPane.showMessageDialog(this, "Complete the login details.", "Sign in", JOptionPane.WARNING_MESSAGE); return; }
        signIn.setEnabled(false); new SwingWorker<User, Void>() {
            @Override protected User doInBackground() { if (mode.getSelectedIndex() == 0) return authService.authenticate(enteredName, enteredPassword); if (mode.getSelectedIndex() == 1) return authService.authenticateByNameAndId(enteredName, enteredId, Role.RECEPTIONIST); return authService.registerOrAuthenticatePatient(enteredName, enteredId); }
            @Override protected void done() { Arrays.fill(enteredPassword, '\0'); password.setText(""); signIn.setEnabled(true); try { User user = get(); if (user == null) { JOptionPane.showMessageDialog(LoginFrame.this, "The name and ID do not match an active account.", "Sign in failed", JOptionPane.ERROR_MESSAGE); return; } dispose(); new MainFrame(user, repository, authService, new ClinicEvents(), reopen).setVisible(true); } catch (Exception error) { FormDialogs.showError(LoginFrame.this, error); } }
        }.execute();
    }
}
