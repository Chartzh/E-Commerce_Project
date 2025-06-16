package e.commerce;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.swing.border.EmptyBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.sql.SQLException;
import e.commerce.ProductRepository.ChatMessage;

// Import Address dan ShippingService dari AddressUI untuk tipe parameter
import e.commerce.AddressUI.Address;
import e.commerce.AddressUI.ShippingService;

public class UserDashboardUI extends JFrame implements ViewController {
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private JLabel profileImageLabel;
    private Timer searchTimer;
    private FavoritesUI favoritesUI;
    private CheckoutUI checkoutUI;
    private User currentUser;
    private ChatSellerUI chatSellerUI;
    private ChatFloatingButton chatFloatingButton;
    private ChatPopupUI chatPopupUI;

    private AddressUI addressUI;
    private PaymentUI paymentUI;
    private SuccessUI successUI;

    // --- Banner Image Paths ---
    private static final String BANNER_1_PATH = "/Resources/Images/Banner1.png";
    private static final String BANNER_2_PATH = "/Resources/Images/Banner2.png";

    // --- Variables for Banner Carousel ---
    private JPanel bannerCarouselContainer;
    private JPanel bannerImagePanel;
    private CardLayout bannerCardLayout;
    private javax.swing.Timer carouselTimer;
    private int currentBannerIndex = 0;
    private final String[] bannerNames = {"Banner1", "Banner2"};
    private final String[] bannerPaths = {BANNER_1_PATH, BANNER_2_PATH};
    private List<JLabel> bannerLabels;

    private static final double BANNER_ASPECT_RATIO = 16.0 / 5.0;
    private static final int DEFAULT_BANNER_HEIGHT = 300;

    private JPanel searchResultPanel;
    private JPanel searchContentCardPanel; // Panel dengan CardLayout untuk konten pencarian
    private CardLayout searchContentCardLayout; // Layout untuk searchContentCardPanel
    private JPanel noResultsPanel; // Panel untuk tampilan "Tidak ada produk"
    private JPanel productsGridPanel; // Panel untuk menampilkan grid produk
    private JLabel searchTitleLabel;

    public UserDashboardUI() {
        currentUser = Authentication.getCurrentUser();
        if (currentUser == null) {
            LoginUI loginUI = new LoginUI();
            loginUI.setVisible(true);
            dispose();
            return;
        }

        setTitle("Quantra");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        IconUtil.setIcon(this);
        setLayout(null);

        mainPanel = new JPanel();
        cardLayout = new CardLayout();
        mainPanel.setLayout(cardLayout);
        checkoutUI = new CheckoutUI(this);

        JPanel headerPanel = createHeaderPanel(currentUser);
        JPanel dashboardPanel = createDashboardPanel();
        ProfileUI profilePanel = new ProfileUI();
        JPanel ordersPanel = createOrdersPanel();

        JPanel cartPanel = new CartUI(this);

        favoritesUI = new FavoritesUI(this, currentUser.getId());

        addressUI = new AddressUI(this);

        searchResultPanel = createSearchResultPanel(); 

        mainPanel.add(dashboardPanel, "Dashboard");
        mainPanel.add(profilePanel, "Profile");
        mainPanel.add(ordersPanel, "Order");
        mainPanel.add(cartPanel, "Cart");
        mainPanel.add(favoritesUI, "Favorites");
        mainPanel.add(checkoutUI, "Checkout");
        mainPanel.add(addressUI, "Address");
        mainPanel.add(searchResultPanel, "SearchResults"); 

        add(headerPanel);
        add(mainPanel);
        
        chatFloatingButton = new ChatFloatingButton(this, this); 
        chatFloatingButton.setSize(chatFloatingButton.getPreferredSize()); 
        chatFloatingButton.setLocation(getWidth() - chatFloatingButton.getWidth() - 30, getHeight() - chatFloatingButton.getHeight() - 30);
        
        JLayeredPane layeredPane = getRootPane().getLayeredPane();
        layeredPane.add(chatFloatingButton, JLayeredPane.PALETTE_LAYER); 

        
        chatPopupUI = new ChatPopupUI(this, this); 
        chatPopupUI.pack(); 
        chatPopupUI.setLocationRelativeTo(this); 


        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                headerPanel.setBounds(0, 0, getWidth(), 70);
                mainPanel.setBounds(0, 70, getWidth(), getHeight() - 70);
                chatFloatingButton.setLocationBasedOnParent(getWidth(), getHeight());
                if (chatPopupUI.isVisible()) {
                    int x = getX() + getWidth() - chatPopupUI.getWidth() - 20;
                    int y = getY() + getHeight() - chatPopupUI.getHeight() - 20;
                    chatPopupUI.setLocation(x, y);
                }
                revalidate();
                repaint();
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                startBannerCarousel();
                updateHeaderCartAndFavCounts();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                stopBannerCarousel();
                if (chatPopupUI != null) {
                    chatPopupUI.stopRefreshTimer();
                    chatPopupUI.dispose();
                }
                // Pastikan searchTimer di-cancel saat aplikasi ditutup
                if (searchTimer != null) {
                    searchTimer.cancel();
                }
            }
        });

        showDashboardView();
    }

    @Override
    public void showProductDetail(FavoritesUI.FavoriteItem product) {
        for (Component comp : mainPanel.getComponents()) {
            if (comp instanceof ProductDetailUI) {
                mainPanel.remove(comp);
                // Penting: Jika ProductDetailUI memiliki resource/listener yang perlu dibersihkan, panggil method di sini
                // Contoh: ((ProductDetailUI)comp).disposeResources();
                break;
            }
        }

        FavoritesUI.FavoriteItem fullProductDetails = ProductRepository.getProductById(product.getId());
        if (fullProductDetails == null) {
            JOptionPane.showMessageDialog(this, "Detail produk tidak ditemukan di database.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        ProductDetailUI productDetailPanel = new ProductDetailUI(fullProductDetails, this);
        mainPanel.add(productDetailPanel, "ProductDetail");
        cardLayout.show(mainPanel, "ProductDetail");
        chatFloatingButton.setVisible(true);
    }

    @Override
    public void showFavoritesView() {
        for (Component comp : mainPanel.getComponents()) {
            if (comp instanceof FavoritesUI) {
                mainPanel.remove(comp);
                break;
            }
        }
        favoritesUI = new FavoritesUI(this, currentUser.getId());
        mainPanel.add(favoritesUI, "Favorites");

        cardLayout.show(mainPanel, "Favorites");
        chatFloatingButton.setVisible(true); 
        updateHeaderCartAndFavCounts();
    }

    @Override
    public void showDashboardView() {
        cardLayout.show(mainPanel, "Dashboard");
        chatFloatingButton.setVisible(true); 
        updateHeaderCartAndFavCounts();
    }

    @Override
    public void showCartView() {
        for (Component comp : mainPanel.getComponents()) {
            if (comp instanceof CartUI) {
                mainPanel.remove(comp);
                break;
            }
        }
        JPanel newCartPanel = new CartUI(this);
        mainPanel.add(newCartPanel, "Cart");
        cardLayout.show(mainPanel, "Cart");
        updateHeaderCartAndFavCounts();
        chatFloatingButton.setVisible(true);
    }

    @Override
    public void showProfileView() {
        cardLayout.show(mainPanel, "Profile");
        chatFloatingButton.setVisible(true);
    }

    @Override
    public void showOrdersView() {
        for (Component comp : mainPanel.getComponents()) {
            if (comp instanceof OrderHistory) {
                mainPanel.remove(comp);
                break;
            }
        }
        OrderHistory newOrdersPanel = new OrderHistory(this);
        mainPanel.add(newOrdersPanel, "Order");
        cardLayout.show(mainPanel, "Order");
        System.out.println("Navigasi ke Halaman Riwayat Pesanan.");
        chatFloatingButton.setVisible(true);
    }

    @Override
    public void showChatWithSeller(int sellerId, String sellerUsername) {
        chatPopupUI.startChatWith(sellerId, sellerUsername);
    }

    @Override
    public void showCheckoutView() {
        cardLayout.show(mainPanel, "Checkout");
        chatFloatingButton.setVisible(false); 
    }

    @Override
    public void showAddressView() {
        for (Component comp : mainPanel.getComponents()) {
            if (comp instanceof AddressUI) {
                mainPanel.remove(comp);
                break;
            }
        }
        AddressUI newAddressUI = new AddressUI(this);
        mainPanel.add(newAddressUI, "Address");
        cardLayout.show(mainPanel, "Address");
        System.out.println("Navigasi ke Halaman Alamat.");
        chatFloatingButton.setVisible(false); 
    }

    @Override
    public void showPaymentView(Address selectedAddress, ShippingService selectedShippingService) {
        for (Component comp : mainPanel.getComponents()) {
            if (comp instanceof PaymentUI) {
                mainPanel.remove(comp);
                break;
            }
        }
        PaymentUI newPaymentUI = new PaymentUI(this, selectedAddress, selectedShippingService);
        mainPanel.add(newPaymentUI, "Payment");
        cardLayout.show(mainPanel, "Payment");
        System.out.println("Navigasi ke Halaman Pembayaran dengan Alamat dan Jasa Pengiriman.");
        chatFloatingButton.setVisible(false); 
    }

    @Override
    public void showSuccessView(int orderId) {
        for (Component comp : mainPanel.getComponents()) {
            if (comp instanceof SuccessUI) {
                mainPanel.remove(comp);
                break;
            }
        }
        SuccessUI newSuccessUI = new SuccessUI(this, orderId);
        mainPanel.add(newSuccessUI, "Success");
        cardLayout.show(mainPanel, "Success");
        System.out.println("Navigasi ke Halaman Sukses dengan ID Pesanan: " + orderId);
        updateHeaderCartAndFavCounts();
        chatFloatingButton.setVisible(false);
    }

    @Override
    public void showOrderDetailView(int orderId) {
        for (Component comp : mainPanel.getComponents()) {
            if (comp instanceof OrderDetailUI) {
                mainPanel.remove(comp);
                break;
            }
        }
        OrderDetailUI orderDetailPanel = new OrderDetailUI(this, orderId);
        mainPanel.add(orderDetailPanel, "OrderDetail");
        cardLayout.show(mainPanel, "OrderDetail");
        System.out.println("Navigasi ke Halaman Detail Pesanan untuk ID: " + orderId);
        chatFloatingButton.setVisible(false);
    }

    private JPanel createHeaderPanel(User currentUser) {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setPreferredSize(new Dimension(getWidth(), 70));
        headerPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel lblLogo = new JLabel();
        ImageIcon logoIcon = new ImageIcon(getClass().getResource("/Resources/Images/Logo.png"));
        Image scaledImage = logoIcon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
        lblLogo.setIcon(new ImageIcon(scaledImage));
        lblLogo.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblLogo.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showDashboardView();
            }
        });
        headerPanel.add(lblLogo, BorderLayout.WEST);

        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        centerPanel.setBackground(Color.WHITE);
        JPanel searchPanel = createModernSearchBar();
        centerPanel.add(searchPanel);
        headerPanel.add(centerPanel, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        rightPanel.setBackground(Color.WHITE);

        ImageIcon favIcon = new ImageIcon(getClass().getClassLoader().getResource("Resources/Images/fav_icon.png"));
        Image originalFavImage = favIcon.getImage();
        Image resizedFavImage = originalFavImage.getScaledInstance(24, 24, Image.SCALE_SMOOTH);
        ImageIcon resizedFavIcon = new ImageIcon(resizedFavImage);

        JLabel lblFav = new JLabel(resizedFavIcon);
        lblFav.setText(" " + getFavItemCount());
        lblFav.setFont(new Font("Arial", Font.BOLD, 14));
        lblFav.setHorizontalTextPosition(SwingConstants.RIGHT);
        lblFav.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblFav.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showFavoritesView();
            }
        });

        ImageIcon cartIcon = new ImageIcon(getClass().getClassLoader().getResource("Resources/Images/cart_icon.png"));
        Image originalCartImage = cartIcon.getImage();
        Image resizedCartImage = originalCartImage.getScaledInstance(24, 24, Image.SCALE_SMOOTH);
        ImageIcon resizedCartIcon = new ImageIcon(resizedCartImage);

        JLabel lblCart = new JLabel(resizedCartIcon);
        lblCart.setText(" " + getCartItemCount());
        lblCart.setFont(new Font("Arial", Font.BOLD, 14));
        lblCart.setHorizontalTextPosition(SwingConstants.RIGHT);
        lblCart.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblCart.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showCartView();
            }
        });

        ImageIcon orderIcon = new ImageIcon(getClass().getClassLoader().getResource("Resources/Images/order_icon.png"));
        Image originalOrderImage = orderIcon.getImage();
        Image resizedOrderImage = originalOrderImage.getScaledInstance(24, 24, Image.SCALE_SMOOTH);
        ImageIcon resizedOrderIcon = new ImageIcon(resizedOrderImage);

        JLabel lblOrders = new JLabel(resizedOrderIcon);
        lblOrders.setHorizontalTextPosition(SwingConstants.RIGHT);
        lblOrders.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblOrders.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showOrdersView();
            }
        });
        rightPanel.add(lblOrders);

        profileImageLabel = new JLabel();
        ImageIcon profileIcon = loadUserProfileImage(currentUser);
        profileImageLabel.setIcon(profileIcon);
        profileImageLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        profileImageLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showProfileView();
            }
        });

        rightPanel.add(lblFav);
        rightPanel.add(lblCart);
        rightPanel.add(profileImageLabel);

        headerPanel.add(rightPanel, BorderLayout.EAST);

        return headerPanel;
    }

    private ImageIcon loadUserProfileImage(User user) {
        ImageIcon defaultIcon = new ImageIcon(getClass().getResource("/Resources/Images/default_profile.png"));

        if (defaultIcon.getIconWidth() <= 0 || defaultIcon.getImageLoadStatus() != MediaTracker.COMPLETE) {
            return createDefaultAvatarIcon(user.getUsername());
        }

        Image scaledImage = defaultIcon.getImage().getScaledInstance(35, 35, Image.SCALE_SMOOTH);
        return new ImageIcon(scaledImage);
    }

    private ImageIcon createDefaultAvatarIcon(String username) {
        String initials = username.length() > 0 ? username.substring(0, 1).toUpperCase() : "U";

        BufferedImage avatar = new BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = avatar.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(new Color(255, 89, 0));
        g2.fillOval(0, 0, 40, 40);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(initials);
        int textHeight = fm.getHeight();

        g2.drawString(initials, (40 - textWidth) / 2, (40 + textHeight / 2) / 2);
        g2.dispose();

        return new ImageIcon(avatar);
    }

    private JPanel createModernSearchBar() {
        JPanel searchContainer = new JPanel();
        searchContainer.setLayout(new BorderLayout());
        searchContainer.setBackground(Color.WHITE);
        searchContainer.setPreferredSize(new Dimension(700, 35));

        searchContainer.setBorder(new CompoundBorder(
            new LineBorder(new Color(230, 230, 230), 1) {
                @Override
                public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.setColor(getLineColor());
                    g2d.drawRoundRect(x, y, width - 1, height - 1, 20, 20);
                    g2d.dispose();
                }
            },
            new EmptyBorder(8, 15, 8, 15)
        ));

        JTextField searchField = createSearchTextField();
        searchContainer.add(searchField, BorderLayout.CENTER);

        JButton searchButton = createSearchButton();
        searchContainer.add(searchButton, BorderLayout.EAST);

        searchContainer.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!searchField.isFocusOwner()) {
                    searchContainer.setBorder(new CompoundBorder(
                        new LineBorder(new Color(180, 180, 180), 1) {
                            @Override
                            public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                                Graphics2D g2d = (Graphics2D) g.create();
                                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                                g2d.setColor(getLineColor());
                                g2d.drawRoundRect(x, y, width - 1, height - 1, 20, 20);
                                g2d.dispose();
                            }
                        },
                        new EmptyBorder(8, 15, 8, 15)
                    ));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!searchField.isFocusOwner()) {
                    searchContainer.setBorder(new CompoundBorder(
                        new LineBorder(new Color(230, 230, 230), 1) {
                            @Override
                            public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                                Graphics2D g2d = (Graphics2D) g.create();
                                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                                g2d.setColor(getLineColor());
                                g2d.drawRoundRect(x, y, width - 1, height - 1, 20, 20);
                                g2d.dispose();
                            }
                        },
                        new EmptyBorder(8, 15, 8, 15)
                    ));
                }
            }
        });

        return searchContainer;
    }

    private JTextField createSearchTextField() {
        JTextField searchField = new JTextField();
        searchField.setFont(new Font("Arial", Font.PLAIN, 14));
        searchField.setBorder(null);
        searchField.setBackground(Color.WHITE);
        searchField.setForeground(Color.BLACK);

        String placeholder = "Cari produk...";
        searchField.setText(placeholder);
        searchField.setForeground(new Color(150, 150, 150));

        searchField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (searchField.getText().equals(placeholder)) {
                    searchField.setText("");
                    searchField.setForeground(Color.BLACK);
                }
                JPanel parent = (JPanel) searchField.getParent();
                parent.setBorder(new CompoundBorder(
                    new LineBorder(new Color(255, 89, 0), 2) {
                        @Override
                        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                            Graphics2D g2d = (Graphics2D) g.create();
                            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            g2d.setColor(getLineColor());
                            g2d.drawRoundRect(x, y, width - 1, height - 1, 20, 20);
                            g2d.dispose();
                        }
                    },
                    new EmptyBorder(8, 15, 8, 15)
                ));
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (searchField.getText().isEmpty()) {
                    searchField.setText(placeholder);
                    searchField.setForeground(new Color(150, 150, 150));
                }
                JPanel parent = (JPanel) searchField.getParent();
                parent.setBorder(new CompoundBorder(
                    new LineBorder(new Color(230, 230, 230), 1) {
                        @Override
                        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                            Graphics2D g2d = (Graphics2D) g.create();
                            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            g2d.setColor(getLineColor());
                            g2d.drawRoundRect(x, y, width - 1, height - 1, 20, 20);
                            g2d.dispose();
                        }
                    },
                    new EmptyBorder(8, 15, 8, 15)
                ));
            }
        });

        searchTimer = new Timer(); // This is java.util.Timer

        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (searchTimer != null) {
                    searchTimer.cancel();
                }
                searchTimer = new Timer();

                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    performSearch(searchField.getText());
                } else if (!searchField.getText().equals(placeholder) && !searchField.getText().trim().isEmpty()) {
                    searchTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            SwingUtilities.invokeLater(() -> {
                                performInstantSearch(searchField.getText());
                            });
                        }
                    }, 500);
                }
            }
        });

        return searchField;
    }

    private JButton createSearchButton() {
        JButton searchButton = new JButton();
        searchButton.setPreferredSize(new Dimension(30, 24));
        searchButton.setBorder(null);
        searchButton.setBackground(Color.WHITE);
        searchButton.setFocusPainted(false);
        searchButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        searchButton.setIcon(createSearchIcon());

        searchButton.addActionListener(e -> {
            JPanel parent = (JPanel) searchButton.getParent();
            JTextField searchField = (JTextField) parent.getComponent(0);
            String searchText = searchField.getText();
            if (!searchText.equals("Cari produk...") && !searchText.trim().isEmpty()) {
                performSearch(searchText);
            }
        });

        searchButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                searchButton.setBackground(new Color(245, 245, 245));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                searchButton.setBackground(Color.WHITE);
            }
        });

        return searchButton;
    }

    private ImageIcon createSearchIcon() {
        BufferedImage icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = icon.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(120, 120, 120));

        g2.drawOval(2, 2, 8, 8);
        g2.drawLine(9, 9, 14, 14);

        g2.dispose();
        return new ImageIcon(icon);
    }

    private void performSearch(String searchText) {
        System.out.println("Mencari: " + searchText);
        
        searchTitleLabel.setText("Hasil Pencarian untuk \"" + searchText + "\"");
        
        // Hapus semua komponen dari productsGridPanel sebelum menambahkan yang baru
        productsGridPanel.removeAll(); 

        List<FavoritesUI.FavoriteItem> searchResults;
        try {
            searchResults = ProductRepository.searchProductsByName(searchText);
        } catch (SQLException e) {
            System.err.println("Error saat mencari produk: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Gagal melakukan pencarian. Silakan coba lagi.", "Error Pencarian", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (searchResults.isEmpty()) {
            searchContentCardLayout.show(searchContentCardPanel, "NO_RESULTS");
        } else {
            // --- REVISI: Atur layout FlowLayout (yang sudah ada) pada productsGridPanel
            productsGridPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 15)); // Pastikan ini tetap FlowLayout
            for (FavoritesUI.FavoriteItem product : searchResults) {
                JPanel productCard = createDashboardProductCard(product);
                productsGridPanel.add(productCard);
            }
            productsGridPanel.revalidate();
            productsGridPanel.repaint();
            searchContentCardLayout.show(searchContentCardPanel, "RESULTS");
        }
        
        cardLayout.show(mainPanel, "SearchResults");
        SwingUtilities.invokeLater(() -> {
            JScrollPane scrollPane = (JScrollPane) searchResultPanel.getComponent(1); 
            if (scrollPane != null && scrollPane.getViewport() != null) {
                scrollPane.getVerticalScrollBar().setValue(0);
            }
        });
    }

    private void performInstantSearch(String searchText) {
        System.out.println("Pencarian langsung: " + searchText);
    }

    private void setScaledImage(JLabel label, String imagePath) {
        label.setIcon(null);
        label.setText("Memuat...");
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.ITALIC, 16));
        label.setForeground(Color.GRAY);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                // Tunggu sampai ukuran panel tersedia
                while (bannerImagePanel.getWidth() == 0 || bannerImagePanel.getHeight() == 0) {
                    Thread.sleep(50);
                }
                int panelWidth = bannerImagePanel.getWidth();
                int panelHeight = bannerImagePanel.getHeight();

                ImageIcon originalIcon = new ImageIcon(getClass().getResource(imagePath));
                Image originalImage = originalIcon.getImage();

                if (originalImage != null && originalIcon.getIconWidth() > 0) {
                    double aspectRatio = (double) originalIcon.getIconWidth() / originalIcon.getIconHeight();
                    int scaledWidth = panelWidth;
                    int scaledHeight = (int) (panelWidth / aspectRatio);

                    if (scaledHeight > panelHeight) {
                        scaledHeight = panelHeight;
                        scaledWidth = (int) (panelHeight * aspectRatio);
                    }

                    Image resizedImage = originalImage.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);
                    ImageIcon scaledIcon = new ImageIcon(resizedImage);

                    SwingUtilities.invokeLater(() -> {
                        label.setIcon(scaledIcon);
                        label.setText(null); // Hapus teks "Memuat..."
                    });
                } else {
                    SwingUtilities.invokeLater(() -> {
                        label.setText("Gambar Tidak Ditemukan");
                        label.setIcon(null);
                    });
                }
                return null;
            }
            @Override
            protected void done() {
                label.revalidate();
                label.repaint();
            }
        }.execute();
    }

    private JPanel createBannerCarouselPanel() {
        bannerCarouselContainer = new JPanel(new BorderLayout());
        bannerCarouselContainer.setBorder(new EmptyBorder(0, 0, 0, 0));

        bannerImagePanel = new JPanel();
        bannerCardLayout = new CardLayout();
        bannerImagePanel.setLayout(bannerCardLayout);
        bannerLabels = new ArrayList<>();

        for (int i = 0; i < bannerPaths.length; i++) {
            JLabel bannerLabel = new JLabel();
            bannerLabel.setHorizontalAlignment(SwingConstants.CENTER);
            bannerLabel.setVerticalAlignment(SwingConstants.CENTER);
            bannerLabels.add(bannerLabel);

            JPanel bannerContainer = new JPanel(new BorderLayout());
            bannerContainer.setBackground(Color.LIGHT_GRAY);
            bannerContainer.add(bannerLabel, BorderLayout.CENTER);
            bannerImagePanel.add(bannerContainer, bannerNames[i]);
        }

        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.add(bannerImagePanel, JLayeredPane.DEFAULT_LAYER);

        JPanel navButtonPanel = new JPanel(new GridBagLayout());
        navButtonPanel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 15, 0, 15);
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;

        JButton prevButton = new JButton("<");
        prevButton.setFocusPainted(false);
        prevButton.setBorderPainted(false);
        prevButton.setBackground(new Color(0, 0, 0, 80));
        prevButton.setForeground(Color.WHITE);
        prevButton.setFont(new Font("Arial", Font.BOLD, 20));
        prevButton.setPreferredSize(new Dimension(40, 40));
        prevButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        prevButton.addActionListener(e -> showPrevBanner());
        prevButton.setUI(new BasicButtonUI() {
            @Override
            protected void installDefaults(AbstractButton b) {
                super.installDefaults(b);
                b.setRolloverEnabled(true);
            }
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(c.getBackground());
                g2d.fillOval(0, 0, c.getWidth(), c.getHeight());

                if (c instanceof AbstractButton && ((AbstractButton) c).getModel().isRollover()) {
                    g2d.setColor(new Color(255, 89, 0, 150));
                    g2d.fillOval(0, 0, c.getWidth(), c.getHeight());
                }

                g2d.setColor(c.getForeground());
                int arrowSize = 8;
                int xCenter = c.getWidth() / 2;
                int yCenter = c.getHeight() / 2;
                g2d.drawLine(xCenter + 2, yCenter - arrowSize, xCenter - arrowSize + 2, yCenter);
                g2d.drawLine(xCenter + 2, yCenter + arrowSize, xCenter - arrowSize + 2, yCenter);
                g2d.dispose();
            }
        });


        JButton nextButton = new JButton(">");
        nextButton.setFocusPainted(false);
        nextButton.setBorderPainted(false);
        nextButton.setBackground(new Color(0, 0, 0, 80));
        nextButton.setForeground(Color.WHITE);
        nextButton.setFont(new Font("Arial", Font.BOLD, 20));
        nextButton.setPreferredSize(new Dimension(40, 40));
        nextButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        nextButton.addActionListener(e -> showNextBanner());
        nextButton.setUI(new BasicButtonUI() {
            @Override
            protected void installDefaults(AbstractButton b) {
                super.installDefaults(b);
                b.setRolloverEnabled(true);
            }
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(c.getBackground());
                g2d.fillOval(0, 0, c.getWidth(), c.getHeight());

                if (c instanceof AbstractButton && ((AbstractButton) c).getModel().isRollover()) {
                    g2d.setColor(new Color(255, 89, 0, 150));
                    g2d.fillOval(0, 0, c.getWidth(), c.getHeight());
                }

                g2d.setColor(c.getForeground());
                int arrowSize = 8;
                int xCenter = c.getWidth() / 2;
                int yCenter = c.getHeight() / 2;
                g2d.drawLine(xCenter - 2, yCenter - arrowSize, xCenter + arrowSize - 2, yCenter);
                g2d.drawLine(xCenter - 2, yCenter + arrowSize, xCenter + arrowSize - 2, yCenter);
                g2d.dispose();
            }
        });

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        navButtonPanel.add(prevButton, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.EAST;
        navButtonPanel.add(nextButton, gbc);

        layeredPane.add(navButtonPanel, JLayeredPane.PALETTE_LAYER);

        bannerCarouselContainer.add(layeredPane, BorderLayout.CENTER);

        bannerCarouselContainer.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int currentWidth = bannerCarouselContainer.getWidth();
                int currentHeight = (int) (currentWidth / BANNER_ASPECT_RATIO);
                bannerCarouselContainer.setPreferredSize(new Dimension(currentWidth, currentHeight));
                bannerCarouselContainer.revalidate();

                layeredPane.setBounds(0, 0, currentWidth, currentHeight);
                bannerImagePanel.setBounds(0, 0, currentWidth, currentHeight);
                navButtonPanel.setBounds(0, 0, currentWidth, currentHeight);

                scaleAllBannerImages();
                bannerCardLayout.show(bannerImagePanel, bannerNames[currentBannerIndex]);
            }
            @Override
            public void componentShown(ComponentEvent e) {
                int currentWidth = bannerCarouselContainer.getWidth();
                int currentHeight = (int) (currentWidth / BANNER_ASPECT_RATIO);
                if (currentWidth <= 0) {
                    currentWidth = getWidth();
                    currentHeight = (int) (currentWidth / BANNER_ASPECT_RATIO);
                    if (currentHeight <= 0) currentHeight = DEFAULT_BANNER_HEIGHT;
                }

                bannerCarouselContainer.setPreferredSize(new Dimension(currentWidth, currentHeight));
                bannerCarouselContainer.revalidate();

                layeredPane.setBounds(0, 0, currentWidth, currentHeight);
                bannerImagePanel.setBounds(0, 0, currentWidth, currentHeight);
                navButtonPanel.setBounds(0, 0, currentWidth, currentHeight);

                scaleAllBannerImages();
                bannerCardLayout.show(bannerImagePanel, bannerNames[currentBannerIndex]);
            }
        });

        bannerCarouselContainer.setPreferredSize(new Dimension(0, DEFAULT_BANNER_HEIGHT));

        return bannerCarouselContainer;
    }

    private void scaleAllBannerImages() {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                while (bannerImagePanel.getWidth() == 0 || bannerImagePanel.getHeight() == 0) {
                    Thread.sleep(50);
                }
                // --- REVISI: Panggil setScaledImage untuk setiap label banner secara individual ---
                for (int i = 0; i < bannerLabels.size(); i++) {
                    // Hanya panggil setScaledImage yang asli, tanpa SwingWorker internal
                    setScaledImage(bannerLabels.get(i), bannerPaths[i]);
                }
                // --- AKHIR REVISI ---
                return null;
            }
            @Override
            protected void done() {
            }
        }.execute();
    }

    private void showPrevBanner() { 
        if (carouselTimer != null && carouselTimer.isRunning()) {
            carouselTimer.stop();
        }
        currentBannerIndex = (currentBannerIndex - 1 + bannerNames.length) % bannerNames.length;
        bannerCardLayout.show(bannerImagePanel, bannerNames[currentBannerIndex]);
        startBannerCarousel();
    }

    private void showNextBanner() { 
        if (carouselTimer != null && carouselTimer.isRunning()) {
            carouselTimer.stop();
        }
        currentBannerIndex = (currentBannerIndex + 1) % bannerNames.length;
        bannerCardLayout.show(bannerImagePanel, bannerNames[currentBannerIndex]);
        startBannerCarousel();
    }


    private void startBannerCarousel() { 
        if (carouselTimer != null && carouselTimer.isRunning()) {
            carouselTimer.stop();
        }

        carouselTimer = new javax.swing.Timer(5000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showNextBanner();
            }
        });
        carouselTimer.start();
    }

    private void stopBannerCarousel() { 
        if (carouselTimer != null) {
            carouselTimer.stop();
        }
    }

    private JPanel createDashboardPanel() {
        JPanel dashboardPanel = new JPanel();
        dashboardPanel.setLayout(new BoxLayout(dashboardPanel, BoxLayout.Y_AXIS));
        dashboardPanel.setBackground(Color.WHITE);

        dashboardPanel.add(createBannerCarouselPanel());

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel categoriesPanel = new JPanel(new GridLayout(2, 3, 15, 15));
        categoriesPanel.setBackground(Color.WHITE);
        categoriesPanel.setBorder(new EmptyBorder(0, 0, 0, 0));

        String[] categoryNames = {"Earphone", "Wear Gadget", "Laptop", "Gaming Console", "VR Oculus", "Speaker"};
        Color[] backgroundColors = {
                new Color(255, 69, 0),
                new Color(255, 215, 0),
                new Color(255, 99, 71),
                new Color(240, 248, 255),
                new Color(50, 205, 50),
                new Color(135, 206, 250)
        };

        for (int i = 0; i < categoryNames.length; i++) {
            JPanel categoryCard = createCategoryCard(categoryNames[i], backgroundColors[i]);
            categoriesPanel.add(categoryCard);
        }

        JPanel featuresPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 50, 10));
        featuresPanel.setBackground(Color.WHITE);
        featuresPanel.setBorder(new EmptyBorder(0, 0, 0, 0));

        String[] featureTexts = {
                "Pengiriman Gratis untuk Pesanan",
                "Jaminan Uang Kembali",
                "Dukungan Online 24/7",
                "Pembayaran Aman"
        };
        for (String text : featureTexts) {
            JLabel featureLabel = new JLabel("<html><center><img src='' width='30' height='30'><br>" + text + "</center></html>");
            featureLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            featureLabel.setForeground(Color.BLACK);
            featuresPanel.add(featureLabel);
        }

        JPanel bestSellerPanel = new JPanel(new BorderLayout());
        bestSellerPanel.setBackground(Color.WHITE);
        bestSellerPanel.setBorder(new EmptyBorder(0, 0, 0, 0));

        JLabel bestSellerTitle = new JLabel("Produk Terlaris");
        bestSellerTitle.setFont(new Font("Arial", Font.BOLD, 20));
        bestSellerPanel.add(bestSellerTitle, BorderLayout.NORTH);

        JPanel bestSellerProducts = new JPanel(new GridLayout(0, 6, 15, 15));
        bestSellerProducts.setBackground(Color.WHITE);

        List<FavoritesUI.FavoriteItem> allProducts = ProductRepository.getAllProducts();
        System.out.println("DEBUG UserDashboardUI: Jumlah produk yang diambil dari database: " + allProducts.size());

        int productsToShow = Math.min(allProducts.size(), 8);

        if (productsToShow == 0) {
            System.out.println("DEBUG UserDashboardUI: Tidak ada produk untuk ditampilkan di Produk Terlaris.");
            JLabel noProductsLabel = new JLabel("Belum ada produk terlaris saat ini.", SwingConstants.CENTER);
            noProductsLabel.setFont(new Font("Arial", Font.PLAIN, 16));
            noProductsLabel.setForeground(Color.GRAY);
            bestSellerProducts.add(noProductsLabel);
        } else {
            for (int i = 0; i < productsToShow; i++) {
                FavoritesUI.FavoriteItem product = allProducts.get(i);
                System.out.println("DEBUG UserDashboardUI: Membuat kartu untuk produk: " + product.getName() + " (ID: " + product.getId() + ")");
                JPanel productCard = createDashboardProductCard(product);
                bestSellerProducts.add(productCard);
            }
        }

         bestSellerPanel.add(bestSellerProducts, BorderLayout.CENTER);

        contentPanel.add(categoriesPanel);
        contentPanel.add(featuresPanel);
        contentPanel.add(bestSellerPanel);

        dashboardPanel.add(contentPanel);

        JScrollPane scrollPane = new JScrollPane(dashboardPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(null);

        JPanel wrapperPanel = new JPanel(new BorderLayout());
        wrapperPanel.add(scrollPane, BorderLayout.CENTER);
        return wrapperPanel;
    }

    private JPanel createSearchResultPanel() {
        JPanel panel = new JPanel(new BorderLayout()); 
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel headerResultsPanel = new JPanel(new BorderLayout());
        headerResultsPanel.setBackground(Color.WHITE);
        headerResultsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JButton backButton = new JButton("← Back");
        backButton.setFont(new Font("Arial", Font.BOLD, 14));
        backButton.setForeground(new Color(50, 50, 50));
        backButton.setBackground(Color.WHITE);
        backButton.setBorderPainted(false);
        backButton.setFocusPainted(false);
        backButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backButton.addActionListener(e -> showDashboardView());
        headerResultsPanel.add(backButton, BorderLayout.WEST);

        searchTitleLabel = new JLabel("Hasil Pencarian"); 
        searchTitleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        searchTitleLabel.setForeground(Color.BLACK);
        searchTitleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        headerResultsPanel.add(searchTitleLabel, BorderLayout.CENTER);
        
        panel.add(headerResultsPanel, BorderLayout.NORTH);

        // Kontainer utama untuk konten yang bisa diskrol di hasil pencarian.
        // Ini akan menampung searchContentCardPanel (yang berisi "no results" atau grid produk)
        // dan Box.createVerticalGlue().
        JPanel mainContentForScroll = new JPanel();
        mainContentForScroll.setLayout(new BoxLayout(mainContentForScroll, BoxLayout.Y_AXIS));
        mainContentForScroll.setBackground(Color.WHITE);
        mainContentForScroll.setAlignmentX(Component.LEFT_ALIGNMENT); 
        
        // Inisialisasi CardLayout dan panel-panelnya di sini
        searchContentCardLayout = new CardLayout();
        searchContentCardPanel = new JPanel(searchContentCardLayout);
        searchContentCardPanel.setBackground(Color.WHITE);

        // Panel untuk "Tidak ada produk"
        noResultsPanel = new JPanel(new GridBagLayout()); 
        noResultsPanel.setBackground(Color.WHITE);
        JLabel noResultsLabel = new JLabel("Tidak ada produk yang ditemukan.", SwingConstants.CENTER);
        noResultsLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        noResultsLabel.setForeground(Color.GRAY);
        GridBagConstraints gbcNoResults = new GridBagConstraints();
        gbcNoResults.gridx = 0;
        gbcNoResults.gridy = 0;
        gbcNoResults.weightx = 1.0;
        gbcNoResults.weighty = 1.0; 
        gbcNoResults.fill = GridBagConstraints.BOTH;
        noResultsPanel.add(noResultsLabel, gbcNoResults);
        noResultsPanel.add(Box.createVerticalGlue(), new GridBagConstraints(0,1,1,1,1.0,1.0,GridBagConstraints.NORTH,GridBagConstraints.VERTICAL, new Insets(0,0,0,0), 0,0)); 

        // Panel untuk menampilkan grid produk
        productsGridPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 15)); 
        productsGridPanel.setBackground(Color.WHITE);
        productsGridPanel.setAlignmentX(Component.LEFT_ALIGNMENT); 

        searchContentCardPanel.add(noResultsPanel, "NO_RESULTS");
        searchContentCardPanel.add(productsGridPanel, "RESULTS");
        
        mainContentForScroll.add(searchContentCardPanel);

        // Tambahkan vertical glue di sini untuk mendorong konten ke atas
        // dan menyerap sisa ruang vertikal yang tidak terpakai.
        mainContentForScroll.add(Box.createVerticalGlue());

        JScrollPane scrollPane = new JScrollPane(mainContentForScroll);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        panel.add(scrollPane, BorderLayout.CENTER); 

        return panel;
    }

    private JPanel createDashboardProductCard(FavoritesUI.FavoriteItem item) {
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
                
                // --- START REVISION for "full" image display in product card ---
                // Gambar latar belakang penuh panel
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
                    // Pilih skala yang lebih besar agar gambar menutupi seluruh dimensi panel
                    double scaleX = (double) panelWidth / originalWidth;
                    double scaleY = (double) panelHeight / originalHeight;
                    double scale = Math.max(scaleX, scaleY); 

                    int scaledWidth = (int) (originalWidth * scale);
                    int scaledHeight = (int) (originalHeight * scale);

                    // Posisi gambar agar terpusat
                    int drawX = (panelWidth - scaledWidth) / 2;
                    int drawY = (panelHeight - scaledHeight) / 2;

                    // Gambar di panel. Clipping tidak perlu jika gambar sudah discale untuk memenuhi
                    // dan tidak ada border yang ingin dipertahankan.
                    g2d.drawImage(mainImage, drawX, drawY, scaledWidth, scaledHeight, null);
                }
                // --- AKHIR REVISI ---
            }
        };
        imagePanel.setOpaque(true);
        imagePanel.setPreferredSize(new Dimension(180, 180)); 

        JPanel imageContainer = new JPanel();
        imageContainer.setLayout(new OverlayLayout(imageContainer));
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
            if (currentUser == null) {
                JOptionPane.showMessageDialog(UserDashboardUI.this,
                    "Anda harus login untuk menambahkan produk ke keranjang.",
                    "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int userId = currentUser.getId();
            int productId = item.getId();
            int quantity = 1;

            try {
                ProductRepository.addProductToCart(userId, productId, quantity);
                JOptionPane.showMessageDialog(UserDashboardUI.this,
                    "'" + item.getName() + "' berhasil ditambahkan ke keranjang!",
                    "Berhasil Ditambahkan",
                    JOptionPane.INFORMATION_MESSAGE);
                updateHeaderCartAndFavCounts();
            } catch (SQLException ex) {
                System.err.println("Error menambahkan produk ke keranjang: " + ex.getMessage());
                JOptionPane.showMessageDialog(UserDashboardUI.this,
                    "Gagal menambahkan produk ke keranjang. Silakan coba lagi.",
                    "Error", JOptionPane.ERROR_MESSAGE);
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
                if (e.getSource() != addToCartButton) {
                    showProductDetail(item);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                card.setCursor(new Cursor(Cursor.HAND_CURSOR));
                card.setBorder(BorderFactory.createLineBorder(new Color(255, 89, 0), 2));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setBorder(BorderFactory.createLineBorder(new Color(240, 240, 240), 1));
            }
        });

        return card;
    }

    private JPanel createCategoryCard(String name, Color bgColor) { /* ... isi metode ini tidak berubah ... */
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(bgColor);
                g2d.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
            }
        };
        card.setLayout(new BorderLayout());
        card.setPreferredSize(new Dimension(200, 200));

        JLabel lblName = new JLabel(name.toUpperCase());
        lblName.setFont(new Font("Arial", Font.BOLD, 20));
        lblName.setForeground(Color.WHITE);
        lblName.setBorder(new EmptyBorder(20, 20, 0, 0));
        card.add(lblName, BorderLayout.NORTH);

        JButton browseButton = new JButton("Jelajahi");
        browseButton.setBackground(Color.WHITE);
        browseButton.setForeground(Color.BLACK);
        browseButton.setBorderPainted(false);
        browseButton.setFocusPainted(false);
        browseButton.setFont(new Font("Arial", Font.BOLD, 12));
        browseButton.setBorder(new EmptyBorder(10, 20, 10, 20));
        browseButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.setBorder(new EmptyBorder(0, 20, 20, 0));
        buttonPanel.add(browseButton);
        card.add(buttonPanel, BorderLayout.SOUTH);

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                card.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }
        });

        return card;
    }

    private JPanel createOrdersPanel() { /* ... isi metode ini tidak berubah ... */
        JPanel ordersPanel = new JPanel(new BorderLayout());
        ordersPanel.setBackground(Color.WHITE);

        JLabel lblNotAvailable = new JLabel("Silakan klik 'Pesanan' di header untuk melihat riwayat pesanan.", SwingConstants.CENTER);
        lblNotAvailable.setFont(new Font("Arial", Font.BOLD, 18));
        ordersPanel.add(lblNotAvailable, BorderLayout.CENTER);

        return ordersPanel;
    }

    private int getCartItemCount() {
        if (currentUser != null) {
            try {
                List<ProductRepository.CartItem> items = ProductRepository.getCartItemsForUser(currentUser.getId());
                return items.stream().mapToInt(ProductRepository.CartItem::getQuantity).sum();
            } catch (Exception e) {
                System.err.println("Error mengambil jumlah item keranjang dari database: " + e.getMessage());
                return 0;
            }
        }
        return 0;
    }

    private int getFavItemCount() {
        if (currentUser != null) {
            try {
                return ProductRepository.getFavoriteItemsForUser(currentUser.getId()).size();
            } catch (SQLException e) {
                System.err.println("Error mengambil jumlah item favorit dari database: " + e.getMessage());
                return 0;
            }
        }
        return 0;
    }

    public void updateHeaderCartAndFavCounts() {
        SwingUtilities.invokeLater(() -> {
            JLabel lblFav = null;
            JLabel lblCart = null;

            JPanel headerPanel = (JPanel) this.getContentPane().getComponent(0);
            if (headerPanel != null) {
                JPanel rightPanel = null;
                // Mendapatkan LayoutManager dari headerPanel
                LayoutManager layout = headerPanel.getLayout();
                if (layout instanceof BorderLayout) {
                    BorderLayout borderLayout = (BorderLayout) layout;
                    // Iterasi melalui komponen headerPanel untuk menemukan rightPanel berdasarkan constraint BorderLayout
                    for (Component comp : headerPanel.getComponents()) {
                        Object constraints = borderLayout.getConstraints(comp);
                        if (BorderLayout.EAST.equals(constraints) && comp instanceof JPanel && ((JPanel) comp).getLayout() instanceof FlowLayout) {
                            rightPanel = (JPanel) comp;
                            break;
                        }
                    }
                }
                
                if (rightPanel != null) {
                    for (Component innerComp : rightPanel.getComponents()) {
                        if (innerComp instanceof JLabel) {
                            JLabel label = (JLabel) innerComp;
                            if (label.getIcon() != null) {
                                String iconDescription = ((ImageIcon)label.getIcon()).getDescription();
                                if (iconDescription != null) {
                                    if (iconDescription.contains("fav_icon.png")) {
                                        lblFav = label;
                                    } else if (iconDescription.contains("cart_icon.png")) {
                                        lblCart = label;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (lblFav != null) {
                lblFav.setText(" " + getFavItemCount());
                lblFav.revalidate();
                lblFav.repaint();
            }
            if (lblCart != null) {
                lblCart.setText(" " + getCartItemCount());
                lblCart.revalidate();
                lblCart.repaint();
            }
        });
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            User dummyUser = new User(1, "testuser", "hashedpass", "test@example.com", "1234567890123456", "08123456789", null, null, "user", true);
            try {
                java.lang.reflect.Field field = Authentication.class.getDeclaredField("currentUser");
                field.setAccessible(true);
                field.set(null, dummyUser);
                System.out.println("User dummy diatur untuk pengujian di UserDashboardUI.");
            } catch (Exception e) {
                System.err.println("Gagal mengatur user dummy untuk pengujian di UserDashboardUI: " + e.getMessage());
            }

            UserDashboardUI dashboard = new UserDashboardUI();
            dashboard.setVisible(true);
            dashboard.updateHeaderCartAndFavCounts();
            dashboard.getComponentListeners()[0].componentResized(new ComponentEvent(dashboard, ComponentEvent.COMPONENT_RESIZED));
        });
    }
}