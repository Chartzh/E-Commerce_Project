package e.commerce;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.List;
import java.util.ArrayList;
import java.sql.SQLException;
import e.commerce.Authentication;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.HashMap;

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

    private JScrollPane outerScrollPane;

    // --- Warna Tema dari Logo Quantra ---
    private static final Color QUANTRA_ORANGE_PRIMARY = new Color(255, 102, 0); // #FF6600
    private static final Color QUANTRA_ORANGE_LIGHT = new Color(255, 153, 51); // Lebih terang
    private static final Color QUANTRA_BLUE_SECONDARY = new Color(74, 110, 255); // Biru (dari tombol Detail/lainnya)
    private static final Color TEXT_DARK = new Color(51, 51, 51);
    private static final Color TEXT_GRAY = new Color(102, 102, 102);
    private static final Color STAR_GOLD = new Color(255, 193, 7); // Emas untuk bintang rating
    private static final Color STAR_GRAY_BACKGROUND = new Color(200, 200, 200); // Abu-abu untuk bintang non-aktif
    private static final Color GRAY_BACKGROUND_LIGHT = new Color(248, 248, 248);


    // Konstruktor utama yang menerima produk dan ViewController
    public ProductDetailUI(FavoritesUI.FavoriteItem product, ViewController viewController) {
        this.currentProduct = product;
        this.viewController = viewController;

        // Periksa status favorit saat inisialisasi (di awal konstruktor)
        if (Authentication.getCurrentUser() != null) {
            try {
                isFavorite = ProductRepository.isProductInFavorites(Authentication.getCurrentUser().getId(), currentProduct.getId());
            } catch (SQLException e) {
                System.err.println("Error checking favorite status: " + e.getMessage());
                isFavorite = false;
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

        JPanel contentWrapperPanel = new JPanel();
        contentWrapperPanel.setLayout(new BoxLayout(contentWrapperPanel, BoxLayout.Y_AXIS));
        contentWrapperPanel.setBackground(Color.WHITE);

        JPanel mainProductInfoSection = new JPanel(new BorderLayout());
        mainProductInfoSection.setBackground(Color.WHITE);
        mainProductInfoSection.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel imageSection = createImageSection();
        mainProductInfoSection.add(imageSection, BorderLayout.WEST);

        JPanel detailSection = createDetailSection();
        mainProductInfoSection.add(detailSection, BorderLayout.CENTER);

        contentWrapperPanel.add(mainProductInfoSection);

        // --- START NEW: Review Section ---
        JPanel reviewSection = createReviewSection();
        contentWrapperPanel.add(reviewSection);
        // --- END NEW: Review Section ---


        JPanel recommendationSection = createRecommendationSection();
        contentWrapperPanel.add(recommendationSection);

        outerScrollPane = new JScrollPane(contentWrapperPanel); // ScrollPane untuk seluruh konten
        outerScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        outerScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        outerScrollPane.setBorder(null);
        outerScrollPane.getVerticalScrollBar().setUnitIncrement(16); // Perbaiki kecepatan scroll

        add(outerScrollPane, BorderLayout.CENTER);

        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                SwingUtilities.invokeLater(() -> {
                    if (outerScrollPane != null) {
                        outerScrollPane.getVerticalScrollBar().setValue(0); // Scroll ke atas saat panel ditampilkan
                    }
                });
            }
        });
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(new EmptyBorder(10, 20, 0, 20));

        JButton backButton = new JButton("← Kembali"); // Ubah teks tombol
        backButton.setFont(new Font("Arial", Font.BOLD, 14));
        backButton.setForeground(TEXT_DARK); // Gunakan warna tema
        backButton.setBackground(Color.WHITE);
        backButton.setBorderPainted(false);
        backButton.setFocusPainted(false);
        backButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backButton.addActionListener(e -> {
            if (viewController != null) {
                viewController.showDashboardView(); // Kembali ke Dashboard
            }
        });
        headerPanel.add(backButton, BorderLayout.WEST);

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

                int panelWidth = getWidth();
                int panelHeight = getHeight();

                int imageAreaSize = Math.min(panelWidth, panelHeight);
                int imageAreaX = (panelWidth - imageAreaSize) / 2;
                int imageAreaY = (panelHeight - imageAreaSize) / 2;

                g2d.setColor(currentProduct.getBgColor()); // Latar belakang dari produk
                g2d.fillRect(0, 0, panelWidth, panelHeight);

                Image displayImage = null;
                if (currentProduct.getLoadedImages() != null &&
                    !currentProduct.getLoadedImages().isEmpty() &&
                    currentImageIndex < currentProduct.getLoadedImages().size()) {
                    displayImage = currentProduct.getLoadedImages().get(currentImageIndex);
                }

                if (displayImage == null) {
                    g2d.setColor(currentProduct.getBgColor().darker());
                    g2d.setFont(new Font("Arial", Font.BOLD, 24));
                    FontMetrics fm = g2d.getFontMetrics();
                    String text = "NO IMAGE";
                    int textWidth = fm.stringWidth(text);
                    int textHeight = fm.getHeight();
                    g2d.drawString(text, (panelWidth - textWidth) / 2, (panelHeight + textHeight / 2) / 2);
                } else {
                    int originalWidth = displayImage.getWidth(null);
                    int originalHeight = displayImage.getHeight(null);

                    // Skala gambar agar pas di area, tapi tetap menjaga aspek rasio
                    double scale = Math.min((double) imageAreaSize / originalWidth, (double) imageAreaSize / originalHeight);

                    int scaledWidth = (int) (originalWidth * scale);
                    int scaledHeight = (int) (originalHeight * scale);

                    int drawX = imageAreaX + (imageAreaSize - scaledWidth) / 2;
                    int drawY = imageAreaY + (imageAreaSize - scaledHeight) / 2;

                    // Menggambar gambar di tengah area persegi
                    g2d.drawImage(displayImage, drawX, drawY, scaledWidth, scaledHeight, null);
                }
            }
        };
        mainImagePanel.setPreferredSize(new Dimension(400, 350));
        mainImagePanel.setBorder(BorderFactory.createLineBorder(new Color(240, 240, 240), 0));
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
            // Tampilkan setidaknya 3 placeholder thumbnail jika tidak ada gambar
            for (int i = 0; i < 3; i++) {
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

                Image thumbImage = null;
                // Ambil gambar dari list, pastikan index tidak out of bounds
                if (currentProduct.getLoadedImages() != null && index < currentProduct.getLoadedImages().size()) {
                    thumbImage = currentProduct.getLoadedImages().get(index);
                }

                int panelWidth = getWidth();
                int panelHeight = getHeight();

                int imageAreaSize = Math.min(panelWidth, panelHeight);
                int imageAreaX = (panelWidth - imageAreaSize) / 2;
                int imageAreaY = (panelHeight - imageAreaSize) / 2;
                
                g2d.setColor(currentProduct.getBgColor());
                g2d.fillRect(0, 0, panelWidth, panelHeight); // Gambar latar belakang warna produk

                if (thumbImage == null) {
                    // Placeholder jika gambar tidak ada
                    g2d.setColor(currentProduct.getBgColor().darker());
                    g2d.setFont(new Font("Arial", Font.PLAIN, 10));
                    FontMetrics fm = g2d.getFontMetrics();
                    String text = "" + (index + 1); // Teks nomor thumbnail
                    int textWidth = fm.stringWidth(text);
                    int textHeight = fm.getHeight();
                    g2d.drawString(text, (panelWidth - textWidth) / 2, (panelHeight + textHeight / 2) / 2);
                } else {
                    int originalWidth = thumbImage.getWidth(null);
                    int originalHeight = thumbImage.getHeight(null);

                    // Skala gambar agar pas di area thumbnail
                    double scale = Math.min((double) imageAreaSize / originalWidth, (double) imageAreaSize / originalHeight);

                    int scaledWidth = (int) (originalWidth * scale);
                    int scaledHeight = (int) (originalHeight * scale);

                    int drawX = imageAreaX + (imageAreaSize - scaledWidth) / 2;
                    int drawY = imageAreaY + (imageAreaSize - scaledHeight) / 2;

                    // Gambar di area clipping
                    g2d.drawImage(thumbImage, drawX, drawY, scaledWidth, scaledHeight, null);
                }
            }
        };
        thumbnail.setPreferredSize(new Dimension(70, 60)); // Ukuran thumbnail
        // Border thumbnail yang dipilih
        thumbnail.setBorder(BorderFactory.createLineBorder(
                index == currentImageIndex ? QUANTRA_ORANGE_PRIMARY : new Color(240, 240, 240), // Warna border sesuai tema
                index == currentImageIndex ? 2 : 0
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
                    i == currentImageIndex ? QUANTRA_ORANGE_PRIMARY : new Color(240, 240, 240), // Warna border sesuai tema
                    i == currentImageIndex ? 2 : 0
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

        JLabel titleLabel = new JLabel("<html>" + currentProduct.getName() + "<br><span style='font-weight:normal; color:" + String.format("#%06X", (TEXT_GRAY.getRGB() & 0xFFFFFF)) + ";'>" + currentProduct.getDescription() + "</span></html>");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(TEXT_DARK); // Warna tema
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
        currencyFormat.setMinimumFractionDigits(0);

        JLabel priceLabel = new JLabel(currencyFormat.format(currentProduct.getPrice()));
        priceLabel.setFont(new Font("Arial", Font.BOLD, 28));
        priceLabel.setForeground(QUANTRA_ORANGE_PRIMARY); // Warna tema
        priceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel originalPriceLabel = null;
        if (currentProduct.getOriginalPrice() > currentProduct.getPrice()) {
            originalPriceLabel = new JLabel("<html><strike>" + currencyFormat.format(currentProduct.getOriginalPrice()) + "</strike></html>");
            originalPriceLabel.setFont(new Font("Arial", Font.PLAIN, 16));
            originalPriceLabel.setForeground(TEXT_GRAY); // Warna tema
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
        detailPanel.add(Box.createVerticalGlue());

        return detailPanel;
    }

    private void addInfoRow(JPanel parent, String label, String value) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        row.setBackground(Color.WHITE);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));

        JLabel labelComp = new JLabel(label);
        labelComp.setFont(new Font("Arial", Font.PLAIN, 14));
        labelComp.setForeground(TEXT_GRAY); // Warna tema
        labelComp.setPreferredSize(new Dimension(120, 20));

        JLabel valueComp = new JLabel(value);
        valueComp.setFont(new Font("Arial", Font.BOLD, 14));
        // Jika brand, gunakan warna sekunder dari tema
        valueComp.setForeground(value.equals(currentProduct.getBrand()) ? QUANTRA_BLUE_SECONDARY : TEXT_DARK);

        row.add(labelComp);
        row.add(valueComp);
        parent.add(row);
    }

    private JPanel createQuantityPanel() {
        JPanel quantityPanel = new JPanel(new GridBagLayout());
        quantityPanel.setBackground(Color.WHITE);
        quantityPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        quantityPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.anchor = GridBagConstraints.WEST;

        // Label "Jumlah"
        JLabel qtyLabel = new JLabel("Jumlah");
        qtyLabel.setFont(new Font("Arial", Font.BOLD, 14));
        qtyLabel.setForeground(TEXT_DARK); // Warna tema
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        quantityPanel.add(qtyLabel, gbc);

        // Panel Kontrol Kuantitas (Spinner + Stock Label)
        JPanel qtyControlSubPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        qtyControlSubPanel.setBackground(Color.WHITE);

        quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, currentProduct.getStock(), 1));
        quantitySpinner.setPreferredSize(new Dimension(80, 28)); 
        quantitySpinner.setFont(new Font("Arial", Font.PLAIN, 14));

        stockLabel = new JLabel("Stok Total: Sisa " + currentProduct.getStock());
        stockLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        stockLabel.setForeground(TEXT_GRAY); // Warna tema

        quantitySpinner.addChangeListener(e -> updateSubtotal());

        qtyControlSubPanel.add(quantitySpinner);
        qtyControlSubPanel.add(stockLabel);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        quantityPanel.add(qtyControlSubPanel, gbc);

        gbc.insets = new Insets(8, 0, 0, 0); 
        // Label "Subtotal" dan nilainya
        JLabel subtotalTextLabel = new JLabel("Subtotal");
        subtotalTextLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        subtotalTextLabel.setForeground(TEXT_GRAY); // Warna tema
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        quantityPanel.add(subtotalTextLabel, gbc);

        subtotalValueLabel = new JLabel();
        subtotalValueLabel.setFont(new Font("Arial", Font.BOLD, 16));
        subtotalValueLabel.setForeground(TEXT_DARK); // Warna tema
        updateSubtotal();
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(8, 5, 0, 0);
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST; 
        quantityPanel.add(subtotalValueLabel, gbc);

        return quantityPanel;
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
        addToCartBtn.setForeground(QUANTRA_ORANGE_PRIMARY); // Warna tema
        addToCartBtn.setBorder(BorderFactory.createLineBorder(QUANTRA_ORANGE_PRIMARY, 2)); // Warna tema
        addToCartBtn.setFont(new Font("Arial", Font.BOLD, 14));
        addToCartBtn.setFocusPainted(false);
        addToCartBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        buyNowBtn = new JButton("Beli Langsung");
        buyNowBtn.setPreferredSize(new Dimension(140, 40));
        buyNowBtn.setBackground(QUANTRA_ORANGE_PRIMARY); // Warna tema
        buyNowBtn.setForeground(Color.WHITE);
        buyNowBtn.setBorderPainted(false);
        buyNowBtn.setFont(new Font("Arial", Font.BOLD, 14));
        buyNowBtn.setFocusPainted(false);
        buyNowBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

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

                if (viewController instanceof UserDashboardUI) {
                    ((UserDashboardUI) viewController).updateHeaderCartAndFavCounts();
                }

            } catch (SQLException ex) {
                System.err.println("Error menambahkan produk ke keranjang: " + ex.getMessage());
                JOptionPane.showMessageDialog(this, "Gagal menambahkan produk ke keranjang. Silakan coba lagi.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        buyNowBtn.addActionListener(e -> {
            User currentUser = Authentication.getCurrentUser();
            if (currentUser == null) {
                JOptionPane.showMessageDialog(this, "Anda harus login untuk melakukan pembelian.", "Login Diperlukan", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int qty = (Integer) quantitySpinner.getValue();
            double total = currentProduct.getPrice() * qty;
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
            currencyFormat.setMinimumFractionDigits(0);

            try {
                ProductRepository.addProductToCart(currentUser.getId(), currentProduct.getId(), qty);
                JOptionPane.showMessageDialog(this,
                    qty + " item(s) berhasil ditambahkan ke keranjang. Melanjutkan ke halaman alamat...",
                    "Beli Langsung",
                    JOptionPane.INFORMATION_MESSAGE);

                if (viewController != null) {
                    viewController.showAddressView();
                }

            } catch (SQLException ex) {
                System.err.println("Error menambahkan produk ke keranjang untuk Beli Langsung: " + ex.getMessage());
                JOptionPane.showMessageDialog(this, "Gagal memproses pembelian. Silakan coba lagi.", "Error", JOptionPane.ERROR_MESSAGE);
            }
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

        ImageIcon chatIcon = new ImageIcon(getClass().getResource("/Resources/Images/chat_icon.png"));
        Image originalChatImage = chatIcon.getImage();
        JButton chatBtn;
        if (originalChatImage == null || chatIcon.getIconWidth() <= 0) {
            chatBtn = new JButton("💬 Chat");
            chatBtn.setPreferredSize(new Dimension(90, 28)); // Menambah tinggi dan lebar
        } else {
            Image resizedChatImage = originalChatImage.getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            chatIcon = new ImageIcon(resizedChatImage);
            chatBtn = new JButton("Chat");
            chatBtn.setIcon(chatIcon);
            chatBtn.setHorizontalTextPosition(SwingConstants.RIGHT);
            chatBtn.setPreferredSize(new Dimension(80, 28)); // Menambah tinggi dan lebar
        }
        chatBtn.setBackground(Color.WHITE);
        chatBtn.setForeground(TEXT_GRAY); // Warna tema
        chatBtn.setBorderPainted(false);
        chatBtn.setFocusPainted(false);
        chatBtn.setFont(new Font("Arial", Font.PLAIN, 12));
        chatBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        ImageIcon favIcon = new ImageIcon(getClass().getResource("/Resources/Images/fav_icon.png"));
        Image originalFavImage = favIcon.getImage();
        if (originalFavImage == null || favIcon.getIconWidth() <= 0) {
            favIcon = null;
        } else {
            Image resizedFavImage = originalFavImage.getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            favIcon = new ImageIcon(resizedFavImage);
        }
        
        if (favIcon != null) {
            wishlistBtn = new JButton("Favorit");
            wishlistBtn.setIcon(favIcon);
            wishlistBtn.setHorizontalTextPosition(SwingConstants.RIGHT);
            wishlistBtn.setPreferredSize(new Dimension(100, 28)); // Menambah tinggi dan lebar
        } else {
            // Gunakan karakter Unicode untuk hati jika ikon tidak dimuat
            wishlistBtn = new JButton((isFavorite ? "️" : "") + " Favorit");
            wishlistBtn.setPreferredSize(new Dimension(110, 28)); // Menambah tinggi dan lebar
        }
        wishlistBtn.setBackground(Color.WHITE);
        wishlistBtn.setForeground(TEXT_GRAY); // Warna tema
        wishlistBtn.setBorderPainted(false);
        wishlistBtn.setFocusPainted(false);
        wishlistBtn.setFont(new Font("Arial", Font.PLAIN, 12));
        wishlistBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        chatBtn.addActionListener(e -> {
            User currentUser = Authentication.getCurrentUser();
            if (currentUser == null) {
                JOptionPane.showMessageDialog(this, "Anda harus login untuk chat dengan penjual.", "Login Diperlukan", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (currentProduct != null) {
                try {
                    int sellerId = currentProduct.getSellerId();

                    if (sellerId != -1) {
                        if (currentUser.getId() == sellerId) {
                            JOptionPane.showMessageDialog(this, "Anda tidak bisa chat dengan diri sendiri.", "Error Chat", JOptionPane.WARNING_MESSAGE);
                            return;
                        }
                        String sellerUsername = ProductRepository.getUsernameById(sellerId);
                        if (!sellerUsername.equals("Unknown User") && viewController != null) {
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
                    boolean success = ProductRepository.addFavoriteItem(userId, productId);
                    if (success) {
                        isFavorite = true;
                        JOptionPane.showMessageDialog(this, "Ditambahkan ke favorit!", "Favorit", JOptionPane.INFORMATION_MESSAGE);
                        if (viewController instanceof UserDashboardUI) {
                            ((UserDashboardUI) viewController).updateHeaderCartAndFavCounts();
                        }
                    } else {
                        JOptionPane.showMessageDialog(this, "Gagal menambahkan ke favorit. Mungkin sudah ada atau ada error.", "Error Favorit", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    boolean success = ProductRepository.removeFavoriteItem(userId, productId);
                    if (success) {
                        isFavorite = false;
                        JOptionPane.showMessageDialog(this, "Dihapus dari favorit!", "Favorit", JOptionPane.INFORMATION_MESSAGE);
                        if (viewController instanceof UserDashboardUI) {
                            ((UserDashboardUI) viewController).updateHeaderCartAndFavCounts();
                        }
                    } else {
                        JOptionPane.showMessageDialog(this, "Gagal menghapus dari favorit.", "Error favorit", JOptionPane.ERROR_MESSAGE);
                    }
                }
                // Perbarui tampilan tombol setelah aksi favorit
                updateWishlistButtonText(); // Memastikan teks tombol diperbarui
            } catch (SQLException ex) {
                System.err.println("Error wishlist operation: " + ex.getMessage());
                JOptionPane.showMessageDialog(this, "Terjadi kesalahan saat memproses wishlist. Periksa koneksi database Anda.", "Error Wishlist", JOptionPane.ERROR_MESSAGE);
            }
        });
        updateWishlistButtonText(); // Set teks awal tombol wishlist

        additionalPanel.add(chatBtn);
        additionalPanel.add(wishlistBtn);
        return additionalPanel;
    }

    // Metode baru untuk memperbarui teks/ikon tombol wishlist
    private void updateWishlistButtonText() {
        if (wishlistBtn != null) {
            if (isFavorite) {
                wishlistBtn.setText("Favorit");
                wishlistBtn.setForeground(new Color(220, 53, 69)); // Merah untuk favorit
            } else {
                wishlistBtn.setText("Favorit");
                wishlistBtn.setForeground(TEXT_GRAY);
            }
        }
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
            tabButtons[i].setForeground(i == 0 ? QUANTRA_ORANGE_PRIMARY : TEXT_GRAY); // Warna tema
            tabButtons[i].setBorderPainted(false);
            tabButtons[i].setFocusPainted(false);
            tabButtons[i].setFont(new Font("Arial", Font.BOLD, 14));
            tabButtons[i].setCursor(new Cursor(Cursor.HAND_CURSOR));
            tabButtons[i].setPreferredSize(new Dimension(120, 35));

            if (i == 0) {
                tabButtons[i].setBorder(BorderFactory.createMatteBorder(0, 0, 3, 0, QUANTRA_ORANGE_PRIMARY)); // Warna tema
            }

            final int tabIndex = i;
            final JButton[] allButtons = tabButtons;
            tabButtons[i].addActionListener(e -> {
                for (int j = 0; j < allButtons.length; j++) {
                    if (j == tabIndex) {
                        allButtons[j].setForeground(QUANTRA_ORANGE_PRIMARY); // Warna tema
                        allButtons[j].setBorder(BorderFactory.createMatteBorder(0, 0, 3, 0, QUANTRA_ORANGE_PRIMARY)); // Warna tema
                    } else {
                        allButtons[j].setForeground(TEXT_GRAY); // Warna tema
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
        detailContent.setForeground(TEXT_DARK); // Warna tema
        detailContent.setBackground(Color.WHITE);
        detailContent.setEditable(false);
        detailContent.setOpaque(false);
        detailContent.setWrapStyleWord(true);
        detailContent.setLineWrap(true);

        JLabel selengkapnyaLabel = new JLabel("Lihat Selengkapnya");
        selengkapnyaLabel.setFont(new Font("Arial", Font.BOLD, 14));
        selengkapnyaLabel.setForeground(QUANTRA_BLUE_SECONDARY); // Warna tema
        selengkapnyaLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        selengkapnyaLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        tabContentPanel.add(detailContent);
        tabContentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        tabContentPanel.add(selengkapnyaLabel);

        tabsSection.add(tabButtonPanel);
        tabsSection.add(tabContentPanel);

        return tabsSection;
    }

    // --- START NEW: createReviewSection() ---
    private JPanel createReviewSection() {
        JPanel reviewSectionPanel = new JPanel(new BorderLayout());
        reviewSectionPanel.setBackground(Color.WHITE);
        reviewSectionPanel.setBorder(new EmptyBorder(30, 20, 30, 20)); // Padding di sekitar bagian review

        JLabel sectionTitle = new JLabel("Our Customer Reviews");
        sectionTitle.setFont(new Font("Arial", Font.BOLD, 24));
        sectionTitle.setForeground(TEXT_DARK);
        reviewSectionPanel.add(sectionTitle, BorderLayout.NORTH);

        JPanel reviewContent = new JPanel();
        reviewContent.setLayout(new BoxLayout(reviewContent, BoxLayout.Y_AXIS));
        reviewContent.setBackground(Color.WHITE);
        reviewContent.setBorder(new EmptyBorder(20, 0, 0, 0)); // Padding antara judul section dan konten review

        // Panel Ringkasan Rating (Kiri: Rata-rata, Kanan: Progress Bar)
        JPanel summaryPanel = new JPanel(new BorderLayout(20, 0));
        summaryPanel.setBackground(GRAY_BACKGROUND_LIGHT); // Latar belakang abu-abu muda
        summaryPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        summaryPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel averageRatingPanel = new JPanel();
        averageRatingPanel.setLayout(new BoxLayout(averageRatingPanel, BoxLayout.Y_AXIS));
        averageRatingPanel.setBackground(GRAY_BACKGROUND_LIGHT);
        averageRatingPanel.setAlignmentX(Component.CENTER_ALIGNMENT); // Pusatkan konten di panel ini

        JLabel avgRatingValueLabel = new JLabel("N/A");
        avgRatingValueLabel.setFont(new Font("Arial", Font.BOLD, 48));
        avgRatingValueLabel.setForeground(TEXT_DARK);
        avgRatingValueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel avgStarsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
        avgStarsPanel.setBackground(GRAY_BACKGROUND_LIGHT);
        
        JLabel totalRatingsLabel = new JLabel("0 Ratings");
        totalRatingsLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        totalRatingsLabel.setForeground(TEXT_GRAY);
        totalRatingsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        averageRatingPanel.add(avgRatingValueLabel);
        averageRatingPanel.add(avgStarsPanel);
        averageRatingPanel.add(totalRatingsLabel);
        summaryPanel.add(averageRatingPanel, BorderLayout.WEST);

        JPanel progressBarPanel = new JPanel();
        progressBarPanel.setLayout(new BoxLayout(progressBarPanel, BoxLayout.Y_AXIS));
        progressBarPanel.setBackground(GRAY_BACKGROUND_LIGHT);
        progressBarPanel.setBorder(new EmptyBorder(0, 10, 0, 0)); // Padding kiri untuk progress bar
        summaryPanel.add(progressBarPanel, BorderLayout.CENTER);

        // Panel Daftar Review
        JPanel reviewListContainer = new JPanel();
        reviewListContainer.setLayout(new BoxLayout(reviewListContainer, BoxLayout.Y_AXIS));
        reviewListContainer.setBackground(Color.WHITE);
        reviewListContainer.setBorder(new EmptyBorder(20, 0, 0, 0)); // Padding atas untuk daftar review

        JScrollPane reviewScrollPane = new JScrollPane(reviewListContainer);
        reviewScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        reviewScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        reviewScrollPane.setBorder(BorderFactory.createEmptyBorder()); // Hilangkan border default scroll pane
        reviewScrollPane.getVerticalScrollBar().setUnitIncrement(16);


        // Muat data review di latar belakang
        new SwingWorker<List<ProductRepository.Review>, Void>() {
            double averageRating = 0.0;
            int reviewCount = 0;
            Map<Integer, Integer> ratingSummary = new HashMap<>(); 

            @Override
            protected List<ProductRepository.Review> doInBackground() throws Exception {
                // Ambil rata-rata dan total count
                double[] avgAndCount = ProductRepository.getProductAverageRatingAndCount(currentProduct.getId());
                averageRating = avgAndCount[0];
                reviewCount = (int) avgAndCount[1];

                // Ambil summary per bintang
                ratingSummary = ProductRepository.getProductRatingSummary(currentProduct.getId());

                // Ambil daftar review
                return ProductRepository.getReviewsForProduct(currentProduct.getId());
            }

            @Override
            protected void done() {
                try {
                    // Update Summary Panel
                    avgRatingValueLabel.setText(String.format("%.1f", averageRating));
                    totalRatingsLabel.setText(reviewCount + " Ratings");
                    
                    // Update bintang rata-rata
                    avgStarsPanel.removeAll();
                    for (int i = 1; i <= 5; i++) {
                        JLabel star = new JLabel("★");
                        // Perbaikan Font Bintang
                        star.setFont(new Font("Dialog", Font.BOLD, 24)); // Gunakan "Dialog" atau "Segoe UI Emoji"
                        star.setForeground(i <= Math.round(averageRating) ? STAR_GOLD : STAR_GRAY_BACKGROUND);
                        avgStarsPanel.add(star);
                    }
                    avgStarsPanel.revalidate();
                    avgStarsPanel.repaint();

                    // Update Progress Bar Panel
                    progressBarPanel.removeAll();
                    for (int i = 5; i >= 1; i--) { // Dari 5 bintang ke 1 bintang
                        int count = ratingSummary.getOrDefault(i, 0);
                        progressBarPanel.add(createRatingProgressBar(i, count, reviewCount));
                        progressBarPanel.add(Box.createVerticalStrut(5)); // Spasi antar bar
                    }
                    progressBarPanel.revalidate();
                    progressBarPanel.repaint();

                    // Tambahkan daftar review
                    List<ProductRepository.Review> reviews = get();
                    if (reviews.isEmpty()) {
                        JLabel noReviewsLabel = new JLabel("Belum ada review untuk produk ini.");
                        noReviewsLabel.setFont(new Font("Arial", Font.ITALIC, 14));
                        noReviewsLabel.setForeground(TEXT_GRAY);
                        noReviewsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                        reviewListContainer.add(noReviewsLabel);
                    } else {
                        for (ProductRepository.Review review : reviews) {
                            reviewListContainer.add(createReviewCard(review));
                            reviewListContainer.add(Box.createVerticalStrut(15)); // Spasi antar review
                        }
                    }
                    reviewListContainer.revalidate();
                    reviewListContainer.repaint();

                } catch (Exception e) {
                    System.err.println("Error displaying reviews: " + e.getMessage());
                    e.printStackTrace();
                    reviewListContainer.removeAll();
                    reviewListContainer.add(new JLabel("Gagal memuat review: " + e.getMessage()));
                    reviewListContainer.revalidate();
                    reviewListContainer.repaint();
                }
            }
        }.execute();

        reviewContent.add(summaryPanel);
        reviewContent.add(reviewScrollPane); // Tambahkan scroll pane yang berisi daftar review
        
        reviewSectionPanel.add(reviewContent, BorderLayout.CENTER);

        return reviewSectionPanel;
    }

    // Helper method untuk membuat progress bar rating
    private JPanel createRatingProgressBar(int rating, int count, int totalReviews) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        panel.setBackground(GRAY_BACKGROUND_LIGHT);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setPreferredSize(new Dimension(300, 20)); // Beri ukuran tetap untuk konsistensi

        JLabel ratingLabel = new JLabel(rating + ".0");
        ratingLabel.setFont(new Font("Arial", Font.BOLD, 12));
        ratingLabel.setForeground(TEXT_DARK);
        panel.add(ratingLabel);

        JLabel starIcon = new JLabel("★");
        // Perbaikan Font Bintang
        starIcon.setFont(new Font("Dialog", Font.BOLD, 12)); // Gunakan "Dialog" atau "Segoe UI Emoji"
        starIcon.setForeground(STAR_GOLD);
        panel.add(starIcon);

        JProgressBar progressBar = new JProgressBar(0, totalReviews > 0 ? totalReviews : 1);
        progressBar.setValue(count);
        progressBar.setForeground(STAR_GOLD);
        progressBar.setBackground(new Color(230, 230, 230));
        progressBar.setPreferredSize(new Dimension(150, 10)); // Ukuran progress bar
        progressBar.setBorderPainted(false);
        panel.add(progressBar);

        JLabel countLabel = new JLabel(count + " Reviews");
        countLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        countLabel.setForeground(TEXT_GRAY);
        panel.add(countLabel);

        return panel;
    }

    // Helper method untuk membuat kartu review individual
    private JPanel createReviewCard(ProductRepository.Review review) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230), 1));
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        card.setAlignmentX(Component.LEFT_ALIGNMENT); // Penting agar mengisi lebar di BoxLayout Y_AXIS

        // Header Review (Nama, Bintang, Tanggal)
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);

        // Perbaikan: Gunakan JPanel untuk menampung nama pengguna dan avatar
        JPanel userInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        userInfoPanel.setBackground(Color.WHITE);

        // --- Tambahkan placeholder avatar atau muat gambar profil jika ada ---
        JLabel avatarLabel = new JLabel("U"); // Inisial atau ikon default
        avatarLabel.setPreferredSize(new Dimension(30, 30));
        avatarLabel.setOpaque(true);
        avatarLabel.setBackground(QUANTRA_ORANGE_LIGHT); // Warna latar belakang avatar
        avatarLabel.setForeground(Color.WHITE);
        avatarLabel.setFont(new Font("Arial", Font.BOLD, 14));
        avatarLabel.setHorizontalAlignment(SwingConstants.CENTER);
        avatarLabel.setVerticalAlignment(SwingConstants.CENTER);
        avatarLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
        // Jika Anda memiliki gambar profil pengguna, Anda bisa memuatnya di sini
        // Misalnya: avatarLabel.setIcon(new ImageIcon(ProductRepository.getProfilePicture(review.getUserId()).getScaledInstance(30,30,Image.SCALE_SMOOTH)));
        
        userInfoPanel.add(avatarLabel);
        // --- Akhir placeholder avatar ---

        JLabel userNameLabel = new JLabel(review.getUserName());
        userNameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        userNameLabel.setForeground(TEXT_DARK);
        userInfoPanel.add(userNameLabel);

        header.add(userInfoPanel, BorderLayout.WEST);

        // Bintang Review
        JPanel starsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        starsPanel.setBackground(Color.WHITE);
        for (int i = 1; i <= 5; i++) {
            JLabel star = new JLabel("★");
            // Perbaikan Font Bintang
            star.setFont(new Font("Dialog", Font.PLAIN, 16)); // Gunakan "Dialog" atau "Segoe UI Emoji"
            star.setForeground(i <= review.getRating() ? STAR_GOLD : STAR_GRAY_BACKGROUND);
            starsPanel.add(star);
        }
        header.add(starsPanel, BorderLayout.CENTER);

        JLabel dateLabel = new JLabel(review.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))); // Format tahun penuh
        dateLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        dateLabel.setForeground(TEXT_GRAY);
        header.add(dateLabel, BorderLayout.EAST);

        card.add(header, BorderLayout.NORTH);

        // Teks Review
        JTextArea reviewTextLabel = new JTextArea(review.getReviewText());
        reviewTextLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        reviewTextLabel.setForeground(TEXT_DARK);
        reviewTextLabel.setBackground(Color.WHITE);
        reviewTextLabel.setEditable(false);
        reviewTextLabel.setLineWrap(true);
        reviewTextLabel.setWrapStyleWord(true);
        reviewTextLabel.setOpaque(false);
        reviewTextLabel.setBorder(new EmptyBorder(10, 0, 0, 0)); // Padding atas untuk teks
        card.add(reviewTextLabel, BorderLayout.CENTER);

        // Gambar Testimoni (jika ada)
        if (review.getLoadedReviewImage() != null) {
            JPanel imagePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
            imagePanel.setBackground(Color.WHITE);
            imagePanel.setBorder(new EmptyBorder(10, 0, 0, 0)); // Padding atas untuk gambar

            JLabel reviewImageLabel = new JLabel();
            Image originalImage = review.getLoadedReviewImage();
            if (originalImage != null) {
                // Skala gambar agar sesuai (misal: maks 100x100 piksel)
                // Sesuaikan ukuran ini jika Anda ingin gambar testimoni lebih besar/kecil
                int targetSize = 100;
                Image scaledReviewImage = originalImage.getScaledInstance(targetSize, targetSize, Image.SCALE_SMOOTH);
                reviewImageLabel.setIcon(new ImageIcon(scaledReviewImage));
            } else {
                reviewImageLabel.setText("Gambar Gagal Muat");
                reviewImageLabel.setForeground(Color.RED);
            }
            imagePanel.add(reviewImageLabel);
            card.add(imagePanel, BorderLayout.SOUTH); // Letakkan di bawah teks review
        }


        return card;
    }


    private JPanel createRecommendationSection() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(30, 20, 20, 20));

        JLabel titleLabel = new JLabel("Produk Serupa");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(TEXT_DARK); // Warna tema
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
            noRecLabel.setForeground(TEXT_GRAY); // Warna tema
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
                
                int panelWidth = getWidth();
                int panelHeight = getHeight();

                g2d.setColor(item.getBgColor());
                g2d.fillRect(0, 0, panelWidth, panelHeight);

                Image mainImage = null;
                if (!item.getLoadedImages().isEmpty()) {
                    mainImage = item.getLoadedImages().get(0);
                }
                if (mainImage == null) {
                     g2d.setColor(item.getBgColor().darker());
                     g2d.setFont(new Font("Arial", Font.BOLD, 16));
                     FontMetrics fm = g2d.getFontMetrics();
                     String text = "NO IMAGE";
                     int textWidth = fm.stringWidth(text);
                     int textHeight = fm.getHeight();
                     g2d.drawString(text, (panelWidth - textWidth) / 2, (panelHeight + textHeight) / 2);
                } else {
                    int originalWidth = mainImage.getWidth(null);
                    int originalHeight = mainImage.getHeight(null);

                    double scaleX = (double) panelWidth / originalWidth;
                    double scaleY = (double) panelHeight / originalHeight;
                    double scale = Math.max(scaleX, scaleY); 

                    int scaledWidth = (int) (originalWidth * scale);
                    int scaledHeight = (int) (originalHeight * scale);

                    int drawX = (panelWidth - scaledWidth) / 2;
                    int drawY = (panelHeight - scaledHeight) / 2;

                    g2d.drawImage(mainImage, drawX, drawY, scaledWidth, scaledHeight, null);
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
        nameLabel.setForeground(TEXT_DARK); // Warna tema
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel priceLabel = new JLabel("Rp " + String.format("%,.0f", item.getPrice()) + "");
        priceLabel.setFont(new Font("Arial", Font.BOLD, 13));
        priceLabel.setForeground(QUANTRA_ORANGE_PRIMARY); // Warna tema
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
                card.setBorder(BorderFactory.createLineBorder(QUANTRA_ORANGE_PRIMARY, 2)); // Warna tema
            }
            @Override
            public void mouseExited(MouseEvent e) {
                card.setBorder(BorderFactory.createLineBorder(new Color(240, 240, 240), 1));
            }
        });

        return card;
    }
}