package e.commerce;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.List;
import java.util.ArrayList;
import java.sql.SQLException; // <--- TAMBAH IMPORT INI
import e.commerce.Authentication; // <--- TAMBAH IMPORT INI

public class ProductDetailUI extends JPanel {
    private FavoritesUI.FavoriteItem currentProduct;
    private int currentImageIndex = 0;
    private JPanel mainImagePanel;
    private JPanel thumbnailPanel;
    private JSpinner quantitySpinner;
    private JLabel stockLabel;
    private JLabel subtotalValueLabel;
    private JButton addToCartBtn;
    private JButton buyNowBtn;
    private JButton wishlistBtn;
    private boolean isFavorite = false;
    private ViewController viewController;

    // Konstruktor utama yang menerima produk dan ViewController
    public ProductDetailUI(FavoritesUI.FavoriteItem product, ViewController viewController) {
        this.currentProduct = product;
        this.viewController = viewController;

        // Periksa status favorit saat inisialisasi (di awal konstruktor)
        if (Authentication.getCurrentUser() != null) {
            try {
                // Memeriksa apakah produk ini sudah ada di daftar favorit pengguna
                isFavorite = ProductRepository.getFavoriteItemsForUser(Authentication.getCurrentUser().getId()) //
                                             .stream()
                                             .anyMatch(item -> item.getId() == currentProduct.getId());
            } catch (SQLException e) {
                System.err.println("Error checking favorite status: " + e.getMessage());
                isFavorite = false; // Default ke false jika ada error database
            }
        }

        initializeUI();
        updateMainImageDisplay();
        updateThumbnailSelection();
        updateSubtotal();
    }

    private void initializeUI() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        JPanel mainContentPanel = new JPanel(new BorderLayout());
        mainContentPanel.setBackground(Color.WHITE);
        mainContentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(Color.WHITE);

        JPanel imageSection = createImageSection();
        contentPanel.add(imageSection, BorderLayout.WEST);

        JPanel detailSection = createDetailSection();
        contentPanel.add(detailSection, BorderLayout.CENTER);

        mainContentPanel.add(contentPanel, BorderLayout.CENTER);

        JPanel tabsSection = createTabsSection();
        mainContentPanel.add(tabsSection, BorderLayout.SOUTH);

        // Tambahkan bagian Rekomendasi
        JPanel recommendationSection = createRecommendationSection();
        JPanel combinedContent = new JPanel();
        combinedContent.setLayout(new BoxLayout(combinedContent, BoxLayout.Y_AXIS));
        combinedContent.setBackground(Color.WHITE);
        combinedContent.add(mainContentPanel);
        combinedContent.add(recommendationSection);

        JScrollPane scrollPane = new JScrollPane(combinedContent);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(new EmptyBorder(10, 20, 0, 20));

        JPanel breadcrumbPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        breadcrumbPanel.setBackground(Color.WHITE);

        String[] breadcrumbs = {"Home", "Komputer & Laptop", "Laptop", "Laptop 2 in 1"};
        for (int i = 0; i < breadcrumbs.length; i++) {
            JLabel breadcrumbLabel = new JLabel(breadcrumbs[i]);
            breadcrumbLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            breadcrumbLabel.setForeground(new Color(0, 150, 136));
            breadcrumbLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            breadcrumbPanel.add(breadcrumbLabel);

            if (i < breadcrumbs.length - 1) {
                JLabel separator = new JLabel(" > ");
                separator.setFont(new Font("Arial", Font.PLAIN, 12));
                separator.setForeground(Color.GRAY);
                breadcrumbPanel.add(separator);
            }
        }

        headerPanel.add(breadcrumbPanel, BorderLayout.WEST);
        return headerPanel;
    }

    private JPanel createImageSection() {
        JPanel imageSection = new JPanel(new BorderLayout());
        imageSection.setBackground(Color.WHITE);
        imageSection.setPreferredSize(new Dimension(450, 500));
        imageSection.setBorder(new EmptyBorder(0, 0, 0, 20));

        mainImagePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                g2d.setColor(currentProduct.getBgColor());
                g2d.fillRect(0, 0, getWidth(), getHeight());

                Image displayImage = null;
                if (!currentProduct.getLoadedImages().isEmpty() && currentImageIndex < currentProduct.getLoadedImages().size()) {
                    displayImage = currentProduct.getLoadedImages().get(currentImageIndex);
                }

                if (displayImage == null) {
                    g2d.setColor(currentProduct.getBgColor().darker());
                    g2d.setFont(new Font("Arial", Font.BOLD, 24));
                    FontMetrics fm = g2d.getFontMetrics();
                    String text = "NO IMAGE"; // Placeholder jika tidak ada gambar
                    int textWidth = fm.stringWidth(text);
                    int textHeight = fm.getHeight();
                    g2d.drawString(text, (getWidth() - textWidth) / 2, (getHeight() + textHeight) / 2);
                } else {
                    int originalWidth = displayImage.getWidth(null);
                    int originalHeight = displayImage.getHeight(null);

                    double scaleX = (double) getWidth() / originalWidth;
                    double scaleY = (double) getHeight() / originalHeight;
                    double scale = Math.min(scaleX, scaleY);

                    int scaledWidth = (int) (originalWidth * scale);
                    int scaledHeight = (int) (originalHeight * scale);

                    int x = (getWidth() - scaledWidth) / 2;
                    int y = (getHeight() - scaledHeight) / 2;

                    g2d.drawImage(displayImage, x, y, scaledWidth, scaledHeight, null);
                }
            }
        };
        mainImagePanel.setPreferredSize(new Dimension(400, 350));
        mainImagePanel.setBorder(BorderFactory.createLineBorder(new Color(240, 240, 240), 1));
        mainImagePanel.setOpaque(true);

        thumbnailPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        thumbnailPanel.setBackground(Color.WHITE);
        thumbnailPanel.setPreferredSize(new Dimension(400, 80));

        if (currentProduct.getLoadedImages() != null && !currentProduct.getLoadedImages().isEmpty()) {
            for (int i = 0; i < currentProduct.getLoadedImages().size(); i++) {
                JPanel thumbnail = createThumbnail(i);
                thumbnailPanel.add(thumbnail);
            }
        } else {
            for (int i = 0; i < 5; i++) {
                JPanel thumbnail = createThumbnail(i);
                thumbnailPanel.add(thumbnail);
            }
        }

        imageSection.add(mainImagePanel, BorderLayout.CENTER);
        imageSection.add(thumbnailPanel, BorderLayout.SOUTH);

        return imageSection;
    }

    private JPanel createThumbnail(int index) {
        JPanel thumbnail = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                g2d.setColor(currentProduct.getBgColor());
                g2d.fillRect(0, 0, getWidth(), getHeight());

                Image thumbImage = null;
                if (currentProduct.getLoadedImages() != null && index < currentProduct.getLoadedImages().size()) {
                    thumbImage = currentProduct.getLoadedImages().get(index);
                }

                if (thumbImage == null) {
                    g2d.setColor(currentProduct.getBgColor().darker());
                    g2d.setFont(new Font("Arial", Font.PLAIN, 10));
                    FontMetrics fm = g2d.getFontMetrics();
                    String text = "" + (index + 1);
                    int textWidth = fm.stringWidth(text);
                    int textHeight = fm.getHeight();
                    g2d.drawString(text, (getWidth() - textWidth) / 2, (getHeight() + textHeight) / 2);
                } else {
                    int originalWidth = thumbImage.getWidth(null);
                    int originalHeight = thumbImage.getHeight(null);

                    double scaleX = (double) getWidth() / originalWidth;
                    double scaleY = (double) getHeight() / originalHeight;
                    double scale = Math.min(scaleX, scaleY);

                    int scaledWidth = (int) (originalWidth * scale);
                    int scaledHeight = (int) (originalHeight * scale);

                    int x = (getWidth() - scaledWidth) / 2;
                    int y = (getHeight() - scaledHeight) / 2;

                    g2d.drawImage(thumbImage, x, y, scaledWidth, scaledHeight, null);
                }
            }
        };
        thumbnail.setPreferredSize(new Dimension(70, 60));
        thumbnail.setBorder(BorderFactory.createLineBorder(
                index == currentImageIndex ? new Color(255, 89, 0) : new Color(240, 240, 240),
                index == currentImageIndex ? 2 : 1
        ));
        thumbnail.setCursor(new Cursor(Cursor.HAND_CURSOR));
        thumbnail.setOpaque(true);

        final int thumbIndex = index;
        thumbnail.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                currentImageIndex = thumbIndex;
                updateThumbnailSelection();
                updateMainImageDisplay();
            }
        });

        return thumbnail;
    }

    private void updateThumbnailSelection() {
        Component[] thumbnails = thumbnailPanel.getComponents();
        for (int i = 0; i < thumbnails.length; i++) {
            JPanel thumb = (JPanel) thumbnails[i];
            thumb.setBorder(BorderFactory.createLineBorder(
                    i == currentImageIndex ? new Color(255, 89, 0) : new Color(240, 240, 240),
                    i == currentImageIndex ? 2 : 1
            ));
        }
        thumbnailPanel.repaint();
    }

    private void updateMainImageDisplay() {
        mainImagePanel.repaint();
        mainImagePanel.revalidate();
    }

    private JPanel createDetailSection() {
        JPanel detailPanel = new JPanel();
        detailPanel.setLayout(new BoxLayout(detailPanel, BoxLayout.Y_AXIS));
        detailPanel.setBackground(Color.WHITE);
        detailPanel.setPreferredSize(new Dimension(500, 500));
        detailPanel.setBorder(new EmptyBorder(0, 20, 0, 0));

        JLabel titleLabel = new JLabel("<html>" + currentProduct.getName() + "<br><span style='font-weight:normal; color:gray;'>" + currentProduct.getDescription() + "</span></html>");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(Color.BLACK);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
        currencyFormat.setMinimumFractionDigits(0);

        JLabel priceLabel = new JLabel(currencyFormat.format(currentProduct.getPrice()));
        priceLabel.setFont(new Font("Arial", Font.BOLD, 28));
        priceLabel.setForeground(new Color(255, 89, 0));
        priceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel originalPriceLabel = null;
        if (currentProduct.getOriginalPrice() > currentProduct.getPrice()) {
            originalPriceLabel = new JLabel("<html><strike>" + currencyFormat.format(currentProduct.getOriginalPrice()) + "</strike></html>");
            originalPriceLabel.setFont(new Font("Arial", Font.PLAIN, 16));
            originalPriceLabel.setForeground(Color.GRAY);
            originalPriceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(Color.WHITE);
        infoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, infoPanel.getPreferredSize().height));

        addInfoRow(infoPanel, "Kondisi:", currentProduct.getCondition());
        addInfoRow(infoPanel, "Min. Pemesanan:", currentProduct.getMinOrder());
        addInfoRow(infoPanel, "Etalase:", currentProduct.getBrand());

        JPanel quantityPanel = createQuantityPanel();

        JPanel actionPanel = createActionPanel();

        JPanel additionalActionPanel = createAdditionalActionPanel();

        detailPanel.add(titleLabel);
        detailPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        detailPanel.add(priceLabel);
        if (originalPriceLabel != null) {
            detailPanel.add(Box.createRigidArea(new Dimension(0, 5)));
            detailPanel.add(originalPriceLabel);
        }
        detailPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        detailPanel.add(infoPanel);
        detailPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        detailPanel.add(quantityPanel);
        detailPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        detailPanel.add(actionPanel);
        detailPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        detailPanel.add(additionalActionPanel);

        return detailPanel;
    }

    private void addInfoRow(JPanel parent, String label, String value) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        row.setBackground(Color.WHITE);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));

        JLabel labelComp = new JLabel(label);
        labelComp.setFont(new Font("Arial", Font.PLAIN, 14));
        labelComp.setForeground(Color.GRAY);
        labelComp.setPreferredSize(new Dimension(120, 20));

        JLabel valueComp = new JLabel(value);
        valueComp.setFont(new Font("Arial", Font.BOLD, 14));
        valueComp.setForeground(value.equals(currentProduct.getBrand()) ? new Color(0, 150, 136) : Color.BLACK);

        row.add(labelComp);
        row.add(valueComp);
        parent.add(row);
    }

    private JPanel createQuantityPanel() {
        JPanel quantityPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        quantityPanel.setBackground(Color.WHITE);
        quantityPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        quantityPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        JLabel qtyLabel = new JLabel("Atur jumlah dan catatan");
        qtyLabel.setFont(new Font("Arial", Font.BOLD, 14));
        qtyLabel.setForeground(Color.BLACK);

        JPanel qtyControlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        qtyControlPanel.setBackground(Color.WHITE);

        quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, currentProduct.getStock(), 1));
        quantitySpinner.setPreferredSize(new Dimension(80, 35));
        quantitySpinner.setFont(new Font("Arial", Font.PLAIN, 14));

        stockLabel = new JLabel("Stok Total: Sisa " + currentProduct.getStock());
        stockLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        stockLabel.setForeground(Color.GRAY);

        subtotalValueLabel = new JLabel();
        subtotalValueLabel.setFont(new Font("Arial", Font.BOLD, 16));
        subtotalValueLabel.setForeground(Color.BLACK);
        updateSubtotal();

        quantitySpinner.addChangeListener(e -> {
            updateSubtotal();
        });

        qtyControlPanel.add(quantitySpinner);
        qtyControlPanel.add(stockLabel);

        JPanel subtotalDisplayPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        subtotalDisplayPanel.setBackground(Color.WHITE);
        subtotalDisplayPanel.add(new JLabel("Subtotal"));
        subtotalDisplayPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        subtotalDisplayPanel.add(subtotalValueLabel);

        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setBackground(Color.WHITE);
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);

        qtyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        qtyControlPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtotalDisplayPanel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        wrapper.add(qtyLabel);
        wrapper.add(qtyControlPanel);
        wrapper.add(subtotalDisplayPanel);

        return wrapper;
    }

    private void updateSubtotal() {
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
        currencyFormat.setMinimumFractionDigits(0);
        int qty = (Integer) quantitySpinner.getValue();
        double subtotal = currentProduct.getPrice() * qty;
        subtotalValueLabel.setText(currencyFormat.format(subtotal));
    }


    private JPanel createActionPanel() {
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actionPanel.setBackground(Color.WHITE);
        actionPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        actionPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        addToCartBtn = new JButton("+ Keranjang");
        addToCartBtn.setPreferredSize(new Dimension(140, 40));
        addToCartBtn.setBackground(Color.WHITE);
        addToCartBtn.setForeground(new Color(255, 89, 0));
        addToCartBtn.setBorder(BorderFactory.createLineBorder(new Color(255, 89, 0), 2));
        addToCartBtn.setFont(new Font("Arial", Font.BOLD, 14));
        addToCartBtn.setFocusPainted(false);
        addToCartBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        buyNowBtn = new JButton("Beli Langsung");
        buyNowBtn.setPreferredSize(new Dimension(140, 40));
        buyNowBtn.setBackground(new Color(255, 89, 0));
        buyNowBtn.setForeground(Color.WHITE);
        buyNowBtn.setBorderPainted(false);
        buyNowBtn.setFont(new Font("Arial", Font.BOLD, 14));
        buyNowBtn.setFocusPainted(false);
        buyNowBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // --- REVISI: ActionListener untuk Add To Cart ---
        addToCartBtn.addActionListener(e -> {
            User currentUser = Authentication.getCurrentUser();
            if (currentUser == null) {
                JOptionPane.showMessageDialog(this, "Anda harus login untuk menambahkan produk ke keranjang.", "Login Diperlukan", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int userId = currentUser.getId();
            int qty = (Integer) quantitySpinner.getValue();
            try {
                ProductRepository.addProductToCart(userId, currentProduct.getId(), qty);
                JOptionPane.showMessageDialog(this,
                    qty + " item(s) berhasil ditambahkan ke keranjang!",
                    "Ditambahkan ke Keranjang",
                    JOptionPane.INFORMATION_MESSAGE);
                // Opsional: perbarui tampilan keranjang di header dashboard
                // Ini akan membutuhkan metode di ViewController atau memanggil UserDashboardUI langsung
                // Contoh: ((UserDashboardUI) viewController).updateHeaderCartAndFavCounts();
            } catch (SQLException ex) {
                System.err.println("Error menambahkan produk ke keranjang: " + ex.getMessage());
                JOptionPane.showMessageDialog(this, "Gagal menambahkan produk ke keranjang. Silakan coba lagi.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        buyNowBtn.addActionListener(e -> {
            int qty = (Integer) quantitySpinner.getValue();
            double total = currentProduct.getPrice() * qty;
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
            currencyFormat.setMinimumFractionDigits(0);
            JOptionPane.showMessageDialog(this,
                "Proceeding to checkout with " + qty + " item(s)\nTotal: " + currencyFormat.format(total),
                "Buy Now",
                JOptionPane.INFORMATION_MESSAGE);
            // Anda mungkin ingin mengarahkan ke checkout dengan produk ini
            // viewController.showCheckoutView(currentProduct, qty); // Jika Anda punya metode showCheckoutView dengan parameter
        });

        actionPanel.add(addToCartBtn);
        actionPanel.add(buyNowBtn);

        return actionPanel;
    }

    private JPanel createAdditionalActionPanel() {
        JPanel additionalPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        additionalPanel.setBackground(Color.WHITE);
        additionalPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        additionalPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JButton chatBtn = new JButton("💬 Chat");
        chatBtn.setBackground(Color.WHITE);
        chatBtn.setForeground(Color.GRAY);
        chatBtn.setBorderPainted(false);
        chatBtn.setFocusPainted(false);
        chatBtn.setFont(new Font("Arial", Font.PLAIN, 12));
        chatBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        wishlistBtn = new JButton((isFavorite ? "❤️" : "🤍") + " Wishlist");
        wishlistBtn.setBackground(Color.WHITE);
        wishlistBtn.setForeground(Color.GRAY);
        wishlistBtn.setBorderPainted(false);
        wishlistBtn.setFocusPainted(false);
        wishlistBtn.setFont(new Font("Arial", Font.PLAIN, 12));
        wishlistBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JButton shareBtn = new JButton("📤 Share");
        shareBtn.setBackground(Color.WHITE);
        shareBtn.setForeground(Color.GRAY);
        shareBtn.setBorderPainted(false);
        shareBtn.setFocusPainted(false);
        shareBtn.setFont(new Font("Arial", Font.PLAIN, 12));
        shareBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // --- REVISI: ActionListener untuk Tombol Chat ---
        chatBtn.addActionListener(e -> {
            User currentUser = Authentication.getCurrentUser();
            if (currentUser == null) {
                JOptionPane.showMessageDialog(this, "Anda harus login untuk chat dengan penjual.", "Login Diperlukan", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Periksa apakah ini chat ke diri sendiri (penjual chat dengan dirinya sendiri)
            if (currentProduct != null) {
                try {
                    // Ambil sellerId dari currentProduct (asumsi sudah ada di FavoriteItem)
                    int sellerId = currentProduct.getSellerId(); // Ini memerlukan FavoriteItem di FavoritesUI.java memiliki getter getSellerId()

                    // Jika FavoriteItem belum punya getSellerId(), Anda harus pakai:
                    // int sellerId = ProductRepository.getSellerIdByProductId(currentProduct.getId());
                    // Perlu diperhatikan bahwa memanggil ProductRepository.getSellerIdByProductId() di sini
                    // akan melakukan query DB tambahan setiap kali tombol chat diklik.
                    // Lebih efisien jika sellerId sudah ada di objek currentProduct.

                    if (sellerId != -1) { // Pastikan sellerId valid
                        if (currentUser.getId() == sellerId) { // Jika pembeli adalah penjual produk ini
                            JOptionPane.showMessageDialog(this, "Anda tidak bisa chat dengan diri sendiri.", "Error Chat", JOptionPane.WARNING_MESSAGE);
                            return;
                        }
                        String sellerUsername = ProductRepository.getUsernameById(sellerId); //
                        if (!sellerUsername.equals("Unknown User") && viewController != null) {
                            // Panggil metode showChatWithSeller di ViewController
                            viewController.showChatWithSeller(sellerId, sellerUsername);
                        } else {
                            JOptionPane.showMessageDialog(this, "Informasi penjual tidak lengkap atau tidak ditemukan.", "Error Chat", JOptionPane.ERROR_MESSAGE);
                        }
                    } else {
                        JOptionPane.showMessageDialog(this, "Informasi penjual tidak tersedia untuk produk ini.", "Error Chat", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (SQLException ex) {
                    System.err.println("Error saat mencoba chat dengan penjual: " + ex.getMessage());
                    JOptionPane.showMessageDialog(this, "Terjadi kesalahan saat memulai chat. Periksa koneksi database Anda.", "Error Chat", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Produk tidak valid untuk memulai chat.", "Error Chat", JOptionPane.ERROR_MESSAGE);
            }
        });


        // --- REVISI: ActionListener untuk Tombol Wishlist ---
        wishlistBtn.addActionListener(e -> {
            User currentUser = Authentication.getCurrentUser();
            if (currentUser == null) {
                JOptionPane.showMessageDialog(this, "Anda harus login untuk menambahkan ke wishlist.", "Login Diperlukan", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int userId = currentUser.getId();
            int productId = currentProduct.getId();

            try {
                if (!isFavorite) {
                    // Tambah ke wishlist
                    boolean success = ProductRepository.addFavoriteItem(userId, productId);
                    if (success) {
                        isFavorite = true;
                        wishlistBtn.setText("❤️ Wishlist");
                        JOptionPane.showMessageDialog(this, "Ditambahkan ke wishlist!", "Wishlist", JOptionPane.INFORMATION_MESSAGE);
                        // Opsional: perbarui jumlah favorit di header dashboard jika ada metode
                        // ((UserDashboardUI) viewController).updateHeaderCartAndFavCounts();
                    } else {
                        JOptionPane.showMessageDialog(this, "Gagal menambahkan ke wishlist. Mungkin sudah ada atau ada error.", "Error Wishlist", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    // Hapus dari wishlist
                    boolean success = ProductRepository.removeFavoriteItem(userId, productId);
                    if (success) {
                        isFavorite = false;
                        wishlistBtn.setText("🤍 Wishlist");
                        JOptionPane.showMessageDialog(this, "Dihapus dari wishlist!", "Wishlist", JOptionPane.INFORMATION_MESSAGE);
                        // Opsional: perbarui jumlah favorit di header dashboard jika ada metode
                        // ((UserDashboardUI) viewController).updateHeaderCartAndFavCounts();
                    } else {
                        JOptionPane.showMessageDialog(this, "Gagal menghapus dari wishlist.", "Error Wishlist", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } catch (SQLException ex) {
                System.err.println("Error wishlist operation: " + ex.getMessage());
                JOptionPane.showMessageDialog(this, "Terjadi kesalahan saat memproses wishlist. Periksa koneksi database Anda.", "Error Wishlist", JOptionPane.ERROR_MESSAGE);
            }
        });

        additionalPanel.add(chatBtn);
        additionalPanel.add(wishlistBtn);
        additionalPanel.add(shareBtn);

        return additionalPanel;
    }

    private JPanel createTabsSection() {
        JPanel tabsSection = new JPanel();
        tabsSection.setLayout(new BoxLayout(tabsSection, BoxLayout.Y_AXIS));
        tabsSection.setBackground(Color.WHITE);
        tabsSection.setBorder(new EmptyBorder(30, 0, 0, 0));

        JPanel tabButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabButtonPanel.setBackground(Color.WHITE);

        String[] tabNames = {"Detail", "Spesifikasi", "Info Penting"};
        JButton[] tabButtons = new JButton[tabNames.length];

        for (int i = 0; i < tabNames.length; i++) {
            tabButtons[i] = new JButton(tabNames[i]);
            tabButtons[i].setBackground(Color.WHITE);
            tabButtons[i].setForeground(i == 0 ? new Color(255, 89, 0) : Color.GRAY);
            tabButtons[i].setBorderPainted(false);
            tabButtons[i].setFocusPainted(false);
            tabButtons[i].setFont(new Font("Arial", Font.BOLD, 14));
            tabButtons[i].setCursor(new Cursor(Cursor.HAND_CURSOR));
            tabButtons[i].setPreferredSize(new Dimension(120, 35));

            if (i == 0) {
                tabButtons[i].setBorder(BorderFactory.createMatteBorder(0, 0, 3, 0, new Color(255, 89, 0)));
            }

            final int tabIndex = i;
            final JButton[] allButtons = tabButtons;
            tabButtons[i].addActionListener(e -> {
                for (int j = 0; j < allButtons.length; j++) {
                    if (j == tabIndex) {
                        allButtons[j].setForeground(new Color(255, 89, 0));
                        allButtons[j].setBorder(BorderFactory.createMatteBorder(0, 0, 3, 0, new Color(255, 89, 0)));
                    } else {
                        allButtons[j].setForeground(Color.GRAY);
                        allButtons[j].setBorder(null);
                    }
                }
            });

            tabButtonPanel.add(tabButtons[i]);
        }

        JPanel tabContentPanel = new JPanel();
        tabContentPanel.setLayout(new BoxLayout(tabContentPanel, BoxLayout.Y_AXIS));
        tabContentPanel.setBackground(Color.WHITE);
        tabContentPanel.setBorder(new EmptyBorder(20, 0, 0, 0));

        JTextArea detailContent = new JTextArea();
        detailContent.setText("VERY WELCOME RESELLER / DEALER (DAPATKAN HARGA TERSPESIAL)\n" +
                              "KITA SALAH SATU SUPPLIER BARANG IT TERBESAR DI INDONESIA SEPERTI :\n" +
                              "* PC BUILT UP\n" +
                              "* SERVER\n" +
                              "* PC AIO\n" +
                              "* LAPTOP..");
        detailContent.setFont(new Font("Arial", Font.PLAIN, 14));
        detailContent.setForeground(Color.BLACK);
        detailContent.setBackground(Color.WHITE);
        detailContent.setEditable(false);
        detailContent.setOpaque(false);
        detailContent.setWrapStyleWord(true);
        detailContent.setLineWrap(true);

        JLabel selengkapnyaLabel = new JLabel("Lihat Selengkapnya");
        selengkapnyaLabel.setFont(new Font("Arial", Font.BOLD, 14));
        selengkapnyaLabel.setForeground(new Color(0, 150, 136));
        selengkapnyaLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        selengkapnyaLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        tabContentPanel.add(detailContent);
        tabContentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        tabContentPanel.add(selengkapnyaLabel);

        tabsSection.add(tabButtonPanel);
        tabsSection.add(tabContentPanel);

        return tabsSection;
    }

    // Metode untuk membuat bagian rekomendasi
    private JPanel createRecommendationSection() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(30, 20, 20, 20));

        JLabel titleLabel = new JLabel("Produk Serupa");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(Color.BLACK);
        panel.add(titleLabel, BorderLayout.NORTH);

        JPanel recommendedProductsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        recommendedProductsPanel.setBackground(Color.WHITE);

        // Ambil rekomendasi dari ProductRepository berdasarkan brand produk saat ini
        List<FavoritesUI.FavoriteItem> recommendations = ProductRepository.getProductsByBrand(
            currentProduct.getBrand(),
            currentProduct.getId()
        );

        if (recommendations.isEmpty()) {
            JLabel noRecLabel = new JLabel("Tidak ada produk serupa yang ditemukan.");
            noRecLabel.setFont(new Font("Arial", Font.PLAIN, 14));
            noRecLabel.setForeground(Color.GRAY);
            recommendedProductsPanel.add(noRecLabel);
        } else {
            for (FavoritesUI.FavoriteItem recProduct : recommendations) {
                JPanel recCard = createSimpleProductCardForRecommendation(recProduct);
                recommendedProductsPanel.add(recCard);
            }
        }
        panel.add(recommendedProductsPanel, BorderLayout.CENTER);

        return panel;
    }

    // Metode sederhana untuk membuat kartu produk rekomendasi
    private JPanel createSimpleProductCardForRecommendation(FavoritesUI.FavoriteItem item) {
        JPanel card = new JPanel();
        card.setLayout(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createLineBorder(new Color(240, 240, 240), 1));
        card.setPreferredSize(new Dimension(180, 250));

        JPanel imagePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.setColor(item.getBgColor());
                g2d.fillRect(0, 0, getWidth(), getHeight());

                Image mainImage = null;
                if (!item.getLoadedImages().isEmpty()) {
                    mainImage = item.getLoadedImages().get(0);
                }
                if (mainImage != null) {
                    int originalWidth = mainImage.getWidth(null);
                    int originalHeight = mainImage.getHeight(null);
                    double scale = Math.min((double) getWidth() / originalWidth, (double) getHeight() / originalHeight);
                    int scaledWidth = (int) (originalWidth * scale);
                    int scaledHeight = (int) (originalHeight * scale);
                    int x = (getWidth() - scaledWidth) / 2;
                    int y = (getHeight() - scaledHeight) / 2;
                    g2d.drawImage(mainImage, x, y, scaledWidth, scaledHeight, null);
                } else {
                     g2d.setColor(item.getBgColor().darker());
                     g2d.setFont(new Font("Arial", Font.BOLD, 16));
                     FontMetrics fm = g2d.getFontMetrics();
                     String text = "NO IMAGE";
                     int textWidth = fm.stringWidth(text);
                     int textHeight = fm.getHeight();
                     g2d.drawString(text, (getWidth() - textWidth) / 2, (getHeight() + textHeight) / 2);
                }
            }
        };
        imagePanel.setPreferredSize(new Dimension(180, 150));

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(Color.WHITE);
        infoPanel.setBorder(new EmptyBorder(8, 8, 8, 8));

        JLabel nameLabel = new JLabel("<html><div style='text-align: left;'>" + item.getName() + "</div></html>");
        nameLabel.setFont(new Font("Arial", Font.BOLD, 12));
        nameLabel.setForeground(Color.BLACK);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel priceLabel = new JLabel("<b>Rp " + String.format("%,.0f", item.getPrice()) + "</b>");
        priceLabel.setFont(new Font("Arial", Font.BOLD, 13));
        priceLabel.setForeground(new Color(255, 89, 0));
        priceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        infoPanel.add(nameLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        infoPanel.add(priceLabel);

        card.add(imagePanel, BorderLayout.NORTH);
        card.add(infoPanel, BorderLayout.CENTER);

        card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (viewController != null) {
                    viewController.showProductDetail(item);
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
}