package com.medicarepro;

import java.awt.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

final class MainFrame extends JFrame {
    private final User user;
    private final ClinicRepository repository;
    private final AuthService authService;
    private final ClinicEvents events;
    private final JPanel content = new JPanel(new CardLayout());
    private final Map<String, JComponent> pageByKey = new LinkedHashMap<>();
    private final Map<String, JButton> navButtons = new LinkedHashMap<>();
    private final JLabel sectionTitle = new JLabel("Clinic overview");
    private final Runnable signOut;

    public MainFrame(User user, ClinicRepository repository, AuthService authService,
                     ClinicEvents events, Runnable signOut) {
        super("MediCare Pro · Hospital Patient Information System");
        this.user = user;
        this.repository = repository;
        this.authService = authService;
        this.events = events;
        this.signOut = signOut;
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setSize(1200, 760);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
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
        if (user.isAdmin()) addNavigation(navigation, "accounts", "User accounts");
        side.add(navigation, BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(4, 9));
        bottom.setOpaque(false);
        JLabel profile = new JLabel("<html><b>" + escape(user.displayName()) + "</b><br><span style='color:#b7cec7'>"
                + accountLabel() + "</span></html>");
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
        JComponent panel = pageByKey.computeIfAbsent(key, this::createPanel);
        String title = switch (key) {
            case "dashboard" -> "Clinic overview";
            case "patients" -> "Patient records";
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

    private JComponent createPanel(String section) {
        return switch (section) {
            case "dashboard" -> new DashboardPanel(repository, user, events, this::showSection);
            case "patients" -> new PatientsPanel(repository, user, events);
            case "accounts" -> new UsersPanel(authService, user, events);
            default -> throw new IllegalArgumentException("Unknown section: " + section);
        };
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
        dispose();
        if (backToLogin) signOut.run();
        else System.exit(0);
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private String accountLabel() {
        if (user.isAdmin()) return "Administrator";
        if (user.isReceptionist()) return "Receptionist · " + escape(user.username());
        if (user.isPatient()) return "Patient · " + escape(user.username());
        return "User · " + escape(user.username());
    }
}

/** Patient-focused dashboard, retaining the existing dashboard layout. */
final class DashboardPanel extends JPanel implements Refreshable {
    private final ClinicRepository repository;
    private final User user;
    private final Consumer<String> navigate;
    private final JLabel[] values = new JLabel[4];
    private final JTable recentTable = new JTable(new DefaultTableModel(
            new Object[]{"Patient ID", "Patient", "Phone", "Registered"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    });

    public DashboardPanel(ClinicRepository repository, User user, ClinicEvents events, Consumer<String> navigate) {
        super(new BorderLayout(18, 18));
        this.repository = repository;
        this.user = user;
        this.navigate = navigate;
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        add(header(), BorderLayout.NORTH);
        add(content(), BorderLayout.CENTER);
        events.addListener(event -> SwingUtilities.invokeLater(this::refresh));
        SwingUtilities.invokeLater(this::refresh);
    }

    private JComponent header() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JPanel text = new JPanel(new GridLayout(2, 1, 0, 5));
        text.setOpaque(false);
        JLabel title = new JLabel("Clinic overview");
        title.setFont(new Font("SansSerif", Font.BOLD, 25));
        JLabel subtitle = new JLabel(user.isAdmin()
                ? "Administrator view · patient activity across all accounts."
                : "A quick view of your patient records.");
        subtitle.setForeground(SwingTheme.MUTED);
        text.add(title);
        text.add(subtitle);
        header.add(text, BorderLayout.CENTER);
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(event -> refresh());
        header.add(refresh, BorderLayout.EAST);
        return header;
    }

    private JComponent content() {
        JPanel content = new JPanel(new BorderLayout(18, 18));
        content.setOpaque(false);
        JPanel cards = new JPanel(new GridLayout(2, 3, 12, 12));
        cards.setOpaque(false);
        String[] labels = {"Total patients", "Active patients", "Follow-up patients", "Online patients"};
        for (int index = 0; index < labels.length; index++) {
            JPanel card = new JPanel(new BorderLayout(8, 8));
            card.setBackground(Color.WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(SwingTheme.LINE),
                    BorderFactory.createEmptyBorder(17, 18, 16, 18)));
            JLabel label = new JLabel(labels[index]);
            label.setForeground(SwingTheme.MUTED);
            label.setFont(new Font("SansSerif", Font.PLAIN, 12));
            values[index] = new JLabel("—");
            values[index].setFont(new Font("SansSerif", Font.BOLD, 28));
            JLabel hint = new JLabel("Open records  ›");
            hint.setForeground(SwingTheme.ACCENT);
            hint.setFont(new Font("SansSerif", Font.BOLD, 10));
            card.add(label, BorderLayout.NORTH);
            card.add(values[index], BorderLayout.CENTER);
            card.add(hint, BorderLayout.SOUTH);
            card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            card.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent event) { navigate.accept("patients"); }
            });
            cards.add(card);
        }
        content.add(cards, BorderLayout.NORTH);

        JPanel recent = new JPanel(new BorderLayout(10, 10));
        recent.setBackground(Color.WHITE);
        recent.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(SwingTheme.LINE),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        JLabel recentTitle = new JLabel("Recently registered patients");
        recentTitle.setFont(new Font("SansSerif", Font.BOLD, 15));
        recent.add(recentTitle, BorderLayout.NORTH);
        recentTable.setRowHeight(30);
        recentTable.setFillsViewportHeight(true);
        recentTable.setAutoCreateRowSorter(true);
        recentTable.getTableHeader().setReorderingAllowed(false);
        recent.add(new JScrollPane(recentTable), BorderLayout.CENTER);
        JButton openPatients = new JButton("Open patients");
        openPatients.addActionListener(event -> navigate.accept("patients"));
        recent.add(openPatients, BorderLayout.SOUTH);
        content.add(recent, BorderLayout.CENTER);
        return content;
    }

    @Override public void refresh() {
        new SwingWorker<Snapshot, Void>() {
            @Override protected Snapshot doInBackground() {
                return new Snapshot(repository.dashboardStats(user), repository.recentPatients(user));
            }

            @Override protected void done() {
                try {
                    Snapshot snapshot = get();
                    DashboardStats stats = snapshot.stats();
                    long[] counts = {stats.patients(), stats.activePatients(),
                            stats.followUpPatients(), stats.onlinePatients()};
                    for (int index = 0; index < counts.length; index++) values[index].setText(String.valueOf(counts[index]));
                    DefaultTableModel model = (DefaultTableModel) recentTable.getModel();
                    model.setRowCount(0);
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM · HH:mm");
                    for (Patient patient : snapshot.patients()) {
                        String registered = patient.createdAt().atZone(ZoneId.systemDefault()).format(formatter);
                        model.addRow(new Object[]{patient.patientCode(), patient.name(), patient.contact(), registered});
                    }
                } catch (Exception error) {
                    FormDialogs.showError(DashboardPanel.this, error);
                }
            }
        }.execute();
    }

    private record Snapshot(DashboardStats stats, List<Patient> patients) { }
}

/** Shared Swing panel behavior; concrete modules override CRUD actions (polymorphism). */
abstract class RecordPanel<T> extends JPanel implements Refreshable {
    protected final JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
    protected final JTable table;
    protected final RecordTableModel<T> tableModel;
    protected final JTextField searchField = new JTextField(24);
    protected final ClinicEvents events;
    private final JLabel countLabel = new JLabel("Loading records…");
    private final JLabel subtitleLabel = new JLabel("Manage clinic records. Your account only sees its own records.");
    private final Timer searchTimer;
    private boolean busy;

    protected RecordPanel(String title, String[] columns, java.util.function.Function<T, Object[]> rowAdapter,
                          ClinicEvents events) {
        super(new BorderLayout(14, 14));
        this.events = events;
        setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));
        tableModel = new RecordTableModel<>(columns, rowAdapter);
        table = new JTable(tableModel);
        table.setRowHeight(34);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setBackground(new Color(239, 243, 239));
        table.getTableHeader().setForeground(SwingTheme.MUTED);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable source, Object value,
                    boolean selected, boolean focus, int row, int column) {
                Component component = super.getTableCellRendererComponent(source, value, selected, focus, row, column);
                if (!selected) component.setBackground(row % 2 == 0 ? SwingTheme.WHITE : new Color(250, 251, 248));
                setBorder(BorderFactory.createEmptyBorder(0, 9, 0, 9));
                return component;
            }
        });
        add(header(title), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(countLabel, BorderLayout.WEST);
        add(footer, BorderLayout.SOUTH);
        if (supportsAdd()) addToolbarButton("Add", () -> onAdd());
        if (supportsEdit()) addToolbarButton("Edit", () -> withSelected(this::onEdit));
        if (supportsDelete()) addToolbarButton("Delete", () -> withSelected(this::onDelete));
        addToolbarButton("Refresh", this::refresh);
        toolbar.add(Box.createHorizontalStrut(12));
        toolbar.add(new JLabel("Search"));
        toolbar.add(searchField);
        searchTimer = new Timer(250, event -> refresh());
        searchTimer.setRepeats(false);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { searchTimer.restart(); }
            @Override public void removeUpdate(DocumentEvent event) { searchTimer.restart(); }
            @Override public void changedUpdate(DocumentEvent event) { searchTimer.restart(); }
        });
        events.addListener(event -> SwingUtilities.invokeLater(this::refresh));
        SwingUtilities.invokeLater(this::refresh);
    }

    private JComponent header(String title) {
        JPanel panel = new JPanel(new BorderLayout(10, 8));
        panel.setOpaque(false);
        JLabel heading = new JLabel(title);
        heading.setFont(new Font("SansSerif", Font.BOLD, 24));
        heading.setForeground(SwingTheme.INK);
        subtitleLabel.setForeground(SwingTheme.MUTED);
        JPanel copy = new JPanel(new GridLayout(2, 1, 0, 4));
        copy.setOpaque(false);
        copy.add(heading);
        copy.add(subtitleLabel);
        panel.add(copy, BorderLayout.CENTER);
        panel.add(toolbar, BorderLayout.SOUTH);
        return panel;
    }

    /**
     * Sets the grey line under the page title. Subclasses call this from their own constructor
     * (after their fields are assigned) instead of overriding a method that the super constructor
     * would call too early.
     */
    protected final void setSubtitle(String text) { subtitleLabel.setText(text); }

    protected void addActionButton(String label, Runnable action) {
        addToolbarButton(label, action);
    }

    protected void addToolbarButton(String label, Runnable action) {
        JButton button = new JButton(label);
        button.addActionListener(event -> action.run());
        toolbar.add(button);
    }

    protected abstract List<T> loadRows(String search) throws Exception;
    protected abstract void onAdd();
    protected void onEdit(T row) { }
    protected void onDelete(T row) { }
    protected boolean supportsAdd() { return true; }
    protected boolean supportsEdit() { return true; }
    protected boolean supportsDelete() { return true; }

    protected T selected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) return null;
        return tableModel.row(table.convertRowIndexToModel(viewRow));
    }

    protected void withSelected(java.util.function.Consumer<T> action) {
        T row = selected();
        if (row == null) {
            JOptionPane.showMessageDialog(this, "Select a row first.", "MediCare Pro", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        action.accept(row);
    }

    protected void runAsync(String operation, Runnable task) {
        if (busy) return;
        busy = true;
        setActionsEnabled(false);
        SwingWorker<Object, Void> worker = new SwingWorker<>() {
            @Override protected Object doInBackground() {
                task.run();
                return null;
            }
            @Override protected void done() {
                busy = false;
                setActionsEnabled(true);
                try {
                    get();
                    events.recordsChanged("clinic");
                    JOptionPane.showMessageDialog(RecordPanel.this, operation + " completed.",
                            "MediCare Pro", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception error) {
                    FormDialogs.showError(RecordPanel.this, error);
                }
                refresh();
            }
        };
        worker.execute();
    }

    private void setActionsEnabled(boolean enabled) {
        searchField.setEnabled(enabled);
        for (Component component : toolbar.getComponents()) component.setEnabled(enabled);
    }

    protected void recordsChanged(String area) {
        firePropertyChange("records.changed", null, area);
    }

    @Override public final void refresh() {
        SwingWorker<List<T>, Void> worker = new SwingWorker<>() {
            @Override protected List<T> doInBackground() throws Exception {
                return loadRows(searchField.getText());
            }
            @Override protected void done() {
                try {
                    List<T> rows = get();
                    tableModel.replaceRows(rows);
                    countLabel.setText(rows.size() + (rows.size() == 1 ? " record" : " records"));
                } catch (Exception error) {
                    tableModel.replaceRows(List.of());
                    countLabel.setText("Could not load records");
                    FormDialogs.showError(RecordPanel.this, error);
                }
            }
        };
        worker.execute();
    }
}

final class PatientsPanel extends RecordPanel<Patient> {
    private static final String[] COLUMNS = {"Patient ID", "Name", "Age", "Gender", "Contact", "Visit", "Status", "Source"};
    private final ClinicRepository repository;
    private final User user;

    public PatientsPanel(ClinicRepository repository, User user, ClinicEvents events) {
        super("Patient records", COLUMNS, PatientsPanel::row, events);
        this.repository = repository;
        this.user = user;
        setSubtitle(user.isAdmin()
                ? "Administrator view · manage patient records across all accounts."
                : "Register, search, update, and remove patient records.");
    }

    @Override protected List<Patient> loadRows(String search) {
        return repository.listPatients(user, search);
    }

    @Override protected void onAdd() {
        Map<String, String> values = patientForm(null);
        if (values == null) return;
        try {
            PatientInput input = toInput(values);
            runAsync("Patient registration", () -> repository.createPatient(user, input));
        } catch (RuntimeException error) {
            FormDialogs.showError(this, error);
        }
    }

    @Override protected void onEdit(Patient patient) {
        Map<String, String> values = patientForm(patient);
        if (values == null) return;
        try {
            PatientInput input = toInput(values);
            runAsync("Patient update", () -> repository.updatePatient(user, patient.id(), input));
        } catch (RuntimeException error) {
            FormDialogs.showError(this, error);
        }
    }

    @Override protected void onDelete(Patient patient) {
        int answer = JOptionPane.showConfirmDialog(this,
                "Delete " + patient.name() + " (" + patient.patientCode() + ")? This cannot be undone.",
                "Delete patient", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer == JOptionPane.YES_OPTION) {
            runAsync("Patient deletion", () -> repository.deletePatient(user, patient.id()));
        }
    }

    private Map<String, String> patientForm(Patient patient) {
        return FormDialogs.show(this, patient == null ? "Register patient" : "Update patient",
                List.of(
                        FieldSpec.text("name", "Full name *", patient == null && user.isPatient() ? user.displayName() : (patient == null ? "" : patient.name())),
                        FieldSpec.text("age", "Age *", patient == null ? "" : String.valueOf(patient.age())),
                        FieldSpec.choice("gender", "Gender", patient == null ? "Not specified" : patient.gender(),
                                "Not specified", "Female", "Male", "Other"),
                        FieldSpec.text("contact", "Contact *", patient == null ? "" : patient.contact()),
                        FieldSpec.choice("visit", "Visit type", patient == null ? "First Visit" : patient.visitType(),
                                "First Visit", "Follow-up"),
                        FieldSpec.multiline("diagnosis", "Diagnosis / notes", patient == null ? "" : patient.diagnosis()),
                        FieldSpec.choice("status", "Patient status", patient == null ? "Active" : patient.status(),
                                "Active", "Follow Up", "Discharged")
                ));
    }

    private static PatientInput toInput(Map<String, String> values) {
        int age;
        try {
            age = Integer.parseInt(values.get("age"));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Age must be a whole number.");
        }
        return new PatientInput(values.get("name"), age, values.get("gender"), values.get("contact"),
                values.get("visit"), values.get("diagnosis"), values.get("status"));
    }

    private static Object[] row(Patient patient) {
        return new Object[]{patient.patientCode(), patient.name(), patient.age(), patient.gender(),
                patient.contact(), patient.visitType(), patient.status(), patient.source()};
    }
}

final class UsersPanel extends RecordPanel<UserSummary> {
    private static final String[] COLUMNS = {"ID", "Username", "Display name", "Role", "Account"};
    private final AuthService authService;
    private final User admin;

    public UsersPanel(AuthService authService, User admin, ClinicEvents events) {
        super("User accounts", COLUMNS, UsersPanel::row, events);
        this.authService = authService;
        this.admin = admin;
        setSubtitle("Administrator only · view accounts and create receptionist logins.");
    }

    @Override protected boolean supportsEdit() { return false; }
    @Override protected boolean supportsDelete() { return false; }

    @Override protected List<UserSummary> loadRows(String search) {
        String term = search == null ? "" : search.trim().toLowerCase();
        return authService.listUsers(admin).stream()
                .filter(user -> term.isEmpty() || user.username().toLowerCase().contains(term)
                        || user.displayName().toLowerCase().contains(term))
                .toList();
    }

    @Override protected void onAdd() {
        Map<String, String> values = FormDialogs.show(this, "Create receptionist login",
                List.of(FieldSpec.text("receptionistId", "Receptionist ID", ""),
                        FieldSpec.text("displayName", "Full name", "")));
        if (values == null) return;
        runAsync("Receptionist creation", () -> authService.createReceptionist(admin, values.get("displayName"), values.get("receptionistId")));
    }

    private static Object[] row(UserSummary user) {
        return new Object[]{user.id(), user.username(), user.displayName(), user.role(),
                user.active() ? "Active" : "Disabled"};
    }
}
