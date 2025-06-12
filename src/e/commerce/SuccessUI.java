package e.commerce; // Sesuaikan dengan package Anda

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.CompoundBorder;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random; // Untuk menghasilkan nomor pesanan dummy

public class SuccessUI extends JPanel {

    private ViewController viewController;
    private User currentUser;

    // Constants for theme colors
    private static final Color ORANGE_THEME = new Color(255, 69, 0);
    private static final Color LIGHT_GRAY_BACKGROUND = new Color(245, 245, 245);
    private static final Color BORDER_COLOR = new Color(230, 230, 230);
    private static final Color DARK_TEXT_COLOR = new Color(50, 50, 50);
    private static final Color GRAY_TEXT_COLOR = new Color(120, 120, 120);
    private static final Color SUCCESS_YELLOW = new Color(255, 193, 7); // Yellow background for checkmark
    private static final Color SUCCESS_GREEN = new Color(76, 175, 80); // Hijau untuk sukses

    public SuccessUI(ViewController viewController) {
        this.viewController = viewController;
        setLayout(new BorderLayout());
        setBackground(LIGHT_GRAY_BACKGROUND);

        this.currentUser = Authentication.getCurrentUser();
        if (this.currentUser == null) {
            System.err.println("SuccessUI: Tidak ada user yang login. Halaman tidak dapat dimuat.");
            JOptionPane.showMessageDialog(this, "Anda harus login untuk melihat halaman ini.", "Error", JOptionPane.ERROR_MESSAGE);
        }

        createComponents();
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
        JButton backButton = new JButton("← Back");
        backButton.setFont(new Font("Arial", Font.BOLD, 14));
        backButton.setForeground(DARK_TEXT_COLOR);
        backButton.setBackground(Color.WHITE);
        backButton.setBorderPainted(false);
        backButton.setFocusPainted(false);
        backButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backButton.addActionListener(e -> {
            if (viewController != null) {
                viewController.showDashboardView(); // Kembali ke Dashboard atau halaman sebelumnya
            }
        });
        leftSection.add(backButton);
        headerPanel.add(leftSection, BorderLayout.WEST);

        JPanel navStepsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        navStepsPanel.setBackground(Color.WHITE);

        String[] steps = {"Cart", "Address", "Payment", "Success"};
        String[] icons = {"🛒", "🏠", "💳", "✅"};

        for (int i = 0; i < steps.length; i++) {
            JPanel stepContainer = new JPanel();
            stepContainer.setLayout(new BoxLayout(stepContainer, BoxLayout.Y_AXIS));
            stepContainer.setBackground(Color.WHITE);
            stepContainer.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel stepIcon = new JLabel(icons[i]);
            stepIcon.setFont(new Font("Arial", Font.PLAIN, 20));
            stepIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel stepLabel = new JLabel(steps[i]);
            stepLabel.setFont(new Font("Arial", Font.PLAIN, 14));
            stepLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            if (steps[i].equals("Success")) { // Highlight current step
                stepIcon.setForeground(ORANGE_THEME);
                stepLabel.setForeground(ORANGE_THEME);
                stepLabel.setFont(new Font("Arial", Font.BOLD, 14));
            } else {
                stepIcon.setForeground(GRAY_TEXT_COLOR);
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
        panel.setBorder(new EmptyBorder(50, 50, 50, 50)); // Padding internal card
        
        // Add subtle shadow effect
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            new EmptyBorder(50, 50, 50, 50)
        ));

        // Circular checkmark icon with yellow background
        JPanel checkmarkContainer = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Draw yellow circle
                g2d.setColor(SUCCESS_YELLOW);
                g2d.fillOval(10, 10, 60, 60);
                
                // Draw checkmark
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

        // "Congratulations" text
        JLabel congratsLabel = new JLabel("Congratulations");
        congratsLabel.setFont(new Font("Arial", Font.BOLD, 28));
        congratsLabel.setForeground(DARK_TEXT_COLOR);
        congratsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(congratsLabel);
        panel.add(Box.createVerticalStrut(20));

        // Subtitle message
        JLabel messageLabel = new JLabel("Lorem ipsum is simply dummy text of the printing!");
        messageLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        messageLabel.setForeground(GRAY_TEXT_COLOR);
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(messageLabel);
        panel.add(Box.createVerticalStrut(40));

        // Order Number section
        JLabel orderNumberTitle = new JLabel("Order Number");
        orderNumberTitle.setFont(new Font("Arial", Font.BOLD, 16));
        orderNumberTitle.setForeground(DARK_TEXT_COLOR);
        orderNumberTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(orderNumberTitle);
        panel.add(Box.createVerticalStrut(10));

        JLabel orderNumberLabel = new JLabel(generateDummyOrderNumber());
        orderNumberLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        orderNumberLabel.setForeground(ORANGE_THEME);
        orderNumberLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(orderNumberLabel);
        panel.add(Box.createVerticalStrut(15));

        // View Order button
        JButton viewOrderButton = new JButton("View Order");
        viewOrderButton.setBackground(Color.WHITE);
        viewOrderButton.setForeground(ORANGE_THEME);
        viewOrderButton.setBorder(new LineBorder(ORANGE_THEME, 1));
        viewOrderButton.setFocusPainted(false);
        viewOrderButton.setFont(new Font("Arial", Font.BOLD, 14));
        viewOrderButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        viewOrderButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        viewOrderButton.setPreferredSize(new Dimension(150, 40));
        viewOrderButton.setMaximumSize(new Dimension(200, 40)); // Batasi lebar
        viewOrderButton.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Melihat detail pesanan...", "View Order", JOptionPane.INFORMATION_MESSAGE);
            if (viewController != null) {
                viewController.showOrdersView(); // Contoh: pindah ke halaman pesanan
            }
        });
        panel.add(viewOrderButton);
        panel.add(Box.createVerticalStrut(20));

        // Continue Shopping button
        JButton continueShoppingButton = new JButton("CONTINUE SHOPPING");
        continueShoppingButton.setBackground(ORANGE_THEME);
        continueShoppingButton.setForeground(Color.WHITE);
        continueShoppingButton.setBorderPainted(false);
        continueShoppingButton.setFocusPainted(false);
        continueShoppingButton.setFont(new Font("Arial", Font.BOLD, 14));
        continueShoppingButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        continueShoppingButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        continueShoppingButton.setPreferredSize(new Dimension(200, 40));
        continueShoppingButton.setMaximumSize(new Dimension(250, 40)); // Batasi lebar
        continueShoppingButton.addActionListener(e -> {
            if (viewController != null) {
                viewController.showDashboardView(); // Kembali ke halaman utama/dashboard
            }
        });
        panel.add(continueShoppingButton);

        return panel;
    }

    private String generateDummyOrderNumber() {
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyMMdd");
        String datePart = today.format(formatter);

        Random random = new Random();
        String randomPart1 = String.format("%05d", random.nextInt(100000)); // 5 digit random
        String randomPart2 = String.format("%06d", random.nextInt(1000000)); // 6 digit random

        return datePart + "-" + randomPart1 + "-" + randomPart2;
    }

    // Dummy method untuk format mata uang (jika tidak ada di kelas lain)
    private String formatCurrency(double amount) {
        return String.format("Rp %,.2f", amount);
    }

    // Main method for testing
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Order Success");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1200, 800);

            // Dummy ViewController untuk pengujian mandiri
            ViewController dummyVC = new ViewController() {
                @Override public void showProductDetail(FavoritesUI.FavoriteItem product) { System.out.println("Dummy: Show Product Detail: " + product.getName()); }
                @Override public void showFavoritesView() { System.out.println("Dummy: Show Favorites View"); }
                @Override public void showDashboardView() { System.out.println("Dummy: Show Dashboard View"); }
                @Override public void showCartView() { System.out.println("Dummy: Show Cart View"); }
                @Override public void showProfileView() { System.out.println("Dummy: Show Profile View"); }
                @Override public void showOrdersView() { System.out.println("Dummy: Show Orders View"); }
                @Override public void showCheckoutView() { System.out.println("Dummy: Show Checkout View (Success)"); }
                @Override public void showAddressView() { System.out.println("Dummy: Show Address View (Success)"); }
                @Override public void showPaymentView() { System.out.println("Dummy: Show Payment View (Success)"); }
                @Override public void showSuccessView() { System.out.println("Dummy: Show Success View (Success)"); }
            };

            SuccessUI successUI = new SuccessUI(dummyVC);
            frame.add(successUI);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}