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
    private Map<String, Timer> otpTimers;
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

    // Simpan OTP untuk email
    public void setOTP(String email, String otp, User pendingUser) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Log input
            System.out.println("Menyimpan OTP untuk email: " + email + ", OTP: " + otp + ", Username: " + pendingUser.getUsername());

            // Cek apakah email sudah ada di tabel users
            String checkUserSql = "SELECT email, is_verified FROM users WHERE email = ?";
            PreparedStatement checkUserStmt = conn.prepareStatement(checkUserSql);
            checkUserStmt.setString(1, email);
            ResultSet rs = checkUserStmt.executeQuery();

            int affectedRows;
            if (!rs.next()) {
                // Email belum ada, masukkan data pengguna sementara ke users
                System.out.println("Email tidak ditemukan, menyimpan data pengguna sementara...");
                String insertUserSql = "INSERT INTO users (username, email, is_verified) VALUES (?, ?, 0)";
                PreparedStatement insertUserStmt = conn.prepareStatement(insertUserSql);
                insertUserStmt.setString(1, pendingUser.getUsername());
                insertUserStmt.setString(2, email);
                affectedRows = insertUserStmt.executeUpdate();
                System.out.println("Insert pengguna sementara ke users affected rows: " + affectedRows);
            }

            // Simpan OTP ke tabel verifications
            // Hapus OTP lama jika ada
            String deleteOldOtpSql = "DELETE FROM verifications WHERE email = ?";
            PreparedStatement deleteOldOtpStmt = conn.prepareStatement(deleteOldOtpSql);
            deleteOldOtpStmt.setString(1, email);
            deleteOldOtpStmt.executeUpdate();

            // Insert OTP baru
            String insertOtpSql = "INSERT INTO verifications (email, otp_code, otp_expires_at) VALUES (?, ?, DATE_ADD(NOW(), INTERVAL 5 MINUTE))";
            PreparedStatement insertOtpStmt = conn.prepareStatement(insertOtpSql);
            insertOtpStmt.setString(1, email);
            insertOtpStmt.setString(2, otp);
            affectedRows = insertOtpStmt.executeUpdate();
            System.out.println("Insert OTP ke verifications affected rows: " + affectedRows);

            if (affectedRows > 0) {
                // Verifikasi penyimpanan
                String verifySql = "SELECT email, otp_code, otp_expires_at FROM verifications WHERE email = ?";
                PreparedStatement verifyStmt = conn.prepareStatement(verifySql);
                verifyStmt.setString(1, email);
                ResultSet verifyRs = verifyStmt.executeQuery();
                if (verifyRs.next()) {
                    System.out.println("Data OTP tersimpan - Email: " + verifyRs.getString("email") +
                            ", OTP: " + verifyRs.getString("otp_code") +
                            ", Expires at: " + verifyRs.getTimestamp("otp_expires_at"));
                } else {
                    System.err.println("Gagal memverifikasi OTP tersimpan untuk email: " + email);
                }
            } else {
                System.err.println("Tidak ada baris yang terpengaruh saat menyimpan OTP untuk email: " + email);
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
            System.err.println("Gagal menyimpan OTP: " + e.getSQLState() + " - " + e.getErrorCode() + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Verifikasi OTP
    public boolean verifyOTP(String email, String otp) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT otp_code, otp_expires_at FROM verifications WHERE email = ? AND otp_code = ? AND otp_expires_at > NOW()";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, email);
            stmt.setString(2, otp);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                // OTP valid, set is_verified = 1 di users dan hapus OTP
                System.out.println("OTP terverifikasi untuk email: " + email + ", OTP: " + otp + ", Expires at: " + rs.getTimestamp("otp_expires_at"));
                String updateUserSql = "UPDATE users SET is_verified = 1 WHERE email = ?";
                PreparedStatement updateUserStmt = conn.prepareStatement(updateUserSql);
                updateUserStmt.setString(1, email);
                int affectedRows = updateUserStmt.executeUpdate();
                System.out.println("Update is_verified di users affected rows: " + affectedRows);

                // Hapus OTP dari verifications
                removeOTP(email);

                // Hentikan timer
                if (otpTimers.containsKey(email)) {
                    otpTimers.get(email).cancel();
                    otpTimers.remove(email);
                }
                return true;
            } else {
                System.err.println("Verifikasi OTP gagal untuk email: " + email + ", OTP: " + otp);
                // Log data untuk debugging
                String debugOtpSql = "SELECT email, otp_code, otp_expires_at FROM verifications WHERE email = ?";
                PreparedStatement debugOtpStmt = conn.prepareStatement(debugOtpSql);
                debugOtpStmt.setString(1, email);
                ResultSet debugOtpRs = debugOtpStmt.executeQuery();
                if (debugOtpRs.next()) {
                    System.err.println("Data OTP - Email: " + debugOtpRs.getString("email") +
                            ", OTP: " + debugOtpRs.getString("otp_code") +
                            ", Expires at: " + debugOtpRs.getTimestamp("otp_expires_at"));
                } else {
                    System.err.println("Tidak ada OTP ditemukan untuk email: " + email);
                }

                String debugUserSql = "SELECT username, email, is_verified FROM users WHERE email = ?";
                PreparedStatement debugUserStmt = conn.prepareStatement(debugUserSql);
                debugUserStmt.setString(1, email);
                ResultSet debugUserRs = debugUserStmt.executeQuery();
                if (debugUserRs.next()) {
                    System.err.println("Data pengguna - Username: " + debugUserRs.getString("username") +
                            ", Email: " + debugUserRs.getString("email") +
                            ", Is Verified: " + debugUserRs.getBoolean("is_verified"));
                }
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Gagal memverifikasi OTP: " + e.getSQLState() + " - " + e.getErrorCode() + " - " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Periksa apakah email sedang dalam proses verifikasi
    public boolean isPendingVerification(String email) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT ov.otp_code, ov.otp_expires_at, u.is_verified " +
                        "FROM verifications ov " +
                        "JOIN users u ON ov.email = u.email " +
                        "WHERE ov.email = ? AND ov.otp_expires_at > NOW() AND u.is_verified = 0";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                System.out.println("Verifikasi tertunda untuk email: " + email + ", OTP: " + rs.getString("otp_code") +
                        ", Expires at: " + rs.getTimestamp("otp_expires_at"));
                return true;
            }
            System.err.println("Tidak ada verifikasi tertunda untuk email: " + email);
            return false;
        } catch (SQLException e) {
            System.err.println("Gagal memeriksa verifikasi tertunda: " + e.getSQLState() + " - " + e.getErrorCode() + " - " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Hapus OTP dari tabel verifications
    public void removeOTP(String email) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "DELETE FROM verifications WHERE email = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, email);
            int affectedRows = stmt.executeUpdate();
            System.out.println("OTP dihapus dari tabel verifications untuk email: " + email + ", Affected rows: " + affectedRows);
        } catch (SQLException e) {
            System.err.println("Gagal menghapus OTP dari tabel verifications: " + e.getSQLState() + " - " + e.getErrorCode() + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Hapus data pengguna sementara jika verifikasi dibatalkan
    public void cancelVerification(String email) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            // Hapus dari verifications
            String deleteOtpSql = "DELETE FROM verifications WHERE email = ?";
            PreparedStatement deleteOtpStmt = conn.prepareStatement(deleteOtpSql);
            deleteOtpStmt.setString(1, email);
            int otpAffectedRows = deleteOtpStmt.executeUpdate();
            System.out.println("Data OTP dihapus untuk email: " + email + ", Affected rows: " + otpAffectedRows);

            // Hapus pengguna sementara dari users
            String deleteUserSql = "DELETE FROM users WHERE email = ? AND password IS NULL AND is_verified = 0";
            PreparedStatement deleteUserStmt = conn.prepareStatement(deleteUserSql);
            deleteUserStmt.setString(1, email);
            int userAffectedRows = deleteUserStmt.executeUpdate();
            System.out.println("Data pengguna sementara dihapus untuk email: " + email + ", Affected rows: " + userAffectedRows);

            // Hentikan timer jika ada
            if (otpTimers.containsKey(email)) {
                otpTimers.get(email).cancel();
                otpTimers.remove(email);
            }
        } catch (SQLException e) {
            System.err.println("Gagal menghapus data sementara: " + e.getSQLState() + " - " + e.getErrorCode() + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Cek status verifikasi pengguna
    public boolean isVerified(String email) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT is_verified FROM users WHERE email = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                boolean verified = rs.getBoolean("is_verified");
                System.out.println("Status verifikasi untuk email " + email + ": " + (verified ? "Terverifikasi" : "Belum terverifikasi"));
                return verified;
            }
            System.err.println("Email tidak ditemukan: " + email);
            return false;
        } catch (SQLException e) {
            System.err.println("Gagal memeriksa status verifikasi: " + e.getSQLState() + " - " + e.getErrorCode() + " - " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}