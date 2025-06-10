package e.commerce;

import java.awt.Image;
import java.io.IOException;
import java.sql.Blob;
import java.sql.Connection; // Tetap diperlukan karena ada metode DB di sini
import java.sql.PreparedStatement; // Tetap diperlukan
import java.sql.ResultSet; // Tetap diperlukan
import java.sql.SQLException; // Tetap diperlukan
import java.sql.Statement; // Tetap diperlukan
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.io.ByteArrayInputStream;

public class User {
    private int id;
    private String username;
    private String password; // Ini adalah hash password
    private String email;
    private String nik;
    private String address;
    private String phone;
    private String city;
    private String province;
    private String postalCode;
    private String country;
    private byte[] profilePicture;
    private byte[] bannerPicture;
    private String role;
    private boolean isVerified; // <--- FIELD BARU: isVerified


    public User() {
        this.profilePicture = null;
        this.bannerPicture = null;
        this.isVerified = false; // <--- INISIALISASI DEFAULT
    }

    // Constructor yang Anda berikan: User(String username, String password, String email)
    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.role = "user";
        this.profilePicture = null;
        this.bannerPicture = null;
        this.isVerified = false; // <--- Inisialisasi isVerified
    }

    // Constructor yang Anda berikan: User(String username, String password, String email, String nik, ..., String country, String role)
    public User(String username, String password, String email, String nik, String address, String phone, String city, String province, String postalCode, String country, String role, boolean isVerified) { // <--- MENAMBAHKAN 'isVerified' di parameter
        this.username = username;
        this.password = password;
        this.email = email;
        this.nik = nik;
        this.address = address;
        this.phone = phone;
        this.city = city;
        this.province = province;
        this.postalCode = postalCode;
        this.country = country;
        this.role = role;
        this.profilePicture = null;
        this.bannerPicture = null;
        this.isVerified = isVerified; // <--- INISIALISASI 'isVerified'
    }

    // Constructor lengkap (jika Anda memuat semua data sekaligus dari DB)
    public User(int id, String username, String password, String email, String nik, String address, String phone, String city, String province, String postalCode, String country, byte[] profilePicture, byte[] bannerPicture, String role, boolean isVerified) { // <--- MENAMBAHKAN 'isVerified' di parameter
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.nik = nik;
        this.address = address;
        this.phone = phone;
        this.city = city;
        this.province = province;
        this.postalCode = postalCode;
        this.country = country;
        this.profilePicture = profilePicture;
        this.bannerPicture = bannerPicture;
        this.role = role;
        this.isVerified = isVerified; // <--- INISIALISASI 'isVerified'
    }

    // --- Getters dan Setters ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getNik() { return nik; }
    public void setNik(String nik) { this.nik = nik; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getProvince() { return province; }
    public void setProvince(String province) { this.province = province; }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public byte[] getProfilePicture() { return profilePicture; }
    public void setProfilePicture(byte[] profilePicture) { this.profilePicture = profilePicture; }
    public byte[] getBannerPicture() { return bannerPicture; }
    public void setBannerPicture(byte[] bannerPicture) { this.bannerPicture = bannerPicture; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    
    // --- GETTER DAN SETTER BARU UNTUK 'isVerified' ---
    public boolean isVerified() {
        return isVerified;
    }
    public void setVerified(boolean verified) {
        isVerified = verified;
    }
    // --- AKHIR PENAMBAHAN 'isVerified' ---


    // Helper method untuk mendapatkan ImageIcon dari profilePicture
    public ImageIcon getProfileImageIcon() {
        if (profilePicture != null) {
            try (ByteArrayInputStream bis = new ByteArrayInputStream(profilePicture)) {
                Image img = ImageIO.read(bis);
                return new ImageIcon(img);
            } catch (IOException e) {
                System.err.println("Error converting profile image: " + e.getMessage());
            }
        }
        return new ImageIcon(getClass().getResource("/resources/default_profile.png")); 
    }

    // Helper method untuk mendapatkan ImageIcon dari bannerPicture
    public ImageIcon getBannerImageIcon() {
        if (bannerPicture != null) {
            try (ByteArrayInputStream bis = new ByteArrayInputStream(bannerPicture)) {
                Image img = ImageIO.read(bis);
                return new ImageIcon(img);
            } catch (IOException e) {
                System.err.println("Error converting banner image: " + e.getMessage());
            }
        }
        return new ImageIcon(getClass().getResource("/resources/default_banner.png")); 
    }

    // --- Metode-metode Database (Asumsi Anda ingin ini tetap ada di User.java) ---
    // register() method di User.java
    public boolean register() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String checkSql = "SELECT id FROM users WHERE email = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setString(1, email);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                System.err.println("Registrasi gagal: Email " + email + " sudah terdaftar.");
                return false;
            } else {
                String insertUserSql = "INSERT INTO users (username, password, email, role, is_verified) VALUES (?, ?, ?, ?, ?)";
                PreparedStatement insertUserStmt = conn.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS);
                insertUserStmt.setString(1, username);
                insertUserStmt.setString(2, password); // Password diharapkan sudah di-hash
                insertUserStmt.setString(3, email);
                insertUserStmt.setString(4, role); 
                insertUserStmt.setBoolean(5, isVerified); // Gunakan 'isVerified' dari objek User

                int affectedRows = insertUserStmt.executeUpdate();
                if (affectedRows > 0) {
                    int newlyGeneratedUserId = -1;
                    try (ResultSet generatedKeys = insertUserStmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            newlyGeneratedUserId = generatedKeys.getInt(1);
                            this.id = newlyGeneratedUserId;
                        }
                    }

                    if (newlyGeneratedUserId != -1) {
                        String insertProfileSql = "INSERT INTO profile (user_id, nik, address, phone, city, province, postal_code, country, profile_picture, banner_picture) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                        PreparedStatement insertProfileStmt = conn.prepareStatement(insertProfileSql);
                        insertProfileStmt.setInt(1, newlyGeneratedUserId);
                        insertProfileStmt.setNull(2, java.sql.Types.VARCHAR); // nik
                        insertProfileStmt.setNull(3, java.sql.Types.VARCHAR); // address
                        insertProfileStmt.setNull(4, java.sql.Types.VARCHAR); // phone
                        insertProfileStmt.setNull(5, java.sql.Types.VARCHAR); // city
                        insertProfileStmt.setNull(6, java.sql.Types.VARCHAR); // province
                        insertProfileStmt.setNull(7, java.sql.Types.VARCHAR); // postal_code
                        insertProfileStmt.setNull(8, java.sql.Types.VARCHAR); // country
                        insertProfileStmt.setNull(9, java.sql.Types.BLOB);    // profile_picture
                        insertProfileStmt.setNull(10, java.sql.Types.BLOB);    // banner_picture

                        int profileAffectedRows = insertProfileStmt.executeUpdate();
                        if (profileAffectedRows > 0) {
                            System.out.println("Registrasi berhasil untuk email: " + email + " dan profil dibuat.");
                            return true;
                        } else {
                            System.err.println("Registrasi pengguna berhasil, tetapi gagal membuat entri profil untuk ID: " + newlyGeneratedUserId);
                            return false;
                        }
                    } else {
                        System.err.println("Registrasi gagal: Gagal mendapatkan ID pengguna baru.");
                        return false;
                    }
                } else {
                    System.err.println("Registrasi gagal: Tidak ada baris yang terpengaruh di tabel users.");
                    return false;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error saat registrasi user: " + e.getSQLState() + " - " + e.getErrorCode() + " - " + e.getMessage());
            throw new RuntimeException("Gagal menyimpan data pengguna (registrasi): " + e.getMessage());
        }
    }
    
    // loadUserById method diubah untuk melakukan JOIN ke tabel 'profile'
    public boolean loadUserById(int userId) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT u.id, u.username, u.password, u.email, u.role, u.is_verified, " + // MENAMBAH u.is_verified
                         "p.nik, p.address, p.phone, p.city, p.province, p.postal_code, p.country, " +
                         "p.profile_picture, p.banner_picture " +
                         "FROM users u " +
                         "LEFT JOIN profile p ON u.id = p.user_id " +
                         "WHERE u.id = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                this.id = rs.getInt("id");
                this.username = rs.getString("username");
                this.password = rs.getString("password");
                this.email = rs.getString("email");
                this.role = rs.getString("role");
                this.isVerified = rs.getBoolean("is_verified"); // MENGAMBIL NILAI isVerified dari DB
                
                this.nik = rs.getString("nik");
                this.address = rs.getString("address");
                this.phone = rs.getString("phone");
                this.city = rs.getString("city");
                this.province = rs.getString("province");
                this.postalCode = rs.getString("postal_code");
                this.country = rs.getString("country");
                
                Blob profileBlob = rs.getBlob("profile_picture");
                if (profileBlob != null) {
                    this.profilePicture = profileBlob.getBytes(1, (int) profileBlob.length());
                    profileBlob.free();
                } else {
                    this.profilePicture = null;
                }

                Blob bannerBlob = rs.getBlob("banner_picture");
                if (bannerBlob != null) {
                    this.bannerPicture = bannerBlob.getBytes(1, (int) bannerBlob.length());
                    bannerBlob.free();
                } else {
                    this.bannerPicture = null;
                }

                System.out.println("Loaded user data for ID: " + userId);
                return true;
            }
            System.err.println("User not found for ID: " + userId);
            return false;
        } catch (SQLException e) {
            System.err.println("Error saat load user: " + e.getSQLState() + " - " + e.getErrorCode() + " - " + e.getMessage());
            throw new RuntimeException("Gagal memuat data pengguna: " + e.getMessage());
        }
    }
}