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
    private final User user;
    private final ClinicPanelFactory panels;
    private final JPanel content = new JPanel(new CardLayout());
    private final Map<String, JComponent> pageByKey = new LinkedHashMap<>();
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();
    private final JLabel sectionTitle = new JLabel("Clinic overview");
    private final Runnable signOut;
    private final PreferencesStore preferencesStore = new PreferencesStore();

    public MainFrame(User user, ClinicRepository repository, AuthService authService,
                     ClinicEvents events, Runnable signOut) {
        super("MediCare Pro · Hospital Patient Information System");
        this.user = user;
        this.panels = new ClinicPanelFactory(repository, authService, events);
        this.signOut = signOut;
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        WindowPreferences preferences;
        try {
            preferences = preferencesStore.load();
        } catch (java.io.IOException error) {
            JOptionPane.showMessageDialog(null,
                    "Saved window settings could not be read. Default size will be used.\n" + error.getMessage(),
                    "MediCare Pro settings", JOptionPane.WARNING_MESSAGE);
            preferences = WindowPreferences.defaults();
        }
        fitToScreen(preferences);
        setLayout(new BorderLayout());
        add(sidebar(), BorderLayout.WEST);
        add(mainContent(), BorderLayout.CENTER);
        showSection("dashboard");
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent event) { confirmClose(); }
        });
    }

    private JComponent sidebar() {
        JPanel side = new JPanel(new BorderLayout());
        side.setPreferredSize(new Dimension(226, 0));
        side.setBackground(new Color(24, 54, 58));
        side.setBorder(BorderFactory.createEmptyBorder(18, 14, 15, 14));
        JPanel top = new JPanel(new GridLayout(2, 1, 0, 5));
        top.setOpaque(false);
        JLabel brand = new JLabel("  ♥  MediCare Pro");
        brand.setForeground(Color.WHITE);
        brand.setFont(new Font("SansSerif", Font.BOLD, 18));
        JLabel edition = new JLabel("      JAVA · CLINIC SYSTEM");
        edition.setForeground(new Color(162, 194, 181));
        edition.setFont(new Font("Monospaced", Font.PLAIN, 9));
        top.add(brand);
        top.add(edition);
        side.add(top, BorderLayout.NORTH);
        JPanel navigation = new JPanel();
        navigation.setOpaque(false);
        navigation.setLayout(new BoxLayout(navigation, BoxLayout.Y_AXIS));
        navigation.setBorder(BorderFactory.createEmptyBorder(32, 0, 12, 0));
        addNavigation(navigation, "dashboard", "Overview");
        addNavigation(navigation, "patients", "Patients");
        addNavigation(navigation, "appointments", "Appointments");
        addNavigation(navigation, "consultations", "Consultations");
        addNavigation(navigation, "doctors", "Doctors");
        addNavigation(navigation, "invoices", "Invoices");
        if (user.isAdmin()) addNavigation(navigation, "accounts", "User accounts");
        side.add(navigation, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(4, 9));
        bottom.setOpaque(false);
        JLabel profile = new JLabel("<html><b>" + escape(user.displayName()) + "</b><br><span style='color:#b7cec7'>"
                + (user.isAdmin() ? "Administrator" : "User · " + escape(user.username())) + "</span></html>");
        profile.setForeground(Color.WHITE);
        profile.setBorder(BorderFactory.createEmptyBorder(8, 4, 8, 4));
        JButton signOut = new JButton("Sign out");
        signOut.addActionListener(event -> logout());
        bottom.add(profile, BorderLayout.CENTER);
        bottom.add(signOut, BorderLayout.SOUTH);
        side.add(bottom, BorderLayout.SOUTH);
        return side;
    }

    private void addNavigation(JPanel navigation, String key, String label) {
        JButton button = new JButton("  " + label);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        button.setFocusPainted(false);
        button.setForeground(new Color(219, 232, 226));
        button.setBackground(new Color(24, 54, 58));
        button.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        button.addActionListener(event -> showSection(key));
        navigation.add(button);
        navigation.add(Box.createVerticalStrut(5));
        navButtons.put(key, button);
    }

    private JComponent mainContent() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(SwingTheme.PAPER);
        JPanel topbar = new JPanel(new BorderLayout());
        topbar.setBackground(Color.WHITE);
        topbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, SwingTheme.LINE),
                BorderFactory.createEmptyBorder(14, 24, 14, 24)));
        JLabel breadcrumb = new JLabel("Workspace   /   ");
        breadcrumb.setForeground(SwingTheme.MUTED);
        JPanel crumb = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        crumb.setOpaque(false);
        crumb.add(breadcrumb);
        sectionTitle.setFont(new Font("SansSerif", Font.BOLD, 12));
        sectionTitle.setForeground(SwingTheme.INK);
        crumb.add(sectionTitle);
        JLabel safety = new JLabel("TRAINING PROJECT · NO REAL PATIENT DATA");
        safety.setFont(new Font("Monospaced", Font.PLAIN, 9));
        safety.setForeground(SwingTheme.MUTED);
        JButton topSignOut = new JButton("Sign out");
        topSignOut.setToolTipText("Sign out of " + user.username());
        topSignOut.addActionListener(event -> logout());
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
        right.setOpaque(false);
        right.add(safety);
        right.add(topSignOut);
        topbar.add(crumb, BorderLayout.WEST);
        topbar.add(right, BorderLayout.EAST);
        main.add(topbar, BorderLayout.NORTH);
        main.add(content, BorderLayout.CENTER);
        return main;
    }

    private void showSection(String key) {
        if ("accounts".equals(key) && !user.isAdmin()) {
            JOptionPane.showMessageDialog(this, "Only administrators can manage accounts.",
                    "Access restricted", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JComponent panel = pageByKey.computeIfAbsent(key,
                section -> panels.create(section, user, this::showSection));
        String title = switch (key) {
            case "dashboard" -> "Clinic overview";
            case "patients" -> "Patient records";
            case "appointments" -> "Appointments";
            case "consultations" -> "Consultations";
            case "doctors" -> "Doctor directory";
            case "invoices" -> "Invoices";
            case "accounts" -> "User accounts";
            default -> "Workspace";
        };
        sectionTitle.setText(title);
        if (panel.getParent() == null) content.add(panel, key);
        ((CardLayout) content.getLayout()).show(content, key);
        if (panel instanceof Refreshable refreshable) refreshable.refresh();
        navButtons.forEach((navKey, button) -> {
            button.setBackground(navKey.equals(key) ? new Color(55, 105, 95) : new Color(24, 54, 58));
            button.setForeground(Color.WHITE);
        });
    }

    /** Sign-out button: ask once, then return to the login screen. */
    private void logout() {
        int choice = JOptionPane.showConfirmDialog(this, "Sign out of MediCare Pro?",
                "Sign out", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) endSession(true);
    }

    /** Window close (X) button: sign out, exit the whole program, or stay. */
    private void confirmClose() {
        Object[] options = {"Sign out", "Exit application", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this, "What would you like to do?",
                "Close MediCare Pro", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, options, options[0]);
        if (choice == 0) endSession(true);
        else if (choice == 1) endSession(false);
    }

    private void endSession(boolean backToLogin) {
        try {
            preferencesStore.save(new WindowPreferences(getWidth(), getHeight()));
        } catch (java.io.IOException error) {
            JOptionPane.showMessageDialog(this, "Window settings could not be saved:\n" + error.getMessage(),
                    "MediCare Pro settings", JOptionPane.WARNING_MESSAGE);
        }
        dispose();
        if (backToLogin) signOut.run();
        else System.exit(0);
    }

    /**
     * Never let the window be taller or wider than the usable screen (screen minus taskbar/dock).
     * A fixed 1360x850 window on a 1366x768 laptop used to push the sign-out button off-screen.
     */
    private void fitToScreen(WindowPreferences preferences) {
        GraphicsConfiguration screen = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration();
        Rectangle bounds = screen.getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(screen);
        Rectangle usable = new Rectangle(bounds.x + insets.left, bounds.y + insets.top,
                bounds.width - insets.left - insets.right, bounds.height - insets.top - insets.bottom);
        int width = Math.min(preferences.width(), usable.width);
        int height = Math.min(preferences.height(), usable.height);
        setMinimumSize(new Dimension(Math.min(1000, usable.width), Math.min(680, usable.height)));
        setSize(width, height);
        setLocation(usable.x + (usable.width - width) / 2, usable.y + (usable.height - height) / 2);
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
