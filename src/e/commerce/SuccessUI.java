package e.commerce;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.CompoundBorder;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.sql.SQLException;
import e.commerce.ProductRepository.Order;
import java.util.List;

public class SuccessUI extends JPanel {

    private ViewController viewController;
    private User currentUser;
    private int orderId; // Untuk menyimpan ID pesanan yang sebenarnya
    private JLabel orderNumberLabel; // Referensi ke label untuk diperbarui

    // Konstanta untuk warna tema
    private static final Color ORANGE_THEME = new Color(255, 69, 0);
    private static final Color LIGHT_GRAY_BACKGROUND = new Color(245, 245, 245);
    private static final Color BORDER_COLOR = new Color(230, 230, 230);
    private static final Color DARK_TEXT_COLOR = new Color(50, 50, 50);
    private static final Color GRAY_TEXT_COLOR = new Color(120, 120, 120);
    private static final Color SUCCESS_YELLOW = new Color(255, 193, 7);
    private static final Color SUCCESS_GREEN = new Color(76, 175, 80);

    // Konstruktor yang dimodifikasi untuk menerima orderId
    public SuccessUI(ViewController viewController, int orderId) {
        this.viewController = viewController;
        this.orderId = orderId; // Simpan ID pesanan yang diterima

        setLayout(new BorderLayout());
        setBackground(LIGHT_GRAY_BACKGROUND);

        this.currentUser = Authentication.getCurrentUser();
        if (this.currentUser == null) {
            System.err.println("SuccessUI: Tidak ada user yang login. Halaman tidak dapat dimuat.");
            JOptionPane.showMessageDialog(this, "Anda harus login untuk melihat halaman ini.", "Error", JOptionPane.ERROR_MESSAGE);
        }

        createComponents();
        loadAndDisplayOrderDetails(); // Muat dan tampilkan detail berdasarkan orderId
    }

    private void createComponents() {
        JPanel topHeaderPanel = createTopHeaderPanel();
        add(topHeaderPanel, BorderLayout.NORTH);

        JPanel mainContentPanel = new JPanel(new GridBagLayout());
        mainContentPanel.setBackground(LIGHT_GRAY_BACKGROUND);
        mainContentPanel.setBorder(new EmptyBorder(40, 40, 40, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;

        JPanel successCardPanel = createSuccessCardPanel();
        mainContentPanel.add(successCardPanel, gbc);

        add(mainContentPanel, BorderLayout.CENTER);
    }

    private JPanel createTopHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(new EmptyBorder(10, 20, 10, 20));

        JPanel leftSection = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftSection.setBackground(Color.WHITE);
        JButton backButton = new JButton("← Kembali"); // Translated
        backButton.setFont(new Font("Arial", Font.BOLD, 14));
        backButton.setForeground(DARK_TEXT_COLOR);
        backButton.setBackground(Color.WHITE);
        backButton.setBorderPainted(false);
        backButton.setFocusPainted(false);
        backButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backButton.addActionListener(e -> {
            if (viewController != null) {
                viewController.showDashboardView();
            }
        });
        leftSection.add(backButton);
        headerPanel.add(leftSection, BorderLayout.WEST);

        JPanel navStepsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        navStepsPanel.setBackground(Color.WHITE);

        String[] steps = {"Keranjang", "Alamat", "Pembayaran", "Selesai"}; // Translated
        String[] iconPaths = {
            "/Resources/Images/cart_icon.png",
            "/Resources/Images/address_icon.png",
            "/Resources/Images/payment_icon.png",
            "/Resources/Images/success_icon.png"
        };

        for (int i = 0; i < steps.length; i++) {
            JPanel stepContainer = new JPanel();
            stepContainer.setLayout(new BoxLayout(stepContainer, BoxLayout.Y_AXIS));
            stepContainer.setBackground(Color.WHITE);
            stepContainer.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel stepIcon = new JLabel();
            try {
                ImageIcon originalIcon = new ImageIcon(getClass().getResource(iconPaths[i]));
                Image originalImage = originalIcon.getImage();
                Image scaledImage = originalImage.getScaledInstance(24, 24, Image.SCALE_SMOOTH);
                stepIcon.setIcon(new ImageIcon(scaledImage));
            } catch (Exception e) {
                System.err.println("Error memuat ikon untuk langkah '" + steps[i] + "': " + e.getMessage()); // Translated
                stepIcon.setText("?");
                stepIcon.setFont(new Font("Arial", Font.PLAIN, 20));
                stepIcon.setForeground(Color.RED);
            }

            stepIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel stepLabel = new JLabel(steps[i]);
            stepLabel.setFont(new Font("Arial", Font.PLAIN, 14));
            stepLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            if (steps[i].equals("Selesai")) { // Translated step name
                if (stepIcon.getIcon() != null) {
                } else {
                    stepIcon.setForeground(ORANGE_THEME);
                }
                stepLabel.setForeground(ORANGE_THEME);
                stepLabel.setFont(new Font("Arial", Font.BOLD, 14));
            } else {
                if (stepIcon.getIcon() != null) {
                } else {
                    stepIcon.setForeground(GRAY_TEXT_COLOR);
                }
                stepLabel.setForeground(GRAY_TEXT_COLOR);
            }
            stepContainer.add(stepIcon);
            stepContainer.add(stepLabel);

            navStepsPanel.add(stepContainer);

            if (i < steps.length - 1) {
                JLabel arrow = new JLabel(">");
                arrow.setFont(new Font("Arial", Font.PLAIN, 18));
                arrow.setForeground(GRAY_TEXT_COLOR);
                navStepsPanel.add(arrow);
            }
        }
        headerPanel.add(navStepsPanel, BorderLayout.CENTER);

        JPanel rightPlaceholderPanel = new JPanel();
        rightPlaceholderPanel.setBackground(Color.WHITE);
        headerPanel.add(rightPlaceholderPanel, BorderLayout.EAST);

        return headerPanel;
    }

    private JPanel createSuccessCardPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            new EmptyBorder(50, 50, 50, 50)
        ));

        JPanel checkmarkContainer = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2d.setColor(SUCCESS_YELLOW);
                g2d.fillOval(10, 10, 60, 60);

                g2d.setColor(Color.WHITE);
                g2d.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2d.drawLine(25, 40, 35, 50);
                g2d.drawLine(35, 50, 55, 30);

                g2d.dispose();
            }
        };
        checkmarkContainer.setPreferredSize(new Dimension(80, 80));
        checkmarkContainer.setMaximumSize(new Dimension(80, 80));
        checkmarkContainer.setBackground(Color.WHITE);
        checkmarkContainer.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(checkmarkContainer);
        panel.add(Box.createVerticalStrut(30));

        JLabel congratsLabel = new JLabel("Selamat"); // Translated
        congratsLabel.setFont(new Font("Arial", Font.BOLD, 28));
        congratsLabel.setForeground(DARK_TEXT_COLOR);
        congratsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(congratsLabel);
        panel.add(Box.createVerticalStrut(20));

        JLabel messageLabel = new JLabel("Pesanan Anda telah berhasil ditempatkan!"); // Translated
        messageLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        messageLabel.setForeground(GRAY_TEXT_COLOR);
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(messageLabel);
        panel.add(Box.createVerticalStrut(40));

        JLabel orderNumberTitle = new JLabel("Nomor Pesanan"); // Translated
        orderNumberTitle.setFont(new Font("Arial", Font.BOLD, 16));
        orderNumberTitle.setForeground(DARK_TEXT_COLOR);
        orderNumberTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(orderNumberTitle);
        panel.add(Box.createVerticalStrut(10));

        orderNumberLabel = new JLabel("Memuat..."); // Translated
        orderNumberLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        orderNumberLabel.setForeground(ORANGE_THEME);
        orderNumberLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(orderNumberLabel);
        panel.add(Box.createVerticalStrut(15));

        JButton viewOrderButton = new JButton("Lihat Pesanan"); // Translated
        viewOrderButton.setBackground(Color.WHITE);
        viewOrderButton.setForeground(ORANGE_THEME);
        viewOrderButton.setBorder(new LineBorder(ORANGE_THEME, 1));
        viewOrderButton.setFocusPainted(false);
        viewOrderButton.setFont(new Font("Arial", Font.BOLD, 14));
        viewOrderButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        viewOrderButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        viewOrderButton.setPreferredSize(new Dimension(150, 40));
        viewOrderButton.setMaximumSize(new Dimension(200, 40));
        viewOrderButton.addActionListener(e -> {
            if (viewController != null) {
                viewController.showOrdersView();
            }
        });
        panel.add(viewOrderButton);
        panel.add(Box.createVerticalStrut(20));

        JButton continueShoppingButton = new JButton("LANJUTKAN BELANJA"); // Translated
        continueShoppingButton.setBackground(ORANGE_THEME);
        continueShoppingButton.setForeground(Color.WHITE);
        continueShoppingButton.setBorderPainted(false);
        continueShoppingButton.setFocusPainted(false);
        continueShoppingButton.setFont(new Font("Arial", Font.BOLD, 14));
        continueShoppingButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        continueShoppingButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        continueShoppingButton.setPreferredSize(new Dimension(200, 40));
        continueShoppingButton.setMaximumSize(new Dimension(250, 40));
        continueShoppingButton.addActionListener(e -> {
            if (viewController != null) {
                viewController.showDashboardView();
            }
        });
        panel.add(continueShoppingButton);

        return panel;
    }

    private void loadAndDisplayOrderDetails() {
        if (orderId == -1) {
            orderNumberLabel.setText("N/A (Pesanan Gagal)"); // Translated
            return;
        }

        try {
            List<Order> orders = ProductRepository.getOrdersForUser(currentUser.getId());
            Order foundOrder = null;
            for (Order order : orders) {
                if (order.getId() == orderId) {
                    foundOrder = order;
                    break;
                }
            }

            if (foundOrder != null) {
                orderNumberLabel.setText(foundOrder.getOrderNumber());
            } else {
                orderNumberLabel.setText("Pesanan #" + orderId + " (Detail tidak ditemukan)"); // Translated
            }
        } catch (SQLException e) {
            System.err.println("Error mengambil detail pesanan untuk SuccessUI: " + e.getMessage()); // Translated
            orderNumberLabel.setText("Error mengambil detail pesanan."); // Translated
            JOptionPane.showMessageDialog(this, "Error mengambil detail pesanan: " + e.getMessage(), "Error Database", JOptionPane.ERROR_MESSAGE); // Translated
        }
    }

    // Metode format mata uang
    private String formatCurrency(double amount) {
        return String.format("Rp %,.2f", amount);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Pesanan Berhasil"); // Translated
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1200, 800);

            User dummyUser = new User(1, "testuser", "hashedpass", "test@example.com", "1234567890123456", "08123456789", null, null, "user", true);
            try {
                java.lang.reflect.Field field = Authentication.class.getDeclaredField("currentUser");
                field.setAccessible(true);
                field.set(null, dummyUser);
                System.out.println("User dummy diatur untuk pengujian di SuccessUI."); // Translated
            } catch (Exception e) {
                System.err.println("Gagal mengatur user dummy untuk pengujian di SuccessUI: " + e.getMessage()); // Translated
            }

            ViewController dummyVC = new ViewController() {
                @Override public void showProductDetail(FavoritesUI.FavoriteItem product) { System.out.println("Dummy: Tampilkan Detail Produk: " + product.getName()); }
                @Override public void showFavoritesView() { System.out.println("Dummy: Tampilkan Tampilan Favorit"); }
                @Override public void showDashboardView() { System.out.println("Dummy: Tampilkan Tampilan Dashboard"); }
                @Override public void showCartView() { System.out.println("Dummy: Tampilkan Tampilan Keranjang"); }
                @Override public void showProfileView() { System.out.println("Dummy: Tampilkan Tampilan Profil"); }
                @Override public void showOrdersView() { System.out.println("Dummy: Tampilkan Tampilan Pesanan"); }
                @Override public void showCheckoutView() { System.out.println("Dummy: Tampilkan Tampilan Checkout (Sukses)"); }
                @Override public void showAddressView(CouponResult couponResult) { System.out.println("Dummy: Tampilkan Tampilan Alamat dengan kupon."); }

                @Override
                public void showPaymentView(AddressUI.Address selectedAddress, AddressUI.ShippingService selectedShippingService, double totalAmount, CouponResult couponResult) {
                    System.out.println("Dummy: Tampilkan Tampilan Pembayaran dengan Alamat dan Kupon.");
                }
                @Override public void showSuccessView(int orderId) {
                    System.out.println("Dummy: Tampilkan Tampilan Sukses (Sukses) dengan ID pesanan: " + orderId);
                }
                @Override
                public void showOrderDetailView(int orderId) {
                    System.out.println("Dummy: Tampilkan Tampilan Detail Pesanan untuk ID: " + orderId);
                }
                @Override
                public void showChatWithSeller(int sellerId, String sellerUsername) {
                    System.out.println("Dummy: Tampilkan Chat dengan Penjual ID: " + sellerId + " (" + sellerUsername + ")");
                }
                @Override public void showProductReviewView(int productId, String productName, String productImage, double productPrice, String sellerName) {
                    System.out.println("Dummy: Tampilkan Tampilan Review Produk");
                }
            };

            SuccessUI successUI = new SuccessUI(dummyVC, 12345);
            frame.add(successUI);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}