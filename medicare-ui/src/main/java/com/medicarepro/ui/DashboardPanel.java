package com.medicarepro.ui;

import com.medicarepro.model.Appointment;
import com.medicarepro.model.DashboardStats;
import com.medicarepro.model.User;
import com.medicarepro.service.ClinicEvents;
import com.medicarepro.service.ClinicRepository;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

public final class DashboardPanel extends JPanel implements Refreshable {
    private final ClinicRepository repository;
    private final User user;
    private final Consumer<String> navigate;
    private final JLabel[] values = new JLabel[6];
    private final JTable recentTable = new JTable(new DefaultTableModel(
            new Object[]{"Patient", "Doctor", "Time", "Status"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    });

    public DashboardPanel(ClinicRepository repository, User user, ClinicEvents events,
                          Consumer<String> navigate) {
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
                ? "Administrator view · combined totals and activity across all accounts."
                : "A quick view of your patients, appointments, and clinic work.");
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
        String[] labels = {"Total patients", "Active patients", "Follow-up patients",
                "Appointments today", "Doctors available", "Unpaid invoices"};
        String[] routes = {"patients", "patients", "patients", "appointments", "doctors", "invoices"};
        for (int i = 0; i < labels.length; i++) {
            final int cardIndex = i;
            JPanel card = new JPanel(new BorderLayout(8, 8));
            card.setBackground(Color.WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(SwingTheme.LINE),
                    BorderFactory.createEmptyBorder(17, 18, 16, 18)));
            JLabel label = new JLabel(labels[i]);
            label.setForeground(SwingTheme.MUTED);
            label.setFont(new Font("SansSerif", Font.PLAIN, 12));
            values[i] = new JLabel("—");
            values[i].setFont(new Font("SansSerif", Font.BOLD, 28));
            JLabel hint = new JLabel("Open records  ›");
            hint.setForeground(SwingTheme.ACCENT);
            hint.setFont(new Font("SansSerif", Font.BOLD, 10));
            card.add(label, BorderLayout.NORTH);
            card.add(values[i], BorderLayout.CENTER);
            card.add(hint, BorderLayout.SOUTH);
            card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            card.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent event) { navigate.accept(routes[cardIndex]); }
            });
            cards.add(card);
        }
        content.add(cards, BorderLayout.NORTH);
        JPanel recent = new JPanel(new BorderLayout(10, 10));
        recent.setBackground(Color.WHITE);
        recent.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SwingTheme.LINE),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        JLabel recentTitle = new JLabel("Upcoming appointments");
        recentTitle.setFont(new Font("SansSerif", Font.BOLD, 15));
        recent.add(recentTitle, BorderLayout.NORTH);
        recentTable.setRowHeight(30);
        recentTable.setFillsViewportHeight(true);
        recentTable.setAutoCreateRowSorter(true);
        recentTable.getTableHeader().setReorderingAllowed(false);
        recentTable.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                    boolean focus, int row, int column) {
                Component component = super.getTableCellRendererComponent(table, value, selected, focus, row, column);
                component.setBackground(new Color(239, 243, 239));
                component.setForeground(SwingTheme.MUTED);
                return component;
            }
        });
        recent.add(new JScrollPane(recentTable), BorderLayout.CENTER);
        JButton openAppointments = new JButton("Open appointments");
        openAppointments.addActionListener(event -> navigate.accept("appointments"));
        recent.add(openAppointments, BorderLayout.SOUTH);
        content.add(recent, BorderLayout.CENTER);
        return content;
    }

    @Override public void refresh() {
        new SwingWorker<Snapshot, Void>() {
            @Override protected Snapshot doInBackground() {
                DashboardStats stats = repository.dashboardStats(user);
                List<Appointment> appointments = repository.listAppointments(user, "").stream()
                        .filter(a -> a.appointmentAt().isAfter(java.time.OffsetDateTime.now()))
                        .filter(a -> "Scheduled".equals(a.status()) || "Yet to be Consulted".equals(a.status()))
                        .limit(6).toList();
                return new Snapshot(stats, appointments);
            }
            @Override protected void done() {
                try {
                    Snapshot snapshot = get();
                    DashboardStats stats = snapshot.stats();
                    long[] counts = {stats.patients(), stats.activePatients(), stats.followUpPatients(),
                            stats.appointmentsToday(), stats.doctorsAvailable(), stats.unpaidInvoices()};
                    for (int i = 0; i < counts.length; i++) values[i].setText(String.valueOf(counts[i]));
                    DefaultTableModel model = (DefaultTableModel) recentTable.getModel();
                    model.setRowCount(0);
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM · HH:mm");
                    for (Appointment appointment : snapshot.appointments()) {
                        model.addRow(new Object[]{appointment.patientName(), appointment.doctorName(),
                                appointment.appointmentAt().atZoneSameInstant(ZoneId.systemDefault()).format(formatter),
                                appointment.status()});
                    }
                } catch (Exception error) {
                    FormDialogs.showError(DashboardPanel.this, error);
                }
            }
        }.execute();
    }

    private record Snapshot(DashboardStats stats, List<Appointment> appointments) { }
}
