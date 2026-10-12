package com.medicarepro;


import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

/** Patient-focused dashboard, retaining the existing dashboard layout. */
public final class DashboardPanel extends JPanel implements Refreshable {
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
