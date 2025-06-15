package e.commerce;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.CompoundBorder;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.sql.SQLException;
import e.commerce.AddressUI.Address;
import e.commerce.AddressUI.ShippingService;

import e.commerce.ProductRepository;
import e.commerce.ProductRepository.CartItem;

public class CartUI extends JPanel {
    private JPanel cartItemsContainer;
    private JLabel totalMRPLabel;
    private JLabel discountOnMRPLabel;
    private JLabel couponSavingsLabel;
    private JLabel applicableGSTLabel;
    private JLabel deliveryLabel;
    private JLabel finalTotalLabel;
    private double totalMRP = 0;
    private double discountOnMRP = 0;
    private double couponSavings = 0;
    private double applicableGST = 0;
    private double deliveryCharge = 0;
    private List<CartItem> cartItems;
    private JButton checkoutButton;
    private ViewController viewController;
    private JLabel myCartTitle;

    // Menghilangkan referensi ke alamat karena akan diurus di halaman terpisah
    // private JLabel currentAddressLabel;
    // private List<Address> savedAddresses;
    // private Address selectedAddress;

    private User currentUser;

    private static final Color ORANGE_THEME = new Color(255, 69, 0);
    private static final Color LIGHT_GRAY_BACKGROUND = new Color(245, 245, 245);
    private static final Color BORDER_COLOR = new Color(230, 230, 230);
    private static final Color DARK_TEXT_COLOR = new Color(50, 50, 50);
    private static final Color GRAY_TEXT_COLOR = new Color(120, 120, 120);

    // Menghilangkan inner class Address karena tidak lagi dibutuhkan di UI ini
    /*
    private static class Address {
        String label;
        String fullAddress;

        public Address(String label, String fullAddress) {
            this.label = label;
            this.fullAddress = fullAddress;
        }

        @Override
        public String toString() {
            return label + ": " + fullAddress;
        }
    }
    */

    public CartUI(ViewController viewController) {
        this.viewController = viewController;
        setLayout(new BorderLayout());
        setBackground(LIGHT_GRAY_BACKGROUND);

        this.currentUser = Authentication.getCurrentUser();
        if (this.currentUser == null) {
            System.err.println("CartUI: Tidak ada user yang login. Keranjang tidak dapat dimuat.");
            JOptionPane.showMessageDialog(this, "Anda harus login untuk melihat keranjang belanja.", "Error", JOptionPane.ERROR_MESSAGE);
        }

        loadCartItemsFromDatabase();
        // initAddresses(); // Tidak perlu menginisialisasi alamat lagi
        createComponents();
        updateTotals();
    }

    private void loadCartItemsFromDatabase() {
        if (currentUser != null) {
            cartItems = ProductRepository.getCartItemsForUser(currentUser.getId());
            System.out.println("DEBUG CartUI: Memuat " + cartItems.size() + " item dari database untuk user ID: " + currentUser.getId());
        } else {
            cartItems = new ArrayList<>();
            System.out.println("DEBUG CartUI: Tidak ada user yang login, keranjang kosong.");
        }
        cartItems.forEach(item -> item.setSelected(true));
    }

    // Menghilangkan metode initAddresses()
    /*
    private void initAddresses() {
        savedAddresses = new ArrayList<>();
        savedAddresses.add(new Address("Rumah", "Jl. Kenangan Indah No. 12, Bekasi, Jawa Barat 17111"));
        savedAddresses.add(new Address("Kantor", "Perkantoran ABC, Tower 3, Lt. 15, Jakarta Pusat 10220"));
        savedAddresses.add(new Address("Ruang Kreatif", "Jl. Seni Rupa No. 5, Bandung, Jawa Barat 40132"));

        if (!savedAddresses.isEmpty()) {
            selectedAddress = savedAddresses.get(0);
        }
    }
    */

    private void createComponents() {
        JPanel topHeaderPanel = createTopHeaderPanel();
        add(topHeaderPanel, BorderLayout.NORTH);

        JPanel mainContentPanel = new JPanel(new GridBagLayout());
        mainContentPanel.setBackground(LIGHT_GRAY_BACKGROUND);
        mainContentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(10, 10, 10, 10);

        JPanel leftColumnPanel = new JPanel();
        leftColumnPanel.setLayout(new BoxLayout(leftColumnPanel, BoxLayout.Y_AXIS));
        leftColumnPanel.setBackground(LIGHT_GRAY_BACKGROUND);

        JPanel offersPanel = createOfferCouponPanel("Available Offers");
        offersPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        leftColumnPanel.add(offersPanel);
        leftColumnPanel.add(Box.createVerticalStrut(15));

        JPanel couponsPanel = createOfferCouponPanel("Apply Coupons");
        couponsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        leftColumnPanel.add(couponsPanel);
        leftColumnPanel.add(Box.createVerticalStrut(15));

        JPanel myCartSectionPanel = new JPanel(new BorderLayout());
        myCartSectionPanel.setBackground(Color.WHITE);
        myCartSectionPanel.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        myCartSectionPanel.setPreferredSize(new Dimension(650, 400));
        myCartSectionPanel.setMinimumSize(new Dimension(400, 200));
        myCartSectionPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        myCartTitle = new JLabel("My Cart (" + cartItems.size() + ")");
        myCartTitle.setFont(new Font("Arial", Font.BOLD, 18));
        myCartTitle.setForeground(DARK_TEXT_COLOR);
        myCartTitle.setBorder(new EmptyBorder(15, 20, 15, 20));
        myCartSectionPanel.add(myCartTitle, BorderLayout.NORTH);

        cartItemsContainer = new JPanel();
        cartItemsContainer.setLayout(new BoxLayout(cartItemsContainer, BoxLayout.Y_AXIS));
        cartItemsContainer.setBackground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(cartItemsContainer);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(Color.WHITE);
        myCartSectionPanel.add(scrollPane, BorderLayout.CENTER);

        leftColumnPanel.add(myCartSectionPanel);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.7; // Sesuaikan bobot agar kolom kiri lebih dominan jika kolom kanan menyusut
        gbc.weighty = 1.0;
        mainContentPanel.add(leftColumnPanel, gbc);

        // Right Column (Hanya Payment Summary)
        JPanel rightColumnPanel = new JPanel();
        rightColumnPanel.setLayout(new BoxLayout(rightColumnPanel, BoxLayout.Y_AXIS));
        rightColumnPanel.setBackground(LIGHT_GRAY_BACKGROUND);

        // Menghilangkan panel Delivery Address
        // JPanel deliveryAddressPanel = createDeliveryAddressPanel();
        // rightColumnPanel.add(deliveryAddressPanel);
        // rightColumnPanel.add(Box.createVerticalStrut(15));

        JPanel paymentSummaryPanel = createPaymentSummaryPanel();
        // Set maximum size for payment summary to control its height
        paymentSummaryPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));
        paymentSummaryPanel.setMinimumSize(new Dimension(300, 280)); // Ensure it has a minimum width and height
        paymentSummaryPanel.setPreferredSize(new Dimension(350, 280)); // A preferred size for initial layout
        rightColumnPanel.add(paymentSummaryPanel);
        rightColumnPanel.add(Box.createVerticalGlue()); // Push payment summary to top if room allows

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.3; // Kolom kanan mengambil 30% sisa ruang
        gbc.weighty = 1.0;
        mainContentPanel.add(rightColumnPanel, gbc);

        add(mainContentPanel, BorderLayout.CENTER);

        refreshCartItems();
        updateTotals();
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
                viewController.showDashboardView();
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

            if (steps[i].equals("Cart")) {
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

        // Empty placeholder for right section
        JPanel rightPlaceholderPanel = new JPanel();
        rightPlaceholderPanel.setBackground(Color.WHITE);
        headerPanel.add(rightPlaceholderPanel, BorderLayout.EAST);

        return headerPanel;
    }

    private JPanel createOfferCouponPanel(String title) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new LineBorder(BORDER_COLOR, 1, true));
        panel.setPreferredSize(new Dimension(panel.getPreferredSize().width, 50));
        panel.setMinimumSize(panel.getPreferredSize());
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(Color.WHITE);
        titlePanel.setBorder(new EmptyBorder(10, 15, 10, 15));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 13));
        titleLabel.setForeground(DARK_TEXT_COLOR);
        titlePanel.add(titleLabel, BorderLayout.WEST);

        JLabel arrowIcon = new JLabel("▼");
        arrowIcon.setFont(new Font("Arial", Font.BOLD, 11));
        arrowIcon.setForeground(ORANGE_THEME);
        titlePanel.add(arrowIcon, BorderLayout.EAST);

        panel.add(titlePanel, BorderLayout.NORTH);
        return panel;
    }

    private JPanel createPaymentSummaryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        panel.setPreferredSize(new Dimension(panel.getPreferredSize().width, 280));
        panel.setMinimumSize(panel.getPreferredSize());
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));

        panel.add(createSectionTitle("Payment Summary", null, null), BorderLayout.NORTH);

        JPanel summaryDetails = new JPanel();
        summaryDetails.setLayout(new BoxLayout(summaryDetails, BoxLayout.Y_AXIS));
        summaryDetails.setBackground(Color.WHITE);
        summaryDetails.setBorder(new EmptyBorder(10, 20, 10, 20));

        totalMRPLabel = new JLabel();
        discountOnMRPLabel = new JLabel();
        couponSavingsLabel = new JLabel();
        applicableGSTLabel = new JLabel();
        deliveryLabel = new JLabel();
        finalTotalLabel = new JLabel();

        summaryDetails.add(createSummaryRowPanel("Total MRP", totalMRPLabel, DARK_TEXT_COLOR, totalMRP));
        summaryDetails.add(Box.createVerticalStrut(5));

        summaryDetails.add(createSummaryRowPanel("Discount on MRP", discountOnMRPLabel, new Color(76, 175, 80), discountOnMRP));
        summaryDetails.add(Box.createVerticalStrut(5));

        summaryDetails.add(createSummaryRowPanel("Coupon savings", couponSavingsLabel, new Color(76, 175, 80), couponSavings));
        summaryDetails.add(Box.createVerticalStrut(5));

        summaryDetails.add(createSummaryRowPanel("Applicable GST", applicableGSTLabel, DARK_TEXT_COLOR, applicableGST));
        summaryDetails.add(Box.createVerticalStrut(5));

        summaryDetails.add(createSummaryRowPanel("Delivery", deliveryLabel, new Color(0, 150, 136), deliveryCharge, "Free"));
        summaryDetails.add(Box.createVerticalStrut(10));

        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setForeground(BORDER_COLOR);
        summaryDetails.add(separator);
        summaryDetails.add(Box.createVerticalStrut(10));

        JPanel finalTotalRow = new JPanel(new BorderLayout());
        finalTotalRow.setBackground(Color.WHITE);
        JLabel totalTextLabel = new JLabel("Total");
        totalTextLabel.setFont(new Font("Arial", Font.BOLD, 16));
        totalTextLabel.setForeground(DARK_TEXT_COLOR);
        finalTotalRow.add(totalTextLabel, BorderLayout.WEST);
        finalTotalRow.add(finalTotalLabel, BorderLayout.EAST);
        summaryDetails.add(finalTotalRow);
        summaryDetails.add(Box.createVerticalStrut(20));

        panel.add(summaryDetails, BorderLayout.CENTER);

        checkoutButton = new JButton("PLACE ORDER");
        checkoutButton.setBackground(ORANGE_THEME);
        checkoutButton.setForeground(Color.WHITE);
        checkoutButton.setBorderPainted(false);
        checkoutButton.setFocusPainted(false);
        checkoutButton.setFont(new Font("Arial", Font.BOLD, 14));
        checkoutButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        checkoutButton.setPreferredSize(new Dimension(200, 40));
        checkoutButton.addActionListener(e -> {
            int selectedCount = getSelectedItemCount();
            if (selectedCount == 0) {
                JOptionPane.showMessageDialog(this,
                    "Mohon pilih setidaknya satu item untuk membuat pesanan.",
                    "Checkout",
                    JOptionPane.INFORMATION_MESSAGE);
            }
            
            else {
                if (viewController != null) {
                    viewController.showAddressView();
                } else {
                    JOptionPane.showMessageDialog(this,
                        "Melanjutkan untuk membuat pesanan\\nTotal: " + finalTotalLabel.getText(),
                        "Buat Pesanan",
                        JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(new EmptyBorder(0, 20, 15, 20));
        buttonPanel.add(checkoutButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createSummaryRowPanel(String labelText, JLabel valueLabelRef, Color valueColor, double amount) {
        return createSummaryRowPanel(labelText, valueLabelRef, valueColor, amount, null);
    }

    private JPanel createSummaryRowPanel(String labelText, JLabel valueLabelRef, Color valueColor, double amount, String customValueText) {
        JPanel rowPanel = new JPanel(new BorderLayout());
        rowPanel.setBackground(Color.WHITE);

        JLabel keyLabel = new JLabel(labelText);
        keyLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        keyLabel.setForeground(DARK_TEXT_COLOR);
        rowPanel.add(keyLabel, BorderLayout.WEST);

        valueLabelRef.setText(customValueText != null ? customValueText : formatCurrency(amount));
        valueLabelRef.setFont(new Font("Arial", Font.PLAIN, 14));
        valueLabelRef.setForeground(valueColor);
        valueLabelRef.setHorizontalAlignment(SwingConstants.RIGHT);
        rowPanel.add(valueLabelRef, BorderLayout.EAST);

        return rowPanel;
    }

    private JPanel createSectionTitle(String title, String actionText, ActionListener listener) {
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(new Color(249, 249, 249));
        titlePanel.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                new EmptyBorder(10, 20, 10, 20)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        titleLabel.setForeground(DARK_TEXT_COLOR);
        titlePanel.add(titleLabel, BorderLayout.WEST);

        if (actionText != null && listener != null) {
            JButton actionButton = new JButton(actionText);
            actionButton.setFont(new Font("Arial", Font.BOLD, 12));
            actionButton.setForeground(ORANGE_THEME);
            actionButton.setBackground(titlePanel.getBackground());
            actionButton.setBorderPainted(false);
            actionButton.setFocusPainted(false);
            actionButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            actionButton.addActionListener(listener);
            titlePanel.add(actionButton, BorderLayout.EAST);
        }
        return titlePanel;
    }

    private void refreshCartItems() {
        cartItemsContainer.removeAll();

        if (cartItems == null || cartItems.isEmpty()) {
            JPanel emptyCartPanel = new JPanel(new GridBagLayout());
            emptyCartPanel.setBackground(Color.WHITE);
            emptyCartPanel.setPreferredSize(new Dimension(cartItemsContainer.getWidth(), 150));
            emptyCartPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
            JLabel emptyLabel = new JLabel("Your cart is empty.", SwingConstants.CENTER);
            emptyLabel.setFont(new Font("Arial", Font.PLAIN, 18));
            emptyLabel.setForeground(GRAY_TEXT_COLOR);
            emptyCartPanel.add(emptyLabel);
            cartItemsContainer.add(emptyCartPanel);
        } else {
            for (CartItem item : cartItems) {
                JPanel itemPanel = createCartItemPanel(item);
                cartItemsContainer.add(itemPanel);
                JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
                separator.setForeground(BORDER_COLOR);
                cartItemsContainer.add(separator);
            }
            if (!cartItems.isEmpty() && cartItemsContainer.getComponentCount() > 0) {
                cartItemsContainer.remove(cartItemsContainer.getComponentCount() - 1);
            }
        }

        cartItemsContainer.add(Box.createVerticalGlue());

        cartItemsContainer.revalidate();
        cartItemsContainer.repaint();
        updateTotals();

        if (myCartTitle != null) {
            myCartTitle.setText("My Cart (" + cartItems.size() + ")");
        }
        updateCheckoutButtonState();
    }

    private JPanel createCartItemPanel(CartItem item) {
        JPanel itemPanel = new JPanel(new GridBagLayout());
        itemPanel.setBackground(Color.WHITE);
        itemPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 5, 0, 5);
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weighty = 1.0;

        // Column 0: Product Image
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        JPanel imagePanel = new ImagePanel(item.getImageData(), item.getFileExtension(), item.getImageColor());
        imagePanel.setPreferredSize(new Dimension(80, 80));
        itemPanel.add(imagePanel, gbc);

        // Column 1: Product Info (Name, Save for Later, Remove)
        gbc.gridx = 1;
        gbc.weightx = 0.55;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel productInfoPanel = new JPanel(new BorderLayout(0, 5));
        productInfoPanel.setBackground(Color.WHITE);

        JLabel nameLabel = new JLabel(item.getName());
        nameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        nameLabel.setForeground(DARK_TEXT_COLOR);
        productInfoPanel.add(nameLabel, BorderLayout.NORTH);

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        actionsPanel.setBackground(Color.WHITE);

        JLabel saveForLaterLabel = new JLabel("Save for Later");
        saveForLaterLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        saveForLaterLabel.setForeground(ORANGE_THEME);
        saveForLaterLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        saveForLaterLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JOptionPane.showMessageDialog(itemPanel, "Product '" + item.getName() + "' saved for later.");
            }
        });
        actionsPanel.add(saveForLaterLabel);

        JLabel separatorLabel = new JLabel("|");
        separatorLabel.setForeground(GRAY_TEXT_COLOR);
        actionsPanel.add(separatorLabel);

        JButton removeButton = new JButton("Remove");
        removeButton.setFont(new Font("Arial", Font.PLAIN, 12));
        removeButton.setForeground(ORANGE_THEME);
        removeButton.setBorderPainted(false);
        removeButton.setContentAreaFilled(false);
        removeButton.setFocusPainted(false);
        removeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        removeButton.addActionListener(e -> {
            int confirmResult = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to remove '" + item.getName() + "' from your cart?",
                    "Confirm Removal", JOptionPane.YES_NO_OPTION);
            if (confirmResult == JOptionPane.YES_OPTION) {
                cartItems.remove(item);
                if (currentUser != null) {
                    try {
                        ProductRepository.removeProductFromCart(currentUser.getId(), item.getId());
                    } catch (SQLException ex) {
                        System.err.println("Error removing product from DB: " + ex.getMessage());
                        JOptionPane.showMessageDialog(this, "Failed to remove product from database.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
                refreshCartItems();
            }
        });
        actionsPanel.add(removeButton);

        productInfoPanel.add(actionsPanel, BorderLayout.SOUTH);
        itemPanel.add(productInfoPanel, gbc);

        // Column 2: Quantity controls
        gbc.gridx = 2;
        gbc.weightx = 0.1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;

        JPanel quantityPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        quantityPanel.setBackground(Color.WHITE);
        quantityPanel.setBorder(new LineBorder(BORDER_COLOR, 1, true));

        JButton minusButton = new JButton("-");
        minusButton.setFont(new Font("Arial", Font.BOLD, 14));
        minusButton.setPreferredSize(new Dimension(30, 30));
        minusButton.setFocusPainted(false);
        minusButton.setBackground(Color.WHITE);
        minusButton.setBorderPainted(false);
        minusButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JTextField quantityField = new JTextField(String.valueOf(item.getQuantity()), 2);
        quantityField.setHorizontalAlignment(JTextField.CENTER);
        quantityField.setFont(new Font("Arial", Font.PLAIN, 14));
        quantityField.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        quantityField.setPreferredSize(new Dimension(40, 30));

        JButton plusButton = new JButton("+");
        plusButton.setFont(new Font("Arial", Font.BOLD, 14));
        plusButton.setPreferredSize(new Dimension(30, 30));
        plusButton.setFocusPainted(false);
        plusButton.setBackground(Color.WHITE);
        plusButton.setBorderPainted(false);
        plusButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        minusButton.addActionListener(e -> {
            if (item.getQuantity() > 1) {
                item.setQuantity(item.getQuantity() - 1);
                quantityField.setText(String.valueOf(item.getQuantity()));
                if (currentUser != null) {
                    try {
                        ProductRepository.updateCartItemQuantity(currentUser.getId(), item.getId(), item.getQuantity());
                    } catch (SQLException ex) {
                        System.err.println("Error updating quantity in DB: " + ex.getMessage());
                        JOptionPane.showMessageDialog(this, "Failed to update quantity in database.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
                updateTotals();
            }
        });

        plusButton.addActionListener(e -> {
            item.setQuantity(item.getQuantity() + 1);
            quantityField.setText(String.valueOf(item.getQuantity()));
            if (currentUser != null) {
                try {
                    ProductRepository.updateCartItemQuantity(currentUser.getId(), item.getId(), item.getQuantity());
                } catch (SQLException ex) {
                    System.err.println("Error updating quantity in DB: " + ex.getMessage());
                    JOptionPane.showMessageDialog(this, "Failed to update quantity in database.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
            updateTotals();
        });

        quantityField.addActionListener(e -> {
            try {
                int newQuantity = Integer.parseInt(quantityField.getText().trim());
                if (newQuantity > 0) {
                    item.setQuantity(newQuantity);
                    if (currentUser != null) {
                        ProductRepository.updateCartItemQuantity(currentUser.getId(), item.getId(), newQuantity);
                    }
                } else {
                    item.setQuantity(1);
                    quantityField.setText("1");
                    if (currentUser != null) {
                        ProductRepository.updateCartItemQuantity(currentUser.getId(), item.getId(), 1);
                    }
                }
                updateTotals();
            } catch (NumberFormatException ex) {
                quantityField.setText(String.valueOf(item.getQuantity()));
                JOptionPane.showMessageDialog(this, "Quantity must be a valid number.", "Input Error", JOptionPane.ERROR_MESSAGE);
            } catch (SQLException ex) {
                 System.err.println("Error updating quantity in DB: " + ex.getMessage());
                 JOptionPane.showMessageDialog(this, "Failed to update quantity in database.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        quantityPanel.add(minusButton);
        quantityPanel.add(quantityField);
        quantityPanel.add(plusButton);
        itemPanel.add(quantityPanel, gbc);

        // Column 3: Price and Discount
        gbc.gridx = 3;
        gbc.weightx = 0.25; // Sisa ruang untuk harga dan diskon
        gbc.anchor = GridBagConstraints.EAST; // Rata kanan di dalam sel
        gbc.fill = GridBagConstraints.NONE; // Tidak mengisi sel

        JPanel priceDiscountPanel = new JPanel();
        priceDiscountPanel.setLayout(new BoxLayout(priceDiscountPanel, BoxLayout.Y_AXIS)); // Stack vertikal
        priceDiscountPanel.setBackground(Color.WHITE);
        priceDiscountPanel.setAlignmentX(Component.RIGHT_ALIGNMENT); // Pastikan panel rata kanan

        JLabel currentPriceLabel = new JLabel(formatCurrency(item.getPrice() * item.getQuantity()));
        currentPriceLabel.setFont(new Font("Arial", Font.BOLD, 14));
        currentPriceLabel.setForeground(DARK_TEXT_COLOR);
        currentPriceLabel.setAlignmentX(Component.RIGHT_ALIGNMENT); // Teks di label rata kanan

        if (item.getPrice() < item.getOriginalPrice()) {
            JLabel originalPriceLabel = new JLabel("<html><strike>" + formatCurrency(item.getOriginalPrice() * item.getQuantity()) + "</strike></html>");
            originalPriceLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            originalPriceLabel.setForeground(GRAY_TEXT_COLOR);
            originalPriceLabel.setAlignmentX(Component.RIGHT_ALIGNMENT); // Teks di label rata kanan

            double discountPercentage = (1 - (item.getPrice() / item.getOriginalPrice())) * 100;
            JLabel discountPercentageLabel = new JLabel(String.format(" %.0f%% off", discountPercentage));
            discountPercentageLabel.setFont(new Font("Arial", Font.BOLD, 12));
            discountPercentageLabel.setForeground(new Color(76, 175, 80));
            discountPercentageLabel.setAlignmentX(Component.RIGHT_ALIGNMENT); // Teks di label rata kanan

            JPanel priceAndDiscountFlowPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0)); // Rata kanan untuk FlowLayout
            priceAndDiscountFlowPanel.setBackground(Color.WHITE);
            priceAndDiscountFlowPanel.add(originalPriceLabel);
            priceAndDiscountFlowPanel.add(discountPercentageLabel);
            priceAndDiscountFlowPanel.setAlignmentX(Component.RIGHT_ALIGNMENT); // Pastikan panel flow layout rata kanan

            priceDiscountPanel.add(currentPriceLabel);
            priceDiscountPanel.add(Box.createVerticalStrut(3));
            priceDiscountPanel.add(priceAndDiscountFlowPanel);
        } else {
            priceDiscountPanel.add(currentPriceLabel);
        }

        itemPanel.add(priceDiscountPanel, gbc);

        return itemPanel;
    }

    private void updateTotals() {
        totalMRP = 0;
        discountOnMRP = 0;
        couponSavings = 0;
        applicableGST = 0;
        deliveryCharge = 0;

        if (cartItems != null) {
            for (CartItem item : cartItems) {
                totalMRP += item.getOriginalPrice() * item.getQuantity();
                discountOnMRP += (item.getOriginalPrice() - item.getPrice()) * item.getQuantity();
            }
        }

        double finalTotal = totalMRP - discountOnMRP - couponSavings + applicableGST + deliveryCharge;

        if (totalMRPLabel != null) totalMRPLabel.setText(formatCurrency(totalMRP));
        if (discountOnMRPLabel != null) discountOnMRPLabel.setText(formatCurrency(discountOnMRP));
        if (couponSavingsLabel != null) couponSavingsLabel.setText(formatCurrency(couponSavings));
        if (applicableGSTLabel != null) applicableGSTLabel.setText(formatCurrency(applicableGST));
        if (finalTotalLabel != null) finalTotalLabel.setText(formatCurrency(finalTotal));

        updateCheckoutButtonState();
    }

    private void updateCheckoutButtonState() {
        if (checkoutButton != null) {
            // Tombol diaktifkan jika ada item dalam keranjang. Pemilihan alamat akan diurus di halaman selanjutnya.
            checkoutButton.setEnabled(!cartItems.isEmpty());
            checkoutButton.setBackground(cartItems.isEmpty() ? new Color(200, 200, 200) : ORANGE_THEME);
        }
    }

    private int getSelectedItemCount() {
        int count = 0;
        if (cartItems != null) {
            for (CartItem item : cartItems) {
                count += item.getQuantity();
            }
        }
        return count;
    }

    private String formatCurrency(double amount) {
        return String.format("Rp %,.2f", amount);
    }

    static class ImagePanel extends JPanel {
        private String colorHex;
        private BufferedImage image;
        private boolean hasImage;

        public ImagePanel(String colorHex) {
            this.colorHex = colorHex;
            this.hasImage = false;
            setPreferredSize(new Dimension(80, 80));
        }

        public ImagePanel(byte[] imageData, String fileExtension, String fallbackColor) {
            this.colorHex = fallbackColor;
            this.hasImage = false;
            setPreferredSize(new Dimension(80, 80));

            if (imageData != null && imageData.length > 0) {
                try (ByteArrayInputStream bis = new ByteArrayInputStream(imageData)) {
                    image = ImageIO.read(bis);
                    if (image != null) {
                        hasImage = true;
                    } else {
                        System.err.println("ImageIO.read returned null for BLOB image for colorHex: " + fallbackColor);
                    }
                } catch (IOException e) {
                    System.err.println("Error loading image from BLOB for colorHex: " + fallbackColor + ": " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.out.println("No BLOB image data provided for image panel (fallback to color for ID: " + fallbackColor + ").");
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (hasImage && image != null) {
                int width = getWidth();
                int height = getHeight();

                g2d.drawImage(image, 0, 0, width, height, null);

            } else {
                int width = getWidth() - 10;
                int height = getHeight() - 10;

                Color color;
                try {
                    color = Color.decode(colorHex);
                } catch (NumberFormatException e) {
                    color = new Color(200, 200, 200);
                }

                Shape roundedRect = new java.awt.geom.RoundRectangle2D.Float(5, 5, width, height, 10, 10);

                g2d.setColor(color);
                g2d.fill(roundedRect);

                g2d.setColor(color.darker());
                g2d.setFont(new Font("Arial", Font.BOLD, 10));
                String text = "NO IMAGE";
                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(text);
                int textHeight = fm.getHeight();
                g2d.drawString(text, (getWidth() - textWidth) / 2, (getHeight() + textHeight / 2) / 2 - fm.getDescent());
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Keranjang Belanja");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1200, 800);

            ViewController dummyVC = new ViewController() {
                @Override public void showProductDetail(FavoritesUI.FavoriteItem product) { System.out.println("Dummy: Tampilkan Detail Produk: " + product.getName()); }
                @Override public void showFavoritesView() { System.out.println("Dummy: Tampilkan Tampilan Favorit"); }
                @Override public void showDashboardView() { System.out.println("Dummy: Tampilkan Tampilan Dashboard"); }
                @Override public void showCartView() { System.out.println("Dummy: Tampilkan Tampilan Keranjang"); }
                @Override public void showProfileView() { System.out.println("Dummy: Tampilkan Tampilan Profil"); }
                @Override public void showOrdersView() { System.out.println("Dummy: Tampilkan Tampilan Pesanan"); }
                @Override public void showCheckoutView() { System.out.println("Dummy: Tampilkan Tampilan Checkout (Sukses)");}
                @Override
                public void showAddressView() {
                    System.out.println("Dummy: Tampilkan Tampilan Alamat.");
                    // Dalam aplikasi nyata, ini akan beralih panel ke AddressUI
                }
                @Override
                public void showPaymentView(AddressUI.Address selectedAddress, AddressUI.ShippingService selectedShippingService) {
                    System.out.println("Dummy: Tampilkan Tampilan Pembayaran dengan alamat dan pengiriman.");
                }
                @Override
                public void showSuccessView(int orderId) {
                    System.out.println("Dummy: Tampilkan Tampilan Sukses dengan ID pesanan: " + orderId);
                }
                @Override
                public void showOrderDetailView(int orderId) {
                    System.out.println("Dummy: Tampilkan Tampilan Detail Pesanan untuk ID: " + orderId);
                }
                @Override
                public void showChatWithSeller(int sellerId, String sellerUsername) { //
                    System.out.println("Dummy: Tampilkan Chat dengan Penjual ID: " + sellerId + " (" + sellerUsername + ")");
                }
            };

            // Authentication.setCurrentUser(new User(1, "testuser", "pass", "mail", "123", "08123", null, null, "user", true)); // Pastikan user dummy diatur untuk pengujian
            CartUI cartUI = new CartUI(dummyVC);
            frame.add(cartUI);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}