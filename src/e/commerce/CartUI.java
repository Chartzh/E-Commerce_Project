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

// Import yang relevan
import e.commerce.ProductRepository.CartItem;
import e.commerce.CouponResult;
import e.commerce.CouponService;
import e.commerce.ViewController;
import e.commerce.AddressUI.Address;
import e.commerce.AddressUI.ShippingService;

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
    private CouponResult appliedCouponResult = null;
    private JTextField couponCodeField;

    private static final Color ORANGE_THEME = new Color(255, 69, 0);
    private static final Color LIGHT_GRAY_BACKGROUND = new Color(245, 245, 245);
    private static final Color BORDER_COLOR = new Color(230, 230, 230);
    private static final Color DARK_TEXT_COLOR = new Color(50, 50, 50);
    private static final Color GRAY_TEXT_COLOR = new Color(120, 120, 120);
    private static final Color GREEN_DISCOUNT_TEXT = new Color(76, 175, 80);

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
    }

    private void loadCartItemsFromDatabase() {
        if (currentUser != null) {
            cartItems = ProductRepository.getCartItemsForUser(currentUser.getId());
        } else {
            cartItems = new ArrayList<>();
        }
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

        JPanel couponsPanel = createFunctionalCouponPanel();
        couponsPanel.setPreferredSize(new Dimension(couponsPanel.getPreferredSize().width, 60));
        couponsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        leftColumnPanel.add(couponsPanel);
        leftColumnPanel.add(Box.createVerticalStrut(15));

        JPanel myCartSectionPanel = new JPanel(new BorderLayout());
        myCartSectionPanel.setBackground(Color.WHITE);
        myCartSectionPanel.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        myCartSectionPanel.setPreferredSize(new Dimension(650, 400));
        myCartSectionPanel.setMinimumSize(new Dimension(400, 200));

        myCartTitle = new JLabel("Keranjang Saya (" + cartItems.size() + ")");
        myCartTitle.setFont(new Font("Arial", Font.BOLD, 18));
        myCartTitle.setForeground(DARK_TEXT_COLOR);
        myCartTitle.setBorder(new EmptyBorder(15, 20, 15, 20));
        myCartSectionPanel.add(myCartTitle, BorderLayout.NORTH);

        // Kontainer utama untuk daftar produk
        cartItemsContainer = new JPanel(new BorderLayout());
        cartItemsContainer.setBackground(Color.WHITE);

        // Panel yang akan menampung semua item produk secara vertikal
        JPanel itemsListPanel = new JPanel();
        itemsListPanel.setLayout(new BoxLayout(itemsListPanel, BoxLayout.Y_AXIS));
        itemsListPanel.setBackground(Color.WHITE);
        
        // Letakkan panel daftar di UTARA agar item menempel di atas
        cartItemsContainer.add(itemsListPanel, BorderLayout.NORTH);

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
    }

    private JPanel createTopHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(new EmptyBorder(10, 20, 10, 20));

        JPanel leftSection = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftSection.setBackground(Color.WHITE);
        JButton backButton = new JButton("← Kembali");
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

        String[] steps = {"Keranjang", "Alamat", "Pembayaran", "Selesai"};
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
                stepIcon.setText("?");
                stepIcon.setFont(new Font("Arial", Font.PLAIN, 20)); 
                stepIcon.setForeground(Color.RED); 
            }
            stepIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel stepLabel = new JLabel(steps[i]);
            stepLabel.setFont(new Font("Arial", Font.PLAIN, 14));
            stepLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            if (steps[i].equals("Keranjang")) {
                stepLabel.setForeground(ORANGE_THEME);
                stepLabel.setFont(new Font("Arial", Font.BOLD, 14));
            } else {
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

    private JPanel createFunctionalCouponPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 0));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new CompoundBorder(
            new LineBorder(BORDER_COLOR, 1, true),
            new EmptyBorder(10, 20, 10, 20)
        ));

        couponCodeField = new JTextField("Masukkan kode kupon");
        couponCodeField.setForeground(GRAY_TEXT_COLOR);
        couponCodeField.setFont(new Font("Arial", Font.ITALIC, 16));
        couponCodeField.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(220, 220, 220)),
            new EmptyBorder(5, 10, 5, 10)
        ));
        
        couponCodeField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (couponCodeField.getText().equals("Masukkan kode kupon")) {
                    couponCodeField.setText("");
                    couponCodeField.setFont(new Font("Arial", Font.PLAIN, 16));
                    couponCodeField.setForeground(DARK_TEXT_COLOR);
                }
            }
            public void focusLost(FocusEvent e) {
                if (couponCodeField.getText().isEmpty()) {
                    couponCodeField.setText("Masukkan kode kupon");
                    couponCodeField.setFont(new Font("Arial", Font.ITALIC, 16));
                    couponCodeField.setForeground(GRAY_TEXT_COLOR);
                }
            }
        });

        JButton applyCouponButton = new JButton("TERAPKAN");
        applyCouponButton.setBackground(ORANGE_THEME);
        applyCouponButton.setForeground(Color.WHITE);
        applyCouponButton.setFont(new Font("Arial", Font.BOLD, 14));
        applyCouponButton.setFocusPainted(false);
        applyCouponButton.setPreferredSize(new Dimension(120, 40));
        applyCouponButton.addActionListener(e -> applyCouponAction());

        panel.add(couponCodeField, BorderLayout.CENTER);
        panel.add(applyCouponButton, BorderLayout.EAST);

        return panel;
    }

    private void applyCouponAction() {
        String code = couponCodeField.getText().trim();
        if (code.isEmpty() || code.equals("Masukkan kode kupon")) {
            JOptionPane.showMessageDialog(this, "Silakan masukkan kode kupon.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (currentUser == null) {
            JOptionPane.showMessageDialog(this, "Anda harus login untuk menggunakan kupon.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        double subtotalForCoupon = totalMRP - discountOnMRP;
        CouponResult result = CouponService.applyCoupon(code, currentUser.getId(), subtotalForCoupon);

        JOptionPane.showMessageDialog(this, result.getMessage(), 
                                      result.isSuccess() ? "Sukses" : "Gagal", 
                                      result.isSuccess() ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);

        if (result.isSuccess()) {
            this.appliedCouponResult = result;
            this.couponSavings = result.getDiscountAmount();
        } else {
            this.appliedCouponResult = null;
            this.couponSavings = 0;
        }
        updateTotals();
    }

    private JPanel createPaymentSummaryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        panel.setPreferredSize(new Dimension(panel.getPreferredSize().width, 280));
        panel.setMinimumSize(panel.getPreferredSize());
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));

        panel.add(createSectionTitle("Ringkasan Pembayaran", null, null), BorderLayout.NORTH);

        JPanel summaryDetails = new JPanel();
        summaryDetails.setLayout(new BoxLayout(summaryDetails, BoxLayout.Y_AXIS));
        summaryDetails.setBackground(Color.WHITE);
        summaryDetails.setBorder(new EmptyBorder(10, 20, 10, 20));

        totalMRPLabel = new JLabel();
        discountOnMRPLabel = new JLabel();
        couponSavingsLabel = new JLabel();
        finalTotalLabel = new JLabel();

        summaryDetails.add(createSummaryRowPanel("Total", totalMRPLabel, DARK_TEXT_COLOR, totalMRP));
        summaryDetails.add(Box.createVerticalStrut(5));
        summaryDetails.add(createSummaryRowPanel("Diskon", discountOnMRPLabel, GREEN_DISCOUNT_TEXT, discountOnMRP));
        summaryDetails.add(Box.createVerticalStrut(5));
        summaryDetails.add(createSummaryRowPanel("Penghematan Kupon", couponSavingsLabel, GREEN_DISCOUNT_TEXT, couponSavings));
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

        checkoutButton = new JButton("LANJUTKAN CHECKOUT");
        checkoutButton.setBackground(ORANGE_THEME);
        checkoutButton.setForeground(Color.WHITE);
        checkoutButton.setBorderPainted(false);
        checkoutButton.setFocusPainted(false);
        checkoutButton.setFont(new Font("Arial", Font.BOLD, 14));
        checkoutButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        checkoutButton.setPreferredSize(new Dimension(200, 40));
        checkoutButton.addActionListener(e -> {
            if (cartItems == null || cartItems.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Keranjang Anda kosong.", "Checkout", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            if (viewController != null) {
                viewController.showAddressView(this.appliedCouponResult); 
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
        JPanel rowPanel = new JPanel(new BorderLayout());
        rowPanel.setBackground(Color.WHITE);

        JLabel keyLabel = new JLabel(labelText);
        keyLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        keyLabel.setForeground(DARK_TEXT_COLOR);
        rowPanel.add(keyLabel, BorderLayout.WEST);

        valueLabelRef.setText(formatCurrency(amount));
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
        // PERBAIKAN: Dapatkan panel yang benar untuk menampung daftar item
        JPanel itemsListPanel = (JPanel) cartItemsContainer.getComponent(0);
        itemsListPanel.removeAll();

        if (cartItems == null || cartItems.isEmpty()) {
            JPanel emptyCartPanel = new JPanel(new GridBagLayout());
            emptyCartPanel.setBackground(Color.WHITE);
            emptyCartPanel.setPreferredSize(new Dimension(itemsListPanel.getWidth(), 200));
            
            JLabel emptyLabel = new JLabel("Keranjang Anda kosong.", SwingConstants.CENTER);
            emptyLabel.setFont(new Font("Arial", Font.PLAIN, 18));
            emptyLabel.setForeground(GRAY_TEXT_COLOR);
            emptyCartPanel.add(emptyLabel);
            
            itemsListPanel.add(emptyCartPanel);
        } else {
            for (int i = 0; i < cartItems.size(); i++) {
                CartItem item = cartItems.get(i);
                JPanel itemPanel = createCartItemPanel(item);
                itemsListPanel.add(itemPanel);
                if (i < cartItems.size() - 1) {
                    JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
                    separator.setForeground(BORDER_COLOR);
                    separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
                    itemsListPanel.add(separator);
                }
            }
        }

        itemsListPanel.revalidate();
        itemsListPanel.repaint();
        updateTotals();

        if (myCartTitle != null) {
            myCartTitle.setText("Keranjang Saya (" + cartItems.size() + ")");
        }
        updateCheckoutButtonState();
    }

    private JPanel createCartItemPanel(CartItem item) {
        JPanel itemPanel = new JPanel(new GridBagLayout());
        itemPanel.setBackground(Color.WHITE);
        itemPanel.setBorder(new EmptyBorder(15, 20, 15, 20));
        itemPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 5, 0, 5);
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;

        // Kolom 0: Gambar
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        JPanel imagePanel = new ImagePanel(item.getLoadedImage(), item.getImageColor());
        imagePanel.setPreferredSize(new Dimension(80, 80));
        itemPanel.add(imagePanel, gbc);

        // Kolom 1: Info Produk
        gbc.gridx = 1;
        gbc.weightx = 0.6;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel productInfoPanel = new JPanel(new BorderLayout(0, 5));
        productInfoPanel.setBackground(Color.WHITE);
        
        JLabel nameLabel = new JLabel(item.getName());
        nameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        nameLabel.setForeground(DARK_TEXT_COLOR);
        productInfoPanel.add(nameLabel, BorderLayout.NORTH);

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        actionsPanel.setBackground(Color.WHITE);
        JLabel saveForLaterLabel = new JLabel("Simpan untuk Nanti");
        saveForLaterLabel.setForeground(ORANGE_THEME);
        saveForLaterLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        actionsPanel.add(saveForLaterLabel);
        actionsPanel.add(new JLabel("|"));
        JButton removeButton = new JButton("Hapus");
        removeButton.setForeground(ORANGE_THEME);
        removeButton.setBorderPainted(false);
        removeButton.setContentAreaFilled(false);
        removeButton.setFocusPainted(false);
        removeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        removeButton.addActionListener(e -> {
            int confirmResult = JOptionPane.showConfirmDialog(this, "Apakah Anda yakin ingin menghapus '" + item.getName() + "' dari keranjang?", "Konfirmasi Penghapusan", JOptionPane.YES_NO_OPTION);
            if (confirmResult == JOptionPane.YES_OPTION) {
                cartItems.remove(item);
                if (currentUser != null) {
                    try {
                        ProductRepository.removeProductFromCart(currentUser.getId(), item.getId());
                    } catch (SQLException ex) {
                        JOptionPane.showMessageDialog(this, "Gagal menghapus produk dari database.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
                refreshCartItems();
            }
        });
        actionsPanel.add(removeButton);
        productInfoPanel.add(actionsPanel, BorderLayout.SOUTH);
        itemPanel.add(productInfoPanel, gbc);

        // Kolom 2: Kontrol Kuantitas
        gbc.gridx = 2; 
        gbc.weightx = 0.2; 
        gbc.fill = GridBagConstraints.NONE;
        JPanel quantityPanel = createQuantityControlPanel(item);
        itemPanel.add(quantityPanel, gbc);
        
        // Kolom 3: Harga
        gbc.gridx = 3;
        gbc.weightx = 0.2; 
        gbc.anchor = GridBagConstraints.EAST; 
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
            JLabel discountPercentageLabel = new JLabel(String.format(" %.0f%% diskon", discountPercentage));
            discountPercentageLabel.setFont(new Font("Arial", Font.BOLD, 12));
            discountPercentageLabel.setForeground(GREEN_DISCOUNT_TEXT);
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
    
    private JPanel createQuantityControlPanel(CartItem item) {
        JPanel quantityPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        quantityPanel.setBackground(Color.WHITE);
        quantityPanel.setBorder(new LineBorder(BORDER_COLOR, 1, true));

        JButton minusButton = new JButton("-");
        setupQuantityButton(minusButton);
        JButton plusButton = new JButton("+");
        setupQuantityButton(plusButton);

        JTextField quantityField = new JTextField(String.valueOf(item.getQuantity()), 2);
        quantityField.setHorizontalAlignment(JTextField.CENTER);
        quantityField.setFont(new Font("Arial", Font.PLAIN, 14));
        quantityField.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 1, BORDER_COLOR));
        quantityField.setPreferredSize(new Dimension(40, 30));

        minusButton.addActionListener(e -> {
            if (item.getQuantity() > 1) {
                item.setQuantity(item.getQuantity() - 1);
                updateItemInDB(item);
                refreshCartItems();
            }
        });

        plusButton.addActionListener(e -> {
            item.setQuantity(item.getQuantity() + 1);
            updateItemInDB(item);
            refreshCartItems();
        });

        quantityField.addActionListener(e -> {
            try {
                int newQuantity = Integer.parseInt(quantityField.getText().trim());
                if (newQuantity > 0) {
                    item.setQuantity(newQuantity);
                } else {
                    item.setQuantity(1);
                }
                updateItemInDB(item);
                refreshCartItems();
            } catch (NumberFormatException ex) {
                quantityField.setText(String.valueOf(item.getQuantity()));
            }
        });

        quantityPanel.add(minusButton);
        quantityPanel.add(quantityField);
        quantityPanel.add(plusButton);
        return quantityPanel;
    }
    
    private void setupQuantityButton(JButton button) {
        button.setFont(new Font("Arial", Font.BOLD, 16));
        button.setPreferredSize(new Dimension(35, 30));
        button.setFocusPainted(false);
        button.setBackground(new Color(250, 250, 250));
        button.setBorder(null);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent evt) {
                button.setBackground(new Color(230, 230, 230));
            }
            public void mouseExited(MouseEvent evt) {
                button.setBackground(new Color(250, 250, 250));
            }
        });
    }
    
    private void updateItemInDB(CartItem item) {
        if (currentUser != null) {
            try {
                ProductRepository.updateCartItemQuantity(currentUser.getId(), item.getId(), item.getQuantity());
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Gagal memperbarui jumlah di database.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void updateTotals() {
        totalMRP = 0;
        discountOnMRP = 0;
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
        if (discountOnMRPLabel != null) discountOnMRPLabel.setText("-" + formatCurrency(discountOnMRP));
        if (couponSavingsLabel != null) couponSavingsLabel.setText("-" + formatCurrency(couponSavings));
        if (finalTotalLabel != null) {
            finalTotalLabel.setText(formatCurrency(finalTotal));
            finalTotalLabel.setFont(new Font("Arial", Font.BOLD, 16));
        }
        updateCheckoutButtonState();
    }

    private void updateCheckoutButtonState() {
        if (checkoutButton != null) {
            checkoutButton.setEnabled(cartItems != null && !cartItems.isEmpty());
            checkoutButton.setBackground(cartItems != null && !cartItems.isEmpty() ? ORANGE_THEME : new Color(200, 200, 200));
        }
    }

    private String formatCurrency(double amount) {
        return String.format("Rp %,.2f", amount);
    }

    static class ImagePanel extends JPanel {
        private Image image;
        private String colorHex;
        private boolean hasImage;

        public ImagePanel(Image imageToDisplay, String hexColor) {
            this.image = imageToDisplay;
            this.colorHex = hexColor;
            this.hasImage = (imageToDisplay != null);
            setPreferredSize(new Dimension(80, 80));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            if (hasImage && image != null) {
                int panelWidth = getWidth();
                int panelHeight = getHeight();
                int originalWidth = image.getWidth(null);
                int originalHeight = image.getHeight(null);
                if (originalWidth > 0 && originalHeight > 0) {
                    double scale = Math.min((double) panelWidth / originalWidth, (double) panelHeight / originalHeight);
                    int scaledWidth = (int) (originalWidth * scale);
                    int scaledHeight = (int) (originalHeight * scale);
                    int drawX = (panelWidth - scaledWidth) / 2;
                    int drawY = (panelHeight - scaledHeight) / 2;
                    g2d.drawImage(image, drawX, drawY, scaledWidth, scaledHeight, null);
                }
            } else {
                g2d.setColor(Color.decode(colorHex != null ? colorHex : "#EEEEEE"));
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.setColor(Color.GRAY);
                g2d.drawString("No Image", 10, getHeight() / 2);
            }
            g2d.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Keranjang Belanja");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1200, 800);

            ViewController dummyVC = new ViewController() {
                @Override public void showProductDetail(FavoritesUI.FavoriteItem product) {}
                @Override public void showFavoritesView() {}
                @Override public void showDashboardView() {}
                @Override public void showCartView() {}
                @Override public void showProfileView() {}
                @Override public void showOrdersView() {}
                @Override public void showCheckoutView() {}
                @Override public void showAddressView(CouponResult couponResult) {}
                @Override public void showPaymentView(Address selectedAddress, ShippingService selectedShippingService, double totalAmount, CouponResult couponResult) {}
                @Override public void showSuccessView(int orderId) {}
                @Override public void showOrderDetailView(int orderId) {}
                @Override public void showChatWithSeller(int sellerId, String sellerUsername) {}
                @Override public void showProductReviewView(int productId, String productName, String productImage, double productPrice, String sellerName) {}
            };

            CartUI cartUI = new CartUI(dummyVC);
            frame.add(cartUI);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
