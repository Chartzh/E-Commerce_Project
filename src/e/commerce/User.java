package e.commerce;

import java.awt.Image;
import java.io.IOException;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
// import java.sql.Statement; // [Perbaikan]: Tidak diperlukan lagi jika register() dihapus
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.io.ByteArrayInputStream;

public class User {
    private int id;
    private String username;
    private String password; // Ini adalah hash password
    private String email;
    private String nik;
    private String phone;
    private byte[] profilePicture;
    private byte[] bannerPicture;
    private String role;
    private boolean isVerified;

    public User() { //
        this.profilePicture = null; //
        this.bannerPicture = null; //
        this.isVerified = false; //
    }

    // Constructor yang digunakan untuk pendaftaran awal di RegisterUI
    public User(String username, String password, String email) { //
        this.username = username; //
        this.password = password; //
        this.email = email; //
        this.role = "user"; // Default role untuk registrasi
        this.profilePicture = null; //
        this.bannerPicture = null; //
        this.isVerified = false; //
    }

    // Constructor yang digunakan untuk membuat dummy user di DatabaseConnection atau Authentication
    public User(String username, String password, String email, String role, boolean isVerified) { //
        this.username = username; //
        this.password = password; //
        this.email = email; //
        this.role = role; //
        this.profilePicture = null; //
        this.bannerPicture = null; //
        this.isVerified = isVerified; //
    }

    // Constructor lengkap (untuk mengambil dari DB)
    public User(int id, String username, String password, String email, String nik, String phone, byte[] profilePicture, byte[] bannerPicture, String role, boolean isVerified) { //
        this.id = id; //
        this.username = username; //
        this.password = password; //
        this.email = email; //
        this.nik = nik; //
        this.phone = phone; //
        this.profilePicture = profilePicture; //
        this.bannerPicture = bannerPicture; //
        this.role = role; //
        this.isVerified = isVerified; //
    }

    // --- Getters dan Setters ---
    public int getId() { return id; } //
    public void setId(int id) { this.id = id; } //
    public String getUsername() { return username; } //
    public void setUsername(String username) { this.username = username; } //
    public String getPassword() { return password; } //
    public void setPassword(String password) { this.password = password; } //
    public String getEmail() { return email; } //
    public void setEmail(String email) { this.email = email; } //
    public String getNik() { return nik; } //
    public void setNik(String nik) { this.nik = nik; } //
    public String getPhone() { return phone; } //
    public void setPhone(String phone) { this.phone = phone; } //
    public byte[] getProfilePicture() { return profilePicture; } //
    public void setProfilePicture(byte[] profilePicture) { this.profilePicture = profilePicture; } //
    public byte[] getBannerPicture() { return bannerPicture; } //
    public void setBannerPicture(byte[] bannerPicture) { this.bannerPicture = bannerPicture; } //
    public String getRole() { return role; } //
    public void setRole(String role) { this.role = role; } //

    public boolean isVerified() { //
        return isVerified; //
    }
    public void setVerified(boolean verified) { //
        isVerified = verified; //
    }

    public ImageIcon getProfileImageIcon() { 
        if (profilePicture != null) { 
            try (ByteArrayInputStream bis = new ByteArrayInputStream(profilePicture)) { 
                Image img = ImageIO.read(bis); 
                if (img != null && img.getWidth(null) > 0 && img.getHeight(null) > 0) { // <--- TAMBAHKAN PENGECEKAN INI
                    return new ImageIcon(img); 
                } else {
                    System.err.println("Warning: Profile picture is invalid or has zero dimensions.");
                }
            } catch (IOException e) { 
                System.err.println("Error converting profile image: " + e.getMessage()); 
            }
        }
        // Pastikan path ke default_profile.png benar
        return new ImageIcon(getClass().getResource("/resources/default_profile.png")); 
    }

    public ImageIcon getBannerImageIcon() { 
        if (bannerPicture != null) { 
            try (ByteArrayInputStream bis = new ByteArrayInputStream(bannerPicture)) { 
                Image img = ImageIO.read(bis); 
                if (img != null && img.getWidth(null) > 0 && img.getHeight(null) > 0) { // <--- TAMBAHKAN PENGECEKAN INI
                    return new ImageIcon(img); 
                } else {
                    System.err.println("Warning: Banner picture is invalid or has zero dimensions.");
                }
            } catch (IOException e) { 
                System.err.println("Error converting banner image: " + e.getMessage()); 
            }
        }
        return new ImageIcon(getClass().getResource("/resources/default_banner.png")); 
    }

    public boolean loadUserById(int userId) { //
        try (Connection conn = DatabaseConnection.getConnection()) { //
            String sql = "SELECT u.id, u.username, u.password, u.email, u.role, u.is_verified, " + //
                         "p.nik, p.phone, p.profile_picture, p.banner_picture " + //
                         "FROM users u " + //
                         "LEFT JOIN profile p ON u.id = p.user_id " + //
                         "WHERE u.id = ?"; //
            PreparedStatement pstmt = conn.prepareStatement(sql); //
            pstmt.setInt(1, userId); //
            ResultSet rs = pstmt.executeQuery(); //

            if (rs.next()) { //
                this.id = rs.getInt("id"); //
                this.username = rs.getString("username"); //
                this.password = rs.getString("password"); //
                this.email = rs.getString("email"); //
                this.role = rs.getString("role"); //
                this.isVerified = rs.getBoolean("is_verified"); //

                this.nik = rs.getString("nik"); //
                this.phone = rs.getString("phone"); //

                Blob profileBlob = rs.getBlob("profile_picture"); //
                if (profileBlob != null) { //
                    this.profilePicture = profileBlob.getBytes(1, (int) profileBlob.length()); //
                    profileBlob.free(); //
                } else { //
                    this.profilePicture = null; //
                }

                Blob bannerBlob = rs.getBlob("banner_picture"); //
                if (bannerBlob != null) { //
                    this.bannerPicture = bannerBlob.getBytes(1, (int) bannerBlob.length()); //
                    bannerBlob.free(); //
                } else { //
                    this.bannerPicture = null; //
                }

                System.out.println("Loaded user data for ID: " + userId); //
                return true; //
            }
            System.err.println("User not found for ID: " + userId); //
            return false; //
        } catch (SQLException e) { //
            System.err.println("Error saat load user: " + e.getSQLState() + " - " + e.getErrorCode() + " - " + e.getMessage()); //
            throw new RuntimeException("Gagal memuat data pengguna: " + e.getMessage()); //
        }
    }
}