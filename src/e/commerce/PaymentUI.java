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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;

import e.commerce.ProductRepository;
import e.commerce.ProductRepository.CartItem;
// Import kelas inner Address dan ShippingService dari AddressUI
import e.commerce.AddressUI.Address;
import e.commerce.AddressUI.ShippingService;


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
    private double deliveryCharge = 0;
    private List<ProductRepository.CartItem> cartItems;

    // Field form untuk Kartu Kredit/Debit
    private JPanel transferBankDetailsPanel;
    private JPanel eWalletDetailsPanel;
    private JPanel codDetailsPanel;
    private JPanel creditDebitCardFormPanel;

    private JTextField cardNumberField;
    private JTextField cardExpiryField;
    private JTextField cvvField;
    private JTextField cardNameField;
    private JButton payNowButton; // Ini akan menjadi tombol utama "BAYAR SEKARANG"

    private ButtonGroup paymentMethodGroup;

    private JPanel transferBankOptionPanel;
    private JPanel eWalletOptionPanel;
    private JPanel codOptionPanel;
    private JPanel creditDebitCardOptionPanel;

    private ViewController viewController;
    private User currentUser;

    // Field baru untuk menyimpan data yang diteruskan dari AddressUI
    private Address selectedShippingAddress;
    private ShippingService selectedDeliveryService;

    // Konstanta untuk warna tema
    private static final Color ORANGE_THEME = new Color(255, 69, 0);
    private static final Color LIGHT_GRAY_BACKGROUND = new Color(245, 245, 245);
    private static final Color BORDER_COLOR = new Color(230, 230, 230);
    private static final Color ACTIVE_BORDER_COLOR = new Color(255, 69, 0);
    private static final Color DARK_TEXT_COLOR = new Color(50, 50, 50);
    private static final Color GRAY_TEXT_COLOR = new Color(120, 120, 120);
    private static final Color GREEN_DISCOUNT_TEXT = new Color(76, 175, 80);
    private static final Color TEAL_FREE_TEXT = new Color(0, 150, 136);

    // Konstruktor yang dimodifikasi untuk menerima selectedAddress dan selectedShippingService
    public PaymentUI(ViewController viewController, Address selectedShippingAddress, ShippingService selectedDeliveryService) {
        this.viewController = viewController;
        this.selectedShippingAddress = selectedShippingAddress;
        this.selectedDeliveryService = selectedDeliveryService;

        setLayout(new BorderLayout());
        setBackground(LIGHT_GRAY_BACKGROUND);

        this.currentUser = Authentication.getCurrentUser();
        if (this.currentUser == null) {
            System.err.println("PaymentUI: Tidak ada user yang login. Halaman tidak dapat dimuat.");
            JOptionPane.showMessageDialog(this, "Anda harus login untuk melihat halaman ini.", "Error", JOptionPane.ERROR_MESSAGE);
            // Secara opsional, arahkan ke halaman login
            return;
        }

        loadCartItemsFromDatabase();
        // Gunakan biaya pengiriman dari selectedDeliveryService
        if (selectedDeliveryService != null) {
            deliveryCharge = selectedDeliveryService.price;
        } else {
            deliveryCharge = 0; // Fallback jika tidak ada jasa pengiriman yang dipilih
        }

        createComponents();
        updateSummaryTotals();
        updatePayNowButtonText(); // Perbarui teks tombol dengan total akhir
    }

    private void loadCartItemsFromDatabase() {
        if (currentUser != null) {
            cartItems = ProductRepository.getCartItemsForUser(currentUser.getId()); // Inisialisasi daftar kosong untuk mencegah NullPointerException
            System.out.println("DEBUG PaymentUI: Memuat " + cartItems.size() + " item dari database untuk user ID: " + currentUser.getId());
        } else {
            cartItems = new ArrayList<>();
            System.out.println("DEBUG PaymentUI: Tidak ada user yang login, keranjang kosong.");
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

        // Inisialisasi panel rincian metode pembayaran di sini (sebelum createSelectPaymentModeSection)
        transferBankDetailsPanel = createTransferBankDetailsPanel();
        eWalletDetailsPanel = createEWalletDetailsPanel();
        codDetailsPanel = createCodDetailsPanel();
        creditDebitCardFormPanel = createCreditDebitCardForm();

        // --- Select Payment Mode Section ---
        JPanel selectPaymentModePanel = createSelectPaymentModeSection();
        selectPaymentModePanel.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        leftColumnPanel.add(selectPaymentModePanel);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.65;
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
        gbc.weightx = 0.35;
        gbc.weighty = 1.0;
        mainContentPanel.add(rightColumnPanel, gbc);

        add(mainContentPanel, BorderLayout.CENTER);
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
                viewController.showAddressView();
            }
        });
        leftSection.add(backButton);
        headerPanel.add(leftSection, BorderLayout.WEST);

        JPanel navStepsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        navStepsPanel.setBackground(Color.WHITE);

        String[] steps = {"Keranjang", "Alamat", "Pembayaran", "Sukses"};
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

            if (steps[i].equals("Pembayaran")) {
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

    private JPanel createSelectPaymentModeSection() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(25, 25, 25, 25));

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(Color.WHITE);
        JLabel titleLabel = new JLabel("Pilih Mode Pembayaran");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setForeground(DARK_TEXT_COLOR);
        titlePanel.add(titleLabel, BorderLayout.WEST);
        panel.add(titlePanel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(new EmptyBorder(15, 0, 0, 0));

        paymentMethodGroup = new ButtonGroup();

        // --- Opsi Pembayaran: Transfer Bank ---
        JRadioButton transferBankRadioButton = new JRadioButton("Transfer Bank");
        transferBankRadioButton.setFont(new Font("Arial", Font.PLAIN, 14));
        transferBankRadioButton.setForeground(DARK_TEXT_COLOR);
        transferBankRadioButton.setBackground(Color.WHITE);
        transferBankRadioButton.setFocusPainted(false);
        transferBankRadioButton.setActionCommand("Transfer Bank");
        transferBankOptionPanel = createPaymentOptionPanel(transferBankRadioButton, transferBankDetailsPanel);
        transferBankRadioButton.addActionListener(e -> showPaymentDetails(transferBankDetailsPanel, transferBankOptionPanel));
        contentPanel.add(transferBankOptionPanel);
        paymentMethodGroup.add(transferBankRadioButton);
        contentPanel.add(Box.createVerticalStrut(10));

        // --- Opsi Pembayaran: E-Wallet (OVO, GoPay, DANA) ---
        JRadioButton eWalletRadioButton = new JRadioButton("E-Wallet (OVO, GoPay, DANA)");
        eWalletRadioButton.setFont(new Font("Arial", Font.PLAIN, 14));
        eWalletRadioButton.setForeground(DARK_TEXT_COLOR);
        eWalletRadioButton.setBackground(Color.WHITE);
        eWalletRadioButton.setFocusPainted(false);
        eWalletRadioButton.setActionCommand("E-Wallet"); // ActionCommand disederhanakan
        eWalletOptionPanel = createPaymentOptionPanel(eWalletRadioButton, eWalletDetailsPanel);
        eWalletRadioButton.addActionListener(e -> showPaymentDetails(eWalletDetailsPanel, eWalletOptionPanel));
        contentPanel.add(eWalletOptionPanel);
        paymentMethodGroup.add(eWalletRadioButton);
        contentPanel.add(Box.createVerticalStrut(10));

        // --- Opsi Pembayaran: Cash on Delivery ---
        JRadioButton codRadioButton = new JRadioButton("Cash on Delivery");
        codRadioButton.setFont(new Font("Arial", Font.PLAIN, 14));
        codRadioButton.setForeground(DARK_TEXT_COLOR);
        codRadioButton.setBackground(Color.WHITE);
        codRadioButton.setFocusPainted(false);
        codRadioButton.setActionCommand("Cash on Delivery");
        codOptionPanel = createPaymentOptionPanel(codRadioButton, codDetailsPanel);
        codRadioButton.addActionListener(e -> showPaymentDetails(codDetailsPanel, codOptionPanel));
        contentPanel.add(codOptionPanel);
        paymentMethodGroup.add(codRadioButton);
        contentPanel.add(Box.createVerticalStrut(10));

        // --- Opsi Pembayaran: Kartu Kredit/Debit ---
        JRadioButton creditDebitCardRadioButton = new JRadioButton("Kartu Kredit/Debit");
        creditDebitCardRadioButton.setFont(new Font("Arial", Font.PLAIN, 14));
        creditDebitCardRadioButton.setForeground(DARK_TEXT_COLOR);
        creditDebitCardRadioButton.setBackground(Color.WHITE);
        creditDebitCardRadioButton.setFocusPainted(false);
        creditDebitCardRadioButton.setActionCommand("Kartu Kredit/Debit");
        creditDebitCardOptionPanel = createPaymentOptionPanel(creditDebitCardRadioButton, creditDebitCardFormPanel);
        creditDebitCardRadioButton.addActionListener(e -> showPaymentDetails(creditDebitCardFormPanel, creditDebitCardOptionPanel));
        contentPanel.add(creditDebitCardOptionPanel);
        paymentMethodGroup.add(creditDebitCardRadioButton);

        // Set pilihan default (contoh: Kartu Kredit/Debit)
        creditDebitCardRadioButton.setSelected(true);
        showPaymentDetails(creditDebitCardFormPanel, creditDebitCardOptionPanel); // Tampilkan form default

        panel.add(contentPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createPaymentOptionPanel(JRadioButton radioButton, JPanel detailPanel) {
        JPanel optionPanel = new JPanel();
        optionPanel.setLayout(new BoxLayout(optionPanel, BoxLayout.Y_AXIS));
        optionPanel.setBackground(Color.WHITE);

        int collapsedHeight = radioButton.getPreferredSize().height + 20;
        optionPanel.setPreferredSize(new Dimension(optionPanel.getPreferredSize().width, collapsedHeight));
        optionPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, collapsedHeight));

        optionPanel.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_COLOR, 1, true),
            new EmptyBorder(10, 15, 10, 15)
        ));

        JPanel radioHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        radioHeader.setBackground(Color.WHITE);
        radioHeader.add(radioButton);
        optionPanel.add(radioHeader);

        optionPanel.add(detailPanel);
        detailPanel.setVisible(false);

        radioButton.addActionListener(e -> {
            updateOptionPanelBorders(optionPanel);
            revalidate();
            repaint();
        });

        optionPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getSource() != radioButton) {
                    radioButton.setSelected(true);
                    radioButton.getActionListeners()[0].actionPerformed(new ActionEvent(radioButton, ActionEvent.ACTION_PERFORMED, "clicked"));
                }
            }
        });

        return optionPanel;
    }

    private void updateOptionPanelBorders(JPanel activeOptionPanel) {
        transferBankOptionPanel.setBorder(BorderFactory.createCompoundBorder(new LineBorder(BORDER_COLOR, 1, true), new EmptyBorder(10, 15, 10, 15)));
        eWalletOptionPanel.setBorder(BorderFactory.createCompoundBorder(new LineBorder(BORDER_COLOR, 1, true), new EmptyBorder(10, 15, 10, 15)));
        codOptionPanel.setBorder(BorderFactory.createCompoundBorder(new LineBorder(BORDER_COLOR, 1, true), new EmptyBorder(10, 15, 10, 15)));
        creditDebitCardOptionPanel.setBorder(BorderFactory.createCompoundBorder(new LineBorder(BORDER_COLOR, 1, true), new EmptyBorder(10, 15, 10, 15)));

        if (activeOptionPanel != null) {
            activeOptionPanel.setBorder(BorderFactory.createCompoundBorder(new LineBorder(ACTIVE_BORDER_COLOR, 2, true), new EmptyBorder(9, 14, 9, 14)));
            activeOptionPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        }
    }

    private void showPaymentDetails(JPanel panelToShow, JPanel selectedOptionPanel) {
        transferBankDetailsPanel.setVisible(false);
        eWalletDetailsPanel.setVisible(false);
        codDetailsPanel.setVisible(false);
        creditDebitCardFormPanel.setVisible(false);

        int collapsedHeight = 36;
        transferBankOptionPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, collapsedHeight));
        eWalletOptionPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, collapsedHeight));
        codOptionPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, collapsedHeight));
        creditDebitCardOptionPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, collapsedHeight));

        if (panelToShow != null) {
            panelToShow.setVisible(true);
            if (selectedOptionPanel != null) {
                 selectedOptionPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            }
        }

        updateOptionPanelBorders(selectedOptionPanel);

        revalidate();
        repaint();
    }

    private JRadioButton createRadioButtonRowPanel(JRadioButton radioButton) {
        return radioButton;
    }

    private JPanel createTransferBankDetailsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(10, 0, 10, 0));

        JLabel title = new JLabel("Detail Transfer Bank");
        title.setFont(new Font("Arial", Font.BOLD, 14));
        title.setForeground(DARK_TEXT_COLOR);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(10));

        panel.add(createDetailRow("Bank BCA", "1234567890 (a.n. PT. Quantra Indonesia)"));
        panel.add(createDetailRow("Bank Mandiri", "0987654321 (a.n. PT. Quantra Indonesia)"));
        panel.add(Box.createVerticalStrut(10));

        String formattedAmount = formatCurrency(getTotalPaymentAmount());
        JLabel amountInstructionLabel = new JLabel("<html>Silakan transfer sejumlah <b>" + formattedAmount + "</b> dalam 24 jam.</html>");
        amountInstructionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(amountInstructionLabel);
        JLabel processingInstructionLabel = new JLabel("Pesanan akan diproses setelah pembayaran terverifikasi.");
        processingInstructionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(processingInstructionLabel);
        panel.add(Box.createVerticalStrut(20));

        // Tombol ini memanggil processPayment
        JButton confirmButton = new JButton("KONFIRMASI PEMBAYARAN");
        confirmButton.setBackground(ORANGE_THEME);
        confirmButton.setForeground(Color.WHITE);
        confirmButton.setBorderPainted(false);
        confirmButton.setFocusPainted(false);
        confirmButton.setFont(new Font("Arial", Font.BOLD, 14));
        confirmButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        confirmButton.setPreferredSize(new Dimension(200, 40));
        confirmButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        confirmButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        confirmButton.addActionListener(e -> processPayment("Transfer Bank"));
        panel.add(confirmButton);
        return panel;
    }

    private JPanel createEWalletDetailsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(10, 0, 10, 0));

        JLabel title = new JLabel("Pembayaran E-Wallet");
        title.setFont(new Font("Arial", Font.BOLD, 14));
        title.setForeground(DARK_TEXT_COLOR);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(10));

        JLabel instructionLabel = new JLabel("Pindai QR code atau ikuti instruksi di aplikasi e-wallet.");
        instructionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(instructionLabel);
        String formattedAmount = formatCurrency(getTotalPaymentAmount());
        JLabel totalAmountLabel = new JLabel("Total pembayaran: <b>" + formattedAmount + "</b>");
        totalAmountLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(totalAmountLabel);
        JLabel qrCodePlaceholder = new JLabel("[QR Code Placeholder / Instruksi E-Wallet]");
        qrCodePlaceholder.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(qrCodePlaceholder);
        panel.add(Box.createVerticalStrut(20));

        JButton payButton = new JButton("BAYAR DENGAN E-WALLET");
        payButton.setBackground(ORANGE_THEME);
        payButton.setForeground(Color.WHITE);
        payButton.setBorderPainted(false);
        payButton.setFocusPainted(false);
        payButton.setFont(new Font("Arial", Font.BOLD, 14));
        payButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        payButton.setPreferredSize(new Dimension(200, 40));
        payButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        payButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        payButton.addActionListener(e -> processPayment("E-Wallet"));
        panel.add(payButton);
        return panel;
    }

    private JPanel createCodDetailsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(10, 0, 10, 0));

        JLabel title = new JLabel("Cash on Delivery (COD)");
        title.setFont(new Font("Arial", Font.BOLD, 14));
        title.setForeground(DARK_TEXT_COLOR);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(10));

        JLabel instruction1Label = new JLabel("Bayar tunai kepada kurir saat pesanan Anda tiba.");
        instruction1Label.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(instruction1Label);
        String formattedAmount = formatCurrency(getTotalPaymentAmount());
        JLabel instruction2Label = new JLabel("Pastikan Anda memiliki uang tunai yang cukup sejumlah <b>" + formattedAmount + "</b>.");
        instruction2Label.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(instruction2Label);
        panel.add(Box.createVerticalStrut(20));

        JButton confirmCodButton = new JButton("KONFIRMASI COD");
        confirmCodButton.setBackground(ORANGE_THEME);
        confirmCodButton.setForeground(Color.WHITE);
        confirmCodButton.setBorderPainted(false);
        confirmCodButton.setFocusPainted(false);
        confirmCodButton.setFont(new Font("Arial", Font.BOLD, 14));
        confirmCodButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        confirmCodButton.setPreferredSize(new Dimension(200, 40));
        confirmCodButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        confirmCodButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        confirmCodButton.addActionListener(e -> processPayment("Cash on Delivery"));
        panel.add(confirmCodButton);
        return panel;
    }

    private JPanel createDetailRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(Color.WHITE);
        JLabel lbl = new JLabel(label + ":");
        lbl.setFont(new Font("Arial", Font.PLAIN, 14));
        lbl.setForeground(DARK_TEXT_COLOR);
        row.add(lbl, BorderLayout.WEST);

        JLabel val = new JLabel("<html><b>" + value + "</b></html>");
        val.setFont(new Font("Arial", Font.PLAIN, 14));
        val.setForeground(DARK_TEXT_COLOR);
        val.setHorizontalAlignment(SwingConstants.RIGHT);
        row.add(val, BorderLayout.EAST);
        row.setBorder(new EmptyBorder(2, 0, 2, 0));
        return row;
    }

    private JPanel createCreditDebitCardForm() {
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(new EmptyBorder(10, 0, 10, 0));

        JLabel formTitle = new JLabel("Detail Kartu Kredit/Debit");
        formTitle.setFont(new Font("Arial", Font.BOLD, 14));
        formTitle.setForeground(DARK_TEXT_COLOR);
        formTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(formTitle);
        formPanel.add(Box.createVerticalStrut(15));

        formPanel.add(createFormField("Nomor Kartu", cardNumberField = createStyledTextField("xxxx xxxx xxxx xxxx")));
        formPanel.add(Box.createVerticalStrut(10));

        JPanel expiryCvvPanel = new JPanel(new GridBagLayout());
        expiryCvvPanel.setBackground(Color.WHITE);
        expiryCvvPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 10);
        gbc.weightx = 0.5;

        JPanel expiryPanel = createFormField("Tanggal Kedaluwarsa (MM/YY)", cardExpiryField = createStyledTextField("MM/YY"));
        gbc.gridx = 0;
        expiryCvvPanel.add(expiryPanel, gbc);

        JPanel cvvMainPanel = new JPanel(new BorderLayout());
        cvvMainPanel.setBackground(Color.WHITE);

        JPanel cvvLabelPanel = new JPanel(new BorderLayout());
        cvvLabelPanel.setBackground(Color.WHITE);
        JLabel cvvLabel = new JLabel("CVV");
        cvvLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        cvvLabel.setForeground(DARK_TEXT_COLOR);

        JButton helpButton = new JButton("?");
        helpButton.setFont(new Font("Arial", Font.BOLD, 10));
        helpButton.setForeground(Color.WHITE);
        helpButton.setBackground(GRAY_TEXT_COLOR);
        helpButton.setBorderPainted(false);
        helpButton.setFocusPainted(false);
        helpButton.setPreferredSize(new Dimension(20, 20));
        helpButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        helpButton.addActionListener(e -> JOptionPane.showMessageDialog(formPanel, "CVV adalah 3 atau 4 digit nomor keamanan di belakang kartu Anda.", "Apa itu CVV?", JOptionPane.INFORMATION_MESSAGE));

        cvvLabelPanel.add(cvvLabel, BorderLayout.WEST);
        cvvLabelPanel.add(helpButton, BorderLayout.EAST);

        cvvMainPanel.add(cvvLabelPanel, BorderLayout.NORTH);
        cvvMainPanel.add(Box.createVerticalStrut(5), BorderLayout.CENTER);

        cvvField = createStyledTextField("123");
        JPanel cvvFieldWrapper = new JPanel(new BorderLayout());
        cvvFieldWrapper.setBackground(Color.WHITE);
        cvvFieldWrapper.add(cvvField, BorderLayout.CENTER);
        cvvMainPanel.add(cvvFieldWrapper, BorderLayout.SOUTH);

        gbc.gridx = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        expiryCvvPanel.add(cvvMainPanel, gbc);

        formPanel.add(expiryCvvPanel);
        formPanel.add(Box.createVerticalStrut(10));

        formPanel.add(createFormField("Nama (Sesuai Kartu)", cardNameField = createStyledTextField("Nama Lengkap")));
        formPanel.add(Box.createVerticalStrut(10));

        JCheckBox saveCardCheckbox = new JCheckBox("Simpan kartu ini dengan aman");
        saveCardCheckbox.setFont(new Font("Arial", Font.PLAIN, 12));
        saveCardCheckbox.setBackground(Color.WHITE);
        saveCardCheckbox.setFocusPainted(false);
        saveCardCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        formPanel.add(saveCardCheckbox);
        formPanel.add(Box.createVerticalStrut(20));

        // Tombol utama "BAYAR SEKARANG" untuk form ini
        payNowButton = new JButton("BAYAR SEKARANG " + formatCurrency(getTotalPaymentAmount()));
        payNowButton.setBackground(ORANGE_THEME);
        payNowButton.setForeground(Color.WHITE);
        payNowButton.setBorderPainted(false);
        payNowButton.setFocusPainted(false);
        payNowButton.setFont(new Font("Arial", Font.BOLD, 14));
        payNowButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        payNowButton.setPreferredSize(new Dimension(200, 40));
        payNowButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        payNowButton.setAlignmentX(Component.LEFT_ALIGNMENT);

        payNowButton.addActionListener(e -> {
            ButtonModel selectedMethodModel = paymentMethodGroup.getSelection();
            if (selectedMethodModel == null) {
                JOptionPane.showMessageDialog(formPanel, "Silakan pilih metode pembayaran.", "Validasi Pembayaran", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String selectedPaymentMethod = selectedMethodModel.getActionCommand();
            processPayment(selectedPaymentMethod); // Panggil metode proses pembayaran
        });
        formPanel.add(payNowButton);

        return formPanel;
    }

    private JPanel createFormField(String labelText, JTextField textField) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Arial", Font.PLAIN, 12));
        label.setForeground(DARK_TEXT_COLOR);
        label.setBorder(new EmptyBorder(0, 0, 5, 0));
        panel.add(label, BorderLayout.NORTH);

        textField.setFont(new Font("Arial", Font.PLAIN, 14));
        textField.setBorder(new LineBorder(BORDER_COLOR));
        panel.add(textField, BorderLayout.CENTER);

        return panel;
    }

    private JTextField createStyledTextField(String placeholder) {
        JTextField field = new JTextField();
        field.setFont(new Font("Arial", Font.PLAIN, 14));
        field.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(206, 212, 218), 1, true),
            new EmptyBorder(8, 10, 8, 10)
        ));
        field.setBackground(Color.WHITE);

        field.setForeground(new Color(150, 150, 150));
        field.setText(placeholder);

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(DARK_TEXT_COLOR);
                }
                field.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(ORANGE_THEME, 2, true),
                    new EmptyBorder(7, 9, 7, 9)
                ));
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(new Color(150, 150, 150));
                }
                field.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(BORDER_COLOR, 1, true),
                    new EmptyBorder(8, 10, 8, 10)
                ));
            }
        });
        return field;
    }

    // Logika pemrosesan pembayaran utama (diekstraksi ke metode baru)
    private void processPayment(String paymentMethod) {
        if (cartItems == null || cartItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Keranjang Anda kosong. Tidak dapat memproses pembayaran.", "Keranjang Kosong", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (selectedShippingAddress == null || selectedDeliveryService == null) {
            JOptionPane.showMessageDialog(this, "Informasi alamat pengiriman atau jasa pengiriman tidak lengkap. Harap kembali ke halaman alamat.", "Error Pembayaran", JOptionPane.ERROR_MESSAGE);
            if (viewController != null) {
                viewController.showAddressView(); // Arahkan kembali ke AddressUI
            }
            return;
        }

        if (paymentMethod.equals("Kartu Kredit/Debit")) {
            if (cardNumberField.getText().isEmpty() || cardExpiryField.getText().isEmpty() ||
                cvvField.getText().isEmpty() || cardNameField.getText().isEmpty() ||
                cardNumberField.getText().equals("xxxx xxxx xxxx xxxx") ||
                cardExpiryField.getText().equals("MM/YY") ||
                cvvField.getText().equals("123") ||
                cardNameField.getText().equals("Nama Lengkap")) {
                JOptionPane.showMessageDialog(this, "Semua field kartu kredit harus diisi.", "Validasi Pembayaran", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!isValidCardNumber(cardNumberField.getText())) {
                JOptionPane.showMessageDialog(this, "Nomor kartu tidak valid.", "Validasi Pembayaran", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!isValidExpiryDate(cardExpiryField.getText())) {
                JOptionPane.showMessageDialog(this, "Tanggal kedaluwarsa tidak valid (MM/YY).", "Validasi Pembayaran", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!isValidCVV(cvvField.getText())) {
                JOptionPane.showMessageDialog(this, "CVV tidak valid (3 atau 4 digit).", "Validasi Pembayaran", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        int orderId = -1;
        try {
            orderId = ProductRepository.createOrder(
                currentUser.getId(),
                cartItems,
                getTotalPaymentAmount(),
                selectedShippingAddress,
                selectedDeliveryService,
                paymentMethod
            );

            if (orderId != -1) {
                JOptionPane.showMessageDialog(this, "Pembayaran berhasil dengan metode: " + paymentMethod + ". Nomor Pesanan: " + orderId, "Pembayaran Sukses", JOptionPane.INFORMATION_MESSAGE);
                if (viewController != null) {
                    viewController.showSuccessView(orderId); // Teruskan ID pesanan yang sebenarnya
                }
            } else {
                JOptionPane.showMessageDialog(this, "Gagal membuat pesanan. Terjadi kesalahan internal.", "Error Pesanan", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException ex) {
            System.err.println("Error membuat pesanan: " + ex.getMessage());
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal memproses pembayaran. Error database: " + ex.getMessage(), "Error Database", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Metode Validasi
    private boolean isValidCardNumber(String cardNumber) {
        String cleanNumber = cardNumber.replaceAll("\\s+", "");
        return cleanNumber.matches("\\d{13,19}");
    }

    private boolean isValidExpiryDate(String expiry) {
        if (!expiry.matches("(0[1-9]|1[0-2])/\\d{2}")) {
            return false;
        }
        String[] parts = expiry.split("/");
        int month = Integer.parseInt(parts[0]);
        int year = Integer.parseInt(parts[1]) + 2000;

        LocalDate currentDate = LocalDate.now();
        LocalDate expiryDate = LocalDate.of(year, month, 1).plusMonths(1).minusDays(1);

        return expiryDate.isAfter(currentDate);
    }

    private boolean isValidCVV(String cvv) {
        return cvv.matches("\\d{3,4}");
    }

    private JPanel createPaymentSummaryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        panel.setPreferredSize(new Dimension(panel.getPreferredSize().width, 280));
        panel.setMinimumSize(new Dimension(300, 280));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));

        panel.add(createSectionTitle("Ringkasan Pembayaran", null, null), BorderLayout.NORTH);

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

        summaryDetails.add(createSummaryRowPanel("Diskon pada MRP", discountOnMRPLabel, GREEN_DISCOUNT_TEXT, discountOnMRP));
        summaryDetails.add(Box.createVerticalStrut(5));

        summaryDetails.add(createSummaryRowPanel("Penghematan Kupon", couponSavingsLabel, GREEN_DISCOUNT_TEXT, couponSavings));
        summaryDetails.add(Box.createVerticalStrut(5));

        summaryDetails.add(createSummaryRowPanel("GST yang Berlaku", applicableGSTLabel, DARK_TEXT_COLOR, applicableGST));
        summaryDetails.add(Box.createVerticalStrut(5));

        // Display "Gratis" jika biaya pengiriman 0
        summaryDetails.add(createSummaryRowPanel("Pengiriman", deliveryLabel, TEAL_FREE_TEXT, deliveryCharge, formatCurrency(deliveryCharge)));
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

        // Jika customValueText disediakan, gunakan itu. Jika tidak, format jumlah mata uang.
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
        // deliveryCharge sudah diatur dari selectedDeliveryService di konstruktor, tidak perlu dihitung ulang dari awal kecuali logika berubah

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

        if (deliveryLabel != null) {
            // Tampilkan "Gratis" hanya jika deliveryCharge benar-benar 0.0
            deliveryLabel.setText(deliveryCharge == 0.0 ? "Gratis" : formatCurrency(deliveryCharge));
        }
        if (finalTotalLabel != null) finalTotalLabel.setText(formatCurrency(finalTotal));

        // Perbarui teks tombol payNowButton
        updatePayNowButtonText();
    }

    // Metode untuk memperbarui teks tombol "BAYAR SEKARANG"
    private void updatePayNowButtonText() {
        if (payNowButton != null) {
             payNowButton.setText("BAYAR SEKARANG " + formatCurrency(getTotalPaymentAmount()));
        }
    }

    // --- Metode untuk menghitung total pembayaran akhir ---
    private double getTotalPaymentAmount() {
        return totalMRP - discountOnMRP - couponSavings + applicableGST + deliveryCharge;
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
                        System.err.println("ImageIO.read mengembalikan null untuk gambar BLOB untuk colorHex: " + fallbackColor);
                    }
                } catch (IOException e) {
                    System.err.println("Error memuat gambar dari BLOB untuk colorHex: " + fallbackColor + ": " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.out.println("Tidak ada data gambar BLOB yang disediakan untuk panel gambar (fallback ke warna untuk ID: " + fallbackColor + ").");
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
                String text = "TIDAK ADA GAMBAR";
                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(text);
                int textHeight = fm.getHeight();
                g2d.drawString(text, (getWidth() - textWidth) / 2, (getHeight() + textHeight / 2) / 2 - fm.getDescent());
            }
        }
    }

    // Metode main untuk pengujian (DIMODIFIKASI untuk meneruskan alamat dan pengiriman dummy)
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Pemilihan Pembayaran");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1200, 800);

            // User Dummy untuk pengujian
            User dummyUser = new User(1, "testuser", "hashedpass", "test@example.com", "1234567890123456", "08123456789", null, null, "user", true);
            try {
                java.lang.reflect.Field field = Authentication.class.getDeclaredField("currentUser");
                field.setAccessible(true);
                field.set(null, dummyUser);
                System.out.println("User dummy diatur untuk pengujian di PaymentUI.");
            } catch (Exception e) {
                System.err.println("Gagal mengatur user dummy untuk pengujian di PaymentUI: " + e.getMessage());
            }

            // Alamat dan Jasa Pengiriman Dummy untuk pengujian
            Address dummyAddress = new Address(1, 1, "Rumah", "Jl. Dummy No. 123", "Jakarta", "DKI Jakarta", "10110", "Indonesia");
            ShippingService dummyShipping = new ShippingService("JNE", "3-4 Hari", 15000.0);

            ViewController dummyVC = new ViewController() {
                @Override public void showProductDetail(FavoritesUI.FavoriteItem product) { System.out.println("Dummy: Tampilkan Detail Produk: " + product.getName()); }
                @Override public void showFavoritesView() { System.out.println("Dummy: Tampilkan Tampilan Favorit"); }
                @Override public void showDashboardView() { System.out.println("Dummy: Tampilkan Tampilan Dashboard"); }
                @Override public void showCartView() { System.out.println("Dummy: Tampilkan Tampilan Keranjang"); }
                @Override public void showProfileView() { System.out.println("Dummy: Tampilkan Tampilan Profil"); }
                @Override public void showOrdersView() { System.out.println("Dummy: Tampilkan Tampilan Pesanan"); }
                @Override public void showCheckoutView() { System.out.println("Dummy: Tampilkan Tampilan Checkout (Sukses)"); }
                @Override public void showAddressView() { System.out.println("Dummy: Tampilkan Tampilan Alamat (Sukses)"); }
                @Override public void showPaymentView(Address selectedAddress, ShippingService selectedShippingService) {
                    System.out.println("Dummy: Tampilkan Tampilan Pembayaran (Sukses) dengan alamat dan pengiriman.");
                }
                @Override public void showSuccessView(int orderId) {
                    System.out.println("Dummy: Tampilkan Tampilan Sukses (Sukses) dengan ID pesanan: " + orderId);
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

            PaymentUI paymentUI = new PaymentUI(dummyVC, dummyAddress, dummyShipping);
            frame.add(paymentUI);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}