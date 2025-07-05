package e.commerce;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import org.mindrot.jbcrypt.BCrypt;

public class RegisterUI extends JFrame {
    private JTextField txtUsername, txtEmail;
    private JPasswordField txtPassword, txtConfirmPassword;
    private JButton btnRegister, btnCancel;
    private JCheckBox chkSeller;

    public RegisterUI() {
        setTitle("E-Commerce App - Register");
        setSize(1050, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        IconUtil.setIcon(this);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(new Color(255, 102, 51));

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
                        System.err.println("Error loading or scaling image for RegisterUI: " + e.getMessage());
                        g.setColor(new Color(255, 102, 51));
                        g.fillRect(0, 0, getWidth(), getHeight());
                        g.setColor(Color.WHITE);
                        g.setFont(new Font("Arial", Font.BOLD, 16));
                        String msg1 = "E-Commerce";
                        String msg2 = "Banner Image Not Found";
                        String msg3 = "(/Resources/Images/banner_login.png)";
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

        JPanel rightPanel = new JPanel();
        rightPanel.setPreferredSize(new Dimension(450, 600));
        rightPanel.setBackground(Color.WHITE);
        rightPanel.setLayout(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createEmptyBorder(60, 50, 60, 50));

        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(Color.WHITE);
        JLabel lblTitle = new JLabel("REGISTER");
        lblTitle.setFont(new Font("Arial", Font.BOLD, 36));
        lblTitle.setForeground(new Color(255, 102, 51));
        headerPanel.add(lblTitle);

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BorderLayout());
        formPanel.setBackground(Color.WHITE);
        
        JPanel fieldsPanel = new JPanel();
        fieldsPanel.setLayout(new GridLayout(4, 2, 10, 20));
        fieldsPanel.setBackground(Color.WHITE);

        JLabel lblUsername = new JLabel("Username:");
        lblUsername.setFont(new Font("Arial", Font.PLAIN, 16));
        lblUsername.setForeground(new Color(255, 102, 51));
        txtUsername = new JTextField();
        txtUsername.setFont(new Font("Arial", Font.PLAIN, 18));
        txtUsername.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(18, 15, 18, 15)
        ));
        txtUsername.setBackground(new Color(230, 230, 230));
        txtUsername.setForeground(Color.BLACK);
        txtUsername.setCaretColor(Color.BLACK);

        JLabel lblPassword = new JLabel("Password:");
        lblPassword.setFont(new Font("Arial", Font.PLAIN, 16));
        lblPassword.setForeground(new Color(255, 102, 51));
        txtPassword = new JPasswordField();
        txtPassword.setFont(new Font("Arial", Font.PLAIN, 18));
        txtPassword.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(18, 15, 18, 15)
        ));
        txtPassword.setBackground(new Color(230, 230, 230));
        txtPassword.setForeground(Color.BLACK);
        txtPassword.setCaretColor(Color.BLACK);

        JLabel lblConfirmPassword = new JLabel("Confirm Password:");
        lblConfirmPassword.setFont(new Font("Arial", Font.PLAIN, 16));
        lblConfirmPassword.setForeground(new Color(255, 102, 51));
        txtConfirmPassword = new JPasswordField();
        txtConfirmPassword.setFont(new Font("Arial", Font.PLAIN, 18));
        txtConfirmPassword.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(18, 15, 18, 15)
        ));
        txtConfirmPassword.setBackground(new Color(230, 230, 230));
        txtConfirmPassword.setForeground(Color.BLACK);
        txtConfirmPassword.setCaretColor(Color.BLACK);

        JLabel lblEmail = new JLabel("Email:");
        lblEmail.setFont(new Font("Arial", Font.PLAIN, 16));
        lblEmail.setForeground(new Color(255, 102, 51));
        txtEmail = new JTextField();
        txtEmail.setFont(new Font("Arial", Font.PLAIN, 18));
        txtEmail.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(18, 15, 18, 15)
        ));
        txtEmail.setBackground(new Color(230, 230, 230));
        txtEmail.setForeground(Color.BLACK);
        txtEmail.setCaretColor(Color.BLACK);

        chkSeller = new JCheckBox("Daftar Menjadi Penjual");
        chkSeller.setFont(new Font("Arial", Font.PLAIN, 16));
        chkSeller.setForeground(new Color(255, 102, 51));
        chkSeller.setBackground(Color.WHITE);
        chkSeller.setFocusPainted(false);
        chkSeller.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JPanel checkboxPanel = new JPanel();
        checkboxPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 10));
        checkboxPanel.setBackground(Color.WHITE);
        checkboxPanel.add(chkSeller);

        fieldsPanel.add(lblUsername);
        fieldsPanel.add(txtUsername);
        fieldsPanel.add(lblPassword);
        fieldsPanel.add(txtPassword);
        fieldsPanel.add(lblConfirmPassword);
        fieldsPanel.add(txtConfirmPassword);
        fieldsPanel.add(lblEmail);
        fieldsPanel.add(txtEmail);
        
        formPanel.add(fieldsPanel, BorderLayout.CENTER);
        formPanel.add(checkboxPanel, BorderLayout.SOUTH);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1, 2, 10, 0));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        btnRegister = new JButton("Register");
        btnRegister.setFont(new Font("Arial", Font.BOLD, 16));
        btnRegister.setBackground(new Color(255, 102, 51));
        btnRegister.setForeground(Color.WHITE);
        btnRegister.setFocusPainted(false);
        btnRegister.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRegister.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        btnCancel = new JButton("Cancel");
        btnCancel.setFont(new Font("Arial", Font.BOLD, 16));
        btnCancel.setBackground(new Color(220, 220, 220));
        btnCancel.setForeground(Color.BLACK);
        btnCancel.setFocusPainted(false);
        btnCancel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        buttonPanel.add(btnRegister);
        buttonPanel.add(btnCancel);

        mainPanel.add(leftPanel, BorderLayout.WEST);
        mainPanel.add(rightPanel, BorderLayout.EAST);

        rightPanel.add(headerPanel, BorderLayout.NORTH);
        rightPanel.add(formPanel, BorderLayout.CENTER);
        rightPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                SwingUtilities.invokeLater(() -> txtUsername.requestFocusInWindow());
            }
        });

        btnRegister.addActionListener(e -> register());

        btnCancel.addActionListener(e -> {
            new LoginUI().setVisible(true);
            dispose();
        });
    }

    private void register() {
        btnRegister.setEnabled(false);

        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword()).trim();
        String confirmPassword = new String(txtConfirmPassword.getPassword()).trim();
        String email = txtEmail.getText().trim();
        boolean isSeller = chkSeller.isSelected();

        if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username, Password, dan Email harus diisi!", "Error", JOptionPane.ERROR_MESSAGE);
            btnRegister.setEnabled(true);
            return;
        }

        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "Password dan konfirmasi password tidak cocok!", "Error", JOptionPane.ERROR_MESSAGE);
            btnRegister.setEnabled(true);
            return;
        }

        if (password.length() < 8) {
            JOptionPane.showMessageDialog(this, "Password harus minimal 8 karakter!", "Error", JOptionPane.ERROR_MESSAGE);
            btnRegister.setEnabled(true);
            return;
        }

        if (!email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            JOptionPane.showMessageDialog(this, "Format email tidak valid!", "Error", JOptionPane.ERROR_MESSAGE);
            btnRegister.setEnabled(true);
            return;
        }

        if (!Authentication.isUsernameAvailable(username)) {
            JOptionPane.showMessageDialog(this, "Username sudah digunakan!", "Error", JOptionPane.ERROR_MESSAGE);
            btnRegister.setEnabled(true);
            return;
        }

        if (!Authentication.isEmailAvailable(email)) {
            JOptionPane.showMessageDialog(this, "Email sudah digunakan!", "Error", JOptionPane.ERROR_MESSAGE);
            btnRegister.setEnabled(true);
            return;
        }

        String hashedPassword;
        try {
            hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal mengenkripsi password: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            btnRegister.setEnabled(true);
            return;
        }

        String role = isSeller ? "seller" : "user";
        User pendingUser = new User(username, hashedPassword, email, role, false);

        String otp = EmailSender.generateOTP();
        boolean emailSent = EmailSender.sendOTPEmail(email, otp);

        if (emailSent) {
            try {
                OTPManager.getInstance().setOTP(email, otp, pendingUser);

                OTPVerificationUI otpUI = new OTPVerificationUI(pendingUser, email);
                otpUI.setVisible(true);
                dispose();
            } catch (RuntimeException e) {
                if (e.getMessage() != null && e.getMessage().contains("sudah terdaftar dan terverifikasi")) {
                     JOptionPane.showMessageDialog(this, "Registrasi gagal: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                     JOptionPane.showMessageDialog(this,
                        "Gagal menyimpan OTP atau data pengguna sementara: " + e.getMessage() + ". Periksa konfigurasi database atau tabel verifications.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
                e.printStackTrace();
                btnRegister.setEnabled(true);
            }
        } else {
            JOptionPane.showMessageDialog(this,
                "Gagal mengirim email verifikasi. Periksa koneksi internet atau pengaturan email.",
                "Error", JOptionPane.ERROR_MESSAGE);
            btnRegister.setEnabled(true);
        }
    }
}