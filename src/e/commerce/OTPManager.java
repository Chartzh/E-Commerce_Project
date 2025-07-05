package e.commerce;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class OTPManager {
    private static OTPManager instance;
    private Map<String, Timer> otpTimers;
    private static final long OTP_EXPIRY_TIME = 5 * 60 * 1000;

    private OTPManager() {
        otpTimers = new HashMap<>();
    }

    public static synchronized OTPManager getInstance() {
        if (instance == null) {
            instance = new OTPManager();
        }
        return instance;
    }

    public void setOTP(String email, String otp, User pendingUser) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            System.out.println("Menyimpan OTP untuk email: " + email + ", OTP: " + otp + ", Username: " + pendingUser.getUsername());

            String checkUserSql = "SELECT id, password, is_verified FROM users WHERE email = ?";
            PreparedStatement checkUserStmt = conn.prepareStatement(checkUserSql);
            checkUserStmt.setString(1, email);
            ResultSet rs = checkUserStmt.executeQuery();

            boolean userExists = false;
            int existingUserId = -1;

            if (rs.next()) {
                userExists = true;
                existingUserId = rs.getInt("id");
                boolean isVerifiedInDb = rs.getBoolean("is_verified");

                System.out.println("Email " + email + " ditemukan di tabel users (ID: " + existingUserId + "). is_verified: " + isVerifiedInDb);

                if (isVerifiedInDb) {
                    System.err.println("Kesalahan: Email " + email + " sudah terdaftar dan terverifikasi. Tidak bisa kirim OTP lagi untuk pendaftaran baru.");
                    throw new RuntimeException("Email " + email + " sudah terdaftar dan terverifikasi. Silakan login.");
                }
            }
            rs.close();

            if (!userExists) {
                System.out.println("Email tidak ditemukan, menyimpan data pengguna baru ke users...");
                String insertUserSql = "INSERT INTO users (username, password, email, role, is_verified) VALUES (?, ?, ?, ?, 0)";
                PreparedStatement insertUserStmt = conn.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS);
                insertUserStmt.setString(1, pendingUser.getUsername());
                insertUserStmt.setString(2, pendingUser.getPassword());
                insertUserStmt.setString(3, pendingUser.getEmail());
                insertUserStmt.setString(4, pendingUser.getRole());
                insertUserStmt.executeUpdate();

                try (ResultSet generatedKeys = insertUserStmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        pendingUser.setId(generatedKeys.getInt(1));
                        System.out.println("Pengguna baru diinsert dengan ID: " + pendingUser.getId());
                    }
                }
                String insertProfileSql = "INSERT INTO profile (user_id) VALUES (?)";
                PreparedStatement insertProfileStmt = conn.prepareStatement(insertProfileSql);
                insertProfileStmt.setInt(1, pendingUser.getId());
                insertProfileStmt.executeUpdate();
                System.out.println("Profil baru dibuat untuk user ID: " + pendingUser.getId());


            } else {
                System.out.println("Email ditemukan, memperbarui data pengguna sementara...");
                String updateUserSql = "UPDATE users SET username = ?, password = ?, role = ?, is_verified = 0 WHERE id = ?";
                PreparedStatement updateUserStmt = conn.prepareStatement(updateUserSql);
                updateUserStmt.setString(1, pendingUser.getUsername());
                updateUserStmt.setString(2, pendingUser.getPassword());
                updateUserStmt.setString(3, pendingUser.getRole());
                updateUserStmt.setInt(4, existingUserId);
                updateUserStmt.executeUpdate();
                pendingUser.setId(existingUserId);
                System.out.println("Pengguna sementara diupdate (ID: " + existingUserId + ").");

                String checkProfileSql = "SELECT user_id FROM profile WHERE user_id = ?";
                PreparedStatement checkProfileStmt = conn.prepareStatement(checkProfileSql);
                checkProfileStmt.setInt(1, existingUserId);
                ResultSet profileRs = checkProfileStmt.executeQuery();
                if (!profileRs.next()) {
                    String insertProfileSql = "INSERT INTO profile (user_id) VALUES (?)";
                    PreparedStatement insertProfileStmt = conn.prepareStatement(insertProfileSql);
                    insertProfileStmt.setInt(1, existingUserId);
                    insertProfileStmt.executeUpdate();
                    System.out.println("Profil baru dibuat untuk user ID: " + existingUserId);
                }
                profileRs.close();
            }

            String deleteOldOtpSql = "DELETE FROM verifications WHERE email = ?";
            PreparedStatement deleteOldOtpStmt = conn.prepareStatement(deleteOldOtpSql);
            deleteOldOtpStmt.setString(1, email);
            deleteOldOtpStmt.executeUpdate();
            System.out.println("OTP lama dihapus dari verifications untuk email: " + email);

            String insertOtpSql = "INSERT INTO verifications (email, otp_code, otp_expires_at) VALUES (?, ?, DATE_ADD(NOW(), INTERVAL 5 MINUTE))";
            PreparedStatement insertOtpStmt = conn.prepareStatement(insertOtpSql);
            insertOtpStmt.setString(1, email);
            insertOtpStmt.setString(2, otp);
            int affectedRows = insertOtpStmt.executeUpdate();
            System.out.println("Insert OTP baru ke verifications affected rows: " + affectedRows);

            if (affectedRows > 0) {
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
                verifyRs.close();
            } else {
                System.err.println("Tidak ada baris yang terpengaruh saat menyimpan OTP untuk email: " + email);
            }

            if (otpTimers.containsKey(email)) {
                otpTimers.get(email).cancel();
            }

            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    removeOTP(email);
                    otpTimers.remove(email);
                }
            }, OTP_EXPIRY_TIME);

            otpTimers.put(email, timer);
            conn.commit();
        } catch (SQLException e) {
            System.err.println("Gagal menyimpan OTP: " + e.getSQLState() + " - " + e.getErrorCode() + " - " + e.getMessage());
            e.printStackTrace();
            try {
                if (conn != null && !conn.getAutoCommit()) {
                    conn.rollback();
                    System.err.println("Transaction rolled back for setOTP.");
                }
            } catch (SQLException rbex) {
                System.err.println("Error rolling back transaction: " + rbex.getMessage());
            }
            throw new RuntimeException("Gagal menyimpan OTP: " + e.getMessage(), e);
        } finally {
            try {
                if (conn != null) conn.setAutoCommit(true);
            } catch (SQLException ex) { System.err.println("Error setting auto commit: " + ex.getMessage()); }
            DatabaseConnection.closeConnection(conn, null);
        }
    }

    public boolean verifyOTP(String email, String otp) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            String sql = "SELECT otp_code, otp_expires_at FROM verifications WHERE email = ? AND otp_code = ? AND otp_expires_at > NOW()";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, email);
            stmt.setString(2, otp);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                System.out.println("OTP terverifikasi untuk email: " + email + ", OTP: " + otp + ", Expires at: " + rs.getTimestamp("otp_expires_at"));
                String updateUserSql = "UPDATE users SET is_verified = 1 WHERE email = ?";
                PreparedStatement updateUserStmt = conn.prepareStatement(updateUserSql);
                updateUserStmt.setString(1, email);
                int affectedRows = updateUserStmt.executeUpdate();
                System.out.println("Update is_verified di users affected rows: " + affectedRows);

                removeOTP(email);

                if (otpTimers.containsKey(email)) {
                    otpTimers.get(email).cancel();
                    otpTimers.remove(email);
                }
                conn.commit();
                return true;
            } else {
                System.err.println("Verifikasi OTP gagal untuk email: " + email + ", OTP: " + otp);
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
                debugOtpRs.close();

                String debugUserSql = "SELECT username, email, is_verified, password FROM users WHERE email = ?";
                PreparedStatement debugUserStmt = conn.prepareStatement(debugUserSql);
                debugUserStmt.setString(1, email);
                ResultSet debugUserRs = debugUserStmt.executeQuery();
                if (debugUserRs.next()) {
                    System.err.println("Data pengguna - Username: " + debugUserRs.getString("username") +
                            ", Email: " + debugUserRs.getString("email") +
                            ", Is Verified: " + debugUserRs.getBoolean("is_verified") +
                            ", Password Hash: " + debugUserRs.getString("password"));
                }
                debugUserRs.close();
                try {
                    if (conn != null && !conn.getAutoCommit()) {
                        conn.rollback();
                        System.err.println("Transaction rolled back for failed verifyOTP.");
                    }
                } catch (SQLException rbex) {
                    System.err.println("Error rolling back transaction: " + rbex.getMessage());
                }
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Gagal memverifikasi OTP: " + e.getSQLState() + " - " + e.getErrorCode() + " - " + e.getMessage());
            e.printStackTrace();
            try {
                if (conn != null && !conn.getAutoCommit()) {
                    conn.rollback();
                    System.err.println("Transaction rolled back for verifyOTP error.");
                }
            } catch (SQLException rbex) {
                System.err.println("Error rolling back transaction: " + rbex.getMessage());
            }
            return false;
        } finally {
            try {
                if (conn != null) conn.setAutoCommit(true);
            } catch (SQLException ex) { System.err.println("Error setting auto commit: " + ex.getMessage()); }
            DatabaseConnection.closeConnection(conn, null);
        }
    }

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

    public void cancelVerification(String email) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            String deleteOtpSql = "DELETE FROM verifications WHERE email = ?";
            PreparedStatement deleteOtpStmt = conn.prepareStatement(deleteOtpSql);
            deleteOtpStmt.setString(1, email);
            int otpAffectedRows = deleteOtpStmt.executeUpdate();
            System.out.println("Data OTP dihapus untuk email: " + email + ", Affected rows: " + otpAffectedRows);

            String deleteUserSql = "DELETE FROM users WHERE email = ? AND is_verified = 0 AND (password IS NULL OR password = '')";
            PreparedStatement deleteUserStmt = conn.prepareStatement(deleteUserSql);
            deleteUserStmt.setString(1, email);
            int userAffectedRows = deleteUserStmt.executeUpdate();
            System.out.println("Data pengguna sementara dihapus untuk email: " + email + ", Affected rows: " + userAffectedRows);

            if (otpTimers.containsKey(email)) {
                otpTimers.get(email).cancel();
                otpTimers.remove(email);
            }
            conn.commit();
        } catch (SQLException e) {
            System.err.println("Gagal menghapus data sementara: " + e.getSQLState() + " - " + e.getErrorCode() + " - " + e.getMessage());
            e.printStackTrace();
            try {
                if (conn != null && !conn.getAutoCommit()) {
                    conn.rollback();
                    System.err.println("Transaction rolled back for cancelVerification.");
                }
            } catch (SQLException rbex) {
                System.err.println("Error rolling back transaction: " + rbex.getMessage());
            }
        } finally {
            try {
                if (conn != null) conn.setAutoCommit(true);
            } catch (SQLException ex) { System.err.println("Error setting auto commit: " + ex.getMessage()); }
            DatabaseConnection.closeConnection(conn, null);
        }
    }

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