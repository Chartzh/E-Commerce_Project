import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class RegisterUI extends JFrame {
    private JTextField txtUsername, txtEmail, txtNik, txtNotelp;
    private JPasswordField txtPassword, txtConfirmPassword;
    private JTextArea txtAlamat;
    private JButton btnRegister, btnCancel;
    
    public RegisterUI() {
        setTitle("E-Commerce App - Register");
        setSize(500, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        
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
        formPanel.setLayout(new GridLayout(7, 2, 10, 10));
        
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
        
        JLabel lblNik = new JLabel("NIK:");
        lblNik.setFont(new Font("Arial", Font.PLAIN, 14));
        txtNik = new JTextField();
        
        JLabel lblAlamat = new JLabel("Alamat:");
        lblAlamat.setFont(new Font("Arial", Font.PLAIN, 14));
        txtAlamat = new JTextArea();
        txtAlamat.setLineWrap(true);
        JScrollPane scrollAlamat = new JScrollPane(txtAlamat);
        
        JLabel lblNotelp = new JLabel("No. Telepon:");
        lblNotelp.setFont(new Font("Arial", Font.PLAIN, 14));
        txtNotelp = new JTextField();
        
        formPanel.add(lblUsername);
        formPanel.add(txtUsername);
        formPanel.add(lblPassword);
        formPanel.add(txtPassword);
        formPanel.add(lblConfirmPassword);
        formPanel.add(txtConfirmPassword);
        formPanel.add(lblEmail);
        formPanel.add(txtEmail);
        formPanel.add(lblNik);
        formPanel.add(txtNik);
        formPanel.add(lblAlamat);
        formPanel.add(scrollAlamat);
        formPanel.add(lblNotelp);
        formPanel.add(txtNotelp);
        
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
        btnRegister.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                register();
            }
        });
        
        btnCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                LoginUI loginUI = new LoginUI();
                loginUI.setVisible(true);
                dispose();
            }
        });
    }
    
    private void register() {
        String username = txtUsername.getText();
        String password = new String(txtPassword.getPassword());
        String confirmPassword = new String(txtConfirmPassword.getPassword());
        String email = txtEmail.getText();
        String nik = txtNik.getText();
        String alamat = txtAlamat.getText();
        String notelp = txtNotelp.getText();
        
        // Validasi input
        if (username.isEmpty() || password.isEmpty() || email.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username, password, dan email harus diisi!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "Password dan konfirmasi password tidak cocok!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Validasi email format
        if (!email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
            JOptionPane.showMessageDialog(this, "Format email tidak valid!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Cek username dan email tersedia
        if (!Authentication.isUsernameAvailable(username)) {
            JOptionPane.showMessageDialog(this, "Username sudah digunakan!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (!Authentication.isEmailAvailable(email)) {
            JOptionPane.showMessageDialog(this, "Email sudah digunakan!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Buat objek user baru dan register ke database
        User user = new User(username, password, email, nik, alamat, notelp, "user");
        boolean success = user.register();
        
        if (success) {
            JOptionPane.showMessageDialog(this, "Registrasi berhasil! Silahkan login.", "Success", JOptionPane.INFORMATION_MESSAGE);
            LoginUI loginUI = new LoginUI();
            loginUI.setVisible(true);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Registrasi gagal! Silahkan coba lagi.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}