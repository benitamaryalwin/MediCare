package com.medicarepro.ui;

import com.medicarepro.model.DashboardStats;
import com.medicarepro.model.User;
import com.medicarepro.service.ClinicEvents;
import com.medicarepro.service.ClinicRepository;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public final class DashboardPanel extends JPanel implements Refreshable {
    private final ClinicRepository repository; private final User user; private final Consumer<String> navigate; private final JLabel[] values = new JLabel[4];

    public DashboardPanel(ClinicRepository repository, User user, ClinicEvents events, Consumer<String> navigate) {
        super(new BorderLayout(18, 18)); this.repository = repository; this.user = user; this.navigate = navigate; setBorder(BorderFactory.createEmptyBorder(24, 30, 28, 30)); setBackground(SwingTheme.PAPER); add(header(), BorderLayout.NORTH); add(content(), BorderLayout.CENTER); events.addListener(event -> SwingUtilities.invokeLater(this::refresh)); SwingUtilities.invokeLater(this::refresh);
    }

    private JComponent header() {
        UiKit.GradientPanel header = new UiKit.GradientPanel(new BorderLayout(20, 10), new Color(22, 86, 88), new Color(32, 139, 116)); header.setBorder(BorderFactory.createEmptyBorder(25, 28, 25, 28));
        JPanel text = new JPanel(new GridLayout(3, 1, 0, 5)); text.setOpaque(false); JLabel eyebrow = new JLabel(user.isAdmin() ? "HEAD DOCTOR CONSOLE" : "YOUR CLINIC WORKSPACE"); eyebrow.setFont(new Font("SansSerif", Font.BOLD, 10)); eyebrow.setForeground(new Color(190, 240, 222)); JLabel title = new JLabel("Good to see you, " + user.displayName()); title.setFont(new Font("SansSerif", Font.BOLD, 25)); title.setForeground(Color.WHITE); JLabel subtitle = new JLabel(user.isAdmin() ? "A live view of your entire clinic." : "Everything important, in one calm place."); subtitle.setForeground(new Color(221, 244, 236)); text.add(eyebrow); text.add(title); text.add(subtitle); header.add(text, BorderLayout.CENTER);
        JButton refresh = UiKit.button("Refresh data", false); refresh.setForeground(SwingTheme.ACCENT_DARK); refresh.setBorder(BorderFactory.createEmptyBorder(7, 12, 7, 12)); refresh.setFont(new Font("SansSerif", Font.BOLD, 11)); refresh.setPreferredSize(new Dimension(118, 36)); refresh.addActionListener(event -> refresh()); JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0)); actions.setOpaque(false); actions.add(refresh); header.add(actions, BorderLayout.EAST); return header;
    }

    private JComponent content() {
        JPanel content = new JPanel(new BorderLayout()); content.setOpaque(false); JPanel cards = new JPanel(new GridLayout(2, 2, 14, 14)); cards.setOpaque(false);
        String[] labels = {"Total patients", "Active patients", "Follow-up patients", "Doctors available"}; String[] routes = {"patients", "patients", "patients", "doctors"};
        for (int i = 0; i < labels.length; i++) { final int cardIndex = i; UiKit.RoundedPanel card = UiKit.surface(new BorderLayout(8, 8), 18, Color.WHITE); card.setBorder(BorderFactory.createEmptyBorder(22, 24, 22, 24)); JLabel label = new JLabel(labels[i]); label.setForeground(SwingTheme.MUTED); label.setFont(new Font("SansSerif", Font.BOLD, 12)); values[i] = new JLabel("—"); values[i].setForeground(SwingTheme.ACCENT_DARK); values[i].setFont(new Font("SansSerif", Font.BOLD, 34)); JLabel hint = new JLabel("View details  →"); hint.setForeground(SwingTheme.ACCENT); hint.setFont(new Font("SansSerif", Font.BOLD, 11)); card.add(label, BorderLayout.NORTH); card.add(values[i], BorderLayout.CENTER); card.add(hint, BorderLayout.SOUTH); card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); card.addMouseListener(new java.awt.event.MouseAdapter() { @Override public void mouseEntered(java.awt.event.MouseEvent event) { card.setBackground(new Color(237, 250, 247)); card.repaint(); } @Override public void mouseExited(java.awt.event.MouseEvent event) { card.setBackground(Color.WHITE); card.repaint(); } @Override public void mouseClicked(java.awt.event.MouseEvent event) { navigate.accept(routes[cardIndex]); } }); cards.add(card); }
        content.add(cards, BorderLayout.CENTER); return content;
    }

    @Override public void refresh() { new SwingWorker<DashboardStats, Void>() { @Override protected DashboardStats doInBackground() { return repository.dashboardStats(user); } @Override protected void done() { try { DashboardStats stats = get(); long[] counts = {stats.patients(), stats.activePatients(), stats.followUpPatients(), stats.doctorsAvailable()}; for (int i = 0; i < counts.length; i++) values[i].setText(String.valueOf(counts[i])); } catch (Exception error) { FormDialogs.showError(DashboardPanel.this, error); } } }.execute(); }
}
