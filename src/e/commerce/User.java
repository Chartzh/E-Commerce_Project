package e.commerce;

import java.awt.Image;
import java.io.IOException;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement; // Import untuk Statement.RETURN_GENERATED_KEYS
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

public class User {
    private int id;
    private String username; // Nama Lengkap
    private String password;
    private String email; // Alamat Email
    private String nik;
    private String address; // Alamat Lengkap
    private String phone; // Nomor Telepon
    private String city; // Kota/Kabupaten
    private String province; // Provinsi
    private String postalCode; // Kode Pos
    private String country; // Negara
    private byte[] profilePicture;
    private byte[] bannerPicture;
    private String role;

    public User() {
        this.profilePicture = null;
        this.bannerPicture = null;
    }

    // NEW: Constructor untuk registrasi (hanya username, password, email)
    public User(String username, String password, String email) {
        this.username = username;
        this.password = password; // Password diharapkan sudah di-hash
        this.email = email;
        this.role = "user"; // Default role untuk pengguna baru
        this.profilePicture = null;
        this.bannerPicture = null;
        // Kolom lainnya (nik, address, phone, city, province, postalCode, country) akan null secara default di DB
    }

    // Constructor yang menerima data dasar (tanpa gambar dan ID) - dipertahankan jika masih diperlukan
    public User(String username, String password, String email, String nik, String address, String phone, String city, String province, String postalCode, String country, String role) {
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
    }

    // Constructor lengkap (jika Anda memuat semua data sekaligus dari DB)
    public User(int id, String username, String password, String email, String nik, String address, String phone, String city, String province, String postalCode, String country, byte[] profilePicture, byte[] bannerPicture, String role) {
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
    }

    // --- Getters dan Setters (Tidak ada perubahan di sini) ---
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

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public byte[] getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(byte[] profilePicture) {
        this.profilePicture = profilePicture;
    }

    public byte[] getBannerPicture() {
        return bannerPicture;
    }

    public void setBannerPicture(byte[] bannerPicture) {
        this.bannerPicture = bannerPicture;
    }
    
    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    // Helper method untuk mendapatkan ImageIcon dari profilePicture
    public ImageIcon getProfileImageIcon() {
        if (profilePicture != null) {
            try {
                Image img = ImageIO.read(new java.io.ByteArrayInputStream(profilePicture));
                return new ImageIcon(img);
            } catch (IOException e) {
                System.err.println("Error converting profile image: " + e.getMessage());
            }
        }
        // Pastikan path default_profile.png sudah benar
        return new ImageIcon(getClass().getResource("/resources/default_profile.png"));
    }

    // Helper method untuk mendapatkan ImageIcon dari bannerPicture
    public ImageIcon getBannerImageIcon() {
        if (bannerPicture != null) {
            try {
                Image img = ImageIO.read(new java.io.ByteArrayInputStream(bannerPicture));
                return new ImageIcon(img);
            } catch (IOException e) {
                System.err.println("Error converting banner image: " + e.getMessage());
            }
        }
        // Pastikan path default_banner.png sudah benar jika ingin ada defaultnya
        return new ImageIcon(getClass().getResource("/resources/default_banner.png")); 
    }

    // --- Metode-metode Database ---

    // Metode register() diubah menjadi operasi INSERT ke users dan profile
    public boolean register() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // 1. Cek apakah email sudah terdaftar
            String checkSql = "SELECT id FROM users WHERE email = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setString(1, email);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                // Email sudah terdaftar, registrasi gagal
                System.err.println("Registrasi gagal: Email " + email + " sudah terdaftar.");
                return false;
            } else {
                // Email belum terdaftar, lakukan INSERT pengguna baru ke tabel 'users'
                // Gunakan RETURN_GENERATED_KEYS untuk mendapatkan ID yang dihasilkan
                String insertUserSql = "INSERT INTO users (username, password, email, role, is_verified) VALUES (?, ?, ?, ?, ?)";
                PreparedStatement insertUserStmt = conn.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS);
                insertUserStmt.setString(1, username);
                insertUserStmt.setString(2, password); // Pastikan password sudah di-hash sebelum disimpan
                insertUserStmt.setString(3, email);
                insertUserStmt.setString(4, role); // Menggunakan role yang sudah diset di constructor (misal "user")
                insertUserStmt.setInt(5, 1); // Diasumsikan langsung terverifikasi saat registrasi awal

                int affectedRows = insertUserStmt.executeUpdate();
                if (affectedRows > 0) {
                    // Dapatkan ID pengguna yang baru saja disisipkan
                    int newlyGeneratedUserId = -1;
                    try (ResultSet generatedKeys = insertUserStmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            newlyGeneratedUserId = generatedKeys.getInt(1);
                            this.id = newlyGeneratedUserId; // Set ID ke objek User saat ini
                        }
                    }

                    if (newlyGeneratedUserId != -1) {
                        // Jika berhasil menyisipkan ke 'users', sekarang sisipkan entri kosong ke tabel 'profile'
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
                        insertProfileStmt.setNull(10, java.sql.Types.BLOB);   // banner_picture

                        int profileAffectedRows = insertProfileStmt.executeUpdate();
                        if (profileAffectedRows > 0) {
                            System.out.println("Registrasi berhasil untuk email: " + email + " dan profil dibuat.");
                            return true;
                        } else {
                            System.err.println("Registrasi pengguna berhasil, tetapi gagal membuat entri profil untuk ID: " + newlyGeneratedUserId);
                            // Opsional: Lakukan rollback users table jika profil gagal dibuat
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
            String sql = "SELECT u.id, u.username, u.password, u.email, u.role, u.is_verified, " +
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
                // this.is_verified (jika ada properti di User class)
                
                // Ambil data dari tabel 'profile'
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