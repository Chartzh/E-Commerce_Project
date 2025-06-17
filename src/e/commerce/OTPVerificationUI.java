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
        // Mengembalikan ukuran jendela ke 1000x600, yang lebih sesuai dengan leftPanel 600x600 (rasio 1:1)
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // IconUtil.setIcon(this); // Pastikan kelas IconUtil ada dan berfungsi

        // Main panel
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(new Color(255, 102, 51)); // Orange background

        // Left panel with OTP verification image
        JPanel leftPanel = new JPanel() {
            private Image bufferedImage;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                try {
                    ImageIcon otpIcon = new ImageIcon(getClass().getResource("/Resources/Images/otp_verification.png"));
                    Image originalImage = otpIcon.getImage();

                    if (originalImage == null || originalImage.getWidth(null) == -1) {
                        throw new Exception("Failed to load OTP verification image or image is empty.");
                    }

                    int panelWidth = getWidth();
                    int panelHeight = getHeight();
                    int imageWidth = panelWidth;
                    int imageHeight = panelHeight;
                    int x = (panelWidth - imageWidth) / 2; 
                    int y = (panelHeight - imageHeight) / 2; 
                    
                    // Skalakan gambar
                    if (bufferedImage == null || bufferedImage.getWidth(null) != imageWidth || bufferedImage.getHeight(null) != imageHeight) {
                        bufferedImage = originalImage.getScaledInstance(imageWidth, imageHeight, Image.SCALE_SMOOTH);
                    }
                    
                    g.drawImage(bufferedImage, x, y, this);
                    
                } catch (Exception e) {
                    System.err.println("Error loading or scaling image: " + e.getMessage());
                    g.setColor(new Color(255, 102, 51));
                    g.fillRect(0, 0, getWidth(), getHeight());
                    g.setColor(Color.WHITE);
                    g.setFont(new Font("Arial", Font.BOLD, 16));
                    String msg1 = "VERIFIKASI OTP";
                    String msg2 = "OTP Verification Image Not Found";
                    String msg3 = "(/Resources/Images/otp_verification.png)";
                    FontMetrics fm = g.getFontMetrics();
                    g.drawString(msg1, (getWidth() - fm.stringWidth(msg1)) / 2, getHeight() / 2 - fm.getHeight());
                    g.drawString(msg2, (getWidth() - fm.stringWidth(msg2)) / 2, getHeight() / 2);
                    g.drawString(msg3, (getWidth() - fm.stringWidth(msg3)) / 2, getHeight() / 2 + fm.getHeight());
                    return;
                }
            }
        };
        leftPanel.setPreferredSize(new Dimension(600, 600));
        leftPanel.setBackground(new Color(255, 102, 51));

        JPanel rightPanel = new JPanel();
        rightPanel.setPreferredSize(new Dimension(400, 600));
        rightPanel.setBackground(Color.WHITE);
        rightPanel.setLayout(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createEmptyBorder(40, 50, 40, 50));

        // Header panel
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(Color.WHITE);

        JLabel lblTitle = new JLabel("VERIFIKASI EMAIL");
        lblTitle.setFont(new Font("Arial", Font.BOLD, 28));
        lblTitle.setForeground(new Color(255, 102, 51));
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblInstruction = new JLabel("<html><center>Kode OTP telah dikirim ke email:<br><b>" + email + "</b></center></html>");
        lblInstruction.setFont(new Font("Arial", Font.PLAIN, 13));
        lblInstruction.setForeground(Color.GRAY);
        lblInstruction.setAlignmentX(Component.CENTER_ALIGNMENT);

        lblTimer = new JLabel("Waktu tersisa: 05:00");
        lblTimer.setFont(new Font("Arial", Font.BOLD, 16));
        lblTimer.setForeground(Color.RED);
        lblTimer.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(lblTitle);
        headerPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        headerPanel.add(lblInstruction);
        headerPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        headerPanel.add(lblTimer);

        // Mengatur komponen tambahan di leftPanel
        JPanel leftPanelContent = new JPanel();
        leftPanelContent.setLayout(new BoxLayout(leftPanelContent, BoxLayout.Y_AXIS));
        leftPanelContent.setOpaque(false); // Agar latar belakang leftPanel terlihat
        leftPanelContent.setBorder(BorderFactory.createEmptyBorder(100, 0, 0, 0)); // Padding atas
      

        // Tambahkan komponen leftPanelContent ke leftPanel
        leftPanel.setLayout(new BorderLayout()); 
        leftPanel.add(leftPanelContent, BorderLayout.NORTH); 

        // Form panel
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(Color.WHITE);

        JLabel lblOTP = new JLabel("Masukkan kode OTP:");
        lblOTP.setFont(new Font("Arial", Font.PLAIN, 16));
        lblOTP.setForeground(new Color(255, 102, 51));
        lblOTP.setAlignmentX(Component.CENTER_ALIGNMENT);

        txtOTP = new JTextField();
        txtOTP.setFont(new Font("Arial", Font.BOLD, 20));
        txtOTP.setHorizontalAlignment(JTextField.CENTER);
        txtOTP.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 2),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        txtOTP.setBackground(new Color(230, 230, 230));
        txtOTP.setForeground(Color.BLACK);
        txtOTP.setCaretColor(Color.BLACK);
        txtOTP.setMaximumSize(new Dimension(300, 50));
        txtOTP.setPreferredSize(new Dimension(300, 50));
        txtOTP.setAlignmentX(Component.CENTER_ALIGNMENT);

        formPanel.add(lblOTP);
        formPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        formPanel.add(txtOTP);

        // Button panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setBackground(Color.WHITE);

        btnVerify = new JButton("Verifikasi");
        btnVerify.setFont(new Font("Arial", Font.BOLD, 16));
        btnVerify.setBackground(new Color(255, 102, 51));
        btnVerify.setForeground(Color.WHITE);
        btnVerify.setFocusPainted(false);
        btnVerify.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnVerify.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        btnVerify.setMaximumSize(new Dimension(300, 50));
        btnVerify.setPreferredSize(new Dimension(300, 50));
        btnVerify.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel secondaryButtonPanel = new JPanel();
        secondaryButtonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 0));
        secondaryButtonPanel.setBackground(Color.WHITE);
        secondaryButtonPanel.setMaximumSize(new Dimension(300, 50));

        btnResend = new JButton("Kirim Ulang");
        btnResend.setFont(new Font("Arial", Font.PLAIN, 14));
        btnResend.setBackground(Color.WHITE);
        btnResend.setForeground(new Color(255, 102, 51));
        btnResend.setFocusPainted(false);
        btnResend.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnResend.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(255, 102, 51), 1),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));

        btnCancel = new JButton("Batal");
        btnCancel.setFont(new Font("Arial", Font.PLAIN, 14));
        btnCancel.setBackground(new Color(240, 240, 240));
        btnCancel.setForeground(Color.GRAY);
        btnCancel.setFocusPainted(false);
        btnCancel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCancel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        secondaryButtonPanel.add(btnResend);
        secondaryButtonPanel.add(btnCancel);

        buttonPanel.add(btnVerify);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        buttonPanel.add(secondaryButtonPanel);

        rightPanel.add(headerPanel, BorderLayout.NORTH);
        rightPanel.add(formPanel, BorderLayout.CENTER);
        rightPanel.add(buttonPanel, BorderLayout.SOUTH);

        mainPanel.add(leftPanel, BorderLayout.WEST);
        mainPanel.add(rightPanel, BorderLayout.EAST);

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

        // Meminta fokus ke field OTP saat jendela dibuka
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                SwingUtilities.invokeLater(() -> txtOTP.requestFocusInWindow());
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
                btnVerify.setEnabled(false);
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
        try {
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

                // Update data pengguna
                try {
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
                } catch (RuntimeException e) {
                    JOptionPane.showMessageDialog(this,
                        "Gagal menyimpan data pengguna: " + e.getMessage() + ". Periksa konfigurasi database.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this,
                    "Kode OTP tidak valid.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this,
                "Gagal memverifikasi OTP: " + e.getMessage() + ". Periksa konfigurasi database.",
                "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void resendOTP() {
        // Nonaktifkan tombol kirim ulang sementara
        btnResend.setEnabled(false);

        // Generate new OTP and send email
        try {
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
                    "Gagal mengirim email. Periksa koneksi internet atau pengaturan email.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this,
                "Gagal mengirim ulang OTP: " + e.getMessage() + ". Periksa konfigurasi database atau tabel verifications.",
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
            try {
                OTPManager.getInstance().cancelVerification(email);
                System.out.println("Verification cancelled for email: " + email);
                new RegisterUI().setVisible(true);
                dispose();
            } catch (RuntimeException e) {
                JOptionPane.showMessageDialog(this,
                    "Gagal membatalkan verifikasi: " + e.getMessage() + ". Periksa konfigurasi database.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
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
            try {
                OTPManager.getInstance().cancelVerification(email);
                System.out.println("Window closed, temporary data removed for email: " + email);
                new RegisterUI().setVisible(true);
                dispose();
            } catch (RuntimeException e) {
                JOptionPane.showMessageDialog(this,
                    "Gagal menghapus data sementara: " + e.getMessage() + ". Periksa konfigurasi database.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}