import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Authentication {
    private static User currentUser = null;

    // Method untuk login
    public static User login(String username, String password) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            pstmt.setString(2, password); // Ideally password should be hashed
            rs = pstmt.executeQuery();

            if (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setUsername(rs.getString("username"));
                user.setPassword(rs.getString("password"));
                user.setEmail(rs.getString("email"));
                user.setNik(rs.getString("nik"));
                user.setAddress(rs.getString("address"));
                user.setPhone(rs.getString("phone"));
                user.setRole(rs.getString("role")); // <-- ambil role dari DB

                currentUser = user;
                return user;
            }
            return null;
        } catch (SQLException e) {
            System.err.println("Error saat login: " + e.getMessage());
            return null;
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
            } catch (SQLException e) {
                System.err.println("Error menutup statement/resultset: " + e.getMessage());
            }
        }
    }

    // Method untuk cek username tersedia
    public static boolean isUsernameAvailable(String username) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT username FROM users WHERE username = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, username);
            rs = pstmt.executeQuery();

            return !rs.next(); // Return true jika username tidak ditemukan
        } catch (SQLException e) {
            System.err.println("Error saat cek username: " + e.getMessage());
            return false;
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
            } catch (SQLException e) {
                System.err.println("Error menutup statement/resultset: " + e.getMessage());
            }
        }
    }

    // Method untuk cek email tersedia
    public static boolean isEmailAvailable(String email) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT email FROM users WHERE email = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, email);
            rs = pstmt.executeQuery();

            return !rs.next(); // Return true jika email tidak ditemukan
        } catch (SQLException e) {
            System.err.println("Error saat cek email: " + e.getMessage());
            return false;
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
            } catch (SQLException e) {
                System.err.println("Error menutup statement/resultset: " + e.getMessage());
            }
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
