package com.medicarepro;
import javax.swing.SwingUtilities;
public final class App {
    private App() { }
    public static void main(String[] args) { SwingUtilities.invokeLater(() -> { try { Database db=new Database(); db.initialize(); new MainFrame(db).showLogin(); } catch(Exception e) { javax.swing.JOptionPane.showMessageDialog(null,e.getMessage(),"MediCare Pro",0); } }); }
}
