package e.commerce;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;

public class UserDashboardUI extends JFrame {
    private JPanel contentPanel;
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private JButton btnHamburger; // Reintroduce hamburger button
    private JPopupMenu hamburgerMenu; // Reintroduce hamburger menu

    public UserDashboardUI() {
        User currentUser = Authentication.getCurrentUser();
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
        
        // Set ikon
        IconUtil.setIcon(this);

        // Main layout
        setLayout(new BorderLayout());

        // Create main panel with CardLayout
        mainPanel = new JPanel();
        cardLayout = new CardLayout();
        mainPanel.setLayout(cardLayout);

        // Create header panel
        JPanel headerPanel = createHeaderPanel(currentUser);

        // Create dashboard panel
        JPanel dashboardPanel = createDashboardPanel();

        // Create profile panel (placeholder)
        ProfileUI profilePanel = new ProfileUI();

        // Create orders panel (placeholder)
        JPanel ordersPanel = createOrdersPanel();

        // Add panels to the card layout
        mainPanel.add(dashboardPanel, "Dashboard");
        mainPanel.add(profilePanel, "Profile");
        mainPanel.add(ordersPanel, "Order");

        // Add components to frame
        add(headerPanel, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);

        // Create hamburger menu
        createHamburgerMenu();
    }

    private JPanel createHeaderPanel(User currentUser) {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setPreferredSize(new Dimension(getWidth(), 60));
        headerPanel.setBorder(new EmptyBorder(10, 20, 10, 20));

        // Logo
        /*JLabel lblLogo = new JLabel("Quantra");
        lblLogo.setFont(new Font("Arial", Font.BOLD, 20));
        lblLogo.setForeground(new Color(255, 99, 71)); // Tomato color
        headerPanel.add(lblLogo, BorderLayout.WEST);
`       */
        
        // Logo with resized image
        JLabel lblLogo = new JLabel();
        ImageIcon logoIcon = new ImageIcon(getClass().getResource("/Resources/Images/Logo.png")); // Ensure image is in src/resources
        // Resize image to 100x50 pixels (adjust as needed)
        Image scaledImage = logoIcon.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
        lblLogo.setIcon(new ImageIcon(scaledImage));
        headerPanel.add(lblLogo, BorderLayout.WEST);

        // Navigation
        /*JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        navPanel.setBackground(Color.WHITE);
        String[] navItems = {"Home", "Shop", "About Us", "Blog", "Contact Us"};
        for (String item : navItems) {
            JLabel navLabel = new JLabel(item);
            navLabel.setFont(new Font("Arial", Font.PLAIN, 14));
            navLabel.setForeground(Color.BLACK);
            navLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            navPanel.add(navLabel);
        }
        headerPanel.add(navPanel, BorderLayout.CENTER);
        */

        // Right side (Hamburger, Login, and Cart)
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.setBackground(Color.WHITE);

        // Hamburger button
        btnHamburger = new JButton("☰");
        btnHamburger.setFont(new Font("Arial", Font.BOLD, 20));
        btnHamburger.setForeground(Color.BLACK);
        btnHamburger.setBackground(Color.WHITE);
        btnHamburger.setBorderPainted(false);
        btnHamburger.setFocusPainted(false);
        btnHamburger.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel lblLogin = new JLabel("Login");
        lblLogin.setFont(new Font("Arial", Font.PLAIN, 14));
        lblLogin.setForeground(Color.BLACK);
        lblLogin.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel lblCart = new JLabel("🛒 0");
        lblCart.setFont(new Font("Arial", Font.PLAIN, 14));
        lblCart.setForeground(Color.BLACK);
        lblCart.setCursor(new Cursor(Cursor.HAND_CURSOR));

        rightPanel.add(btnHamburger);
        rightPanel.add(Box.createRigidArea(new Dimension(20, 0)));
        rightPanel.add(lblLogin);
        rightPanel.add(Box.createRigidArea(new Dimension(20, 0)));
        rightPanel.add(lblCart);
        headerPanel.add(rightPanel, BorderLayout.EAST);

        return headerPanel;
    }

    private void createHamburgerMenu() {
        // Create custom popup menu with a styled look
        hamburgerMenu = new JPopupMenu() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(52, 58, 64));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            }
        };

        // Set menu properties
        hamburgerMenu.setBorder(new CompoundBorder(
                new LineBorder(new Color(40, 44, 52), 1, true),
                new EmptyBorder(5, 0, 5, 0)
        ));

        // Profile menu item
        JMenuItem menuProfile = createMenuItem("Profile", "\uf007"); // Unicode for user icon
        menuProfile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(mainPanel, "Profile");
            }
        });

        // Orders menu item
        JMenuItem menuOrders = createMenuItem("Orders", "\uf0ae"); // Unicode for tasks icon
        menuOrders.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(mainPanel, "Order");
            }
        });

        // Logout menu item
        JMenuItem menuLogout = createMenuItem("Logout", "\uf08b"); // Unicode for sign-out icon
        menuLogout.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int response = JOptionPane.showConfirmDialog(UserDashboardUI.this,
                        "Are you sure you want to logout?",
                        "Confirm",
                        JOptionPane.YES_NO_OPTION);

                if (response == JOptionPane.YES_OPTION) {
                    Authentication.logout();
                    LoginUI loginUI = new LoginUI();
                    loginUI.setVisible(true);
                    dispose();
                }
            }
        });

        // Add menu items to popup menu
        hamburgerMenu.add(menuProfile);

        // Separator
        JSeparator separator1 = new JSeparator();
        separator1.setBackground(new Color(60, 65, 72));
        separator1.setForeground(new Color(70, 75, 82));
        hamburgerMenu.add(separator1);

        hamburgerMenu.add(menuOrders);

        // Separator for logout
        JSeparator separator2 = new JSeparator();
        separator2.setBackground(new Color(60, 65, 72));
        separator2.setForeground(new Color(70, 75, 82));
        hamburgerMenu.add(separator2);

        hamburgerMenu.add(menuLogout);

        // Hamburger button action
        btnHamburger.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                hamburgerMenu.show(btnHamburger, -hamburgerMenu.getPreferredSize().width + btnHamburger.getWidth(), btnHamburger.getHeight());
            }
        });
    }

    private JMenuItem createMenuItem(String text, String iconCode) {
        // Create panel for menu item layout
        JPanel itemPanel = new JPanel(new BorderLayout());
        itemPanel.setBackground(new Color(52, 58, 64));
        itemPanel.setBorder(new EmptyBorder(8, 15, 8, 30));

        // Icon (using unicode as placeholder; in practice, use a font like FontAwesome)
        JLabel iconLabel = new JLabel(iconCode);
        iconLabel.setForeground(Color.WHITE);
        iconLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        iconLabel.setPreferredSize(new Dimension(20, 16));

        // Text
        JLabel textLabel = new JLabel(text);
        textLabel.setForeground(Color.WHITE);
        textLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        textLabel.setBorder(new EmptyBorder(0, 10, 0, 0));

        itemPanel.add(iconLabel, BorderLayout.WEST);
        itemPanel.add(textLabel, BorderLayout.CENTER);

        // Create custom menu item
        JMenuItem menuItem = new JMenuItem("") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getModel().isArmed()) {
                    g2d.setColor(new Color(75, 83, 91)); // Hover background
                } else {
                    g2d.setColor(new Color(52, 58, 64)); // Normal background
                }
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };

        menuItem.setLayout(new BorderLayout());
        menuItem.add(itemPanel);
        menuItem.setBorderPainted(false);
        menuItem.setBorder(null);
        menuItem.setPreferredSize(new Dimension(180, 36));

        return menuItem;
    }

    private JPanel createDashboardPanel() {
        JPanel dashboardPanel = new JPanel();
        dashboardPanel.setLayout(new BoxLayout(dashboardPanel, BoxLayout.Y_AXIS));
        dashboardPanel.setBackground(Color.WHITE);

        // Banner Section
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

        JButton shopButton = new JButton("Shop Category");
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

        JLabel bannerDesc = new JLabel("<html>There are many variations passages of lorem ipsum available, but the majority have suffered alteration</html>");
        bannerDesc.setFont(new Font("Arial", Font.PLAIN, 12));
        bannerDesc.setForeground(Color.DARK_GRAY);
        bannerDesc.setBorder(new EmptyBorder(0, 0, 20, 20));
        bannerPanel.add(bannerDesc, BorderLayout.EAST);

        // Product Categories Section
        JPanel categoriesPanel = new JPanel(new GridLayout(2, 3, 15, 15));
        categoriesPanel.setBackground(Color.WHITE);
        categoriesPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        String[] categoryNames = {"Earphone", "Wear Gadget", "Laptop", "Gaming Console", "VR Oculus", "Speaker"};
        Color[] backgroundColors = {
                new Color(255, 69, 0), // Red
                new Color(255, 215, 0), // Yellow
                new Color(255, 99, 71), // Tomato
                new Color(240, 248, 255), // AliceBlue
                new Color(50, 205, 50), // LimeGreen
                new Color(135, 206, 250) // SkyBlue
        };

        for (int i = 0; i < categoryNames.length; i++) {
            JPanel categoryCard = createCategoryCard(categoryNames[i], backgroundColors[i]);
            categoriesPanel.add(categoryCard);
        }

        // Features Section
        JPanel featuresPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 50, 10));
        featuresPanel.setBackground(Color.WHITE);
        featuresPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        String[] featureTexts = {
                "Free Shipping On Order",
                "Money Back Guarantee",
                "Online Support 24/7",
                "Secure Payment"
        };
        for (String text : featureTexts) {
            JLabel featureLabel = new JLabel("<html><center><img src='' width='30' height='30'><br>" + text + "</center></html>");
            featureLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            featureLabel.setForeground(Color.BLACK);
            featuresPanel.add(featureLabel);
        }

        // Promotional Banner
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

        JLabel promoTitle = new JLabel("<html>20% OFF<br>FIND SMILE</html>");
        promoTitle.setFont(new Font("Arial", Font.BOLD, 30));
        promoTitle.setForeground(Color.WHITE);
        promoTitle.setBorder(new EmptyBorder(20, 20, 0, 0));
        promoPanel.add(promoTitle, BorderLayout.WEST);

        JButton shopNowButton = new JButton("Shop");
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

        // Best Seller Section
        JPanel bestSellerPanel = new JPanel(new BorderLayout());
        bestSellerPanel.setBackground(Color.WHITE);
        bestSellerPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel bestSellerTitle = new JLabel("Best Seller Products");
        bestSellerTitle.setFont(new Font("Arial", Font.BOLD, 20));
        bestSellerPanel.add(bestSellerTitle, BorderLayout.NORTH);

        JPanel bestSellerProducts = new JPanel(new GridLayout(1, 4, 15, 15));
        bestSellerProducts.setBackground(Color.WHITE);
        for (int i = 1; i <= 4; i++) {
            JPanel productCard = createProductCard(i, "Product " + i, 150000 + (i * 50000));
            bestSellerProducts.add(productCard);
        }
        bestSellerPanel.add(bestSellerProducts, BorderLayout.CENTER);

        // Add all sections to dashboard panel
        dashboardPanel.add(bannerPanel);
        dashboardPanel.add(categoriesPanel);
        dashboardPanel.add(featuresPanel);
        dashboardPanel.add(promoPanel);
        dashboardPanel.add(bestSellerPanel);

        // Wrap in a scroll pane
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

        JButton browseButton = new JButton("Browse");
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
                String text = "Product " + id;
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
                JOptionPane.showMessageDialog(UserDashboardUI.this,
                        "Product: " + name + "\nPrice: Rp " + String.format("%.0f", price),
                        "Product Details",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });

        return card;
    }

    private JPanel createOrdersPanel() {
        JPanel ordersPanel = new JPanel(new BorderLayout());
        ordersPanel.setBackground(Color.WHITE);

        JLabel lblNotAvailable = new JLabel("Orders Page Not Available", SwingConstants.CENTER);
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new UserDashboardUI().setVisible(true);
        });
    }
}