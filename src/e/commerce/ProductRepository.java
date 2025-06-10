package e.commerce;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.awt.Color;
import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

public class ProductRepository {

    /**
     * Fetches all products from the database.
     * Images (BLOB data) are not fetched here for efficiency; they are fetched
     * on demand by getProductById or getProductsBySeller/Brand for main image.
     * @return A list of FavoriteItem objects representing all products.
     */
    public static List<FavoritesUI.FavoriteItem> getAllProducts() {
        // SQL query to select all product details from the 'products' table.
        // Backticks are used for 'condition' and 'brand' as they are reserved keywords.
        String sql = "SELECT product_id, name, description, price, original_price, stock, `condition`, min_order, `brand` FROM products";

        List<FavoritesUI.FavoriteItem> products = new ArrayList<>(); 
        
        try (Connection conn = DatabaseConnection.getConnection(); // Use DatabaseConnection
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
                
                String hexColor = "#FDF8E8"; // Dummy background color for product card
                
                // Construct FavoriteItem. rawImageDataList and rawFileExtensionList are null here.
                // Images will be loaded when getProductById is called.
                products.add(new FavoritesUI.FavoriteItem(
                    id, name, description, price, originalPrice,
                    stock, condition, minOrder, brand,
                    hexColor, null 
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching all products: " + e.getMessage());
            e.printStackTrace();
        }
        return products; 
    }

    /**
     * Fetches a single product by its ID, including all associated image BLOBs.
     * This is used for displaying product details where all images are needed.
     * @param productId The ID of the product to fetch.
     * @return A FavoriteItem object with complete details and loaded images, or null if not found.
     */
    public static FavoritesUI.FavoriteItem getProductById(int productId) {
        // SQL query to join 'products' and 'product_images' to get product details and all image BLOBs.
        String sql = "SELECT p.product_id, p.name, p.description, p.price, p.original_price, p.stock, p.`condition`, p.min_order, p.`brand`, " +
                     "pi.image_data, pi.file_extension FROM products p LEFT JOIN product_images pi ON p.product_id = pi.product_id WHERE p.product_id = ?";

        FavoritesUI.FavoriteItem product = null; 

        try (Connection conn = DatabaseConnection.getConnection(); // Use DatabaseConnection
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, productId);
            try (ResultSet rs = pstmt.executeQuery()) {
                List<byte[]> imageDataList = new ArrayList<>();
                List<String> fileExtensionList = new ArrayList<>();
                
                // Process the first row to get product details
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
                    String hexColor = "#FDF8E8";
                    
                    product = new FavoritesUI.FavoriteItem(
                        id, name, description, price, originalPrice,
                        stock, condition, minOrder, brand,
                        hexColor, null 
                    );

                    // Add the first image found
                    byte[] imageData = rs.getBytes("image_data");
                    String fileExtension = rs.getString("file_extension");
                    if (imageData != null) {
                        imageDataList.add(imageData);
                        fileExtensionList.add(fileExtension);
                    }
                    
                    // Add any remaining images for the same product
                    while (rs.next()) {
                        imageData = rs.getBytes("image_data");
                        fileExtension = rs.getString("file_extension");
                        if (imageData != null) {
                            imageDataList.add(imageData);
                            fileExtensionList.add(fileExtension);
                        }
                    }
                    product.setImageDataLists(imageDataList, fileExtensionList); // Set all collected image data
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
     * Fetches products uploaded by a specific seller (supervisor/admin).
     * Only fetches the main image (is_main_image = TRUE) for each product for efficiency.
     * @param sellerId The ID of the seller (user).
     * @return A list of FavoriteItem objects representing the seller's products.
     */
    public static List<FavoritesUI.FavoriteItem> getProductsBySeller(int sellerId) {
        String sql = "SELECT p.product_id, p.name, p.description, p.price, p.original_price, p.stock, p.`condition`, p.min_order, p.`brand`, " +
                     "pi.image_data, pi.file_extension FROM products p " +
                     "LEFT JOIN product_images pi ON p.product_id = pi.product_id AND pi.is_main_image = TRUE " + // Only fetch main image
                     "WHERE p.seller_id = ?";

        List<FavoritesUI.FavoriteItem> products = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection(); // Use DatabaseConnection
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
     * Fetches recommended products based on brand, excluding a specific product.
     * Only fetches the main image for each recommended product for efficiency.
     * @param brand The brand to match.
     * @param excludeProductId The ID of the product to exclude from recommendations.
     * @return A list of recommended FavoriteItem objects.
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
     * Saves a product image BLOB to the 'product_images' table.
     * This method is called from the UI when a seller uploads images.
     * @param productId The ID of the product this image belongs to.
     * @param imageData The byte array of the image.
     * @param fileExtension The file extension (e.g., "jpg", "png").
     * @param isMainImage True if this is the main image for the product.
     * @param orderIndex The display order of the image.
     * @return The generated ID of the image, or -1 if insertion fails.
     * @throws SQLException If a database error occurs.
     */
    public static int saveProductImage(int productId, byte[] imageData, String fileExtension, boolean isMainImage, int orderIndex) throws SQLException {
        String sql = "INSERT INTO product_images (product_id, image_data, file_extension, is_main_image, order_index) VALUES (?, ?, ?, ?, ?)";
        int generatedId = -1;
        try (Connection conn = DatabaseConnection.getConnection(); // Use DatabaseConnection
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
     * Saves a new product's details to the 'products' table.
     * This method is called from the UI when a seller adds a new product.
     * @param name Product name.
     * @param description Product description.
     * @param price Product price.
     * @param originalPrice Original price (can be 0.0 if not applicable).
     * @param stock Product stock quantity.
     * @param condition Product condition (e.g., "Baru", "Bekas").
     * @param minOrder Minimum order quantity (e.g., "1 Buah").
     * @param brand Product brand.
     * @param sellerId The ID of the user who is selling this product.
     * @return The generated ID of the new product, or -1 if insertion fails.
     * @throws SQLException If a database error occurs.
     */
    public static int saveProduct(String name, String description, double price, double originalPrice, int stock, String condition, String minOrder, String brand, Integer sellerId) throws SQLException {
        String sql = "INSERT INTO products (name, description, price, original_price, stock, `condition`, min_order, `brand`, seller_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        int generatedProductId = -1;
        try (Connection conn = DatabaseConnection.getConnection(); // Use DatabaseConnection
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
     * Deletes a product and its associated images from the database.
     * This operation is transactional (all or nothing).
     * @param productId The ID of the product to delete.
     * @throws SQLException If a database error occurs during deletion or transaction management.
     */
    public static void deleteProduct(int productId) throws SQLException {
        String sqlDeleteImages = "DELETE FROM product_images WHERE product_id = ?";
        String sqlDeleteProduct = "DELETE FROM products WHERE product_id = ?";

        Connection conn = null;
        try { 
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            
            try (PreparedStatement pstmtImages = conn.prepareStatement(sqlDeleteImages)) {
                pstmtImages.setInt(1, productId);
                pstmtImages.executeUpdate();
            }

            try (PreparedStatement pstmtProduct = conn.prepareStatement(sqlDeleteProduct)) {
                pstmtProduct.setInt(1, productId);
                pstmtProduct.executeUpdate();
            }

            conn.commit();
            System.out.println("Product and its images deleted for product ID: " + productId);
        } catch (SQLException e) {
            if (e != null && e.getMessage() != null && e.getMessage().contains("Cannot delete or update a parent row: a foreign key constraint fails")) {
                 System.err.println("Attempted to delete product " + productId + " which might still be referenced elsewhere (e.g., order_items).");
            }
            if (conn != null) {
                try {
                    conn.rollback();
                    System.err.println("Transaction rolled back for product deletion.");
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
                    System.out.println("Connection for deleteProduct closed.");
                } catch (SQLException fe) {
                    System.err.println("Error setting auto commit or closing connection: " + fe.getMessage());
                }
            }
        }
    }
    
    /**
     * Updates an existing product's details in the 'products' table.
     * Image updates are handled separately (by deleting old and inserting new images).
     * @param productId The ID of the product to update.
     * @param name New product name.
     * @param description New product description.
     * @param price New product price.
     * @param originalPrice New original price.
     * @param stock New stock quantity.
     * @param condition New product condition.
     * @param minOrder New minimum order quantity.
     * @param brand New product brand.
     * @return True if the product was updated successfully, false otherwise.
     * @throws SQLException If a database error occurs.
     */
    public static boolean updateProduct(int productId, String name, String description, double price, double originalPrice, int stock, String condition, String minOrder, String brand) throws SQLException {
        String sql = "UPDATE products SET name = ?, description = ?, price = ?, original_price = ?, stock = ?, `condition` = ?, min_order = ?, `brand` = ? WHERE product_id = ?";
        try (Connection conn = DatabaseConnection.getConnection(); // Use DatabaseConnection
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
                return true; // Update successful
            } else {
                System.out.println("No product found with ID: " + productId + " to update.");
                return false; // No rows affected
            }
        }
    }

    /**
     * Deletes all images associated with a specific product from 'product_images' table.
     * This is typically used before re-uploading new images for an edited product.
     * @param productId The ID of the product whose images are to be deleted.
     * @throws SQLException If a database error occurs.
     */
    public static void deleteProductImagesForProduct(int productId) throws SQLException {
        String sql = "DELETE FROM product_images WHERE product_id = ?";
        try (Connection conn = DatabaseConnection.getConnection(); // Use DatabaseConnection
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, productId);
            int affectedRows = pstmt.executeUpdate();
            System.out.println(affectedRows + " images deleted for product ID: " + productId);
        }
    }

    /**
     * Deletes a single image from the 'product_images' table by its image ID.
     * @param imageId The ID of the image to delete.
     * @throws SQLException If a database error occurs.
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
     * Sets a specific image as the main image for a product.
     * This involves setting is_main_image to FALSE for all other images of the product,
     * then setting it to TRUE for the specified image. This is a transactional operation.
     * @param productId The ID of the product.
     * @param imageId The ID of the image to set as main.
     * @throws SQLException If a database error occurs.
     */
    public static void setMainProductImage(int productId, int imageId) throws SQLException {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Start transaction

            // 1. Set all images for this product to not main
            String sqlResetMain = "UPDATE product_images SET is_main_image = FALSE WHERE product_id = ?";
            try (PreparedStatement pstmtReset = conn.prepareStatement(sqlResetMain)) {
                pstmtReset.setInt(1, productId);
                pstmtReset.executeUpdate();
            }

            // 2. Set the specified image as main
            String sqlSetMain = "UPDATE product_images SET is_main_image = TRUE WHERE image_id = ? AND product_id = ?";
            try (PreparedStatement pstmtSet = conn.prepareStatement(sqlSetMain)) {
                pstmtSet.setBoolean(1, true); // Set is_main_image ke TRUE
                pstmtSet.setInt(2, imageId);
                pstmtSet.setInt(3, productId); // Parameter ke-3
                pstmtSet.executeUpdate();
            }

            conn.commit(); // Commit transaction
            System.out.println("Image ID " + imageId + " set as main for Product ID " + productId);
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback(); // Rollback on error
                    System.err.println("Transaction rolled back for setMainProductImage: " + e.getMessage());
                } catch (SQLException rbex) {
                    System.err.println("Error rolling back transaction: " + rbex.getMessage());
                }
            }
            throw e; // Re-throw for caller handling
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // Reset auto-commit
                    conn.close(); // Close connection
                } catch (SQLException fe) {
                    System.err.println("Error setting auto commit or closing connection: " + fe.getMessage());
                }
            }
        }
    }

    /**
     * Fetches details of all images for a specific product from the database.
     * This is used by ProductImageManagerUI to display all images for editing.
     * @param productId The ID of the product.
     * @return A list of Object arrays, each containing (image_id, image_data, file_extension, is_main_image, order_index).
     * @throws SQLException If a database error occurs.
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
}