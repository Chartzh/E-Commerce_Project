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
import java.util.Map;

import e.commerce.ProductRepository;
import e.commerce.ProductRepository.CartItem;

public class AddressUI extends JPanel {
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

    private JLabel currentAddressNameLabel;
    private JLabel currentAddressDetailLabel;
    private JLabel currentAddressMobileLabel;
    private JLabel deliveryEstimateHeaderLabel;

    private JPanel shippingServiceContainer;

    private ViewController viewController;
    private User currentUser;
    private List<ProductRepository.CartItem> cartItems;

    private List<Address> savedAddresses;
    private Address selectedAddress;

    private Map<String, ShippingService> shippingServices;
    private ShippingService selectedShippingService;

    private static final Color ORANGE_THEME = new Color(255, 69, 0);
    private static final Color LIGHT_GRAY_BACKGROUND = new Color(245, 245, 245);
    private static final Color BORDER_COLOR = new Color(230, 230, 230);
    private static final Color DARK_TEXT_COLOR = new Color(50, 50, 50);
    private static final Color GRAY_TEXT_COLOR = new Color(120, 120, 120);
    private static final Color GREEN_DISCOUNT_TEXT = new Color(76, 175, 80);
    private static final Color TEAL_FREE_TEXT = new Color(0, 150, 136);

    private static class Address {
        String label;
        String name;
        String street;
        String cityStateZip;
        String mobile;
        boolean isDefault;

        public Address(String label, String name, String street, String cityStateZip, String mobile, boolean isDefault) {
            this.label = label;
            this.name = name;
            this.street = street;
            this.cityStateZip = cityStateZip;
            this.mobile = mobile;
            this.isDefault = isDefault;
        }

        public String getFullAddressDetails() {
            return name + "<br>" + street + "<br>" + cityStateZip + "<br>Mobile: " + mobile;
        }
    }

    private static class ShippingService {
        String name;
        String arrivalEstimate;
        double price;

        public ShippingService(String name, String arrivalEstimate, double price) {
            this.name = name;
            this.arrivalEstimate = arrivalEstimate;
            this.price = price;
        }
    }

    public AddressUI(ViewController viewController) {
        this.viewController = viewController;
        setLayout(new BorderLayout());
        setBackground(LIGHT_GRAY_BACKGROUND);

        this.currentUser = Authentication.getCurrentUser();
        if (this.currentUser == null) {
            System.err.println("AddressUI: Tidak ada user yang login. Halaman tidak dapat dimuat.");
            JOptionPane.showMessageDialog(this, "Anda harus login untuk melihat halaman ini.", "Error", JOptionPane.ERROR_MESSAGE);
        }

        loadCartItemsFromDatabase();
        initAddresses();
        initShippingServices();
        createComponents();
        updateSummaryTotals();
    }

    private void loadCartItemsFromDatabase() {
        if (currentUser != null) {
            cartItems = ProductRepository.getCartItemsForUser(currentUser.getId());
            System.out.println("DEBUG AddressUI: Memuat " + cartItems.size() + " item dari database untuk user ID: " + currentUser.getId());
        } else {
            cartItems = new ArrayList<>();
            System.out.println("DEBUG AddressUI: Tidak ada user yang login, keranjang kosong.");
        }
    }

    private void initAddresses() {
        savedAddresses = new ArrayList<>();
        savedAddresses.add(new Address("Home", "Sajith Kumar", "A/8, 2nd Floor, Amrut Purushottama Phule Society,", "Mumbai, Maharashtra - 400024", "+91 1234567890", true));
        savedAddresses.add(new Address("Office", "Sajith Kumar", "Office Complex, Unit 501, Business Park", "Mumbai, Maharashtra - 400010", "+91 9876543210", false));
        savedAddresses.add(new Address("Other", "Sajith Kumar", "101, Creative Hub, Art Street", "Pune, Maharashtra - 411001", "+91 7654321098", false));

        if (!savedAddresses.isEmpty()) {
            selectedAddress = savedAddresses.stream().filter(a -> a.isDefault).findFirst().orElse(savedAddresses.get(0));
        }
    }

    private void initShippingServices() {
        shippingServices = new LinkedHashMap<>();
        shippingServices.put("JNE", new ShippingService("JNE", "3-4 Hari", 15000.0));
        shippingServices.put("J&T", new ShippingService("J&T", "2-3 Hari", 14000.0));
        shippingServices.put("TIKI", new ShippingService("TIKI", "3-5 Hari", 16000.0));
        shippingServices.put("SiCepat", new ShippingService("SiCepat", "1-2 Hari", 18000.0));
        shippingServices.put("AnterAja", new ShippingService("AnterAja", "2-4 Hari", 13000.0));
        shippingServices.put("GoSend/GrabExpress", new ShippingService("GoSend/GrabExpress", "Hari Ini", 25000.0));

        if (!shippingServices.isEmpty()) {
            selectedShippingService = shippingServices.values().iterator().next();
            deliveryCharge = selectedShippingService.price;
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

        JPanel deliveryAddressPanel = createDeliveryAddressSection();
        deliveryAddressPanel.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        leftColumnPanel.add(deliveryAddressPanel);
        leftColumnPanel.add(Box.createVerticalStrut(15));

        JPanel shippingServicesSection = createShippingServicesSection();
        shippingServicesSection.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        leftColumnPanel.add(shippingServicesSection);
        leftColumnPanel.add(Box.createVerticalGlue());

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

        updateDisplayedAddress();
        populateShippingServices();
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
                viewController.showCartView();
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

            if (steps[i].equals("Address")) {
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

    private JPanel createDeliveryAddressSection() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        // Padding luar panel
        panel.setBorder(new EmptyBorder(25, 25, 25, 25)); //

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(Color.WHITE);
        JLabel titleLabel = new JLabel("Delivery Address");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setForeground(DARK_TEXT_COLOR);
        titlePanel.add(titleLabel, BorderLayout.WEST);

        JButton changeButton = new JButton("CHANGE");
        changeButton.setFont(new Font("Arial", Font.BOLD, 12));
        changeButton.setForeground(ORANGE_THEME);
        changeButton.setBackground(Color.WHITE);
        changeButton.setBorderPainted(false);
        changeButton.setFocusPainted(false);
        changeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        changeButton.addActionListener(e -> showAddressSelectionDialog());
        titlePanel.add(changeButton, BorderLayout.EAST);
        panel.add(titlePanel, BorderLayout.NORTH);

        // Menggunakan BoxLayout untuk detail alamat agar lebih rapih dan padding mudah dikontrol
        JPanel addressDetailsContainer = new JPanel();
        addressDetailsContainer.setLayout(new BoxLayout(addressDetailsContainer, BoxLayout.Y_AXIS));
        addressDetailsContainer.setBackground(Color.WHITE);
        addressDetailsContainer.setBorder(new EmptyBorder(15, 0, 15, 0)); // Padding vertikal dari title dan status

        // Baris Nama
        currentAddressNameLabel = new JLabel();
        currentAddressNameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        currentAddressNameLabel.setForeground(DARK_TEXT_COLOR);
        currentAddressNameLabel.setAlignmentX(Component.LEFT_ALIGNMENT); // Pastikan rata kiri
        currentAddressNameLabel.setBorder(new EmptyBorder(0, 0, 5, 0)); // Padding bawah antar baris
        addressDetailsContainer.add(currentAddressNameLabel);

        // Baris Detail Alamat
        currentAddressDetailLabel = new JLabel();
        currentAddressDetailLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        currentAddressDetailLabel.setForeground(DARK_TEXT_COLOR);
        currentAddressDetailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        currentAddressDetailLabel.setBorder(new EmptyBorder(0, 0, 5, 0)); // Padding bawah
        addressDetailsContainer.add(currentAddressDetailLabel);

        // Baris Mobile
        currentAddressMobileLabel = new JLabel();
        currentAddressMobileLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        currentAddressMobileLabel.setForeground(DARK_TEXT_COLOR);
        currentAddressMobileLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        currentAddressMobileLabel.setBorder(new EmptyBorder(0, 0, 5, 0)); // Padding bawah
        addressDetailsContainer.add(currentAddressMobileLabel);

        // Status and delivery estimate
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBackground(Color.WHITE);
        statusPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

        String orangeHexColor = String.format("#%06x", ORANGE_THEME.getRGB() & 0xFFFFFF);
        JLabel codLabel = new JLabel("<html><font color='" + orangeHexColor + "'>&#9679;</font> Cash on delivery available</html>");
        codLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        codLabel.setForeground(DARK_TEXT_COLOR);
        statusPanel.add(codLabel, BorderLayout.WEST);

        deliveryEstimateHeaderLabel = new JLabel("Estimated delivery 24 - 25 Sep"); // Dummy date
        deliveryEstimateHeaderLabel.setFont(new Font("Arial", Font.BOLD, 13));
        deliveryEstimateHeaderLabel.setForeground(DARK_TEXT_COLOR);
        statusPanel.add(deliveryEstimateHeaderLabel, BorderLayout.EAST);

        panel.add(addressDetailsContainer, BorderLayout.CENTER);
        panel.add(statusPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void updateDisplayedAddress() {
        if (selectedAddress != null) {
            currentAddressNameLabel.setText(selectedAddress.name);
            currentAddressDetailLabel.setText("<html>" + selectedAddress.street + "<br>" + selectedAddress.cityStateZip + "</html>");
            currentAddressMobileLabel.setText("Mobile: " + selectedAddress.mobile);
            // Tanggal estimasi di header Delivery Address
            deliveryEstimateHeaderLabel.setText("Estimated delivery " + calculateDeliveryDates("header")); //
        } else {
            currentAddressNameLabel.setText("No address selected");
            currentAddressDetailLabel.setText("");
            currentAddressMobileLabel.setText("");
            deliveryEstimateHeaderLabel.setText("");
        }
    }

    private void showAddressSelectionDialog() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Pilih Alamat Pengiriman", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(450, 400); // Slightly larger dialog
        dialog.setLocationRelativeTo(this);

        JPanel addressListPanel = new JPanel();
        addressListPanel.setLayout(new BoxLayout(addressListPanel, BoxLayout.Y_AXIS));
        addressListPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        addressListPanel.setBackground(LIGHT_GRAY_BACKGROUND); // Light gray background for the list

        ButtonGroup addressButtonGroup = new ButtonGroup();

        for (Address addr : savedAddresses) {
            JPanel addressCard = createAddressCard(addr, addressButtonGroup);
            addressListPanel.add(addressCard);
            addressListPanel.add(Box.createVerticalStrut(10));
        }

        JScrollPane scrollPane = new JScrollPane(addressListPanel);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(LIGHT_GRAY_BACKGROUND); // Match list background
        dialog.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(Color.WHITE);
        JButton closeButton = new JButton("Tutup");
        closeButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(closeButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private JPanel createAddressCard(Address addr, ButtonGroup buttonGroup) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(new LineBorder(BORDER_COLOR, 1, true));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        card.setPreferredSize(new Dimension(400, 100)); // Fixed size for cards
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100)); // Prevent vertical expansion

        JPanel radioAndLabelPanel = new JPanel(new BorderLayout());
        radioAndLabelPanel.setBackground(Color.WHITE);
        radioAndLabelPanel.setBorder(new EmptyBorder(10, 10, 5, 10)); // Padding

        JRadioButton radioButton = new JRadioButton();
        radioButton.setBackground(Color.WHITE);
        radioButton.setFocusPainted(false);
        radioButton.setActionCommand(addr.label); // Store label as action command

        JLabel labelTag = new JLabel(addr.label);
        labelTag.setFont(new Font("Arial", Font.BOLD, 14));
        labelTag.setForeground(DARK_TEXT_COLOR);

        if (addr.isDefault) {
            JLabel defaultTag = new JLabel("Default");
            defaultTag.setFont(new Font("Arial", Font.PLAIN, 11));
            defaultTag.setForeground(Color.WHITE);
            defaultTag.setBackground(ORANGE_THEME);
            defaultTag.setOpaque(true);
            defaultTag.setBorder(new EmptyBorder(2, 5, 2, 5));
            defaultTag.setHorizontalAlignment(SwingConstants.CENTER);
            defaultTag.setVerticalAlignment(SwingConstants.CENTER);

            JPanel tagPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            tagPanel.setBackground(Color.WHITE);
            tagPanel.add(labelTag);
            tagPanel.add(defaultTag);
            radioAndLabelPanel.add(tagPanel, BorderLayout.CENTER);
        } else {
            radioAndLabelPanel.add(labelTag, BorderLayout.CENTER);
        }

        radioAndLabelPanel.add(radioButton, BorderLayout.WEST); // Radio button on the left

        JLabel fullAddressDetailLabel = new JLabel("<html>" + addr.name + "<br>" + addr.street + "<br>" + addr.cityStateZip + "<br>Mobile: " + addr.mobile + "</html>");
        fullAddressDetailLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        fullAddressDetailLabel.setForeground(GRAY_TEXT_COLOR);
        fullAddressDetailLabel.setBorder(new EmptyBorder(0, 10, 10, 10)); // Padding

        card.add(radioAndLabelPanel, BorderLayout.NORTH);
        card.add(fullAddressDetailLabel, BorderLayout.CENTER);

        // Select the radio button if it's the currently selected address
        if (selectedAddress != null && addr.label.equals(selectedAddress.label)) {
            radioButton.setSelected(true);
        }

        radioButton.addActionListener(e -> {
            for (Address sa : savedAddresses) {
                if (sa.label.equals(e.getActionCommand())) {
                    selectedAddress = sa;
                    updateDisplayedAddress(); // Update the main display
                    break;
                }
            }
        });

        buttonGroup.add(radioButton);

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getSource() != radioButton) {
                    radioButton.setSelected(true);
                    selectedAddress = savedAddresses.stream()
                                    .filter(sa -> sa.label.equals(radioButton.getActionCommand()))
                                    .findFirst().orElse(null);
                    updateDisplayedAddress();
                }
            }
        });

        return card;
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

        JButton continueButton = new JButton("CONTINUE");
        continueButton.setBackground(ORANGE_THEME);
        continueButton.setForeground(Color.WHITE);
        continueButton.setBorderPainted(false);
        continueButton.setFocusPainted(false);
        continueButton.setFont(new Font("Arial", Font.BOLD, 14));
        continueButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        continueButton.setPreferredSize(new Dimension(200, 40));
        continueButton.addActionListener(e -> {
            if (selectedAddress == null) {
                JOptionPane.showMessageDialog(this, "Silakan pilih alamat pengiriman.", "Peringatan", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (selectedShippingService == null) {
                JOptionPane.showMessageDialog(this, "Silakan pilih jasa pengiriman.", "Peringatan", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (viewController != null) {
                viewController.showPaymentView();
            } else {
                JOptionPane.showMessageDialog(this, "Proceeding to Payment page.", "Payment", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(new EmptyBorder(0, 20, 15, 20));
        buttonPanel.add(continueButton);
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

    private JPanel createShippingServicesSection() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel titleLabel = new JLabel("Jasa Pengiriman");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setForeground(DARK_TEXT_COLOR);
        panel.add(titleLabel, BorderLayout.NORTH);

        JLabel subtitleLabel = new JLabel("Pilih metode pengiriman yang Anda inginkan.");
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        subtitleLabel.setForeground(GRAY_TEXT_COLOR);
        panel.add(subtitleLabel, BorderLayout.CENTER);

        shippingServiceContainer = new JPanel();
        shippingServiceContainer.setLayout(new BoxLayout(shippingServiceContainer, BoxLayout.Y_AXIS));
        shippingServiceContainer.setBackground(Color.WHITE);
        shippingServiceContainer.setBorder(new EmptyBorder(10, 0, 0, 0));

        JScrollPane scrollPane = new JScrollPane(shippingServiceContainer);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        panel.add(scrollPane, BorderLayout.SOUTH);

        return panel;
    }

    private void populateShippingServices() {
        shippingServiceContainer.removeAll();
        ButtonGroup serviceButtonGroup = new ButtonGroup();

        for (Map.Entry<String, ShippingService> entry : shippingServices.entrySet()) {
            ShippingService service = entry.getValue();
            JPanel servicePanel = createShippingServicePanel(service, serviceButtonGroup);
            shippingServiceContainer.add(servicePanel);
            shippingServiceContainer.add(Box.createVerticalStrut(10));
        }

        if (selectedShippingService != null) {
            for (AbstractButton button : java.util.Collections.list(serviceButtonGroup.getElements())) {
                if (button.getActionCommand().equals(selectedShippingService.name)) {
                    button.setSelected(true);
                    break;
                }
            }
        } else if (!shippingServices.isEmpty()) {
            selectedShippingService = shippingServices.values().iterator().next();
            deliveryCharge = selectedShippingService.price;
            java.util.Collections.list(serviceButtonGroup.getElements()).get(0).setSelected(true);
        }

        shippingServiceContainer.revalidate();
        shippingServiceContainer.repaint();
        updateSummaryTotals();
    }

    private JPanel createShippingServicePanel(ShippingService service, ButtonGroup buttonGroup) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(new LineBorder(BORDER_COLOR, 1, true));
        panel.setPreferredSize(new Dimension(panel.getPreferredSize().width, 70));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        panel.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JRadioButton radioButton = new JRadioButton(service.name);
        radioButton.setFont(new Font("Arial", Font.BOLD, 14));
        radioButton.setBackground(Color.WHITE);
        radioButton.setFocusPainted(false);
        radioButton.setActionCommand(service.name);

        radioButton.addActionListener(e -> {
            selectedShippingService = shippingServices.get(e.getActionCommand());
            deliveryCharge = selectedShippingService.price;
            updateSummaryTotals();
        });
        buttonGroup.add(radioButton);

        JPanel leftContent = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        leftContent.setBackground(Color.WHITE);
        leftContent.add(radioButton);

        panel.add(leftContent, BorderLayout.WEST);

        JPanel rightContent = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        rightContent.setBackground(Color.WHITE);

        JLabel estimateLabel = new JLabel(service.arrivalEstimate);
        estimateLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        estimateLabel.setForeground(GRAY_TEXT_COLOR);
        rightContent.add(estimateLabel);

        JLabel priceLabel = new JLabel(formatCurrency(service.price));
        priceLabel.setFont(new Font("Arial", Font.BOLD, 14));
        priceLabel.setForeground(DARK_TEXT_COLOR);
        rightContent.add(priceLabel);

        panel.add(rightContent, BorderLayout.EAST);

        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getSource() != radioButton) {
                    radioButton.setSelected(true);
                    selectedShippingService = shippingServices.get(radioButton.getActionCommand());
                    deliveryCharge = selectedShippingService.price;
                    updateSummaryTotals();
                }
            }
        });

        return panel;
    }


    private void updateSummaryTotals() {
        totalMRP = 0;
        discountOnMRP = 0;
        couponSavings = 0;
        applicableGST = 0;

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
    }

    private String calculateDeliveryDates(String context) {
        // Current time is Wednesday, June 11, 2025 at 11:35:30 PM WIB.
        LocalDate orderDate = LocalDate.of(2025, 6, 11);

        LocalDate shippedDate = orderDate.plusDays(1);
        LocalDate minDeliveryDate = orderDate.plusDays(3);
        LocalDate maxDeliveryDate = orderDate.plusDays(4);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM");

        if (context.equals("header")) {
            return String.format("%s - %s", shippedDate.format(formatter), maxDeliveryDate.format(formatter));
        } else {
            return String.format("Estimated delivery by %s", maxDeliveryDate.format(formatter));
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
            JFrame frame = new JFrame("Address Selection");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1200, 800);

            ViewController dummyVC = new ViewController() {
                @Override public void showProductDetail(FavoritesUI.FavoriteItem product) { System.out.println("Dummy: Show Product Detail: " + product.getName()); }
                @Override public void showFavoritesView() { System.out.println("Dummy: Show Favorites View"); }
                @Override public void showDashboardView() { System.out.println("Dummy: Show Dashboard View"); }
                @Override public void showCartView() { System.out.println("Dummy: Show Cart View"); }
                @Override public void showProfileView() { System.out.println("Dummy: Show Profile View"); }
                @Override public void showOrdersView() { System.out.println("Dummy: Show Orders View"); }
                @Override public void showCheckoutView() { System.out.println("Dummy: Show Checkout View (Success)"); }
                @Override public void showAddressView() { System.out.println("Dummy: Show Address View (Success)"); }
                @Override public void showPaymentView() {
                    JOptionPane.showMessageDialog(null, "Simulasi Pindah ke PaymentUI", "Dummy Payment", JOptionPane.INFORMATION_MESSAGE);
                }
            };

            AddressUI addressUI = new AddressUI(dummyVC);
            frame.add(addressUI);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}