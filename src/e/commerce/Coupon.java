package e.commerce;

import java.sql.Timestamp;

public class Coupon {
    private int id;
    private String couponCode;
    private String discountType; // 'percentage' atau 'fixed'
    private double discountValue;
    private double minPurchaseAmount;
    private double maxDiscountAmount;
    private int usageLimitPerUser;
    private int totalUsageLimit;
    private int currentUsageCount;
    private Timestamp validFrom;
    private Timestamp validUntil;
    private boolean isActive;

    // Constructor, Getters, dan Setters
    public Coupon(int id, String couponCode, String discountType, double discountValue, double minPurchaseAmount, double maxDiscountAmount, int usageLimitPerUser, int totalUsageLimit, int currentUsageCount, Timestamp validFrom, Timestamp validUntil, boolean isActive) {
        this.id = id;
        this.couponCode = couponCode;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minPurchaseAmount = minPurchaseAmount;
        this.maxDiscountAmount = maxDiscountAmount;
        this.usageLimitPerUser = usageLimitPerUser;
        this.totalUsageLimit = totalUsageLimit;
        this.currentUsageCount = currentUsageCount;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.isActive = isActive;
    }

    public int getId() { return id; }
    public String getCouponCode() { return couponCode; }
    public String getDiscountType() { return discountType; }
    public double getDiscountValue() { return discountValue; }
    public double getMinPurchaseAmount() { return minPurchaseAmount; }
    public double getMaxDiscountAmount() { return maxDiscountAmount; }
    public int getUsageLimitPerUser() { return usageLimitPerUser; }
    public int getTotalUsageLimit() { return totalUsageLimit; }
    public int getCurrentUsageCount() { return currentUsageCount; }
    public Timestamp getValidFrom() { return validFrom; }
    public Timestamp getValidUntil() { return validUntil; }
    public boolean isActive() { return isActive; }
}