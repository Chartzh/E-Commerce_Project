package e.commerce;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.sql.*;

public class ProfileUI extends JPanel {
    private JLabel lblProfilePic;
    private JTextField txtUsername, txtEmail, txtNIK, txtPhone;
    private JTextArea txtAddress;
    private JButton btnUpdate, btnChangePhoto;
    private ImageIcon profileImage;
    private File selectedFile;
    private User currentUser;
    
    public ProfileUI() {
        currentUser = Authentication.getCurrentUser();
        setLayout(new BorderLayout(20, 20));
        setBorder(new EmptyBorder(30, 30, 30, 30));
        setBackground(Color.WHITE);
        
        // Main content panel
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBackground(Color.WHITE);
        
        // Header
        JLabel headerLabel = new JLabel("Profile");
        headerLabel.setFont(new Font("Arial", Font.BOLD, 24));
        headerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Profile Section
        JPanel profilePanel = new JPanel(new BorderLayout(20, 0));
        profilePanel.setBackground(Color.WHITE);
        profilePanel.setBorder(new EmptyBorder(20, 0, 20, 0));
        
        // Photo panel
        JPanel photoPanel = new JPanel(new BorderLayout());
        photoPanel.setBackground(Color.WHITE);
        photoPanel.setPreferredSize(new Dimension(200, 250));
        
        // Profile picture
        lblProfilePic = new JLabel();
        lblProfilePic.setHorizontalAlignment(JLabel.CENTER);
        lblProfilePic.setPreferredSize(new Dimension(180, 180));
        lblProfilePic.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        
        // Load profile picture from database if exists
        loadProfilePicture();
        
        // Change photo button
        btnChangePhoto = new JButton("Change Photo");
        btnChangePhoto.setFont(new Font("Arial", Font.PLAIN, 12));
        btnChangePhoto.setBackground(new Color(240, 240, 240));
        btnChangePhoto.setFocusPainted(false);
        btnChangePhoto.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Select Profile Picture");
                fileChooser.setFileFilter(new FileNameExtensionFilter("Image files", "jpg", "jpeg", "png", "gif"));
                
                int result = fileChooser.showOpenDialog(ProfileUI.this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    selectedFile = fileChooser.getSelectedFile();
                    try {
                        // Resize image for display
                        ImageIcon originalIcon = new ImageIcon(selectedFile.getPath());
                        Image image = originalIcon.getImage().getScaledInstance(180, 180, Image.SCALE_SMOOTH);
                        profileImage = new ImageIcon(image);
                        lblProfilePic.setIcon(profileImage);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(ProfileUI.this, 
                            "Error loading image: " + ex.getMessage(), 
                            "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        
        photoPanel.add(lblProfilePic, BorderLayout.CENTER);
        photoPanel.add(btnChangePhoto, BorderLayout.SOUTH);
        
        // Information panel
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(Color.WHITE);
        
        // Form fields
        JPanel formPanel = new JPanel(new GridLayout(5, 2, 10, 15));
        formPanel.setBackground(Color.WHITE);
        formPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Username
        JLabel lblUsername = new JLabel("Username:");
        lblUsername.setFont(new Font("Arial", Font.BOLD, 14));
        txtUsername = new JTextField(currentUser.getUsername());
        txtUsername.setFont(new Font("Arial", Font.PLAIN, 14));
        txtUsername.setEditable(false); // Username cannot be changed
        
        // Email
        JLabel lblEmail = new JLabel("Email:");
        lblEmail.setFont(new Font("Arial", Font.BOLD, 14));
        txtEmail = new JTextField(currentUser.getEmail());
        txtEmail.setFont(new Font("Arial", Font.PLAIN, 14));
        
        // NIK
        JLabel lblNIK = new JLabel("NIK:");
        lblNIK.setFont(new Font("Arial", Font.BOLD, 14));
        txtNIK = new JTextField(currentUser.getNik());
        txtNIK.setFont(new Font("Arial", Font.PLAIN, 14));
        
        // Phone
        JLabel lblPhone = new JLabel("No. Telepon:");
        lblPhone.setFont(new Font("Arial", Font.BOLD, 14));
        txtPhone = new JTextField(currentUser.getPhone());
        txtPhone.setFont(new Font("Arial", Font.PLAIN, 14));
        
        // Add fields to form panel
        formPanel.add(lblUsername);
        formPanel.add(txtUsername);
        formPanel.add(lblEmail);
        formPanel.add(txtEmail);
        formPanel.add(lblNIK);
        formPanel.add(txtNIK);
        formPanel.add(lblPhone);
        formPanel.add(txtPhone);
        
        // Address (separate because it needs more space)
        JLabel lblAddress = new JLabel("Alamat:");
        lblAddress.setFont(new Font("Arial", Font.BOLD, 14));
        lblAddress.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        txtAddress = new JTextArea(currentUser.getAddress());
        txtAddress.setFont(new Font("Arial", Font.PLAIN, 14));
        txtAddress.setLineWrap(true);
        txtAddress.setWrapStyleWord(true);
        txtAddress.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        
        JScrollPane addressScrollPane = new JScrollPane(txtAddress);
        addressScrollPane.setPreferredSize(new Dimension(0, 100));
        addressScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Update button
        btnUpdate = new JButton("Update Profile");
        btnUpdate.setFont(new Font("Arial", Font.BOLD, 14));
        btnUpdate.setBackground(new Color(0, 123, 255));
        btnUpdate.setForeground(Color.WHITE);
        btnUpdate.setFocusPainted(false);
        btnUpdate.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnUpdate.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateProfile();
            }
        });
        
        // Add components
        infoPanel.add(formPanel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        infoPanel.add(lblAddress);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        infoPanel.add(addressScrollPane);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        infoPanel.add(btnUpdate);
        
        profilePanel.add(photoPanel, BorderLayout.WEST);
        profilePanel.add(infoPanel, BorderLayout.CENTER);
        
        mainPanel.add(headerLabel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        mainPanel.add(profilePanel);
        
        add(mainPanel, BorderLayout.CENTER);
    }
    
    private void loadProfilePicture() {
    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet rs = null;

    try {
        conn = DatabaseConnection.getConnection();
        String query = "SELECT profile_picture FROM users WHERE id = ?";
        stmt = conn.prepareStatement(query);
        stmt.setInt(1, currentUser.getId());
        rs = stmt.executeQuery();

        if (rs.next() && rs.getBytes("profile_picture") != null) {
            byte[] imgData = rs.getBytes("profile_picture");
            ImageIcon originalIcon = new ImageIcon(imgData);
            Image image = originalIcon.getImage().getScaledInstance(180, 180, Image.SCALE_SMOOTH);
            profileImage = new ImageIcon(image);
            lblProfilePic.setIcon(profileImage);
        } else {
            // No profile picture, just leave empty or set placeholder text
            lblProfilePic.setText("No Image");
            lblProfilePic.setHorizontalAlignment(JLabel.CENTER);
            lblProfilePic.setVerticalAlignment(JLabel.CENTER);
        }
    } catch (Exception e) {
        System.err.println("Gagal load gambar profil: " + e.getMessage());
        // Tetap tampilkan UI, jangan crash
        lblProfilePic.setText("No Image");
        lblProfilePic.setHorizontalAlignment(JLabel.CENTER);
        lblProfilePic.setVerticalAlignment(JLabel.CENTER);
    } finally {
        DatabaseConnection.closeConnection(conn, stmt, rs);
    }
}

    
    private void updateProfile() {
        String email = txtEmail.getText().trim();
        String nik = txtNIK.getText().trim();
        String phone = txtPhone.getText().trim();
        String address = txtAddress.getText().trim();
        
        // Validate input
        if (email.isEmpty() || !email.contains("@")) {
            JOptionPane.showMessageDialog(this, "Please enter a valid email address.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (nik.isEmpty() || nik.length() < 16) {
            JOptionPane.showMessageDialog(this, "NIK should be at least 16 digits.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (phone.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter your phone number.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        Connection conn = null;
        PreparedStatement stmt = null;
        
        try {
            conn = DatabaseConnection.getConnection();
            String query;
            
            if (selectedFile != null) {
                // Update profile with new photo
                query = "UPDATE users SET email = ?, nik = ?, phone = ?, address = ?, profile_picture = ? WHERE id = ?";
                stmt = conn.prepareStatement(query);
                
                FileInputStream fis = new FileInputStream(selectedFile);
                stmt.setString(1, email);
                stmt.setString(2, nik);
                stmt.setString(3, phone);
                stmt.setString(4, address);
                stmt.setBinaryStream(5, fis, (int) selectedFile.length());
                stmt.setInt(6, currentUser.getId());
            } else {
                // Update profile without changing photo
                query = "UPDATE users SET email = ?, nik = ?, phone = ?, address = ? WHERE id = ?";
                stmt = conn.prepareStatement(query);
                
                stmt.setString(1, email);
                stmt.setString(2, nik);
                stmt.setString(3, phone);
                stmt.setString(4, address);
                stmt.setInt(5, currentUser.getId());
            }
            
            int result = stmt.executeUpdate();
            
            if (result > 0) {
                // Update the current user object
                currentUser.setEmail(email);
                currentUser.setNik(nik);
                currentUser.setPhone(phone);
                currentUser.setAddress(address);
                
                JOptionPane.showMessageDialog(this, 
                    "Profile updated successfully!", 
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Failed to update profile.", 
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
            
        } catch (SQLException | FileNotFoundException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                "Error updating profile: " + e.getMessage(), 
                "Database Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            DatabaseConnection.closeConnection(conn, stmt, null);
        }
    }
}