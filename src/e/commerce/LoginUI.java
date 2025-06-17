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
        setSize(1000, 600); 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        IconUtil.setIcon(this);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(new Color(255, 102, 51)); // Orange background

        // Left panel with the full banner design as background
        JPanel leftPanel = new JPanel() {
            private Image bufferedImage; 

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (bufferedImage == null || bufferedImage.getWidth(null) != getWidth() || bufferedImage.getHeight(null) != getHeight()) {
                    try {
                        ImageIcon bannerIcon = new ImageIcon(getClass().getResource("/Resources/Images/banner_login.png"));
                        Image originalImage = bannerIcon.getImage();

                        if (originalImage == null || originalImage.getWidth(null) == -1) {
                            throw new Exception("Failed to load banner image or image is empty.");
                        }

                        bufferedImage = originalImage.getScaledInstance(getWidth(), getHeight(), Image.SCALE_SMOOTH);
                    } catch (Exception e) {
                        System.err.println("Error loading or scaling image: " + e.getMessage());
                        g.setColor(new Color(255, 102, 51)); 
                        g.fillRect(0, 0, getWidth(), getHeight());
                        g.setColor(Color.WHITE);
                        g.setFont(new Font("Arial", Font.BOLD, 16));
                        String msg1 = "E-Commerce";
                        String msg2 = "Banner Image Not Found";
                        String msg3 = "(/Resources/Images/banner_login.jpg)";
                        FontMetrics fm = g.getFontMetrics();
                        g.drawString(msg1, (getWidth() - fm.stringWidth(msg1)) / 2, getHeight() / 2 - fm.getHeight());
                        g.drawString(msg2, (getWidth() - fm.stringWidth(msg2)) / 2, getHeight() / 2);
                        g.drawString(msg3, (getWidth() - fm.stringWidth(msg3)) / 2, getHeight() / 2 + fm.getHeight());
                        return;
                    }
                }
                if (bufferedImage != null) {
                    g.drawImage(bufferedImage, 0, 0, this);
                }
            }
        };
        leftPanel.setPreferredSize(new Dimension(600, 600)); 
        leftPanel.setBackground(new Color(255, 102, 51)); 

        // Right panel with login form
        JPanel rightPanel = new JPanel();
        rightPanel.setPreferredSize(new Dimension(400, 600)); 
        rightPanel.setBackground(Color.WHITE);
        rightPanel.setLayout(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createEmptyBorder(60, 50, 60, 50)); 

        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(Color.WHITE);
        JLabel lblTitle = new JLabel("Login");
        lblTitle.setFont(new Font("Arial", Font.BOLD, 36));
        lblTitle.setForeground(new Color(255, 102, 51));
        headerPanel.add(lblTitle);

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new GridLayout(4, 1, 0, 5)); // vgap 20
        formPanel.setBackground(Color.WHITE);

        JLabel lblUsername = new JLabel("Username");
        lblUsername.setFont(new Font("Arial", Font.PLAIN, 16));
        lblUsername.setForeground(new Color(255, 102, 51));
        
        txtUsername = new JTextField();
        txtUsername.setFont(new Font("Arial", Font.PLAIN, 18)); 
        txtUsername.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(1, 15, 1, 15) 
        ));
        txtUsername.setBackground(new Color(230, 230, 230)); 
        txtUsername.setForeground(Color.BLACK);
        txtUsername.setCaretColor(Color.BLACK);
        txtUsername.setFocusable(true); 
        txtUsername.setRequestFocusEnabled(true); 

        JLabel lblPassword = new JLabel("Password");
        lblPassword.setFont(new Font("Arial", Font.PLAIN, 16));
        lblPassword.setForeground(new Color(255, 102, 51));
        
        txtPassword = new JPasswordField();
        txtPassword.setFont(new Font("Arial", Font.PLAIN, 18)); 
        txtPassword.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(1, 15, 1, 15) 
        ));
        txtPassword.setBackground(new Color(230, 230, 230)); 
        txtPassword.setForeground(Color.BLACK);
        txtPassword.setCaretColor(Color.BLACK);
        txtPassword.setFocusable(true); 
        txtPassword.setRequestFocusEnabled(true); 

        formPanel.add(lblUsername);
        formPanel.add(txtUsername);
        formPanel.add(lblPassword);
        formPanel.add(txtPassword);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(3, 1, 0, 5)); 
        buttonPanel.setBackground(Color.WHITE);

        btnLogin = new JButton("Login");
        btnLogin.setFont(new Font("Arial", Font.BOLD, 16));
        btnLogin.setBackground(new Color(255, 102, 51));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFocusPainted(false);
        btnLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLogin.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel lblDontHaveAccount = new JLabel("Don't have an account?");
        lblDontHaveAccount.setFont(new Font("Arial", Font.PLAIN, 14));
        lblDontHaveAccount.setForeground(Color.GRAY);
        lblDontHaveAccount.setHorizontalAlignment(JLabel.CENTER);

        btnRegister = new JButton("Register Now");
        btnRegister.setFont(new Font("Arial", Font.PLAIN, 14));
        btnRegister.setBackground(Color.WHITE);
        btnRegister.setForeground(new Color(255, 102, 51));
        btnRegister.setFocusPainted(false);
        btnRegister.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRegister.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));

        buttonPanel.add(btnLogin);
        buttonPanel.add(lblDontHaveAccount);
        buttonPanel.add(btnRegister);

        rightPanel.add(headerPanel, BorderLayout.NORTH);
        rightPanel.add(formPanel, BorderLayout.CENTER);
        rightPanel.add(buttonPanel, BorderLayout.SOUTH);

        mainPanel.add(leftPanel, BorderLayout.WEST);
        mainPanel.add(rightPanel, BorderLayout.EAST);

        add(mainPanel);

        // Meminta fokus ke field username saat jendela dibuka
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                SwingUtilities.invokeLater(() -> txtUsername.requestFocusInWindow());
            }
        });
        
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

        System.out.println("Username: " + username);
        System.out.println("Password: " + password);

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username dan password harus diisi!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        User user = Authentication.login(username, password);

        if (user != null) {
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