package e.commerce;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.awt.Color; // Digunakan oleh CartItem untuk imageColor
import java.awt.Image; // Digunakan oleh ProductItem untuk gambar
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageIO; // Digunakan untuk membaca gambar
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import e.commerce.AddressUI.Address; // Import Address dari AddressUI
import e.commerce.AddressUI.ShippingService; // Import ShippingService dari AddressUI


/**
 * Kelas repositori untuk mengelola interaksi database terkait produk dan item keranjang.
 * Menyediakan metode untuk mengambil, menyimpan, memperbarui, dan menghapus data produk dan keranjang.
 */
public class ProductRepository {

    /**
     * Inner class publik dan statis untuk merepresentasikan item produk di keranjang.
     * Termasuk detail produk, data gambar BLOB, dan status pilihan dalam UI.
     * Ini dapat diakses langsung melalui `ProductRepository.CartItem`.
     */
    public static class CartItem {
        private int id;
        private String name;
        private double price;
        private double originalPrice;
        private int quantity;
        private String imageColor; // Warna hex untuk fallback/placeholder
        private byte[] imageData;    // Data gambar BLOB
        private String fileExtension; // Ekstensi file untuk BLOB
        private boolean isSelected;  // Properti untuk mengelola pilihan di CartUI

        /**
         * Konstruktor untuk item keranjang dengan data gambar BLOB.
         * @param id ID produk.
         * @param name Nama produk.
         * @param price Harga produk saat ini.
         * @param originalPrice Harga asli produk (untuk diskon).
         * @param quantity Kuantitas item di keranjang.
         * @param imageColor Warna fallback/placeholder (hex string).
         * @param imageData Data gambar BLOB sebagai array byte.
         * @param fileExtension Ekstensi file gambar (misal: "jpg", "png").
         */
        public CartItem(int id, String name, double price, double originalPrice, int quantity, String imageColor, byte[] imageData, String fileExtension) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.originalPrice = originalPrice;
            this.quantity = quantity;
            this.imageColor = imageColor;
            this.imageData = imageData;
            this.fileExtension = fileExtension;
            this.isSelected = true; // Secara default terpilih saat dimuat ke UI keranjang
        }

        /**
         * Konstruktor overload untuk item keranjang tanpa data gambar BLOB eksplisit,
         * hanya menggunakan warna sebagai fallback.
         * @param id ID produk.
         * @param name Nama produk.
         * @param price Harga produk saat ini.
         * @param originalPrice Harga asli produk.
         * @param quantity Kuantitas item di keranjang.
         * @param imageColor Warna fallback/placeholder (hex string).
         */
        public CartItem(int id, String name, double price, double originalPrice, int quantity, String imageColor) {
            this(id, name, price, originalPrice, quantity, imageColor, null, null);
        }

        // --- Getters ---
        public int getId() { return id; }
        public String getName() { return name; }
        public double getPrice() { return price; }
        public double getOriginalPrice() { return originalPrice; }
        public int getQuantity() { return quantity; }
        public String getImageColor() { return imageColor; }
        public byte[] getImageData() { return imageData; }
        public String getFileExtension() { return fileExtension; }
        public boolean isSelected() { return isSelected; }

        // --- Setters ---
        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public void setSelected(boolean selected) {
            this.isSelected = selected;
        }

        // --- Properti yang Dihitung ---
        public double getTotal() {
            return price * quantity;
        }

        public double getSavings() {
            return (originalPrice - price) * quantity;
        }
    }


    /**
     * Mengambil semua produk dari database.
     * Data gambar (BLOB) hanya diambil untuk gambar utama (`is_main_image = TRUE`)
     * untuk efisiensi.
     * @return Sebuah daftar objek FavoriteItem yang merepresentasikan semua produk.
     */
    public static List<FavoritesUI.FavoriteItem> getAllProducts() {
        String sql = "SELECT p.product_id, p.name, p.description, p.price, p.original_price, p.stock, p.`condition`, p.min_order, p.`brand`, " +
                     "pi.image_data, pi.file_extension FROM products p " +
                     "LEFT JOIN product_images pi ON p.product_id = pi.product_id AND pi.is_main_image = TRUE"; // Hanya ambil gambar utama

        List<FavoritesUI.FavoriteItem> products = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("product_id");
                String name = rs.getString("name");
                String description = rs.getString("description");
                double price = rs.getDouble("price");
                double originalPrice = rs.getDouble("original_price");
                int stock = rs.getInt("stock");
                String condition = rs.getString("condition");
                String minOrder = rs.getString("min_order");
                String brand = rs.getString("brand");

                // Ambil data gambar BLOB dan ekstensi dari ResultSet
                byte[] imageData = rs.getBytes("image_data"); // Dari tabel product_images
                String fileExtension = rs.getString("file_extension"); // Dari tabel product_images

                // Gunakan warna dummy. Jika Anda ingin warna spesifik produk,
                // tambahkan kolom 'color_hex' ke tabel produk dan ambil di sini.
                String hexColor = "#FDF8E8";

                FavoritesUI.FavoriteItem product = new FavoritesUI.FavoriteItem(
                    id, name, description, price, originalPrice,
                    stock, condition, minOrder, brand,
                    hexColor, null // List<String> imagePaths sekarang tidak digunakan
                );

                // Set data BLOB gambar ke objek FavoriteItem
                if (imageData != null) {
                    List<byte[]> singleImageList = new ArrayList<>();
                    singleImageList.add(imageData);
                    List<String> singleExtensionList = new ArrayList<>();
                    singleExtensionList.add(fileExtension != null ? fileExtension : "jpg"); // Gunakan ekstensi asli atau default
                    product.setImageDataLists(singleImageList, singleExtensionList);
                }
                products.add(product);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching all products (with main image): " + e.getMessage());
            e.printStackTrace();
        }
        return products;
    }

    /**
     * Mengambil satu produk berdasarkan ID-nya, termasuk semua BLOB gambar terkait.
     * Ini digunakan untuk menampilkan detail produk di mana semua gambar diperlukan.
     * @param productId ID produk yang akan diambil.
     * @return Objek FavoriteItem dengan detail lengkap dan gambar yang dimuat, atau null jika tidak ditemukan.
     */
    public static FavoritesUI.FavoriteItem getProductById(int productId) {
        // Query SQL untuk menggabungkan tabel 'products' dan 'product_images'
        String sql = "SELECT p.product_id, p.name, p.description, p.price, p.original_price, p.stock, p.`condition`, p.min_order, p.`brand`, " +
                     "pi.image_data, pi.file_extension FROM products p LEFT JOIN product_images pi ON p.product_id = pi.product_id WHERE p.product_id = ?";

        FavoritesUI.FavoriteItem product = null;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, productId);
            try (ResultSet rs = pstmt.executeQuery()) {
                List<byte[]> imageDataList = new ArrayList<>();
                List<String> fileExtensionList = new ArrayList<>();

                // Proses baris pertama untuk mendapatkan detail produk
                if (rs.next()) {
                    int id = rs.getInt("product_id");
                    String name = rs.getString("name");
                    String description = rs.getString("description");
                    double price = rs.getDouble("price");
                    double originalPrice = rs.getDouble("original_price");
                    int stock = rs.getInt("stock");
                    String condition = rs.getString("condition");
                    String minOrder = rs.getString("min_order");
                    String brand = rs.getString("brand");
                    String hexColor = "#FDF8E8"; // Warna dummy

                    product = new FavoritesUI.FavoriteItem(
                        id, name, description, price, originalPrice,
                        stock, condition, minOrder, brand,
                        hexColor, null
                    );

                    // Tambahkan gambar pertama yang ditemukan
                    byte[] imageData = rs.getBytes("image_data");
                    String fileExtension = rs.getString("file_extension");
                    if (imageData != null) {
                        imageDataList.add(imageData);
                        fileExtensionList.add(fileExtension);
                    }

                    // Tambahkan gambar-gambar lainnya untuk produk yang sama (jika ada)
                    while (rs.next()) {
                        imageData = rs.getBytes("image_data");
                        fileExtension = rs.getString("file_extension");
                        if (imageData != null) {
                            imageDataList.add(imageData);
                            fileExtensionList.add(fileExtension);
                        }
                    }
                    product.setImageDataLists(imageDataList, fileExtensionList); // Set semua data gambar yang terkumpul
                }
            }
            return product;

        } catch (SQLException e) {
            System.err.println("Error fetching product by ID: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Mengambil produk yang diunggah oleh penjual tertentu (supervisor/admin).
     * Hanya mengambil gambar utama (`is_main_image = TRUE`) untuk setiap produk untuk efisiensi.
     * @param sellerId ID penjual (pengguna).
     * @return Sebuah daftar objek FavoriteItem yang merepresentasikan produk-produk penjual.
     */
    public static List<FavoritesUI.FavoriteItem> getProductsBySeller(int sellerId) {
        String sql = "SELECT p.product_id, p.name, p.description, p.price, p.original_price, p.stock, p.`condition`, p.min_order, p.`brand`, " +
                     "pi.image_data, pi.file_extension FROM products p " +
                     "LEFT JOIN product_images pi ON p.product_id = pi.product_id AND pi.is_main_image = TRUE " + // Hanya ambil gambar utama
                     "WHERE p.seller_id = ?";

        List<FavoritesUI.FavoriteItem> products = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, sellerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("product_id");
                    String name = rs.getString("name");
                    String description = rs.getString("description");
                    double price = rs.getDouble("price");
                    double originalPrice = rs.getDouble("original_price");
                    int stock = rs.getInt("stock");
                    byte[] imageData = rs.getBytes("image_data");
                    String fileExtension = rs.getString("file_extension");
                    String condition = rs.getString("condition");
                    String minOrder = rs.getString("min_order");
                    String brand = rs.getString("brand");

                    String hexColor = "#FDF8E8";

                    FavoritesUI.FavoriteItem product = new FavoritesUI.FavoriteItem(
                        id, name, description, price, originalPrice,
                        stock, condition, minOrder, brand,
                        hexColor, null
                    );

                    if (imageData != null) {
                        List<byte[]> singleImageList = new ArrayList<>();
                        singleImageList.add(imageData);
                        List<String> singleExtensionList = new ArrayList<>();
                        singleExtensionList.add(fileExtension != null ? fileExtension : "jpg");
                        product.setImageDataLists(singleImageList, singleExtensionList);
                    }
                    products.add(product);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching products by seller: " + e.getMessage());
            e.printStackTrace();
        }
        return products;
    }

    /**
     * Mengambil produk yang direkomendasikan berdasarkan merek, tidak termasuk produk tertentu.
     * Hanya mengambil gambar utama untuk setiap produk yang direkomendasikan untuk efisiensi.
     * @param brand Merek yang akan dicocokkan.
     * @param excludeProductId ID produk yang akan dikecualikan dari rekomendasi.
     * @return Sebuah daftar objek FavoriteItem yang direkomendasikan.
     */
    public static List<FavoritesUI.FavoriteItem> getProductsByBrand(String brand, int excludeProductId) {
        String sql = "SELECT p.product_id, p.name, p.description, p.price, p.original_price, p.stock, p.`condition`, p.min_order, p.`brand`, " +
                     "pi.image_data, pi.file_extension FROM products p " +
                     "LEFT JOIN product_images pi ON p.product_id = pi.product_id AND pi.is_main_image = TRUE " +
                     "WHERE p.`brand` = ? AND p.product_id != ? LIMIT 6";

        List<FavoritesUI.FavoriteItem> recommendedProducts = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, brand);
            pstmt.setInt(2, excludeProductId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("product_id");
                    String name = rs.getString("name");
                    String description = rs.getString("description");
                    double price = rs.getDouble("price");
                    double originalPrice = rs.getDouble("original_price");
                    int stock = rs.getInt("stock");
                    byte[] imageData = rs.getBytes("image_data");
                    String fileExtension = rs.getString("file_extension");
                    String condition = rs.getString("condition");
                    String minOrder = rs.getString("min_order");
                    String foundBrand = rs.getString("brand");
                    String hexColor = "#FDF8E8";

                    FavoritesUI.FavoriteItem product = new FavoritesUI.FavoriteItem(
                        id, name, description, price, originalPrice,
                        stock, condition, minOrder, foundBrand,
                        hexColor, null
                    );

                    if (imageData != null) {
                        List<byte[]> singleImageList = new ArrayList<>();
                        singleImageList.add(imageData);
                        List<String> singleExtensionList = new ArrayList<>();
                        singleExtensionList.add(fileExtension != null ? fileExtension : "jpg");
                        product.setImageDataLists(singleImageList, singleExtensionList);
                    }
                    recommendedProducts.add(product);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching products by brand: " + e.getMessage());
            e.printStackTrace();
        }
        return recommendedProducts;
    }

    /**
     * Menyimpan BLOB gambar produk ke tabel 'product_images'.
     * Metode ini dipanggil dari UI ketika penjual mengunggah gambar.
     * @param productId ID produk tempat gambar ini berada.
     * @param imageData Array byte dari gambar.
     * @param fileExtension Ekstensi file (misal: "jpg", "png").
     * @param isMainImage True jika ini adalah gambar utama untuk produk.
     * @param orderIndex Urutan tampilan gambar.
     * @return ID yang dihasilkan dari gambar, atau -1 jika penyisipan gagal.
     * @throws SQLException Jika terjadi kesalahan database.
     */
    public static int saveProductImage(int productId, byte[] imageData, String fileExtension, boolean isMainImage, int orderIndex) throws SQLException {
        String sql = "INSERT INTO product_images (product_id, image_data, file_extension, is_main_image, order_index) VALUES (?, ?, ?, ?, ?)";
        int generatedId = -1;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, productId);
            pstmt.setBytes(2, imageData);
            pstmt.setString(3, fileExtension);
            pstmt.setBoolean(4, isMainImage);
            pstmt.setInt(5, orderIndex);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        generatedId = rs.getInt(1);
                    }
                }
            }
            System.out.println("Image saved for product ID: " + productId + ", Image ID: " + generatedId);
        }
        return generatedId;
    }

    /**
     * Menyimpan detail produk baru ke tabel 'products'.
     * Metode ini dipanggil dari UI ketika penjual menambahkan produk baru.
     * @param name Nama produk.
     * @param description Deskripsi produk.
     * @param price Harga produk.
     * @param originalPrice Harga asli (bisa 0.0 jika tidak berlaku).
     * @param stock Kuantitas stok produk.
     * @param condition Kondisi produk (misal: "Baru", "Bekas").
     * @param minOrder Kuantitas pesanan minimum (misal: "1 Buah").
     * @param brand Merek produk.
     * @param sellerId ID pengguna yang menjual produk ini.
     * @return ID yang dihasilkan dari produk baru, atau -1 jika penyisipan gagal.
     * @throws SQLException Jika terjadi kesalahan database.
     */
    public static int saveProduct(String name, String description, double price, double originalPrice, int stock, String condition, String minOrder, String brand, Integer sellerId) throws SQLException {
        String sql = "INSERT INTO products (name, description, price, original_price, stock, `condition`, min_order, `brand`, seller_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        int generatedProductId = -1;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, name);
            pstmt.setString(2, description);
            pstmt.setDouble(3, price);
            pstmt.setDouble(4, originalPrice);
            pstmt.setInt(5, stock);
            pstmt.setString(6, condition);
            pstmt.setString(7, minOrder);
            pstmt.setString(8, brand);
            if (sellerId != null) {
                pstmt.setInt(9, sellerId);
            } else {
                pstmt.setNull(9, java.sql.Types.INTEGER);
            }

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        generatedProductId = rs.getInt(1);
                    }
                }
            }
            System.out.println("Product saved: " + name + ", Product ID: " + generatedProductId);
        }
        return generatedProductId;
    }

    /**
     * Menghapus produk dan gambar terkaitnya dari database.
     * Operasi ini bersifat transaksional (semua atau tidak sama sekali).
     * @param productId ID produk yang akan dihapus.
     * @throws SQLException Jika terjadi kesalahan database selama penghapusan atau manajemen transaksi.
     */
    public static void deleteProduct(int productId) throws SQLException {
        String sqlDeleteImages = "DELETE FROM product_images WHERE product_id = ?";
        String sqlDeleteProduct = "DELETE FROM products WHERE product_id = ?";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Mulai transaksi

            try (PreparedStatement pstmtImages = conn.prepareStatement(sqlDeleteImages)) {
                pstmtImages.setInt(1, productId);
                pstmtImages.executeUpdate();
            }

            try (PreparedStatement pstmtProduct = conn.prepareStatement(sqlDeleteProduct)) {
                pstmtProduct.setInt(1, productId);
                pstmtProduct.executeUpdate();
            }

            conn.commit(); // Commit transaksi
            System.out.println("Product and its images deleted for product ID: " + productId);
        } catch (SQLException e) {
            if (e != null && e.getMessage() != null && e.getMessage().contains("Cannot delete or update a parent row: a foreign key constraint fails")) {
                 System.err.println("Attempted to delete product " + productId + " which might still be referenced elsewhere (e.g., order_items).");
            }
            if (conn != null) {
                try {
                    conn.rollback(); // Rollback saat error
                    System.err.println("Transaction rolled back for product deletion.");
                } catch (SQLException rbex) {
                    System.err.println("Error rolling back transaction: " + rbex.getMessage());
                }
            }
            throw e; // Lemparkan kembali untuk penanganan pemanggil
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // Reset auto-commit
                    conn.close(); // Tutup koneksi
                } catch (SQLException fe) {
                    System.err.println("Error setting auto commit or closing connection: " + fe.getMessage());
                }
            }
        }
    }

    /**
     * Memperbarui detail produk yang ada di tabel 'products'.
     * Pembaruan gambar ditangani secara terpisah (dengan menghapus yang lama dan menyisipkan yang baru).
     * @param productId ID produk yang akan diperbarui.
     * @param name Nama produk baru.
     * @param description Deskripsi produk baru.
     * @param price Harga produk baru.
     * @param originalPrice Harga asli baru.
     * @param stock Kuantitas stok baru.
     * @param condition Kondisi produk baru.
     * @param minOrder Kuantitas pesanan minimum baru.
     * @param brand Merek produk baru.
     * @return True jika produk berhasil diperbarui, false jika tidak.
     * @throws SQLException Jika terjadi kesalahan database.
     */
    public static boolean updateProduct(int productId, String name, String description, double price, double originalPrice, int stock, String condition, String minOrder, String brand) throws SQLException {
        String sql = "UPDATE products SET name = ?, description = ?, price = ?, original_price = ?, stock = ?, `condition` = ?, min_order = ?, `brand` = ? WHERE product_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            pstmt.setString(2, description);
            pstmt.setDouble(3, price);
            pstmt.setDouble(4, originalPrice);
            pstmt.setInt(5, stock);
            pstmt.setString(6, condition);
            pstmt.setString(7, minOrder);
            pstmt.setString(8, brand);
            pstmt.setInt(9, productId);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("Product updated: " + name + ", Product ID: " + productId);
                return true; // Update berhasil
            } else {
                System.out.println("No product found with ID: " + productId + " to update.");
                return false; // Tidak ada baris yang terpengaruh
            }
        }
    }

    /**
     * Menghapus semua gambar yang terkait dengan produk tertentu dari tabel 'product_images'.
     * Ini biasanya digunakan sebelum mengunggah ulang gambar baru untuk produk yang diedit.
     * @param productId ID produk yang gambarnya akan dihapus.
     * @throws SQLException Jika terjadi kesalahan database.
     */
    public static void deleteProductImagesForProduct(int productId) throws SQLException {
        String sql = "DELETE FROM product_images WHERE product_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, productId);
            int affectedRows = pstmt.executeUpdate();
            System.out.println(affectedRows + " images deleted for product ID: " + productId);
        }
    }

    /**
     * Menghapus satu gambar dari tabel 'product_images' berdasarkan ID gambarnya.
     * @param imageId ID gambar yang akan dihapus.
     * @throws SQLException Jika terjadi kesalahan database.
     */
    public static void deleteProductImageById(int imageId) throws SQLException {
        String sql = "DELETE FROM product_images WHERE image_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, imageId);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("Image ID " + imageId + " deleted successfully.");
            } else {
                System.out.println("Image ID " + imageId + " not found for deletion.");
            }
        }
    }

    /**
     * Mengatur gambar tertentu sebagai gambar utama untuk suatu produk.
     * Ini melibatkan pengaturan `is_main_image` menjadi `FALSE` untuk semua gambar lain dari produk,
     * lalu mengaturnya menjadi `TRUE` untuk gambar yang ditentukan. Ini adalah operasi transaksional.
     * @param productId ID produk.
     * @param imageId ID gambar yang akan diatur sebagai utama.
     * @throws SQLException Jika terjadi kesalahan database.
     */
    public static void setMainProductImage(int productId, int imageId) throws SQLException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Mulai transaksi

            // 1. Atur semua gambar untuk produk ini menjadi bukan utama
            String sqlResetMain = "UPDATE product_images SET is_main_image = FALSE WHERE product_id = ?";
            try (PreparedStatement pstmtReset = conn.prepareStatement(sqlResetMain)) {
                pstmtReset.setInt(1, productId);
                pstmtReset.executeUpdate();
            }

            // 2. Atur gambar yang ditentukan sebagai utama
            String sqlSetMain = "UPDATE product_images SET is_main_image = TRUE WHERE image_id = ? AND product_id = ?";
            try (PreparedStatement pstmtSet = conn.prepareStatement(sqlSetMain)) {
                pstmtSet.setInt(1, imageId);
                pstmtSet.setInt(2, productId);
                pstmtSet.executeUpdate();
            }

            conn.commit(); // Commit transaksi
            System.out.println("Image ID " + imageId + " set as main for Product ID " + productId);
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback(); // Rollback saat error
                    System.err.println("Transaction rolled back for setMainProductImage: " + e.getMessage());
                } catch (SQLException rbex) {
                    System.err.println("Error rolling back transaction: " + rbex.getMessage());
                }
            }
            throw e; // Lemparkan kembali untuk penanganan pemanggil
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // Reset auto-commit
                    conn.close(); // Tutup koneksi
                } catch (SQLException fe) {
                    System.err.println("Error setting auto commit or closing connection: " + fe.getMessage());
                }
            }
        }
    }

    /**
     * Mengambil detail semua gambar untuk produk tertentu dari database.
     * Ini digunakan oleh ProductImageManagerUI untuk menampilkan semua gambar untuk diedit.
     * @param productId ID produk.
     * @return Sebuah daftar array Object, masing-masing berisi (image_id, image_data, file_extension, is_main_image, order_index).
     * @throws SQLException Jika terjadi kesalahan database.
     */
    public static List<Object[]> getAllProductImagesDetails(int productId) throws SQLException {
        List<Object[]> images = new ArrayList<>();
        String sql = "SELECT image_id, image_data, file_extension, is_main_image, order_index FROM product_images WHERE product_id = ? ORDER BY order_index ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, productId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    images.add(new Object[]{
                        rs.getInt("image_id"),
                        rs.getBytes("image_data"),
                        rs.getString("file_extension"),
                        rs.getBoolean("is_main_image"),
                        rs.getInt("order_index")
                    });
                }
            }
        }
        return images;
    }

    /**
     * Mengambil item keranjang untuk pengguna tertentu dari database.
     * Menggabungkan dengan tabel produk dan gambar produk untuk mendapatkan detail item lengkap
     * dan gambar utama.
     * @param userId ID pengguna yang sedang login.
     * @return Sebuah daftar objek CartItem.
     */
    public static List<CartItem> getCartItemsForUser(int userId) {
        List<CartItem> cartItems = new ArrayList<>();
        String sql = "SELECT ci.product_id, ci.quantity, " +
                     "p.name, p.price, p.original_price, " +
                     "pi.image_data, pi.file_extension " +
                     "FROM cart_items ci " +
                     "JOIN products p ON ci.product_id = p.product_id " +
                     "LEFT JOIN product_images pi ON p.product_id = pi.product_id AND pi.is_main_image = TRUE " +
                     "WHERE ci.user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int productId = rs.getInt("product_id");
                    int quantity = rs.getInt("quantity");
                    String name = rs.getString("name");
                    double price = rs.getDouble("price");
                    double originalPrice = rs.getDouble("original_price");
                    byte[] imageData = rs.getBytes("image_data");
                    String fileExtension = rs.getString("file_extension");

                    // Menggunakan warna dummy untuk saat ini, karena tidak ada di skema DB Anda untuk produk
                    // Jika Anda ingin warna spesifik produk, tambahkan kolom 'color_hex' ke tabel 'products' dan ambil di sini.
                    String imageColor = "#CCCCCC"; // Warna abu-abu default

                    CartItem item = new CartItem(
                        productId, name, price, originalPrice, quantity,
                        imageColor, imageData, fileExtension
                    );
                    cartItems.add(item);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching cart items for user " + userId + ": " + e.getMessage());
            e.printStackTrace();
        }
        return cartItems;
    }

    /**
     * Menambahkan produk ke keranjang belanja pengguna.
     * Jika produk sudah ada, kuantitasnya akan diperbarui. Jika tidak, item baru akan disisipkan.
     * @param userId ID pengguna.
     * @param productId ID produk.
     * @param quantity Kuantitas yang akan ditambahkan.
     * @throws SQLException Jika terjadi kesalahan database.
     */
    public static void addProductToCart(int userId, int productId, int quantity) throws SQLException {
        // Cek apakah produk sudah ada di keranjang
        String checkSql = "SELECT quantity FROM cart_items WHERE user_id = ? AND product_id = ?";
        String updateSql = "UPDATE cart_items SET quantity = quantity + ? WHERE user_id = ? AND product_id = ?";
        String insertSql = "INSERT INTO cart_items (user_id, product_id, quantity) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {

            checkStmt.setInt(1, userId);
            checkStmt.setInt(2, productId);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                // Produk sudah ada, perbarui kuantitas
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setInt(1, quantity);
                    updateStmt.setInt(2, userId);
                    updateStmt.setInt(3, productId);
                    updateStmt.executeUpdate();
                }
                System.out.println("Updated quantity of product " + productId + " for user " + userId + " in cart.");
            } else {
                // Produk belum ada, sisipkan item baru
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setInt(1, userId);
                    insertStmt.setInt(2, productId);
                    insertStmt.setInt(3, quantity);
                    insertStmt.executeUpdate();
                }
                System.out.println("Added product " + productId + " to cart for user " + userId + " with quantity " + quantity + ".");
            }
        } catch (SQLException e) {
            System.err.println("Error adding product to cart: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Memperbarui kuantitas item di keranjang belanja pengguna.
     * @param userId ID pengguna.
     * @param productId ID produk.
     * @param newQuantity Kuantitas baru untuk produk.
     * @throws SQLException Jika terjadi kesalahan database.
     */
    public static void updateCartItemQuantity(int userId, int productId, int newQuantity) throws SQLException {
        String sql = "UPDATE cart_items SET quantity = ? WHERE user_id = ? AND product_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, newQuantity);
            pstmt.setInt(2, userId);
            pstmt.setInt(3, productId);
            pstmt.executeUpdate();
            System.out.println("Updated quantity for product " + productId + " to " + newQuantity + " for user " + userId + ".");
        } catch (SQLException e) {
            System.err.println("Error updating cart item quantity: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Menghapus produk dari keranjang belanja pengguna.
     * @param userId ID pengguna.
     * @param productId ID produk yang akan dihapus.
     * @throws SQLException Jika terjadi kesalahan database.
     */
    public static void removeProductFromCart(int userId, int productId) throws SQLException {
        String sql = "DELETE FROM cart_items WHERE user_id = ? AND product_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, productId);
            pstmt.executeUpdate();
            System.out.println("Removed product " + productId + " from cart for user " + userId + ".");
        } catch (SQLException e) {
            System.err.println("Error removing product from cart: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Mengosongkan seluruh keranjang belanja untuk pengguna tertentu.
     * @param userId ID pengguna.
     * @throws SQLException Jika terjadi kesalahan database.
     */
    public static void clearCart(int userId) throws SQLException {
        String sql = "DELETE FROM cart_items WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
            System.out.println("Cleared cart for user " + userId + ".");
        } catch (SQLException e) {
            System.err.println("Error clearing cart: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Membuat pesanan baru di database dan memindahkan item dari keranjang ke order_items.
     *
     * @param userId            ID pengguna yang membuat pesanan.
     * @param cartItems         Daftar item dalam keranjang pengguna.
     * @param totalAmount       Jumlah total akhir pesanan.
     * @param shippingAddress   Alamat pengiriman yang dipilih.
     * @param shippingService   Jasa pengiriman yang dipilih.
     * @param paymentMethod     Metode pembayaran yang dipilih (misalnya "Credit Card", "Bank Transfer").
     * @return ID pesanan yang baru dibuat, atau -1 jika operasi gagal.
     * @throws SQLException jika terjadi kesalahan akses database.
     */
    public static int createOrder(int userId, List<CartItem> cartItems, double totalAmount,
                                  Address shippingAddress, ShippingService shippingService,
                                  String paymentMethod) throws SQLException {
        Connection conn = null;
        PreparedStatement orderStmt = null;
        PreparedStatement itemStmt = null;
        int orderId = -1;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Mulai transaksi

            // 1. Insert ke tabel orders
            String orderSql = "INSERT INTO orders (user_id, order_number, order_date, total_amount, " +
                              "shipping_address_id, shipping_service_name, shipping_cost, payment_method, order_status, delivery_estimate) " +
                              "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            orderStmt = conn.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS);

            String orderNumber = generateUniqueOrderNumber(); // Metode untuk menghasilkan nomor pesanan unik
            LocalDate orderDate = LocalDate.now();

            orderStmt.setInt(1, userId);
            orderStmt.setString(2, orderNumber);
            orderStmt.setDate(3, java.sql.Date.valueOf(orderDate));
            orderStmt.setDouble(4, totalAmount);
            orderStmt.setInt(5, shippingAddress.getId());
            orderStmt.setString(6, shippingService.name);
            orderStmt.setDouble(7, shippingService.price);
            orderStmt.setString(8, paymentMethod);
            orderStmt.setString(9, "Processing"); // Status awal
            orderStmt.setString(10, shippingService.arrivalEstimate);

            int affectedRows = orderStmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Membuat pesanan gagal, tidak ada baris yang terpengaruh.");
            }

            try (ResultSet generatedKeys = orderStmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    orderId = generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Membuat pesanan gagal, tidak ada ID yang didapatkan.");
                }
            }

            // 2. Insert ke tabel order_items untuk setiap item keranjang
            String itemSql = "INSERT INTO order_items (order_id, product_id, product_name, quantity, price_per_item, original_price_per_item) VALUES (?, ?, ?, ?, ?, ?)";
            itemStmt = conn.prepareStatement(itemSql);

            for (CartItem item : cartItems) {
                itemStmt.setInt(1, orderId);
                itemStmt.setInt(2, item.getId()); // Asumsi CartItem.getId() mengembalikan product_id
                itemStmt.setString(3, item.getName());
                itemStmt.setInt(4, item.getQuantity());
                itemStmt.setDouble(5, item.getPrice());
                itemStmt.setDouble(6, item.getOriginalPrice());
                itemStmt.addBatch();
            }
            itemStmt.executeBatch();

            // 3. Mengosongkan keranjang pengguna setelah pesanan berhasil dibuat
            clearCart(userId); // Menggunakan clearCart yang lebih umum

            conn.commit(); // Commit transaksi
            System.out.println("Pesanan " + orderNumber + " berhasil dibuat dengan ID: " + orderId);
            return orderId;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback(); // Rollback transaksi jika terjadi error
                    System.err.println("Transaksi di-rollback karena error: " + e.getMessage());
                } catch (SQLException ex) {
                    System.err.println("Error saat rollback: " + ex.getMessage());
                }
            }
            throw e; // Lemparkan kembali exception agar UI bisa menanganinya
        } finally {
            DatabaseConnection.closeConnection(conn, orderStmt); // itemStmt akan ditutup saat orderStmt ditutup jika dibatch
            if (itemStmt != null) {
                itemStmt.close();
            }
        }
    }

    // Metode bantuan untuk menghasilkan nomor pesanan unik (bisa Anda tingkatkan)
    private static String generateUniqueOrderNumber() {
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyMMdd");
        String datePart = today.format(formatter);

        Random random = new Random();
        String randomPart1 = String.format("%05d", random.nextInt(100000));
        String randomPart2 = String.format("%06d", random.nextInt(1000000));

        return datePart + "-" + randomPart1 + "-" + randomPart2;
    }

    /**
     * Mengambil semua pesanan untuk pengguna tertentu dari database.
     *
     * @param userId ID pengguna.
     * @return Daftar objek Order.
     * @throws SQLException jika terjadi kesalahan akses database.
     */
    public static List<Order> getOrdersForUser(int userId) throws SQLException {
        List<Order> orders = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT o.id, o.order_number, o.order_date, o.total_amount, o.order_status, " +
                         "o.delivery_estimate, o.shipping_service_name, o.shipping_cost, o.user_id," +
                         "a.full_address, a.city, a.province, a.postal_code, o.payment_method " +
                         "FROM orders o JOIN addresses a ON o.shipping_address_id = a.id " +
                         "WHERE o.user_id = ? ORDER BY o.order_date DESC, o.id DESC";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            rs = stmt.executeQuery();

            while (rs.next()) {
                Order order = new Order(
                    rs.getInt("id"),
                    rs.getString("order_number"),
                    rs.getDate("order_date").toLocalDate(),
                    rs.getDouble("total_amount"),
                    rs.getString("order_status"),
                    rs.getString("delivery_estimate"),
                    rs.getString("shipping_service_name"),
                    rs.getDouble("shipping_cost"),
                    rs.getString("full_address") + ", " + rs.getString("city") + ", " + rs.getString("province") + ", " + rs.getString("postal_code"),
                    rs.getString("payment_method"),
                    rs.getInt("user_id")
                     
                );
                order.setItems(getOrderItemsForOrder(order.getId()));
                orders.add(order);
            }
        } finally {
            DatabaseConnection.closeConnection(conn, stmt, rs);
        }
        return orders;
    }

    public static List<OrderItem> getOrderItemsForOrder(int orderId) throws SQLException {
        List<OrderItem> items = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT oi.id, oi.product_id, oi.product_name, oi.quantity, oi.price_per_item, oi.original_price_per_item, " +
                         "p.brand, " +
                         "pi.image_data, pi.file_extension, " +
                         "u.username AS seller_username " + // Ambil username sebagai seller_username
                         "FROM order_items oi " +
                         "JOIN products p ON oi.product_id = p.product_id " + // KOREKSI JOIN KEY: p.product_id
                         "LEFT JOIN product_images pi ON oi.product_id = pi.product_id AND pi.is_main_image = 1 " + // LEFT JOIN untuk gambar utama
                         "LEFT JOIN users u ON p.seller_id = u.id " + // JOIN ke tabel users
                         "WHERE oi.order_id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, orderId);
            rs = stmt.executeQuery();

            while (rs.next()) {
                String sellerName = rs.getString("seller_username");
                if (sellerName == null) sellerName = "Penjual"; // Fallback jika username seller tidak ditemukan

                items.add(new OrderItem(
                    rs.getInt("id"),
                    rs.getInt("product_id"),
                    rs.getString("product_name"),
                    rs.getInt("quantity"),
                    rs.getDouble("price_per_item"),
                    rs.getDouble("original_price_per_item"),
                    rs.getBytes("image_data"),
                    rs.getString("brand"),
                    rs.getString("file_extension"),
                    sellerName // Lewatkan nama seller
                ));
            }
        } finally {
            DatabaseConnection.closeConnection(conn, stmt, rs);
        }
        return items;
    }

    /**
     * Memperbarui status pesanan di database.
     *
     * @param orderId   ID pesanan yang akan diperbarui.
     * @param newStatus Status baru (misalnya "Cancelled", "Delivered").
     * @throws SQLException jika terjadi kesalahan akses database.
     */
    public static void updateOrderStatus(int orderId, String newStatus) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DatabaseConnection.getConnection();
            String sql = "UPDATE orders SET order_status = ? WHERE id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, newStatus);
            stmt.setInt(2, orderId);
            stmt.executeUpdate();
            System.out.println("Status pesanan ID " + orderId + " diperbarui menjadi: " + newStatus);
        } finally {
            DatabaseConnection.closeConnection(conn, stmt);
        }
    }


    // Kelas inner untuk objek Order (representasi baris dari tabel 'orders')
    public static class Order {
        private int id;
        private String orderNumber;
        private LocalDate orderDate;
        private double totalAmount;
        private String orderStatus;
        private String deliveryEstimate;
        private String shippingServiceName;
        private double shippingCost; 
        private String fullShippingAddress;
        private List<OrderItem> items; 
        private String paymentMethod;
        private int userId;

        // Konstruktor yang diperbarui (10 parameter)
        public Order(int id, String orderNumber, LocalDate orderDate, double totalAmount, String orderStatus,
                     String deliveryEstimate, String shippingServiceName, double shippingCost,
                     String fullShippingAddress, String paymentMethod, int userId) {
            this.id = id;
            this.orderNumber = orderNumber;
            this.orderDate = orderDate;
            this.totalAmount = totalAmount;
            this.orderStatus = orderStatus;
            this.deliveryEstimate = deliveryEstimate;
            this.shippingServiceName = shippingServiceName;
            this.shippingCost = shippingCost;
            this.fullShippingAddress = fullShippingAddress;
            this.paymentMethod = paymentMethod;
            this.items = new ArrayList<>(); // Inisialisasi daftar item kosong secara default
        }

        // --- Getters ---
        public int getId() { return id; }
        public String getOrderNumber() { return orderNumber; }
        public LocalDate getOrderDate() { return orderDate; }
        public double getTotalAmount() { return totalAmount; }
        public String getOrderStatus() { return orderStatus; }
        public String getDeliveryEstimate() { return deliveryEstimate; }
        public String getShippingServiceName() { return shippingServiceName; }
        public double getShippingCost() { return shippingCost; }
        public String getFullShippingAddress() { return fullShippingAddress; }
        public List<OrderItem> getItems() { return items; }
        public String getPaymentMethod() { return paymentMethod; }
        public int getUserId() { return userId; }


        // --- Setter ---
        // Penting: Setter untuk items yang dipanggil setelah objek Order dibuat
        public void setItems(List<OrderItem> items) {
            this.items = items;
        }
        public void setOrderStatus(String orderStatus) { // TAMBAHKAN SETTER INI
            this.orderStatus = orderStatus;
        }
    }

    // Kelas inner untuk objek OrderItem (representasi baris dari tabel 'order_items')
     public static class OrderItem {
        private int id;
        private int productId;
        private String productName;
        private int quantity;
        private double pricePerItem;
        private double originalPricePerItem;
        private byte[] imageData;
        private String brand;
        private String fileExtension;
        private String sellerName;

        public OrderItem(int id, int productId, String productName, int quantity,
                         double pricePerItem, double originalPricePerItem,
                         byte[] imageData, String brand, String fileExtension, String sellerName) {
            this.id = id;
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.pricePerItem = pricePerItem;
            this.originalPricePerItem = originalPricePerItem;
            this.imageData = imageData;
            this.brand = brand;
            this.fileExtension = fileExtension;
            this.sellerName = sellerName;
        }

        // --- Getters ---
        public byte[] getImageData() { return imageData; }
        public String getBrand() { return brand; }
        public String getFileExtension() { return fileExtension; }
        public String getSellerName() { return sellerName; }
        public int getId() { return id; }
        public int getProductId() { return productId; }
        public String getProductName() { return productName; }
        public int getQuantity() { return quantity; }
        public double getPricePerItem() { return pricePerItem; }
        public double getOriginalPricePerItem() { return originalPricePerItem; }
    }
     
    /**
 * Mengambil pesanan yang mengandung produk-produk yang dijual oleh seller tertentu.
 * Ini mencakup pesanan di mana setidaknya satu item pesanan dijual oleh seller ini.
 *
 * @param sellerId ID seller (dari currentUser.getId()).
 * @return Daftar objek Order yang relevan untuk seller ini.
 * @throws SQLException jika terjadi kesalahan akses database.
 */
public static List<Order> getOrdersForSeller(int sellerId) throws SQLException {
    List<Order> sellerOrders = new ArrayList<>();
    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet rs = null;
    try {
        conn = DatabaseConnection.getConnection();
        // Query untuk mendapatkan ID pesanan yang memiliki produk dari seller ini
        String orderIdsSql = "SELECT DISTINCT oi.order_id FROM order_items oi JOIN products p ON oi.product_id = p.product_id WHERE p.seller_id = ?";
        
        try (PreparedStatement orderIdsStmt = conn.prepareStatement(orderIdsSql)) {
            orderIdsStmt.setInt(1, sellerId);
            ResultSet orderIdsRs = orderIdsStmt.executeQuery();
            
            List<Integer> distinctOrderIds = new ArrayList<>();
            while (orderIdsRs.next()) {
                distinctOrderIds.add(orderIdsRs.getInt("order_id"));
            }
            orderIdsRs.close();

            if (distinctOrderIds.isEmpty()) {
                return sellerOrders; // Tidak ada pesanan untuk seller ini
            }

            // Sekarang ambil detail lengkap untuk setiap pesanan tersebut
            // Membuat klausa IN untuk query SQL
            StringBuilder inClause = new StringBuilder();
            for (int i = 0; i < distinctOrderIds.size(); i++) {
                inClause.append("?");
                if (i < distinctOrderIds.size() - 1) {
                    inClause.append(",");
                }
            }

            String sql = "SELECT o.id, o.order_number, o.order_date, o.total_amount, o.order_status, " +
                         "o.delivery_estimate, o.shipping_service_name, o.shipping_cost, o.user_id," +
                         "a.full_address, a.city, a.province, a.postal_code, o.payment_method " +
                         "FROM orders o JOIN addresses a ON o.shipping_address_id = a.id " +
                         "WHERE o.id IN (" + inClause.toString() + ") ORDER BY o.order_date DESC, o.id DESC";
            
            stmt = conn.prepareStatement(sql);
            for (int i = 0; i < distinctOrderIds.size(); i++) {
                stmt.setInt(i + 1, distinctOrderIds.get(i));
            }
            rs = stmt.executeQuery();

            while (rs.next()) {
                Order order = new Order(
                    rs.getInt("id"),
                    rs.getString("order_number"),
                    rs.getDate("order_date").toLocalDate(),
                    rs.getDouble("total_amount"),
                    rs.getString("order_status"),
                    rs.getString("delivery_estimate"),
                    rs.getString("shipping_service_name"),
                    rs.getDouble("shipping_cost"),
                    rs.getString("full_address") + ", " + rs.getString("city") + ", " + rs.getString("province") + ", " + rs.getString("postal_code"),
                    rs.getString("payment_method"),
                    rs.getInt("user_id")
                );
                // Penting: Filter order items hanya untuk produk seller ini
                order.setItems(getSellerSpecificOrderItemsForOrder(order.getId(), sellerId));
                sellerOrders.add(order);
            }
        }
    } finally {
        DatabaseConnection.closeConnection(conn, stmt, rs);
    }
    return sellerOrders;
}

    /**
     * Mengambil item pesanan untuk suatu pesanan tertentu, difilter hanya untuk produk dari seller tertentu.
     *
     * @param orderId ID pesanan.
     * @param sellerId ID seller.
     * @return Daftar objek OrderItem.
     * @throws SQLException jika terjadi kesalahan akses database.
     */
    public static List<OrderItem> getSellerSpecificOrderItemsForOrder(int orderId, int sellerId) throws SQLException {
        List<OrderItem> items = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT oi.id, oi.product_id, oi.product_name, oi.quantity, oi.price_per_item, oi.original_price_per_item, " +
                         "p.brand, " +
                         "pi.image_data, pi.file_extension, " +
                         "u.username AS seller_username " +
                         "FROM order_items oi " +
                         "JOIN products p ON oi.product_id = p.product_id " +
                         "LEFT JOIN product_images pi ON oi.product_id = pi.product_id AND pi.is_main_image = 1 " +
                         "LEFT JOIN users u ON p.seller_id = u.id " +
                         "WHERE oi.order_id = ? AND p.seller_id = ?"; // FILTER BY SELLER_ID HERE
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, orderId);
            stmt.setInt(2, sellerId);
            rs = stmt.executeQuery();

            while (rs.next()) {
                String sellerName = rs.getString("seller_username");
                if (sellerName == null) sellerName = "Penjual";

                items.add(new OrderItem(
                    rs.getInt("id"),
                    rs.getInt("product_id"),
                    rs.getString("product_name"),
                    rs.getInt("quantity"),
                    rs.getDouble("price_per_item"),
                    rs.getDouble("original_price_per_item"),
                    rs.getBytes("image_data"),
                    rs.getString("brand"),
                    rs.getString("file_extension"),
                    sellerName
                ));
            }
        } finally {
            DatabaseConnection.closeConnection(conn, stmt, rs);
        }
        return items;
    }
    
    /**
    * Mengambil detail lengkap sebuah pesanan berdasarkan ID Pesanan.
    * Metode ini tidak memfilter berdasarkan user_id, digunakan untuk melihat detail
    * pesanan tertentu (misalnya oleh seller/admin atau untuk detail umum).
    *
    * @param orderId ID pesanan yang akan diambil.
    * @return Objek Order yang lengkap, atau null jika tidak ditemukan.
    * @throws SQLException jika terjadi kesalahan akses database.
    */
   public static Order getOrderById(int orderId) throws SQLException {
       Connection conn = null;
       PreparedStatement stmt = null;
       ResultSet rs = null;
       Order order = null;
       try {
           conn = DatabaseConnection.getConnection();
           String sql = "SELECT o.id, o.order_number, o.order_date, o.total_amount, o.order_status, " +
                        "o.delivery_estimate, o.shipping_service_name, o.shipping_cost,  o.user_id," +
                        "a.full_address, a.city, a.province, a.postal_code, o.payment_method " +
                        "FROM orders o JOIN addresses a ON o.shipping_address_id = a.id " +
                        "WHERE o.id = ?"; // Hanya filter berdasarkan order ID
           stmt = conn.prepareStatement(sql);
           stmt.setInt(1, orderId);
           rs = stmt.executeQuery();

           if (rs.next()) { // Hanya perlu satu baris
               order = new Order(
                   rs.getInt("id"),
                   rs.getString("order_number"),
                   rs.getDate("order_date").toLocalDate(),
                   rs.getDouble("total_amount"),
                   rs.getString("order_status"),
                   rs.getString("delivery_estimate"),
                   rs.getString("shipping_service_name"),
                   rs.getDouble("shipping_cost"),
                   rs.getString("full_address") + ", " + rs.getString("city") + ", " + rs.getString("province") + ", " + rs.getString("postal_code"),
                   rs.getString("payment_method"),
                   rs.getInt("user_id")
               );
               // Ambil item-item pesanan terkait (OrderItems)
               order.setItems(getOrderItemsForOrder(order.getId())); // Panggil metode yang sudah ada
           }
       } finally {
           DatabaseConnection.closeConnection(conn, stmt, rs);
       }
       return order;
   }
   
    public static List<FavoritesUI.FavoriteItem> getFavoriteItemsForUser(int userId) throws SQLException {
        List<FavoritesUI.FavoriteItem> favoriteProducts = new ArrayList<>();
        // Join favorites table with products and product_images to get full details
        String sql = "SELECT p.product_id, p.name, p.description, p.price, p.original_price, p.stock, p.`condition`, p.min_order, p.`brand`, " +
                     "pi.image_data, pi.file_extension " +
                     "FROM favorites f " +
                     "JOIN products p ON f.product_id = p.product_id " +
                     "LEFT JOIN product_images pi ON p.product_id = pi.product_id AND pi.is_main_image = TRUE " + // Only main image
                     "WHERE f.user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("product_id");
                    String name = rs.getString("name");
                    String description = rs.getString("description");
                    double price = rs.getDouble("price");
                    double originalPrice = rs.getDouble("original_price");
                    int stock = rs.getInt("stock");
                    String condition = rs.getString("condition");
                    String minOrder = rs.getString("min_order");
                    String brand = rs.getString("brand");
                    byte[] imageData = rs.getBytes("image_data");
                    String fileExtension = rs.getString("file_extension");

                    String hexColor = "#FDF8E8"; // Default color

                    FavoritesUI.FavoriteItem product = new FavoritesUI.FavoriteItem(
                        id, name, description, price, originalPrice,
                        stock, condition, minOrder, brand,
                        hexColor, null // imagePaths is null as we directly use imageData
                    );

                    if (imageData != null) {
                        List<byte[]> singleImageList = new ArrayList<>();
                        singleImageList.add(imageData);
                        List<String> singleExtensionList = new ArrayList<>();
                        singleExtensionList.add(fileExtension != null ? fileExtension : "jpg");
                        product.setImageDataLists(singleImageList, singleExtensionList);
                    }
                    favoriteProducts.add(product);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching favorite items for user " + userId + ": " + e.getMessage());
            throw e; // Re-throw to be handled by FavoritesUI
        }
        return favoriteProducts;
    }

    /**
     * Menambahkan produk ke daftar favorit pengguna.
     * Mencegah duplikasi entri favorit.
     * @param userId ID pengguna.
     * @param productId ID produk yang akan ditambahkan ke favorit.
     * @return true jika berhasil ditambahkan, false jika sudah ada atau gagal.
     * @throws SQLException Jika terjadi kesalahan database.
     */
    public static boolean addFavoriteItem(int userId, int productId) throws SQLException {
        // First, check if the item is already a favorite
        String checkSql = "SELECT COUNT(*) FROM favorites WHERE user_id = ? AND product_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setInt(1, userId);
            checkStmt.setInt(2, productId);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    System.out.println("Product " + productId + " is already a favorite for user " + userId + ".");
                    return false; // Already a favorite
                }
            }
        }

        // If not already a favorite, insert it
        String insertSql = "INSERT INTO favorites (user_id, product_id, favorited_at) VALUES (?, ?, NOW())";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, productId);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("Product " + productId + " added to favorites for user " + userId + ".");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error adding product " + productId + " to favorites for user " + userId + ": " + e.getMessage());
            throw e;
        }
        return false;
    }

    /**
     * Menghapus produk dari daftar favorit pengguna.
     * @param userId ID pengguna.
     * @param productId ID produk yang akan dihapus dari favorit.
     * @return true jika berhasil dihapus, false jika tidak ditemukan.
     * @throws SQLException Jika terjadi kesalahan database.
     */
    public static boolean removeFavoriteItem(int userId, int productId) throws SQLException {
        String sql = "DELETE FROM favorites WHERE user_id = ? AND product_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, productId);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("Product " + productId + " removed from favorites for user " + userId + ".");
                return true;
            } else {
                System.out.println("Product " + productId + " not found in favorites for user " + userId + ".");
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Error removing product " + productId + " from favorites for user " + userId + ": " + e.getMessage());
            throw e;
        }
    }

}