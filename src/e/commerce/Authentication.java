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
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Ambil data user dari database
            String sql = "SELECT * FROM users WHERE username = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String storedPassword = rs.getString("password");
                String hashedPassword = storedPassword; // Default ke storedPassword
                boolean isPasswordValid = false;

                // Coba verifikasi dengan BCrypt
                try {
                    isPasswordValid = BCrypt.checkpw(password, storedPassword);
                } catch (IllegalArgumentException e) {
                    // Jika bukan hash BCrypt, cek plain text
                    System.err.println("Bukan hash BCrypt, mencoba plain text: " + e.getMessage());
                    if (password.equals(storedPassword)) {
                        isPasswordValid = true;
                        // Migrasi ke BCrypt
                        hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
                        // Update password di database
                        String updateSql = "UPDATE users SET password = ? WHERE username = ?";
                        try (PreparedStatement updatePstmt = conn.prepareStatement(updateSql)) {
                            updatePstmt.setString(1, hashedPassword);
                            updatePstmt.setString(2, username);
                            updatePstmt.executeUpdate();
                            System.out.println("Password untuk " + username + " dimigrasi ke BCrypt.");
                        }
                    }
                }

                // Jika password valid (BCrypt atau plain text)
                if (isPasswordValid) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setUsername(rs.getString("username"));
                    user.setPassword(hashedPassword); // Simpan hash terbaru
                    user.setEmail(rs.getString("email"));
                    user.setNik(rs.getString("nik"));
                    user.setAddress(rs.getString("address"));
                    user.setPhone(rs.getString("phone"));
                    user.setRole(rs.getString("role"));

                    currentUser = user;
                    return user;
                }
            }
            System.out.println("Login gagal: Username atau password salah.");
            return null;
        } catch (SQLException e) {
            System.err.println("Error saat login: " + e.getSQLState() + " - " + e.getErrorCode() + " - " + e.getMessage());
            return null;
        }
    }

    // Method untuk cek username tersedia
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

    // Method untuk cek email tersedia
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

    // Method untuk mendapatkan user yang sedang login
    public static User getCurrentUser() {
        return currentUser;
    }

    // Method untuk logout
    public static void logout() {
        currentUser = null;
    }
}