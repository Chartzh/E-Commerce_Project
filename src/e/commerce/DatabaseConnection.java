import java.sql.*;

/**
 * Database connection utility class for the E-Commerce application.
 * Manages connections to the MySQL database using JDBC.
 */
public class DatabaseConnection {
    // Database connection parameters
    private static final String DB_URL = "jdbc:mysql://localhost:3306/ecommerce_database";
    private static final String USER = "root";
    private static final String PASSWORD = "rajif1234"; // Change this to your actual MySQL password
    
    /**
     * Get a connection to the database
     * @return Connection object
     * @throws SQLException if connection fails
     */
    public static Connection getConnection() throws SQLException {
        try {
            // Load the JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // Create and return the connection
            return DriverManager.getConnection(DB_URL, USER, PASSWORD);
            
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found", e);
        } catch (SQLException e) {
            throw new SQLException("Failed to connect to database", e);
        }
    }
    
    /**
     * Close the database connection and statement
     * @param conn Connection to close
     * @param stmt Statement to close
     */
    public static void closeConnection(Connection conn, Statement stmt) {
        closeConnection(conn, stmt, null);
    }
    
    /**
     * Close the database connection, statement and result set
     * @param conn Connection to close
     * @param stmt Statement to close
     * @param rs ResultSet to close
     */
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
            }
        } catch (SQLException e) {
            System.err.println("Error closing Connection: " + e.getMessage());
        }
    }
    
    /**
     * Initialize the database schema if it doesn't exist
     * This method creates all necessary tables for the application
     */
    public static void initializeDatabase() {
        Connection conn = null;
        Statement stmt = null;
        
        try {
            conn = getConnection();
            stmt = conn.createStatement();
            
            // Create users table if it doesn't exist
            String createUsersTable = "CREATE TABLE IF NOT EXISTS users (" +
                                      "user_id INT AUTO_INCREMENT PRIMARY KEY, " +
                                      "username VARCHAR(50) NOT NULL UNIQUE, " +
                                      "email VARCHAR(100) NOT NULL, " +
                                      "password VARCHAR(255) NOT NULL, " +
                                      "nik VARCHAR(20), " +
                                      "phone VARCHAR(20), " +
                                      "address TEXT, " +
                                      "profile_picture MEDIUMBLOB, " +
                                      "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
            stmt.executeUpdate(createUsersTable);
            
            // Create products table if it doesn't exist
            String createProductsTable = "CREATE TABLE IF NOT EXISTS products (" +
                                         "product_id INT AUTO_INCREMENT PRIMARY KEY, " +
                                         "name VARCHAR(100) NOT NULL, " +
                                         "description TEXT, " +
                                         "price DECIMAL(10,2) NOT NULL, " +
                                         "stock INT NOT NULL DEFAULT 0, " +
                                         "image MEDIUMBLOB, " +
                                         "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
            stmt.executeUpdate(createProductsTable);
            
            // Create orders table if it doesn't exist
            String createOrdersTable = "CREATE TABLE IF NOT EXISTS orders (" +
                                       "order_id INT AUTO_INCREMENT PRIMARY KEY, " +
                                       "user_id INT NOT NULL, " +
                                       "total_amount DECIMAL(10,2) NOT NULL, " +
                                       "status VARCHAR(20) NOT NULL DEFAULT 'pending', " +
                                       "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                                       "FOREIGN KEY (user_id) REFERENCES users(user_id))";
            stmt.executeUpdate(createOrdersTable);
            
            // Create order_items table if it doesn't exist
            String createOrderItemsTable = "CREATE TABLE IF NOT EXISTS order_items (" +
                                           "item_id INT AUTO_INCREMENT PRIMARY KEY, " +
                                           "order_id INT NOT NULL, " +
                                           "product_id INT NOT NULL, " +
                                           "quantity INT NOT NULL, " +
                                           "price DECIMAL(10,2) NOT NULL, " +
                                           "FOREIGN KEY (order_id) REFERENCES orders(order_id), " +
                                           "FOREIGN KEY (product_id) REFERENCES products(product_id))";
            stmt.executeUpdate(createOrderItemsTable);
            
            System.out.println("Database schema initialized successfully");
            
        } catch (SQLException e) {
            System.err.println("Error initializing database schema: " + e.getMessage());
            e.printStackTrace();
        } finally {
            closeConnection(conn, stmt);
        }
    }
    
    /**
     * Test the database connection
     * @return true if connection is successful, false otherwise
     */
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