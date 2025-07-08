package e.commerce;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;


public class CouponService {

    /**
     * Memvalidasi dan menerapkan kode kupon.
     *
     * @param couponCode Kode kupon yang dimasukkan pengguna.
     * @param userId     ID pengguna yang sedang login.
     * @param cartTotal  Total harga dari keranjang belanja (sebelum diskon).
     * @return CouponResult yang berisi status, pesan, dan jumlah diskon.
     */
    public static CouponResult applyCoupon(String couponCode, int userId, double cartTotal) {
        try {
            // 1. Dapatkan kupon dari database
            Coupon coupon = ProductRepository.getCouponByCode(couponCode);
            if (coupon == null || !coupon.isActive()) {
                return new CouponResult(false, "Kupon tidak ditemukan atau tidak aktif.");
            }

            // 2. Validasi tanggal berlaku
            Timestamp now = new Timestamp(new Date().getTime());
            if (coupon.getValidFrom().after(now)) {
                return new CouponResult(false, "Kupon ini belum dapat digunakan.");
            }
            if (coupon.getValidUntil().before(now)) {
                return new CouponResult(false, "Masa berlaku kupon telah habis.");
            }

            // 3. Validasi batas total penggunaan kupon
            if (coupon.getCurrentUsageCount() >= coupon.getTotalUsageLimit()) {
                return new CouponResult(false, "Kuota penggunaan kupon ini telah habis.");
            }

            // 4. Validasi batas penggunaan per pengguna
            int userUsageCount = ProductRepository.getUserCouponUsageCount(userId, coupon.getId());
            if (userUsageCount >= coupon.getUsageLimitPerUser()) {
                return new CouponResult(false, "Anda telah mencapai batas maksimal penggunaan kupon ini.");
            }

            // 5. Validasi minimum pembelian
            if (cartTotal < coupon.getMinPurchaseAmount()) {
                return new CouponResult(false, "Belanja minimal untuk kupon ini adalah " + formatCurrency(coupon.getMinPurchaseAmount()));
            }

            // --- Semua validasi berhasil, hitung diskon ---
            double discountAmount = 0;
            if ("percentage".equalsIgnoreCase(coupon.getDiscountType())) {
                discountAmount = (cartTotal * coupon.getDiscountValue()) / 100;
                // Cek apakah ada batas maksimal diskon
                if (coupon.getMaxDiscountAmount() > 0 && discountAmount > coupon.getMaxDiscountAmount()) {
                    discountAmount = coupon.getMaxDiscountAmount();
                }
            } else if ("fixed".equalsIgnoreCase(coupon.getDiscountType())) {
                discountAmount = coupon.getDiscountValue();
            }

            // Pastikan diskon tidak lebih besar dari total belanja
            if (discountAmount > cartTotal) {
                discountAmount = cartTotal;
            }

            return new CouponResult(true, "Kupon berhasil diterapkan!", discountAmount, coupon.getId(), coupon.getCouponCode());

        } catch (SQLException e) {
            System.err.println("Error saat menerapkan kupon: " + e.getMessage());
            e.printStackTrace();
            return new CouponResult(false, "Terjadi kesalahan pada server. Coba lagi nanti.");
        }
    }

    private static String formatCurrency(double amount) {
        return String.format("Rp%,.0f", amount);
    }
}
