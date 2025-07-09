// ShippingCalculator.java
package e.commerce;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class ShippingCalculator {

    private static final Map<String, Map<String, String[]>> SHIPPING_ZONES_CONFIG = new HashMap<>();

    private static String ORIGIN_PROVINCE;
    private static String ORIGIN_KABUPATEN; 

    static {
        // Inisialisasi konfigurasi zona
        // Struktur: ZoneName -> { "provinsi" -> [Provinsi1, Provinsi2], "kabupaten" -> [Kabupaten1, Kabupaten2] }

        // Zona Lokal: Bekasi (jika pengirim di Bekasi)
        SHIPPING_ZONES_CONFIG.put("Lokal_Bekasi", new HashMap<String, String[]>() {{
            put("provinsi", new String[]{"JAWA BARAT"});
            put("kabupaten", new String[]{"BEKASI", "KOTA BEKASI"});
        }});

        // Zona Jabodetabek
        SHIPPING_ZONES_CONFIG.put("Jabodetabek", new HashMap<String, String[]>() {{
            put("provinsi", new String[]{"DKI JAKARTA", "BANTEN", "JAWA BARAT"});
            put("kabupaten", new String[]{
                "JAKARTA PUSAT", "JAKARTA UTARA", "JAKARTA BARAT", "JAKARTA TIMUR", "JAKARTA SELATAN",
                "KOTA BOGOR", "KABUPATEN BOGOR", "KOTA DEPOK", "KOTA TANGERANG", "KABUPATEN TANGERANG", "KOTA TANGERANG SELATAN", "KOTA BEKASI", "BEKASI"
            });
        }});

        // Zona Jawa Barat Lainnya (di luar Jabodetabek)
        SHIPPING_ZONES_CONFIG.put("Jawa_Barat_Lainnya", new HashMap<String, String[]>() {{
            put("provinsi", new String[]{"JAWA BARAT"});
            put("kabupaten", new String[]{
                "BANDUNG", "KOTA BANDUNG", "BANDUNG BARAT", "KOTA CIMAHI", "CIREBON", "KOTA CIREBON",
                "GARUT", "INDRAMAYU", "KARAWANG", "KUNINGAN", "MAJALENGKA", "PANGANDARAN", "PURWAKARTA",
                "SUBANG", "SUKABUMI", "KOTA SUKABUMI", "SUMEDANG", "TASIKMALAYA", "KOTA TASIKMALAYA", "CIAMIS", "CIANJUR", "BANJAR"
            });
        }});

        // Zona Jawa Tengah & DIY
        SHIPPING_ZONES_CONFIG.put("Jawa_Tengah_DIY", new HashMap<String, String[]>() {{
            put("provinsi", new String[]{"JAWA TENGAH", "DAERAH ISTIMEWA YOGYAKARTA"});
            put("kabupaten", new String[]{}); // Mencakup semua kabupaten/kota di provinsi ini jika kosong
        }});

        // Zona Jawa Timur
        SHIPPING_ZONES_CONFIG.put("Jawa_Timur", new HashMap<String, String[]>() {{
            put("provinsi", new String[]{"JAWA TIMUR"});
            put("kabupaten", new String[]{});
        }});

        // Zona Sumatera
        SHIPPING_ZONES_CONFIG.put("Sumatera", new HashMap<String, String[]>() {{
            put("provinsi", new String[]{
                "ACEH", "SUMATERA UTARA", "SUMATERA BARAT", "RIAU", "KEPULAUAN RIAU", "JAMBI",
                "SUMATERA SELATAN", "BENGKULU", "LAMPUNG", "KEPULAUAN BANGKA BELITUNG"
            });
            put("kabupaten", new String[]{});
        }});
        
        // Zona Kalimantan
        SHIPPING_ZONES_CONFIG.put("Kalimantan", new HashMap<String, String[]>() {{
            put("provinsi", new String[]{
                "KALIMANTAN BARAT", "KALIMANTAN TENGAH", "KALIMANTAN SELATAN", "KALIMANTAN TIMUR", "KALIMANTAN UTARA"
            });
            put("kabupaten", new String[]{});
        }});

        // Zona Sulawesi
        SHIPPING_ZONES_CONFIG.put("Sulawesi", new HashMap<String, String[]>() {{
            put("provinsi", new String[]{
                "SULAWESI UTARA", "GORONTALO", "SULAWESI TENGAH", "SULAWESI BARAT", "SULAWESI SELATAN", "SULAWESI TENGGARA"
            });
            put("kabupaten", new String[]{});
        }});

        // Zona Bali & Nusa Tenggara
        SHIPPING_ZONES_CONFIG.put("Bali_Nusa_Tenggara", new HashMap<String, String[]>() {{
            put("provinsi", new String[]{"BALI", "NUSA TENGGARA BARAT", "NUSA TENGGARA TIMUR"});
            put("kabupaten", new String[]{});
        }});

        // Zona Maluku & Papua
        SHIPPING_ZONES_CONFIG.put("Maluku_Papua", new HashMap<String, String[]>() {{
            put("provinsi", new String[]{
                "MALUKU", "MALUKU UTARA", "PAPUA", "PAPUA BARAT", "PAPUA SELATAN", "PAPUA TENGAH", "PAPUA PEGUNUNGAN"
            });
            put("kabupaten", new String[]{});
        }});

        // Zona Default/Unknown (fallback)
        SHIPPING_ZONES_CONFIG.put("Unknown", new HashMap<String, String[]>() {{
            put("provinsi", new String[]{});
            put("kabupaten", new String[]{});
        }});

        // Panggil metode untuk memuat alamat asal dari database saat kelas dimuat
        loadOriginAddressFromDatabase();
    }

    // --- KONFIGURASI TARIF DASAR PER ZONA ---
    // Ini adalah biaya dasar dan biaya per kg untuk 'Standard' delivery per zona
    private static final Map<String, Map<String, Double>> BASE_SHIPPING_RATES_PER_ZONE = new HashMap<>();

    static {
        BASE_SHIPPING_RATES_PER_ZONE.put("Lokal_Bekasi", new HashMap<String, Double>() {{
            put("base_cost", 9000.0);
            put("cost_per_kg", 2000.0);
        }});
        BASE_SHIPPING_RATES_PER_ZONE.put("Jabodetabek", new HashMap<String, Double>() {{
            put("base_cost", 12000.0);
            put("cost_per_kg", 3500.0);
        }});
        BASE_SHIPPING_RATES_PER_ZONE.put("Jawa_Barat_Lainnya", new HashMap<String, Double>() {{
            put("base_cost", 15000.0);
            put("cost_per_kg", 4000.0);
        }});
        BASE_SHIPPING_RATES_PER_ZONE.put("Jawa_Tengah_DIY", new HashMap<String, Double>() {{
            put("base_cost", 18000.0);
            put("cost_per_kg", 6000.0);
        }});
        BASE_SHIPPING_RATES_PER_ZONE.put("Jawa_Timur", new HashMap<String, Double>() {{
            put("base_cost", 22000.0);
            put("cost_per_kg", 7500.0);
        }});
        BASE_SHIPPING_RATES_PER_ZONE.put("Sumatera", new HashMap<String, Double>() {{
            put("base_cost", 30000.0);
            put("cost_per_kg", 10000.0);
        }});
        BASE_SHIPPING_RATES_PER_ZONE.put("Kalimantan", new HashMap<String, Double>() {{
            put("base_cost", 35000.0);
            put("cost_per_kg", 12000.0);
        }});
        BASE_SHIPPING_RATES_PER_ZONE.put("Sulawesi", new HashMap<String, Double>() {{
            put("base_cost", 40000.0);
            put("cost_per_kg", 15000.0);
        }});
        BASE_SHIPPING_RATES_PER_ZONE.put("Bali_Nusa_Tenggara", new HashMap<String, Double>() {{
            put("base_cost", 38000.0);
            put("cost_per_kg", 13000.0);
        }});
        BASE_SHIPPING_RATES_PER_ZONE.put("Maluku_Papua", new HashMap<String, Double>() {{
            put("base_cost", 50000.0);
            put("cost_per_kg", 20000.0);
        }});
        BASE_SHIPPING_RATES_PER_ZONE.put("Unknown", new HashMap<String, Double>() {{
            put("base_cost", 50000.0); 
            put("cost_per_kg", 25000.0);
        }});
    }

    // --- KONFIGURASI PENYEDIA PENGIRIMAN ---
    // Struktur: ProviderName -> { "price_multiplier" -> faktor, "min_days" -> hari, "max_days" -> hari }
    private static final Map<String, Map<String, Double>> SHIPPING_PROVIDERS_CONFIG = new HashMap<>();

    static {
        SHIPPING_PROVIDERS_CONFIG.put("JNE Reguler", new HashMap<String, Double>() {{
            put("price_multiplier", 1.0);
            put("min_days", 2.0); // Default for non-local
            put("max_days", 4.0); // Default for non-local
        }});
        SHIPPING_PROVIDERS_CONFIG.put("TIKI Reguler", new HashMap<String, Double>() {{
            put("price_multiplier", 1.05);
            put("min_days", 2.0);
            put("max_days", 4.0);
        }});
        SHIPPING_PROVIDERS_CONFIG.put("POS Kilat Khusus", new HashMap<String, Double>() {{
            put("price_multiplier", 0.95); // Slightly cheaper
            put("min_days", 3.0);
            put("max_days", 5.0);
        }});
        SHIPPING_PROVIDERS_CONFIG.put("SiCepat Reg", new HashMap<String, Double>() {{
            put("price_multiplier", 0.98);
            put("min_days", 2.0);
            put("max_days", 3.0);
        }});
        SHIPPING_PROVIDERS_CONFIG.put("Anteraja Reguler", new HashMap<String, Double>() {{
            put("price_multiplier", 1.0);
            put("min_days", 2.0);
            put("max_days", 4.0);
        }});
        SHIPPING_PROVIDERS_CONFIG.put("JNE YES (Yakin Esok Sampai)", new HashMap<String, Double>() {{
            put("price_multiplier", 1.8); // Express is more expensive
            put("min_days", 1.0);
            put("max_days", 1.0);
        }});
        SHIPPING_PROVIDERS_CONFIG.put("TIKI ONS (Over Night Service)", new HashMap<String, Double>() {{
            put("price_multiplier", 1.9); // Express is more expensive
            put("min_days", 1.0);
            put("max_days", 1.0);
        }});
    }

    /**
     * Memuat alamat asal (provinsi dan kabupaten) dari database,
     * mencari user dengan role 'supervisor'.
     */
    private static void loadOriginAddressFromDatabase() {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            
            // 1. Dapatkan user_id dari user dengan role 'supervisor'
            String getUserIdQuery = "SELECT id FROM users WHERE role = 'seller' LIMIT 1";
            stmt = conn.prepareStatement(getUserIdQuery);
            rs = stmt.executeQuery();

            int supervisorUserId = -1;
            if (rs.next()) {
                supervisorUserId = rs.getInt("id");
                System.out.println("DEBUG ShippingCalculator: Ditemukan supervisor_user_id: " + supervisorUserId);
            } else {
                System.err.println("WARNING ShippingCalculator: Tidak ada user dengan role 'supervisor' ditemukan.");
                // Set default atau kosong jika tidak ada supervisor
                ORIGIN_PROVINCE = "UNKNOWN";
                ORIGIN_KABUPATEN = "UNKNOWN";
                return;
            }
            
            // Tutup ResultSet dan Statement sebelumnya sebelum membuat yang baru
            rs.close();
            stmt.close();

            // 2. Dapatkan alamat dari tabel addresses menggunakan user_id supervisor
            String getAddressQuery = "SELECT province, city FROM addresses WHERE user_id = ? ORDER BY id ASC LIMIT 1";
            stmt = conn.prepareStatement(getAddressQuery);
            stmt.setInt(1, supervisorUserId);
            rs = stmt.executeQuery();

            if (rs.next()) {
                ORIGIN_PROVINCE = rs.getString("province").toUpperCase();
                ORIGIN_KABUPATEN = rs.getString("city").toUpperCase();
                System.out.println("DEBUG ShippingCalculator: Alamat asal dimuat - Provinsi: " + ORIGIN_PROVINCE + ", Kabupaten: " + ORIGIN_KABUPATEN);
            } else {
                System.err.println("WARNING ShippingCalculator: Tidak ada alamat ditemukan untuk supervisor_user_id: " + supervisorUserId + ". Menggunakan alamat 'UNKNOWN'.");
                ORIGIN_PROVINCE = "UNKNOWN";
                ORIGIN_KABUPATEN = "UNKNOWN";
            }

        } catch (SQLException e) {
            System.err.println("ERROR ShippingCalculator: Gagal memuat alamat asal dari database: " + e.getMessage());
            e.printStackTrace();
            // Fallback jika ada error database
            ORIGIN_PROVINCE = "UNKNOWN";
            ORIGIN_KABUPATEN = "UNKNOWN";
        } finally {
            DatabaseConnection.closeConnection(conn, stmt, rs);
        }
    }

    /**
     * Menentukan zona pengiriman berdasarkan provinsi dan kabupaten tujuan.
     * Menggunakan konfigurasi SHIPPING_ZONES_CONFIG.
     *
     * @param province Nama provinsi tujuan (akan dikonversi ke uppercase).
     * @param kabupaten Nama kabupaten tujuan (akan dikonversi ke uppercase).
     * @return Nama zona yang cocok (misal: "Jabodetabek", "Jawa_Timur"), atau "Unknown" jika tidak ditemukan.
     */
    public static String getShippingZone(String province, String kabupaten) {
        String upperProvince = province.toUpperCase();
        String upperKabupaten = kabupaten.toUpperCase();

        // Prioritaskan zona Lokal_Bekasi jika pengirim dan penerima di Bekasi
        if (ORIGIN_PROVINCE != null && ORIGIN_KABUPATEN != null &&
            upperProvince.equals(ORIGIN_PROVINCE) && upperKabupaten.equals(ORIGIN_KABUPATEN)) {
            return "Lokal_Bekasi";
        }
        
        for (Map.Entry<String, Map<String, String[]>> entry : SHIPPING_ZONES_CONFIG.entrySet()) {
            String zoneName = entry.getKey();
            Map<String, String[]> zoneData = entry.getValue();

            // Lewati zona 'Lokal_Bekasi' karena sudah ditangani di atas
            if (zoneName.equals("Lokal_Bekasi")) {
                continue;
            }

            String[] zoneProvinces = zoneData.get("provinsi");
            String[] zoneKabupaten = zoneData.get("kabupaten");

            boolean provinceMatch = false;
            if (zoneProvinces != null) {
                for (String zp : zoneProvinces) {
                    if (upperProvince.equals(zp)) {
                        provinceMatch = true;
                        break;
                    }
                }
            }

            if (provinceMatch) {
                if (zoneKabupaten == null || zoneKabupaten.length == 0) {
                    return zoneName;
                } else {
                    for (String zk : zoneKabupaten) {
                        if (upperKabupaten.contains(zk)) { 
                            return zoneName;
                        }
                    }
                }
            }
        }
        return "Unknown"; // Jika tidak ada zona yang cocok
    }

    /**
     * Menghitung total berat barang dari daftar CartItem.
     *
     * @param cartItems Daftar objek CartItem dari keranjang.
     * @return Total berat dalam kilogram.
     */
    public static double calculateTotalWeight(java.util.List<ProductRepository.CartItem> cartItems) {
        double totalWeight = 0.0;
        if (cartItems != null) {
            for (ProductRepository.CartItem item : cartItems) {
                totalWeight += item.getWeight() * item.getQuantity();
            }
        }
        return totalWeight;
    }

    /**
     * Menghitung biaya pengiriman untuk berbagai penyedia layanan berdasarkan total berat dan kode pos tujuan.
     *
     * @param totalWeightKg Berat total barang dalam kilogram.
     * @param destinationKodepos Kode pos tujuan.
     * @return Daftar objek ShippingCostResult, satu untuk setiap penyedia layanan, atau objek error jika terjadi masalah.
     */
    public static List<ShippingCostResult> calculateShippingCosts(double totalWeightKg, String destinationKodepos) {
        List<ShippingCostResult> results = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            String query = "SELECT provinsi, kabupaten FROM tbl_kodepos WHERE kodepos = ? LIMIT 1";
            stmt = conn.prepareStatement(query);
            stmt.setString(1, destinationKodepos);
            rs = stmt.executeQuery();

            if (!rs.next()) {
                results.add(new ShippingCostResult("Kode pos tujuan tidak ditemukan: " + destinationKodepos));
                return results;
            }

            String destinationProvince = rs.getString("provinsi");
            String destinationKabupaten = rs.getString("kabupaten");

            String destinationZone = getShippingZone(destinationProvince, destinationKabupaten);

            if (destinationZone.equals("Unknown") && !destinationKodepos.isEmpty()) {
                System.err.println("Zona pengiriman tidak dapat ditentukan untuk: " + destinationProvince + ", " + destinationKabupaten + ". Menggunakan zona Unknown.");
            }
            
            // Dapatkan tarif dasar per zona
            Map<String, Double> baseRates = BASE_SHIPPING_RATES_PER_ZONE.get(destinationZone);
            if (baseRates == null) {
                baseRates = BASE_SHIPPING_RATES_PER_ZONE.get("Unknown");
                if (baseRates == null) {
                    results.add(new ShippingCostResult("Konfigurasi tarif dasar pengiriman tidak lengkap untuk zona Unknown."));
                    return results;
                }
            }

            double baseCostPerZone = baseRates.get("base_cost");
            double costPerKgPerZone = baseRates.get("cost_per_kg");

            // Aturan perhitungan berat: minimal 1 kg, dibulatkan ke atas
            double chargeableWeight = Math.max(1.0, Math.ceil(totalWeightKg));
            
            // Hitung biaya untuk setiap penyedia layanan
            for (Map.Entry<String, Map<String, Double>> providerEntry : SHIPPING_PROVIDERS_CONFIG.entrySet()) {
                String providerName = providerEntry.getKey();
                Map<String, Double> providerProps = providerEntry.getValue();

                double priceMultiplier = providerProps.get("price_multiplier");
                double minDays = providerProps.get("min_days");
                double maxDays = providerProps.get("max_days");

                // Hitung biaya berdasarkan tarif dasar zona dan faktor pengali provider
                double finalCost = (baseCostPerZone + (chargeableWeight * costPerKgPerZone)) * priceMultiplier;

                // Tentukan estimasi tanggal pengiriman
                String arrivalEstimate = calculateDeliveryDates(minDays, maxDays);

                results.add(new ShippingCostResult(
                    providerName, // Nama penyedia
                    finalCost,
                    destinationZone,
                    baseCostPerZone,
                    costPerKgPerZone,
                    chargeableWeight,
                    arrivalEstimate
                ));
            }

            return results;

        } catch (SQLException e) {
            System.err.println("Error saat menghitung ongkir: " + e.getMessage());
            e.printStackTrace();
            results.add(new ShippingCostResult("Database Error: " + e.getMessage()));
            return results;
        } finally {
            DatabaseConnection.closeConnection(conn, stmt, rs);
        }
    }

    /**
     * Mengitung rentang tanggal pengiriman berdasarkan min_days dan max_days.
     * @param minDays Hari minimum pengiriman dari sekarang.
     * @param maxDays Hari maksimum pengiriman dari sekarang.
     * @return String format tanggal pengiriman (misal: "12 - 14 Jul").
     */
    private static String calculateDeliveryDates(double minDays, double maxDays) {
        LocalDate orderDate = LocalDate.now();
        LocalDate minDeliveryDate = orderDate.plusDays((long) Math.floor(minDays));
        LocalDate maxDeliveryDate = orderDate.plusDays((long) Math.ceil(maxDays));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM");

        if (minDeliveryDate.isEqual(maxDeliveryDate)) {
            return String.format("Sampai %s", minDeliveryDate.format(formatter));
        } else {
            return String.format("%s - %s", minDeliveryDate.format(formatter), maxDeliveryDate.format(formatter));
        }
    }
}