// AddressUI.java
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
import e.commerce.FavoritesUI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement; 
import java.util.stream.Collectors;


import e.commerce.ProductRepository;
import e.commerce.ProductRepository.CartItem;
import javax.swing.text.JTextComponent;


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
    private double deliveryCharge = 0; // Akan dihitung dinamis

    private JLabel currentAddressNameLabel;
    private JLabel currentAddressDetailLabel;
    private JLabel currentAddressMobileLabel;
    private JLabel deliveryEstimateHeaderLabel;

    private JPanel shippingServiceContainer;

    private ViewController viewController;
    private User currentUser;
    private List<ProductRepository.CartItem> cartItems;

    private List<Address> savedAddresses;
    private Address selectedAddress;      // Alamat yang dipilih dari database

    private Map<String, ShippingService> shippingServices; // Key: nama jasa pengiriman
    private ShippingService selectedShippingService; // Ini akan menyimpan ShippingService yang terhitung


    private static final Color ORANGE_THEME = new Color(255, 69, 0);
    private static final Color LIGHT_GRAY_BACKGROUND = new Color(245, 245, 245);
    private static final Color BORDER_COLOR = new Color(230, 230, 230);
    private static final Color DARK_TEXT_COLOR = new Color(50, 50, 50);
    private static final Color GRAY_TEXT_COLOR = new Color(120, 120, 120);
    private static final Color GREEN_DISCOUNT_TEXT = new Color(76, 175, 80);
    private static final Color TEAL_FREE_TEXT = new Color(0, 150, 136);

    public static class Address {
        int id; 
        int userId; 
        String label; 
        String fullAddress; // Ini akan menjadi "Nama Jalan"
        String city; 
        String province; 
        String postalCode; 
        String country; // Akan dihilangkan dari form, tapi tetap di model jika perlu

        // Tambahan kolom untuk Kecamatan dan Kelurahan
        String kecamatan;
        String kelurahan;

    
        public Address(int id, int userId, String label, String fullAddress, String city, String province, String postalCode, String country, String kecamatan, String kelurahan) {
            this.id = id;
            this.userId = userId;
            this.label = label;
            this.fullAddress = fullAddress;
            this.city = city;
            this.province = province;
            this.postalCode = postalCode;
            this.country = country;
            this.kecamatan = kecamatan;
            this.kelurahan = kelurahan;
        }

        // Getters and Setters for new fields
        public String getKecamatan() { return kecamatan; }
        public void setKecamatan(String kecamatan) { this.kecamatan = kecamatan; }
        public String getKelurahan() { return kelurahan; }
        public void setKelurahan(String kelurahan) { this.kelurahan = kelurahan; }

        public int getId() { return id; }
        public int getUserId() { return userId; }
        public String getLabel() { return label; }
        public String getFullAddress() { return fullAddress; }
        public String getCity() { return city; }
        public String getProvince() { return province; }
        public String getPostalCode() { return postalCode; }
        public String getCountry() { return country; }

        public void setLabel(String label) { this.label = label; }
        public void setFullAddress(String fullAddress) { this.fullAddress = fullAddress; }
        public void setCity(String city) { this.city = city; }
        public void setProvince(String province) { this.province = province; }
        public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
        public void setCountry(String country) { this.country = country; }

        public String getFormattedAddressDetailsForDisplay(User user) {
            String recipientName = user.getUsername(); 
            String mobileNumber = user.getPhone() != null ? user.getPhone() : "N/A"; 

            // Format alamat untuk ditampilkan, sertakan kecamatan dan kelurahan
            return recipientName + "<br>" +
                   fullAddress + "<br>" + // Nama Jalan
                   kelurahan + ", " + kecamatan + "<br>" + // Kelurahan, Kecamatan
                   city + ", " + province + " - " + postalCode + "<br>" +
                   "Mobile: " + mobileNumber; // Country removed from display as it's fixed to Indonesia
        }
    }
 
    public static class ShippingService {
        String providerName; // Nama provider (JNE, TIKI, dll.)
        String arrivalEstimate; // Contoh: "24 - 25 Jul"
        double price;
        String zoneName;
        double baseCost;
        double costPerKg;
        double chargeableWeight;

        // Constructor baru yang menerima ShippingCostResult
        public ShippingService(ShippingCostResult costResult) {
            this.providerName = costResult.getProviderName();
            this.arrivalEstimate = costResult.getArrivalEstimate();
            this.price = costResult.getCost();
            this.zoneName = costResult.getZoneName();
            this.baseCost = costResult.getBaseCost();
            this.costPerKg = costResult.getCostPerKg();
            this.chargeableWeight = costResult.getChargeableWeight();
        }

        // Getters
        public String getProviderName() { return providerName; }
        public String getArrivalEstimate() { return arrivalEstimate; }
        public double getPrice() { return price; }
        public String getZoneName() { return zoneName; }
        public double getBaseCost() { return baseCost; }
        public double getCostPerKg() { return costPerKg; }
        public double getChargeableWeight() { return chargeableWeight; }
    }

    public AddressUI(ViewController viewController) {
        this.viewController = viewController;
        setLayout(new BorderLayout());
        setBackground(LIGHT_GRAY_BACKGROUND);

        this.currentUser = Authentication.getCurrentUser();
        if (this.currentUser == null) {
            System.err.println("AddressUI: Tidak ada user yang login. Halaman tidak dapat dimuat.");
            JOptionPane.showMessageDialog(this, "Anda harus login untuk melihat halaman ini.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        loadCartItemsFromDatabase();
        initAddressesFromDatabase();
        createComponents();
        updateSummaryTotals();
        
        calculateAndPopulateShippingServices(); 
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

    private void initAddressesFromDatabase() {
        savedAddresses = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            String query = "SELECT id, user_id, label, full_address, city, province, postal_code, country, kecamatan, kelurahan " + // Added kecamatan, kelurahan
                           "FROM addresses WHERE user_id = ? ORDER BY id ASC";
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, currentUser.getId());
            rs = stmt.executeQuery();

            while (rs.next()) {
                Address addr = new Address(
                    rs.getInt("id"),
                    rs.getInt("user_id"),
                    rs.getString("label"),
                    rs.getString("full_address"),
                    rs.getString("city"),
                    rs.getString("province"),
                    rs.getString("postal_code"),
                    rs.getString("country"),
                    rs.getString("kecamatan"), // Added
                    rs.getString("kelurahan")  // Added
                );
                savedAddresses.add(addr);
            }

            if (!savedAddresses.isEmpty()) {
                selectedAddress = savedAddresses.get(0);
            } else {
                selectedAddress = null; // Tidak ada alamat
            }
        } catch (SQLException e) {
            System.err.println("Error loading addresses from database: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal memuat alamat dari database: " + e.getMessage(), "Error Database", JOptionPane.ERROR_MESSAGE);
            selectedAddress = null;
        } finally {
            DatabaseConnection.closeConnection(conn, stmt, rs);
        }
    }

    private void calculateAndPopulateShippingServices() {
        shippingServices = new LinkedHashMap<>(); // Gunakan LinkedHashMap untuk menjaga urutan
        
        if (selectedAddress == null || selectedAddress.getPostalCode() == null || selectedAddress.getPostalCode().isEmpty()) {
            System.out.println("Tidak ada alamat yang dipilih atau kodepos kosong, tidak dapat menghitung ongkir.");
            selectedShippingService = null;
            deliveryCharge = 0;
            deliveryEstimateHeaderLabel.setText("Pilih alamat untuk estimasi pengiriman."); // Translated
            populateShippingServicesUI();
            updateSummaryTotals();
            return;
        }

        double totalWeight = ShippingCalculator.calculateTotalWeight(cartItems);
        System.out.println("Total berat barang di keranjang: " + totalWeight + " kg");

        List<ShippingCostResult> results = ShippingCalculator.calculateShippingCosts(totalWeight, selectedAddress.getPostalCode());
        
        if (results.isEmpty() || results.get(0).hasError()) {
            String errorMessage = results.isEmpty() ? "Tidak ada jasa pengiriman tersedia." : results.get(0).getErrorMessage();
            System.err.println("Error calculating shipping costs: " + errorMessage);
            JOptionPane.showMessageDialog(this, "Gagal menghitung ongkir: " + errorMessage, "Error Ongkir", JOptionPane.ERROR_MESSAGE);
            selectedShippingService = null;
            deliveryCharge = 0;
            deliveryEstimateHeaderLabel.setText("Tidak ada estimasi pengiriman."); // Translated
        } else {
            // Populate all available shipping services
            for (ShippingCostResult result : results) {
                ShippingService service = new ShippingService(result);
                shippingServices.put(service.getProviderName(), service);
            }
            
            // Set default selected service (e.g., the first one, or "JNE Reguler" if available)
            if (shippingServices.containsKey("JNE Reguler")) {
                selectedShippingService = shippingServices.get("JNE Reguler");
            } else if (!shippingServices.isEmpty()) {
                selectedShippingService = shippingServices.values().iterator().next();
            } else {
                selectedShippingService = null;
            }

            if (selectedShippingService != null) {
                deliveryCharge = selectedShippingService.getPrice();
                deliveryEstimateHeaderLabel.setText("Perkiraan Sampai: " + selectedShippingService.getArrivalEstimate()); // Update with selected service
            } else {
                deliveryCharge = 0;
                deliveryEstimateHeaderLabel.setText("Tidak ada jasa pengiriman tersedia."); // Translated
            }
        }
        
        populateShippingServicesUI();
        updateSummaryTotals();
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
                viewController.showCartView();
            }
        });
        leftSection.add(backButton);
        headerPanel.add(leftSection, BorderLayout.WEST);

        JPanel navStepsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        navStepsPanel.setBackground(Color.WHITE);
      
        String[] steps = {"Keranjang", "Alamat", "Pembayaran", "Selesai"}; // Translated steps
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

            if (steps[i].equals("Alamat")) { // Check for "Alamat" // Translated
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

    private JPanel createDeliveryAddressSection() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(230, 230, 230), 1, true),
                new EmptyBorder(20, 20, 20, 20)
        ));

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(Color.WHITE);
        titlePanel.setBorder(new EmptyBorder(0, 0, 15, 0));

        JLabel titleLabel = new JLabel("Alamat Pengiriman"); // Translated
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setForeground(new Color(51, 51, 51));
        titlePanel.add(titleLabel, BorderLayout.WEST);

        JButton changeButton = new JButton("GANTI ALAMAT"); // Translated
        changeButton.setFont(new Font("Arial", Font.PLAIN, 11));
        changeButton.setForeground(ORANGE_THEME);
        changeButton.setBackground(Color.WHITE);
        changeButton.setBorderPainted(false);
        changeButton.setFocusPainted(false);
        changeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        changeButton.addActionListener(e -> showAddressSelectionDialog());
        titlePanel.add(changeButton, BorderLayout.EAST);

        panel.add(titlePanel, BorderLayout.NORTH);

        JPanel addressDetailsContainer = new JPanel();
        addressDetailsContainer.setLayout(new BoxLayout(addressDetailsContainer, BoxLayout.Y_AXIS));
        addressDetailsContainer.setBackground(Color.WHITE);
        addressDetailsContainer.setBorder(new EmptyBorder(0, 0, 15, 0));

        currentAddressNameLabel = new JLabel();
        currentAddressNameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        currentAddressNameLabel.setForeground(new Color(51, 51, 51));
        currentAddressNameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        currentAddressNameLabel.setBorder(new EmptyBorder(0, 0, 8, 0));
        addressDetailsContainer.add(currentAddressNameLabel);

        JLabel addressLabel = new JLabel("Alamat"); // Translated
        addressLabel.setFont(new Font("Arial", Font.BOLD, 12));
        addressLabel.setForeground(new Color(102, 102, 102));
        addressLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        addressLabel.setBorder(new EmptyBorder(0, 0, 5, 0));
        addressDetailsContainer.add(addressLabel);

        currentAddressDetailLabel = new JLabel();
        currentAddressDetailLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        currentAddressDetailLabel.setForeground(new Color(51, 51, 51));
        currentAddressDetailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        currentAddressDetailLabel.setBorder(new EmptyBorder(0, 0, 8, 0));
        addressDetailsContainer.add(currentAddressDetailLabel);

        currentAddressMobileLabel = new JLabel();
        currentAddressMobileLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        currentAddressMobileLabel.setForeground(new Color(51, 51, 51));
        currentAddressMobileLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        currentAddressMobileLabel.setBorder(new EmptyBorder(0, 0, 15, 0));
        addressDetailsContainer.add(currentAddressMobileLabel);

        panel.add(addressDetailsContainer, BorderLayout.CENTER);

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBackground(Color.WHITE);
        statusPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

        JPanel codPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        codPanel.setBackground(Color.WHITE);

        JLabel codDot = new JLabel("●");
        codDot.setFont(new Font("Arial", Font.BOLD, 12));
        codDot.setForeground(new Color(46, 204, 113));
        codPanel.add(codDot);

        JLabel codLabel = new JLabel("Tersedia bayar di tempat"); // Translated
        codLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        codLabel.setForeground(new Color(51, 51, 51));
        codPanel.add(codLabel);

        statusPanel.add(codPanel, BorderLayout.WEST);

        deliveryEstimateHeaderLabel = new JLabel("Perkiraan Sampai: Memuat..."); // Translated initial text
        deliveryEstimateHeaderLabel.setFont(new Font("Arial", Font.BOLD, 12));
        deliveryEstimateHeaderLabel.setForeground(new Color(51, 51, 51));
        statusPanel.add(deliveryEstimateHeaderLabel, BorderLayout.EAST);

        panel.add(statusPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void updateDisplayedAddress() {
        if (selectedAddress != null && currentUser != null) {
            currentAddressNameLabel.setText(currentUser.getUsername());
            currentAddressMobileLabel.setText("Mobile: " + (currentUser.getPhone() != null ? currentUser.getPhone() : "N/A"));
            currentAddressDetailLabel.setText("<html>" + selectedAddress.getFormattedAddressDetailsForDisplay(currentUser) + "</html>");
            calculateAndPopulateShippingServices(); // Recalculate shipping when address changes
        } else {
            currentAddressNameLabel.setText("Tidak ada alamat ditemukan"); // Translated
            currentAddressDetailLabel.setText("<html><i>Silakan tambahkan alamat di pengaturan profil Anda.</i></html>"); // Translated
            currentAddressMobileLabel.setText("");
            deliveryEstimateHeaderLabel.setText("Tidak ada estimasi pengiriman."); // Translated
            shippingServices.clear();
            selectedShippingService = null;
            deliveryCharge = 0;
            populateShippingServicesUI();
            updateSummaryTotals();
        }
    }

    private void showAddressSelectionDialog() {
        final JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                                    "Pilih Alamat Pengiriman", Dialog.ModalityType.APPLICATION_MODAL); // Translated title
        dialog.setLayout(new BorderLayout());
        dialog.setSize(500, 450);
        dialog.setLocationRelativeTo(this);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel headerLabel = new JLabel("Pilih Alamat Pengiriman"); // Translated
        headerLabel.setFont(new Font("Arial", Font.BOLD, 16));
        headerLabel.setForeground(new Color(51, 51, 51));
        headerPanel.add(headerLabel, BorderLayout.WEST);

        dialog.add(headerPanel, BorderLayout.NORTH);

        JPanel addressListPanel = new JPanel();
        addressListPanel.setLayout(new BoxLayout(addressListPanel, BoxLayout.Y_AXIS));
        addressListPanel.setBorder(new EmptyBorder(10, 20, 10, 20));
        addressListPanel.setBackground(new Color(248, 248, 248));

        ButtonGroup addressButtonGroup = new ButtonGroup();

        List<Address> addressesForDialog = getAllAddressesForUser(currentUser.getId());

        if (addressesForDialog.isEmpty()) {
            JLabel noAddressesLabel = new JLabel("Tidak ada alamat yang ditemukan. Silakan tambahkan di pengaturan profil."); // Translated
            noAddressesLabel.setFont(new Font("Arial", Font.ITALIC, 13));
            noAddressesLabel.setForeground(GRAY_TEXT_COLOR);
            noAddressesLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            addressListPanel.add(Box.createVerticalGlue());
            addressListPanel.add(noAddressesLabel);
            addressListPanel.add(Box.createVerticalGlue());
        } else {
            for (Address addr : addressesForDialog) {
                JPanel addressCard = createAddressCardForSelection(addr, addressButtonGroup, dialog);
                addressListPanel.add(addressCard);
                addressListPanel.add(Box.createVerticalStrut(10));
            }
        }

        JScrollPane scrollPane = new JScrollPane(addressListPanel);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(new Color(248, 248, 248));
        dialog.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

        JButton addAddressButton = new JButton("Tambah Alamat Baru"); // Translated
        addAddressButton.setFont(new Font("Arial", Font.BOLD, 12));
        addAddressButton.setBackground(new Color(70, 130, 180));
        addAddressButton.setForeground(Color.WHITE);
        addAddressButton.setBorder(new EmptyBorder(8, 15, 8, 15));
        addAddressButton.addActionListener(e -> {
            showAddEditAddressDialog(null);
            dialog.dispose();
        });
        buttonPanel.add(addAddressButton);


        JButton selectButton = new JButton("Pilih Alamat"); // Translated
        selectButton.setFont(new Font("Arial", Font.BOLD, 12));
        selectButton.setBackground(ORANGE_THEME);
        selectButton.setForeground(Color.WHITE);
        selectButton.setBorder(new EmptyBorder(8, 15, 8, 15));
        selectButton.addActionListener(e -> {
            String selectedLabel = addressButtonGroup.getSelection() != null ? addressButtonGroup.getSelection().getActionCommand() : null;
            if (selectedLabel != null) {
                selectedAddress = addressesForDialog.stream()
                                                    .filter(a -> a.getLabel().equals(selectedLabel))
                                                    .findFirst()
                                                    .orElse(null);
                updateDisplayedAddress();
                dialog.dispose();
            } else {
                JOptionPane.showMessageDialog(dialog, "Silakan pilih alamat.", "Tidak Ada Pilihan", JOptionPane.WARNING_MESSAGE); // Translated
            }
        });
        buttonPanel.add(selectButton);

        JButton closeButton = new JButton("Tutup"); // Translated
        closeButton.setFont(new Font("Arial", Font.PLAIN, 12));
        closeButton.setBackground(new Color(240, 240, 240));
        closeButton.setBorder(new EmptyBorder(8, 15, 8, 15));
        closeButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(closeButton);

        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private JPanel createAddressCardForSelection(Address addr, ButtonGroup buttonGroup, JDialog parentDialog) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(230, 230, 230), 1, true),
            new EmptyBorder(15, 15, 15, 15)
        ));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);

        JRadioButton radioButton = new JRadioButton();
        radioButton.setBackground(Color.WHITE);
        radioButton.setFocusPainted(false);
        radioButton.setActionCommand(addr.getLabel());
        headerPanel.add(radioButton, BorderLayout.WEST);

        JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        labelPanel.setBackground(Color.WHITE);

        JLabel labelTag = new JLabel(addr.getLabel());
        labelTag.setFont(new Font("Arial", Font.BOLD, 14));
        labelTag.setForeground(new Color(51, 51, 51));
        labelPanel.add(labelTag);

        if (selectedAddress != null && selectedAddress.getId() == addr.getId()) {
            radioButton.setSelected(true);
        }

        headerPanel.add(labelPanel, BorderLayout.CENTER);
        card.add(headerPanel, BorderLayout.NORTH);

        JLabel addressDetailLabel = new JLabel("<html>" + addr.getFormattedAddressDetailsForDisplay(currentUser) + "</html>");
        addressDetailLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        addressDetailLabel.setForeground(new Color(102, 102, 102));
        addressDetailLabel.setBorder(new EmptyBorder(8, 25, 0, 0));
        card.add(addressDetailLabel, BorderLayout.CENTER);

        buttonGroup.add(radioButton);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        actionPanel.setBackground(Color.WHITE);

        JButton editButton = new JButton("Edit"); // Translated
        editButton.setFont(new Font("Arial", Font.PLAIN, 10));
        editButton.setForeground(new Color(70, 130, 180));
        editButton.setBackground(Color.WHITE);
        editButton.setBorderPainted(false);
        editButton.setFocusPainted(false);
        editButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        editButton.addActionListener(e -> {
            showAddEditAddressDialog(addr);
            parentDialog.dispose();
        });
        actionPanel.add(editButton);

        JButton deleteButton = new JButton("Hapus"); // Translated
        deleteButton.setFont(new Font("Arial", Font.PLAIN, 10));
        deleteButton.setForeground(new Color(220, 20, 60));
        deleteButton.setBackground(Color.WHITE);
        deleteButton.setBorderPainted(false);
        deleteButton.setFocusPainted(false);
        deleteButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        deleteButton.addActionListener(e -> {
            deleteAddress(addr.getId());
            parentDialog.dispose();
        });
        actionPanel.add(deleteButton);

        card.add(actionPanel, BorderLayout.SOUTH);

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getSource() == card) {
                    radioButton.setSelected(true);
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
        panel.setMinimumSize(new Dimension(300, 280));
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

        summaryDetails.add(createSummaryRowPanel("Total MRP", totalMRPLabel, DARK_TEXT_COLOR, totalMRP));
        summaryDetails.add(Box.createVerticalStrut(5));

        summaryDetails.add(createSummaryRowPanel("Diskon dari MRP", discountOnMRPLabel, GREEN_DISCOUNT_TEXT, discountOnMRP)); // Translated
        summaryDetails.add(Box.createVerticalStrut(5));

        summaryDetails.add(createSummaryRowPanel("Penghematan Kupon", couponSavingsLabel, GREEN_DISCOUNT_TEXT, couponSavings)); // Translated
        summaryDetails.add(Box.createVerticalStrut(5));

        summaryDetails.add(createSummaryRowPanel("PPN yang Berlaku", applicableGSTLabel, DARK_TEXT_COLOR, applicableGST)); // Translated
        summaryDetails.add(Box.createVerticalStrut(5));

        summaryDetails.add(createSummaryRowPanel("Pengiriman", deliveryLabel, TEAL_FREE_TEXT, deliveryCharge)); // Translated
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

        JButton continueButton = new JButton("LANJUTKAN"); // Translated
        continueButton.setBackground(ORANGE_THEME);
        continueButton.setForeground(Color.WHITE);
        continueButton.setBorderPainted(false);
        continueButton.setFocusPainted(false);
        continueButton.setFont(new Font("Arial", Font.BOLD, 14));
        continueButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        continueButton.setPreferredSize(new Dimension(200, 40));
        continueButton.addActionListener(e -> {
            if (selectedAddress == null) {
                JOptionPane.showMessageDialog(this, "Silakan pilih alamat pengiriman.", "Peringatan", JOptionPane.WARNING_MESSAGE); // Translated
                return;
            }
            if (selectedShippingService == null) {
                JOptionPane.showMessageDialog(this, "Silakan pilih jasa pengiriman.", "Peringatan", JOptionPane.WARNING_MESSAGE); // Translated
                return;
            }

            if (viewController != null) {
                updateSummaryTotals();
                double finalTotal = totalMRP - discountOnMRP - couponSavings + applicableGST + deliveryCharge;

                viewController.showPaymentView(selectedAddress, selectedShippingService, finalTotal); 
            } else {
                JOptionPane.showMessageDialog(this, "Lanjut ke halaman Pembayaran.", "Pembayaran", JOptionPane.INFORMATION_MESSAGE); // Translated
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

        if (labelText.equals("Pengiriman") && amount == 0) { // Changed for "Pengiriman"
            valueLabelRef.setText("Gratis"); // Translated
        } else {
            valueLabelRef.setText(customValueText != null ? customValueText : formatCurrency(amount));
        }
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
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(230, 230, 230), 1, true),
                new EmptyBorder(20, 20, 20, 20)
        ));

        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(new EmptyBorder(0, 0, 20, 0));

        JLabel titleLabel = new JLabel("Jasa Pengiriman"); // Translated
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setForeground(new Color(51, 51, 51));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerPanel.add(titleLabel);

        headerPanel.add(Box.createVerticalStrut(5));

        JLabel subtitleLabel = new JLabel("Pilih metode pengiriman yang Anda inginkan."); // Translated
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        subtitleLabel.setForeground(new Color(102, 102, 102));
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerPanel.add(subtitleLabel);

        panel.add(headerPanel, BorderLayout.NORTH);

        shippingServiceContainer = new JPanel();
        shippingServiceContainer.setLayout(new BoxLayout(shippingServiceContainer, BoxLayout.Y_AXIS));
        shippingServiceContainer.setBackground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(shippingServiceContainer);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void populateShippingServicesUI() {
        shippingServiceContainer.removeAll();
        ButtonGroup serviceButtonGroup = new ButtonGroup();

        if (shippingServices.isEmpty()) {
            JLabel noServiceLabel = new JLabel("Tidak ada jasa pengiriman tersedia untuk alamat ini."); // Translated
            noServiceLabel.setFont(new Font("Arial", Font.ITALIC, 13));
            noServiceLabel.setForeground(GRAY_TEXT_COLOR);
            noServiceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            shippingServiceContainer.add(Box.createVerticalGlue());
            shippingServiceContainer.add(noServiceLabel);
            shippingServiceContainer.add(Box.createVerticalGlue());
            deliveryEstimateHeaderLabel.setText("Tidak ada estimasi pengiriman."); // Translated
        } else {
            // Urutkan layanan pengiriman berdasarkan harga (termurah di atas), dan prioritaskan express di paling atas
            List<ShippingService> sortedServices = shippingServices.values().stream()
                .sorted((s1, s2) -> {
                    // Prioritaskan Express di paling atas
                    boolean s1IsExpress = s1.getProviderName().toLowerCase().contains("express") || s1.getProviderName().toLowerCase().contains("yes") || s1.getProviderName().toLowerCase().contains("ons");
                    boolean s2IsExpress = s2.getProviderName().toLowerCase().contains("express") || s2.getProviderName().toLowerCase().contains("yes") || s2.getProviderName().toLowerCase().contains("ons");

                    if (s1IsExpress && !s2IsExpress) return -1;
                    if (!s1IsExpress && s2IsExpress) return 1;
                    
                    // Kemudian urutkan berdasarkan harga
                    return Double.compare(s1.getPrice(), s2.getPrice());
                })
                .collect(Collectors.toList());

            boolean isFirstSelected = true;
            for (ShippingService service : sortedServices) {
                JPanel servicePanel = createShippingServicePanel(service, serviceButtonGroup);

                if (shippingServiceContainer.getComponentCount() > 0) {
                    shippingServiceContainer.add(Box.createVerticalStrut(8));
                }
                shippingServiceContainer.add(servicePanel);

                // Set the first service (or previously selected if matches) as selected
                if (isFirstSelected || (selectedShippingService != null && selectedShippingService.getProviderName().equals(service.getProviderName()))) {
                    ((JRadioButton)((JPanel)((JPanel)servicePanel.getComponent(0)).getComponent(0)).getComponent(0)).setSelected(true);
                    selectedShippingService = service; // Ensure selectedShippingService is set
                    deliveryCharge = selectedShippingService.getPrice(); // Update deliveryCharge
                    deliveryEstimateHeaderLabel.setText("Perkiraan Sampai: " + selectedShippingService.getArrivalEstimate()); // Update header
                    isFirstSelected = false; // Only mark the very first one as default selected
                }
            }
        }

        shippingServiceContainer.revalidate();
        shippingServiceContainer.repaint();
        updateSummaryTotals(); // Ensure totals are updated after selection
    }

    private JPanel createShippingServicePanel(ShippingService service, ButtonGroup buttonGroup) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(220, 220, 220), 1, true),
                new EmptyBorder(15, 15, 15, 15)
        ));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        panel.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JPanel leftSection = new JPanel(new BorderLayout());
        leftSection.setBackground(Color.WHITE);

        JRadioButton radioButton = new JRadioButton();
        radioButton.setBackground(Color.WHITE);
        radioButton.setFocusPainted(false);
        radioButton.setActionCommand(service.getProviderName()); // Use provider name as action command
        radioButton.addActionListener(e -> {
            selectedShippingService = shippingServices.get(e.getActionCommand());
            if (selectedShippingService != null) {
                deliveryCharge = selectedShippingService.getPrice();
                deliveryEstimateHeaderLabel.setText("Perkiraan Sampai: " + selectedShippingService.getArrivalEstimate()); // Update header
            } else {
                deliveryCharge = 0;
                deliveryEstimateHeaderLabel.setText("Tidak ada estimasi pengiriman."); // Fallback
            }
            updateSummaryTotals();
        });
        buttonGroup.add(radioButton);

        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        radioPanel.setBackground(Color.WHITE);
        radioPanel.add(radioButton);
        leftSection.add(radioPanel, BorderLayout.WEST);

        JLabel serviceNameLabel = new JLabel(service.getProviderName()); // Display provider name
        serviceNameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        serviceNameLabel.setForeground(new Color(51, 51, 51));
        serviceNameLabel.setBorder(new EmptyBorder(0, 15, 0, 0));
        leftSection.add(serviceNameLabel, BorderLayout.CENTER);

        panel.add(leftSection, BorderLayout.WEST);

        JPanel rightSection = new JPanel();
        rightSection.setLayout(new BoxLayout(rightSection, BoxLayout.Y_AXIS));
        rightSection.setBackground(Color.WHITE);

        JLabel priceLabel = new JLabel(formatCurrency(service.getPrice())); // Use getPrice()
        priceLabel.setFont(new Font("Arial", Font.BOLD, 14));
        priceLabel.setForeground(new Color(51, 51, 51));
        priceLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        rightSection.add(priceLabel);

        rightSection.add(Box.createVerticalStrut(3));

        JLabel estimateLabel = new JLabel(service.getArrivalEstimate()); // Use getArrivalEstimate()
        estimateLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        estimateLabel.setForeground(new Color(102, 102, 102));
        estimateLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        rightSection.add(estimateLabel);

        panel.add(rightSection, BorderLayout.EAST);

        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                panel.setBackground(new Color(248, 249, 250));
                panel.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(new Color(200, 200, 200), 1, true),
                    new EmptyBorder(15, 15, 15, 15)
                ));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                panel.setBackground(Color.WHITE);
                panel.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(new Color(220, 220, 220), 1, true),
                    new EmptyBorder(15, 15, 15, 15)
                ));
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getSource() != radioButton) {
                    radioButton.setSelected(true);
                    selectedShippingService = shippingServices.get(radioButton.getActionCommand());
                    if (selectedShippingService != null) {
                        deliveryCharge = selectedShippingService.getPrice();
                        deliveryEstimateHeaderLabel.setText("Perkiraan Sampai: " + selectedShippingService.getArrivalEstimate());
                    } else {
                        deliveryCharge = 0;
                        deliveryEstimateHeaderLabel.setText("Tidak ada estimasi pengiriman.");
                    }
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

        if (deliveryLabel != null) {
            if (deliveryCharge == 0) {
                deliveryLabel.setText("Gratis");
                deliveryLabel.setForeground(TEAL_FREE_TEXT);
            } else {
                deliveryLabel.setText(formatCurrency(deliveryCharge));
                deliveryLabel.setForeground(DARK_TEXT_COLOR);
            }
        }
        
        if (finalTotalLabel != null) finalTotalLabel.setText(formatCurrency(finalTotal));
    }

    // This method is now removed as delivery dates come from ShippingCostResult
    /*
    private String calculateDeliveryDates(String zone) {
        // ... (removed)
    }
    */

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

 
    private Address getPrimaryDeliveryAddress(int userId) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Address primaryAddress = null;

        try {
            conn = DatabaseConnection.getConnection();
            String query = "SELECT id, user_id, label, full_address, city, province, postal_code, country, kecamatan, kelurahan " + // Added kecamatan, kelurahan
                           "FROM addresses WHERE user_id = ? ORDER BY id ASC LIMIT 1";
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            rs = stmt.executeQuery();

            if (rs.next()) {
                primaryAddress = new Address(
                    rs.getInt("id"),
                    rs.getInt("user_id"),
                    rs.getString("label"),
                    rs.getString("full_address"),
                    rs.getString("city"),
                    rs.getString("province"),
                    rs.getString("postal_code"),
                    rs.getString("country"),
                    rs.getString("kecamatan"), // Added
                    rs.getString("kelurahan")  // Added
                );
            }
        } catch (SQLException e) {
            System.err.println("Error loading primary delivery address from DB: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error memuat alamat pengiriman utama: " + e.getMessage(), "Error Database", JOptionPane.ERROR_MESSAGE);
        } finally {
            DatabaseConnection.closeConnection(conn, stmt, rs);
        }
        return primaryAddress;
    }

    private List<Address> getAllAddressesForUser(int userId) {
        List<Address> addresses = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            String query = "SELECT id, user_id, label, full_address, city, province, postal_code, country, kecamatan, kelurahan FROM addresses WHERE user_id = ?"; // Added kecamatan, kelurahan
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            rs = stmt.executeQuery();
            while (rs.next()) {
                addresses.add(new Address(
                    rs.getInt("id"),
                    rs.getInt("user_id"),
                    rs.getString("label"),
                    rs.getString("full_address"),
                    rs.getString("city"),
                    rs.getString("province"),
                    rs.getString("postal_code"),
                    rs.getString("country"),
                    rs.getString("kecamatan"), // Added
                    rs.getString("kelurahan")  // Added
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error loading all addresses for dialog: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error memuat semua alamat untuk dialog: " + e.getMessage(), "Error Database", JOptionPane.ERROR_MESSAGE);
        } finally {
            DatabaseConnection.closeConnection(conn, stmt, rs);
        }
        return addresses;
    }

    private void showAddEditAddressDialog(Address addressToEdit) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                                     addressToEdit == null ? "Tambah Alamat Baru" : "Edit Alamat", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(450, 650); // Increased height to accommodate new fields
        dialog.setLocationRelativeTo(this);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        formPanel.setBackground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 5, 8, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Label Alamat
        JTextField labelField = new JTextField(20);
        setupPlaceholder(labelField, "Contoh: Rumah, Kantor");

        // Nama Jalan (previously Full Address)
        JTextArea streetAddressArea = new JTextArea(3, 20); // Reduced rows, as more specific fields are added
        streetAddressArea.setLineWrap(true);
        streetAddressArea.setWrapStyleWord(true);
        streetAddressArea.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        setupPlaceholder(streetAddressArea, "Nama jalan, nomor rumah/gedung, RT/RW, blok, dll."); // Translated placeholder
        JScrollPane streetAddressScrollPane = new JScrollPane(streetAddressArea);

        // Province Dropdown
        JComboBox<String> provinceComboBox = new JComboBox<>();
        provinceComboBox.addItem("Pilih Provinsi");
        getProvincesFromDB().forEach(provinceComboBox::addItem);

        // City Dropdown
        JComboBox<String> cityComboBox = new JComboBox<>();
        cityComboBox.addItem("Pilih Kota/Kabupaten");
        cityComboBox.setEnabled(false);

        // Kecamatan Dropdown (NEW)
        JComboBox<String> kecamatanComboBox = new JComboBox<>();
        kecamatanComboBox.addItem("Pilih Kecamatan");
        kecamatanComboBox.setEnabled(false);

        // Kelurahan Dropdown (NEW)
        JComboBox<String> kelurahanComboBox = new JComboBox<>();
        kelurahanComboBox.addItem("Pilih Kelurahan");
        kelurahanComboBox.setEnabled(false);

        // Postal Code Dropdown
        JComboBox<String> postalCodeComboBox = new JComboBox<>();
        postalCodeComboBox.addItem("Pilih Kode Pos");
        postalCodeComboBox.setEnabled(false);

        // Populate fields if editing an existing address
        if (addressToEdit != null) {
            labelField.setText(addressToEdit.getLabel());
            setupPlaceholder(labelField, "Contoh: Rumah, Kantor");

            streetAddressArea.setText(addressToEdit.getFullAddress()); // Using fullAddress for street name
            setupPlaceholder(streetAddressArea, "Nama jalan, nomor rumah/gedung, RT/RW, blok, dll.");

            if (addressToEdit.getProvince() != null) {
                provinceComboBox.setSelectedItem(addressToEdit.getProvince());
                List<String> cities = getCitiesFromDB(addressToEdit.getProvince());
                cityComboBox.removeAllItems();
                cityComboBox.addItem("Pilih Kota/Kabupaten");
                cities.forEach(cityComboBox::addItem);
                cityComboBox.setEnabled(true);

                if (addressToEdit.getCity() != null) {
                    cityComboBox.setSelectedItem(addressToEdit.getCity());
                    List<String> kecamatans = getKecamatansFromDB(addressToEdit.getProvince(), addressToEdit.getCity()); // NEW
                    kecamatanComboBox.removeAllItems();
                    kecamatanComboBox.addItem("Pilih Kecamatan");
                    kecamatans.forEach(kecamatanComboBox::addItem);
                    kecamatanComboBox.setEnabled(true);

                    if (addressToEdit.getKecamatan() != null) { // NEW
                        kecamatanComboBox.setSelectedItem(addressToEdit.getKecamatan());
                        List<String> kelurahans = getKelurahansFromDB(addressToEdit.getProvince(), addressToEdit.getCity(), addressToEdit.getKecamatan()); // NEW
                        kelurahanComboBox.removeAllItems();
                        kelurahanComboBox.addItem("Pilih Kelurahan");
                        kelurahans.forEach(kelurahanComboBox::addItem);
                        kelurahanComboBox.setEnabled(true);

                        if (addressToEdit.getKelurahan() != null) { // NEW
                            kelurahanComboBox.setSelectedItem(addressToEdit.getKelurahan());
                            List<String> postalCodes = getPostalCodesFromDB(addressToEdit.getProvince(), addressToEdit.getCity(), addressToEdit.getKecamatan(), addressToEdit.getKelurahan()); // Modified
                            postalCodeComboBox.removeAllItems();
                            postalCodeComboBox.addItem("Pilih Kode Pos");
                            postalCodes.forEach(postalCodeComboBox::addItem);
                            postalCodeComboBox.setEnabled(true);
                            if (addressToEdit.getPostalCode() != null) {
                                postalCodeComboBox.setSelectedItem(addressToEdit.getPostalCode());
                            }
                        }
                    }
                }
            }
        }

        // Add change listeners for dropdowns (cascading logic)
        provinceComboBox.addActionListener(e -> {
            String selectedProvince = (String) provinceComboBox.getSelectedItem();
            cityComboBox.removeAllItems();
            cityComboBox.addItem("Pilih Kota/Kabupaten");
            kecamatanComboBox.removeAllItems(); // Clear kecamatan
            kecamatanComboBox.addItem("Pilih Kecamatan");
            kelurahanComboBox.removeAllItems(); // Clear kelurahan
            kelurahanComboBox.addItem("Pilih Kelurahan");
            postalCodeComboBox.removeAllItems();
            postalCodeComboBox.addItem("Pilih Kode Pos");

            cityComboBox.setEnabled(false);
            kecamatanComboBox.setEnabled(false);
            kelurahanComboBox.setEnabled(false);
            postalCodeComboBox.setEnabled(false);

            if (selectedProvince != null && !selectedProvince.equals("Pilih Provinsi")) {
                getCitiesFromDB(selectedProvince).forEach(cityComboBox::addItem);
                cityComboBox.setEnabled(true);
            }
        });

        cityComboBox.addActionListener(e -> {
            String selectedProvince = (String) provinceComboBox.getSelectedItem();
            String selectedCity = (String) cityComboBox.getSelectedItem();
            kecamatanComboBox.removeAllItems(); // Clear kecamatan
            kecamatanComboBox.addItem("Pilih Kecamatan");
            kelurahanComboBox.removeAllItems(); // Clear kelurahan
            kelurahanComboBox.addItem("Pilih Kelurahan");
            postalCodeComboBox.removeAllItems();
            postalCodeComboBox.addItem("Pilih Kode Pos");

            kecamatanComboBox.setEnabled(false);
            kelurahanComboBox.setEnabled(false);
            postalCodeComboBox.setEnabled(false);

            if (selectedProvince != null && !selectedProvince.equals("Pilih Provinsi") &&
                selectedCity != null && !selectedCity.equals("Pilih Kota/Kabupaten")) {
                getKecamatansFromDB(selectedProvince, selectedCity).forEach(kecamatanComboBox::addItem); // NEW
                kecamatanComboBox.setEnabled(true);
            }
        });

        kecamatanComboBox.addActionListener(e -> { // NEW
            String selectedProvince = (String) provinceComboBox.getSelectedItem();
            String selectedCity = (String) cityComboBox.getSelectedItem();
            String selectedKecamatan = (String) kecamatanComboBox.getSelectedItem();
            kelurahanComboBox.removeAllItems();
            kelurahanComboBox.addItem("Pilih Kelurahan");
            postalCodeComboBox.removeAllItems();
            postalCodeComboBox.addItem("Pilih Kode Pos");

            kelurahanComboBox.setEnabled(false);
            postalCodeComboBox.setEnabled(false);

            if (selectedProvince != null && !selectedProvince.equals("Pilih Provinsi") &&
                selectedCity != null && !selectedCity.equals("Pilih Kota/Kabupaten") &&
                selectedKecamatan != null && !selectedKecamatan.equals("Pilih Kecamatan")) {
                getKelurahansFromDB(selectedProvince, selectedCity, selectedKecamatan).forEach(kelurahanComboBox::addItem); // NEW
                kelurahanComboBox.setEnabled(true);
            }
        });

        kelurahanComboBox.addActionListener(e -> { // NEW
            String selectedProvince = (String) provinceComboBox.getSelectedItem();
            String selectedCity = (String) cityComboBox.getSelectedItem();
            String selectedKecamatan = (String) kecamatanComboBox.getSelectedItem();
            String selectedKelurahan = (String) kelurahanComboBox.getSelectedItem();
            postalCodeComboBox.removeAllItems();
            postalCodeComboBox.addItem("Pilih Kode Pos");

            postalCodeComboBox.setEnabled(false);

            if (selectedProvince != null && !selectedProvince.equals("Pilih Provinsi") &&
                selectedCity != null && !selectedCity.equals("Pilih Kota/Kabupaten") &&
                selectedKecamatan != null && !selectedKecamatan.equals("Pilih Kecamatan") &&
                selectedKelurahan != null && !selectedKelurahan.equals("Pilih Kelurahan")) {
                getPostalCodesFromDB(selectedProvince, selectedCity, selectedKecamatan, selectedKelurahan).forEach(postalCodeComboBox::addItem); // Modified call
                postalCodeComboBox.setEnabled(true);
            }
        });

        // Add components to panel (using translated labels)
        int row = 0;
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        formPanel.add(new JLabel("Label Alamat (Contoh: Rumah, Kantor):"), gbc);
        row++;
        gbc.gridy = row;
        formPanel.add(labelField, gbc);
        row++;

        gbc.gridy = row;
        formPanel.add(new JLabel("Nama Jalan:"), gbc); // Changed label
        row++;
        gbc.gridy = row;
        formPanel.add(streetAddressScrollPane, gbc); // Changed variable name
        row++;

        gbc.gridy = row;
        formPanel.add(new JLabel("Provinsi:"), gbc);
        row++;
        gbc.gridy = row;
        formPanel.add(provinceComboBox, gbc);
        row++;

        gbc.gridy = row;
        formPanel.add(new JLabel("Kota/Kabupaten:"), gbc);
        row++;
        gbc.gridy = row;
        formPanel.add(cityComboBox, gbc);
        row++;

        gbc.gridy = row;
        formPanel.add(new JLabel("Kecamatan:"), gbc); // NEW
        row++;
        gbc.gridy = row;
        formPanel.add(kecamatanComboBox, gbc); // NEW
        row++;

        gbc.gridy = row;
        formPanel.add(new JLabel("Kelurahan:"), gbc); // NEW
        row++;
        gbc.gridy = row;
        formPanel.add(kelurahanComboBox, gbc); // NEW
        row++;

        gbc.gridy = row;
        formPanel.add(new JLabel("Kode Pos:"), gbc);
        row++;
        gbc.gridy = row;
        formPanel.add(postalCodeComboBox, gbc);
        row++;

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

        JButton saveButton = new JButton(addressToEdit == null ? "Tambah Alamat" : "Simpan Perubahan");
        saveButton.setBackground(ORANGE_THEME);
        saveButton.setForeground(Color.WHITE);
        saveButton.setFont(new Font("Arial", Font.BOLD, 12));
        saveButton.setBorder(new EmptyBorder(8, 15, 8, 15));
        saveButton.addActionListener(e -> {
            String label = getTextFieldValue(labelField);
            String fullAddress = getTextAreaValue(streetAddressArea); // Get value from new text area
            String province = (String) provinceComboBox.getSelectedItem();
            String city = (String) cityComboBox.getSelectedItem();
            String kecamatan = (String) kecamatanComboBox.getSelectedItem(); // NEW
            String kelurahan = (String) kelurahanComboBox.getSelectedItem();   // NEW
            String postalCode = (String) postalCodeComboBox.getSelectedItem();
            String country = "Indonesia"; // Hardcoded country as per requirement

            if (label.isEmpty() || fullAddress.isEmpty() || province == null || province.equals("Pilih Provinsi") ||
                city == null || city.equals("Pilih Kota/Kabupaten") || kecamatan == null || kecamatan.equals("Pilih Kecamatan") || // NEW Validation
                kelurahan == null || kelurahan.equals("Pilih Kelurahan") || // NEW Validation
                postalCode == null || postalCode.equals("Pilih Kode Pos")) {
                JOptionPane.showMessageDialog(dialog, "Semua kolom harus diisi!", "Validasi Input", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (addressToEdit == null) {
                if ("supervisor".equalsIgnoreCase(currentUser.getRole())) {
                    if (getAllAddressesForUser(currentUser.getId()).size() >= 1) {
                        JOptionPane.showMessageDialog(dialog, "Pengguna dengan role 'supervisor' hanya dapat memiliki satu alamat.", "Peringatan", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                }
                // Modified Address constructor call
                Address newAddress = new Address(0, currentUser.getId(), label, fullAddress, city, province, postalCode, country, kecamatan, kelurahan);
                addAddressToDatabase(newAddress);
            } else {
                // Modified Address setter calls
                addressToEdit.setLabel(label);
                addressToEdit.setFullAddress(fullAddress);
                addressToEdit.setProvince(province);
                addressToEdit.setCity(city);
                addressToEdit.setKecamatan(kecamatan); // NEW
                addressToEdit.setKelurahan(kelurahan);   // NEW
                addressToEdit.setPostalCode(postalCode);
                addressToEdit.setCountry(country); // Still set, but hardcoded
                updateAddressInDatabase(addressToEdit);
            }
            dialog.dispose();
        });
        buttonPanel.add(saveButton);

        JButton cancelButton = new JButton("Batal");
        cancelButton.setBackground(new Color(240, 240, 240));
        cancelButton.setFont(new Font("Arial", Font.PLAIN, 12));
        cancelButton.setBorder(new EmptyBorder(8, 15, 8, 15));
        cancelButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(cancelButton);

        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }
    
    private List<String> getProvincesFromDB() {
        List<String> provinces = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            if (conn == null) {
                System.err.println("ERROR: Koneksi database adalah NULL di getProvincesFromDB.");
                return provinces; // Return empty list if connection is null
            }
            String query = "SELECT DISTINCT provinsi FROM tbl_kodepos ORDER BY provinsi";
            System.out.println("DEBUG: Executing query: " + query); // Log query
            pstmt = conn.prepareStatement(query);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                provinces.add(rs.getString("provinsi"));
            }
            System.out.println("DEBUG: Provinces loaded: " + provinces.size()); // Log count
        } catch (SQLException e) {
            System.err.println("ERROR in getProvincesFromDB: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error memuat provinsi dari kodepos: " + e.getMessage(), "Error Database", JOptionPane.ERROR_MESSAGE);
        } finally {
            DatabaseConnection.closeConnection(conn, pstmt, rs);
        }
        return provinces;
    }

    private List<String> getCitiesFromDB(String provinceName) {
        List<String> cities = new ArrayList<>();
        if (provinceName == null || provinceName.isEmpty() || provinceName.equals("Pilih Provinsi")) {
            System.out.println("DEBUG: Province name is null, empty, or placeholder in getCitiesFromDB. Returning empty list.");
            return cities;
        }

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            if (conn == null) {
                System.err.println("ERROR: Koneksi database adalah NULL di getCitiesFromDB.");
                return cities; // Return empty list if connection is null
            }
            String query = "SELECT DISTINCT kabupaten FROM tbl_kodepos WHERE provinsi = ? ORDER BY kabupaten";
            System.out.println("DEBUG: Executing query: " + query + " with province: " + provinceName); // Log query
            pstmt = conn.prepareStatement(query);
            pstmt.setString(1, provinceName);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                cities.add(rs.getString("kabupaten"));
            }
            System.out.println("DEBUG: Cities loaded for " + provinceName + ": " + cities.size()); // Log count
        } catch (SQLException e) {
            System.err.println("ERROR in getCitiesFromDB: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error memuat kota/kabupaten dari kodepos: " + e.getMessage(), "Error Database", JOptionPane.ERROR_MESSAGE);
        } finally {
            DatabaseConnection.closeConnection(conn, pstmt, rs);
        }
        return cities;
    }

    // NEW METHOD: getKecamatansFromDB
    private List<String> getKecamatansFromDB(String provinceName, String cityName) {
        List<String> kecamatans = new ArrayList<>();
        if (provinceName == null || provinceName.isEmpty() || cityName == null || cityName.isEmpty()) return kecamatans;

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            String query = "SELECT DISTINCT kecamatan FROM tbl_kodepos WHERE provinsi = ? AND kabupaten = ? ORDER BY kecamatan";
            pstmt = conn.prepareStatement(query);
            pstmt.setString(1, provinceName);
            pstmt.setString(2, cityName);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                kecamatans.add(rs.getString("kecamatan"));
            }
        } catch (SQLException e) {
            System.err.println("Error loading kecamatans from tbl_kodepos: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error memuat kecamatan dari kodepos: " + e.getMessage(), "Error Database", JOptionPane.ERROR_MESSAGE);
        } finally {
            DatabaseConnection.closeConnection(conn, pstmt, rs);
        }
        return kecamatans;
    }

    // NEW METHOD: getKelurahansFromDB
    private List<String> getKelurahansFromDB(String provinceName, String cityName, String kecamatanName) {
        List<String> kelurahans = new ArrayList<>();
        if (provinceName == null || provinceName.isEmpty() || cityName == null || cityName.isEmpty() || kecamatanName == null || kecamatanName.isEmpty()) return kelurahans;

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            String query = "SELECT DISTINCT kelurahan FROM tbl_kodepos WHERE provinsi = ? AND kabupaten = ? AND kecamatan = ? ORDER BY kelurahan";
            pstmt = conn.prepareStatement(query);
            pstmt.setString(1, provinceName);
            pstmt.setString(2, cityName);
            pstmt.setString(3, kecamatanName);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                kelurahans.add(rs.getString("kelurahan"));
            }
        } catch (SQLException e) {
            System.err.println("Error loading kelurahans from tbl_kodepos: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error memuat kelurahan dari kodepos: " + e.getMessage(), "Error Database", JOptionPane.ERROR_MESSAGE);
        } finally {
            DatabaseConnection.closeConnection(conn, pstmt, rs);
        }
        return kelurahans;
    }

    // MODIFIED METHOD: getPostalCodesFromDB to include kecamatan and kelurahan
    private List<String> getPostalCodesFromDB(String provinceName, String cityName, String kecamatanName, String kelurahanName) {
        List<String> postalCodes = new ArrayList<>();
        if (provinceName == null || provinceName.isEmpty() || cityName == null || cityName.isEmpty() || 
            kecamatanName == null || kecamatanName.isEmpty() || kelurahanName == null || kelurahanName.isEmpty()) return postalCodes;

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            // Query modified to filter by kelurahan and kecamatan
            String query = "SELECT DISTINCT kodepos FROM tbl_kodepos WHERE provinsi = ? AND kabupaten = ? AND kecamatan = ? AND kelurahan = ? ORDER BY kodepos";
            pstmt = conn.prepareStatement(query);
            pstmt.setString(1, provinceName);
            pstmt.setString(2, cityName);
            pstmt.setString(3, kecamatanName);
            pstmt.setString(4, kelurahanName);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                postalCodes.add(rs.getString("kodepos"));
            }
        } catch (SQLException e) {
            System.err.println("Error loading postal codes from tbl_kodepos: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error memuat kode pos dari kodepos: " + e.getMessage(), "Error Database", JOptionPane.ERROR_MESSAGE);
        } finally {
            DatabaseConnection.closeConnection(conn, pstmt, rs);
        }
        return postalCodes;
    }

    private void addAddressToDatabase(Address address) {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DatabaseConnection.getConnection();
            String query = "INSERT INTO addresses (user_id, label, full_address, city, province, postal_code, country, kecamatan, kelurahan) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"; // Modified query
            stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            stmt.setInt(1, address.getUserId());
            stmt.setString(2, address.getLabel());
            stmt.setString(3, address.getFullAddress());
            stmt.setString(4, address.getCity());
            stmt.setString(5, address.getProvince());
            stmt.setString(6, address.getPostalCode());
            stmt.setString(7, address.getCountry());
            stmt.setString(8, address.getKecamatan()); // NEW
            stmt.setString(9, address.getKelurahan());  // NEW
            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        address.id = generatedKeys.getInt(1);
                    }
                }
                JOptionPane.showMessageDialog(null, "Alamat berhasil ditambahkan!", "Sukses", JOptionPane.INFORMATION_MESSAGE);
                initAddressesFromDatabase();
                updateDisplayedAddress();
            } else {
                JOptionPane.showMessageDialog(null, "Gagal menambahkan alamat.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            System.err.println("Error adding address to DB: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            DatabaseConnection.closeConnection(conn, stmt, null);
        }
    }

    private void updateAddressInDatabase(Address address) {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DatabaseConnection.getConnection();
            String query = "UPDATE addresses SET label = ?, full_address = ?, city = ?, province = ?, postal_code = ?, country = ?, kecamatan = ?, kelurahan = ? WHERE id = ? AND user_id = ?"; // Modified query
            stmt = conn.prepareStatement(query);
            stmt.setString(1, address.getLabel());
            stmt.setString(2, address.getFullAddress());
            stmt.setString(3, address.getCity());
            stmt.setString(4, address.getProvince());
            stmt.setString(5, address.getPostalCode());
            stmt.setString(6, address.getCountry());
            stmt.setString(7, address.getKecamatan()); // NEW
            stmt.setString(8, address.getKelurahan());  // NEW
            stmt.setInt(9, address.getId());
            stmt.setInt(10, currentUser.getId());
            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                JOptionPane.showMessageDialog(null, "Alamat berhasil diperbarui!", "Sukses", JOptionPane.INFORMATION_MESSAGE);
                initAddressesFromDatabase();
                updateDisplayedAddress();
            } else {
                JOptionPane.showMessageDialog(null, "Gagal memperbarui alamat. Alamat tidak ditemukan atau tidak ada perubahan.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            System.err.println("Error updating address in DB: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            DatabaseConnection.closeConnection(conn, stmt, null);
        }
    }

    private void deleteAddress(int addressId) {
        int confirm = JOptionPane.showConfirmDialog(null, "Apakah Anda yakin ingin menghapus alamat ini?", "Konfirmasi Hapus", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            Connection conn = null;
            PreparedStatement stmt = null;
            try {
                conn = DatabaseConnection.getConnection();
                String query = "DELETE FROM addresses WHERE id = ? AND user_id = ?";
                stmt = conn.prepareStatement(query);
                stmt.setInt(1, addressId);
                stmt.setInt(2, currentUser.getId());
                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    JOptionPane.showMessageDialog(null, "Alamat berhasil dihapus!", "Sukses", JOptionPane.INFORMATION_MESSAGE);
                    initAddressesFromDatabase();
                    updateDisplayedAddress();
                } else {
                    JOptionPane.showMessageDialog(null, "Gagal menghapus alamat. Alamat tidak ditemukan.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException e) {
                System.err.println("Error deleting address from DB: " + e.getMessage());
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } finally {
                DatabaseConnection.closeConnection(conn, stmt, null);
            }
        }
    }
    private String getTextFieldValue(JTextField textField) {
        if (Boolean.TRUE.equals(textField.getClientProperty("showingPlaceholder"))) {
            return "";
        }
        return textField.getText().trim();
    }

    private String getTextAreaValue(JTextArea textArea) {
        if (Boolean.TRUE.equals(textArea.getClientProperty("showingPlaceholder"))) {
            return "";
        }
        return textArea.getText().trim();
    }

    private void setupPlaceholder(JTextComponent component, String placeholder) {
        component.putClientProperty("placeholder", placeholder);

        Object originalFg = component.getClientProperty("originalForeground");
        if (originalFg == null || !(originalFg instanceof Color)) {
            component.putClientProperty("originalForeground", component.getForeground());
        }

        boolean isTextEmptyOrPlaceholder = component.getText().isEmpty() || component.getText().equals(placeholder);
        component.putClientProperty("showingPlaceholder", isTextEmptyOrPlaceholder);

        if (isTextEmptyOrPlaceholder) {
            component.setText(placeholder);
            component.setForeground(GRAY_TEXT_COLOR);
        } else {
            component.setForeground((Color) component.getClientProperty("originalForeground"));
        }

        boolean listenerExists = false;
        for (FocusListener listener : component.getFocusListeners()) {
            if (listener instanceof PlaceholderFocusListener && ((PlaceholderFocusListener) listener).getComponent() == component) {
                listenerExists = true;
                break;
            }
        }
        if (!listenerExists) {
            component.addFocusListener(new PlaceholderFocusListener(component, placeholder, GRAY_TEXT_COLOR));
        }
    }

    private class PlaceholderFocusListener implements FocusListener {
        private JTextComponent component;
        private String placeholder;
        private Color placeholderColor;
        private Color originalForeground;

        public PlaceholderFocusListener(JTextComponent component, String placeholder, Color placeholderColor) {
            this.component = component;
            this.placeholder = placeholder;
            this.placeholderColor = placeholderColor;
            this.originalForeground = (Color) component.getClientProperty("originalForeground");
            if (this.originalForeground == null) {
                this.originalForeground = component.getForeground();
                component.putClientProperty("originalForeground", this.originalForeground);
            }
        }

        public JTextComponent getComponent() {
            return component;
        }

        @Override
        public void focusGained(FocusEvent e) {
            if (Boolean.TRUE.equals(component.getClientProperty("showingPlaceholder"))) {
                component.setText("");
                component.setForeground(originalForeground);
                component.putClientProperty("showingPlaceholder", false);
            }
        }

        @Override
        public void focusLost(FocusEvent e) {
            if (component.getText().isEmpty()) {
                component.setText(placeholder);
                component.setForeground(placeholderColor);
                component.putClientProperty("showingPlaceholder", true);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("UI Pemilihan Alamat");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1200, 800);

           
            User dummyUser = new User(1, "testuser", "hashedpass", "test@example.com", "1234567890123456", "08123456789", null, null, "user", true);
            // Untuk menguji role supervisor:
            // User dummyUser = new User(18, "manager", "hashedpass", "manager@example.com", "1234567890123456", "08123456789", null, null, "supervisor", true);
            try {
                java.lang.reflect.Field field = Authentication.class.getDeclaredField("currentUser");
                field.setAccessible(true);
                field.set(null, dummyUser);
                System.out.println("User dummy diatur untuk pengujian di AddressUI.");
            } catch (Exception e) {
                System.err.println("Gagal mengatur user dummy untuk pengujian di AddressUI: " + e.getMessage());
            }

            ViewController dummyVC = new ViewController() {
                @Override public void showProductDetail(FavoritesUI.FavoriteItem product) { System.out.println("Dummy: Tampilkan Detail Produk: " + product.getName()); }
                @Override public void showFavoritesView() { System.out.println("Dummy: Tampilkan Tampilan Favorit"); }
                @Override public void showDashboardView() { System.out.println("Dummy: Tampilkan Tampilan Dashboard"); }
                @Override public void showCartView() { System.out.println("Dummy: Tampilkan Tampilan Keranjang"); }
                @Override public void showProfileView() { System.out.println("Dummy: Tampilkan Tampilan Profil"); }
                @Override public void showOrdersView() { System.out.println("Dummy: Tampilkan Tampilan Pesanan"); }
                @Override public void showCheckoutView() { System.out.println("Dummy: Tampilkan Tampilan Checkout (Sukses)"); }
                @Override public void showAddressView() { System.out.println("Dummy: Tampilkan Tampilan Alamat (Sukses)"); }
                @Override
                public void showPaymentView(Address selectedAddress, ShippingService selectedShippingService, double totalAmount) {
                    System.out.println("Dummy: Tampilkan Tampilan Pembayaran dengan Alamat (" + selectedAddress.getLabel() + ") dan Jasa Pengiriman (" + selectedShippingService.providerName + ") dengan Total Amount: " + totalAmount);
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
                    System.out.println("Dummy: Tampilkan Chat dengan Penjual ID: " + sellerId + " (" + sellerUsername + ")");
                }
                @Override public void showProductReviewView(int productId, String productName, String productImage, double productPrice, String sellerName) {
                    System.out.println("Dummy: Tampilkan Tampilan Review Produk");
                }
            };

            AddressUI addressUI = new AddressUI(dummyVC);
            frame.add(addressUI);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}