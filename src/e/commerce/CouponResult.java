package e.commerce;

public class CouponResult {
    private boolean success;
    private String message;
    private double discountAmount;
    private int couponId;
    private String couponCode;

    // Constructor untuk kasus sukses
    public CouponResult(boolean success, String message, double discountAmount, int couponId, String couponCode) {
        this.success = success;
        this.message = message;
        this.discountAmount = discountAmount;
        this.couponId = couponId;
        this.couponCode = couponCode;
    }

    // Constructor untuk kasus gagal
    public CouponResult(boolean success, String message) {
        this.success = success;
        this.message = message;
        this.discountAmount = 0;
        this.couponId = -1;
        this.couponCode = null;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public double getDiscountAmount() { return discountAmount; }
    public int getCouponId() { return couponId; }
    public String getCouponCode() { return couponCode; }
}
