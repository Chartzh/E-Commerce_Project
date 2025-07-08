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

    private User currentUser;

    private static final Color ORANGE_THEME = new Color(255, 69, 0);
    private static final Color LIGHT_GRAY_BACKGROUND = new Color(245, 245, 245);
    private static final Color BORDER_COLOR = new Color(230, 230, 230);
    private static final Color DARK_TEXT_COLOR = new Color(50, 50, 50);
    private static final Color GRAY_TEXT_COLOR = new Color(120, 120, 120);

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

        JPanel offersPanel = createOfferCouponPanel("Penawaran Tersedia"); // Translated
        offersPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        leftColumnPanel.add(offersPanel);
        leftColumnPanel.add(Box.createVerticalStrut(15));

        JPanel couponsPanel = createOfferCouponPanel("Terapkan Kupon"); // Translated
        couponsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        leftColumnPanel.add(couponsPanel);
        leftColumnPanel.add(Box.createVerticalStrut(15));

        JPanel myCartSectionPanel = new JPanel(new BorderLayout());
        myCartSectionPanel.setBackground(Color.WHITE);
        myCartSectionPanel.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        myCartSectionPanel.setPreferredSize(new Dimension(650, 400));
        myCartSectionPanel.setMinimumSize(new Dimension(400, 200));
        myCartSectionPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        myCartTitle = new JLabel("Keranjang Saya (" + cartItems.size() + ")"); // Translated
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
        gbc.weightx = 0.7; 
        gbc.weighty = 1.0;
        mainContentPanel.add(leftColumnPanel, gbc);

        JPanel rightColumnPanel = new JPanel();
        rightColumnPanel.setLayout(new BoxLayout(rightColumnPanel, BoxLayout.Y_AXIS));
        rightColumnPanel.setBackground(LIGHT_GRAY_BACKGROUND);

        JPanel paymentSummaryPanel = createPaymentSummaryPanel();
        paymentSummaryPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));
        paymentSummaryPanel.setMinimumSize(new Dimension(300, 280)); 
        paymentSummaryPanel.setPreferredSize(new Dimension(350, 280)); 
        rightColumnPanel.add(paymentSummaryPanel);
        rightColumnPanel.add(Box.createVerticalGlue()); 

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.3; 
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

            if (steps[i].equals("Keranjang")) { // Translated step name
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

        panel.add(createSectionTitle("Ringkasan Pembayaran", null, null), BorderLayout.NORTH); // Translated

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

        summaryDetails.add(createSummaryRowPanel("Total MRP", totalMRPLabel, DARK_TEXT_COLOR, totalMRP)); // Translated
        summaryDetails.add(Box.createVerticalStrut(5));

        summaryDetails.add(createSummaryRowPanel("Diskon pada MRP", discountOnMRPLabel, new Color(76, 175, 80), discountOnMRP)); // Translated
        summaryDetails.add(Box.createVerticalStrut(5));

        summaryDetails.add(createSummaryRowPanel("Penghematan Kupon", couponSavingsLabel, new Color(76, 175, 80), couponSavings)); // Translated
        summaryDetails.add(Box.createVerticalStrut(5));

        summaryDetails.add(createSummaryRowPanel("PPN yang Berlaku", applicableGSTLabel, DARK_TEXT_COLOR, applicableGST)); // Translated
        summaryDetails.add(Box.createVerticalStrut(5));

        summaryDetails.add(createSummaryRowPanel("Pengiriman", deliveryLabel, new Color(0, 150, 136), deliveryCharge, "Gratis")); // Translated
        summaryDetails.add(Box.createVerticalStrut(10));

        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setForeground(BORDER_COLOR);
        summaryDetails.add(separator);
        summaryDetails.add(Box.createVerticalStrut(10));

        JPanel finalTotalRow = new JPanel(new BorderLayout());
        finalTotalRow.setBackground(Color.WHITE);
        JLabel totalTextLabel = new JLabel("Total"); // Translated
        totalTextLabel.setFont(new Font("Arial", Font.BOLD, 16));
        totalTextLabel.setForeground(DARK_TEXT_COLOR);
        finalTotalRow.add(totalTextLabel, BorderLayout.WEST);
        finalTotalRow.add(finalTotalLabel, BorderLayout.EAST);
        summaryDetails.add(finalTotalRow);
        summaryDetails.add(Box.createVerticalStrut(20));

        panel.add(summaryDetails, BorderLayout.CENTER);

        checkoutButton = new JButton("LANJUTKAN CHECKOUT"); // Translated
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
                    "Mohon pilih setidaknya satu item untuk membuat pesanan.", // Translated
                    "Checkout", // Translated
                    JOptionPane.INFORMATION_MESSAGE);
            }
            
            else {
                if (viewController != null) {
                    viewController.showAddressView();
                } else {
                    JOptionPane.showMessageDialog(this,
                        "Melanjutkan untuk membuat pesanan\nTotal: " + finalTotalLabel.getText(), // Translated
                        "Buat Pesanan", // Translated
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
            JLabel emptyLabel = new JLabel("Keranjang Anda kosong.", SwingConstants.CENTER); // Translated
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
            myCartTitle.setText("Keranjang Saya (" + cartItems.size() + ")"); // Translated
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

        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        JPanel imagePanel = new ImagePanel(item.getLoadedImage(), item.getImageColor());
        imagePanel.setPreferredSize(new Dimension(80, 80));
        itemPanel.add(imagePanel, gbc);

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

        JLabel saveForLaterLabel = new JLabel("Simpan untuk Nanti"); // Translated
        saveForLaterLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        saveForLaterLabel.setForeground(ORANGE_THEME);
        saveForLaterLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        saveForLaterLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JOptionPane.showMessageDialog(itemPanel, "Produk '" + item.getName() + "' disimpan untuk nanti."); // Translated
            }
        });
        actionsPanel.add(saveForLaterLabel);

        JLabel separatorLabel = new JLabel("|");
        separatorLabel.setForeground(GRAY_TEXT_COLOR);
        actionsPanel.add(separatorLabel);

        JButton removeButton = new JButton("Hapus"); // Translated
        removeButton.setFont(new Font("Arial", Font.PLAIN, 12));
        removeButton.setForeground(ORANGE_THEME);
        removeButton.setBorderPainted(false);
        removeButton.setContentAreaFilled(false);
        removeButton.setFocusPainted(false);
        removeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        removeButton.addActionListener(e -> {
            int confirmResult = JOptionPane.showConfirmDialog(this,
                    "Apakah Anda yakin ingin menghapus '" + item.getName() + "' dari keranjang Anda?", // Translated
                    "Konfirmasi Penghapusan", JOptionPane.YES_NO_OPTION); // Translated
            if (confirmResult == JOptionPane.YES_OPTION) {
                cartItems.remove(item);
                if (currentUser != null) {
                    try {
                        ProductRepository.removeProductFromCart(currentUser.getId(), item.getId());
                    } catch (SQLException ex) {
                        System.err.println("Error menghapus produk dari DB: " + ex.getMessage()); // Translated
                        JOptionPane.showMessageDialog(this, "Gagal menghapus produk dari database.", "Error", JOptionPane.ERROR_MESSAGE); // Translated
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
                        System.err.println("Error memperbarui jumlah di DB: " + ex.getMessage()); // Translated
                        JOptionPane.showMessageDialog(this, "Gagal memperbarui jumlah di database.", "Error", JOptionPane.ERROR_MESSAGE); // Translated
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
                    System.err.println("Error memperbarui jumlah di DB: " + ex.getMessage()); // Translated
                    JOptionPane.showMessageDialog(this, "Gagal memperbarui jumlah di database.", "Error", JOptionPane.ERROR_MESSAGE); // Translated
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
                JOptionPane.showMessageDialog(this, "Jumlah harus berupa angka yang valid.", "Input Error", JOptionPane.ERROR_MESSAGE); // Translated
            } catch (SQLException ex) {
                 System.err.println("Error memperbarui jumlah di DB: " + ex.getMessage()); // Translated
                 JOptionPane.showMessageDialog(this, "Gagal memperbarui jumlah di database.", "Error", JOptionPane.ERROR_MESSAGE); // Translated
            }
        });

        quantityPanel.add(minusButton);
        quantityPanel.add(quantityField);
        quantityPanel.add(plusButton);
        itemPanel.add(quantityPanel, gbc);

        gbc.gridx = 3;
        gbc.weightx = 0.25; 
        gbc.anchor = GridBagConstraints.EAST; 
        gbc.fill = GridBagConstraints.NONE; 

        JPanel priceDiscountPanel = new JPanel();
        priceDiscountPanel.setLayout(new BoxLayout(priceDiscountPanel, BoxLayout.Y_AXIS)); 
        priceDiscountPanel.setBackground(Color.WHITE);
        priceDiscountPanel.setAlignmentX(Component.RIGHT_ALIGNMENT); 

        JLabel currentPriceLabel = new JLabel(formatCurrency(item.getPrice() * item.getQuantity()));
        currentPriceLabel.setFont(new Font("Arial", Font.BOLD, 14));
        currentPriceLabel.setForeground(DARK_TEXT_COLOR);
        currentPriceLabel.setAlignmentX(Component.RIGHT_ALIGNMENT); 

        if (item.getPrice() < item.getOriginalPrice()) {
            JLabel originalPriceLabel = new JLabel("<html><strike>" + formatCurrency(item.getOriginalPrice() * item.getQuantity()) + "</strike></html>");
            originalPriceLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            originalPriceLabel.setForeground(GRAY_TEXT_COLOR);
            originalPriceLabel.setAlignmentX(Component.RIGHT_ALIGNMENT); 

            double discountPercentage = (1 - (item.getPrice() / item.getOriginalPrice())) * 100;
            JLabel discountPercentageLabel = new JLabel(String.format(" %.0f%% diskon", discountPercentage)); // Translated
            discountPercentageLabel.setFont(new Font("Arial", Font.BOLD, 12));
            discountPercentageLabel.setForeground(new Color(76, 175, 80));
            discountPercentageLabel.setAlignmentX(Component.RIGHT_ALIGNMENT); 

            JPanel priceAndDiscountFlowPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0)); 
            priceAndDiscountFlowPanel.setBackground(Color.WHITE);
            priceAndDiscountFlowPanel.add(originalPriceLabel);
            priceAndDiscountFlowPanel.add(discountPercentageLabel);
            priceAndDiscountFlowPanel.setAlignmentX(Component.RIGHT_ALIGNMENT); 

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
        private Image image;
        private boolean hasImage;

        public ImagePanel(Image imageToDisplay, String hexColor) {
            this.image = imageToDisplay;
            this.colorHex = hexColor;
            this.hasImage = (imageToDisplay != null);
            setPreferredSize(new Dimension(80, 80));
            setOpaque(true);
        }

        public ImagePanel(byte[] imageData, String fileExtension, String fallbackColor) {
            this(null, fallbackColor);
            if (imageData != null && imageData.length > 0) {
                try (ByteArrayInputStream bis = new ByteArrayInputStream(imageData)) {
                    BufferedImage bImage = ImageIO.read(bis);
                    if (bImage != null) {
                        this.image = bImage;
                        this.hasImage = true;
                    } else {
                        System.err.println("ImageIO.read mengembalikan null untuk gambar BLOB untuk fallbackColor: " + fallbackColor); // Translated
                    }
                } catch (IOException e) {
                    System.err.println("Error memuat gambar dari BLOB untuk fallbackColor: " + fallbackColor + ": " + e.getMessage()); // Translated
                    e.printStackTrace();
                }
            } else {
                System.out.println("Tidak ada data gambar BLOB yang disediakan untuk panel gambar (fallback ke warna untuk ID: " + fallbackColor + ")."); // Translated
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            if (hasImage && image != null) {
                int panelWidth = getWidth();
                int panelHeight = getHeight();
                
                int originalWidth = image.getWidth(null);
                int originalHeight = image.getHeight(null);

                if (originalWidth > 0 && originalHeight > 0) {
                    double scaleX = (double) panelWidth / originalWidth;
                    double scaleY = (double) panelHeight / originalHeight;
                    double scale = Math.min(scaleX, scaleY);

                    int scaledWidth = (int) (originalWidth * scale);
                    int scaledHeight = (int) (originalHeight * scale);

                    int drawX = (panelWidth - scaledWidth) / 2;
                    int drawY = (panelHeight - scaledHeight) / 2;

                    g2d.drawImage(image, drawX, drawY, scaledWidth, scaledHeight, null);
                } else {
                     drawNoImagePlaceholder(g2d);
                }

            } else {
                drawNoImagePlaceholder(g2d);
            }
        }

        private void drawNoImagePlaceholder(Graphics2D g2d) {
            int width = getWidth();
            int height = getHeight();

            Color color;
            try {
                color = Color.decode(colorHex);
            } catch (NumberFormatException e) {
                color = new Color(200, 200, 200);
            }

            g2d.setColor(color);
            g2d.fillRect(0, 0, width, height);

            g2d.setColor(color.darker());
            g2d.setFont(new Font("Arial", Font.BOLD, 10));
            String text = "TIDAK ADA GAMBAR"; // Translated
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            int textHeight = fm.getHeight();
            g2d.drawString(text, (width - textWidth) / 2, (height + textHeight) / 2 - fm.getDescent());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Keranjang Belanja"); // Translated
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
                    System.out.println("Dummy: Tampilkan Tampilan Alamat."); // Translated
                    // Dalam aplikasi nyata, ini akan beralih panel ke AddressUI
                }
                @Override
                public void showPaymentView(AddressUI.Address selectedAddress, AddressUI.ShippingService selectedShippingService, double totalAmount) {
                    System.out.println("Dummy: Tampilkan Tampilan Pembayaran dengan alamat dan pengiriman."); // Translated
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
                public void showChatWithSeller(int sellerId, String sellerUsername) { 
                    System.out.println("Dummy: Tampilkan Chat dengan Penjual ID: " + sellerId + " (" + sellerUsername + ")"); // Translated
                }
                @Override public void showProductReviewView(int productId, String productName, String productImage, double productPrice, String sellerName) {
                    System.out.println("Dummy: Tampilkan Tampilan Review Produk");
                }
            };

            CartUI cartUI = new CartUI(dummyVC);
            frame.add(cartUI);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}