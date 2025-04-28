package e.commerce;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class OTPManager {
    private static OTPManager instance;
    private Map<String, Timer> otpTimers; // email -> timer
    private static final long OTP_EXPIRY_TIME = 5 * 60 * 1000; // 5 menit dalam milidetik

    private OTPManager() {
        otpTimers = new HashMap<>();
    }

    public static synchronized OTPManager getInstance() {
        if (instance == null) {
            instance = new OTPManager();
        }
        return instance;
    }

    // Simpan OTP untuk email ke tabel users
    public void setOTP(String email, String otp, User pendingUser) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Log input
            System.out.println("Attempting to save OTP for email: " + email + ", OTP: " + otp + ", Username: " + pendingUser.getUsername());

            // Cek apakah email sudah ada di tabel users
            String checkSql = "SELECT email FROM users WHERE email = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setString(1, email);
            ResultSet rs = checkStmt.executeQuery();

            int affectedRows;
            if (rs.next()) {
                // Email sudah ada, update OTP
                System.out.println("Email found, updating OTP...");
                String sql = "UPDATE users SET otp_code = ?, otp_expires_at = DATE_ADD(NOW(), INTERVAL 5 MINUTE) WHERE email = ?";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, otp);
                stmt.setString(2, email);
                affectedRows = stmt.executeUpdate();
                System.out.println("Update OTP affected rows: " + affectedRows);
            } else {
                // Email belum ada, masukkan data pengguna sementara
                System.out.println("Email not found, inserting temporary user data...");
                String sql = "INSERT INTO users (username, email, otp_code, otp_expires_at) VALUES (?, ?, ?, DATE_ADD(NOW(), INTERVAL 5 MINUTE))";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, pendingUser.getUsername());
                stmt.setString(2, email);
                stmt.setString(3, otp);
                affectedRows = stmt.executeUpdate();
                System.out.println("Insert temporary user affected rows: " + affectedRows);
            }

            if (affectedRows > 0) {
                // Verifikasi penyimpanan
                String verifySql = "SELECT username, email, otp_code, otp_expires_at FROM users WHERE email = ?";
                PreparedStatement verifyStmt = conn.prepareStatement(verifySql);
                verifyStmt.setString(1, email);
                ResultSet verifyRs = verifyStmt.executeQuery();
                if (verifyRs.next()) {
                    System.out.println("Stored data - Username: " + verifyRs.getString("username") +
                                      ", Email: " + verifyRs.getString("email") +
                                      ", OTP: " + verifyRs.getString("otp_code") +
                                      ", Expires at: " + verifyRs.getTimestamp("otp_expires_at"));
                } else {
                    System.err.println("Failed to verify stored OTP for email: " + email);
                }
            } else {
                System.err.println("No rows affected when saving OTP for email: " + email);
            }

            // Hentikan timer lama jika ada
            if (otpTimers.containsKey(email)) {
                otpTimers.get(email).cancel();
            }

            // Atur timer baru untuk menghapus OTP
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    removeOTP(email);
                    otpTimers.remove(email);
                }
            }, OTP_EXPIRY_TIME);

            otpTimers.put(email, timer);
        } catch (SQLException e) {
            System.err.println("Failed to save OTP to users table: " + e.getSQLState() + " - " + e.getErrorCode() + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Verifikasi OTP
    public boolean verifyOTP(String email, String otp) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT otp_code, otp_expires_at FROM users WHERE email = ? AND otp_code = ? AND otp_expires_at > NOW()";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, email);
            stmt.setString(2, otp);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                System.out.println("OTP verified for email: " + email + ", OTP: " + otp + ", Expires at: " + rs.getTimestamp("otp_expires_at"));
                return true;
            } else {
                System.err.println("OTP verification failed for email: " + email + ", OTP: " + otp);
                // Log data untuk debugging
                String debugSql = "SELECT username, email, otp_code, otp_expires_at FROM users WHERE email = ?";
                PreparedStatement debugStmt = conn.prepareStatement(debugSql);
                debugStmt.setString(1, email);
                ResultSet debugRs = debugStmt.executeQuery();
                if (debugRs.next()) {
                    System.err.println("Stored data - Username: " + debugRs.getString("username") +
                                      ", Email: " + debugRs.getString("email") +
                                      ", OTP: " + debugRs.getString("otp_code") +
                                      ", Expires at: " + debugRs.getTimestamp("otp_expires_at"));
                } else {
                    System.err.println("No OTP found for email: " + email);
                }
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Failed to verify OTP: " + e.getSQLState() + " - " + e.getErrorCode() + " - " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        
    }

    // Periksa apakah email sedang dalam proses verifikasi
    public boolean isPendingVerification(String email) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT otp_code, otp_expires_at FROM users WHERE email = ? AND otp_expires_at > NOW()";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                System.out.println("Pending verification for email: " + email + ", OTP: " + rs.getString("otp_code") + ", Expires at: " + rs.getTimestamp("otp_expires_at"));
                return true;
            }
            System.err.println("No pending verification for email: " + email);
            return false;
        } catch (SQLException e) {
            System.err.println("Failed to check pending verification: " + e.getSQLState() + " - " + e.getErrorCode() + " - " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Hapus OTP dari tabel users
    public void removeOTP(String email) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "UPDATE users SET otp_code = NULL, otp_expires_at = NULL WHERE email = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, email);
            int affectedRows = stmt.executeUpdate();
            System.out.println("OTP removed from users table for email: " + email + ", Affected rows: " + affectedRows);
        } catch (SQLException e) {
            System.err.println("Failed to remove OTP from users table: " + e.getSQLState() + " - " + e.getErrorCode() + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Hapus data pengguna sementara jika verifikasi dibatalkan
    public void cancelVerification(String email) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "DELETE FROM users WHERE email = ? AND password IS NULL";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, email);
            int affectedRows = stmt.executeUpdate();
            System.out.println("Temporary user data removed for email: " + email + ", Affected rows: " + affectedRows);
        } catch (SQLException e) {
            System.err.println("Failed to remove temporary user data: " + e.getSQLState() + " - " + e.getErrorCode() + " - " + e.getMessage());
            e.printStackTrace();
        }
    }
}