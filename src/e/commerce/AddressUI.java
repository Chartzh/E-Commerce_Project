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


// Import tambahan untuk database
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement; // Untuk Statement.RETURN_GENERATED_KEYS

import e.commerce.ProductRepository;
import e.commerce.ProductRepository.CartItem;
import javax.swing.text.JTextComponent;
// Asumsi DatabaseConnection, Authentication, User, dan ViewController ada di paket yang sama.

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

    private List<Address> savedAddresses; // Akan diisi dari database
    private Address selectedAddress;      // Alamat yang dipilih dari database

    private Map<String, ShippingService> shippingServices;
    private ShippingService selectedShippingService;

    // Tambahan untuk dropdown database
    private Map<String, Integer> provinceMap;
    private Map<String, Integer> cityMap;


    private static final Color ORANGE_THEME = new Color(255, 69, 0);
    private static final Color LIGHT_GRAY_BACKGROUND = new Color(245, 245, 245);
    private static final Color BORDER_COLOR = new Color(230, 230, 230);
    private static final Color DARK_TEXT_COLOR = new Color(50, 50, 50);
    private static final Color GRAY_TEXT_COLOR = new Color(120, 120, 120);
    private static final Color GREEN_DISCOUNT_TEXT = new Color(76, 175, 80);
    private static final Color TEAL_FREE_TEXT = new Color(0, 150, 136);

    // =========================================================================
    // MODIFIKASI: INNER CLASS Address
    // Disertakan di sini, dan disesuaikan untuk data dari database 'addresses'
    // =========================================================================
    private static class Address {
        int id; // ID dari database (kolom 'id')
        int userId; // user_id dari database (kolom 'user_id')
        String label; // e.g., 'Home', 'Office' (kolom 'label')
        String fullAddress; // e.g., 'Jl. Merdeka No. 10' (kolom 'full_address')
        String city; // e.g., 'Jakarta Pusat' (kolom 'city')
        String province; // e.g., 'DKI Jakarta' (kolom 'province')
        String postalCode; // e.g., '10110' (kolom 'postal_code')
        String country; // e.g., 'Indonesia' (kolom 'country')

        // 'isDefault' dari versi dummy sebelumnya tidak ada di sini,
        // karena pemilihan alamat default dilakukan saat query DB (ORDER BY).
        // Parameter 'name' dan 'mobile' dari constructor dummy lama akan diganti
        // dengan data dari objek User yang login saat display.

        public Address(int id, int userId, String label, String fullAddress, String city, String province, String postalCode, String country) {
            this.id = id;
            this.userId = userId;
            this.label = label;
            this.fullAddress = fullAddress;
            this.city = city;
            this.province = province;
            this.postalCode = postalCode;
            this.country = country;
        }

        // Getters (dibuat agar kompatibel dengan data DB)
        public int getId() { return id; }
        public int getUserId() { return userId; }
        public String getLabel() { return label; }
        public String getFullAddress() { return fullAddress; }
        public String getCity() { return city; }
        public String getProvince() { return province; }
        public String getPostalCode() { return postalCode; }
        public String getCountry() { return country; }

        // Setters (diperlukan jika objek Address ini akan diubah via dialog Add/Edit)
        public void setLabel(String label) { this.label = label; }
        public void setFullAddress(String fullAddress) { this.fullAddress = fullAddress; }
        public void setCity(String city) { this.city = city; }
        public void setProvince(String province) { this.province = province; }
        public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
        public void setCountry(String country) { this.country = country; }

        // Metode untuk format detail alamat untuk ditampilkan di UI.
        // Menerima objek User untuk mendapatkan nama penerima dan nomor HP.
        public String getFormattedAddressDetailsForDisplay(User user) {
            String recipientName = user.getUsername(); // Mengambil nama penerima dari User
            String mobileNumber = user.getPhone() != null ? user.getPhone() : "N/A"; // Mengambil mobile dari User

            // Menggunakan HTML untuk format multi-baris di JLabel
            return recipientName + "<br>" +
                   fullAddress + "<br>" +
                   city + ", " + province + " - " + postalCode + "<br>" +
                   country + "<br>" +
                   "Mobile: " + mobileNumber;
        }
    }
    // =========================================================================
    // END MODIFIKASI: INNER CLASS Address
    // =========================================================================

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
            return;
        }

        loadCartItemsFromDatabase();
        initAddressesFromDatabase(); // MODIFIKASI: Memuat alamat dari database
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

    // =========================================================================
    // MODIFIKASI: initAddresses() -> initAddressesFromDatabase()
    // Mengambil alamat dari database (mengganti data dummy)
    // =========================================================================
    private void initAddressesFromDatabase() { // Nama metode diubah dari initAddresses
        savedAddresses = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            // Mengambil semua alamat dari database untuk user ini.
            // Diurutkan berdasarkan ID untuk konsistensi, bisa ditambahkan is_default jika ada.
            String query = "SELECT id, user_id, label, full_address, city, province, postal_code, country " +
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
                    rs.getString("country")
                );
                savedAddresses.add(addr);
            }

            if (!savedAddresses.isEmpty()) {
                // Pilih alamat pertama sebagai alamat yang sedang terpilih untuk display utama
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
    // =========================================================================
    // END MODIFIKASI: initAddresses() -> initAddressesFromDatabase()
    // =========================================================================

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

        updateDisplayedAddress(); // Panggil ini setelah komponen dibuat dan alamat dimuat
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
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(230, 230, 230), 1, true),
                new EmptyBorder(20, 20, 20, 20)
        ));

        // Header dengan title dan tombol change
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(Color.WHITE);
        titlePanel.setBorder(new EmptyBorder(0, 0, 15, 0));

        JLabel titleLabel = new JLabel("Delivery Address");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setForeground(new Color(51, 51, 51));
        titlePanel.add(titleLabel, BorderLayout.WEST);

        JButton changeButton = new JButton("CHANGE ADDRESS");
        changeButton.setFont(new Font("Arial", Font.PLAIN, 11));
        changeButton.setForeground(ORANGE_THEME);
        changeButton.setBackground(Color.WHITE);
        changeButton.setBorderPainted(false);
        changeButton.setFocusPainted(false);
        changeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        changeButton.addActionListener(e -> showAddressSelectionDialog());
        titlePanel.add(changeButton, BorderLayout.EAST);

        panel.add(titlePanel, BorderLayout.NORTH);

        // Container untuk detail alamat
        JPanel addressDetailsContainer = new JPanel();
        addressDetailsContainer.setLayout(new BoxLayout(addressDetailsContainer, BoxLayout.Y_AXIS));
        addressDetailsContainer.setBackground(Color.WHITE);
        addressDetailsContainer.setBorder(new EmptyBorder(0, 0, 15, 0));

        // Nama penerima
        currentAddressNameLabel = new JLabel();
        currentAddressNameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        currentAddressNameLabel.setForeground(new Color(51, 51, 51));
        currentAddressNameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        currentAddressNameLabel.setBorder(new EmptyBorder(0, 0, 8, 0));
        addressDetailsContainer.add(currentAddressNameLabel);

        // Label alamat
        JLabel addressLabel = new JLabel("Address");
        addressLabel.setFont(new Font("Arial", Font.BOLD, 12));
        addressLabel.setForeground(new Color(102, 102, 102));
        addressLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        addressLabel.setBorder(new EmptyBorder(0, 0, 5, 0));
        addressDetailsContainer.add(addressLabel);

        // Detail alamat
        currentAddressDetailLabel = new JLabel();
        currentAddressDetailLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        currentAddressDetailLabel.setForeground(new Color(51, 51, 51));
        currentAddressDetailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        currentAddressDetailLabel.setBorder(new EmptyBorder(0, 0, 8, 0));
        addressDetailsContainer.add(currentAddressDetailLabel);

        // Nomor telepon
        currentAddressMobileLabel = new JLabel();
        currentAddressMobileLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        currentAddressMobileLabel.setForeground(new Color(51, 51, 51));
        currentAddressMobileLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        currentAddressMobileLabel.setBorder(new EmptyBorder(0, 0, 15, 0));
        addressDetailsContainer.add(currentAddressMobileLabel);

        panel.add(addressDetailsContainer, BorderLayout.CENTER);

        // Status panel dengan COD dan estimasi pengiriman
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBackground(Color.WHITE);
        statusPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

        // COD status
        JPanel codPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        codPanel.setBackground(Color.WHITE);

        JLabel codDot = new JLabel("●");
        codDot.setFont(new Font("Arial", Font.BOLD, 12));
        codDot.setForeground(new Color(46, 204, 113)); // Green color
        codPanel.add(codDot);

        JLabel codLabel = new JLabel(" Cash on delivery available");
        codLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        codLabel.setForeground(new Color(51, 51, 51));
        codPanel.add(codLabel);

        statusPanel.add(codPanel, BorderLayout.WEST);

        // Estimasi pengiriman
        deliveryEstimateHeaderLabel = new JLabel("Estimated Delivery 24 - 25 Sep"); // Ini akan diupdate
        deliveryEstimateHeaderLabel.setFont(new Font("Arial", Font.BOLD, 12));
        deliveryEstimateHeaderLabel.setForeground(new Color(51, 51, 51));
        statusPanel.add(deliveryEstimateHeaderLabel, BorderLayout.EAST);

        panel.add(statusPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void updateDisplayedAddress() {
        if (selectedAddress != null && currentUser != null) {
            // Mengambil nama penerima dari User yang login (asumsi username adalah nama penerima)
            currentAddressNameLabel.setText(currentUser.getUsername());
            // Mengambil mobile dari User yang login (asumsi phone adalah mobile)
            currentAddressMobileLabel.setText("Mobile: " + (currentUser.getPhone() != null ? currentUser.getPhone() : "N/A"));

            // Detail alamat dari objek Address (menggunakan HTML untuk JLabel)
            currentAddressDetailLabel.setText("<html>" + selectedAddress.getFormattedAddressDetailsForDisplay(currentUser) + "</html>");
            deliveryEstimateHeaderLabel.setText("Estimated Delivery " + calculateDeliveryDates("header"));
        } else {
            currentAddressNameLabel.setText("No address found");
            currentAddressDetailLabel.setText("<html><i>Please add an address in your profile settings.</i></html>");
            currentAddressMobileLabel.setText("");
            deliveryEstimateHeaderLabel.setText("");
        }
    }

    private void showAddressSelectionDialog() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                                    "Select Delivery Address", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(500, 450);
        dialog.setLocationRelativeTo(this);

        // Header dialog
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

        JLabel headerLabel = new JLabel("Select Delivery Address");
        headerLabel.setFont(new Font("Arial", Font.BOLD, 16));
        headerLabel.setForeground(new Color(51, 51, 51));
        headerPanel.add(headerLabel, BorderLayout.WEST);

        dialog.add(headerPanel, BorderLayout.NORTH);

        // Address list panel (now populated from database)
        JPanel addressListPanel = new JPanel();
        addressListPanel.setLayout(new BoxLayout(addressListPanel, BoxLayout.Y_AXIS));
        addressListPanel.setBorder(new EmptyBorder(10, 20, 10, 20));
        addressListPanel.setBackground(new Color(248, 248, 248));

        ButtonGroup addressButtonGroup = new ButtonGroup();

        // Load addresses for the dialog in real-time
        List<Address> addressesForDialog = getAllAddressesForUser(currentUser.getId());


        if (addressesForDialog.isEmpty()) {
            JLabel noAddressesLabel = new JLabel("No addresses found. Please add one in your profile settings.");
            noAddressesLabel.setFont(new Font("Arial", Font.ITALIC, 13));
            noAddressesLabel.setForeground(GRAY_TEXT_COLOR);
            noAddressesLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            addressListPanel.add(Box.createVerticalGlue());
            addressListPanel.add(noAddressesLabel);
            addressListPanel.add(Box.createVerticalGlue());
        } else {
            for (Address addr : addressesForDialog) {
                JPanel addressCard = createAddressCardForSelection(addr, addressButtonGroup);
                addressListPanel.add(addressCard);
                addressListPanel.add(Box.createVerticalStrut(10));
            }
        }


        JScrollPane scrollPane = new JScrollPane(addressListPanel);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(new Color(248, 248, 248));
        dialog.add(scrollPane, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

        JButton selectButton = new JButton("Select Address");
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
                updateDisplayedAddress(); // Update main panel with newly selected address
                dialog.dispose();
            } else {
                JOptionPane.showMessageDialog(dialog, "Please select an address.", "No Selection", JOptionPane.WARNING_MESSAGE);
            }
        });
        buttonPanel.add(selectButton);

        JButton closeButton = new JButton("Close");
        closeButton.setFont(new Font("Arial", Font.PLAIN, 12));
        closeButton.setBackground(new Color(240, 240, 240));
        closeButton.setBorder(new EmptyBorder(8, 15, 8, 15));
        closeButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(closeButton);

        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    // Helper method to create address cards for the selection dialog
    private JPanel createAddressCardForSelection(Address addr, ButtonGroup buttonGroup) {
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
        radioButton.setActionCommand(addr.getLabel()); // Use label as action command
        headerPanel.add(radioButton, BorderLayout.WEST);

        JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        labelPanel.setBackground(Color.WHITE);

        JLabel labelTag = new JLabel(addr.getLabel());
        labelTag.setFont(new Font("Arial", Font.BOLD, 14));
        labelTag.setForeground(new Color(51, 51, 51));
        labelPanel.add(labelTag);

        // Check if this address is the currently selected one in the main UI
        if (selectedAddress != null && selectedAddress.getId() == addr.getId()) {
            radioButton.setSelected(true);
        }

        headerPanel.add(labelPanel, BorderLayout.CENTER);
        card.add(headerPanel, BorderLayout.NORTH);

        // Detail alamat (menggunakan getFormattedAddressDetailsForDisplay dari objek Address)
        JLabel addressDetailLabel = new JLabel("<html>" + addr.getFormattedAddressDetailsForDisplay(currentUser) + "</html>");
        addressDetailLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        addressDetailLabel.setForeground(new Color(102, 102, 102));
        addressDetailLabel.setBorder(new EmptyBorder(8, 25, 0, 0));
        card.add(addressDetailLabel, BorderLayout.CENTER);

        buttonGroup.add(radioButton); // Add to button group for single selection

        // Click listener for the entire card
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                radioButton.setSelected(true);
                // No need to update selectedAddress here directly, it's handled by Select Address button
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
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(230, 230, 230), 1, true),
                new EmptyBorder(20, 20, 20, 20)
        ));

        // Header section
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(new EmptyBorder(0, 0, 20, 0));

        JLabel titleLabel = new JLabel("Jasa Pengiriman");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setForeground(new Color(51, 51, 51));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerPanel.add(titleLabel);

        headerPanel.add(Box.createVerticalStrut(5));

        JLabel subtitleLabel = new JLabel("Pilih metode pengiriman yang Anda inginkan.");
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        subtitleLabel.setForeground(new Color(102, 102, 102));
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerPanel.add(subtitleLabel);

        panel.add(headerPanel, BorderLayout.NORTH);

        // Services container
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

    private void populateShippingServices() {
        shippingServiceContainer.removeAll();
        ButtonGroup serviceButtonGroup = new ButtonGroup();

        boolean isFirst = true;
        for (Map.Entry<String, ShippingService> entry : shippingServices.entrySet()) {
            ShippingService service = entry.getValue();
            JPanel servicePanel = createShippingServicePanel(service, serviceButtonGroup);

            if (!isFirst) {
                shippingServiceContainer.add(Box.createVerticalStrut(8));
            }
            shippingServiceContainer.add(servicePanel);
            isFirst = false;
        }

        // Auto-select logic
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
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(220, 220, 220), 1, true),
                new EmptyBorder(15, 15, 15, 15)
        ));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        panel.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Left section with radio button and service info
        JPanel leftSection = new JPanel(new BorderLayout());
        leftSection.setBackground(Color.WHITE);

        JRadioButton radioButton = new JRadioButton();
        radioButton.setBackground(Color.WHITE);
        radioButton.setFocusPainted(false);
        radioButton.setActionCommand(service.name);
        radioButton.addActionListener(e -> {
            selectedShippingService = shippingServices.get(e.getActionCommand());
            deliveryCharge = selectedShippingService.price;
            updateSummaryTotals();
        });
        buttonGroup.add(radioButton);

        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        radioPanel.setBackground(Color.WHITE);
        radioPanel.add(radioButton);
        leftSection.add(radioPanel, BorderLayout.WEST);

        // Service name
        JLabel serviceNameLabel = new JLabel(service.name);
        serviceNameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        serviceNameLabel.setForeground(new Color(51, 51, 51));
        serviceNameLabel.setBorder(new EmptyBorder(0, 15, 0, 0));
        leftSection.add(serviceNameLabel, BorderLayout.CENTER);

        panel.add(leftSection, BorderLayout.WEST);

        // Right section with price and estimate
        JPanel rightSection = new JPanel();
        rightSection.setLayout(new BoxLayout(rightSection, BoxLayout.Y_AXIS));
        rightSection.setBackground(Color.WHITE);

        JLabel priceLabel = new JLabel(formatCurrency(service.price));
        priceLabel.setFont(new Font("Arial", Font.BOLD, 14));
        priceLabel.setForeground(new Color(51, 51, 51));
        priceLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        rightSection.add(priceLabel);

        rightSection.add(Box.createVerticalStrut(3));

        JLabel estimateLabel = new JLabel(service.arrivalEstimate);
        estimateLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        estimateLabel.setForeground(new Color(102, 102, 102));
        estimateLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        rightSection.add(estimateLabel);

        panel.add(rightSection, BorderLayout.EAST);

        // Add hover effect
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
        LocalDate orderDate = LocalDate.now(); // Menggunakan tanggal hari ini

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

    // =========================================================================
    // DATABASE METHODS FOR ADDRESS (Tambahan, untuk mengambil dan mengelola alamat)
    // =========================================================================

    // Mengambil alamat utama/default dari database (untuk panel "Delivery Address" utama)
    private Address getPrimaryDeliveryAddress(int userId) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Address primaryAddress = null;

        try {
            conn = DatabaseConnection.getConnection();
            String query = "SELECT id, user_id, label, full_address, city, province, postal_code, country " +
                           "FROM addresses WHERE user_id = ? ORDER BY id ASC LIMIT 1"; // Mengambil alamat pertama
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
                    rs.getString("country")
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

    // Metode untuk memuat semua alamat dari database (untuk dialog "CHANGE ADDRESS")
    private List<Address> getAllAddressesForUser(int userId) {
        List<Address> addresses = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            String query = "SELECT id, user_id, label, full_address, city, province, postal_code, country FROM addresses WHERE user_id = ?";
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
                    rs.getString("country")
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

    // Metode untuk mengambil daftar provinsi dari tbl_kodepos (untuk dropdown di dialog)
    // Map untuk provinsi dan kota tidak diperlukan di sini karena hanya untuk dropdown dialog
    // dan tidak menyimpan ID untuk update Address object.
    private List<String> getProvincesFromDB() {
        List<String> provinces = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            String query = "SELECT DISTINCT provinsi FROM tbl_kodepos ORDER BY provinsi";
            pstmt = conn.prepareStatement(query);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                provinces.add(rs.getString("provinsi"));
            }
        } catch (SQLException e) {
            System.err.println("Error loading provinces from tbl_kodepos: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error memuat provinsi dari kodepos: " + e.getMessage(), "Error Database", JOptionPane.ERROR_MESSAGE);
        } finally {
            DatabaseConnection.closeConnection(conn, pstmt, rs);
        }
        return provinces;
    }

    // Metode untuk mengambil daftar kabupaten/kota dari tbl_kodepos berdasarkan provinsi (untuk dropdown di dialog)
    private List<String> getCitiesFromDB(String provinceName) {
        List<String> cities = new ArrayList<>();
        if (provinceName == null || provinceName.isEmpty()) return cities;

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            String query = "SELECT DISTINCT kabupaten FROM tbl_kodepos WHERE provinsi = ? ORDER BY kabupaten";
            pstmt = conn.prepareStatement(query);
            pstmt.setString(1, provinceName);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                cities.add(rs.getString("kabupaten"));
            }
        } catch (SQLException e) {
            System.err.println("Error loading cities from tbl_kodepos: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error memuat kota/kabupaten dari kodepos: " + e.getMessage(), "Error Database", JOptionPane.ERROR_MESSAGE);
        } finally {
            DatabaseConnection.closeConnection(conn, pstmt, rs);
        }
        return cities;
    }

    // Metode untuk mengambil daftar kodepos dari tbl_kodepos berdasarkan provinsi dan kabupaten/kota (untuk dropdown di dialog)
    private List<String> getPostalCodesFromDB(String provinceName, String cityName) {
        List<String> postalCodes = new ArrayList<>();
        if (provinceName == null || provinceName.isEmpty() || cityName == null || cityName.isEmpty()) return postalCodes;

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            String query = "SELECT DISTINCT kodepos FROM tbl_kodepos WHERE provinsi = ? AND kabupaten = ? ORDER BY kodepos";
            pstmt = conn.prepareStatement(query);
            pstmt.setString(1, provinceName);
            pstmt.setString(2, cityName);
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

    // Metode untuk menambahkan alamat baru ke database (dipanggil dari dialog)
    // Perhatikan: dialog Add/Edit saat ini tidak ada di AddressUI ini.
    // Jika Anda ingin tombol "Add New Address" di dialog pemilihan, Anda perlu mengimplementasikan dialog tersebut.
    private void addAddressToDatabase(Address address) {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DatabaseConnection.getConnection();
            String query = "INSERT INTO addresses (user_id, label, full_address, city, province, postal_code, country) VALUES (?, ?, ?, ?, ?, ?, ?)";
            stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            stmt.setInt(1, address.getUserId());
            stmt.setString(2, address.getLabel());
            stmt.setString(3, address.getFullAddress());
            stmt.setString(4, address.getCity());
            stmt.setString(5, address.getProvince());
            stmt.setString(6, address.getPostalCode());
            stmt.setString(7, address.getCountry());
            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        address.id = generatedKeys.getInt(1);
                    }
                }
                JOptionPane.showMessageDialog(null, "Alamat berhasil ditambahkan!", "Sukses", JOptionPane.INFORMATION_MESSAGE);
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

    // Metode untuk mengupdate alamat di database (dipanggil dari dialog)
    private void updateAddressInDatabase(Address address) {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DatabaseConnection.getConnection();
            String query = "UPDATE addresses SET label = ?, full_address = ?, city = ?, province = ?, postal_code = ?, country = ? WHERE id = ? AND user_id = ?";
            stmt = conn.prepareStatement(query);
            stmt.setString(1, address.getLabel());
            stmt.setString(2, address.getFullAddress());
            stmt.setString(3, address.getCity());
            stmt.setString(4, address.getProvince());
            stmt.setString(5, address.getPostalCode());
            stmt.setString(6, address.getCountry());
            stmt.setInt(7, address.getId());
            stmt.setInt(8, address.getUserId());
            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                JOptionPane.showMessageDialog(null, "Alamat berhasil diperbarui!", "Sukses", JOptionPane.INFORMATION_MESSAGE);
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

    // Metode untuk menghapus alamat dari database (dipanggil dari dialog)
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
                    // Setelah dihapus, muat ulang alamat di UI
                    initAddressesFromDatabase(); // Muat ulang selectedAddress dan savedAddresses
                    updateDisplayedAddress(); // Update tampilan
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
    // =========================================================================
    // END DATABASE METHODS FOR ADDRESS
    // =========================================================================

    // =========================================================================
    // UTILITY METHODS (Untuk dialog Add/Edit Address, diambil dari ProfileUI/AddressUI sebelumnya)
    // =========================================================================
    // Ini adalah utility method untuk placeholder yang mungkin tidak digunakan di AddressUI ini
    // tapi disertakan agar tidak ada error jika masih dipanggil.
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
            component.setForeground(GRAY_TEXT_COLOR); // Gunakan GRAY_TEXT_COLOR dari AddressUI
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
            component.addFocusListener(new PlaceholderFocusListener(component, placeholder, GRAY_TEXT_COLOR)); // Gunakan GRAY_TEXT_COLOR
        }
    }

    // Inner class for Placeholder FocusListener
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
    // =========================================================================
    // END UTILITY METHODS
    // =========================================================================


    // =========================================================================
    // MAIN METHOD (untuk testing standalone AddressUI) - tidak berubah
    // =========================================================================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Address Selection UI");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1200, 800);

            // Dummy user untuk testing (HANYA UNTUK TESTING)
            User dummyUser = new User(1, "testuser", "hashedpass", "test@example.com", "1234567890123456", "08123456789", null, null, "user", true);
            try {
                // Menggunakan reflection untuk mengatur currentUser di Authentication
                java.lang.reflect.Field field = Authentication.class.getDeclaredField("currentUser");
                field.setAccessible(true);
                field.set(null, dummyUser);
                System.out.println("Dummy user set for testing.");
            } catch (Exception e) {
                System.err.println("Failed to set dummy user for testing: " + e.getMessage());
            }

            // Dummy ViewController
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

            AddressUI addressUI = new AddressUI(dummyVC);
            frame.add(addressUI);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}