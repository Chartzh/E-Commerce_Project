package e.commerce;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.swing.border.EmptyBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import java.util.ArrayList; // Import untuk List
import java.util.List; // Import untuk List
import java.util.Timer;
import java.util.TimerTask;

public class UserDashboardUI extends JFrame implements ViewController {
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private JLabel profileImageLabel;
    private Timer searchTimer;
    private FavoritesUI favoritesUI;

    public UserDashboardUI() {
        // Logika otentikasi (pastikan kelas User dan Authentication ada)
        User currentUser = Authentication.getCurrentUser();
        if (currentUser == null) {
            LoginUI loginUI = new LoginUI(); // Pastikan LoginUI ada
            loginUI.setVisible(true);
            dispose();
            return;
        }

        // Pengaturan dasar JFrame
        setTitle("Quantra");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        IconUtil.setIcon(this); // Pastikan IconUtil ada dan berfungsi
        setLayout(new BorderLayout());

        // Inisialisasi CardLayout dan mainPanel
        mainPanel = new JPanel();
        cardLayout = new CardLayout();
        mainPanel.setLayout(cardLayout);

        // Buat panel-panel utama
        JPanel headerPanel = createHeaderPanel(currentUser);
        JPanel dashboardPanel = createDashboardPanel();
        ProfileUI profilePanel = new ProfileUI(); // Pastikan ProfileUI ada
        JPanel ordersPanel = createOrdersPanel();
        JPanel cartPanel = new CartUI(); // Pastikan CartUI ada
        // Berikan 'this' (UserDashboardUI yang mengimplementasikan ViewController) ke FavoritesUI
        favoritesUI = new FavoritesUI(this);

        // Tambahkan panel ke CardLayout
        mainPanel.add(dashboardPanel, "Dashboard");
        mainPanel.add(profilePanel, "Profile");
        mainPanel.add(ordersPanel, "Order");
        mainPanel.add(cartPanel, "Cart");
        mainPanel.add(favoritesUI, "Favorites");

        // Tambahkan komponen ke JFrame
        add(headerPanel, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);
    }

    // --- Implementasi metode dari interface ViewController ---
    @Override
    public void showProductDetail(FavoritesUI.FavoriteItem product) {
        // Hapus ProductDetailUI yang mungkin sudah ada sebelumnya
        for (Component comp : mainPanel.getComponents()) {
            if (comp instanceof ProductDetailUI) {
                mainPanel.remove(comp);
                break;
            }
        }
        
        // Panggil ProductRepository untuk mendapatkan detail lengkap (dengan BLOB gambar)
        FavoritesUI.FavoriteItem fullProductDetails = ProductRepository.getProductById(product.getId());
        if (fullProductDetails == null) {
            JOptionPane.showMessageDialog(this, "Detail produk tidak ditemukan di database.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Buat ProductDetailUI baru dengan detail lengkap, dan berikan 'this' sebagai ViewController
        ProductDetailUI productDetailPanel = new ProductDetailUI(fullProductDetails, this);
        mainPanel.add(productDetailPanel, "ProductDetail");
        cardLayout.show(mainPanel, "ProductDetail");
    }

    @Override
    public void showFavoritesView() {
        cardLayout.show(mainPanel, "Favorites");
    }

    @Override
    public void showDashboardView() {
        cardLayout.show(mainPanel, "Dashboard");
    }

    @Override
    public void showCartView() {
        cardLayout.show(mainPanel, "Cart");
    }

    @Override
    public void showProfileView() {
        cardLayout.show(mainPanel, "Profile");
    }

    @Override
    public void showOrdersView() {
        cardLayout.show(mainPanel, "Order");
    }
    // --- Akhir implementasi ViewController ---

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
        JOptionPane.showMessageDialog(this,
            "Mencari: \"" + searchText + "\"\n\nFungsionalitas pencarian akan diimplementasikan di sini.",
            "Hasil Pencarian",
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void performInstantSearch(String searchText) {
        System.out.println("Pencarian langsung: " + searchText);
    }

    private JPanel createDashboardPanel() {
        JPanel dashboardPanel = new JPanel();
        dashboardPanel.setLayout(new BoxLayout(dashboardPanel, BoxLayout.Y_AXIS));
        dashboardPanel.setBackground(Color.WHITE);

        JPanel bannerPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gradient = new GradientPaint(
                        0, 0, new Color(173, 216, 230),
                        getWidth(), getHeight(), new Color(255, 182, 193));
                g2d.setPaint(gradient);
                g2d.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
            }
        };
        bannerPanel.setLayout(new BorderLayout());
        bannerPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        bannerPanel.setPreferredSize(new Dimension(0, 300));

        JLabel bannerTitle = new JLabel("<html>Beats Solo<br>WIRELESS HEADPHONE</html>");
        bannerTitle.setFont(new Font("Arial", Font.BOLD, 40));
        bannerTitle.setForeground(Color.BLACK);
        bannerTitle.setBorder(new EmptyBorder(20, 20, 0, 0));
        bannerPanel.add(bannerTitle, BorderLayout.WEST);

        JButton shopButton = new JButton("Belanja Kategori");
        shopButton.setBackground(new Color(255, 69, 0));
        shopButton.setForeground(Color.WHITE);
        shopButton.setBorderPainted(false);
        shopButton.setFocusPainted(false);
        shopButton.setFont(new Font("Arial", Font.BOLD, 14));
        shopButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        shopButton.setBorder(new EmptyBorder(10, 20, 10, 20));
        shopButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.setBorder(new EmptyBorder(10, 20, 20, 0));
        buttonPanel.add(shopButton);
        bannerPanel.add(buttonPanel, BorderLayout.SOUTH);

        JPanel categoriesPanel = new JPanel(new GridLayout(2, 3, 15, 15));
        categoriesPanel.setBackground(Color.WHITE);
        categoriesPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

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
        featuresPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

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

        JPanel promoPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(255, 69, 0));
                g2d.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
            }
        };
        promoPanel.setLayout(new BorderLayout());
        promoPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        promoPanel.setPreferredSize(new Dimension(0, 200));

        JLabel promoTitle = new JLabel("<html>DISKON 20%<br>TEMUKAN SENYUM</html>");
        promoTitle.setFont(new Font("Arial", Font.BOLD, 30));
        promoTitle.setForeground(Color.WHITE);
        promoTitle.setBorder(new EmptyBorder(20, 20, 0, 0));
        promoPanel.add(promoTitle, BorderLayout.WEST);

        JButton shopNowButton = new JButton("Belanja Sekarang");
        shopNowButton.setBackground(Color.WHITE);
        shopNowButton.setForeground(Color.BLACK);
        shopNowButton.setBorderPainted(false);
        shopNowButton.setFocusPainted(false);
        shopNowButton.setFont(new Font("Arial", Font.BOLD, 14));
        shopNowButton.setBorder(new EmptyBorder(10, 20, 10, 20));
        shopNowButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        JPanel promoButtonPanel = new JPanel();
        promoButtonPanel.setOpaque(false);
        promoButtonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        promoButtonPanel.setBorder(new EmptyBorder(0, 0, 20, 20));
        promoButtonPanel.add(shopNowButton);
        promoPanel.add(promoButtonPanel, BorderLayout.EAST);

        JPanel bestSellerPanel = new JPanel(new BorderLayout());
        bestSellerPanel.setBackground(Color.WHITE);
        bestSellerPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel bestSellerTitle = new JLabel("Produk Terlaris");
        bestSellerTitle.setFont(new Font("Arial", Font.BOLD, 20));
        bestSellerPanel.add(bestSellerTitle, BorderLayout.NORTH);

        JPanel bestSellerProducts = new JPanel(new GridLayout(1, 4, 15, 15));
        bestSellerProducts.setBackground(Color.WHITE);
        for (int i = 1; i <= 4; i++) {
            JPanel productCard = createProductCard(i, "Produk " + i, 150000 + (i * 50000));
            bestSellerProducts.add(productCard);
        }
        bestSellerPanel.add(bestSellerProducts, BorderLayout.CENTER);

        dashboardPanel.add(bannerPanel);
        dashboardPanel.add(categoriesPanel);
        dashboardPanel.add(featuresPanel);
        dashboardPanel.add(promoPanel);
        dashboardPanel.add(bestSellerPanel);

        JScrollPane scrollPane = new JScrollPane(dashboardPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(null);

        JPanel wrapperPanel = new JPanel(new BorderLayout());
        wrapperPanel.add(scrollPane, BorderLayout.CENTER);
        return wrapperPanel;
    }

    private JPanel createCategoryCard(String name, Color bgColor) {
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

    private JPanel createProductCard(int id, String name, double price) {
        JPanel card = new JPanel();
        card.setLayout(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220), 1));

        JPanel imagePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int r = 180 + (id * 10) % 50;
                int gr = 180 + (id * 15) % 50;
                int b = 180 + (id * 20) % 50;
                GradientPaint gradient = new GradientPaint(
                        0, 0, new Color(r, gr, b),
                        getWidth(), getHeight(), new Color(r - 30, gr - 30, b - 30));
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 24));
                FontMetrics fm = g2d.getFontMetrics();
                String text = "Produk " + id;
                int textWidth = fm.stringWidth(text);
                int textHeight = fm.getHeight();
                g2d.drawString(text, (getWidth() - textWidth) / 2, (getHeight() + textHeight) / 2 - fm.getDescent());
            }
        };
        imagePanel.setPreferredSize(new Dimension(100, 150));

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(Color.WHITE);
        infoPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel lblName = new JLabel(name);
        lblName.setFont(new Font("Arial", Font.BOLD, 14));
        lblName.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblPrice = new JLabel(String.format("Rp %.0f", price));
        lblPrice.setFont(new Font("Arial", Font.PLAIN, 14));
        lblPrice.setForeground(new Color(255, 89, 0));
        lblPrice.setAlignmentX(Component.LEFT_ALIGNMENT);

        infoPanel.add(lblName);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        infoPanel.add(lblPrice);

        card.add(imagePanel, BorderLayout.CENTER);
        card.add(infoPanel, BorderLayout.SOUTH);

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                card.setCursor(new Cursor(Cursor.HAND_CURSOR));
                card.setBorder(BorderFactory.createLineBorder(new Color(255, 89, 0), 2));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220), 1));
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                // Ambil produk lengkap dari database (yang akan mengambil BLOB gambarnya)
                FavoritesUI.FavoriteItem clickedProduct = ProductRepository.getProductById(id);
                if (clickedProduct == null) {
                    // Fallback jika tidak ditemukan di database (penting untuk ID dummy yang belum ada BLOB)
                    // Karena gambar dummy dari DB belum ada, kita bisa pakai placeholder Image kosong
                    clickedProduct = new FavoritesUI.FavoriteItem(
                        id, name, "Deskripsi produk " + name + " (dummy)", price, price + 50000,
                        10, "Baru", "1 Buah", "Merk Dummy", "#FDF8E8", null // List<String> imagePaths kosong
                    );
                }
                showProductDetail(clickedProduct);
            }
        });

        return card;
    }

    private JPanel createOrdersPanel() {
        JPanel ordersPanel = new JPanel(new BorderLayout());
        ordersPanel.setBackground(Color.WHITE);

        JLabel lblNotAvailable = new JLabel("Halaman Pesanan Belum Tersedia", SwingConstants.CENTER);
        lblNotAvailable.setFont(new Font("Arial", Font.BOLD, 18));
        ordersPanel.add(lblNotAvailable, BorderLayout.CENTER);

        return ordersPanel;
    }

    static class IconLabel extends JLabel {
        private Color iconColor;

        public IconLabel(String text, Color color) {
            super(text);
            this.iconColor = color;
            setForeground(color);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(iconColor);
            super.paintComponent(g2d);
            g2d.dispose();
        }
    }
    
    private int getCartItemCount() {
        // Ini akan mengambil dari database Cart nanti
        return 4;
    }
    
    private int getFavItemCount() {
        // Ini akan mengambil dari database Favorites nanti
        return 6;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new UserDashboardUI().setVisible(true);
        });
    }
}