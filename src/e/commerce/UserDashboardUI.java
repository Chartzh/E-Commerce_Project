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

import e.commerce.AddressUI.Address;
import e.commerce.AddressUI.ShippingService;
import e.commerce.ProductRepository.SupportTicket;
import java.text.SimpleDateFormat;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

public class UserDashboardUI extends JFrame implements ViewController {
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private JLabel profileImageLabel;
    private Timer searchTimer;
    private FavoritesUI favoritesUI;
    private User currentUser;
    private ChatSellerUI chatSellerUI;
    private ChatFloatingButton chatFloatingButton;
    private ChatPopupUI chatPopupUI;

    private AddressUI addressUI;
    private PaymentUI paymentUI;
    private SuccessUI successUI;

    private static final String BANNER_1_PATH = "/Resources/Images/Banner1.png";
    private static final String BANNER_2_PATH = "/Resources/Images/Banner2.png";

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
    
    private static final Color ORANGE_THEME = new Color(255, 69, 0);

    private JPanel searchResultPanel;
    private JPanel searchContentCardPanel;
    private CardLayout searchContentCardLayout;
    private JPanel noResultsPanel;
    private JPanel productsGridPanel;
    private JLabel searchTitleLabel;
    
    private JTable ticketTable;
    private DefaultTableModel ticketTableModel;
    private JTextField subjectField;
    private JTextArea messageArea;

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

        JPanel headerPanel = createHeaderPanel(currentUser);
        JPanel dashboardPanel = createDashboardPanel();
        ProfileUI profilePanel = new ProfileUI();
        JPanel ordersPanel = createOrdersPanel();
        JPanel supportPanel = createCustomerSupportPanel();
        JPanel cartPanel = new CartUI(this);

        favoritesUI = new FavoritesUI(this, currentUser.getId());

        addressUI = new AddressUI(this, null);

        searchResultPanel = createSearchResultPanel();

        mainPanel.add(dashboardPanel, "Dashboard");
        mainPanel.add(profilePanel, "Profile");
        mainPanel.add(ordersPanel, "Order");
        mainPanel.add(cartPanel, "Cart");
        mainPanel.add(favoritesUI, "Favorites");
        mainPanel.add(addressUI, "Address");
        mainPanel.add(searchResultPanel, "SearchResults");
        mainPanel.add(supportPanel, "Support");


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
    public void showAddressView(CouponResult couponResult) {
        for (Component comp : mainPanel.getComponents()) {
            if (comp instanceof AddressUI) {
                mainPanel.remove(comp);
                break;
            }
        }
        AddressUI newAddressUI = new AddressUI(this, couponResult);
        mainPanel.add(newAddressUI, "Address");
        cardLayout.show(mainPanel, "Address");
        System.out.println("Navigasi ke Halaman Alamat.");
        chatFloatingButton.setVisible(false);
    }
    

    @Override
    public void showPaymentView(Address selectedAddress, ShippingService selectedShippingService, double totalAmount, CouponResult couponResult) {
        for (Component comp : mainPanel.getComponents()) {
            if (comp instanceof PaymentUI) {
                mainPanel.remove(comp);
                break;
            }
        }
        PaymentUI newPaymentUI = new PaymentUI(this, selectedAddress, selectedShippingService, couponResult);
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

    @Override
    public void showProductReviewView(int productId, String productName, String productImage, double productPrice, String sellerName) {
        for (Component comp : mainPanel.getComponents()) {
            if (comp instanceof ProductReview) {
                mainPanel.remove(comp);
                break;
            }
        }
        ProductReview productReviewPanel = new ProductReview(this, productId, productName, productImage, productPrice, sellerName);
        mainPanel.add(productReviewPanel, "ProductReview");
        cardLayout.show(mainPanel, "ProductReview");
        this.setTitle("Review Produk: " + productName);
        chatFloatingButton.setVisible(false);
        this.revalidate();
        this.repaint();
    }
    
    public void showSupportView() {
        cardLayout.show(mainPanel, "Support");
    }

    private JPanel createHeaderPanel(User currentUser) {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setPreferredSize(new Dimension(getWidth(), 70));
        headerPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel lblLogo = new JLabel();
        ImageIcon logoIcon = new ImageIcon(getClass().getResource("/Resources/Images/Logo.png"));
        Image scaledImage = logoIcon.getImage().getScaledInstance(100, 35, Image.SCALE_SMOOTH);
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
        
        ImageIcon supportIcon = new ImageIcon(getClass().getClassLoader().getResource("Resources/Images/support_icon.png"));
        Image scaledSupportImage = supportIcon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
        JLabel lblSupport = new JLabel(new ImageIcon(scaledSupportImage));
        lblSupport.setToolTipText("Pusat Bantuan");
        lblSupport.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblSupport.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showSupportView();
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
        
        rightPanel.add(lblSupport);
        rightPanel.add(lblOrders);
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

        searchTimer = new Timer();

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

        searchContainer.add(searchField, BorderLayout.CENTER);

        JButton searchButton = new JButton();
        searchButton.setPreferredSize(new Dimension(30, 24));
        searchButton.setBorder(null);
        searchButton.setBackground(Color.WHITE);
        searchButton.setFocusPainted(false);
        searchButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        BufferedImage icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = icon.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(120, 120, 120));
        g2.drawOval(2, 2, 8, 8);
        g2.drawLine(9, 9, 14, 14);
        g2.dispose();
        searchButton.setIcon(new ImageIcon(icon));

        searchButton.addActionListener(e -> {
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

        searchContainer.add(searchButton, BorderLayout.EAST);

        return searchContainer;
    }

    private void performSearch(String searchText) {
        System.out.println("Mencari: " + searchText);

        searchTitleLabel.setText("Hasil Pencarian untuk \"" + searchText + "\"");

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
            productsGridPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 15));
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
                        label.setText(null);
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
                for (int i = 0; i < bannerLabels.size(); i++) {
                    setScaledImage(bannerLabels.get(i), bannerPaths[i]);
                }
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
        contentPanel.setBorder(new EmptyBorder(30, 20, 30, 20));

        JPanel categoriesPanel = new JPanel(new GridLayout(2, 3, 15, 15));
        categoriesPanel.setBackground(Color.WHITE);
        categoriesPanel.setBorder(new EmptyBorder(0, 0, 0, 0));

        String[] categoryNames = {"Earphone", "Wear Gadget", "Laptop", "Gaming Console", "VR Oculus", "Speaker"};

        Color[] backgroundColors = {
                new Color(255, 89, 0),
                new Color(255, 215, 0),
                new Color(255, 99, 71),
                new Color(80, 71, 173),
                new Color(50, 205, 50),
                new Color(135, 206, 250)
        };

        String[] categoryImagePaths = {
            "/Resources/Images/earphone_category.png",
            "/Resources/Images/wear_category.png",
            "/Resources/Images/laptop_category.png",
            "/Resources/Images/console_category.png",
            "/Resources/Images/vr_category.png",
            "/Resources/Images/speaker_category.png"
        };


        for (int i = 0; i < categoryNames.length; i++) {
            JPanel categoryCard = createCategoryCard(categoryNames[i], backgroundColors[i], categoryImagePaths[i]);
            categoriesPanel.add(categoryCard);
        }

        JPanel featuresPanel = new JPanel(new GridLayout(1, 4, 100, 0));
        featuresPanel.setBackground(Color.WHITE);
        featuresPanel.setBorder(new EmptyBorder(0, 0, 0, 0));

        String[] featureTexts = {
                "Pengiriman Gratis",
                "Jaminan Uang Kembali",
                "Dukungan Online 24/7",
                "Pembayaran Aman"
        };
        String[] featureIconPaths = {
            "/Resources/Images/shipping_icon.png",
            "/Resources/Images/money_back_icon.png",
            "/Resources/Images/support_icon.png",
            "/Resources/Images/secure_payment_icon.png"
        };
        for (int i = 0; i < featureTexts.length; i++) {
            JPanel featureItemPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
            featureItemPanel.setBackground(Color.WHITE);

            JLabel featureIconLabel = new JLabel();
            try {
                ImageIcon originalIcon = new ImageIcon(getClass().getResource(featureIconPaths[i]));
                Image originalImage = originalIcon.getImage();

                Image scaledImage = originalImage.getScaledInstance(50, 50, Image.SCALE_SMOOTH);
                featureIconLabel.setIcon(new ImageIcon(scaledImage));
            } catch (Exception e) {
                System.err.println("Error loading feature icon from: " + featureIconPaths[i] + " - " + e.getMessage());
                featureIconLabel.setText("!");
            }

            JLabel featureTextLabel = new JLabel(featureTexts[i]);
            featureTextLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            featureTextLabel.setForeground(Color.BLACK);

            featureItemPanel.add(featureIconLabel);
            featureItemPanel.add(featureTextLabel);

            featuresPanel.add(featureItemPanel);
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
        contentPanel.add(Box.createRigidArea(new Dimension(0, 30)));
        contentPanel.add(featuresPanel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 30)));
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

        JPanel mainContentForScroll = new JPanel();
        mainContentForScroll.setLayout(new BoxLayout(mainContentForScroll, BoxLayout.Y_AXIS));
        mainContentForScroll.setBackground(Color.WHITE);
        mainContentForScroll.setAlignmentX(Component.LEFT_ALIGNMENT);

        searchContentCardLayout = new CardLayout();
        searchContentCardPanel = new JPanel(searchContentCardLayout);
        searchContentCardPanel.setBackground(Color.WHITE);

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

        productsGridPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 15));
        productsGridPanel.setBackground(Color.WHITE);
        productsGridPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        searchContentCardPanel.add(noResultsPanel, "NO_RESULTS");
        searchContentCardPanel.add(productsGridPanel, "RESULTS");

        mainContentForScroll.add(searchContentCardPanel);

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

  private JPanel createCategoryCard(String name, Color bgColor, String imagePath) {
    JPanel card = new JPanel(new BorderLayout()) {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(bgColor);
            g2d.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
        }
    };
    card.setPreferredSize(new Dimension(200, 200));
    card.setOpaque(false);

    JLabel lblName = new JLabel(name.toUpperCase());
    lblName.setFont(new Font("Arial", Font.BOLD, 20));
    lblName.setForeground(Color.WHITE);
    lblName.setBorder(new EmptyBorder(20, 20, 0, 0));
    lblName.setHorizontalAlignment(SwingConstants.LEFT);
    card.add(lblName, BorderLayout.NORTH);

    JPanel contentContainer = new JPanel(new GridBagLayout());
    contentContainer.setOpaque(false);

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.BOTH;
    gbc.insets = new Insets(0, 0, 0, 0);

    JPanel leftContentPanel = new JPanel();
    leftContentPanel.setOpaque(false);
    leftContentPanel.setLayout(new BoxLayout(leftContentPanel, BoxLayout.Y_AXIS));
    leftContentPanel.setBorder(new EmptyBorder(0, 20, 20, 0));

    JButton browseButton = new JButton("Jelajahi");
    browseButton.setBackground(Color.WHITE);
    browseButton.setForeground(Color.BLACK);
    browseButton.setBorderPainted(false);
    browseButton.setFocusPainted(false);
    browseButton.setFont(new Font("Arial", Font.BOLD, 12));
    browseButton.setBorder(new EmptyBorder(10, 20, 10, 20));
    browseButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
    browseButton.setAlignmentX(Component.LEFT_ALIGNMENT);
    leftContentPanel.add(Box.createVerticalGlue());
    leftContentPanel.add(browseButton);
    leftContentPanel.add(Box.createVerticalGlue());

    gbc.gridx = 0;
    gbc.weightx = 0.6;
    contentContainer.add(leftContentPanel, gbc);

    if (imagePath != null) {
        JPanel imageRenderPanel = new JPanel() {
            private Image cachedImage = null;

            @Override
            public void addNotify() {
                super.addNotify();
                if (cachedImage == null) {
                    try {
                        ImageIcon originalIcon = new ImageIcon(getClass().getResource(imagePath));
                        cachedImage = originalIcon.getImage();
                        if (cachedImage == null) {
                            System.err.println("Gagal memuat gambar: " + imagePath);
                        }
                    } catch (Exception e) {
                        System.err.println("Error memuat gambar untuk kategori " + name + ": " + e.getMessage());
                    }
                    SwingUtilities.invokeLater(() -> repaint());
                }
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                if (cachedImage != null) {
                    int panelWidth = getWidth();
                    int panelHeight = getHeight();
                    int originalWidth = cachedImage.getWidth(null);
                    int originalHeight = cachedImage.getHeight(null);

                    if (panelWidth <= 0 || panelHeight <= 0 || originalWidth <= 0 || originalHeight <= 0) {
                        return;
                    }

                    double scaleY = (double) panelHeight / originalHeight;
                    int scaledWidth = (int) (originalWidth * scaleY);
                    int scaledHeight = panelHeight;
                    int drawX = panelWidth - scaledWidth;
                    int offsetPixelsLeft = 30;

                    drawX += offsetPixelsLeft;

                    int finalDrawX;
                    if (scaledWidth > panelWidth) {
                        float cropRatioFromLeft = 0.2f;
                        int pixelsToCutFromLeft = (int) ((scaledWidth - panelWidth) * cropRatioFromLeft);
                        finalDrawX = -pixelsToCutFromLeft;
                    } else {
                        finalDrawX = panelWidth - scaledWidth;
                        finalDrawX -= 10;
                    }

                    int drawY = -4;

                    g2d.drawImage(cachedImage, finalDrawX, drawY, scaledWidth, scaledHeight, null);
                } else {
                    g2d.setColor(Color.WHITE);
                    g2d.setFont(new Font("Arial", Font.BOLD, 12));
                    String noImgText = "NO IMAGE";
                    FontMetrics fm = g2d.getFontMetrics();
                    int textWidth = fm.stringWidth(noImgText);
                    int textHeight = fm.getHeight();
                    g2d.drawString(noImgText, (getWidth() - textWidth) / 2, (getHeight() + textHeight / 2) / 2);
                }
            }
        };
        imageRenderPanel.setOpaque(false);
        imageRenderPanel.setMinimumSize(new Dimension(120, 180));
        imageRenderPanel.setPreferredSize(new Dimension(120, 180));

        gbc.gridx = 1;
        gbc.weightx = 1;
        contentContainer.add(imageRenderPanel, gbc);
    }

    card.add(contentContainer, BorderLayout.CENTER);

    card.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseEntered(MouseEvent e) {
            card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        }
    });

    return card;
}

    private JPanel createOrdersPanel() {
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
                LayoutManager layout = headerPanel.getLayout();
                if (layout instanceof BorderLayout) {
                    BorderLayout borderLayout = (BorderLayout) layout;
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
    
    private JPanel createCustomerSupportPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(new Color(250, 250, 250));

        // Header Panel
        JPanel headerCsPanel = new JPanel(new BorderLayout());
        headerCsPanel.setBackground(Color.WHITE);
        headerCsPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(224, 224, 224)),
            new EmptyBorder(15, 20, 15, 20)
        ));

        // Back Button
        JButton backButton = createModernButton("← Kembali", Color.WHITE);
        backButton.setForeground(new Color(50, 50, 50));
        backButton.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        backButton.addActionListener(e -> showDashboardView());
        headerCsPanel.add(backButton, BorderLayout.WEST);

        // Title
        JLabel titleLabel = new JLabel("Pusat Bantuan");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(new Color(255, 69, 0));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        headerCsPanel.add(titleLabel, BorderLayout.CENTER);

        headerCsPanel.add(Box.createHorizontalStrut(backButton.getPreferredSize().width), BorderLayout.EAST);

        panel.add(headerCsPanel, BorderLayout.NORTH);

        // Main Content
        JPanel mainPanelCs = new JPanel(new BorderLayout(30, 0));
        mainPanelCs.setBorder(new EmptyBorder(25, 25, 25, 25));
        mainPanelCs.setBackground(new Color(250, 250, 250));

        JPanel historyPanel = createHistoryPanel();
        JPanel formPanel = createFormPanel();

        mainPanelCs.add(historyPanel, BorderLayout.CENTER);
        mainPanelCs.add(formPanel, BorderLayout.EAST);

        panel.add(mainPanelCs, BorderLayout.CENTER);

        loadUserTickets();
        return panel;
    }

    private JPanel createHistoryPanel() {
        JPanel historyPanel = new JPanel(new BorderLayout(15, 15));
        historyPanel.setBackground(new Color(250, 250, 250));

        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(250, 250, 250));
        headerPanel.setBorder(new EmptyBorder(0, 0, 15, 0));

        JLabel title = new JLabel("Riwayat Pengaduan Anda");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(new Color(255, 69, 0));
        headerPanel.add(title, BorderLayout.WEST);

        historyPanel.add(headerPanel, BorderLayout.NORTH);

        // Table setup
        String[] columnNames = {"ID Tiket", "Subjek", "Status", "Tanggal Dibuat"};
        ticketTableModel = new DefaultTableModel(columnNames, 0) {
            @Override 
            public boolean isCellEditable(int row, int column) { 
                return false; 
            }
        };

        ticketTable = new JTable(ticketTableModel);
        setupModernTable(ticketTable);

        ticketTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    int row = ticketTable.rowAtPoint(evt.getPoint());
                    if (row >= 0) {
                        int ticketId = (int) ticketTableModel.getValueAt(row, 0);
                        showTicketDetail(ticketId);
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(ticketTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        historyPanel.add(scrollPane, BorderLayout.CENTER);

        return historyPanel;
    }

    private JPanel createFormPanel() {
        JPanel formContainer = new JPanel(new BorderLayout());
        formContainer.setBackground(new Color(250, 250, 250));
        formContainer.setPreferredSize(new Dimension(400, 0));

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
            new EmptyBorder(25, 25, 25, 25)
        ));

        // Title
        JLabel formTitle = new JLabel("Buat Pengaduan Baru");
        formTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        formTitle.setForeground(new Color(255, 69, 0));
        formTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Subject field
        JLabel subjectLabel = new JLabel("Subjek");
        subjectLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        subjectLabel.setForeground(new Color(70, 70, 70));
        subjectLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        subjectField = new JTextField();
        subjectField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subjectField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        subjectField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            new EmptyBorder(10, 12, 10, 12)
        ));
        subjectField.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Message area
        JLabel messageLabel = new JLabel("Pesan Anda");
        messageLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        messageLabel.setForeground(new Color(70, 70, 70));
        messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        messageArea = new JTextArea(8, 0);
        messageArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setBorder(new EmptyBorder(15, 15, 15, 15));
        messageArea.setBackground(Color.WHITE);

        JScrollPane messageScrollPane = new JScrollPane(messageArea);
        messageScrollPane.setBorder(createModernTitledBorder("Tulis pesan Anda di sini..."));
        messageScrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        messageScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Submit button
        JButton submitButton = createModernButton("Kirim Pengaduan", new Color(255, 69, 0));
        submitButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        submitButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        submitButton.addActionListener(e -> submitTicket());

        // Layout
        formPanel.add(formTitle);
        formPanel.add(Box.createVerticalStrut(25));
        formPanel.add(subjectLabel);
        formPanel.add(Box.createVerticalStrut(8));
        formPanel.add(subjectField);
        formPanel.add(Box.createVerticalStrut(20));
        formPanel.add(messageLabel);
        formPanel.add(Box.createVerticalStrut(8));
        formPanel.add(messageScrollPane);
        formPanel.add(Box.createVerticalStrut(25));
        formPanel.add(submitButton);
        formPanel.add(Box.createVerticalGlue());

        formContainer.add(formPanel, BorderLayout.NORTH);
        return formContainer;
    }

    private void setupModernTable(JTable table) {
        table.setRowHeight(45);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(new Color(255, 69, 0, 50)); // Light orange selection
        table.setSelectionForeground(Color.BLACK);
        table.setGridColor(new Color(230, 230, 230));
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));

        // Header styling
        JTableHeader header = table.getTableHeader();
        header.setBackground(new Color(255, 69, 0));
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setPreferredSize(new Dimension(0, 40));
        header.setBorder(BorderFactory.createEmptyBorder());

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(80);
        table.getColumnModel().getColumn(1).setPreferredWidth(200);
        table.getColumnModel().getColumn(2).setPreferredWidth(120);
        table.getColumnModel().getColumn(3).setPreferredWidth(150);

        // Status column renderer
        table.getColumnModel().getColumn(2).setCellRenderer(new StatusCellRenderer());
    }

    private JButton createModernButton(String text, Color backgroundColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setBackground(backgroundColor);
        button.setForeground(backgroundColor.equals(Color.WHITE) ? new Color(50, 50, 50) : Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Hover effect
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (backgroundColor.equals(Color.WHITE)) {
                    button.setBackground(new Color(240, 240, 240));
                } else {
                    button.setBackground(backgroundColor.darker());
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(backgroundColor);
            }
        });

        return button;
    }

    private javax.swing.border.TitledBorder createModernTitledBorder(String title) {
        javax.swing.border.TitledBorder border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
            title
        );
        border.setTitleFont(new Font("Segoe UI", Font.BOLD, 14));
        border.setTitleColor(new Color(255, 69, 0));
        return border;
    }

    // Custom cell renderer for status column
    private class StatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (!isSelected) {
                String status = value.toString().toLowerCase();
                switch (status) {
                    case "dibuka":
                    case "open":
                    case "terbuka":
                        setBackground(new Color(254, 243, 199));
                        setForeground(new Color(146, 64, 14));
                        break;
                    case "selesai":
                    case "closed":
                    case "tertutup":
                        setBackground(new Color(220, 252, 231));
                        setForeground(new Color(22, 101, 52));
                        break;
                    case "dibalas manajer":
                    case "pending":
                        setBackground(new Color(255, 69, 0, 30));
                        setForeground(new Color(255, 69, 0));
                        break;
                    default:
                        setBackground(Color.WHITE);
                        setForeground(Color.BLACK);
                }
            }

            setFont(new Font("Segoe UI", Font.BOLD, 12));
            setHorizontalAlignment(SwingConstants.CENTER);
            setBorder(new EmptyBorder(8, 12, 8, 12));

            return c;
        }
    }

    private void loadUserTickets() {
        ticketTableModel.setRowCount(0);
        try {
            List<SupportTicket> tickets = ProductRepository.getTicketsByUserId(currentUser.getId());
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm"); 
            for (SupportTicket ticket : tickets) {
                ticketTableModel.addRow(new Object[]{
                    ticket.getId(), 
                    ticket.getSubject(), 
                    ticket.getStatus(), 
                    sdf.format(ticket.getCreatedAt())
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Gagal memuat riwayat tiket.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void submitTicket() {
        String subject = subjectField.getText().trim();
        String message = messageArea.getText().trim();
        if (subject.isEmpty() || message.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Subjek dan pesan tidak boleh kosong.", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            if (ProductRepository.createSupportTicket(currentUser.getId(), subject, message)) {
                JOptionPane.showMessageDialog(this, "Pengaduan Anda berhasil dikirim.", "Sukses", JOptionPane.INFORMATION_MESSAGE);
                subjectField.setText("");
                messageArea.setText("");
                loadUserTickets();
            } else {
                JOptionPane.showMessageDialog(this, "Gagal mengirim pengaduan.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Terjadi kesalahan database.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showTicketDetail(int ticketId) {
        try {
            SupportTicket ticket = ProductRepository.getTicketById(ticketId);
            if (ticket == null) return;

            JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Detail Pengaduan #" + ticketId, true);
            dialog.setSize(550, 650);
            dialog.setLocationRelativeTo(this);
            dialog.setLayout(new BorderLayout(15, 15));
            dialog.getContentPane().setBackground(new Color(250, 250, 250));

            // Header panel with ticket info
            JPanel headerPanel = new JPanel();
            headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
            headerPanel.setBackground(new Color(250, 250, 250));
            headerPanel.setBorder(new EmptyBorder(20, 20, 10, 20));

            JLabel titleLabel = new JLabel("Detail Pengaduan #" + ticketId);
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
            titleLabel.setForeground(new Color(255, 69, 0));
            titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            JPanel infoPanel = new JPanel(new GridLayout(3, 2, 10, 10));
            infoPanel.setBackground(new Color(250, 250, 250));
            infoPanel.setBorder(new EmptyBorder(15, 0, 0, 0));

            infoPanel.add(createInfoLabel("ID Tiket:", String.valueOf(ticket.getId())));
            infoPanel.add(new JLabel()); // Empty cell
            infoPanel.add(createInfoLabel("Subjek:", ticket.getSubject()));
            infoPanel.add(new JLabel()); // Empty cell
            infoPanel.add(createInfoLabel("Status:", ticket.getStatus()));
            infoPanel.add(new JLabel()); // Empty cell

            headerPanel.add(titleLabel);
            headerPanel.add(infoPanel);

            // Message area
            JTextArea messageArea = new JTextArea(ticket.getMessage());
            messageArea.setEditable(false);
            messageArea.setLineWrap(true);
            messageArea.setWrapStyleWord(true);
            messageArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            messageArea.setBackground(Color.WHITE);
            messageArea.setBorder(new EmptyBorder(15, 15, 15, 15));

            JScrollPane messageScroll = new JScrollPane(messageArea);
            messageScroll.setBorder(createModernTitledBorder("Pesan Anda"));
            messageScroll.setPreferredSize(new Dimension(0, 150));

            // Reply area
            JTextArea replyArea = new JTextArea(ticket.getManagerReply() != null ? ticket.getManagerReply() : "Belum ada balasan dari manajer.");
            replyArea.setEditable(false);
            replyArea.setLineWrap(true);
            replyArea.setWrapStyleWord(true);
            replyArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            replyArea.setBackground(Color.WHITE);
            replyArea.setBorder(new EmptyBorder(15, 15, 15, 15));

            JScrollPane replyScroll = new JScrollPane(replyArea);
            replyScroll.setBorder(createModernTitledBorder("Balasan Manajer"));
            replyScroll.setPreferredSize(new Dimension(0, 150));

            // Control panel
            JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
            controlPanel.setBackground(new Color(250, 250, 250));
            controlPanel.setBorder(new EmptyBorder(10, 20, 20, 20));

            JButton closeButton = createModernButton("Tutup", new Color(255, 69, 0));
            closeButton.addActionListener(e -> dialog.dispose());
            controlPanel.add(closeButton);

            // Center panel for message and reply
            JPanel centerPanel = new JPanel(new GridLayout(2, 1, 0, 15));
            centerPanel.setBackground(new Color(250, 250, 250));
            centerPanel.setBorder(new EmptyBorder(0, 20, 0, 20));
            centerPanel.add(messageScroll);
            centerPanel.add(replyScroll);

            dialog.add(headerPanel, BorderLayout.NORTH);
            dialog.add(centerPanel, BorderLayout.CENTER);
            dialog.add(controlPanel, BorderLayout.SOUTH);
            dialog.setVisible(true);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Gagal mengambil detail tiket.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel createInfoLabel(String label, String value) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setBackground(new Color(250, 250, 250));

        JLabel labelComponent = new JLabel(label);
        labelComponent.setFont(new Font("Segoe UI", Font.BOLD, 14));
        labelComponent.setForeground(new Color(70, 70, 70));

        JLabel valueComponent = new JLabel(value);
        valueComponent.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        valueComponent.setForeground(new Color(100, 100, 100));

        panel.add(labelComponent);
        panel.add(Box.createHorizontalStrut(10));
        panel.add(valueComponent);

        return panel;
    }
}