package com.medicarepro.ui;

import com.medicarepro.model.User;
import com.medicarepro.model.WindowPreferences;
import com.medicarepro.service.AuthService;
import com.medicarepro.service.ClinicEvents;
import com.medicarepro.service.ClinicRepository;
import com.medicarepro.service.PreferencesStore;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public final class MainFrame extends JFrame {
    private static final Color NAVY = new Color(18, 49, 91);
    private static final Color NAVY_DARK = new Color(13, 39, 74);
    private static final Color TEAL = new Color(39, 174, 182);
    private final User user;
    private final ClinicPanelFactory panels;
    private final JPanel content = new JPanel(new CardLayout());
    private final Map<String, JComponent> pageByKey = new LinkedHashMap<>();
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();
    private final JLabel sectionTitle = new JLabel("Clinic overview");
    private final Runnable signOut;
    private final PreferencesStore preferencesStore = new PreferencesStore();

    public MainFrame(User user, ClinicRepository repository, AuthService authService, ClinicEvents events, Runnable signOut) {
        super("MediCare Pro · Hospital Patient Information System");
        this.user = user; this.panels = new ClinicPanelFactory(repository, authService, events); this.signOut = signOut;
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        WindowPreferences preferences;
        try { preferences = preferencesStore.load(); } catch (java.io.IOException error) { preferences = WindowPreferences.defaults(); }
        fitToScreen(preferences); setLayout(new BorderLayout()); add(header(), BorderLayout.NORTH); add(workspace(), BorderLayout.CENTER); showSection("dashboard");
        addWindowListener(new java.awt.event.WindowAdapter() { @Override public void windowClosing(java.awt.event.WindowEvent event) { confirmClose(); } });
    }

    private JComponent header() {
        JPanel shell = new JPanel(new BorderLayout());
        JPanel top = new JPanel(new BorderLayout(18, 0)); top.setBackground(NAVY); top.setBorder(BorderFactory.createEmptyBorder(16, 28, 13, 28));
        JLabel brand = new JLabel("♥  MediCare Pro"); brand.setForeground(Color.WHITE); brand.setFont(new Font("SansSerif", Font.BOLD, 22));
        JLabel edition = new JLabel("Clinic management system  ·  SDP Project 2026"); edition.setForeground(new Color(180, 207, 224)); edition.setFont(new Font("SansSerif", Font.PLAIN, 11));
        JPanel brandBox = new JPanel(new GridLayout(2, 1, 0, 3)); brandBox.setOpaque(false); brandBox.add(brand); brandBox.add(edition); top.add(brandBox, BorderLayout.WEST);
        JPanel account = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0)); account.setOpaque(false);
        JLabel identity = new JLabel("<html><b>" + escape(user.displayName()) + "</b><br><span style='color:#b4cde0'>" + roleLabel() + "</span></html>"); identity.setForeground(Color.WHITE); identity.setHorizontalAlignment(SwingConstants.RIGHT);
        JButton logout = UiKit.button("Logout", false); logout.setBackground(NAVY); logout.setForeground(Color.WHITE); logout.setPreferredSize(new Dimension(78, 32)); logout.setBorder(BorderFactory.createLineBorder(new Color(101, 145, 180))); logout.addActionListener(event -> logout());
        account.add(identity); account.add(logout); top.add(account, BorderLayout.EAST);
        shell.add(top, BorderLayout.NORTH);
        JPanel nav = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); nav.setBackground(NAVY_DARK); nav.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
        addNavigation(nav, "dashboard", "⌂  Overview"); addNavigation(nav, "patients", "♟  Patients"); addNavigation(nav, "appointments", "▣  Appointments"); addNavigation(nav, "doctors", "✚  Doctors"); if (user.isAdmin()) addNavigation(nav, "accounts", "⚙  User accounts");
        shell.add(nav, BorderLayout.SOUTH); return shell;
    }

    private String roleLabel() { return user.isHeadDoctor() ? "Head Doctor" : user.isReceptionist() ? "Receptionist · Offline" : user.isPatient() ? "Patient · Online" : "Clinic User"; }

    private void addNavigation(JPanel navigation, String key, String label) {
        JButton button = new JButton(label); button.setFocusPainted(false); button.setForeground(new Color(193, 215, 230)); button.setBackground(NAVY_DARK); button.setBorder(BorderFactory.createEmptyBorder(13, 18, 12, 18)); button.setFont(new Font("SansSerif", Font.BOLD, 12)); button.addActionListener(event -> showSection(key)); navigation.add(button); navButtons.put(key, button);
    }

    private JComponent workspace() {
        JPanel workspace = new JPanel(new BorderLayout(0, 0)); workspace.setBackground(new Color(239, 244, 249));
        JPanel heading = new JPanel(new BorderLayout()); heading.setOpaque(false); heading.setBorder(BorderFactory.createEmptyBorder(20, 30, 8, 30));
        JLabel crumb = new JLabel("WORKSPACE  /  "); crumb.setForeground(new Color(120, 140, 158)); crumb.setFont(new Font("SansSerif", Font.BOLD, 10));
        sectionTitle.setForeground(NAVY); sectionTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); left.setOpaque(false); left.add(crumb); left.add(sectionTitle); heading.add(left, BorderLayout.WEST);
        JLabel safety = new JLabel("DEMO DATA ONLY"); safety.setForeground(new Color(39, 128, 140)); safety.setFont(new Font("SansSerif", Font.BOLD, 10)); heading.add(safety, BorderLayout.EAST);
        workspace.add(heading, BorderLayout.NORTH); content.setOpaque(false); workspace.add(content, BorderLayout.CENTER); return workspace;
    }

    private void showSection(String key) {
        if ("accounts".equals(key) && !user.isAdmin()) { JOptionPane.showMessageDialog(this, "Only the Head Doctor can manage user accounts.", "Access restricted", JOptionPane.WARNING_MESSAGE); return; }
        JComponent panel = pageByKey.computeIfAbsent(key, section -> panels.create(section, user, this::showSection));
        sectionTitle.setText(switch (key) { case "dashboard" -> "Clinic overview"; case "patients" -> "Patient records"; case "appointments" -> "Appointments"; case "doctors" -> "Doctor directory"; case "accounts" -> "User accounts"; default -> "Workspace"; });
        if (panel.getParent() == null) content.add(panel, key); ((CardLayout) content.getLayout()).show(content, key);
        if (panel instanceof Refreshable refreshable) refreshable.refresh();
        navButtons.forEach((navKey, button) -> { boolean active = navKey.equals(key); button.setBackground(active ? new Color(29, 105, 124) : NAVY_DARK); button.setForeground(active ? Color.WHITE : new Color(193, 215, 230)); });
    }

    private void logout() { if (JOptionPane.showConfirmDialog(this, "Sign out of MediCare Pro?", "Sign out", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) endSession(true); }
    private void confirmClose() { Object[] options = {"Sign out", "Exit application", "Cancel"}; int choice = JOptionPane.showOptionDialog(this, "What would you like to do?", "Close MediCare Pro", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]); if (choice == 0) endSession(true); else if (choice == 1) endSession(false); }
    private void endSession(boolean backToLogin) { try { preferencesStore.save(new WindowPreferences(getWidth(), getHeight())); } catch (java.io.IOException ignored) { } dispose(); if (backToLogin) signOut.run(); else System.exit(0); }

    private void fitToScreen(WindowPreferences preferences) {
        GraphicsConfiguration screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration(); Rectangle bounds = screen.getBounds(); Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(screen);
        Rectangle usable = new Rectangle(bounds.x + insets.left, bounds.y + insets.top, bounds.width - insets.left - insets.right, bounds.height - insets.top - insets.bottom);
        int width = Math.min(Math.max(preferences.width(), 1100), usable.width); int height = Math.min(Math.max(preferences.height(), 720), usable.height); setMinimumSize(new Dimension(Math.min(1050, usable.width), Math.min(680, usable.height))); setSize(width, height); setLocation(usable.x + (usable.width - width) / 2, usable.y + (usable.height - height) / 2);
    }

    private static String escape(String text) { return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }
}
