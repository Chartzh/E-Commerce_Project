package e.commerce;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.mindrot.jbcrypt.BCrypt;

public class Authentication {
    private static User currentUser = null;

    // Method untuk login
    public static User login(String username, String password) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();

            // Query untuk login: JOIN users dan profile untuk mendapatkan detail user
            // Hapus kolom alamat dari tabel profile
            String sql = "SELECT u.id, u.username, u.email, u.password, u.role, u.is_verified, " +
                         "p.nik, p.phone, p.profile_picture, p.banner_picture " + // 'address', 'city', 'province', 'postal_code', 'country' DIHAPUS DARI SINI
                         "FROM users u LEFT JOIN profile p ON u.id = p.user_id " +
                         "WHERE u.username = ?";

            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                // Ambil data dari ResultSet
                int id = rs.getInt("id");
                String dbUsername = rs.getString("username");
                String dbEmail = rs.getString("email");
                String dbHashedPassword = rs.getString("password");
                String dbRole = rs.getString("role");
                boolean dbIsVerified = rs.getBoolean("is_verified");

                String nik = rs.getString("nik");
                String phone = rs.getString("phone");
                // Kolom alamat tidak lagi diambil dari ResultSet
                // String address = rs.getString("address");
                // String city = rs.getString("city");
                // String province = rs.getString("province");
                // String postalCode = rs.getString("postal_code");
                // String country = rs.getString("country");
                byte[] profilePicture = rs.getBytes("profile_picture");
                byte[] bannerPicture = rs.getBytes("banner_picture");

                boolean isPasswordValid = false;

                // Coba verifikasi dengan BCrypt
                try {
                    isPasswordValid = BCrypt.checkpw(password, dbHashedPassword);
                } catch (IllegalArgumentException e) {
                    System.err.println("Stored password is not a BCrypt hash, trying plain text. Error: " + e.getMessage());
                    if (password.equals(dbHashedPassword)) {
                        isPasswordValid = true;
                        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
                        String updateSql = "UPDATE users SET password = ? WHERE id = ?";
                        try (PreparedStatement updatePstmt = conn.prepareStatement(updateSql)) {
                            updatePstmt.setString(1, hashedPassword);
                            updatePstmt.setInt(2, id);
                            updatePstmt.executeUpdate();
                            System.out.println("Password for " + username + " migrated to BCrypt.");
                        }
                    }
                }

                // Jika password valid (BCrypt atau plain text)
                if (isPasswordValid) {
                    // Buat objek User tanpa kolom alamat
                    // Anda perlu menyesuaikan konstruktor User agar tidak lagi menerima parameter alamat
                    currentUser = new User(id, dbUsername, dbHashedPassword, dbEmail, nik, phone, profilePicture, bannerPicture, dbRole, dbIsVerified);
                    System.out.println("Login berhasil untuk user: " + currentUser.getUsername() + " dengan role: " + currentUser.getRole());
                    return currentUser;
                }
            }
            System.out.println("Login gagal: Username atau password salah.");
            return null;
        } catch (SQLException e) {
            System.err.println("Error saat login: " + e.getSQLState() + " - " + e.getErrorCode() + " - " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            DatabaseConnection.closeConnection(conn, pstmt, rs);
        }
    }

    // Method untuk cek username tersedia
    public static boolean isUsernameAvailable(String username) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            rs = pstmt.executeQuery();

            if (rs.next() && rs.getInt(1) > 0) {
                System.out.println("Username " + username + " sudah terdaftar.");
                return false;
            }
            System.out.println("Username " + username + " tersedia.");
            return true;
        } catch (SQLException e) {
            System.err.println("Database error checking username availability: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            DatabaseConnection.closeConnection(conn, pstmt, rs);
        }
    }

    // Method untuk cek email tersedia
    public static boolean isEmailAvailable(String email) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, email);
            rs = pstmt.executeQuery();

            if (rs.next() && rs.getInt(1) > 0) {
                System.out.println("Email " + email + " sudah terdaftar.");
                return false;
            }
            System.out.println("Email " + email + " tersedia.");
            return true;
        } catch (SQLException e) {
            System.err.println("Database error checking email availability: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            DatabaseConnection.closeConnection(conn, pstmt, rs);
        }
    }

    // Method untuk registrasi user baru
    public static boolean register(String username, String email, String password, String role, boolean isVerified) {
        Connection conn = null;
        PreparedStatement pstmtUser = null;
        PreparedStatement pstmtProfile = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Sisipkan ke tabel users
            String insertUserSql = "INSERT INTO users (username, email, password, role, is_verified) VALUES (?, ?, ?, ?, ?)";
            pstmtUser = conn.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS);

            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
            System.out.println("Generated Hash for " + username + ": " + hashedPassword);

            pstmtUser.setString(1, username);
            pstmtUser.setString(2, email);
            pstmtUser.setString(3, hashedPassword);
            pstmtUser.setString(4, role);
            pstmtUser.setBoolean(5, isVerified);

            int affectedRows = pstmtUser.executeUpdate();

            if (affectedRows == 0) {
                conn.rollback();
                System.err.println("Registrasi gagal: Tidak ada baris yang terpengaruh di tabel users.");
                return false;
            }

            int userId = -1;
            try (ResultSet generatedKeys = pstmtUser.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    userId = generatedKeys.getInt(1);
                } else {
                    conn.rollback();
                    System.err.println("Registrasi gagal: Gagal mendapatkan ID pengguna baru.");
                    return false;
                }
            }

            // 2. Sisipkan entri default ke tabel profile (hanya user_id yang diperlukan sekarang)
            String insertProfileSql = "INSERT INTO profile (user_id) VALUES (?)";
            pstmtProfile = conn.prepareStatement(insertProfileSql);
            pstmtProfile.setInt(1, userId);

            affectedRows = pstmtProfile.executeUpdate();
            if (affectedRows == 0) {
                conn.rollback();
                System.err.println("Registrasi pengguna berhasil, tetapi gagal membuat entri profil untuk ID: " + userId);
                return false;
            }

            conn.commit();
            System.out.println("Registrasi berhasil untuk email: " + email + " (ID: " + userId + ") dan profil dibuat.");
            return true;

        } catch (SQLException e) {
            System.err.println("Database error during registration: " + e.getMessage());
            if (e.getSQLState().startsWith("23")) {
                System.err.println("Username or Email already registered: " + username + " / " + email);
            }
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException rbex) {
                System.err.println("Error rolling back transaction: " + rbex.getMessage());
            }
            return false;
        } finally {
            try {
                if (conn != null) conn.setAutoCommit(true);
            } catch (SQLException ex) { System.err.println("Error setting auto commit: " + ex.getMessage()); }
            DatabaseConnection.closeConnection(conn, pstmtUser, null);
            DatabaseConnection.closeConnection(null, pstmtProfile, null);
        }
    }

    // Metode Register yang dipanggil dari RegisterUI (default role user, not verified)
    public static boolean register(String username, String email, String password) {
        return register(username, email, password, "user", false);
    }

    // Method untuk update status verifikasi user di DB
    public static boolean updateVerificationStatus(String email, boolean isVerified) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = DatabaseConnection.getConnection();
            String sql = "UPDATE users SET is_verified = ? WHERE email = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setBoolean(1, isVerified);
            pstmt.setString(2, email);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error updating verification status: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            DatabaseConnection.closeConnection(conn, pstmt, null);
        }
    }

    // Method untuk mendapatkan user berdasarkan email (mengambil semua detail dari users dan profile)
    // Hapus kolom alamat dari tabel profile di query ini juga
    public static User getUserByEmail(String email) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        String sql = "SELECT u.id, u.username, u.password, u.email, u.role, u.is_verified, " +
                     "p.nik, p.phone, p.profile_picture, p.banner_picture " + // 'address', 'city', 'province', 'postal_code', 'country' DIHAPUS DARI SINI
                     "FROM users u LEFT JOIN profile p ON u.id = p.user_id " +
                     "WHERE u.email = ?";
        try {
            conn = DatabaseConnection.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, email);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                int id = rs.getInt("id");
                String username = rs.getString("username");
                String passwordHash = rs.getString("password");
                String dbEmail = rs.getString("email");
                String role = rs.getString("role");
                boolean isVerified = rs.getBoolean("is_verified");

                String nik = rs.getString("nik");
                String phone = rs.getString("phone");
                // Kolom alamat tidak lagi diambil dari ResultSet
                // String address = rs.getString("address");
                // String city = rs.getString("city");
                // String province = rs.getString("province");
                // String postalCode = rs.getString("postal_code");
                // String country = rs.getString("country");
                byte[] profilePicture = rs.getBytes("profile_picture");
                byte[] bannerPicture = rs.getBytes("banner_picture");

                // Memanggil konstruktor User yang sudah disesuaikan
                return new User(id, username, passwordHash, dbEmail, nik, phone, profilePicture, bannerPicture, role, isVerified);
            }
            return null;
        } catch (SQLException e) {
            System.err.println("Error getting user by email: " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            DatabaseConnection.closeConnection(conn, pstmt, rs);
        }
    }

    // Method untuk mendapatkan user yang sedang login (tidak berubah)
    public static User getCurrentUser() {
        return currentUser;
    }

    // Method untuk logout (tidak berubah)
    public static void logout() {
        currentUser = null;
    }
}