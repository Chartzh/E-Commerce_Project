package e.commerce;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.awt.Image;
import java.util.Map;
import java.util.HashMap;
import java.time.LocalDate;
import java.time.LocalDateTime; // Pastikan ini ada
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.sql.Timestamp; // Pastikan ini ada

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
     * @param isFirstFetch boolean yang menunjukkan apakah ini fetch pertama untuk detail produk ini
     * @return List of java.awt.Image untuk semua gambar produk.
     * @throws SQLException jika terjadi kesalahan database saat memuat dari ResultSet.
     */
    private static List<Image> getOrCacheAllImages(int productId, ResultSet rs, boolean isFirstFetch) throws SQLException {
        // Jika sudah ada di cache dan ini bukan fetch pertama kali untuk detail produk, kembalikan dari cache
        if (productImageCache.containsKey(productId) && !isFirstFetch) {
            return productImageCache.get(productId);
        }

        List<Image> images = new ArrayList<>();
        // Untuk tujuan caching: kumpulkan semua gambar yang bisa ditemukan dari DB
        List<byte[]> allImageDataBytes = new ArrayList<>();
        // --- Perbaikan: Pastikan rs.beforeFirst() dilakukan sebelum iterasi
        // Ini memastikan ResultSet bisa diulang atau diposisikan ulang
        if (rs != null) { // Tambahkan null check untuk rs
            try {
                rs.beforeFirst(); 
            } catch (SQLException e) {
                // Handle the exception if the ResultSet is not scrollable (TYPE_FORWARD_ONLY)
                // In this case, we might need to re-execute the query or handle it differently.
                // For now, print a warning and proceed without beforeFirst().
                System.err.println("Warning: ResultSet is not scrollable. Cannot use rs.beforeFirst(). " + e.getMessage());
                // As a fallback, if ResultSet is not scrollable, you might need to re-execute the query
                // or ensure the initial query setup allows for scrolling.
                // For this scenario, assuming getProductById ensures scrollable ResultSet.
            }
        }

        while(rs != null && rs.next()){ // Tambahkan null check untuk rs
            byte[] imageData = rs.getBytes("image_data");
            if(imageData != null){
                allImageDataBytes.add(imageData);
            }
        }
        if (rs != null) { // Tambahkan null check untuk rs
            try {
                rs.first(); // Kembali ke baris pertama setelah mengumpulkan semua byte
            } catch (SQLException e) {
                System.err.println("Warning: ResultSet is not scrollable. Cannot use rs.first(). " + e.getMessage());
            }
        }
        // --- End Perbaikan ---

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
  
    public static class Review {
        private int id; // product_reviews.id
        private int productId; // product_reviews.product_id
        private int userId; // product_reviews.user_id
        private String userName; // users.username (join)
        private int rating; // product_reviews.rating
        private String reviewText; // product_reviews.review_text
        private LocalDateTime createdAt; // product_reviews.created_at
        private byte[] reviewImageData; // product_reviews.review_image (BLOB)
        private Image loadedReviewImage; // Gambar testimoni yang dimuat

        public Review(int id, int productId, int userId, String userName, int rating, String reviewText, LocalDateTime createdAt, byte[] reviewImageData) {
            this.id = id;
            this.productId = productId;
            this.userId = userId;
            this.userName = userName;
            this.rating = rating;
            this.reviewText = reviewText;
            this.createdAt = createdAt;
            this.reviewImageData = reviewImageData;
            this.loadedReviewImage = loadImageFromBytes(reviewImageData); // Memuat gambar saat objek dibuat
        }

        // --- Getters ---
        public int getId() { return id; }
        public int getProductId() { return productId; }
        public int getUserId() { return userId; }
        public String getUserName() { return userName; }
        public int getRating() { return rating; }
        public String getReviewText() { return reviewText; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public Image getLoadedReviewImage() { return loadedReviewImage; }
    }


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
        private Image loadedImage; 
        private boolean isSelected;  // Properti untuk mengelola pilihan di CartUI
        private double weight; //

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
         * @param weight Berat produk dalam kg.
         */
        public CartItem(int id, String name, double price, double originalPrice, int quantity, String imageColor, byte[] imageData, String fileExtension, double weight) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.originalPrice = originalPrice;
            this.quantity = quantity;
            this.imageColor = imageColor;
            this.loadedImage = loadImageFromBytes(imageData);
            this.isSelected = true;
            this.weight = weight;
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
         * @param weight Berat produk dalam kg.
         */
        public CartItem(int id, String name, double price, double originalPrice, int quantity, String imageColor, double weight) {
            this(id, name, price, originalPrice, quantity, imageColor, null, null, weight);
        }

        // --- Getters ---
        public int getId() { return id; }
        public String getName() { return name; }
        public double getPrice() { return price; }
        public double getOriginalPrice() { return originalPrice; }
        public int getQuantity() { return quantity; }
        public String getImageColor() { return imageColor; }
        public Image getLoadedImage() { return loadedImage; }
        public boolean isSelected() { return isSelected; }
        public double getWeight() { return weight; }

        // --- Setters ---
        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public void setSelected(boolean selected) {
            this.isSelected = selected;
        }
        
        public void setLoadedImage(Image loadedImage) { 
            this.loadedImage = loadedImage;
        }
        
        public void setWeight(double weight) {
            this.weight = weight;
        }


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
    
    /**
     * Mengambil daftar pengguna unik yang pernah berkomunikasi dengan pengguna saat ini,
     * beserta pesan terakhir dari setiap percakapan. Diurutkan berdasarkan waktu pesan terakhir.
     * @param currentUserId ID pengguna yang sedang login.
     * @return Daftar objek ChatUser yang unik dan terurut.
     * @throws SQLException jika terjadi kesalahan database.
     */
    public static List<ChatUser> getRecentChatUsersWithLastMessage(int currentUserId) throws SQLException {
        List<ChatUser> users = new ArrayList<>();
        List<Integer> partnerIds = new ArrayList<>();

        // --- LANGKAH 1: Ambil semua ID unik lawan bicara dengan query super sederhana ---
        String getPartnersSql = "SELECT sender_id FROM messages WHERE receiver_id = ? " +
                                "UNION " + // UNION, bukan UNION ALL, untuk menghapus duplikat secara otomatis
                                "SELECT receiver_id FROM messages WHERE sender_id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(getPartnersSql)) {
            
            pstmt.setInt(1, currentUserId);
            pstmt.setInt(2, currentUserId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    partnerIds.add(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching recent chat partner IDs: " + e.getMessage());
            throw e;
        }

        if (partnerIds.isEmpty()) {
            return users; // Tidak ada riwayat chat, kembalikan list kosong.
        }

        // --- LANGKAH 2: Untuk setiap ID, ambil detail dan pesan terakhirnya ---
        String getDetailsSql = "SELECT u.username, m.message_text " +
                               "FROM messages m JOIN users u ON u.id = ? " +
                               "WHERE (m.sender_id = ? AND m.receiver_id = ?) OR (m.sender_id = ? AND m.receiver_id = ?) " +
                               "ORDER BY m.timestamp DESC LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(getDetailsSql)) {
            
            for (Integer partnerId : partnerIds) {
                pstmt.setInt(1, partnerId);
                pstmt.setInt(2, currentUserId);
                pstmt.setInt(3, partnerId);
                pstmt.setInt(4, partnerId);
                pstmt.setInt(5, currentUserId);

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        users.add(new ChatUser(partnerId, rs.getString("username"), rs.getString("message_text")));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching user details and last message: " + e.getMessage());
            throw e;
        }
        
        // Catatan: Pengurutan berdasarkan waktu pesan terakhir bisa ditambahkan di sini jika perlu,
        // tapi untuk sekarang, fokusnya adalah membuat fungsionalitas berjalan tanpa error.
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
        private Image loadedImage; 
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
            this.loadedImage = loadImageFromBytes(imageData); 
            this.brand = brand;
            this.sellerName = sellerName;
        }

        // --- Getters ---
        public Image getLoadedImage() { return loadedImage; } 
        public String getBrand() { return brand; }
        public String getSellerName() { return sellerName; }
        public int getId() { return id; }
        public int getProductId() { return productId; }
        public String getProductName() { return productName; }
        public int getQuantity() { return quantity; }
        public double getPricePerItem() { return pricePerItem; }
        public double getOriginalPricePerItem() { return originalPricePerItem; }
        
        public void setLoadedImage(Image loadedImage) { 
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

        public ChatUser(int id, String username, String lastMessage) {
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
        String sql = "SELECT p.product_id, p.name, p.description, p.price, p.original_price, p.stock, p.`condition`, p.min_order, p.`brand`, p.seller_id, p.weight, " + 
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
                double weight = rs.getDouble("weight"); 
                
                String hexColor = DEFAULT_IMAGE_COLOR_HEX;

                byte[] imageData = rs.getBytes("image_data");
                String fileExtension = rs.getString("file_extension");

                FavoriteItem product = new FavoriteItem(
                    id, name, description, price, originalPrice,
                    stock, condition, minOrder, brand,
                    hexColor, null, sellerId, weight 
                );

                product.setLoadedImages(getOrCacheMainImage(id, imageData)); 
                
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
        String sql = "SELECT p.product_id, p.name, p.description, p.price, p.original_price, p.stock, p.`condition`, p.min_order, p.`brand`, p.seller_id, p.weight, " + 
                     "pi.image_data, pi.file_extension FROM products p " +
                     "LEFT JOIN product_images pi ON p.product_id = pi.product_id " +
                     "WHERE p.product_id = ? ORDER BY pi.order_index ASC"; 

        FavoriteItem product = null;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {

            pstmt.setInt(1, productId);
            try (ResultSet rs = pstmt.executeQuery()) {
                List<Image> loadedImages = new ArrayList<>();
                boolean isFirstFetch = !productImageCache.containsKey(productId) || productImageCache.get(productId).isEmpty();

                loadedImages = getOrCacheAllImages(productId, rs, isFirstFetch); 

                // --- Perbaikan: Pastikan rs.beforeFirst() dilakukan sebelum membuat objek Product
                // Ini penting karena getOrCacheAllImages() sudah memajukan kursor ResultSet.
                // Jika ResultSet tidak scrollable, ini akan error, tetapi kita sudah setting TYPE_SCROLL_INSENSITIVE.
                if (rs != null) {
                    try {
                        rs.beforeFirst(); 
                    } catch (SQLException e) {
                        System.err.println("Warning: ResultSet for getProductById is not scrollable. Cannot use rs.beforeFirst(). " + e.getMessage());
                    }
                }
                // --- End Perbaikan ---
                
                boolean firstRow = true;

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
                        double weight = rs.getDouble("weight"); 

                        String hexColor = DEFAULT_IMAGE_COLOR_HEX;

                        // --- Ini baris yang dimaksud di screenshot ---
                        product = new FavoriteItem(
                            id, name, description, price, originalPrice,
                            stock, condition, minOrder, brand,
                            hexColor, null, sellerId, weight 
                        );
                        // --- Akhir baris yang dimaksud ---
                        product.setLoadedImages(loadedImages);
                        firstRow = false;
                    }
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
        String sql = "SELECT p.product_id, p.name, p.description, p.price, p.original_price, p.stock, p.`condition`, p.min_order, p.`brand`, p.seller_id, p.weight, " + 
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
                    double weight = rs.getDouble("weight"); 

                    String hexColor = DEFAULT_IMAGE_COLOR_HEX;

                    FavoriteItem product = new FavoriteItem(
                        id, name, description, price, originalPrice,
                        stock, condition, minOrder, brand,
                        hexColor, null, foundSellerId, weight 
                    );

                    product.setLoadedImages(getOrCacheMainImage(id, imageData));
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
        String sql = "SELECT p.product_id, p.name, p.description, p.price, p.original_price, p.stock, p.`condition`, p.min_order, p.`brand`, p.seller_id, p.weight, " + 
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
                    double weight = rs.getDouble("weight"); 
                    
                    String hexColor = DEFAULT_IMAGE_COLOR_HEX;

                    FavoriteItem product = new FavoriteItem(
                        id, name, description, price, originalPrice,
                        stock, condition, minOrder, foundBrand,
                        hexColor, null, sellerId, weight 
                    );

                    product.setLoadedImages(getOrCacheMainImage(id, imageData));
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
        String sql = "SELECT p.product_id, p.name, p.description, p.price, p.original_price, p.stock, p.`condition`, p.min_order, p.`brand`, p.seller_id, p.weight, " + 
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
                    double weight = rs.getDouble("weight"); 
                    
                    String hexColor = DEFAULT_IMAGE_COLOR_HEX; 

                    byte[] imageData = rs.getBytes("image_data");
                    String fileExtension = rs.getString("file_extension");

                    FavoriteItem product = new FavoriteItem(
                        id, name, description, price, originalPrice,
                        stock, condition, minOrder, brand,
                        hexColor, null, sellerId, weight 
                    );

                    product.setLoadedImages(getOrCacheMainImage(id, imageData));
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
            // Clear cache untuk produk ini agar gambar baru dimuat
            if (generatedId != -1) {
                productImageCache.remove(productId);
            }
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
     * @param weight Berat produk dalam kg.
     * @return ID yang dihasilkan dari produk baru, atau -1 jika penyisipan gagal.
     * @throws SQLException Jika terjadi kesalahan database.
     */
    public static int saveProduct(String name, String description, double price, double originalPrice, int stock, String condition, String minOrder, String brand, Integer sellerId, double weight) throws SQLException {
        String sql = "INSERT INTO products (name, description, price, original_price, stock, `condition`, min_order, `brand`, seller_id, weight) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"; 
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
            pstmt.setDouble(10, weight); 

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        generatedProductId = rs.getInt(1); 
                    }
                }
            }
            if (generatedProductId != -1) {
                clearImageCache();
            }
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
            productImageCache.remove(productId); // Hapus produk ini dari cache
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
     * @param weight Berat produk dalam kg.
     * @return True jika produk berhasil diperbarui, false jika tidak.
     * @throws SQLException Jika terjadi kesalahan database.
     */
    public static boolean updateProduct(int productId, String name, String description, double price, double originalPrice, int stock, String condition, String minOrder, String brand, double weight) throws SQLException {
        String sql = "UPDATE products SET name = ?, description = ?, price = ?, original_price = ?, stock = ?, `condition` = ?, min_order = ?, `brand` = ?, weight = ? WHERE product_id = ?"; 
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
            pstmt.setDouble(9, weight); 
            pstmt.setInt(10, productId); 
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                productImageCache.remove(productId);
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
            productImageCache.remove(productId); 
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
                if (productId != -1) {
                    productImageCache.remove(productId);
                }
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
            productImageCache.remove(productId); // Hapus produk ini dari cache agar gambar utama baru dimuat
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
        String sql = "SELECT p.product_id, p.name, p.description, p.price, p.original_price, p.stock, p.`condition`, p.min_order, p.`brand`, p.seller_id, p.weight, " + 
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
                    double weight = rs.getDouble("weight"); 
                    byte[] imageData = rs.getBytes("image_data");
                    String fileExtension = rs.getString("file_extension");

                    String hexColor = DEFAULT_IMAGE_COLOR_HEX;

                    FavoriteItem product = new FavoriteItem(
                        id, name, description, price, originalPrice,
                        stock, condition, minOrder, brand,
                        hexColor, null, sellerId, weight 
                    );

                    product.setLoadedImages(getOrCacheMainImage(id, imageData));
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
            return affectedRows > 0;
        }
        catch (SQLException e) {
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
                     "p.name, p.price, p.original_price, p.weight, " + 
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
                    double weight = rs.getDouble("weight"); 
                    byte[] imageData = rs.getBytes("image_data");
                    String fileExtension = rs.getString("file_extension");
                    String imageColor = DEFAULT_IMAGE_COLOR_HEX;

                    CartItem item = new CartItem(
                        productId, name, price, originalPrice, quantity,
                        imageColor, imageData, fileExtension, weight 
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
                    // Query lama ini tidak mengambil 'last_message', jadi kita lewatkan string kosong.
                    users.add(new ChatUser(rs.getInt("id"), rs.getString("username"), "")); 
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
        /**
     * Membuat pesanan baru di database. VERSI LAMA.
     * Anda bisa menghapus ini atau menjaganya jika masih ada bagian kode lain yang menggunakannya.
     */
    public static int createOrder(int userId, List<CartItem> cartItems, double totalAmount,
                                  Address shippingAddress, ShippingService shippingService,
                                  String paymentMethod) throws SQLException {
        // Panggil versi baru dengan parameter kupon null
        return createOrder(userId, cartItems, totalAmount, shippingAddress, shippingService, paymentMethod, null);
    }

    /**
     * Membuat pesanan baru di database dan memindahkan item dari keranjang ke order_items.
     * VERSI BARU dengan penanganan kupon.
     *
     * @param userId            ID pengguna yang membuat pesanan.
     * @param cartItems         Daftar item dalam keranjang pengguna.
     * @param totalAmount       Jumlah total akhir pesanan (sudah termasuk diskon).
     * @param shippingAddress   Alamat pengiriman yang dipilih.
     * @param shippingService   Jasa pengiriman yang dipilih.
     * @param paymentMethod     Metode pembayaran yang dipilih.
     * @param appliedCoupon     Objek CouponResult dari kupon yang diterapkan (bisa null).
     * @return ID pesanan yang baru dibuat, atau -1 jika operasi gagal.
     * @throws SQLException jika terjadi kesalahan akses database.
     */
    public static int createOrder(int userId, List<CartItem> cartItems, double totalAmount,
                                  Address shippingAddress, ShippingService shippingService,
                                  String paymentMethod, CouponResult appliedCoupon) throws SQLException {
        Connection conn = null;
        int orderId = -1;

        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); 

            // Modifikasi SQL untuk menyertakan coupon_id dan discount_applied
            String orderSql = "INSERT INTO orders (user_id, order_number, order_date, total_amount, " + 
                              "shipping_address_id, shipping_service_name, shipping_cost, payment_method, order_status, " + 
                              "delivery_estimate, created_at, coupon_id, discount_applied) " + 
                              "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), ?, ?)"; 
            try (PreparedStatement orderStmt = conn.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS)) {

                String orderNumber = generateUniqueOrderNumber();
                LocalDate orderDate = LocalDate.now();

                orderStmt.setInt(1, userId);
                orderStmt.setString(2, orderNumber);
                orderStmt.setDate(3, java.sql.Date.valueOf(orderDate));
                orderStmt.setDouble(4, totalAmount);
                orderStmt.setInt(5, shippingAddress.getId()); 
                orderStmt.setString(6, shippingService.getProviderName());
                orderStmt.setDouble(7, shippingService.getPrice()); 
                orderStmt.setString(8, paymentMethod);
                orderStmt.setString(9, "Pending Payment"); 
                orderStmt.setString(10, shippingService.getArrivalEstimate());
                
                // Set data kupon
                if (appliedCoupon != null && appliedCoupon.isSuccess()) {
                    orderStmt.setInt(11, appliedCoupon.getCouponId());
                    orderStmt.setDouble(12, appliedCoupon.getDiscountAmount());
                } else {
                    orderStmt.setNull(11, java.sql.Types.INTEGER);
                    orderStmt.setDouble(12, 0.0);
                }
                
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

            // ... (Kode untuk memasukkan order_items tetap sama)
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

            // Jika kupon digunakan, catat penggunaannya
            if (appliedCoupon != null && appliedCoupon.isSuccess()) {
                recordCouponUsage(conn, userId, appliedCoupon.getCouponId());
            }

            // Kosongkan keranjang
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
                        rs.getBytes("image_data"), 
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
    
    /**
     * Memeriksa apakah produk tertentu ada di daftar favorit pengguna.
     * @param userId ID pengguna.
     * @param productId ID produk.
     * @return true jika produk ada di favorit, false jika tidak.
     * @throws SQLException Jika terjadi kesalahan database.
     */
    public static boolean isProductInFavorites(int userId, int productId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM favorites WHERE user_id = ? AND product_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, productId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking if product is in favorites for user " + userId + ", product " + productId + ": " + e.getMessage());
            throw e;
        }
        return false;
    }

    /**
     * Menambahkan review produk baru ke database.
     *
     * @param userId ID pengguna yang memberikan review.
     * @param productId ID produk yang direview.
     * @param rating Rating bintang (1-5).
     * @param reviewText Teks review.
     * @param reviewImageData Data gambar testimoni sebagai byte array (BLOB), bisa null.
     * @throws SQLException Jika terjadi kesalahan database saat menyimpan review.
     */
    public static void addProductReview(int userId, int productId, int rating, String reviewText, byte[] reviewImageData) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DatabaseConnection.getConnection();
            // Sesuaikan nama kolom jika Anda mengubahnya di database (misal: review_image)
            String sql = "INSERT INTO product_reviews (user_id, product_id, rating, review_text, created_at, review_image) VALUES (?, ?, ?, ?, NOW(), ?)";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            stmt.setInt(2, productId);
            stmt.setInt(3, rating);
            stmt.setString(4, reviewText);
            
            if (reviewImageData != null) {
                stmt.setBytes(5, reviewImageData);
            } else {
                stmt.setNull(5, java.sql.Types.BLOB);
            }
            
            stmt.executeUpdate();
            System.out.println("Review added for product ID: " + productId + " by user ID: " + userId + (reviewImageData != null ? " with image." : "."));
        } finally {
            DatabaseConnection.closeConnection(conn, stmt, null);
        }
    }
    
     /**
     * Memeriksa apakah seorang pengguna sudah mereview produk tertentu.
     * @param userId ID pengguna yang diperiksa.
     * @param productId ID produk yang diperiksa.
     * @return true jika pengguna sudah mereview produk, false jika belum.
     * @throws SQLException Jika terjadi kesalahan database.
     */
    public static boolean hasUserReviewedProduct(int userId, int productId) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT COUNT(*) FROM product_reviews WHERE user_id = ? AND product_id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            stmt.setInt(2, productId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
        } finally {
            DatabaseConnection.closeConnection(conn, stmt, rs);
        }
    }
    
     
    
    
    /**
     * Mengambil semua review untuk produk tertentu dari database.
     * Menggabungkan dengan tabel users untuk mendapatkan username pemberi review.
     * @param productId ID produk yang review-nya akan diambil.
     * @return Daftar objek Review untuk produk tersebut, diurutkan dari yang terbaru.
     * @throws SQLException Jika terjadi kesalahan database.
     */
    public static List<Review> getReviewsForProduct(int productId) throws SQLException {
        List<Review> reviews = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT pr.id, pr.product_id, pr.user_id, u.username, pr.rating, pr.review_text, pr.created_at, pr.review_image " +
                         "FROM product_reviews pr " +
                         "JOIN users u ON pr.user_id = u.id " +
                         "WHERE pr.product_id = ? " +
                         "ORDER BY pr.created_at DESC"; // Urutkan dari yang terbaru

            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, productId);
            rs = stmt.executeQuery();

            while (rs.next()) {
                int id = rs.getInt("id");
                int fetchedProductId = rs.getInt("product_id");
                int userId = rs.getInt("user_id");
                String userName = rs.getString("username");
                int rating = rs.getInt("rating");
                String reviewText = rs.getString("review_text");
                LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();
                byte[] reviewImageData = rs.getBytes("review_image");

                reviews.add(new Review(id, fetchedProductId, userId, userName, rating, reviewText, createdAt, reviewImageData));
            }
        } finally {
            DatabaseConnection.closeConnection(conn, stmt, rs);
        }
        return reviews;
    }

    /**
     * Menghitung rata-rata rating dan jumlah rating untuk produk tertentu.
     * @param productId ID produk.
     * @return Array double: [rata-rata rating, total rating count]. Jika tidak ada review, [0.0, 0.0].
     */
    public static double[] getProductAverageRatingAndCount(int productId) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        double averageRating = 0.0;
        int reviewCount = 0;

        try {
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT AVG(rating) AS avg_rating, COUNT(id) AS review_count " +
                         "FROM product_reviews WHERE product_id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, productId);
            rs = stmt.executeQuery();

            if (rs.next()) {
                averageRating = rs.getDouble("avg_rating");
                reviewCount = rs.getInt("review_count");
            }
        } finally {
            DatabaseConnection.closeConnection(conn, stmt, rs);
        }
        return new double[]{averageRating, reviewCount};
    }
    
    // Di dalam kelas ProductRepository.java

    /**
     * Menghitung jumlah review untuk setiap rating bintang (1-5) untuk produk tertentu.
     * @param productId ID produk.
     * @return Map<Integer, Integer> di mana kunci adalah rating (1-5) dan nilai adalah jumlah review.
     * @throws SQLException Jika terjadi kesalahan database.
     */
    public static Map<Integer, Integer> getProductRatingSummary(int productId) throws SQLException {
        Map<Integer, Integer> ratingSummary = new HashMap<>();
        // Inisialisasi semua rating 1-5 dengan 0
        for (int i = 1; i <= 5; i++) {
            ratingSummary.put(i, 0);
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT rating, COUNT(id) AS count FROM product_reviews WHERE product_id = ? GROUP BY rating ORDER BY rating ASC";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, productId);
            rs = stmt.executeQuery();

            while (rs.next()) {
                ratingSummary.put(rs.getInt("rating"), rs.getInt("count"));
            }
        } finally {
            DatabaseConnection.closeConnection(conn, stmt, rs);
        }
        return ratingSummary;
    }
    
    /**
     * Mengambil detail kupon dari database berdasarkan kodenya.
     *
     * @param couponCode Kode kupon yang dicari.
     * @return Objek Coupon jika ditemukan, atau null jika tidak.
     * @throws SQLException jika terjadi kesalahan database.
     */
    public static Coupon getCouponByCode(String couponCode) throws SQLException {
        String sql = "SELECT * FROM coupons WHERE coupon_code = ? AND is_active = 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, couponCode);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Coupon(
                        rs.getInt("id"),
                        rs.getString("coupon_code"),
                        rs.getString("discount_type"),
                        rs.getDouble("discount_value"),
                        rs.getDouble("min_purchase_amount"),
                        rs.getDouble("max_discount_amount"),
                        rs.getInt("usage_limit_per_user"),
                        rs.getInt("total_usage_limit"),
                        rs.getInt("current_usage_count"),
                        rs.getTimestamp("valid_from"),
                        rs.getTimestamp("valid_until"),
                        rs.getBoolean("is_active")
                    );
                }
            }
        }
        return null; // Kupon tidak ditemukan
    }

    /**
     * Mendapatkan berapa kali seorang pengguna telah menggunakan kupon tertentu.
     *
     * @param userId    ID pengguna.
     * @param couponId  ID kupon.
     * @return Jumlah penggunaan, atau 0 jika belum pernah digunakan.
     * @throws SQLException jika terjadi kesalahan database.
     */
    public static int getUserCouponUsageCount(int userId, int couponId) throws SQLException {
        String sql = "SELECT usage_count FROM user_coupon_usage WHERE user_id = ? AND coupon_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.setInt(2, couponId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("usage_count");
                }
            }
        }
        return 0; // Belum pernah pakai
    }

    /**
     * Mencatat penggunaan kupon setelah pesanan berhasil dibuat.
     * Metode ini harus dipanggil di dalam transaksi yang sama dengan createOrder.
     *
     * @param conn      Koneksi database yang aktif (dari transaksi).
     * @param userId    ID pengguna.
     * @param couponId  ID kupon yang digunakan.
     * @throws SQLException jika terjadi kesalahan database.
     */
    public static void recordCouponUsage(Connection conn, int userId, int couponId) throws SQLException {
        // 1. Tingkatkan jumlah penggunaan global
        String updateTotalUsageSql = "UPDATE coupons SET current_usage_count = current_usage_count + 1 WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(updateTotalUsageSql)) {
            pstmt.setInt(1, couponId);
            pstmt.executeUpdate();
        }

        // 2. Tingkatkan jumlah penggunaan per pengguna (atau buat entri baru)
        String updateUserUsageSql = "INSERT INTO user_coupon_usage (user_id, coupon_id, usage_count, last_used_at) " +
                                    "VALUES (?, ?, 1, NOW()) " +
                                    "ON DUPLICATE KEY UPDATE usage_count = usage_count + 1, last_used_at = NOW()";
        try (PreparedStatement pstmt = conn.prepareStatement(updateUserUsageSql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, couponId);
            pstmt.executeUpdate();
        }
    }
    
    /**
     * Mengambil semua kupon dari database.
     * @return Daftar semua objek Coupon.
     * @throws SQLException jika terjadi kesalahan database.
     */
    public static List<Coupon> getAllCoupons() throws SQLException {
        List<Coupon> coupons = new ArrayList<>();
        String sql = "SELECT * FROM coupons ORDER BY id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                coupons.add(mapRowToCoupon(rs));
            }
        }
        return coupons;
    }

    /**
     * Mengambil satu kupon berdasarkan ID-nya.
     * @param couponId ID kupon yang dicari.
     * @return Objek Coupon jika ditemukan, atau null.
     * @throws SQLException jika terjadi kesalahan database.
     */
    public static Coupon getCouponById(int couponId) throws SQLException {
        String sql = "SELECT * FROM coupons WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, couponId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToCoupon(rs);
                }
            }
        }
        return null;
    }

    /**
     * Membuat kupon baru di database.
     * @throws SQLException jika terjadi kesalahan database.
     */
    public static void createCoupon(String code, String type, double value, double minPurchase, double maxDiscount, int limitPerUser, int totalLimit, Timestamp validFrom, Timestamp validUntil, boolean isActive) throws SQLException {
        String sql = "INSERT INTO coupons (coupon_code, discount_type, discount_value, min_purchase_amount, max_discount_amount, usage_limit_per_user, total_usage_limit, valid_from, valid_until, is_active, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, code);
            pstmt.setString(2, type);
            pstmt.setDouble(3, value);
            pstmt.setDouble(4, minPurchase);
            pstmt.setDouble(5, maxDiscount);
            pstmt.setInt(6, limitPerUser);
            pstmt.setInt(7, totalLimit);
            pstmt.setTimestamp(8, validFrom);
            pstmt.setTimestamp(9, validUntil);
            pstmt.setBoolean(10, isActive);
            
            pstmt.executeUpdate();
        }
    }

    /**
     * Memperbarui kupon yang sudah ada di database.
     * @throws SQLException jika terjadi kesalahan database.
     */
    public static void updateCoupon(int id, String code, String type, double value, double minPurchase, double maxDiscount, int limitPerUser, int totalLimit, Timestamp validFrom, Timestamp validUntil, boolean isActive) throws SQLException {
        String sql = "UPDATE coupons SET coupon_code = ?, discount_type = ?, discount_value = ?, min_purchase_amount = ?, max_discount_amount = ?, usage_limit_per_user = ?, total_usage_limit = ?, valid_from = ?, valid_until = ?, is_active = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, code);
            pstmt.setString(2, type);
            pstmt.setDouble(3, value);
            pstmt.setDouble(4, minPurchase);
            pstmt.setDouble(5, maxDiscount);
            pstmt.setInt(6, limitPerUser);
            pstmt.setInt(7, totalLimit);
            pstmt.setTimestamp(8, validFrom);
            pstmt.setTimestamp(9, validUntil);
            pstmt.setBoolean(10, isActive);
            pstmt.setInt(11, id);

            pstmt.executeUpdate();
        }
    }

    /**
     * Helper method untuk memetakan baris ResultSet ke objek Coupon.
     * @param rs ResultSet yang aktif.
     * @return Objek Coupon.
     * @throws SQLException jika terjadi kesalahan database.
     */
    private static Coupon mapRowToCoupon(ResultSet rs) throws SQLException {
        return new Coupon(
            rs.getInt("id"),
            rs.getString("coupon_code"),
            rs.getString("discount_type"),
            rs.getDouble("discount_value"),
            rs.getDouble("min_purchase_amount"),
            rs.getDouble("max_discount_amount"),
            rs.getInt("usage_limit_per_user"),
            rs.getInt("total_usage_limit"),
            rs.getInt("current_usage_count"),
            rs.getTimestamp("valid_from"),
            rs.getTimestamp("valid_until"),
            rs.getBoolean("is_active")
        );
    }

}