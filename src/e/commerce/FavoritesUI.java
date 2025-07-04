package e.commerce;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.List;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.SQLException; 
import java.awt.Image; 

public class FavoritesUI extends JPanel {
    private List<FavoriteItem> favoriteItems; 
    private JPanel favoriteProductsPanel;
    private ViewController viewController;
    private int currentUserId; 

    public FavoritesUI(ViewController viewController, int userId) { 
        this.viewController = viewController;
        this.currentUserId = userId; 
        loadFavoriteData(); 
        initializeUI();
    }

    private void loadFavoriteData() {
        try {
            favoriteItems = ProductRepository.getFavoriteItemsForUser(currentUserId);
        } catch (SQLException e) {
            System.err.println("Error loading favorite items from database: " + e.getMessage());
            e.printStackTrace();
            favoriteItems = new ArrayList<>(); 
            JOptionPane.showMessageDialog(this,
                    "Error loading your favorite items. Please try again later.",
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void initializeUI() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        JPanel contentPanel = createContentPanel();
        add(contentPanel, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(new EmptyBorder(20, 20, 10, 20));

        JLabel titleLabel = new JLabel("Favorit Saya");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(Color.BLACK);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JLabel countLabel = new JLabel(favoriteItems.size() + " item");
        countLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        countLabel.setForeground(Color.GRAY);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.setBackground(Color.WHITE);

        JButton continueShoppingBtn = new JButton("Lanjutkan Belanja");
        continueShoppingBtn.setBackground(Color.WHITE);
        continueShoppingBtn.setForeground(new Color(255, 69, 0));
        continueShoppingBtn.setBorderPainted(false);
        continueShoppingBtn.setFocusPainted(false);
        continueShoppingBtn.setFont(new Font("Arial", Font.BOLD, 14));
        continueShoppingBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        continueShoppingBtn.addActionListener(e -> {
            if (viewController != null) {
                viewController.showDashboardView();
            }
        });

        rightPanel.add(countLabel);
        rightPanel.add(Box.createRigidArea(new Dimension(20, 0)));
        rightPanel.add(continueShoppingBtn);
        headerPanel.add(rightPanel, BorderLayout.EAST);

        return headerPanel;
    }

    private JPanel createContentPanel() {
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(Color.WHITE);

        if (favoriteItems.isEmpty()) {
            JPanel emptyPanel = createEmptyStatePanel();
            contentPanel.add(emptyPanel, BorderLayout.CENTER);
        } else {
            JPanel productsWrapperPanel = new JPanel();
            productsWrapperPanel.setLayout(new BoxLayout(productsWrapperPanel, BoxLayout.Y_AXIS));
            productsWrapperPanel.setBackground(Color.WHITE);
            productsWrapperPanel.setBorder(new EmptyBorder(10, 20, 20, 20));
            //productsWrapperPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100)); 

            favoriteProductsPanel = new JPanel (new FlowLayout(FlowLayout.LEFT, 15, 20)); 
            favoriteProductsPanel.setBackground(Color.WHITE);
            favoriteProductsPanel.setAlignmentX(Component.LEFT_ALIGNMENT); 

            productsWrapperPanel.add(favoriteProductsPanel);
            productsWrapperPanel.add(Box.createVerticalGlue()); 

            refreshFavoritesList();

            JScrollPane scrollPane = new JScrollPane(productsWrapperPanel); 
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setBorder(null);
            scrollPane.getVerticalScrollBar().setUnitIncrement(16);

            contentPanel.add(scrollPane, BorderLayout.CENTER);
        }

        return contentPanel;
    }

    private JPanel createEmptyStatePanel() {
        JPanel emptyPanel = new JPanel(new BorderLayout());
        emptyPanel.setBackground(Color.WHITE);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(Color.WHITE);

        JLabel heartIcon = new JLabel("♡");
        heartIcon.setFont(new Font("Arial", Font.PLAIN, 80));
        heartIcon.setForeground(Color.LIGHT_GRAY);
        heartIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel emptyTitle = new JLabel("Belum Ada Favorit");
        emptyTitle.setFont(new Font("Arial", Font.BOLD, 24));
        emptyTitle.setForeground(Color.GRAY);
        emptyTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel emptyDesc = new JLabel("Mulai tambahkan produk ke favorit Anda!");
        emptyDesc.setFont(new Font("Arial", Font.PLAIN, 16));
        emptyDesc.setForeground(Color.LIGHT_GRAY);
        emptyDesc.setAlignmentX(Component.CENTER_ALIGNMENT);

        centerPanel.add(Box.createVerticalGlue());
        centerPanel.add(heartIcon);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        centerPanel.add(emptyTitle);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        centerPanel.add(emptyDesc);
        centerPanel.add(Box.createVerticalGlue());

        emptyPanel.add(centerPanel, BorderLayout.CENTER);
        return emptyPanel;
    }

    private void refreshFavoritesList() {
        favoriteProductsPanel.removeAll();

        for (FavoriteItem item : favoriteItems) {
            JPanel productCard = createFavoriteProductCard(item);
            favoriteProductsPanel.add(productCard);
        }

        favoriteProductsPanel.revalidate();
        favoriteProductsPanel.repaint();
        
        // Memastikan update countLabel di header saat refresh
        // Akses kembali label count
        Component[] headerComponents = ((JPanel)getComponent(0)).getComponents(); 
        for (Component comp : headerComponents) {
            if (comp instanceof JPanel && ((JPanel)comp).getLayout() instanceof FlowLayout) { 
                for (Component innerComp : ((JPanel)comp).getComponents()) {
                    if (innerComp instanceof JLabel) {
                        JLabel label = (JLabel) innerComp;
                        if (label.getText().contains(" item")) { 
                            label.setText(favoriteItems.size() + " item");
                            label.revalidate();
                            label.repaint();
                            break;
                        }
                    }
                }
                break;
            }
        }
        // Juga panggil updateHeaderCartAndFavCounts di UserDashboardUI jika FavoriteUI adalah childnya
        if (viewController instanceof UserDashboardUI) {
             ((UserDashboardUI)viewController).updateHeaderCartAndFavCounts();
        }
    }

    private JPanel createFavoriteProductCard(FavoriteItem item) {
        JPanel card = new JPanel();
        card.setLayout(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createLineBorder(new Color(240, 240, 240), 1));
        card.setPreferredSize(new Dimension(180, 320));

        JPanel imagePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                int panelWidth = getWidth();
                int panelHeight = getHeight();

                g2d.setColor(item.getBgColor());
                g2d.fillRect(0, 0, panelWidth, panelHeight);

                Image mainImage = null;
                if (item.getLoadedImages() != null && !item.getLoadedImages().isEmpty()) { 
                    mainImage = item.getLoadedImages().get(0);
                }

                if (mainImage == null) {
                    g2d.setColor(item.getBgColor().darker());
                    g2d.setFont(new Font("Arial", Font.BOLD, 12));
                    FontMetrics fm = g2d.getFontMetrics();
                    String text = "NO IMAGE";
                    int textWidth = fm.stringWidth(text);
                    int textHeight = fm.getHeight();
                    g2d.drawString(text, (panelWidth - textWidth) / 2, (panelHeight + textHeight / 2) / 2 - fm.getDescent());
                } else {
                    int originalWidth = mainImage.getWidth(null);
                    int originalHeight = mainImage.getHeight(null);

                    // Penskalaan yang mengisi seluruh area panel (fill mode)
                    double scaleX = (double) panelWidth / originalWidth;
                    double scaleY = (double) panelHeight / originalHeight;
                    double scale = Math.max(scaleX, scaleY); // Pilih skala yang lebih besar

                    int scaledWidth = (int) (originalWidth * scale);
                    int scaledHeight = (int) (originalHeight * scale);

                    // Posisi gambar agar terpusat
                    int x = (panelWidth - scaledWidth) / 2;
                    int y = (panelHeight - scaledHeight) / 2;

                    g2d.drawImage(mainImage, x, y, scaledWidth, scaledHeight, null);
                }

            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(180, 180);
            }

            @Override
            public Dimension getMinimumSize() {
                return new Dimension(50, 50);
            }

            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
            }
        };
        imagePanel.setOpaque(true);

        JPanel overlayPanel = new JPanel();
        overlayPanel.setLayout(new BorderLayout());
        overlayPanel.setOpaque(false);

        JPanel heartPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        heartPanel.setOpaque(false);
        heartPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        JButton heartButton = new JButton("♥");
        heartButton.setFont(new Font("Arial", Font.BOLD, 14));
        heartButton.setForeground(new Color(255, 69, 0));
        heartButton.setBackground(Color.WHITE);
        heartButton.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        heartButton.setFocusPainted(false);
        heartButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        heartButton.setToolTipText("Hapus dari favorit");

        heartButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                heartButton.setBackground(new Color(255, 240, 240));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                heartButton.setBackground(Color.WHITE);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                int result = JOptionPane.showConfirmDialog(
                        FavoritesUI.this,
                        "Hapus '" + item.getName() + "' dari favorit?",
                        "Hapus Favorit",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );

                if (result == JOptionPane.YES_OPTION) {
                    try {
                        ProductRepository.removeFavoriteItem(currentUserId, item.getId()); 
                        removeFavoriteItem(item); 
                        JOptionPane.showMessageDialog(FavoritesUI.this,
                                "'" + item.getName() + "' berhasil dihapus dari favorit.",
                                "Hapus Berhasil",
                                JOptionPane.INFORMATION_MESSAGE);
                    } catch (SQLException ex) {
                        System.err.println("Error removing favorite item from database: " + ex.getMessage());
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(FavoritesUI.this,
                                "Gagal menghapus '" + item.getName() + "' dari favorit. Silakan coba lagi.",
                                "Error Hapus Favorit",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        heartPanel.add(heartButton);
        overlayPanel.add(heartPanel, BorderLayout.NORTH);

        JPanel imageContainer = new JPanel();
        imageContainer.setLayout(new OverlayLayout(imageContainer));
        imageContainer.add(overlayPanel);
        imageContainer.add(imagePanel);

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(Color.WHITE);
        infoPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel nameLabel = new JLabel("<html><div style='text-align: center;'>" + item.getName() + "</div></html>");
        nameLabel.setFont(new Font("Arial", Font.BOLD, 13));
        nameLabel.setForeground(Color.BLACK);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel stockLabel = new JLabel("<html><div style='text-align: center;'>Stok: " + item.getStock() + "</div></html>");
        stockLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        stockLabel.setForeground(Color.GRAY);
        stockLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel priceLabel = new JLabel("<html><div style='text-align: center;'><b>Rp " + String.format("%,.0f", item.getPrice()) + "</b></div></html>");
        priceLabel.setFont(new Font("Arial", Font.BOLD, 14));
        priceLabel.setForeground(new Color(255, 89, 0));
        priceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel originalPriceLabel = null;
        if (item.getOriginalPrice() > item.getPrice()) {
            originalPriceLabel = new JLabel("<html><div style='text-align: center;'><strike>Rp " + String.format("%,.0f", item.getOriginalPrice()) + "</strike></div></html>");
            originalPriceLabel.setFont(new Font("Arial", Font.PLAIN, 11));
            originalPriceLabel.setForeground(Color.GRAY);
            originalPriceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        }

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 5));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton addToCartButton = new JButton("Tambah ke Keranjang");
        addToCartButton.setBackground(new Color(255, 89, 0));
        addToCartButton.setForeground(Color.WHITE);
        addToCartButton.setBorderPainted(false);
        addToCartButton.setFocusPainted(false);
        addToCartButton.setFont(new Font("Arial", Font.BOLD, 10));
        addToCartButton.setBorder(new EmptyBorder(6, 12, 6, 12));
        addToCartButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        

        addToCartButton.addActionListener(e -> {
            try {
                ProductRepository.addProductToCart(currentUserId, item.getId(), 1);
                JOptionPane.showMessageDialog(
                        FavoritesUI.this,
                        "'" + item.getName() + "' ditambahkan ke keranjang!",
                        "Ditambahkan ke Keranjang",
                        JOptionPane.INFORMATION_MESSAGE
                );
            } catch (SQLException ex) {
                System.err.println("Error adding product to cart: " + ex.getMessage());
                ex.printStackTrace();
                JOptionPane.showMessageDialog(FavoritesUI.this,
                        "Gagal menambahkan '" + item.getName() + "' ke keranjang. Silakan coba lagi.",
                        "Error Tambah ke Keranjang",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        buttonPanel.add(addToCartButton);

        infoPanel.add(nameLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 2)));
        infoPanel.add(stockLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        infoPanel.add(priceLabel);
        if (originalPriceLabel != null) {
            infoPanel.add(Box.createRigidArea(new Dimension(0, 2)));
            infoPanel.add(originalPriceLabel);
        }
        infoPanel.add(Box.createRigidArea(new Dimension(0, 6)));
        infoPanel.add(buttonPanel);

        card.add(imageContainer, BorderLayout.NORTH);
        card.add(infoPanel, BorderLayout.CENTER);

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!SwingUtilities.isDescendingFrom(e.getComponent(), heartButton) &&
                    !SwingUtilities.isDescendingFrom(e.getComponent(), addToCartButton)) {

                    showProductDetails(item);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBorder(BorderFactory.createLineBorder(new Color(255, 89, 0), 2));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setBorder(BorderFactory.createLineBorder(new Color(240, 240, 240), 1));
            }
        });

        return card;
    }

    private void removeFavoriteItem(FavoriteItem item) {
        favoriteItems.remove(item);
        // Re-initialize the UI to reflect the changes (e.g., update item count, show empty state if needed)
        initializeUI();
        revalidate();
        repaint();
    }

    private void showProductDetails(FavoriteItem item) {
        if (viewController != null) {
            viewController.showProductDetail(item);
        }
    }

    // *** INNER CLASS FavoriteItem (Keep this as is, but consider moving it to a more central location if used by other UIs) ***
    public static class FavoriteItem {
        private int id;
        private String name;
        private String description;
        private double price;
        private double originalPrice;
        private int stock;
        private String condition;
        private String minOrder;
        private String brand;
        private Color bgColor; // Ini sudah benar, diinisialisasi dari hexColor
        private int sellerId;
        private double weight;
        
        // --- DIHAPUS: rawImageDataList dan rawFileExtensionList
        // private List<byte[]> rawImageDataList;
        // private List<String> rawFileExtensionList;

        protected List<Image> loadedImages; // Ini yang akan diisi

        // --- KONSTRUKTOR YANG DIREVISI: Menggunakan List<Image> BUKAN List<String> ---
        public FavoriteItem(int id, String name, String description, double price, double originalPrice,
                            int stock, String condition, String minOrder, String brand,
                            String hexColor, List<Image> loadedImages, int sellerId, double weight) { // <<< DIREVISI DI SINI
            this.id = id;
            this.name = name;
            this.description = description;
            this.price = price;
            this.originalPrice = originalPrice;
            this.stock = stock;
            this.condition = condition;
            this.minOrder = minOrder;
            this.brand = brand;
            try { // Tambahkan try-catch untuk Color.decode
                this.bgColor = Color.decode(hexColor);
            } catch (NumberFormatException e) {
                this.bgColor = Color.LIGHT_GRAY; // Fallback jika hexColor tidak valid
                System.err.println("Invalid hexColor for FavoriteItem: " + hexColor + ". Defaulting to Light Gray.");
            }
            this.loadedImages = loadedImages != null ? loadedImages : new ArrayList<>(); // Inisialisasi dari parameter
            this.sellerId = sellerId;
            this.weight = weight;
        }

        // --- TAMBAHAN: Konstruktor overload agar kode lama (tanpa weight) tidak error (opsional) ---
        public FavoriteItem(int id, String name, String description, double price, double originalPrice,
                            int stock, String condition, String minOrder, String brand,
                            String hexColor, List<Image> loadedImages, int sellerId) {
            this(id, name, description, price, originalPrice, stock, condition, minOrder, brand,
                 hexColor, loadedImages, sellerId, 0.0); // Panggil konstruktor utama dengan weight 0.0
        }


        public void setLoadedImages(List<Image> images) {
            this.loadedImages = images;
        }
        public void setWeight(double weight) { this.weight = weight; }

        public int getId() { return id; }
        public int getSellerId() { return sellerId; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public double getPrice() { return price; }
        public double getOriginalPrice() { return originalPrice; }
        public int getStock() { return stock; }
        public String getCondition() { return condition; }
        public String getMinOrder() { return minOrder; }
        public String getBrand() { return brand; }
        public Color getBgColor() { return bgColor; }
        public List<Image> getLoadedImages() { return loadedImages; }
        public double getWeight() { return weight; }

        // --- Tambahan: getProductId() yang diperlukan SupervisorDashboardUI ---
        public int getProductId() {
            return this.id;
        }
        // --- Akhir Tambahan ---
    }
}