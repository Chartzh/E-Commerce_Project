package e.commerce;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DatabaseConnection.initializeDatabase(); // Inisialisasi database
            LoginUI loginUI = new LoginUI();
            loginUI.setVisible(true);
        });
    }
}