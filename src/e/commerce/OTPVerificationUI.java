package e.commerce;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class OTPVerificationUI extends JFrame {
    private JTextField txtOTP;
    private JButton btnVerify, btnResend, btnCancel;
    private JLabel lblTimer;
    private Timer countdownTimer;
    private int timeLeft = 300; // 5 menit dalam detik
    private User pendingUser;
    private String email;
    
    public OTPVerificationUI(User user, String email) {
        this.pendingUser = user;
        this.email = email;
        
        setTitle("E-Commerce App - Email Verification");
        setSize(400, 250);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Ubah ke DO_NOTHING untuk menangani penutupan manual
        setLocationRelativeTo(null);
        setResizable(false);
        
        // Main panel
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Header panel
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        
        JLabel lblTitle = new JLabel("VERIFIKASI EMAIL");
        lblTitle.setFont(new Font("Arial", Font.BOLD, 18));
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel lblInstruction = new JLabel("Kode OTP telah dikirim ke email: " + email);
        lblInstruction.setFont(new Font("Arial", Font.PLAIN, 12));
        lblInstruction.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        lblTimer = new JLabel("Waktu tersisa: 05:00");
        lblTimer.setFont(new Font("Arial", Font.PLAIN, 12));
        lblTimer.setForeground(Color.RED);
        lblTimer.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        headerPanel.add(lblTitle);
        headerPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        headerPanel.add(lblInstruction);
        headerPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        headerPanel.add(lblTimer);
        
        // Form panel
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        
        JLabel lblOTP = new JLabel("Masukkan kode OTP:");
        txtOTP = new JTextField(15);
        txtOTP.setFont(new Font("Arial", Font.BOLD, 16));
        txtOTP.setHorizontalAlignment(JTextField.CENTER);
        
        formPanel.add(lblOTP);
        formPanel.add(txtOTP);
        
        // Button panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
        
        btnVerify = new JButton("Verifikasi");
        btnVerify.setFont(new Font("Arial", Font.BOLD, 14));
        btnVerify.setBackground(new Color(0, 123, 255));
        btnVerify.setForeground(Color.WHITE);
        btnVerify.setFocusPainted(false);
        btnVerify.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        btnResend = new JButton("Kirim Ulang");
        btnResend.setFont(new Font("Arial", Font.PLAIN, 14));
        btnResend.setFocusPainted(false);
        btnResend.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        btnCancel = new JButton("Batal");
        btnCancel.setFont(new Font("Arial", Font.PLAIN, 14));
        btnCancel.setBackground(new Color(240, 240, 240));
        btnCancel.setFocusPainted(false);
        btnCancel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        buttonPanel.add(btnVerify);
        buttonPanel.add(btnResend);
        buttonPanel.add(btnCancel);
        
        // Add all panels to main panel
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(formPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Add main panel to frame
        add(mainPanel);
        
        // Start countdown timer
        startCountdownTimer();
        
        // Button event listeners
        btnVerify.addActionListener(e -> verifyOTP());
        btnResend.addActionListener(e -> resendOTP());
        btnCancel.addActionListener(e -> cancelVerification());
        
        // Window listener untuk menangani penutupan jendela
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleWindowClosing();
            }
        });
    }
    
    private void startCountdownTimer() {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
        
        timeLeft = 300; // Reset ke 5 menit
        updateTimerLabel();
        
        countdownTimer = new Timer(1000, e -> {
            timeLeft--;
            updateTimerLabel();
            
            if (timeLeft <= 0) {
                ((Timer)e.getSource()).stop();
                JOptionPane.showMessageDialog(this, 
                    "Waktu verifikasi habis. Silakan kirim ulang kode OTP.", 
                    "Timeout", JOptionPane.WARNING_MESSAGE);
                btnVerify.setEnabled(false); // Nonaktifkan tombol verifikasi
            }
        });
        countdownTimer.start();
    }
    
    private void updateTimerLabel() {
        int minutes = timeLeft / 60;
        int seconds = timeLeft % 60;
        lblTimer.setText(String.format("Waktu tersisa: %02d:%02d", minutes, seconds));
    }
    
    private void verifyOTP() {
        String otp = txtOTP.getText().trim();
        
        // Validasi input OTP
        if (otp.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Masukkan kode OTP!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (!otp.matches("\\d{6}")) {
            JOptionPane.showMessageDialog(this, "Kode OTP harus 6 digit angka!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Cek apakah OTP masih ada di database
        if (!OTPManager.getInstance().isPendingVerification(email)) {
            JOptionPane.showMessageDialog(this, 
                "Kode OTP sudah kedaluwarsa atau tidak ditemukan. Silakan kirim ulang.", 
                "Error", JOptionPane.ERROR_MESSAGE);
            if (countdownTimer != null) {
                countdownTimer.stop();
            }
            btnVerify.setEnabled(false);
            return;
        }
        
        if (OTPManager.getInstance().verifyOTP(email, otp)) {
            // OTP valid, simpan user ke database
            if (countdownTimer != null) {
                countdownTimer.stop();
            }
            
            // Update data pengguna alih-alih insert baru
            boolean success = pendingUser.register();
            
            if (success) {
                // Hapus OTP setelah registrasi berhasil
                OTPManager.getInstance().removeOTP(email);
                JOptionPane.showMessageDialog(this, 
                    "Verifikasi email berhasil! Akun Anda telah dibuat.", 
                    "Sukses", JOptionPane.INFORMATION_MESSAGE);
                    
                new LoginUI().setVisible(true);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Gagal menyimpan data pengguna. Silakan coba lagi.", 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, 
                "Kode OTP tidak valid.", 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void resendOTP() {
        // Nonaktifkan tombol kirim ulang sementara
        btnResend.setEnabled(false);
        
        // Generate new OTP and send email
        String newOTP = EmailSender.generateOTP();
        boolean emailSent = EmailSender.sendOTPEmail(email, newOTP);
        
        if (emailSent) {
            OTPManager.getInstance().setOTP(email, newOTP, pendingUser);
            JOptionPane.showMessageDialog(this, 
                "Kode OTP baru telah dikirim ke email Anda.", 
                "Info", JOptionPane.INFORMATION_MESSAGE);
            
            // Reset timer
            startCountdownTimer();
            btnVerify.setEnabled(true);
        } else {
            JOptionPane.showMessageDialog(this, 
                "Gagal mengirim email. Silakan coba lagi.", 
                "Error", JOptionPane.ERROR_MESSAGE);
        }
        
        // Aktifkan kembali tombol kirim ulang setelah 5 detik
        new Timer(5000, e -> {
            btnResend.setEnabled(true);
            ((Timer)e.getSource()).stop();
        }).start();
    }
    
    private void cancelVerification() {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
        
        int option = JOptionPane.showConfirmDialog(this,
            "Apakah Anda yakin ingin membatalkan verifikasi?\nData pendaftaran Anda tidak akan disimpan.",
            "Konfirmasi", JOptionPane.YES_NO_OPTION);
            
        if (option == JOptionPane.YES_OPTION) {
            // Hapus data pengguna sementara dari database
            OTPManager.getInstance().cancelVerification(email);
            System.out.println("Verification cancelled for email: " + email);
            new RegisterUI().setVisible(true);
            dispose();
        }
    }
    
    private void handleWindowClosing() {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
        
        int option = JOptionPane.showConfirmDialog(this,
            "Apakah Anda yakin ingin keluar?\nData pendaftaran Anda tidak akan disimpan.",
            "Konfirmasi Keluar", JOptionPane.YES_NO_OPTION);
            
        if (option == JOptionPane.YES_OPTION) {
            // Hapus data pengguna sementara dari database
            OTPManager.getInstance().cancelVerification(email);
            System.out.println("Window closed, temporary data removed for email: " + email);
            new RegisterUI().setVisible(true);
            dispose();
        }
    }
}