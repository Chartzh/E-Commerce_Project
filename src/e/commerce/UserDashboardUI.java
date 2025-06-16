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
    private FavoritesUI favoritesUI; // Deklarasi tetap
    private CheckoutUI checkoutUI;
    private User currentUser;
    private ChatSellerUI chatSellerUI; // Tidak digunakan di sini, tetapi tetap ada
    private ChatFloatingButton chatFloatingButton;
    private ChatPopupUI chatPopupUI;

    private AddressUI addressUI;
    private PaymentUI paymentUI; // Tidak diinisialisasi atau digunakan langsung di sini, tapi di showPaymentView
    private SuccessUI successUI; // Tidak diinisialisasi atau digunakan langsung di sini, tapi di showSuccessView

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
        //mainPanel.setBounds(0, 70, getWidth(), getHeight() - 70); // Akan diatur di componentResized
        checkoutUI = new CheckoutUI(this);

        JPanel headerPanel = createHeaderPanel(currentUser);
        //headerPanel.setBounds(0, 0, getWidth(), 70); // Akan diatur di componentResized
        JPanel dashboardPanel = createDashboardPanel();
        ProfileUI profilePanel = new ProfileUI();
        JPanel ordersPanel = createOrdersPanel(); // Ini akan jadi placeholder karena OrderHistory adalah JFrame

        // --- REVISI: Inisialisasi awal panel UI.
        // CartUI dan FavoritesUI akan dibuat ulang (atau di-refresh) saat di-show
        // untuk memastikan data terbaru. AddressUI juga bisa direfresh.
        // Panel-panel ini akan ditambahkan ke mainPanel secara dinamis saat show...View() dipanggil
        // agar data selalu segar.
        // Jika Anda ingin panel ini tetap ada di CardLayout sejak awal (meskipun dengan data lama),
        // Anda bisa mengembalikan baris mainPanel.add(...) seperti sebelumnya.
        // Untuk tujuan refresh data, pendekatan membuat ulang/refresh adalah yang terbaik.

        // mainPanel.add(new CartUI(this), "Cart"); // Dihapus, akan dibuat ulang di showCartView
        // mainPanel.add(new FavoritesUI(this, currentUser.getId()), "Favorites"); // Dihapus, akan dibuat ulang di showFavoritesView
        // mainPanel.add(new AddressUI(this), "Address"); // Dihapus, akan dibuat ulang di showAddressView
        // --- AKHIR REVISI ---

        mainPanel.add(dashboardPanel, "Dashboard");
        mainPanel.add(profilePanel, "Profile");
        mainPanel.add(ordersPanel, "Order"); // Placeholder
        mainPanel.add(checkoutUI, "Checkout");


        add(headerPanel);
        add(mainPanel);

        chatFloatingButton = new ChatFloatingButton(this, this);
        chatFloatingButton.setSize(chatFloatingButton.getPreferredSize());
        // chatFloatingButton.setLocation akan diatur oleh componentResized

        // JLayeredPane untuk menempatkan floating button di atas semua komponen
        JLayeredPane layeredPane = getRootPane().getLayeredPane();
        layeredPane.add(chatFloatingButton, JLayeredPane.PALETTE_LAYER);


        chatPopupUI = new ChatPopupUI(this, this);
        chatPopupUI.pack();
        // chatPopupUI.setLocation akan diatur oleh componentResized


        // Listener untuk mengatur ulang posisi floating button saat frame diubah ukurannya
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
                // --- REVISI: Perbarui hitungan saat jendela dibuka ---
                updateHeaderCartAndFavCounts();
                // --- AKHIR REVISI ---
            }

            @Override
            public void windowClosing(WindowEvent e) {
                stopBannerCarousel();
                if (chatPopupUI != null) {
                    chatPopupUI.stopRefreshTimer();
                    chatPopupUI.dispose();
                }
            }
        });

        // Set initial view to Dashboard
        showDashboardView();
    }

    @Override
    public void showProductDetail(FavoritesUI.FavoriteItem product) {
        // --- REVISI: Pastikan panel dihapus sebelum menambah yang baru ---
        for (Component comp : mainPanel.getComponents()) {
            if (comp instanceof ProductDetailUI) {
                mainPanel.remove(comp);
                break;
            }
        }
        // --- AKHIR REVISI ---

        FavoritesUI.FavoriteItem fullProductDetails = ProductRepository.getProductById(product.getId()); //
        if (fullProductDetails == null) { //
            JOptionPane.showMessageDialog(this, "Detail produk tidak ditemukan di database.", "Error", JOptionPane.ERROR_MESSAGE); //
            return; //
        }

        ProductDetailUI productDetailPanel = new ProductDetailUI(fullProductDetails, this); //
        mainPanel.add(productDetailPanel, "ProductDetail"); //
        cardLayout.show(mainPanel, "ProductDetail"); //
        chatFloatingButton.setVisible(true); //
    }

    @Override
    public void showFavoritesView() {
        // --- REVISI: Hapus FavoritesUI lama dan buat/inisialisasi ulang yang baru ---
        for (Component comp : mainPanel.getComponents()) {
            if (comp instanceof FavoritesUI) {
                mainPanel.remove(comp);
                break;
            }
        }
        // Pastikan favoritesUI selalu diinisialisasi ulang atau di-refresh
        favoritesUI = new FavoritesUI(this, currentUser.getId()); //
        mainPanel.add(favoritesUI, "Favorites");
        // --- AKHIR REVISI ---

        cardLayout.show(mainPanel, "Favorites"); //
        chatFloatingButton.setVisible(true); //
        // --- REVISI: Pastikan jumlah favorit di header diperbarui ---
        updateHeaderCartAndFavCounts();
        // --- AKHIR REVISI ---
    }

    @Override
    public void showDashboardView() {
        cardLayout.show(mainPanel, "Dashboard"); //
        chatFloatingButton.setVisible(true); //
        // --- REVISI: Perbarui jumlah di header saat kembali ke Dashboard ---
        updateHeaderCartAndFavCounts();
        // --- AKHIR REVISI ---
    }

    @Override
    public void showCartView() {
        // --- REVISI: Hapus CartUI lama dan buat yang baru ---
        for (Component comp : mainPanel.getComponents()) {
            if (comp instanceof CartUI) {
                mainPanel.remove(comp);
                break;
            }
        }
        JPanel newCartPanel = new CartUI(this); //
        mainPanel.add(newCartPanel, "Cart");
        // --- AKHIR REVISI ---

        cardLayout.show(mainPanel, "Cart"); //
        updateHeaderCartAndFavCounts(); //
        chatFloatingButton.setVisible(true); //
    }

    @Override
    public void showProfileView() {
        cardLayout.show(mainPanel, "Profile"); //
        chatFloatingButton.setVisible(true); //
    }

    @Override
    public void showOrdersView() {
        // --- REVISI: Hapus OrderHistory lama dan buat yang baru ---
        for (Component comp : mainPanel.getComponents()) {
            if (comp instanceof OrderHistory) {
                mainPanel.remove(comp);
                break;
            }
        }
        // OrderHistory diasumsikan sebagai JPanel yang menerima ViewController
        // Jika OrderHistory Anda adalah JFrame, Anda harus memanggil new OrderHistory(this).setVisible(true);
        // dan tidak menambahkannya ke mainPanel.
        OrderHistory newOrdersPanel = new OrderHistory(this); //
        mainPanel.add(newOrdersPanel, "Order");
        // --- AKHIR REVISI ---

        cardLayout.show(mainPanel, "Order"); //
        System.out.println("Navigasi ke Halaman Riwayat Pesanan."); //
        chatFloatingButton.setVisible(true); //
    }

    @Override
    public void showChatWithSeller(int sellerId, String sellerUsername) {
        // Panggil metode startChatWith di ChatPopupUI
        chatPopupUI.startChatWith(sellerId, sellerUsername); //
    }

    @Override
    public void showCheckoutView() {
        cardLayout.show(mainPanel, "Checkout"); //
        chatFloatingButton.setVisible(false); //
    }

    @Override
    public void showAddressView() {
        // --- REVISI: Hapus AddressUI lama dan buat yang baru ---
        for (Component comp : mainPanel.getComponents()) {
            if (comp instanceof AddressUI) {
                mainPanel.remove(comp);
                break;
            }
        }
        AddressUI newAddressUI = new AddressUI(this); //
        mainPanel.add(newAddressUI, "Address");
        // --- AKHIR REVISI ---

        cardLayout.show(mainPanel, "Address"); //
        System.out.println("Navigasi ke Halaman Alamat."); //
        chatFloatingButton.setVisible(false); //
    }

    @Override
    public void showPaymentView(Address selectedAddress, ShippingService selectedShippingService) {
        // --- REVISI: Hapus PaymentUI lama dan buat yang baru ---
        for (Component comp : mainPanel.getComponents()) {
            if (comp instanceof PaymentUI) {
                mainPanel.remove(comp);
                break;
            }
        }
        PaymentUI newPaymentUI = new PaymentUI(this, selectedAddress, selectedShippingService);
        mainPanel.add(newPaymentUI, "Payment");
        // --- AKHIR REVISI ---

        cardLayout.show(mainPanel, "Payment"); //
        System.out.println("Navigasi ke Halaman Pembayaran dengan Alamat dan Jasa Pengiriman."); //
        chatFloatingButton.setVisible(false); //
    }

    @Override
    public void showSuccessView(int orderId) {
        // --- REVISI: Hapus SuccessUI lama dan buat yang baru ---
        for (Component comp : mainPanel.getComponents()) {
            if (comp instanceof SuccessUI) {
                mainPanel.remove(comp);
                break;
            }
        }
        SuccessUI newSuccessUI = new SuccessUI(this, orderId); //
        mainPanel.add(newSuccessUI, "Success");
        // --- AKHIR REVISI ---

        cardLayout.show(mainPanel, "Success"); //
        System.out.println("Navigasi ke Halaman Sukses dengan ID Pesanan: " + orderId); //
        updateHeaderCartAndFavCounts(); //
        chatFloatingButton.setVisible(false); //
    }

    @Override
    public void showOrderDetailView(int orderId) {
        // --- REVISI: Hapus OrderDetailUI lama dan buat yang baru ---
        for (Component comp : mainPanel.getComponents()) {
            if (comp instanceof OrderDetailUI) {
                mainPanel.remove(comp);
                break;
            }
        }
        OrderDetailUI orderDetailPanel = new OrderDetailUI(this, orderId); //
        mainPanel.add(orderDetailPanel, "OrderDetail");
        // --- AKHIR REVISI ---

        cardLayout.show(mainPanel, "OrderDetail"); //
        System.out.println("Navigasi ke Halaman Detail Pesanan untuk ID: " + orderId); //
        chatFloatingButton.setVisible(false); //
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
        // UBAH: Ambil jumlah favorit dari database
        lblFav.setText(" " + getFavItemCount()); //
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
        lblCart.setText(" " + getCartItemCount()); //
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
                showOrdersView(); // Memanggil metode yang akan menampilkan OrderHistory
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

    private ImageIcon loadUserProfileImage(User user) { /* ... isi metode ini tidak berubah ... */
        ImageIcon defaultIcon = new ImageIcon(getClass().getResource("/Resources/Images/default_profile.png"));

        if (defaultIcon.getIconWidth() <= 0 || defaultIcon.getImageLoadStatus() != MediaTracker.COMPLETE) {
            return createDefaultAvatarIcon(user.getUsername());
        }

        Image scaledImage = defaultIcon.getImage().getScaledInstance(35, 35, Image.SCALE_SMOOTH);
        return new ImageIcon(scaledImage);
    }

    private ImageIcon createDefaultAvatarIcon(String username) { /* ... isi metode ini tidak berubah ... */
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

    private JPanel createModernSearchBar() { /* ... isi metode ini tidak berubah ... */
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

    private JTextField createSearchTextField() { /* ... isi metode ini tidak berubah ... */
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

    private JButton createSearchButton() { /* ... isi metode ini tidak berubah ... */
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

    private ImageIcon createSearchIcon() { /* ... isi metode ini tidak berubah ... */
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

    private void performSearch(String searchText) { /* ... isi metode ini tidak berubah ... */
        System.out.println("Mencari: " + searchText);
        JOptionPane.showMessageDialog(this,
            "Mencari: \"" + searchText + "\"\n\nFungsionalitas pencarian akan diimplementasikan di sini.",
            "Hasil Pencarian",
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void performInstantSearch(String searchText) { /* ... isi metode ini tidak berubah ... */
        System.out.println("Pencarian langsung: " + searchText);
    }

    private void setScaledImage(JLabel label, String imagePath) { /* ... isi metode ini tidak berubah ... */
        label.setIcon(null);
        label.setText("Memuat...");
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.ITALIC, 16));
        label.setForeground(Color.GRAY);

        new SwingWorker<ImageIcon, Void>() {
            @Override
            protected ImageIcon doInBackground() throws Exception {
                ImageIcon originalIcon = new ImageIcon(getClass().getResource(imagePath));
                if (originalIcon.getImageLoadStatus() != MediaTracker.COMPLETE) {
                    System.err.println("Gagal memuat gambar: " + imagePath);
                    return null;
                }

                Image originalImage = originalIcon.getImage();
                int originalWidth = originalImage.getWidth(null);
                int originalHeight = originalImage.getHeight(null);

                if (originalWidth <= 0 || originalHeight <= 0) {
                    System.err.println("Dimensi gambar tidak valid untuk: " + imagePath);
                    return null;
                }

                int availableWidth = label.getParent().getWidth();
                if (availableWidth <= 0) {
                    availableWidth = 1160;
                }

                int targetHeightForArea = (int) (availableWidth / BANNER_ASPECT_RATIO);

                double scaleX = (double) availableWidth / originalWidth;
                double scaleY = (double) targetHeightForArea / originalHeight;
                double scale = Math.min(scaleX, scaleY);

                int scaledWidth = (int) (originalWidth * scale);
                int scaledHeight = (int) (originalHeight * scale);

                return new ImageIcon(originalImage.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH));
            }

            @Override
            protected void done() {
                try {
                    ImageIcon icon = get();
                    if (icon != null) {
                        label.setIcon(icon);
                        label.setText("");
                        label.setHorizontalAlignment(SwingConstants.CENTER);
                        label.setVerticalAlignment(SwingConstants.CENTER);
                        label.revalidate();
                        label.repaint();
                    } else {
                        label.setText("Gagal Memuat Banner");
                        label.setFont(new Font("Arial", Font.BOLD, 20));
                        label.setHorizontalAlignment(SwingConstants.CENTER);
                        label.setVerticalAlignment(SwingConstants.CENTER);
                        label.setForeground(Color.RED);
                    }
                } catch (Exception ex) {
                    System.err.println("Error mengatur gambar banner: " + ex.getMessage());
                    label.setText("Error Memuat Banner");
                    label.setFont(new Font("Arial", Font.BOLD, 20));
                    label.setHorizontalAlignment(SwingConstants.CENTER);
                    label.setVerticalAlignment(SwingConstants.CENTER);
                    label.setForeground(Color.RED);
                }
            }
        }.execute();
    }

    private JPanel createBannerCarouselPanel() { /* ... isi metode ini tidak berubah ... */
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
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c.getBackground());
                g2.fillOval(0, 0, c.getWidth(), c.getHeight());

                if (c instanceof AbstractButton && ((AbstractButton) c).getModel().isRollover()) {
                    g2.setColor(new Color(255, 89, 0, 150));
                    g2.fillOval(0, 0, c.getWidth(), c.getHeight());
                }

                g2.setColor(c.getForeground());
                int arrowSize = 8;
                int xCenter = c.getWidth() / 2;
                int yCenter = c.getHeight() / 2;
                g2.drawLine(xCenter + 2, yCenter - arrowSize, xCenter - arrowSize + 2, yCenter);
                g2.drawLine(xCenter + 2, yCenter + arrowSize, xCenter - arrowSize + 2, yCenter);
                g2.dispose();
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
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c.getBackground());
                g2.fillOval(0, 0, c.getWidth(), c.getHeight());

                if (c instanceof AbstractButton && ((AbstractButton) c).getModel().isRollover()) {
                    g2.setColor(new Color(255, 89, 0, 150));
                    g2.fillOval(0, 0, c.getWidth(), c.getHeight());
                }

                g2.setColor(c.getForeground());
                int arrowSize = 8;
                int xCenter = c.getWidth() / 2;
                int yCenter = c.getHeight() / 2;
                g2.drawLine(xCenter - 2, yCenter - arrowSize, xCenter + arrowSize - 2, yCenter);
                g2.drawLine(xCenter - 2, yCenter + arrowSize, xCenter + arrowSize - 2, yCenter);
                g2.dispose();
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

    private void scaleAllBannerImages() { /* ... isi metode ini tidak berubah ... */
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                while (bannerImagePanel.getWidth() == 0 || bannerImagePanel.getHeight() == 0) {
                    // Tunggu hingga panel banner memiliki ukuran yang valid
                    Thread.sleep(50);
                }
                for (int i = 0; i < bannerLabels.size(); i++) {
                    setScaledImage(bannerLabels.get(i), bannerPaths[i]);
                }
                return null;
            }
        }.execute();
    }

    private void showPrevBanner() { /* ... isi metode ini tidak berubah ... */
        if (carouselTimer != null && carouselTimer.isRunning()) {
            carouselTimer.stop();
        }
        currentBannerIndex = (currentBannerIndex - 1 + bannerNames.length) % bannerNames.length;
        bannerCardLayout.show(bannerImagePanel, bannerNames[currentBannerIndex]);
        startBannerCarousel();
    }

    private void showNextBanner() { /* ... isi metode ini tidak berubah ... */
        if (carouselTimer != null && carouselTimer.isRunning()) {
            carouselTimer.stop();
        }
        currentBannerIndex = (currentBannerIndex + 1) % bannerNames.length;
        bannerCardLayout.show(bannerImagePanel, bannerNames[currentBannerIndex]);
        startBannerCarousel();
    }


    private void startBannerCarousel() { /* ... isi metode ini tidak berubah ... */
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

    private void stopBannerCarousel() { /* ... isi metode ini tidak berubah ... */
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

        List<FavoritesUI.FavoriteItem> allProducts = ProductRepository.getAllProducts(); //
        System.out.println("DEBUG UserDashboardUI: Jumlah produk yang diambil dari database: " + allProducts.size()); //

        int productsToShow = Math.min(allProducts.size(), 8); //

        if (productsToShow == 0) { //
            System.out.println("DEBUG UserDashboardUI: Tidak ada produk untuk ditampilkan di Produk Terlaris."); //
            JLabel noProductsLabel = new JLabel("Belum ada produk terlaris saat ini.", SwingConstants.CENTER); //
            noProductsLabel.setFont(new Font("Arial", Font.PLAIN, 16)); //
            noProductsLabel.setForeground(Color.GRAY); //
            bestSellerProducts.add(noProductsLabel); //
        } else {
            for (int i = 0; i < productsToShow; i++) { //
                FavoritesUI.FavoriteItem product = allProducts.get(i); //
                System.out.println("DEBUG UserDashboardUI: Membuat kartu untuk produk: " + product.getName() + " (ID: " + product.getId() + ")"); //
                JPanel productCard = createDashboardProductCard(product); //
                bestSellerProducts.add(productCard); //
            }
        }

         bestSellerPanel.add(bestSellerProducts, BorderLayout.CENTER); //

        contentPanel.add(categoriesPanel); //
        contentPanel.add(featuresPanel); //
        contentPanel.add(bestSellerPanel); //

        dashboardPanel.add(contentPanel); //

        JScrollPane scrollPane = new JScrollPane(dashboardPanel); //
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); //
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED); //
        scrollPane.setBorder(null); //

        JPanel wrapperPanel = new JPanel(new BorderLayout()); //
        wrapperPanel.add(scrollPane, BorderLayout.CENTER); //
        return wrapperPanel; //
    }

    private JPanel createDashboardProductCard(FavoritesUI.FavoriteItem item) {
        JPanel card = new JPanel(); //
        card.setLayout(new BorderLayout()); //
        card.setBackground(Color.WHITE); //
        card.setBorder(BorderFactory.createLineBorder(new Color(240, 240, 240), 1)); //
        card.setPreferredSize(new Dimension(180, 320)); //

        JPanel imagePanel = new JPanel() { //
            @Override
            protected void paintComponent(Graphics g) { //
            super.paintComponent(g); //
            Graphics2D g2d = (Graphics2D) g; //
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); //
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR); //

            g2d.setColor(item.getBgColor()); //
            g2d.fillRect(0, 0, getWidth(), getHeight()); //

            Image mainImage = null; //
            if (item.getLoadedImages() != null && !item.getLoadedImages().isEmpty()) { //
                mainImage = item.getLoadedImages().get(0); //
            }

            if (mainImage == null) { //
                g2d.setColor(item.getBgColor().darker()); //
                g2d.setFont(new Font("Arial", Font.BOLD, 12)); //
                FontMetrics fm = g2d.getFontMetrics(); //
                String text = "NO IMAGE"; //
                int textWidth = fm.stringWidth(text); //
                int textHeight = fm.getHeight(); //
                g2d.drawString(text, (getWidth() - textWidth) / 2, (getHeight() + textHeight / 2) / 2 - fm.getDescent()); //
                System.out.println("DEBUG UserDashboardUI (ImagePanel): Gambar TIDAK ditemukan untuk produk: " + item.getName()); //
            } else {
                int originalWidth = mainImage.getWidth(null); //
                int originalHeight = mainImage.getHeight(null); //

                double scaleX = (double) getWidth() / originalWidth; //
                double scaleY = (double) getHeight() / originalHeight; //
                double scale = Math.min(scaleX, scaleY); //

                int scaledWidth = (int) (originalWidth * scale); //
                int scaledHeight = (int) (originalHeight * scale); //

                int x = (getWidth() - scaledWidth) / 2; //
                int y = (getHeight() - scaledHeight) / 2; //

                g2d.drawImage(mainImage, x, y, scaledWidth, scaledHeight, null); //
                System.out.println("DEBUG UserDashboardUI (ImagePanel): Menggambar gambar untuk produk: " + item.getName() + " (Ukuran: " + originalWidth + "x" + originalHeight + ")"); //
            }
        }

            @Override
            public Dimension getPreferredSize() { //
                return new Dimension(180, 180); //
            }

            @Override
            public Dimension getMinimumSize() { //
                return new Dimension(50, 50); //
            }

            @Override
            public Dimension getMaximumSize() { //
                return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE); //
            }
        };
        imagePanel.setOpaque(true); //

        JPanel imageContainer = new JPanel(); //
        imageContainer.setLayout(new OverlayLayout(imageContainer)); //
        imageContainer.add(imagePanel); //

        JPanel infoPanel = new JPanel(); //
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS)); //
        infoPanel.setBackground(Color.WHITE); //
        infoPanel.setBorder(new EmptyBorder(10, 10, 10, 10)); //

        JLabel nameLabel = new JLabel("<html><div style='text-align: center;'>" + item.getName() + "</div></html>"); //
        nameLabel.setFont(new Font("Arial", Font.BOLD, 13)); //
        nameLabel.setForeground(Color.BLACK); //
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT); //

        JLabel stockLabel = new JLabel("<html><div style='text-align: center;'>Stok: " + item.getStock() + "</div></html>"); //
        stockLabel.setFont(new Font("Arial", Font.PLAIN, 11)); //
        stockLabel.setForeground(Color.GRAY); //
        stockLabel.setAlignmentX(Component.CENTER_ALIGNMENT); //

        JLabel priceLabel = new JLabel("<html><div style='text-align: center;'><b>Rp " + String.format("%,.0f", item.getPrice()) + "</b></div></html>"); //
        priceLabel.setFont(new Font("Arial", Font.BOLD, 14)); //
        priceLabel.setForeground(new Color(255, 89, 0)); //
        priceLabel.setAlignmentX(Component.CENTER_ALIGNMENT); //

        JLabel originalPriceLabel = null; //
        if (item.getOriginalPrice() > item.getPrice()) { //
            originalPriceLabel = new JLabel("<html><div style='text-align: center;'><strike>Rp " + String.format("%,.0f", item.getOriginalPrice()) + "</strike></div></html>"); //
            originalPriceLabel.setFont(new Font("Arial", Font.PLAIN, 11)); //
            originalPriceLabel.setForeground(Color.GRAY); //
            originalPriceLabel.setAlignmentX(Component.CENTER_ALIGNMENT); //
        }

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 5)); //
        buttonPanel.setBackground(Color.WHITE); //
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT); //

        JButton addToCartButton = new JButton("Tambah ke Keranjang"); //
        addToCartButton.setBackground(new Color(255, 89, 0)); //
        addToCartButton.setForeground(Color.WHITE); //
        addToCartButton.setBorderPainted(false); //
        addToCartButton.setFocusPainted(false); //
        addToCartButton.setFont(new Font("Arial", Font.BOLD, 10)); //
        addToCartButton.setBorder(new EmptyBorder(6, 12, 6, 12)); //
        addToCartButton.setCursor(new Cursor(Cursor.HAND_CURSOR)); //

        addToCartButton.addActionListener(e -> { //
            if (currentUser == null) { //
                JOptionPane.showMessageDialog(UserDashboardUI.this, //
                    "Anda harus login untuk menambahkan produk ke keranjang.", //
                    "Error", JOptionPane.ERROR_MESSAGE); //
                return; //
            }

            int userId = currentUser.getId(); //
            int productId = item.getId(); //
            int quantity = 1; //

            try {
                ProductRepository.addProductToCart(userId, productId, quantity); //
                JOptionPane.showMessageDialog(UserDashboardUI.this, //
                    "'" + item.getName() + "' berhasil ditambahkan ke keranjang!", //
                    "Berhasil Ditambahkan", //
                    JOptionPane.INFORMATION_MESSAGE); //
                // Setelah menambahkan ke keranjang, perbarui jumlah di header
                updateHeaderCartAndFavCounts(); //
            } catch (SQLException ex) {
                System.err.println("Error menambahkan produk ke keranjang: " + ex.getMessage()); //
                JOptionPane.showMessageDialog(UserDashboardUI.this, //
                    "Gagal menambahkan produk ke keranjang. Silakan coba lagi.", //
                    "Error", JOptionPane.ERROR_MESSAGE); //
            }
        });

        buttonPanel.add(addToCartButton); //

        infoPanel.add(nameLabel); //
        infoPanel.add(Box.createRigidArea(new Dimension(0, 2))); //
        infoPanel.add(stockLabel); //
        infoPanel.add(Box.createRigidArea(new Dimension(0, 4))); //
        infoPanel.add(priceLabel); //
        if (originalPriceLabel != null) { //
            infoPanel.add(Box.createRigidArea(new Dimension(0, 2))); //
            infoPanel.add(originalPriceLabel); //
        }
        infoPanel.add(Box.createRigidArea(new Dimension(0, 6))); //
        infoPanel.add(buttonPanel); //

        card.add(imageContainer, BorderLayout.NORTH); //
        card.add(infoPanel, BorderLayout.CENTER); //

        card.addMouseListener(new MouseAdapter() { //
            @Override
            public void mouseClicked(MouseEvent e) { //
                // Pastikan klik tidak berasal dari tombol "Tambah ke Keranjang"
                if (e.getSource() != addToCartButton) { //
                    showProductDetail(item); //
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) { //
                card.setCursor(new Cursor(Cursor.HAND_CURSOR)); //
                card.setBorder(BorderFactory.createLineBorder(new Color(255, 89, 0), 2)); //
            }

            @Override
            public void mouseExited(MouseEvent e) { //
                card.setBorder(BorderFactory.createLineBorder(new Color(240, 240, 240), 1)); //
            }
        });

        return card; //
    }

    private JPanel createCategoryCard(String name, Color bgColor) { /* ... isi metode ini tidak berubah ... */
        JPanel card = new JPanel() { //
            @Override
            protected void paintComponent(Graphics g) { //
                super.paintComponent(g); //
                Graphics2D g2d = (Graphics2D) g; //
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); //
                g2d.setColor(bgColor); //
                g2d.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20); //
            }
        };
        card.setLayout(new BorderLayout()); //
        card.setPreferredSize(new Dimension(200, 200)); //

        JLabel lblName = new JLabel(name.toUpperCase()); //
        lblName.setFont(new Font("Arial", Font.BOLD, 20)); //
        lblName.setForeground(Color.WHITE); //
        lblName.setBorder(new EmptyBorder(20, 20, 0, 0)); //
        card.add(lblName, BorderLayout.NORTH); //

        JButton browseButton = new JButton("Jelajahi"); //
        browseButton.setBackground(Color.WHITE); //
        browseButton.setForeground(Color.BLACK); //
        browseButton.setBorderPainted(false); //
        browseButton.setFocusPainted(false); //
        browseButton.setFont(new Font("Arial", Font.BOLD, 12)); //
        browseButton.setBorder(new EmptyBorder(10, 20, 10, 20)); //
        browseButton.setCursor(new Cursor(Cursor.HAND_CURSOR)); //
        JPanel buttonPanel = new JPanel(); //
        buttonPanel.setOpaque(false); //
        buttonPanel.setLayout(new FlowLayout(FlowLayout.LEFT)); //
        buttonPanel.setBorder(new EmptyBorder(0, 20, 20, 0)); //
        buttonPanel.add(browseButton); //
        card.add(buttonPanel, BorderLayout.SOUTH); //

        card.addMouseListener(new MouseAdapter() { //
            @Override
            public void mouseEntered(MouseEvent e) { //
                card.setCursor(new Cursor(Cursor.HAND_CURSOR)); //
            }
        });

        return card; //
    }

    private JPanel createOrdersPanel() {
        // Karena OrderHistory sekarang adalah JFrame (seperti yang Anda berikan),
        // Kita tidak bisa menempatkannya langsung di dalam mainPanel (yang adalah JPanel).
        // Sebagai gantinya, jika showOrdersView() dipanggil, itu akan membuka OrderHistory di jendela baru.
        // Oleh karena itu, panel ini hanya menjadi placeholder jika Anda ingin item menu "Order" tetap ada.
        // Jika Anda ingin OrderHistory sebagai JPanel, Anda perlu memodifikasi OrderHistory.java agar meng-extend JPanel,
        // dan memiliki konstruktor yang sesuai (misalnya, OrderHistory(ViewController vc)).
        // Untuk saat ini, kita akan mengembalikan JPanel kosong sebagai placeholder.
        JPanel ordersPanel = new JPanel(new BorderLayout()); //
        ordersPanel.setBackground(Color.WHITE); //

        JLabel lblNotAvailable = new JLabel("Silakan klik 'Pesanan' di header untuk melihat riwayat pesanan.", SwingConstants.CENTER); //
        lblNotAvailable.setFont(new Font("Arial", Font.BOLD, 18)); //
        ordersPanel.add(lblNotAvailable, BorderLayout.CENTER); //

        return ordersPanel; //
    }

    private int getCartItemCount() {
        if (currentUser != null) { //
            try {
                // Panggil ProductRepository untuk mendapatkan item keranjang
                List<ProductRepository.CartItem> items = ProductRepository.getCartItemsForUser(currentUser.getId()); //
                return items.stream().mapToInt(ProductRepository.CartItem::getQuantity).sum(); //
            } catch (Exception e) {
                System.err.println("Error mengambil jumlah item keranjang dari database: " + e.getMessage()); //
                return 0; //
            }
        }
        return 0; //
    }

    private int getFavItemCount() {
        if (currentUser != null) { //
            try {
                // Panggil ProductRepository untuk mendapatkan jumlah item favorit
                return ProductRepository.getFavoriteItemsForUser(currentUser.getId()).size(); //
            } catch (SQLException e) {
                System.err.println("Error mengambil jumlah item favorit dari database: " + e.getMessage()); //
                return 0; //
            }
        }
        return 0; //
    }

    // TAMBAHKAN METODE BARU INI untuk memperbarui hitungan di header
    public void updateHeaderCartAndFavCounts() { // Ubah akses menjadi public agar bisa diakses dari luar
        SwingUtilities.invokeLater(() -> {
            // Dapatkan referensi ke JLabel lblFav dan lblCart di headerPanel
            // Ini adalah pendekatan yang lebih robust daripada mencari berdasarkan indeks komponen
            JLabel lblFav = null;
            JLabel lblCart = null;

            // HeaderPanel adalah komponen pertama di JFrame
            JPanel headerPanel = (JPanel) this.getContentPane().getComponent(0);
            if (headerPanel != null) {
                // rightPanel adalah komponen di BorderLayout.EAST dari headerPanel
                Component[] headerComponents = headerPanel.getComponents();
                for (Component comp : headerComponents) {
                    if (comp instanceof JPanel && ((BorderLayout)headerPanel.getLayout()).getConstraints(comp) == BorderLayout.EAST) {
                        JPanel rightPanel = (JPanel) comp;
                        for (Component innerComp : rightPanel.getComponents()) {
                            if (innerComp instanceof JLabel) {
                                JLabel label = (JLabel) innerComp;
                                // Identifikasi label berdasarkan ikonnya (asumsi nama file ikon unik)
                                ImageIcon icon = (ImageIcon) label.getIcon();
                                if (icon != null && icon.getDescription() != null) {
                                    if (icon.getDescription().contains("fav_icon.png")) {
                                        lblFav = label;
                                    } else if (icon.getDescription().contains("cart_icon.png")) {
                                        lblCart = label;
                                    }
                                }
                            }
                        }
                        break; // rightPanel ditemukan
                    }
                }
            }

            // Perbarui teks label jika ditemukan
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
                // Menggunakan refleksi untuk mengatur currentUser di Authentication
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
            // Memanggil listener resize untuk mengatur layout awal
            dashboard.getComponentListeners()[0].componentResized(new ComponentEvent(dashboard, ComponentEvent.COMPONENT_RESIZED));
        });
    }
}