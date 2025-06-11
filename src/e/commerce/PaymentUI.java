package e.commerce; // Sesuaikan dengan package Anda

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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap; // Jika diperlukan, meskipun PaymentUI mungkin tidak langsung menggunakannya

import e.commerce.ProductRepository;
import e.commerce.ProductRepository.CartItem;

public class PaymentUI extends JPanel {
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
    private double deliveryCharge = 0; // Mungkin dari AddressUI
    private List<ProductRepository.CartItem> cartItems; // Untuk kalkulasi ringkasan

    private JTextField cardNumberField;
    private JTextField cardExpiryField;
    private JTextField cvvField;
    private JTextField cardNameField;
    private JButton payNowButton;

    private ViewController viewController;
    private User currentUser;

    // Constants for theme colors
    private static final Color ORANGE_THEME = new Color(255, 69, 0);
    private static final Color LIGHT_GRAY_BACKGROUND = new Color(245, 245, 245);
    private static final Color BORDER_COLOR = new Color(230, 230, 230);
    private static final Color DARK_TEXT_COLOR = new Color(50, 50, 50);
    private static final Color GRAY_TEXT_COLOR = new Color(120, 120, 120);
    private static final Color GREEN_DISCOUNT_TEXT = new Color(76, 175, 80);
    private static final Color TEAL_FREE_TEXT = new Color(0, 150, 136);

    public PaymentUI(ViewController viewController) {
        this.viewController = viewController;
        setLayout(new BorderLayout());
        setBackground(LIGHT_GRAY_BACKGROUND);

        this.currentUser = Authentication.getCurrentUser();
        if (this.currentUser == null) {
            System.err.println("PaymentUI: Tidak ada user yang login. Halaman tidak dapat dimuat.");
            JOptionPane.showMessageDialog(this, "Anda harus login untuk melihat halaman ini.", "Error", JOptionPane.ERROR_MESSAGE);
        }

        loadCartItemsFromDatabase(); // Load cart items for summary
        // Anda mungkin juga perlu mendapatkan data deliveryCharge dari AddressUI
        // Untuk demo, kita asumsikan deliveryCharge sudah ada atau dihitung dummy.
        // deliveryCharge = ... (bisa diatur dari ViewController atau parameter konstruktor)

        createComponents();
        updateSummaryTotals();
    }

    private void loadCartItemsFromDatabase() {
        if (currentUser != null) {
            cartItems = ProductRepository.getCartItemsForUser(currentUser.getId());
            System.out.println("DEBUG PaymentUI: Memuat " + cartItems.size() + " item dari database untuk user ID: " + currentUser.getId());
        } else {
            cartItems = new ArrayList<>();
            System.out.println("DEBUG PaymentUI: Tidak ada user yang login, keranjang kosong.");
        }
    }

    private void createComponents() {
        // Top Header
        JPanel topHeaderPanel = createTopHeaderPanel();
        add(topHeaderPanel, BorderLayout.NORTH);

        // Main content panel with GridBagLayout for the two-column structure
        JPanel mainContentPanel = new JPanel(new GridBagLayout());
        mainContentPanel.setBackground(LIGHT_GRAY_BACKGROUND);
        mainContentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(10, 10, 10, 10);

        // Left Column (Select Payment Mode)
        JPanel leftColumnPanel = new JPanel();
        leftColumnPanel.setLayout(new BoxLayout(leftColumnPanel, BoxLayout.Y_AXIS));
        leftColumnPanel.setBackground(LIGHT_GRAY_BACKGROUND);

        // --- Select Payment Mode Section ---
        JPanel selectPaymentModePanel = createSelectPaymentModeSection();
        selectPaymentModePanel.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        leftColumnPanel.add(selectPaymentModePanel);
        leftColumnPanel.add(Box.createVerticalGlue()); // Push content to top

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.65; // Left column takes 65% width
        gbc.weighty = 1.0;
        mainContentPanel.add(leftColumnPanel, gbc);

        // Right Column (Payment Summary)
        JPanel rightColumnPanel = new JPanel();
        rightColumnPanel.setLayout(new BoxLayout(rightColumnPanel, BoxLayout.Y_AXIS));
        rightColumnPanel.setBackground(LIGHT_GRAY_BACKGROUND);

        // --- Payment Summary Section ---
        JPanel paymentSummaryPanel = createPaymentSummaryPanel();
        paymentSummaryPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));
        paymentSummaryPanel.setMinimumSize(new Dimension(300, 280));
        paymentSummaryPanel.setPreferredSize(new Dimension(350, 280));
        rightColumnPanel.add(paymentSummaryPanel);
        rightColumnPanel.add(Box.createVerticalGlue()); // Push content to top

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 0.35; // Right column takes 35% width
        gbc.weighty = 1.0;
        mainContentPanel.add(rightColumnPanel, gbc);

        add(mainContentPanel, BorderLayout.CENTER);
    }

    private JPanel createTopHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(new EmptyBorder(10, 20, 10, 20));

        // Left section: ONLY Back button
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
                viewController.showAddressView(); // Kembali ke halaman Address
            }
        });
        leftSection.add(backButton);
        headerPanel.add(leftSection, BorderLayout.WEST);

        // Center section: Navigation steps (Cart > Address > Payment > Success)
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

            if (steps[i].equals("Payment")) { // Highlight current step
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

        // Right section: Empty placeholder
        JPanel rightPlaceholderPanel = new JPanel();
        rightPlaceholderPanel.setBackground(Color.WHITE);
        headerPanel.add(rightPlaceholderPanel, BorderLayout.EAST);

        return headerPanel;
    }

    private JPanel createSelectPaymentModeSection() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(25, 25, 25, 25)); // Padding yang cukup

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(Color.WHITE);
        JLabel titleLabel = new JLabel("Select Payment Mode");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setForeground(DARK_TEXT_COLOR);
        titlePanel.add(titleLabel, BorderLayout.WEST);
        panel.add(titlePanel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(new EmptyBorder(15, 0, 0, 0)); // Padding dari judul

        ButtonGroup paymentMethodGroup = new ButtonGroup();

        // Saved Cards/UPI
        JRadioButton savedCardsRadioButton = new JRadioButton("Saved Cards/UPI");
        savedCardsRadioButton.setFont(new Font("Arial", Font.PLAIN, 14));
        savedCardsRadioButton.setBackground(Color.WHITE);
        savedCardsRadioButton.setFocusPainted(false);
        savedCardsRadioButton.setBorder(new EmptyBorder(5, 0, 5, 0));
        contentPanel.add(createRadioButtonPanel(savedCardsRadioButton));
        paymentMethodGroup.add(savedCardsRadioButton);

        contentPanel.add(Box.createVerticalStrut(10)); // Jarak antar metode

        // Cash on Delivery
        JRadioButton codRadioButton = new JRadioButton("Cash on Delivery");
        codRadioButton.setFont(new Font("Arial", Font.PLAIN, 14));
        codRadioButton.setBackground(Color.WHITE);
        codRadioButton.setFocusPainted(false);
        codRadioButton.setBorder(new EmptyBorder(5, 0, 5, 0));
        contentPanel.add(createRadioButtonPanel(codRadioButton));
        paymentMethodGroup.add(codRadioButton);

        contentPanel.add(Box.createVerticalStrut(10));

        // Credit Card
        JRadioButton creditCardRadioButton = new JRadioButton("Credit Card");
        creditCardRadioButton.setFont(new Font("Arial", Font.PLAIN, 14));
        creditCardRadioButton.setBackground(Color.WHITE);
        creditCardRadioButton.setFocusPainted(false);
        creditCardRadioButton.setBorder(new EmptyBorder(5, 0, 5, 0));
        contentPanel.add(createRadioButtonPanel(creditCardRadioButton));
        paymentMethodGroup.add(creditCardRadioButton);

        // Credit Card Details Form (initially hidden)
        JPanel creditCardForm = createCreditCardForm();
        creditCardForm.setVisible(false); // Sembunyikan secara default
        contentPanel.add(creditCardForm);

        // Listener untuk menampilkan/menyembunyikan form kartu kredit
        creditCardRadioButton.addActionListener(e -> creditCardForm.setVisible(creditCardRadioButton.isSelected()));
        codRadioButton.addActionListener(e -> creditCardForm.setVisible(false));
        savedCardsRadioButton.addActionListener(e -> creditCardForm.setVisible(false));


        contentPanel.add(Box.createVerticalStrut(10));

        // Debit Card
        JRadioButton debitCardRadioButton = new JRadioButton("Debit Card");
        debitCardRadioButton.setFont(new Font("Arial", Font.PLAIN, 14));
        debitCardRadioButton.setBackground(Color.WHITE);
        debitCardRadioButton.setFocusPainted(false);
        debitCardRadioButton.setBorder(new EmptyBorder(5, 0, 5, 0));
        contentPanel.add(createRadioButtonPanel(debitCardRadioButton));
        paymentMethodGroup.add(debitCardRadioButton);

        // Listener untuk debit card (asumsi tidak ada form detail terpisah, atau sama seperti credit card)
        debitCardRadioButton.addActionListener(e -> creditCardForm.setVisible(debitCardRadioButton.isSelected()));


        // Set default selection
        creditCardRadioButton.setSelected(true); // Default ke Credit Card
        creditCardForm.setVisible(true);


        panel.add(contentPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createRadioButtonPanel(JRadioButton radioButton) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); // No extra gaps
        panel.setBackground(Color.WHITE);
        panel.add(radioButton);
        return panel;
    }

    private JPanel createCreditCardForm() {
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(new EmptyBorder(10, 30, 10, 10)); // Indentasi untuk form kartu

        // Card Number
        JPanel cardNumberPanel = new JPanel(new BorderLayout());
        cardNumberPanel.setBackground(Color.WHITE);
        cardNumberField = new JTextField();
        cardNumberField.setFont(new Font("Arial", Font.PLAIN, 14));
        cardNumberField.setBorder(new LineBorder(BORDER_COLOR));
        cardNumberPanel.add(new JLabel("Card Number"), BorderLayout.NORTH);
        cardNumberPanel.add(cardNumberField, BorderLayout.CENTER);
        formPanel.add(cardNumberPanel);
        formPanel.add(Box.createVerticalStrut(10));

        // Card Expiry Date & CVV
        JPanel expiryCvvPanel = new JPanel(new GridBagLayout());
        expiryCvvPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 10); // Jarak antar kolom
        gbc.weightx = 0.5;

        JPanel expiryPanel = new JPanel(new BorderLayout());
        expiryPanel.setBackground(Color.WHITE);
        cardExpiryField = new JTextField("MM/YY");
        cardExpiryField.setFont(new Font("Arial", Font.PLAIN, 14));
        cardExpiryField.setBorder(new LineBorder(BORDER_COLOR));
        expiryPanel.add(new JLabel("Card Expiry Date"), BorderLayout.NORTH);
        expiryPanel.add(cardExpiryField, BorderLayout.CENTER);
        gbc.gridx = 0;
        expiryCvvPanel.add(expiryPanel, gbc);

        JPanel cvvPanel = new JPanel(new BorderLayout());
        cvvPanel.setBackground(Color.WHITE);
        cvvField = new JTextField();
        cvvField.setFont(new Font("Arial", Font.PLAIN, 14));
        cvvField.setBorder(new LineBorder(BORDER_COLOR));
        JLabel cvvLabel = new JLabel("CVV");
        JButton helpButton = new JButton("Help");
        helpButton.setFont(new Font("Arial", Font.PLAIN, 10));
        helpButton.setForeground(Color.BLUE);
        helpButton.setBorderPainted(false);
        helpButton.setContentAreaFilled(false);
        helpButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        helpButton.addActionListener(e -> JOptionPane.showMessageDialog(formPanel, "CVV adalah 3 atau 4 digit nomor keamanan di belakang kartu Anda.", "Apa itu CVV?", JOptionPane.INFORMATION_MESSAGE));
        
        JPanel cvvLabelPanel = new JPanel(new BorderLayout());
        cvvLabelPanel.setBackground(Color.WHITE);
        cvvLabelPanel.add(cvvLabel, BorderLayout.WEST);
        cvvLabelPanel.add(helpButton, BorderLayout.EAST);
        
        cvvPanel.add(cvvLabelPanel, BorderLayout.NORTH);
        cvvPanel.add(cvvField, BorderLayout.CENTER);
        gbc.gridx = 1;
        gbc.insets = new Insets(0, 0, 0, 0); // Reset insets
        expiryCvvPanel.add(cvvPanel, gbc);

        formPanel.add(expiryCvvPanel);
        formPanel.add(Box.createVerticalStrut(10));

        // Card Name
        JPanel cardNamePanel = new JPanel(new BorderLayout());
        cardNamePanel.setBackground(Color.WHITE);
        cardNameField = new JTextField();
        cardNameField.setFont(new Font("Arial", Font.PLAIN, 14));
        cardNameField.setBorder(new LineBorder(BORDER_COLOR));
        cardNamePanel.add(new JLabel("Name (As on card)"), BorderLayout.NORTH);
        cardNamePanel.add(cardNameField, BorderLayout.CENTER);
        formPanel.add(cardNamePanel);
        formPanel.add(Box.createVerticalStrut(10));

        // Save this card securely checkbox
        JCheckBox saveCardCheckbox = new JCheckBox("Save this card securely");
        saveCardCheckbox.setFont(new Font("Arial", Font.PLAIN, 12));
        saveCardCheckbox.setBackground(Color.WHITE);
        saveCardCheckbox.setFocusPainted(false);
        formPanel.add(saveCardCheckbox);
        formPanel.add(Box.createVerticalStrut(20));

        // Pay Now Button
        payNowButton = new JButton("PAY NOW " + formatCurrency(totalMRP - discountOnMRP - couponSavings + applicableGST + deliveryCharge));
        payNowButton.setBackground(ORANGE_THEME);
        payNowButton.setForeground(Color.WHITE);
        payNowButton.setBorderPainted(false);
        payNowButton.setFocusPainted(false);
        payNowButton.setFont(new Font("Arial", Font.BOLD, 14));
        payNowButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        payNowButton.setPreferredSize(new Dimension(200, 40));
        payNowButton.setAlignmentX(Component.LEFT_ALIGNMENT); // Align button to left

        payNowButton.addActionListener(e -> {
            // Validasi form kartu kredit (contoh sederhana)
            if (payNowButton.getParent().getParent().isVisible()) { // Jika form kartu kredit terlihat
                if (cardNumberField.getText().isEmpty() || cardExpiryField.getText().isEmpty() ||
                    cvvField.getText().isEmpty() || cardNameField.getText().isEmpty()) {
                    JOptionPane.showMessageDialog(formPanel, "Semua field kartu kredit harus diisi.", "Validasi Pembayaran", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                // Tambahkan validasi format lebih lanjut (regex untuk nomor kartu, tanggal, dll.)
            }
            
            // Lanjutkan ke halaman Sukses
            if (viewController != null) {
                viewController.showCheckoutView(); // Asumsi CheckoutView adalah halaman sukses akhir
            } else {
                JOptionPane.showMessageDialog(formPanel, "Pembayaran sebesar " + payNowButton.getText() + " berhasil!", "Pembayaran Sukses", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        formPanel.add(payNowButton);

        return formPanel;
    }


    private JPanel createPaymentSummaryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        panel.setPreferredSize(new Dimension(panel.getPreferredSize().width, 280));
        panel.setMinimumSize(new Dimension(300, 280));
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

        summaryDetails.add(createSummaryRowPanel("Discount on MRP", discountOnMRPLabel, GREEN_DISCOUNT_TEXT, discountOnMRP));
        summaryDetails.add(Box.createVerticalStrut(5));

        summaryDetails.add(createSummaryRowPanel("Coupon savings", couponSavingsLabel, GREEN_DISCOUNT_TEXT, couponSavings));
        summaryDetails.add(Box.createVerticalStrut(5));

        summaryDetails.add(createSummaryRowPanel("Applicable GST", applicableGSTLabel, DARK_TEXT_COLOR, applicableGST));
        summaryDetails.add(Box.createVerticalStrut(5));

        summaryDetails.add(createSummaryRowPanel("Delivery", deliveryLabel, TEAL_FREE_TEXT, deliveryCharge, formatCurrency(deliveryCharge)));
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

        // Mengganti tombol "CONTINUE" dengan placeholder untuk payment summary
        // Tombol pembayaran sudah ada di form kartu kredit
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(new EmptyBorder(0, 20, 15, 20));
        // buttonPanel.add(continueButton); // Ini tidak lagi dibutuhkan di sini
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


    private void updateSummaryTotals() {
        totalMRP = 0;
        discountOnMRP = 0;
        couponSavings = 0;
        applicableGST = 0;
        // deliveryCharge seharusnya sudah diatur saat pemilihan jasa pengiriman dari AddressUI.
        // Untuk demo di PaymentUI saja, kita bisa set dummy jika tidak ada data dari AddressUI.
        if (deliveryCharge == 0 && cartItems != null && !cartItems.isEmpty()) {
            deliveryCharge = 15000.0; // Dummy default jika belum ada
        }


        if (cartItems != null) {
            for (ProductRepository.CartItem item : cartItems) {
                totalMRP += item.getOriginalPrice() * item.getQuantity();
                discountOnMRP += (item.getOriginalPrice() - item.getPrice()) * item.getQuantity();
            }
        }

        double finalTotal = totalMRP - discountOnMRP - couponSavings + applicableGST + deliveryCharge;

        if (totalMRPLabel != null) totalMRPLabel.setText(formatCurrency(totalMRP));
        if (discountOnMRPLabel != null) discountOnMRPLabel.setText(formatCurrency(discountOnMRP));
        if (couponSavingsLabel != null) couponSavingsLabel.setText(formatCurrency(couponSavings));
        if (applicableGSTLabel != null) applicableGSTLabel.setText(formatCurrency(applicableGST));

        if (deliveryLabel != null) deliveryLabel.setText(deliveryCharge == 0 ? "Free" : formatCurrency(deliveryCharge));
        if (finalTotalLabel != null) finalTotalLabel.setText(formatCurrency(finalTotal));
        
        // Update teks tombol PAY NOW dengan total final
        if (payNowButton != null) {
             payNowButton.setText("PAY NOW " + formatCurrency(finalTotal));
        }
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
            JFrame frame = new JFrame("Payment Selection");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1200, 800);

            ViewController dummyVC = new ViewController() {
                @Override public void showProductDetail(FavoritesUI.FavoriteItem product) { System.out.println("Dummy: Show Product Detail: " + product.getName()); }
                @Override public void showFavoritesView() { System.out.println("Dummy: Show Favorites View"); }
                @Override public void showDashboardView() { System.out.println("Dummy: Show Dashboard View"); }
                @Override public void showCartView() { System.out.println("Dummy: Show Cart View"); }
                @Override public void showProfileView() { System.out.println("Dummy: Show Profile View"); }
                @Override public void showOrdersView() { System.out.println("Dummy: Show Orders View"); }
                @Override public void showCheckoutView() { System.out.println("Dummy: Show Checkout View (Success)");
                    JOptionPane.showMessageDialog(null, "Simulasi Pindah ke Halaman Sukses", "Dummy Sukses", JOptionPane.INFORMATION_MESSAGE);
                }
                @Override public void showAddressView() { System.out.println("Dummy: Show Address View (Success)"); }
                @Override public void showPaymentView() { System.out.println("Dummy: Show Payment View (Success)"); }
            };

            PaymentUI paymentUI = new PaymentUI(dummyVC);
            frame.add(paymentUI);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}