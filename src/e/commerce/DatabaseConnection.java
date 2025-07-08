package e.commerce;

import java.sql.*;

public class DatabaseConnection {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/ecommerce_database?useSSL=false&serverTimezone=Asia/Jakarta&allowMultiQueries=true";
    private static final String USER = "root";
    private static final String PASSWORD = "rajif1234";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("Attempting to connect to: " + DB_URL);
            Connection conn = DriverManager.getConnection(DB_URL, USER, PASSWORD);
            System.out.println("Database connection successful!");
            return conn;
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found. Make sure mysql-connector-j.jar is in your classpath.");
            throw new SQLException("MySQL JDBC Driver not found", e);
        } catch (SQLException e) {
            System.err.println("Failed to connect to database: " + e.getMessage());
            throw new SQLException("Failed to connect to database: " + e.getMessage(), e);
        }
    }

    public static void closeConnection(Connection conn, Statement stmt) {
        closeConnection(conn, stmt, null);
    }

    public static void closeConnection(Connection conn, Statement stmt, ResultSet rs) {
        try {
            if (rs != null) {
                rs.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing ResultSet: " + e.getMessage());
        }

        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing Statement: " + e.getMessage());
        }

        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            System.err.println("Error closing Connection: " + e.getMessage());
        }
    }

    public static void initializeDatabase() {
        Connection conn = null;
        Statement stmt = null;

        try {
            conn = getConnection();
            stmt = conn.createStatement();

            String createUsersTable = "CREATE TABLE IF NOT EXISTS users (" +
                                      "id INT AUTO_INCREMENT PRIMARY KEY, " +
                                      "username VARCHAR(50) NOT NULL UNIQUE, " +
                                      "password VARCHAR(60) NOT NULL, " +
                                      "email VARCHAR(100) NOT NULL UNIQUE, " +
                                      "role VARCHAR(20) DEFAULT 'user', " +
                                      "is_verified TINYINT(1) DEFAULT 0, " +
                                      "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
            stmt.executeUpdate(createUsersTable);

            String createVerificationsTable = "CREATE TABLE IF NOT EXISTS verifications (" +
                                              "email VARCHAR(100) PRIMARY KEY, " +
                                              "otp_code VARCHAR(10) NOT NULL, " +
                                              "otp_expires_at TIMESTAMP NOT NULL, " +
                                              "FOREIGN KEY (email) REFERENCES users(email) ON DELETE CASCADE ON UPDATE CASCADE)";
            stmt.executeUpdate(createVerificationsTable);

            String createProfileTable = "CREATE TABLE IF NOT EXISTS profile (" +
                                        "user_id INT NOT NULL PRIMARY KEY, " +
                                        "nik VARCHAR(20) NULL, " +
                                        "address VARCHAR(255) NULL, " +
                                        "phone VARCHAR(20) NULL, " +
                                        "city VARCHAR(100) NULL, " +
                                        "province VARCHAR(100) NULL, " +
                                        "postal_code VARCHAR(10) NULL, " +
                                        "country VARCHAR(100) NULL, " +
                                        "profile_picture MEDIUMBLOB NULL, " +
                                        "banner_picture MEDIUMBLOB NULL, " +
                                        "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE ON UPDATE NO ACTION)";
            stmt.executeUpdate(createProfileTable);

            String createProductsTable = "CREATE TABLE IF NOT EXISTS products (" +
                                         "product_id INT AUTO_INCREMENT PRIMARY KEY, " +
                                         "name VARCHAR(255) NOT NULL, " +
                                         "description TEXT, " +
                                         "price DECIMAL(10,2) NOT NULL, " +
                                         "original_price DECIMAL(10,2) NULL, " +
                                         "stock INT NOT NULL DEFAULT 0, " +
                                         "`condition` VARCHAR(50) NULL, " +
                                         "`min_order` VARCHAR(50) NULL, " +
                                         "`brand` VARCHAR(100) NULL, " +
                                         "seller_id INT NULL, " +
                                         "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                                         "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                                         "FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE SET NULL ON UPDATE NO ACTION)";
            stmt.executeUpdate(createProductsTable);

            String createProductImagesTable = "CREATE TABLE IF NOT EXISTS product_images (" +
                                              "image_id INT NOT NULL AUTO_INCREMENT, " +
                                              "product_id INT NOT NULL, " +
                                              "image_data MEDIUMBLOB NOT NULL, " +
                                              "file_extension VARCHAR(10) NOT NULL, " +
                                              "is_main_image BOOLEAN DEFAULT FALSE, " +
                                              "order_index INT DEFAULT 0, " +
                                              "PRIMARY KEY (image_id), " +
                                              "FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE ON UPDATE NO ACTION)";
            stmt.executeUpdate(createProductImagesTable);

            // [START NEW TABLE: product_reviews]
            String createProductReviewsTable = "CREATE TABLE IF NOT EXISTS product_reviews (" +
                                               "id INT AUTO_INCREMENT PRIMARY KEY, " +
                                               "product_id INT NOT NULL, " +
                                               "user_id INT NOT NULL, " +
                                               "rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5), " +
                                               "review_text TEXT, " +
                                               "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                                               "FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE, " +
                                               "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)";
            stmt.executeUpdate(createProductReviewsTable);
            // [END NEW TABLE: product_reviews]

            String createOrdersTable = "CREATE TABLE IF NOT EXISTS orders (" +
                                       "order_id INT AUTO_INCREMENT PRIMARY KEY, " +
                                       "user_id INT NOT NULL, " +
                                       "total_amount DECIMAL(10,2) NOT NULL, " +
                                       "status VARCHAR(20) NOT NULL DEFAULT 'pending', " +
                                       "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                                       "FOREIGN KEY (user_id) REFERENCES users(id))";
            stmt.executeUpdate(createOrdersTable);

            String createOrderItemsTable = "CREATE TABLE IF NOT EXISTS order_items (" +
                                           "item_id INT AUTO_INCREMENT PRIMARY KEY, " +
                                           "order_id INT NOT NULL, " +
                                           "product_id INT NOT NULL, " +
                                           "quantity INT NOT NULL, " +
                                           "price DECIMAL(10,2) NOT NULL, " +
                                           "FOREIGN KEY (order_id) REFERENCES orders(order_id) ON DELETE CASCADE, " +
                                           "FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE RESTRICT)";
            stmt.executeUpdate(createOrderItemsTable);

            System.out.println("Database schema initialized successfully (from DatabaseConnection.java)");

            if (countTableRows(conn, "users") == 0) {
                boolean adminRegistered = Authentication.register("admin_user", "admin@example.com", "adminpass", "admin", true);
                boolean supervisorRegistered = Authentication.register("seller_user", "seller@example.com", "sellerpass", "seller", true);
                boolean opManagerRegistered = Authentication.register("op_manager_user", "manager@example.com", "managerpass", "op_manager", true);
                boolean userRegistered = Authentication.register("user_customer", "customer@example.com", "customerpass", "user", true);

                if (adminRegistered && supervisorRegistered && opManagerRegistered && userRegistered) {
                     System.out.println("Dummy users inserted via Authentication.register.");
                } else {
                     System.err.println("Failed to insert dummy users via Authentication.register. They might already exist.");
                }
            }

            if (countTableRows(conn, "products") == 0) {
                User supervisorUser = Authentication.getUserByEmail("supervisor@example.com");
                Integer supervisorId = null;
                if (supervisorUser != null) {
                    supervisorId = supervisorUser.getId();
                }

                if (supervisorId != null) {
                    stmt.executeUpdate("INSERT INTO products (name, description, price, original_price, stock, `condition`, min_order, `brand`, seller_id) VALUES " +
                        "('HP Pavilion 14 EK2222TU Touch 2in1', 'Laptop HP Pavilion terbaru dengan performa tinggi untuk pekerjaan dan hiburan, dilengkapi layar sentuh dan fungsi 2-in-1 yang fleksibel.', 13252000.00, 15500000.00, 5, 'Baru', '1 Buah', 'HEWLETT PACKARD', " + supervisorId + ")," +
                        "('Wireless Headphone', 'Headphone nirkabel dengan kualitas suara premium dan nyaman dipakai sepanjang hari. Ideal untuk musik dan panggilan.', 249000.00, 299000.00, 10, 'Baru', '1 Hari', 'Audio Brand', " + supervisorId + ")," +
                        "('Bluetooth Speaker', 'Speaker Bluetooth portabel dengan bass yang kuat dan desain ringkas. Cocok untuk dibawa bepergian.', 189000.00, 219000.00, 15, 'Baru', '1 Hari', 'Audio Brand', " + supervisorId + ")," +
                        "('Gaming Mouse', 'Mouse gaming presisi tinggi dengan pencahayaan RGB yang dapat disesuaikan. Tingkatkan pengalaman bermain game Anda.', 329000.00, 399000.00, 8, 'Baru', '1 Hari', 'Gaming Gear', " + supervisorId + ")," +
                        "('Mechanical Keyboard', 'Keyboard mekanik dengan switch taktil dan konstruksi tahan lama. Respon cepat untuk mengetik dan gaming.', 450000.00, 499000.00, 5, 'Baru', '1 Hari', 'Gaming Gear', " + supervisorId + ")," +
                        "('Smartwatch X200', 'Smartwatch multifungsi dengan monitor detak jantung, notifikasi cerdas, dan daya tahan baterai lama.', 1800000.00, 2100000.00, 3, 'Baru', '1 Hari', 'Wearables Inc.', " + supervisorId + ")," +
                        "('Portable SSD 1TB', 'SSD portabel 1TB dengan transfer data super cepat. Desain ringkas, ideal untuk penyimpanan eksternal.', 950000.00, 1200000.00, 12, 'Baru', '1 Hari', 'Storage Solutions', " + supervisorId + ")," +
                        "('USB-C Hub Multiport', 'Hub USB-C multifungsi untuk memperluas konektivitas laptop Anda. Dilengkapi port HDMI, USB, dan card reader.', 300000.00, 350000.00, 20, 'Baru', '1 Hari', 'Connectivity Co.', " + supervisorId + ")," +
                        "('Webcam Full HD', 'Webcam Full HD untuk panggilan video jernih dan streaming. Dilengkapi mikrofon internal.', 400000.00, 480000.00, 7, 'Baru', '1 Hari', 'Video Gear', " + supervisorId + ")," +
                        "('Phone Case Premium', 'Casing ponsel premium dengan perlindungan stylish. Desain ergonomis dan bahan berkualitas tinggi.', 150000.00, 200000.00, 50, 'Baru', '1 Hari', 'Case Master', " + supervisorId + ")," +
                        "('Laptop Stand', 'Stand laptop ergonomis untuk kenyamanan maksimal. Meningkatkan aliran udara dan mengurangi ketegangan leher.', 280000.00, 320000.00, 8, 'Baru', '1 Hari', 'ErgoTech', " + supervisorId + ")," +
                        "('Power Bank 20000mAh', 'Power bank 200000mAh untuk mengisi daya perangkat Anda saat bepergian. Desain ringkas, daya besar.', 320000.00, 380000.00, 12, 'Baru', '1 Hari', 'PowerUp', " + supervisorId + ")," +
                        "('Wireless Charger', 'Charger nirkabel cepat dan nyaman. Cukup letakkan ponsel Anda untuk mulai mengisi daya.', 180000.00, 220000.00, 15, 'Baru', '1 Hari', 'ChargeFast', " + supervisorId + ")");
                    System.out.println("Dummy products inserted.");
                } else {
                    System.err.println("Supervisor user not found for inserting dummy products. Please ensure 'supervisor_user' is in the users table.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error initializing database schema: " + e.getSQLState() + " - " + e.getErrorCode() + " - " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeConnection(conn, stmt);
        }
    }

    private static int countTableRows(Connection conn, String tableName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM `" + tableName + "`";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    public static boolean testConnection() {
        Connection conn = null;
        try {
            conn = getConnection();
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("Database connection test failed: " + e.getMessage());
            return false;
        } finally {
            if (conn != null) {
                closeConnection(conn, null);
            }
        }
    }
}