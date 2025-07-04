package e.commerce;

public class ShippingCostResult {
    private String providerName; // NEW: Nama penyedia pengiriman (e.g., JNE, TIKI)
    private double cost;
    private String zoneName;
    private double baseCost;
    private double costPerKg;
    private double chargeableWeight;
    private String errorMessage;
    private String arrivalEstimate; // NEW: Estimasi waktu kedatangan (e.g., "1-2 Hari")


    // Constructor untuk hasil sukses (diperbarui dengan providerName dan arrivalEstimate)
    public ShippingCostResult(String providerName, double cost, String zoneName, double baseCost, double costPerKg, double chargeableWeight, String arrivalEstimate) {
        this.providerName = providerName;
        this.cost = cost;
        this.zoneName = zoneName;
        this.baseCost = baseCost;
        this.costPerKg = costPerKg;
        this.chargeableWeight = chargeableWeight;
        this.arrivalEstimate = arrivalEstimate;
        this.errorMessage = null; // Tidak ada error
    }

    // Constructor asli untuk hasil sukses (jika masih ada yang memanggilnya tanpa provider/estimate)
    // Akan dipertahankan untuk kompatibilitas mundur, tapi akan menginisialisasi provider/estimate ke default
    public ShippingCostResult(double cost, String zoneName, double baseCost, double costPerKg, double chargeableWeight) {
        this("Default Provider", cost, zoneName, baseCost, costPerKg, chargeableWeight, "N/A");
    }


    // Constructor untuk hasil gagal (ada error) - diperbarui untuk menjaga konsistensi dengan fields baru
    public ShippingCostResult(String errorMessage) {
        this.errorMessage = errorMessage;
        // Inisialisasi default untuk nilai lain jika terjadi error
        this.providerName = "Error"; // Default untuk kasus error
        this.cost = 0.0;
        this.zoneName = null;
        this.baseCost = 0.0;
        this.costPerKg = 0.0;
        this.chargeableWeight = 0.0;
        this.arrivalEstimate = "N/A"; // Default untuk kasus error
    }

    // Getters
    public String getProviderName() {
        return providerName;
    }

    public double getCost() {
        return cost;
    }

    public String getZoneName() {
        return zoneName;
    }

    public double getBaseCost() {
        return baseCost;
    }

    public double getCostPerKg() {
        return costPerKg;
    }

    public double getChargeableWeight() {
        return chargeableWeight;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getArrivalEstimate() { // NEW Getter
        return arrivalEstimate;
    }

    // Helper method untuk mengecek apakah ada error
    public boolean hasError() {
        return errorMessage != null;
    }

    @Override
    public String toString() {
        if (hasError()) {
            return "Error: " + errorMessage;
        } else {
            return "Shipping Cost: " + String.format("Rp %,.2f", cost) +
                   " (Provider: " + providerName + // Ditambahkan
                   ", Zone: " + zoneName +
                   ", Base: " + String.format("Rp %,.2f", baseCost) +
                   ", Per Kg: " + String.format("Rp %,.2f", costPerKg) +
                   ", Berat Dihitung: " + String.format("%.2f", chargeableWeight) + " kg" +
                   ", Estimasi Tiba: " + arrivalEstimate + ")"; // Ditambahkan
        }
    }
}