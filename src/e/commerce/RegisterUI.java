package e.commerce;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import org.mindrot.jbcrypt.BCrypt;

public class RegisterUI extends JFrame {
    private JTextField txtUsername, txtEmail;
    private JPasswordField txtPassword, txtConfirmPassword;
    private JButton btnRegister, btnCancel;

    public RegisterUI() {
        setTitle("E-Commerce App - Register");
        // Mengatur ukuran frame agar konsisten dengan LoginUI (dua panel)
        // Lebar: 600 (leftPanel) + 450 (rightPanel) = 1050
        // Tinggi: 600 (konsisten dengan LoginUI)
        setSize(1050, 600); //
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // --- MENGAKTIFKAN KEMBALI IconUtil ---
        // Pastikan kelas IconUtil ada dan berfungsi di proyek Anda
        IconUtil.setIcon(this); 

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(new Color(255, 102, 51)); // Latar belakang oranye yang sama dengan LoginUI

        // --- LEFT PANEL DENGAN GAMBAR (KONSISTEN DENGAN LoginUI) ---
        JPanel leftPanel = new JPanel() {
            private Image bufferedImage; 

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (bufferedImage == null || bufferedImage.getWidth(null) != getWidth() || bufferedImage.getHeight(null) != getHeight()) {
                    try {
                        // Menggunakan gambar yang sama dengan LoginUI
                        // Pastikan nama file dan path benar: /Resources/Images/banner_login.png
                        ImageIcon bannerIcon = new ImageIcon(getClass().getResource("/Resources/Images/banner_login.png"));
                        Image originalImage = bannerIcon.getImage();

                        if (originalImage == null || originalImage.getWidth(null) == -1) {
                            throw new Exception("Failed to load banner image or image is empty.");
                        }
                        // Skala gambar menggunakan SCALE_SMOOTH agar mengisi panel secara proporsional
                        bufferedImage = originalImage.getScaledInstance(getWidth(), getHeight(), Image.SCALE_SMOOTH);
                    } catch (Exception e) {
                        System.err.println("Error loading or scaling image for RegisterUI: " + e.getMessage());
                        // Fallback jika gambar tidak ditemukan
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
        // Ukuran panel kiri 1:1 (600x600), konsisten dengan LoginUI
        leftPanel.setPreferredSize(new Dimension(600, 600)); 
        leftPanel.setBackground(new Color(255, 102, 51)); 

        // --- RIGHT PANEL UNTUK FORM REGISTRASI ---
        JPanel rightPanel = new JPanel();
        // Ukuran rightPanel disesuaikan agar konten form registrasi muat dengan nyaman
        // Lebar 450 (600+450 = 1050), Tinggi 600
        rightPanel.setPreferredSize(new Dimension(450, 600));
        rightPanel.setBackground(Color.WHITE);
        rightPanel.setLayout(new BorderLayout());
        // Padding eksternal rightPanel konsisten dengan LoginUI
        rightPanel.setBorder(BorderFactory.createEmptyBorder(60, 50, 60, 50));

        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(Color.WHITE);
        JLabel lblTitle = new JLabel("REGISTER"); // Judul untuk registrasi
        lblTitle.setFont(new Font("Arial", Font.BOLD, 36)); // Ukuran font besar, konsisten dengan LoginUI
        lblTitle.setForeground(new Color(255, 102, 51));
        headerPanel.add(lblTitle);

        JPanel formPanel = new JPanel();
        // GridLayout disesuaikan: 4 baris untuk field, 2 kolom untuk label dan field
        // hgap: 10 (horizontal gap antar kolom)
        // vgap: 20 (vertical gap antar baris), konsisten dengan penyesuaian LoginUI terakhir
        formPanel.setLayout(new GridLayout(4, 2, 10, 20));
        formPanel.setBackground(Color.WHITE);

        // --- Komponen Form (Gaya diselaraskan dengan LoginUI) ---
        JLabel lblUsername = new JLabel("Username:");
        lblUsername.setFont(new Font("Arial", Font.PLAIN, 16)); // Ukuran font label konsisten
        lblUsername.setForeground(new Color(255, 102, 51));
        txtUsername = new JTextField();
        txtUsername.setFont(new Font("Arial", Font.PLAIN, 18)); // Ukuran font teks di dalam field konsisten
        txtUsername.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(18, 15, 18, 15) // Padding internal yang sama dengan LoginUI
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
            BorderFactory.createEmptyBorder(18, 15, 18, 15) // Padding internal yang sama
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
            BorderFactory.createEmptyBorder(18, 15, 18, 15) // Padding internal yang sama
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
            BorderFactory.createEmptyBorder(18, 15, 18, 15) // Padding internal yang sama
        ));
        txtEmail.setBackground(new Color(230, 230, 230));
        txtEmail.setForeground(Color.BLACK);
        txtEmail.setCaretColor(Color.BLACK);
        
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
        // Menggunakan GridLayout untuk menempatkan tombol berdampingan, konsisten dengan LoginUI
        // 1 baris, 2 kolom, hgap 10, vgap 0 (karena hanya 1 baris)
        buttonPanel.setLayout(new GridLayout(1, 2, 10, 0));
        buttonPanel.setBackground(Color.WHITE);
        // Memberi padding di atas buttonPanel untuk memisahkan dari formPanel
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        btnRegister = new JButton("Register"); // Tombol Register
        btnRegister.setFont(new Font("Arial", Font.BOLD, 16));
        btnRegister.setBackground(new Color(255, 102, 51)); // Warna tombol oranye
        btnRegister.setForeground(Color.WHITE);
        btnRegister.setFocusPainted(false);
        btnRegister.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnRegister.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20)); // Padding tombol

        btnCancel = new JButton("Cancel");
        btnCancel.setFont(new Font("Arial", Font.BOLD, 16)); // Font bold agar terlihat seragam
        btnCancel.setBackground(new Color(220, 220, 220)); // Warna abu-abu terang
        btnCancel.setForeground(Color.BLACK);
        btnCancel.setFocusPainted(false);
        btnCancel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20)); // Padding tombol

        buttonPanel.add(btnRegister);
        buttonPanel.add(btnCancel);

        // Menambahkan panel kiri dan kanan ke mainPanel
        mainPanel.add(leftPanel, BorderLayout.WEST);
        mainPanel.add(rightPanel, BorderLayout.EAST);

        // Menambahkan header, form, dan button panels ke rightPanel
        rightPanel.add(headerPanel, BorderLayout.NORTH);
        rightPanel.add(formPanel, BorderLayout.CENTER);
        rightPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Menambahkan mainPanel ke frame
        add(mainPanel);

        // Meminta fokus ke field username saat jendela dibuka
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                SwingUtilities.invokeLater(() -> txtUsername.requestFocusInWindow());
            }
        });
        
        // Event listeners untuk tombol
        btnRegister.addActionListener(e -> register());

        btnCancel.addActionListener(e -> {
            new LoginUI().setVisible(true);
            dispose();
        });
    }

    private void register() {
        btnRegister.setEnabled(false); //

        String username = txtUsername.getText().trim(); //
        String password = new String(txtPassword.getPassword()).trim(); //
        String confirmPassword = new String(txtConfirmPassword.getPassword()).trim(); //
        String email = txtEmail.getText().trim(); //

        if (username.isEmpty() || password.isEmpty() || email.isEmpty()) { //
            JOptionPane.showMessageDialog(this, "Username, Password, dan Email harus diisi!", "Error", JOptionPane.ERROR_MESSAGE); //
            btnRegister.setEnabled(true); //
            return; //
        }

        if (!password.equals(confirmPassword)) { //
            JOptionPane.showMessageDialog(this, "Password dan konfirmasi password tidak cocok!", "Error", JOptionPane.ERROR_MESSAGE); //
            btnRegister.setEnabled(true); //
            return; //
        }

        if (password.length() < 8) { //
            JOptionPane.showMessageDialog(this, "Password harus minimal 8 karakter!", "Error", JOptionPane.ERROR_MESSAGE); //
            btnRegister.setEnabled(true); //
            return; //
        }

        if (!email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) { //
            JOptionPane.showMessageDialog(this, "Format email tidak valid!", "Error", JOptionPane.ERROR_MESSAGE); //
            btnRegister.setEnabled(true); //
            return; //
        }
        
        if (!Authentication.isUsernameAvailable(username)) { //
            JOptionPane.showMessageDialog(this, "Username sudah digunakan!", "Error", JOptionPane.ERROR_MESSAGE); //
            btnRegister.setEnabled(true); //
            return; //
        }

        if (!Authentication.isEmailAvailable(email)) { //
            JOptionPane.showMessageDialog(this, "Email sudah digunakan!", "Error", JOptionPane.ERROR_MESSAGE); //
            btnRegister.setEnabled(true); //
            return; //
        }

        String hashedPassword; //
        try { //
            hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt()); //
        } catch (Exception e) { //
            JOptionPane.showMessageDialog(this, "Gagal mengenkripsi password: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); //
            btnRegister.setEnabled(true); //
            return; //
        }

        User pendingUser = new User(username, hashedPassword, email); //

        String otp = EmailSender.generateOTP(); //
        boolean emailSent = EmailSender.sendOTPEmail(email, otp); //

        if (emailSent) { //
            try { //
                OTPManager.getInstance().setOTP(email, otp, pendingUser); //

                OTPVerificationUI otpUI = new OTPVerificationUI(pendingUser, email); //
                otpUI.setVisible(true); //
                dispose(); //
            } catch (RuntimeException e) { //
                JOptionPane.showMessageDialog(this, 
                    "Gagal menyimpan OTP atau data pengguna sementara: " + e.getMessage() + ". Periksa konfigurasi database atau tabel verifications.", 
                    "Error", JOptionPane.ERROR_MESSAGE); //
                e.printStackTrace(); //
                btnRegister.setEnabled(true); //
            }
        } else { //
            JOptionPane.showMessageDialog(this, 
                "Gagal mengirim email verifikasi. Periksa koneksi internet atau pengaturan email.", 
                "Error", JOptionPane.ERROR_MESSAGE); //
            btnRegister.setEnabled(true); //
        }
    }
}