package e.commerce;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.awt.Color;
import java.awt.image.BufferedImage; // Import ini
import javax.imageio.ImageIO; // Import ini
import java.io.ByteArrayInputStream; // Import ini
import java.awt.Image; // Import ini
import java.util.Map; // Import ini
import java.util.HashMap; // Import ini
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.sql.Timestamp;

import e.commerce.AddressUI.Address;
import e.commerce.AddressUI.ShippingService;
import e.commerce.FavoritesUI.FavoriteItem;
import java.awt.Graphics2D;
import java.io.IOException;

/**
 * Kelas repositori untuk mengelola interaksi database terkait produk, keranjang, pesanan, dan pesan chat.
 * Menyediakan metode untuk mengambil, menyimpan, memperbarui, dan menghapus data.
 */
public class ProductRepository {

    private static final String DEFAULT_IMAGE_COLOR_HEX = "#FDF8E8";

    // --- START REVISION: Image Caching ---
    // Cache untuk menyimpan gambar produk yang sudah dimuat (key: product_id, value: List of java.awt.Image)
    private static Map<Integer, List<Image>> productImageCache = new HashMap<>();

    /**
     * Membersihkan cache gambar produk.
     * Dapat dipanggil untuk membebaskan memori yang digunakan oleh gambar.
     */
    public static void clearImageCache() {
        productImageCache.clear();
        System.out.println("Product image cache cleared.");
    }

    /**
     * Mengonversi byte array gambar menjadi objek java.awt.Image.
     * @param imageData Array byte BLOB gambar.
     * @return Objek java.awt.Image, atau null jika gagal.
     */
    private static Image loadImageFromBytes(byte[] imageData) {
        if (imageData == null) {
            return null;
        }
        try (ByteArrayInputStream bis = new ByteArrayInputStream(imageData)) {
            return ImageIO.read(bis);
        } catch (IOException e) {
            System.err.println("Error reading image from bytes: " + e.getMessage());
            return null;
        }
    }

    /**
     * Mendapatkan gambar utama produk dari database atau cache.
     * Digunakan untuk list tampilan produk seperti dashboard atau hasil pencarian.
     * @param productId ID produk.
     * @param imageData Byte array gambar utama dari DB.
     * @return java.awt.Image dari gambar utama.
     */
    private static List<Image> getOrCacheMainImage(int productId, byte[] imageData) {
        List<Image> cachedImages = productImageCache.get(productId);
        if (cachedImages != null && !cachedImages.isEmpty()) {
            return cachedImages; // Sudah ada di cache
        }

        List<Image> images = new ArrayList<>();
        if (imageData != null) {
            Image img = loadImageFromBytes(imageData);
            if (img != null) {
                images.add(img);
                productImageCache.put(productId, images); // Cache hanya gambar utama di sini
            }
        }
        return images;
    }

    /**
     * Mendapatkan semua gambar produk dari database atau cache.
     * Digunakan untuk tampilan detail produk di mana semua gambar diperlukan.
     * @param productId ID produk.
     * @param rs ResultSet yang sedang aktif, digunakan untuk memuat gambar jika tidak di cache.
     * @param firstRowData List of byte arrays dari gambar produk
     * @return List of java.awt.Image untuk semua gambar produk.
     * @throws SQLException jika terjadi kesalahan database saat memuat dari ResultSet.
     */
    private static List<Image> getOrCacheAllImages(int productId, ResultSet rs, boolean isFirstFetch) throws SQLException {
        // Jika sudah ada di cache dan ini bukan fetch pertama kali untuk detail produk, kembalikan dari cache
        if (productImageCache.containsKey(productId) && !isFirstFetch) {
            return productImageCache.get(productId);
        }

        List<Image> images = new ArrayList<>();
        // Jika ini adalah fetch pertama atau tidak di cache, muat semua gambar dari ResultSet
        // Asumsi ResultSet akan di-rewind atau dikelola oleh pemanggil untuk mendapatkan semua baris gambar.
        // Jika rs sudah di posisi yang benar dan akan diiterasi untuk semua gambar:
        // (Logika ini akan diintegrasikan langsung ke getProductById)

        // Untuk tujuan caching: kumpulkan semua gambar yang bisa ditemukan dari DB
        List<byte[]> allImageDataBytes = new ArrayList<>();
        rs.beforeFirst(); // Pastikan ResultSet berada di awal
        while(rs.next()){
            byte[] imageData = rs.getBytes("image_data");
            if(imageData != null){
                allImageDataBytes.add(imageData);
            }
        }
        rs.first(); // Kembali ke baris pertama setelah mengumpulkan semua byte

        if(!allImageDataBytes.isEmpty()){
            for (byte[] imageData : allImageDataBytes) {
                Image img = loadImageFromBytes(imageData);
                if (img != null) {
                    images.add(img);
                }
            }
            if(!images.isEmpty()){
                productImageCache.put(productId, images); // Simpan semua gambar ke cache
            }
        } else { // Jika tidak ada gambar dari DB, tambahkan placeholder
            BufferedImage placeholder = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = placeholder.createGraphics();
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.fillRect(0, 0, 100, 100);
            g2d.setColor(Color.DARK_GRAY);
            g2d.drawString("No Image", 15, 55);
            g2d.dispose();
            images.add(placeholder);
            productImageCache.put(productId, images);
        }
        
        return images;
    }
    // --- END REVISION: Image Caching ---


    /**
     * Inner class publik dan statis untuk merepresentasikan item produk di keranjang.
     * Termasuk detail produk, data gambar BLOB, dan status pilihan dalam UI.
     * Ini dapat diakses langsung melalui `ProductRepository.CartItem`.
     */
    public static class CartItem {
        private int id; // ID Produk (sesuai products.product_id)
        private String name;
        private double price;
        private double originalPrice;
        private int quantity;
        private String imageColor; // Warna hex untuk fallback/placeholder
        // private byte[] imageData;    // Data gambar BLOB - DIHAPUS karena akan pakai Image
        // private String fileExtension; // Ekstensi file untuk BLOB - DIHAPUS
        private Image loadedImage; // --- REVISI: Tambah properti ini ---
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
         * @param fileExtension Ekstensi file untuk BLOB (misal: "jpg", "png").
         */
        public CartItem(int id, String name, double price, double originalPrice, int quantity, String imageColor, byte[] imageData, String fileExtension) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.originalPrice = originalPrice;
            this.quantity = quantity;
            this.imageColor = imageColor;
            this.loadedImage = loadImageFromBytes(imageData); // --- REVISI: Langsung load image ---
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
        // public byte[] getImageData() { return imageData; } // DIHAPUS
        // public String getFileExtension() { return fileExtension; } // DIHAPUS
        public Image getLoadedImage() { return loadedImage; } // --- REVISI: Getter baru ---
        public boolean isSelected() { return isSelected; }

        // --- Setters ---
        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public void setSelected(boolean selected) {
            this.isSelected = selected;
        }
        
        public void setLoadedImage(Image loadedImage) { // --- REVISI: Setter baru ---
            this.loadedImage = loadedImage;
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
     * Inner class untuk merepresentasikan pesan chat.
     */
    public static class ChatMessage {
        private int messageId; // messages.message_id
        private int senderId; // messages.sender_id
        private int receiverId; // messages.receiver_id
        private String messageText; // messages.message_text
        private LocalDateTime timestamp; // messages.timestamp
        private boolean isRead; // messages.is_read
        private String senderUsername;
        private String receiverUsername;

        public ChatMessage(int messageId, int senderId, String senderUsername, int receiverId, String receiverUsername, String messageText, LocalDateTime timestamp, boolean isRead) {
            this.messageId = messageId;
            this.senderId = senderId;
            this.senderUsername = senderUsername;
            this.receiverId = receiverId;
            this.receiverUsername = receiverUsername;
            this.messageText = messageText;
            this.timestamp = timestamp;
            this.isRead = isRead;
        }

        // Getters
        public int getMessageId() { return messageId; }
        public int getSenderId() { return senderId; }
        public int getReceiverId() { return receiverId; }
        public String getMessageText() { return messageText; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public boolean isRead() { return isRead; }
        public String getSenderUsername() { return senderUsername; }
        public String getReceiverUsername() { return receiverUsername; }

        // Setter (opsional, jika ingin mengubah status read)
        public void setRead(boolean read) { isRead = read; }
    }
    
    public static List<ChatUser> getRecentChatUsersWithLastMessage(int currentUserId) throws SQLException {
        List<ChatUser> users = new ArrayList<>();
        // Perbaikan: Ubah ORDER BY untuk kompatibilitas MySQL lama
        String sql = "SELECT u.id, u.username, COALESCE(last_msg.message_text, '') as last_message_text, last_msg.timestamp as last_message_timestamp " + // Tambahkan last_message_timestamp
                     "FROM users u " +
                     "JOIN (SELECT DISTINCT sender_id AS user_id FROM messages WHERE receiver_id = ? " +
                     "      UNION ALL " +
                     "      SELECT DISTINCT receiver_id AS user_id FROM messages WHERE sender_id = ?) AS chat_participants " +
                     "ON u.id = chat_participants.user_id AND u.id != ? " +
                     "LEFT JOIN ( " +
                     "    SELECT sender_id, receiver_id, message_text, timestamp, " +
                     "           ROW_NUMBER() OVER(PARTITION BY LEAST(sender_id, receiver_id), GREATEST(sender_id, receiver_id) ORDER BY timestamp DESC) as rn " +
                     "    FROM messages " +
                     ") AS last_msg ON (last_msg.sender_id = u.id AND last_msg.receiver_id = ?) OR (last_msg.receiver_id = u.id AND last_msg.sender_id = ?) " +
                     "WHERE rn = 1 OR last_msg.message_text IS NULL " + // Baris ini harusnya OK, rn=1 memastikan hanya pesan terakhir, NULL untuk kontak baru
                     "ORDER BY (last_message_timestamp IS NULL) ASC, last_message_timestamp DESC"; // <--- PERUBAHAN DI SINI
                     // (last_message_timestamp IS NULL) ASC akan menempatkan NULL di akhir (TRUE=1, FALSE=0)
                     // Lalu diikuti dengan ORDER BY timestamp DESC
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, currentUserId);
            pstmt.setInt(2, currentUserId);
            pstmt.setInt(3, currentUserId);
            pstmt.setInt(4, currentUserId);
            pstmt.setInt(5, currentUserId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String lastMessage = rs.getString("last_message_text");
                    users.add(new ChatUser(rs.getInt("id"), rs.getString("username"), lastMessage));
                }
            }
        }
        return users;
    }

    /**
     * Inner class untuk objek Order (representasi baris dari tabel 'orders').
     * Direvisi agar sesuai dengan skema dan penggunaan di UI.
     */
    public static class Order {
        private int id; // orders.id
        private String orderNumber; // orders.order_number
        private LocalDate orderDate; // orders.order_date
        private double totalAmount; // orders.total_amount
        private String orderStatus; // orders.order_status
        private String deliveryEstimate; // orders.delivery_estimate
        private String shippingServiceName; // orders.shipping_service_name
        private double shippingCost; // orders.shipping_cost
        private String fullShippingAddress; // Detail alamat digabung menjadi satu string
        private String paymentMethod; // orders.payment_method
        private int userId; // orders.user_id
        private List<OrderItem> items; // Items dalam pesanan ini

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
            this.userId = userId;
            this.items = new ArrayList<>(); // Inisialisasi kosong, akan diisi nanti
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
        public void setItems(List<OrderItem> items) {
            this.items = items;
        }
        public void setOrderStatus(String orderStatus) {
            this.orderStatus = orderStatus;
        }
    }

    /**
     * Inner class untuk objek OrderItem (representasi baris dari tabel 'order_items').
     * Direvisi agar sesuai dengan skema dan penggunaan di UI.
     */
    public static class OrderItem {
        private int id; // order_items.id
        private int productId; // order_items.product_id
        private String productName; // order_items.product_name
        private int quantity; // order_items.quantity
        private double pricePerItem; // order_items.price_per_item
        private double originalPricePerItem; // order_items.original_price_per_item
        // private byte[] imageData; // DIHAPUS
        // private String fileExtension; // DIHAPUS
        private Image loadedImage; // --- REVISI: Tambah properti ini ---
        private String brand;
        private String sellerName; // Nama penjual produk ini

        public OrderItem(int id, int productId, String productName, int quantity,
                         double pricePerItem, double originalPricePerItem,
                         byte[] imageData, String brand, String fileExtension, String sellerName) {
            this.id = id;
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.pricePerItem = pricePerItem;
            this.originalPricePerItem = originalPricePerItem;
            this.loadedImage = loadImageFromBytes(imageData); // --- REVISI: Langsung load image ---
            this.brand = brand;
            this.sellerName = sellerName;
        }

        // --- Getters ---
        // public byte[] getImageData() { return imageData; } // DIHAPUS
        public Image getLoadedImage() { return loadedImage; } // --- REVISI: Getter baru ---
        public String getBrand() { return brand; }
        // public String getFileExtension() { return fileExtension; } // DIHAPUS
        public String getSellerName() { return sellerName; }
        public int getId() { return id; }
        public int getProductId() { return productId; }
        public String getProductName() { return productName; }
        public int getQuantity() { return quantity; }
        public double getPricePerItem() { return pricePerItem; }
        public double getOriginalPricePerItem() { return originalPricePerItem; }
        
        public void setLoadedImage(Image loadedImage) { // --- REVISI: Setter baru ---
            this.loadedImage = loadedImage;
        }
    }

    /**
     * Inner class ChatUser
     */
    public static class ChatUser {
        private int id; // users.id
        private String username; // users.username
        private String lastMessage;

        public ChatUser(int id, String username, String lastMessage) { // <--- KONSTRUKTOR BARU (SUDAH ADA di kode Anda)
            this.id = id;
            this.username = username;
            this.lastMessage = lastMessage;
        }

        public int getId() { return id; }
        public String getUsername() { return username; }
        public String getLastMessage() { return lastMessage; }

        @Override
        public String toString() {
            return username; // Digunakan oleh JList
        }
    }


    // --- PRODUCT AND FAVORITES METHODS ---

    /**
     * Mengambil semua produk dari database.
     * Data gambar (BLOB) hanya diambil untuk gambar utama (`is_main_image = TRUE`)
     * untuk efisiensi.
     * @return Sebuah daftar objek FavoriteItem yang merepresentasikan semua produk.
     */
    public static List<FavoriteItem> getAllProducts() {
        String sql = "SELECT p.product_id, p.name, p.description, p.price, p.original_price, p.stock, p.`condition`, p.min_order, p.`brand`, p.seller_id, " +
                     "pi.image_data, pi.file_extension FROM products p " +
                     "LEFT JOIN product_images pi ON p.product_id = pi.product_id AND pi.is_main_image = TRUE"; 

        List<FavoriteItem> products = new ArrayList<>();

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
                int sellerId = rs.getInt("seller_id");
                
                String hexColor = DEFAULT_IMAGE_COLOR_HEX;

                byte[] imageData = rs.getBytes("image_data");
                String fileExtension = rs.getString("file_extension"); // Masih perlu untuk konversi

                FavoriteItem product = new FavoriteItem(
                    id, name, description, price, originalPrice,
                    stock, condition, minOrder, brand,
                    hexColor, null, sellerId // Mengatur loadedImages menjadi null sementara
                );

                // --- START REVISION: Caching main image ---
                product.setLoadedImages(getOrCacheMainImage(id, imageData)); 
                // --- AKHIR REVISI ---
                
                products.add(product);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching all products (with main image): " + e.getMessage());
            e.printStackTrace();
        }
        return products;
    }

    /**
     * Mengambil satu produk berdasarkan ID-nya, termasuk semua BLOB gambar terkait dan seller_id.
     * Ini digunakan untuk menampilkan detail produk di mana semua gambar diperlukan.
     * @param productId ID produk yang akan diambil.
     * @return Objek FavoriteItem dengan detail lengkap, gambar yang dimuat, dan seller_id, atau null jika tidak ditemukan.
     */
    public static FavoriteItem getProductById(int productId) {
        // SQL untuk mengambil detail produk dan SEMUA gambar terkait
        String sql = "SELECT p.product_id, p.name, p.description, p.price, p.original_price, p.stock, p.`condition`, p.min_order, p.`brand`, p.seller_id, " +
                     "pi.image_data, pi.file_extension FROM products p " +
                     "LEFT JOIN product_images pi ON p.product_id = pi.product_id " +
                     "WHERE p.product_id = ? ORDER BY pi.order_index ASC"; 

        FavoriteItem product = null;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {

            pstmt.setInt(1, productId);
            try (ResultSet rs = pstmt.executeQuery()) {
                // --- START REVISION: Caching all images for product detail ---
                List<Image> loadedImages = new ArrayList<>();
                boolean isFirstFetch = !productImageCache.containsKey(productId) || productImageCache.get(productId).isEmpty();

                // Dapatkan semua gambar dari ResultSet dan cache jika ini adalah fetch pertama
                loadedImages = getOrCacheAllImages(productId, rs, isFirstFetch); 

                // Reset ResultSet ke awal untuk proses data produk
                rs.beforeFirst();
                // --- AKHIR REVISI ---

                boolean firstRow = true; // Bendera untuk memastikan objek produk dibuat hanya sekali

                while (rs.next()) {
                    if (firstRow) {
                        int id = rs.getInt("product_id");
                        String name = rs.getString("name");
                        String description = rs.getString("description");
                        double price = rs.getDouble("price");
                        double originalPrice = rs.getDouble("original_price");
                        int stock = rs.getInt("stock");
                        String condition = rs.getString("condition");
                        String minOrder = rs.getString("min_order");
                        String brand = rs.getString("brand");
                        int sellerId = rs.getInt("seller_id");

                        String hexColor = DEFAULT_IMAGE_COLOR_HEX;

                        product = new FavoriteItem(
                            id, name, description, price, originalPrice,
                            stock, condition, minOrder, brand,
                            hexColor, null, sellerId
                        );
                        product.setLoadedImages(loadedImages); // Set gambar yang sudah di-cache/dimuat
                        firstRow = false;
                    }
                    // Jika ada beberapa baris (multiple images), data produk hanya dari baris pertama.
                    // Gambar sudah diambil dan di-cache di getOrCacheAllImages.
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
    public static List<FavoriteItem> getProductsBySeller(int sellerId) {
        String sql = "SELECT p.product_id, p.name, p.description, p.price, p.original_price, p.stock, p.`condition`, p.min_order, p.`brand`, p.seller_id, " +
                     "pi.image_data, pi.file_extension FROM products p " +
                     "LEFT JOIN product_images pi ON p.product_id = pi.product_id AND pi.is_main_image = TRUE " +
                     "WHERE p.seller_id = ?"; 

        List<FavoriteItem> products = new ArrayList<>();
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
                    int foundSellerId = rs.getInt("seller_id");

                    String hexColor = DEFAULT_IMAGE_COLOR_HEX;

                    FavoriteItem product = new FavoriteItem(
                        id, name, description, price, originalPrice,
                        stock, condition, minOrder, brand,
                        hexColor, null, foundSellerId
                    );

                    // --- START REVISION: Caching main image ---
                    product.setLoadedImages(getOrCacheMainImage(id, imageData));
                    // --- AKHIR REVISI ---
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
    public static List<FavoriteItem> getProductsByBrand(String brand, int excludeProductId) {
        String sql = "SELECT p.product_id, p.name, p.description, p.price, p.original_price, p.stock, p.`condition`, p.min_order, p.`brand`, p.seller_id, " +
                     "pi.image_data, pi.file_extension FROM products p " +
                     "LEFT JOIN product_images pi ON p.product_id = pi.product_id AND pi.is_main_image = TRUE " +
                     "WHERE p.`brand` = ? AND p.product_id != ? LIMIT 6"; 

        List<FavoriteItem> recommendedProducts = new ArrayList<>();

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
                    int sellerId = rs.getInt("seller_id");
                    
                    String hexColor = DEFAULT_IMAGE_COLOR_HEX;

                    FavoriteItem product = new FavoriteItem(
                        id, name, description, price, originalPrice,
                        stock, condition, minOrder, foundBrand,
                        hexColor, null, sellerId
                    );

                    // --- START REVISION: Caching main image ---
                    product.setLoadedImages(getOrCacheMainImage(id, imageData));
                    // --- AKHIR REVISI ---
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
     * Mencari produk berdasarkan nama atau deskripsi yang mengandung kata kunci.
     * @param searchText Kata kunci pencarian.
     * @return Daftar FavoriteItem yang cocok dengan kriteria pencarian.
     * @throws SQLException Jika terjadi kesalahan database.
     */
    public static List<FavoriteItem> searchProductsByName(String searchText) throws SQLException {
        List<FavoriteItem> searchResults = new ArrayList<>();
        String sql = "SELECT p.product_id, p.name, p.description, p.price, p.original_price, p.stock, p.`condition`, p.min_order, p.`brand`, p.seller_id, " +
                     "pi.image_data, pi.file_extension FROM products p " +
                     "LEFT JOIN product_images pi ON p.product_id = pi.product_id AND pi.is_main_image = TRUE " +
                     "WHERE p.name LIKE ? OR p.description LIKE ?"; 

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String searchPattern = "%" + searchText + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);

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
                    int sellerId = rs.getInt("seller_id");
                    
                    String hexColor = DEFAULT_IMAGE_COLOR_HEX; 

                    byte[] imageData = rs.getBytes("image_data");
                    String fileExtension = rs.getString("file_extension");

                    FavoriteItem product = new FavoriteItem(
                        id, name, description, price, originalPrice,
                        stock, condition, minOrder, brand,
                        hexColor, null, sellerId
                    );

                    // --- START REVISION: Caching main image ---
                    product.setLoadedImages(getOrCacheMainImage(id, imageData));
                    // --- AKHIR REVISI ---
                    searchResults.add(product);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error searching products by name: " + e.getMessage());
            throw e;
        }
        return searchResults;
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
            // --- START REVISION: Clear cache on image save ---
            // Clear cache untuk produk ini agar gambar baru dimuat
            if (generatedId != -1) {
                productImageCache.remove(productId);
            }
            // --- AKHIR REVISI ---
            System.out.println("Image saved for product ID: " + productId + ", Image ID: " + generatedId);
        } catch (SQLException e) {
            System.err.println("Error saving product image: " + e.getMessage());
            throw e;
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
            // --- START REVISION: Clear cache on product save ---
            // Clear cache jika ada produk baru ditambahkan (memaksa reload pada get all)
            if (generatedProductId != -1) {
                clearImageCache(); // Clear seluruh cache agar data baru di-reflect
            }
            // --- AKHIR REVISI ---
            System.out.println("Product saved: " + name + ", Product ID: " + generatedProductId);
        } catch (SQLException e) {
            System.err.println("Error saving product: " + e.getMessage());
            throw e;
        }
        return generatedProductId;
    }

    /**
     * Menghapus produk dan data terkaitnya dari database.
     * Operasi ini bersifat transaksional (semua atau tidak sama sekali).
     * @param productId ID produk yang akan dihapus.
     * @throws SQLException Jika terjadi kesalahan database selama penghapusan atau manajemen transaksi.
     */
    public static void deleteProduct(int productId) throws SQLException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); 

            // Hapus dari cart_items yang referensi produk ini
            String deleteCartItemsSql = "DELETE FROM cart_items WHERE product_id = ?"; 
            try (PreparedStatement pstmt = conn.prepareStatement(deleteCartItemsSql)) {
                pstmt.setInt(1, productId);
                pstmt.executeUpdate();
            }

            // Hapus dari favorites yang referensi produk ini
            String deleteFavoritesSql = "DELETE FROM favorites WHERE product_id = ?"; 
            try (PreparedStatement pstmt = conn.prepareStatement(deleteFavoritesSql)) {
                pstmt.setInt(1, productId);
                pstmt.executeUpdate();
            }

            // Hapus gambar terkait
            String deleteImagesSql = "DELETE FROM product_images WHERE product_id = ?"; 
            try (PreparedStatement pstmt = conn.prepareStatement(deleteImagesSql)) {
                pstmt.setInt(1, productId);
                pstmt.executeUpdate();
            }

            // Hapus produk itu sendiri
            String deleteProductSql = "DELETE FROM products WHERE product_id = ?"; 
            try (PreparedStatement pstmt = conn.prepareStatement(deleteProductSql)) {
                pstmt.setInt(1, productId);
                pstmt.executeUpdate();
            }

            conn.commit(); 
            // --- START REVISION: Clear cache on product delete ---
            productImageCache.remove(productId); // Hapus produk ini dari cache
            // --- AKHIR REVISI ---
            System.out.println("Product and its related data deleted for product ID: " + productId);
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback(); 
                    System.err.println("Transaction rolled back for product deletion for ID " + productId + ": " + e.getMessage());
                } catch (SQLException rbex) {
                    System.err.println("Error rolling back transaction: " + rbex.getMessage());
                }
            }
            throw e; 
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); 
                    conn.close();
                } catch (SQLException fe) {
                    System.err.println("Error setting auto commit or closing connection: " + fe.getMessage());
                }
            }
        }
    }

    /**
     * Memperbarui detail produk yang ada di tabel 'products'.
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
                // --- START REVISION: Clear cache on product update ---
                productImageCache.remove(productId); // Hapus produk ini dari cache
                // --- AKHIR REVISI ---
                System.out.println("Product updated: " + name + ", Product ID: " + productId);
                return true;
            } else {
                System.out.println("No product found with ID: " + productId + " to update.");
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Error updating product: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Menghapus semua gambar yang terkait dengan produk tertentu dari tabel 'product_images'.
     * @param productId ID produk yang gambarnya akan dihapus.
     * @throws SQLException Jika terjadi kesalahan database.
     */
    public static void deleteProductImagesForProduct(int productId) throws SQLException {
        String sql = "DELETE FROM product_images WHERE product_id = ?"; 
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, productId);
            int affectedRows = pstmt.executeUpdate();
            // --- START REVISION: Clear cache on image delete ---
            productImageCache.remove(productId); 
            // --- AKHIR REVISI ---
            System.out.println(affectedRows + " images deleted for product ID: " + productId);
        } catch (SQLException e) {
            System.err.println("Error deleting product images for product ID " + productId + ": " + e.getMessage());
            throw e;
        }
    }

    /**
     * Menghapus satu gambar dari tabel 'product_images' berdasarkan ID gambarnya.
     * @param imageId ID gambar yang akan dihapus.
     * @throws SQLException Jika terjadi kesalahan database.
     */
    public static void deleteProductImageById(int imageId) throws SQLException {
        // Dapatkan product_id terkait sebelum menghapus gambar
        int productId = -1;
        String getProductIdSql = "SELECT product_id FROM product_images WHERE image_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(getProductIdSql)) {
            pstmt.setInt(1, imageId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    productId = rs.getInt("product_id");
                }
            }
        }

        String sql = "DELETE FROM product_images WHERE image_id = ?"; 
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, imageId);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                // --- START REVISION: Clear cache for affected product ---
                if (productId != -1) {
                    productImageCache.remove(productId);
                }
                // --- AKHIR REVISI ---
                System.out.println("Image ID " + imageId + " deleted successfully.");
            } else {
                System.out.println("Image ID " + imageId + " not found for deletion.");
            }
        } catch (SQLException e) {
            System.err.println("Error deleting product image by ID " + imageId + ": " + e.getMessage());
            throw e;
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
            conn.setAutoCommit(false); 

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
                int affectedRows = pstmtSet.executeUpdate();
                if (affectedRows == 0) {
                     System.err.println("Warning: setMainProductImage did not update any row. Image ID " + imageId + " might not belong to Product ID " + productId);
                }
            }

            conn.commit(); 
            // --- START REVISION: Clear cache on main image change ---
            productImageCache.remove(productId); // Hapus produk ini dari cache agar gambar utama baru dimuat
            // --- AKHIR REVISI ---
            System.out.println("Image ID " + imageId + " set as main for Product ID " + productId);
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback(); 
                    System.err.println("Transaction rolled back for setMainProductImage: " + e.getMessage());
                } catch (SQLException rbex) {
                    System.err.println("Error rolling back transaction: " + rbex.getMessage());
                }
            }
            throw e; 
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); 
                    conn.close();
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
        } catch (SQLException e) {
            System.err.println("Error fetching all product images details for product ID " + productId + ": " + e.getMessage());
            throw e;
        }
        return images;
    }

    /**
     * Mengambil daftar produk yang ditandai sebagai favorit oleh pengguna tertentu.
     * Menggabungkan tabel 'favorites', 'products', dan 'product_images' untuk mendapatkan
     * detail lengkap produk favorit, termasuk gambar utama mereka.
     * @param userId ID pengguna yang favoritnya akan diambil.
     * @return Sebuah daftar objek FavoriteItem yang merepresentasikan produk favorit pengguna.
     * @throws SQLException Jika terjadi kesalahan database.
     */
    public static List<FavoriteItem> getFavoriteItemsForUser(int userId) throws SQLException {
        List<FavoriteItem> favoriteProducts = new ArrayList<>();
        String sql = "SELECT p.product_id, p.name, p.description, p.price, p.original_price, p.stock, p.`condition`, p.min_order, p.`brand`, p.seller_id, " +
                     "pi.image_data, pi.file_extension " +
                     "FROM favorites f " +
                     "JOIN products p ON f.product_id = p.product_id " +
                     "LEFT JOIN product_images pi ON p.product_id = pi.product_id AND pi.is_main_image = TRUE " +
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
                    int sellerId = rs.getInt("seller_id");
                    byte[] imageData = rs.getBytes("image_data");
                    String fileExtension = rs.getString("file_extension");

                    String hexColor = DEFAULT_IMAGE_COLOR_HEX;

                    FavoriteItem product = new FavoriteItem(
                        id, name, description, price, originalPrice,
                        stock, condition, minOrder, brand,
                        hexColor, null, sellerId
                    );

                    // --- START REVISION: Caching main image ---
                    product.setLoadedImages(getOrCacheMainImage(id, imageData));
                    // --- AKHIR REVISI ---
                    favoriteProducts.add(product);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching favorite items for user " + userId + ": " + e.getMessage());
            throw e;
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
        String checkSql = "SELECT COUNT(*) FROM favorites WHERE user_id = ? AND product_id = ?"; 
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setInt(1, userId);
            checkStmt.setInt(2, productId);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    System.out.println("Product " + productId + " is already a favorite for user " + userId + ".");
                    return false;
                }
            }
        }

        String insertSql = "INSERT INTO favorites (user_id, product_id, favorited_at) VALUES (?, ?, NOW())"; 
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, productId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error adding product " + productId + " to favorites for user " + userId + ": " + e.getMessage());
            throw e;
        }
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

    /**
     * Mengambil ID penjual (seller_id) berdasarkan ID produk.
     * @param productId ID produk.
     * @return ID penjual, atau -1 jika tidak ditemukan.
     * @throws SQLException Jika terjadi kesalahan database.
     */
    public static int getSellerIdByProductId(int productId) throws SQLException {
        String sql = "SELECT seller_id FROM products WHERE product_id = ?"; 
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, productId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("seller_id");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching seller ID by product ID: " + e.getMessage());
            throw e;
        }
        return -1; 
    }

    /**
     * Mengambil username pengguna berdasarkan ID-nya.
     * @param userId ID pengguna.
     * @return Username, atau "Unknown User" jika tidak ditemukan.
     * @throws SQLException Jika terjadi kesalahan database.
     */
    public static String getUsernameById(int userId) throws SQLException {
        String sql = "SELECT username FROM users WHERE id = ?"; 
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("username");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching username by ID: " + e.getMessage());
            throw e;
        }
        return "Unknown User";
    }


    // --- CART METHODS ---

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
                    String imageColor = DEFAULT_IMAGE_COLOR_HEX;

                    CartItem item = new CartItem(
                        productId, name, price, originalPrice, quantity,
                        imageColor, imageData, fileExtension // imageData akan diubah jadi Image di konstruktor CartItem
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
        String checkSql = "SELECT quantity FROM cart_items WHERE user_id = ? AND product_id = ?"; 
        String updateSql = "UPDATE cart_items SET quantity = quantity + ? WHERE user_id = ? AND product_id = ?"; 
        String insertSql = "INSERT INTO cart_items (user_id, product_id, quantity, added_at) VALUES (?, ?, ?, NOW())"; 

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {

            checkStmt.setInt(1, userId);
            checkStmt.setInt(2, productId);
            ResultSet rs = checkStmt.executeQuery();

            if (rs.next()) {
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setInt(1, quantity);
                    updateStmt.setInt(2, userId);
                    updateStmt.setInt(3, productId);
                    updateStmt.executeUpdate();
                }
                System.out.println("Updated quantity of product " + productId + " for user " + userId + " in cart.");
            } else {
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
     * @return true jika berhasil dihapus, false jika tidak ditemukan.
     * @throws SQLException Jika terjadi kesalahan database.
     */
    public static boolean removeProductFromCart(int userId, int productId) throws SQLException {
        String sql = "DELETE FROM cart_items WHERE user_id = ? AND product_id = ?"; 
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, productId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
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

    // --- CHAT METHODS ---

    /**
     * Mengambil semua pesan antara dua pengguna tertentu, diurutkan berdasarkan waktu.
     * @param userId1 ID pengguna pertama.
     * @param userId2 ID pengguna kedua.
     * @return Daftar objek ChatMessage.
     * @throws SQLException Jika terjadi kesalahan database.
     */
    public static List<ChatMessage> getChatMessages(int userId1, int userId2) throws SQLException {
        List<ChatMessage> messages = new ArrayList<>();
        String sql = "SELECT m.message_id, m.sender_id, u1.username AS sender_username, " + 
                     "m.receiver_id, u2.username AS receiver_username, m.message_text, m.timestamp, m.is_read " + 
                     "FROM messages m " + 
                     "JOIN users u1 ON m.sender_id = u1.id " + 
                     "JOIN users u2 ON m.receiver_id = u2.id " + 
                     "WHERE (m.sender_id = ? AND m.receiver_id = ?) OR (m.sender_id = ? AND m.receiver_id = ?) " + 
                     "ORDER BY m.timestamp ASC"; 

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId1);
            pstmt.setInt(2, userId2);
            pstmt.setInt(3, userId2);
            pstmt.setInt(4, userId1);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int messageId = rs.getInt("message_id");
                    int senderId = rs.getInt("sender_id");
                    String senderUsername = rs.getString("sender_username");
                    int receiverId = rs.getInt("receiver_id");
                    String receiverUsername = rs.getString("receiver_username");
                    String messageText = rs.getString("message_text");
                    LocalDateTime timestamp = rs.getTimestamp("timestamp").toLocalDateTime();
                    boolean isRead = rs.getBoolean("is_read");

                    messages.add(new ChatMessage(messageId, senderId, senderUsername, receiverId, receiverUsername, messageText, timestamp, isRead));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching chat messages: " + e.getMessage());
            throw e;
        }
        return messages;
    }

    /**
     * Menyimpan pesan chat baru ke database.
     * @param senderId ID pengirim.
     * @param receiverId ID penerima.
     * @param messageText Teks pesan.
     * @return true jika pesan berhasil dikirim, false jika gagal.
     * @throws SQLException Jika terjadi kesalahan database.
     */
    public static boolean sendMessage(int senderId, int receiverId, String messageText) throws SQLException {
        String sql = "INSERT INTO messages (sender_id, receiver_id, message_text, timestamp, is_read) VALUES (?, ?, ?, ?, ?)"; 
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, senderId);
            pstmt.setInt(2, receiverId);
            pstmt.setString(3, messageText);
            pstmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setBoolean(5, false); // Default: belum dibaca
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error sending message: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Memperbarui status 'is_read' pesan menjadi TRUE untuk semua pesan
     * yang diterima oleh `receiverId` dari `senderId` yang belum dibaca.
     * @param receiverId ID penerima pesan (pengguna yang sedang melihat chat).
     * @param senderId ID pengirim pesan.
     * @throws SQLException Jika terjadi kesalahan database.
     */
    public static void markMessagesAsRead(int receiverId, int senderId) throws SQLException {
        String sql = "UPDATE messages SET is_read = TRUE WHERE receiver_id = ? AND sender_id = ? AND is_read = FALSE"; 
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, receiverId);
            pstmt.setInt(2, senderId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error marking messages as read: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Mengambil daftar pengguna yang pernah di-chat oleh `currentUserId` atau pernah chat `currentUserId`.
     * Ini akan menjadi daftar "kontak" di pop-up chat.
     * @param currentUserId ID pengguna yang sedang login.
     * @return Daftar objek ChatUser (ID dan Username dari penjual/pembeli yang terlibat chat).
     * @throws SQLException Jika terjadi kesalahan database.
     */
    public static List<ChatUser> getRecentChatUsers(int currentUserId) throws SQLException {
        List<ChatUser> users = new ArrayList<>();
        String sql = "SELECT DISTINCT u.id, u.username FROM users u " + 
                     "WHERE u.id IN (" + 
                     "    SELECT DISTINCT sender_id FROM messages WHERE receiver_id = ?" + 
                     "    UNION " +
                     "    SELECT DISTINCT receiver_id FROM messages WHERE sender_id = ?" + 
                     ") AND u.id != ?"; 
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, currentUserId);
            pstmt.setInt(2, currentUserId);
            pstmt.setInt(3, currentUserId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String lastMessage = rs.getString("last_message_text"); 
                    users.add(new ChatUser(rs.getInt("id"), rs.getString("username"), lastMessage)); 
               }
}
        } catch (SQLException e) {
            System.err.println("Error fetching recent chat users: " + e.getMessage());
            throw e;
        }
        return users;
    }

    // --- ORDER METHODS ---

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
        int orderId = -1;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); 

            // 1. Insert ke tabel orders
            String orderSql = "INSERT INTO orders (user_id, order_number, order_date, total_amount, " + 
                              "shipping_address_id, shipping_service_name, shipping_cost, payment_method, order_status, delivery_estimate, created_at) " + 
                              "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())"; 
            try (PreparedStatement orderStmt = conn.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS)) {

                String orderNumber = generateUniqueOrderNumber();
                LocalDate orderDate = LocalDate.now();

                orderStmt.setInt(1, userId);
                orderStmt.setString(2, orderNumber);
                orderStmt.setDate(3, java.sql.Date.valueOf(orderDate));
                orderStmt.setDouble(4, totalAmount);
                orderStmt.setInt(5, shippingAddress.getId()); 
                orderStmt.setString(6, shippingService.name);
                orderStmt.setDouble(7, shippingService.price);
                orderStmt.setString(8, paymentMethod);
                orderStmt.setString(9, "Pending Payment"); 
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
            }

            // 2. Insert ke tabel order_items untuk setiap item keranjang
            String itemSql = "INSERT INTO order_items (order_id, product_id, product_name, quantity, price_per_item, original_price_per_item) VALUES (?, ?, ?, ?, ?, ?)"; 
            try (PreparedStatement itemStmt = conn.prepareStatement(itemSql)) {
                for (CartItem item : cartItems) {
                    itemStmt.setInt(1, orderId);
                    itemStmt.setInt(2, item.getId()); 
                    itemStmt.setString(3, item.getName());
                    itemStmt.setInt(4, item.getQuantity());
                    itemStmt.setDouble(5, item.getPrice());
                    itemStmt.setDouble(6, item.getOriginalPrice());
                    itemStmt.addBatch();
                }
                itemStmt.executeBatch();
            }


            // 3. Mengosongkan keranjang pengguna setelah pesanan berhasil dibuat
            clearCart(userId);

            conn.commit(); 
            System.out.println("Pesanan " + orderId + " berhasil dibuat.");
            return orderId;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback(); 
                    System.err.println("Transaksi di-rollback karena error: " + e.getMessage());
                } catch (SQLException ex) {
                    System.err.println("Error saat rollback: " + ex.getMessage());
                }
            }
            throw e; 
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); 
                    conn.close();
                } catch (SQLException fe) {
                    System.err.println("Error setting auto commit or closing connection: " + fe.getMessage());
                }
            }
        }
    }

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
        String sql = "SELECT o.id, o.order_number, o.order_date, o.total_amount, o.order_status, " + 
                     "o.delivery_estimate, o.shipping_service_name, o.shipping_cost, o.user_id," + 
                     "a.full_address, a.city, a.province, a.postal_code, o.payment_method " + 
                     "FROM orders o JOIN addresses a ON o.shipping_address_id = a.id " + 
                     "WHERE o.user_id = ? ORDER BY o.order_date DESC, o.id DESC"; 
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
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
                    // Ambil item pesanan untuk setiap pesanan
                    order.setItems(getOrderItemsForOrder(order.getId()));
                    orders.add(order);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching orders for user " + userId + ": " + e.getMessage());
            throw e;
        }
        return orders;
    }

    /**
     * Mengambil item-item pesanan untuk suatu pesanan.
     *
     * @param orderId ID pesanan.
     * @return Daftar objek OrderItem.
     * @throws SQLException jika terjadi kesalahan akses database.
     */
    public static List<OrderItem> getOrderItemsForOrder(int orderId) throws SQLException {
        List<OrderItem> items = new ArrayList<>();
        String sql = "SELECT oi.id, oi.product_id, oi.product_name, oi.quantity, oi.price_per_item, oi.original_price_per_item, " + 
                     "p.brand, " + 
                     "pi.image_data, pi.file_extension, " + 
                     "u.username AS seller_username " + 
                     "FROM order_items oi " + 
                     "JOIN products p ON oi.product_id = p.product_id " + 
                     "LEFT JOIN product_images pi ON oi.product_id = pi.product_id AND pi.is_main_image = 1 " + 
                     "LEFT JOIN users u ON p.seller_id = u.id " + 
                     "WHERE oi.order_id = ?"; 
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, orderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String sellerName = rs.getString("seller_username");
                    if (sellerName == null) sellerName = "Penjual Tidak Dikenal";

                    items.add(new OrderItem(
                        rs.getInt("id"), 
                        rs.getInt("product_id"),
                        rs.getString("product_name"),
                        rs.getInt("quantity"),
                        rs.getDouble("price_per_item"),
                        rs.getDouble("original_price_per_item"),
                        rs.getBytes("image_data"), // imageData akan diubah jadi Image di konstruktor OrderItem
                        rs.getString("brand"),
                        rs.getString("file_extension"),
                        sellerName
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching order items for order ID " + orderId + ": " + e.getMessage());
            throw e;
        }
        return items;
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
        try {
            conn = DatabaseConnection.getConnection();
            String orderIdsSql = "SELECT DISTINCT oi.order_id FROM order_items oi JOIN products p ON oi.product_id = p.product_id WHERE p.seller_id = ?"; 

            try (PreparedStatement orderIdsStmt = conn.prepareStatement(orderIdsSql)) {
                orderIdsStmt.setInt(1, sellerId);
                try (ResultSet orderIdsRs = orderIdsStmt.executeQuery()) {

                    List<Integer> distinctOrderIds = new ArrayList<>();
                    while (orderIdsRs.next()) {
                        distinctOrderIds.add(orderIdsRs.getInt("order_id"));
                    }

                    if (distinctOrderIds.isEmpty()) {
                        return sellerOrders;
                    }

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

                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        for (int i = 0; i < distinctOrderIds.size(); i++) {
                            stmt.setInt(i + 1, distinctOrderIds.get(i));
                        }
                        try (ResultSet rs = stmt.executeQuery()) {
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
                                order.setItems(getSellerSpecificOrderItemsForOrder(order.getId(), sellerId));
                                sellerOrders.add(order);
                            }
                        }
                    }
                }
            }
        } finally {
            if (conn != null) conn.close();
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
        String sql = "SELECT oi.id, oi.product_id, oi.product_name, oi.quantity, oi.price_per_item, oi.original_price_per_item, " + 
                     "p.brand, " + 
                     "pi.image_data, pi.file_extension, " + 
                     "u.username AS seller_username " + 
                     "FROM order_items oi " + 
                     "JOIN products p ON oi.product_id = p.product_id " + 
                     "LEFT JOIN product_images pi ON oi.product_id = pi.product_id AND pi.is_main_image = 1 " + 
                     "LEFT JOIN users u ON p.seller_id = u.id " + 
                     "WHERE oi.order_id = ? AND p.seller_id = ?"; 
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, orderId);
            pstmt.setInt(2, sellerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String sellerName = rs.getString("seller_username");
                    if (sellerName == null) sellerName = "Penjual Tidak Dikenal";

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
            }
        } catch (SQLException e) {
            System.err.println("Error fetching seller-specific order items for order ID " + orderId + " and seller ID " + sellerId + ": " + e.getMessage());
            throw e;
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
       String sql = "SELECT o.id, o.order_number, o.order_date, o.total_amount, o.order_status, " + 
                    "o.delivery_estimate, o.shipping_service_name, o.shipping_cost,  o.user_id," + 
                    "a.full_address, a.city, a.province, a.postal_code, o.payment_method " + 
                    "FROM orders o JOIN addresses a ON o.shipping_address_id = a.id " + 
                    "WHERE o.id = ?"; 
       try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
           pstmt.setInt(1, orderId);
           try (ResultSet rs = pstmt.executeQuery()) {
               if (rs.next()) {
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
                   return order;
               }
           }
       } catch (SQLException e) {
           System.err.println("Error fetching order by ID " + orderId + ": " + e.getMessage());
           throw e;
       }
       return null;
   }

   /**
     * Memperbarui status pesanan di database.
     *
     * @param orderId   ID pesanan yang akan diperbarui.
     * @param newStatus Status baru (misalnya "Cancelled", "Delivered").
     * @throws SQLException jika terjadi kesalahan akses database.
     */
    public static void updateOrderStatus(int orderId, String newStatus) throws SQLException {
        String sql = "UPDATE orders SET order_status = ? WHERE id = ?"; 
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newStatus);
            pstmt.setInt(2, orderId);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                 System.out.println("Order status for ID " + orderId + " updated to: " + newStatus);
            } else {
                 System.out.println("No order found with ID " + orderId + " to update status.");
            }
        } catch (SQLException e) {
            System.err.println("Error updating order status for ID " + orderId + ": " + e.getMessage());
            throw e;
        }
    }
    
    public static int getTotalProductCount() {
        int count = 0;
        String query = "SELECT COUNT(*) FROM products";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                count = rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching total product count: " + e.getMessage());
            e.printStackTrace();
        }
        return count;
    }

    public static int getTotalOrderCount() {
        int count = 0;
        String query = "SELECT COUNT(*) FROM orders";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                count = rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching total order count: " + e.getMessage());
            e.printStackTrace();
        }
        return count;
    }

    public static boolean updateUserVerificationStatus(int userId, boolean isVerified) throws SQLException {
        String sql = "UPDATE users SET is_verified = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setBoolean(1, isVerified);
            pstmt.setInt(2, userId);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error updating user verification status for ID " + userId + ": " + e.getMessage());
            throw e;
        }
    }
}