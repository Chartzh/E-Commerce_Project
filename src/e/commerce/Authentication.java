package e.commerce;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
            
            // Ambil data user dari database (hanya untuk verifikasi password dan mendapatkan ID)
            // Query hanya dari tabel users karena password dan username ada di sana
            String sql = "SELECT id, password FROM users WHERE username = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                int userId = rs.getInt("id");
                String storedPassword = rs.getString("password");
                boolean isPasswordValid = false;

                // Coba verifikasi dengan BCrypt
                try {
                    isPasswordValid = BCrypt.checkpw(password, storedPassword);
                } catch (IllegalArgumentException e) {
                    // Jika bukan hash BCrypt, cek plain text (untuk migrasi password lama)
                    System.err.println("Bukan hash BCrypt, mencoba plain text. Error: " + e.getMessage());
                    if (password.equals(storedPassword)) {
                        isPasswordValid = true;
                        // Migrasi ke BCrypt
                        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
                        String updateSql = "UPDATE users SET password = ? WHERE id = ?"; // Update berdasarkan ID
                        try (PreparedStatement updatePstmt = conn.prepareStatement(updateSql)) {
                            updatePstmt.setString(1, hashedPassword);
                            updatePstmt.setInt(2, userId); // Gunakan userId yang didapat
                            updatePstmt.executeUpdate();
                            System.out.println("Password untuk " + username + " dimigrasi ke BCrypt.");
                        }
                    }
                }

                // Jika password valid (BCrypt atau plain text)
                if (isPasswordValid) {
                    // Panggil loadUserById dari objek User untuk memuat semua data, termasuk dari tabel profile
                    User user = new User();
                    if (user.loadUserById(userId)) { // Gunakan ID yang didapat dari tabel users
                        currentUser = user;
                        System.out.println("Login berhasil untuk user: " + user.getUsername());
                        return user;
                    } else {
                        System.err.println("Login berhasil, tetapi gagal memuat data profil lengkap untuk ID: " + userId);
                        return null; // Gagal memuat profil lengkap
                    }
                }
            }
            System.out.println("Login gagal: Username atau password salah.");
            return null;
        } catch (SQLException e) {
            System.err.println("Error saat login: " + e.getSQLState() + " - " + e.getErrorCode() + " - " + e.getMessage());
            return null;
        } finally {
            // Pastikan resource ditutup
            DatabaseConnection.closeConnection(conn, pstmt, rs);
        }
    }

    // Method untuk cek username tersedia (tidak berubah, karena username di tabel users)
    public static boolean isUsernameAvailable(String username) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT username FROM users WHERE username = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            return !rs.next(); // Return true jika username tidak ditemukan
        } catch (SQLException e) {
            System.err.println("Error saat cek username: " + e.getSQLState() + " - " + e.getErrorCode() + " - " + e.getMessage());
            return false;
        }
    }

    // Method untuk cek email tersedia (tidak berubah, karena email di tabel users)
    public static boolean isEmailAvailable(String email) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT email FROM users WHERE email = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();

            return !rs.next(); // Return true jika email tidak ditemukan
        } catch (SQLException e) {
            System.err.println("Error saat cek email: " + e.getSQLState() + " - " + e.getErrorCode() + " - " + e.getMessage());
            return false;
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