package e.commerce;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import org.mindrot.jbcrypt.BCrypt;

public class RegisterUI extends JFrame {
    private JTextField txtUsername, txtEmail;
    private JPasswordField txtPassword, txtConfirmPassword;
    // Fields for NIK, Alamat, No. Telepon removed from RegisterUI
    // private JTextField txtNik, txtNotelp;
    // private JTextArea txtAlamat;

    private JButton btnRegister, btnCancel;

    public RegisterUI() {
        setTitle("E-Commerce App - Register");
        setSize(400, 350); // Ukuran frame disesuaikan karena field berkurang
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Set ikon
        // Pastikan kelas IconUtil sudah ada dan dapat diakses
        // IconUtil.setIcon(this); 

        // Main panel
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Header panel
        JPanel headerPanel = new JPanel();
        JLabel lblTitle = new JLabel("REGISTER");
        lblTitle.setFont(new Font("Arial", Font.BOLD, 24));
        headerPanel.add(lblTitle);

        // Form panel
        JPanel formPanel = new JPanel();
        // Hanya 4 baris: Username, Password, Confirm Password, Email
        formPanel.setLayout(new GridLayout(4, 2, 10, 10)); 

        JLabel lblUsername = new JLabel("Username:");
        lblUsername.setFont(new Font("Arial", Font.PLAIN, 14));
        txtUsername = new JTextField();

        JLabel lblPassword = new JLabel("Password:");
        lblPassword.setFont(new Font("Arial", Font.PLAIN, 14));
        txtPassword = new JPasswordField();

        JLabel lblConfirmPassword = new JLabel("Confirm Password:");
        lblConfirmPassword.setFont(new Font("Arial", Font.PLAIN, 14));
        txtConfirmPassword = new JPasswordField();

        JLabel lblEmail = new JLabel("Email:");
        lblEmail.setFont(new Font("Arial", Font.PLAIN, 14));
        txtEmail = new JTextField();

        // Menambahkan komponen ke formPanel
        formPanel.add(lblUsername);
        formPanel.add(txtUsername);
        formPanel.add(lblPassword);
        formPanel.add(txtPassword);
        formPanel.add(lblConfirmPassword);
        formPanel.add(txtConfirmPassword);
        formPanel.add(lblEmail);
        formPanel.add(txtEmail);
        
        // Button panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));

        btnRegister = new JButton("Register");
        btnRegister.setFont(new Font("Arial", Font.BOLD, 14));
        btnRegister.setBackground(new Color(0, 123, 255));
        btnRegister.setForeground(Color.WHITE);
        btnRegister.setFocusPainted(false);
        btnRegister.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnCancel = new JButton("Cancel");
        btnCancel.setFont(new Font("Arial", Font.PLAIN, 14));
        btnCancel.setBackground(new Color(240, 240, 240));
        btnCancel.setFocusPainted(false);
        btnCancel.setCursor(new Cursor(Cursor.HAND_CURSOR));

        buttonPanel.add(btnRegister);
        buttonPanel.add(btnCancel);

        // Add all panels to main panel
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(formPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Add main panel to frame
        add(mainPanel);

        // Button event listeners
        btnRegister.addActionListener(e -> register());

        btnCancel.addActionListener(e -> {
            new LoginUI().setVisible(true); // Pastikan LoginUI dapat diakses
            dispose();
        });
    }

    private void register() {
        // Nonaktifkan tombol register untuk mencegah klik ganda
        btnRegister.setEnabled(false);

        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword()).trim();
        String confirmPassword = new String(txtConfirmPassword.getPassword()).trim();
        String email = txtEmail.getText().trim();
        // Removed NIK, Alamat, No. Telepon as they are no longer required for initial registration
        // String nik = txtNik.getText().trim();
        // String alamat = txtAlamat.getText().trim();
        // String notelp = txtNotelp.getText().trim();

        // Validasi input minimal (username, password, email)
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

        // Validasi panjang password
        if (password.length() < 8) {
            JOptionPane.showMessageDialog(this, "Password harus minimal 8 karakter!", "Error", JOptionPane.ERROR_MESSAGE);
            btnRegister.setEnabled(true);
            return;
        }

        // Validasi email format
        if (!email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            JOptionPane.showMessageDialog(this, "Format email tidak valid!", "Error", JOptionPane.ERROR_MESSAGE);
            btnRegister.setEnabled(true);
            return;
        }
        
        // Validasi NIK dan No. Telepon dihapus dari sini, akan diisi di ProfileUI
        /*
        // Validasi NIK (hanya angka, misalnya 16 digit)
        if (!nik.matches("\\d{16}")) {
            JOptionPane.showMessageDialog(this, "NIK harus 16 digit angka!", "Error", JOptionPane.ERROR_MESSAGE);
            btnRegister.setEnabled(true);
            return;
        }

        // Validasi nomor telepon (hanya angka, 10-13 digit)
        if (!notelp.matches("\\d{10,13}")) {
            JOptionPane.showMessageDialog(this, "Nomor telepon harus 10-13 digit angka!", "Error", JOptionPane.ERROR_MESSAGE);
            btnRegister.setEnabled(true);
            return;
        }
        */

        // Cek username dan email tersedia menggunakan Authentication (asumsi method ini sudah diupdate)
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

        // Hash password sebelum membuat objek User
        String hashedPassword;
        try {
            hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal mengenkripsi password: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            btnRegister.setEnabled(true);
            return;
        }

        // Buat objek user baru dengan password yang sudah di-hash
        // Menggunakan constructor User(username, hashedPassword, email) yang baru
        User pendingUser = new User(username, hashedPassword, email);

        // Generate OTP dan kirim ke email
        String otp = EmailSender.generateOTP();
        boolean emailSent = EmailSender.sendOTPEmail(email, otp);

        if (emailSent) {
            try {
                // Simpan OTP dan data pengguna sementara (seperti User object ini)
                // Ini penting agar data 'pendingUser' bisa diakses di OTPVerificationUI
                OTPManager.getInstance().setOTP(email, otp, pendingUser);

                // Tampilkan form verifikasi OTP
                // OTPVerificationUI perlu tahu bahwa setelah verifikasi, pengguna akan diinsert.
                // Logika insert User ke DB harusnya ada di dalam OTPVerificationUI setelah OTP berhasil.
                OTPVerificationUI otpUI = new OTPVerificationUI(pendingUser, email);
                otpUI.setVisible(true);
                dispose();
            } catch (RuntimeException e) {
                JOptionPane.showMessageDialog(this, 
                    "Gagal menyimpan OTP atau data pengguna sementara: " + e.getMessage() + ". Periksa konfigurasi database atau tabel verifications.", 
                    "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace(); // Penting untuk debugging
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