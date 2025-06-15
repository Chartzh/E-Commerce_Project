package e.commerce;

import e.commerce.IconUtil;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class LoginUI extends JFrame {
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin, btnRegister;

    public LoginUI() {
        setTitle("E-Commerce App - Login");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        IconUtil.setIcon(this);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel headerPanel = new JPanel();
        JLabel lblTitle = new JLabel("LOGIN");
        lblTitle.setFont(new Font("Arial", Font.BOLD, 24));
        headerPanel.add(lblTitle);

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new GridLayout(3, 2, 10, 10));

        JLabel lblUsername = new JLabel("Username:");
        lblUsername.setFont(new Font("Arial", Font.PLAIN, 14));
        txtUsername = new JTextField();
        txtUsername.setFont(new Font("Arial", Font.PLAIN, 14));

        JLabel lblPassword = new JLabel("Password:");
        lblPassword.setFont(new Font("Arial", Font.PLAIN, 14));
        txtPassword = new JPasswordField();

        formPanel.add(lblUsername);
        formPanel.add(txtUsername);
        formPanel.add(lblPassword);
        formPanel.add(txtPassword);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));

        btnLogin = new JButton("Login");
        btnLogin.setFont(new Font("Arial", Font.BOLD, 14));
        btnLogin.setBackground(new Color(0, 123, 255));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFocusPainted(false);
        btnLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnRegister = new JButton("Register");
        btnRegister.setFont(new Font("Arial", Font.PLAIN, 14));
        btnRegister.setBackground(new Color(240, 240, 240));
        btnRegister.setFocusPainted(false);
        btnRegister.setCursor(new Cursor(Cursor.HAND_CURSOR));

        buttonPanel.add(btnLogin);
        buttonPanel.add(btnRegister);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(formPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);

        btnLogin.addActionListener(e -> login());

        btnRegister.addActionListener(e -> {
            try {
                RegisterUI registerUI = new RegisterUI();
                registerUI.setVisible(true);
                dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Gagal membuka form registrasi: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        });
    }

    private void login() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username dan password harus diisi!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        User user = Authentication.login(username, password);

        if (user != null) {
            // Karena User.java sekarang memiliki isVerified, ini akan berfungsi
            if (user.isVerified()) {
                switch (user.getRole().toLowerCase()) {
                    case "admin":
                        AdminDashboardUI adminDashboard = new AdminDashboardUI();
                        adminDashboard.setVisible(true);
                        break;
                    case "supervisor":
                        SupervisorDashboardUI supervisorDashboard = new SupervisorDashboardUI();
                        supervisorDashboard.setVisible(true);
                        break;
                    case "user":
                    default:
                        UserDashboardUI userDashboard = new UserDashboardUI();
                        userDashboard.setVisible(true);
                        break;
                }
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Akun Anda belum diverifikasi. Silakan verifikasi email Anda.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Username atau password salah!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        try {
            DatabaseConnection.initializeDatabase();
        } catch (Exception e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Failed to connect or initialize database. Check console for details.", "Database Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }

        SwingUtilities.invokeLater(() -> new LoginUI().setVisible(true));
    }
}