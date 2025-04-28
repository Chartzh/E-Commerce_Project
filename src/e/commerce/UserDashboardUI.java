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
    private JButton btnHamburger, btnBack;
    private JPopupMenu hamburgerMenu;
    
    public UserDashboardUI() {
        User currentUser = Authentication.getCurrentUser();
        if (currentUser == null) {
            //JOptionPane.showMessageDialog(this, "Session tidak valid! Silahkan login kembali.", "Error", JOptionPane.ERROR_MESSAGE);
            LoginUI loginUI = new LoginUI();
            loginUI.setVisible(true);
            dispose();
            return;
        }
        
        setTitle("E-Commerce App - Dashboard");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
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
        
        // Create profile panel
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
        headerPanel.setBackground(new Color(33, 37, 41));
        headerPanel.setPreferredSize(new Dimension(getWidth(), 60));
        headerPanel.setBorder(new EmptyBorder(10, 20, 10, 20));
        
        // Logo
        JLabel lblLogo = new JLabel("Quantra");
        lblLogo.setFont(new Font("Arial", Font.BOLD, 24));
        lblLogo.setForeground(Color.WHITE);
        headerPanel.add(lblLogo, BorderLayout.WEST);
        
        // Welcome message
        JLabel lblWelcome = new JLabel("Selamat Datang, " + currentUser.getUsername());
        lblWelcome.setFont(new Font("Arial", Font.PLAIN, 14));
        lblWelcome.setForeground(Color.WHITE);
        lblWelcome.setHorizontalAlignment(SwingConstants.CENTER);
        headerPanel.add(lblWelcome, BorderLayout.CENTER);
        
        // Hamburger button
        btnHamburger = new JButton("☰");
        btnHamburger.setFont(new Font("Arial", Font.BOLD, 20));
        btnHamburger.setForeground(Color.WHITE);
        btnHamburger.setBackground(new Color(33, 37, 41));
        btnHamburger.setBorderPainted(false);
        btnHamburger.setFocusPainted(false);
        btnHamburger.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Back button (initially not visible)
        btnBack = new JButton("< Kembali");
        btnBack.setFont(new Font("Arial", Font.BOLD, 14));
        btnBack.setForeground(Color.WHITE);
        btnBack.setBackground(new Color(33, 37, 41));
        btnBack.setBorderPainted(false);
        btnBack.setFocusPainted(false);
        btnBack.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnBack.setVisible(false);
        
        // Action listener for back button
        btnBack.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(mainPanel, "Dashboard");
                btnHamburger.setVisible(true);
                btnBack.setVisible(false);
            }
        });
        
        // Panel for hamburger and back buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(new Color(33, 37, 41));
        buttonPanel.add(btnBack);
        buttonPanel.add(btnHamburger);
        
        headerPanel.add(buttonPanel, BorderLayout.EAST);
        
        return headerPanel;
    }
    
    private void createHamburgerMenu() {
        // Buat custom popup menu dengan style yang lebih baik
        hamburgerMenu = new JPopupMenu() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(52, 58, 64));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            }
        };
        
        // Set properti menu
        hamburgerMenu.setBorder(new CompoundBorder(
            new LineBorder(new Color(40, 44, 52), 1, true),
            new EmptyBorder(5, 0, 5, 0)
        ));
        
        // Profile menu item dengan icon
        JMenuItem menuProfile = createMenuItem("Profil", "\uf007");  // Unicode for user icon
        menuProfile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(mainPanel, "Profile");
                btnHamburger.setVisible(false);
                btnBack.setVisible(true);
            }
        });
        
        // Orders menu item dengan icon
        JMenuItem menuOrders = createMenuItem("Pesanan", "\uf07a");  // Unicode for shopping cart icon
        menuOrders.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(mainPanel, "Orders");
                btnHamburger.setVisible(false);
                btnBack.setVisible(true);
            }
        });
        
        // Logout menu item dengan icon
        JMenuItem menuLogout = createMenuItem("Logout", "\uf08b");  // Unicode for sign-out icon
        menuLogout.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int response = JOptionPane.showConfirmDialog(UserDashboardUI.this,
                        "Apakah Anda yakin ingin keluar?",
                        "Konfirmasi",
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
        
        // Separator yang lebih stylish
        JSeparator separator1 = new JSeparator();
        separator1.setBackground(new Color(60, 65, 72));
        separator1.setForeground(new Color(70, 75, 82));
        hamburgerMenu.add(separator1);
        
        hamburgerMenu.add(menuOrders);
        
        // Separator untuk logout
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
        // Buat panel untuk layout item menu
        JPanel itemPanel = new JPanel(new BorderLayout());
        itemPanel.setBackground(new Color(52, 58, 64));
        itemPanel.setBorder(new EmptyBorder(8, 15, 8, 30));
        
        // Icon (Catatan: dalam implementasi sebenarnya, gunakan font icon seperti FontAwesome)
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
        
        // Buat menu item custom
        JMenuItem menuItem = new JMenuItem("") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (getModel().isArmed()) {
                    g2d.setColor(new Color(75, 83, 91));  // Hover background
                } else {
                    g2d.setColor(new Color(52, 58, 64));  // Normal background
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
        dashboardPanel.setLayout(new BorderLayout());
        dashboardPanel.setBackground(new Color(245, 245, 245));
        
        // Title panel
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(new Color(245, 245, 245));
        titlePanel.setBorder(new EmptyBorder(20, 20, 10, 20));
        
        JLabel lblTitle = new JLabel("Produk Populer");
        lblTitle.setFont(new Font("Arial", Font.BOLD, 20));
        titlePanel.add(lblTitle, BorderLayout.WEST);
        
        // Products grid panel
        JPanel productsPanel = new JPanel(new GridLayout(2, 3, 15, 15));
        productsPanel.setBackground(new Color(245, 245, 245));
        productsPanel.setBorder(new EmptyBorder(10, 20, 20, 20));
        
        // Dummy products
        String[] productNames = {
            "Sepatu Sport", "Kemeja Formal", "Jam Tangan", 
            "Celana Jeans", "Tas Ransel", "Kacamata Hitam"
        };
        
        double[] productPrices = {
            299000, 199000, 450000, 
            245000, 189000, 159000
        };
        
        for (int i = 0; i < 6; i++) {
            JPanel productCard = createProductCard(i+1, productNames[i], productPrices[i]);
            productsPanel.add(productCard);
        }
        
        // Add components to dashboard panel
        dashboardPanel.add(titlePanel, BorderLayout.NORTH);
        dashboardPanel.add(productsPanel, BorderLayout.CENTER);
        
        return dashboardPanel;
    }
    
    private JPanel createProductCard(int id, String name, double price) {
        JPanel card = new JPanel();
        card.setLayout(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220), 1));
        
        // Create dummy image with gradient background
        JPanel imagePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Create gradient background for product image
                int r = 180 + (id * 10) % 50;
                int gr = 180 + (id * 15) % 50;
                int b = 180 + (id * 20) % 50;
                
                GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(r, gr, b), 
                    getWidth(), getHeight(), new Color(r-30, gr-30, b-30)
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                
                // Draw product number
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
        
        // Product info panel
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(Color.WHITE);
        infoPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Product name
        JLabel lblName = new JLabel(name);
        lblName.setFont(new Font("Arial", Font.BOLD, 14));
        lblName.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Product price
        JLabel lblPrice = new JLabel(String.format("Rp %.0f", price));
        lblPrice.setFont(new Font("Arial", Font.PLAIN, 14));
        lblPrice.setForeground(new Color(255, 89, 0));
        lblPrice.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Rating stars (sebagai contoh saja)
        JPanel ratingPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        ratingPanel.setBackground(Color.WHITE);
        ratingPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        for (int i = 0; i < 5; i++) {
            JLabel star = new JLabel("★");
            star.setFont(new Font("Arial", Font.PLAIN, 12));
            star.setForeground(i < 4 ? new Color(255, 215, 0) : new Color(200, 200, 200));
            ratingPanel.add(star);
        }
        
        infoPanel.add(lblName);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        infoPanel.add(lblPrice);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 3)));
        infoPanel.add(ratingPanel);
        
        // Add components to card
        card.add(imagePanel, BorderLayout.CENTER);
        card.add(infoPanel, BorderLayout.SOUTH);
        
        // Add hover effect and click functionality
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
                        "Produk: " + name + "\nHarga: Rp " + String.format("%.0f", price), 
                        "Detail Produk", 
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });
        
        return card;
    }
    
    private JPanel createOrdersPanel() {
        JPanel ordersPanel = new JPanel(new BorderLayout());
        ordersPanel.setBackground(Color.WHITE);
        
        JLabel lblNotAvailable = new JLabel("Halaman Pesanan belum tersedia", SwingConstants.CENTER);
        lblNotAvailable.setFont(new Font("Arial", Font.BOLD, 18));
        ordersPanel.add(lblNotAvailable, BorderLayout.CENTER);
        
        return ordersPanel;
    }
    
    // Custom JLabel untuk Icon dengan warna yang bisa dikustomisasi
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
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new UserDashboardUI().setVisible(true);
            }
        });
    }
}