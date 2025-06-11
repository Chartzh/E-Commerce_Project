package e.commerce;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CheckoutUI extends JPanel {
    private ViewController viewController;
    private List<CheckoutItem> checkoutItems;
    private JPanel itemsPanel;
    private JLabel subtotalLabel;
    private JLabel shippingLabel;
    private JLabel totalLabel;
    private JTextField nameField;
    private JTextField phoneField;
    private JTextArea addressArea;
    private JComboBox<String> cityComboBox;
    private JComboBox<String> shippingComboBox;
    private JComboBox<String> paymentComboBox;
    private JButton checkoutButton;
    
    private double subtotal = 0;
    private double shippingCost = 0;
    private NumberFormat currencyFormat;

    public CheckoutUI(ViewController viewController) {
        this.viewController = viewController;
        this.checkoutItems = new ArrayList<>();
        this.currencyFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        
        initializeUI();
        loadCheckoutItems(); // Load items from cart
    }

    private void initializeUI() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // Header Panel
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // Main Content with ScrollPane
        JScrollPane scrollPane = new JScrollPane(createMainContent());
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(new EmptyBorder(20, 30, 20, 30));

        JButton backButton = new JButton("← Kembali");
        backButton.setFont(new Font("Arial", Font.PLAIN, 14));
        backButton.setForeground(new Color(255, 89, 0));
        backButton.setBorderPainted(false);
        backButton.setContentAreaFilled(false);
        backButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backButton.addActionListener(e -> viewController.showCartView());

        JLabel titleLabel = new JLabel("Checkout");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(Color.BLACK);

        headerPanel.add(backButton, BorderLayout.WEST);
        headerPanel.add(titleLabel, BorderLayout.CENTER);

        return headerPanel;
    }

    private JPanel createMainContent() {
        JPanel mainContent = new JPanel(new BorderLayout());
        mainContent.setBackground(Color.WHITE);
        mainContent.setBorder(new EmptyBorder(0, 30, 30, 30));

        // Left Panel - Order Items and Shipping Info
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBackground(Color.WHITE);

        // Order Items Section
        JPanel orderSection = createOrderItemsSection();
        leftPanel.add(orderSection);
        leftPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // Shipping Information Section
        JPanel shippingSection = createShippingInfoSection();
        leftPanel.add(shippingSection);
        leftPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // Payment Method Section
        JPanel paymentSection = createPaymentMethodSection();
        leftPanel.add(paymentSection);

        // Right Panel - Order Summary
        JPanel rightPanel = createOrderSummarySection();

        // Layout with proper spacing
        JPanel contentWrapper = new JPanel(new BorderLayout(30, 0));
        contentWrapper.setBackground(Color.WHITE);
        
        JScrollPane leftScrollPane = new JScrollPane(leftPanel);
        leftScrollPane.setBorder(null);
        leftScrollPane.setPreferredSize(new Dimension(700, 0));
        contentWrapper.add(leftScrollPane, BorderLayout.CENTER);
        contentWrapper.add(rightPanel, BorderLayout.EAST);

        mainContent.add(contentWrapper, BorderLayout.CENTER);
        return mainContent;
    }

    private JPanel createOrderItemsSection() {
        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(Color.WHITE);
        section.setBorder(createSectionBorder("Ringkasan Pesanan"));

        itemsPanel = new JPanel();
        itemsPanel.setLayout(new BoxLayout(itemsPanel, BoxLayout.Y_AXIS));
        itemsPanel.setBackground(Color.WHITE);

        JScrollPane itemsScrollPane = new JScrollPane(itemsPanel);
        itemsScrollPane.setBorder(null);
        itemsScrollPane.setPreferredSize(new Dimension(0, 300));
        section.add(itemsScrollPane, BorderLayout.CENTER);

        return section;
    }

    private JPanel createShippingInfoSection() {
        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(Color.WHITE);
        section.setBorder(createSectionBorder("Informasi Pengiriman"));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;

        // Name Field
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Nama Lengkap:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        nameField = createStyledTextField();
        formPanel.add(nameField, gbc);

        // Phone Field
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        formPanel.add(new JLabel("Nomor Telepon:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        phoneField = createStyledTextField();
        formPanel.add(phoneField, gbc);

        // Address Field
        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        formPanel.add(new JLabel("Alamat Lengkap:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.BOTH; gbc.weightx = 1.0; gbc.weighty = 1.0;
        addressArea = new JTextArea(4, 30);
        addressArea.setFont(new Font("Arial", Font.PLAIN, 14));
        addressArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        addressArea.setLineWrap(true);
        addressArea.setWrapStyleWord(true);
        JScrollPane addressScrollPane = new JScrollPane(addressArea);
        addressScrollPane.setBorder(new LineBorder(new Color(220, 220, 220), 1));
        formPanel.add(addressScrollPane, gbc);

        // City Field
        gbc.gridx = 0; gbc.gridy = 3; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0; gbc.weighty = 0;
        formPanel.add(new JLabel("Kota:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        cityComboBox = new JComboBox<>(new String[]{
            "Jakarta", "Bandung", "Surabaya", "Medan", "Semarang", 
            "Makassar", "Palembang", "Tangerang", "Depok", "Bekasi"
        });
        cityComboBox.setFont(new Font("Arial", Font.PLAIN, 14));
        formPanel.add(cityComboBox, gbc);

        // Shipping Method
        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Metode Pengiriman:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        shippingComboBox = new JComboBox<>(new String[]{
            "Regular (3-5 hari) - Gratis",
            "Express (1-2 hari) - Rp 15.000",
            "Same Day (Hari ini) - Rp 25.000"
        });
        shippingComboBox.setFont(new Font("Arial", Font.PLAIN, 14));
        shippingComboBox.addActionListener(e -> updateShippingCost());
        formPanel.add(shippingComboBox, gbc);

        section.add(formPanel, BorderLayout.CENTER);
        return section;
    }

    private JPanel createPaymentMethodSection() {
        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(Color.WHITE);
        section.setBorder(createSectionBorder("Metode Pembayaran"));

        JPanel paymentPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        paymentPanel.setBackground(Color.WHITE);

        paymentComboBox = new JComboBox<>(new String[]{
            "Transfer Bank (BCA, Mandiri, BRI)",
            "E-Wallet (GoPay, OVO, Dana)",
            "Kartu Kredit/Debit",
            "COD (Bayar di Tempat)"
        });
        paymentComboBox.setFont(new Font("Arial", Font.PLAIN, 14));
        paymentComboBox.setPreferredSize(new Dimension(300, 35));

        paymentPanel.add(new JLabel("Pilih Metode Pembayaran:"));
        paymentPanel.add(paymentComboBox);

        section.add(paymentPanel, BorderLayout.CENTER);
        return section;
    }

    private JPanel createOrderSummarySection() {
        JPanel section = new JPanel(new BorderLayout());
        section.setBackground(Color.WHITE);
        section.setBorder(createSectionBorder("Ringkasan Pembayaran"));
        section.setPreferredSize(new Dimension(350, 0));

        JPanel summaryPanel = new JPanel();
        summaryPanel.setLayout(new BoxLayout(summaryPanel, BoxLayout.Y_AXIS));
        summaryPanel.setBackground(Color.WHITE);
        summaryPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Subtotal
        JPanel subtotalPanel = createSummaryRow("Subtotal:", "Rp 0");
        subtotalLabel = (JLabel) subtotalPanel.getComponent(1);
        summaryPanel.add(subtotalPanel);
        summaryPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Shipping
        JPanel shippingPanel = createSummaryRow("Ongkos Kirim:", "Gratis");
        shippingLabel = (JLabel) shippingPanel.getComponent(1);
        summaryPanel.add(shippingPanel);
        summaryPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Separator
        JSeparator separator = new JSeparator();
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        summaryPanel.add(separator);
        summaryPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Total
        JPanel totalPanel = createSummaryRow("Total:", "Rp 0");
        totalPanel.getComponent(0).setFont(new Font("Arial", Font.BOLD, 16));
        totalPanel.getComponent(1).setFont(new Font("Arial", Font.BOLD, 16));
        ((JLabel) totalPanel.getComponent(1)).setForeground(new Color(255, 89, 0));
        totalLabel = (JLabel) totalPanel.getComponent(1);
        summaryPanel.add(totalPanel);
        summaryPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // Checkout Button
        checkoutButton = new JButton("Buat Pesanan");
        checkoutButton.setBackground(new Color(255, 89, 0));
        checkoutButton.setForeground(Color.WHITE);
        checkoutButton.setFont(new Font("Arial", Font.BOLD, 16));
        checkoutButton.setBorderPainted(false);
        checkoutButton.setFocusPainted(false);
        checkoutButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        checkoutButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        checkoutButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        checkoutButton.addActionListener(this::processCheckout);

        // Hover effect for button
        checkoutButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                checkoutButton.setBackground(new Color(230, 70, 0));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                checkoutButton.setBackground(new Color(255, 89, 0));
            }
        });

        summaryPanel.add(checkoutButton);
        summaryPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Security info
        JLabel securityLabel = new JLabel("<html><center><small>🔒 Transaksi Aman & Terpercaya</small></center></html>");
        securityLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        securityLabel.setForeground(Color.GRAY);
        securityLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        summaryPanel.add(securityLabel);

        section.add(summaryPanel, BorderLayout.CENTER);
        return section;
    }

    private JPanel createSummaryRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(Color.WHITE);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));

        JLabel labelComponent = new JLabel(label);
        labelComponent.setFont(new Font("Arial", Font.PLAIN, 14));

        JLabel valueComponent = new JLabel(value);
        valueComponent.setFont(new Font("Arial", Font.BOLD, 14));

        row.add(labelComponent, BorderLayout.WEST);
        row.add(valueComponent, BorderLayout.EAST);

        return row;
    }

    private TitledBorder createSectionBorder(String title) {
        TitledBorder border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(230, 230, 230), 1),
            title
        );
        border.setTitleFont(new Font("Arial", Font.BOLD, 16));
        border.setTitleColor(new Color(50, 50, 50));
        return border;
    }

    private JTextField createStyledTextField() {
        JTextField field = new JTextField();
        field.setFont(new Font("Arial", Font.PLAIN, 14));
        field.setBorder(new EmptyBorder(10, 10, 10, 10));
        field.setPreferredSize(new Dimension(0, 35));
        
        // Add border
        field.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(220, 220, 220), 1),
            new EmptyBorder(8, 12, 8, 12)
        ));
        
        return field;
    }

    private void loadCheckoutItems() {
        // Simulate loading items from cart
        // Replace this with actual cart data loading
        checkoutItems.clear();
        
        // Example items - replace with actual cart items
        checkoutItems.add(new CheckoutItem(1, "Beats Solo Wireless Headphone", 899000, 2, "/Resources/Images/headphone.png"));
        checkoutItems.add(new CheckoutItem(2, "Apple MacBook Pro 13\"", 18999000, 1, "/Resources/Images/laptop.png"));
        checkoutItems.add(new CheckoutItem(3, "Samsung Galaxy Watch", 2799000, 1, "/Resources/Images/watch.png"));
        
        updateItemsDisplay();
        calculateTotals();
    }

    private void updateItemsDisplay() {
        itemsPanel.removeAll();
        
        for (CheckoutItem item : checkoutItems) {
            JPanel itemCard = createCheckoutItemCard(item);
            itemsPanel.add(itemCard);
            itemsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        }
        
        itemsPanel.revalidate();
        itemsPanel.repaint();
    }

    private JPanel createCheckoutItemCard(CheckoutItem item) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(240, 240, 240), 1),
            new EmptyBorder(15, 15, 15, 15)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        // Product Image (placeholder)
        JPanel imagePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(245, 245, 245));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2d.setColor(Color.GRAY);
                g2d.setFont(new Font("Arial", Font.PLAIN, 10));
                String text = "IMG";
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(text)) / 2;
                int y = (getHeight() + fm.getAscent()) / 2;
                g2d.drawString(text, x, y);
            }
        };
        imagePanel.setPreferredSize(new Dimension(70, 70));

        // Product Info
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(Color.WHITE);

        JLabel nameLabel = new JLabel(item.getName());
        nameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel priceLabel = new JLabel(String.format("Rp %,.0f", item.getPrice()));
        priceLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        priceLabel.setForeground(new Color(255, 89, 0));
        priceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel quantityLabel = new JLabel("Jumlah: " + item.getQuantity());
        quantityLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        quantityLabel.setForeground(Color.GRAY);
        quantityLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        infoPanel.add(nameLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        infoPanel.add(priceLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        infoPanel.add(quantityLabel);

        // Total Price
        JLabel totalLabel = new JLabel(String.format("Rp %,.0f", item.getTotalPrice()));
        totalLabel.setFont(new Font("Arial", Font.BOLD, 16));
        totalLabel.setForeground(new Color(255, 89, 0));

        card.add(imagePanel, BorderLayout.WEST);
        card.add(Box.createRigidArea(new Dimension(15, 0)), BorderLayout.CENTER);
        card.add(infoPanel, BorderLayout.CENTER);
        card.add(totalLabel, BorderLayout.EAST);

        return card;
    }

    private void updateShippingCost() {
        String selectedShipping = (String) shippingComboBox.getSelectedItem();
        if (selectedShipping.contains("Express")) {
            shippingCost = 15000;
        } else if (selectedShipping.contains("Same Day")) {
            shippingCost = 25000;
        } else {
            shippingCost = 0;
        }
        calculateTotals();
    }

    private void calculateTotals() {
        subtotal = checkoutItems.stream()
                .mapToDouble(CheckoutItem::getTotalPrice)
                .sum();
        
        double total = subtotal + shippingCost;
        
        subtotalLabel.setText(String.format("Rp %,.0f", subtotal));
        
        if (shippingCost == 0) {
            shippingLabel.setText("Gratis");
        } else {
            shippingLabel.setText(String.format("Rp %,.0f", shippingCost));
        }
        
        totalLabel.setText(String.format("Rp %,.0f", total));
    }

    private void processCheckout(ActionEvent e) {
        // Validate required fields
        if (!validateForm()) {
            return;
        }

        // Show loading state
        checkoutButton.setText("Memproses...");
        checkoutButton.setEnabled(false);

        // Simulate processing time
        Timer timer = new Timer(2000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Show success dialog
                showSuccessDialog();
                
                // Reset button
                checkoutButton.setText("Buat Pesanan");
                checkoutButton.setEnabled(true);
                
                ((Timer) e.getSource()).stop();
            }
        });
        timer.start();
    }

    private boolean validateForm() {
        StringBuilder errors = new StringBuilder();

        if (nameField.getText().trim().isEmpty()) {
            errors.append("- Nama lengkap harus diisi\n");
        }

        if (phoneField.getText().trim().isEmpty()) {
            errors.append("- Nomor telepon harus diisi\n");
        }

        if (addressArea.getText().trim().isEmpty()) {
            errors.append("- Alamat lengkap harus diisi\n");
        }

        if (checkoutItems.isEmpty()) {
            errors.append("- Tidak ada item untuk di-checkout\n");
        }

        if (errors.length() > 0) {
            JOptionPane.showMessageDialog(
                this,
                "Mohon lengkapi data berikut:\n\n" + errors.toString(),
                "Data Tidak Lengkap",
                JOptionPane.WARNING_MESSAGE
            );
            return false;
        }

        return true;
    }

    private void showSuccessDialog() {
        String orderNumber = "QTR" + System.currentTimeMillis();
        
        JDialog successDialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this), "Pesanan Berhasil", true);
        successDialog.setSize(400, 300);
        successDialog.setLocationRelativeTo(this);
        
        JPanel dialogPanel = new JPanel();
        dialogPanel.setLayout(new BoxLayout(dialogPanel, BoxLayout.Y_AXIS));
        dialogPanel.setBackground(Color.WHITE);
        dialogPanel.setBorder(new EmptyBorder(30, 30, 30, 30));
        
        // Success icon (using text)
        JLabel iconLabel = new JLabel("✓");
        iconLabel.setFont(new Font("Arial", Font.BOLD, 48));
        iconLabel.setForeground(new Color(34, 197, 94));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel titleLabel = new JLabel("Pesanan Berhasil Dibuat!");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel orderLabel = new JLabel("Nomor Pesanan: " + orderNumber);
        orderLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        orderLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel infoLabel = new JLabel("<html><center>Terima kasih telah berbelanja!<br>Kami akan segera memproses pesanan Anda.</center></html>");
        infoLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        infoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        infoLabel.setForeground(Color.GRAY);
        
        JButton okButton = new JButton("OK");
        okButton.setBackground(new Color(255, 89, 0));
        okButton.setForeground(Color.WHITE);
        okButton.setBorderPainted(false);
        okButton.setFocusPainted(false);
        okButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        okButton.setPreferredSize(new Dimension(100, 35));
        okButton.addActionListener(ev -> {
            successDialog.dispose();
            viewController.showDashboardView(); // Return to dashboard
        });
        
        dialogPanel.add(iconLabel);
        dialogPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        dialogPanel.add(titleLabel);
        dialogPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        dialogPanel.add(orderLabel);
        dialogPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        dialogPanel.add(infoLabel);
        dialogPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        dialogPanel.add(okButton);
        
        successDialog.add(dialogPanel);
        successDialog.setVisible(true);
    }

    // Inner class for checkout items
    public static class CheckoutItem {
        private int id;
        private String name;
        private double price;
        private int quantity;
        private String imagePath;

        public CheckoutItem(int id, String name, double price, int quantity, String imagePath) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.quantity = quantity;
            this.imagePath = imagePath;
        }

        // Getters
        public int getId() { return id; }
        public String getName() { return name; }
        public double getPrice() { return price; }
        public int getQuantity() { return quantity; }
        public String getImagePath() { return imagePath; }
        public double getTotalPrice() { return price * quantity; }
    }
}