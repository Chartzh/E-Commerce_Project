package e.commerce;

import java.awt.Image;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

public class User {
    private int id;
    private String username;
    private String password;
    private String email;
    private String nik;
    private String address;
    private String phone;
    private byte[] profileImage;
    private String role;

    public User() {
    }

    public User(String username, String password, String email, String nik, String address, String phone, String role) {
        this.username = username;
        this.password = password; // Password diharapkan sudah di-hash
        this.email = email;
        this.nik = nik;
        this.address = address;
        this.phone = phone;
        this.role = role;
    }

    // Getters dan Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getNik() {
        return nik;
    }

    public void setNik(String nik) {
        this.nik = nik;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public byte[] getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(byte[] profileImage) {
        this.profileImage = profileImage;
    }
    
    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public ImageIcon getProfileImageIcon() {
        if (profileImage != null) {
            try {
                Image img = ImageIO.read(new java.io.ByteArrayInputStream(profileImage));
                return new ImageIcon(img);
            } catch (IOException e) {
                System.err.println("Error converting profile image: " + e.getMessage());
            }
        }
        return new ImageIcon(getClass().getResource("/resources/default_profile.png"));
    }

    // Method untuk menyimpan user baru ke database
    public boolean register() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Cek apakah email sudah ada (data sementara)
            String checkSql = "SELECT email FROM users WHERE email = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setString(1, email);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                // Email sudah ada, update data
                String sql = "UPDATE users SET username = ?, password = ?, nik = ?, address = ?, phone = ?, role = ?, otp_code = NULL, otp_expires_at = NULL WHERE email = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, username);
                stmt.setString(2, password);
                stmt.setString(3, nik);
                stmt.setString(4, address);
                stmt.setString(5, phone);
                stmt.setString(6, role);
                stmt.setString(7, email);
                int affectedRows = stmt.executeUpdate();
                return affectedRows > 0;
            } else {
                // Email belum ada (seharusnya tidak terjadi karena data sementara sudah dimasukkan)
                System.err.println("Email " + email + " tidak ditemukan di tabel users saat registrasi.");
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Error saat registrasi user: " + e.getSQLState() + " - " + e.getErrorCode() + " - " + e.getMessage());
            return false;
        }
    }
    
    // Method untuk update profil user
    public boolean updateProfile() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "UPDATE users SET email = ?, nik = ?, address = ?, phone = ? WHERE username = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, email);
            pstmt.setString(2, nik);
            pstmt.setString(3, address);
            pstmt.setString(4, phone);
            pstmt.setString(5, username);
            
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error saat update profil: " + e.getSQLState() + " - " + e.getErrorCode() + " - " + e.getMessage());
            return false;
        }
    }
    
    // Method untuk update foto profil
    public boolean updateProfileImage(File imageFile) {
        try (Connection conn = DatabaseConnection.getConnection();
             FileInputStream fis = new FileInputStream(imageFile)) {
            // Convert image file to byte array
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            for (int readNum; (readNum = fis.read(buf)) != -1;) {
                bos.write(buf, 0, readNum);
            }
            profileImage = bos.toByteArray();
            
            String sql = "UPDATE users SET profile_image = ? WHERE username = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setBytes(1, profileImage);
            pstmt.setString(2, username);
            
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (IOException | SQLException e) {
            System.err.println("Error saat update foto profil: " + e.getMessage());
            return false;
        }
    }
    
    // Method untuk load user data dari database berdasarkan username
    public boolean loadUserByUsername(String username) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT * FROM users WHERE username = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                this.id = rs.getInt("id");
                this.username = rs.getString("username");
                this.password = rs.getString("password");
                this.email = rs.getString("email");
                this.nik = rs.getString("nik");
                this.address = rs.getString("address");
                this.phone = rs.getString("phone");
                this.role = rs.getString("role");
                
                Blob blob = rs.getBlob("profile_image");
                if (blob != null) {
                    this.profileImage = blob.getBytes(1, (int) blob.length());
                }
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Error saat load user: " + e.getSQLState() + " - " + e.getErrorCode() + " - " + e.getMessage());
            return false;
        }
    }
}