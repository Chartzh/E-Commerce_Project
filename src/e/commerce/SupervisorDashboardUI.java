package e.commerce;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import java.text.NumberFormat;
import java.util.Locale;

public class SupervisorDashboardUI extends JFrame {
    private JPanel mainPanel;
    private CardLayout cardLayout;
    private DefaultTableModel productTableModel;
    private JTable productTable;
    private DefaultTableModel orderTableModel;
    private JTable orderTable;
    private JButton[] navButtons;
    private User currentUser;

    public SupervisorDashboardUI() {
        currentUser = Authentication.getCurrentUser();
        if (currentUser == null || (!currentUser.getRole().equals("supervisor") && !currentUser.getRole().equals("admin"))) {
            LoginUI loginUI = new LoginUI();
            loginUI.setVisible(true);
            dispose();
            return;
        }

        setTitle("Quantra - Supervisor Dashboard");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        IconUtil.setIcon(this);
        setLayout(new BorderLayout());

        JPanel sidebar = createSidebar();
        mainPanel = new JPanel();
        cardLayout = new CardLayout();
        mainPanel.setLayout(cardLayout);

        JPanel headerPanel = createHeaderPanel(currentUser);
        JPanel productPanel = createProductManagementPanel();
        JPanel ordersPanel = createOrdersPanel();
        ProfileUI profilePanel = new ProfileUI();

        mainPanel.add(productPanel, "Products");
        mainPanel.add(ordersPanel, "Orders");
        mainPanel.add(profilePanel, "Profile");

        JPanel contentWrapper = new JPanel(new BorderLayout());
        contentWrapper.add(headerPanel, BorderLayout.NORTH);
        contentWrapper.add(mainPanel, BorderLayout.CENTER);

        add(sidebar, BorderLayout.WEST);
        add(contentWrapper, BorderLayout.CENTER);
        
        cardLayout.show(mainPanel, "Products");
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(245, 245, 245));
        sidebar.setPreferredSize(new Dimension(200, 0));
        sidebar.setBorder(new EmptyBorder(20, 10, 20, 10));

        JLabel lblLogo = new JLabel("Quantra");
        lblLogo.setFont(new Font("Arial", Font.BOLD, 20));
        lblLogo.setForeground(new Color(255, 99, 71));
        lblLogo.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(lblLogo);
        sidebar.add(Box.createRigidArea(new Dimension(0, 30)));

        String[] navItems = {"Products", "Orders", "Analytics", "Settings"};
        String[] panelNames = {"Products", "Orders", "Products", "Profile"};
        navButtons = new JButton[navItems.length];

        for (int i = 0; i < navItems.length; i++) {
            JButton navButton = new JButton(navItems[i]);
            navButton.setFont(new Font("Arial", Font.PLAIN, 14));
            navButton.setForeground(Color.BLACK);
            navButton.setBackground(new Color(245, 245, 245));
            navButton.setBorderPainted(false);
            navButton.setFocusPainted(false);
            navButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            navButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            navButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

            if (navItems[i].equals("Products")) {
                navButton.setBackground(new Color(255, 99, 71));
                navButton.setForeground(Color.WHITE);
            }

            navButtons[i] = navButton;

            final String panelName = panelNames[i];
            final int index = i;
            navButton.addActionListener(e -> {
                for (JButton btn : navButtons) {
                    btn.setBackground(new Color(245, 245, 245));
                    btn.setForeground(Color.BLACK);
                }
                navButtons[index].setBackground(new Color(255, 99, 71));
                navButtons[index].setForeground(Color.WHITE);

                cardLayout.show(mainPanel, panelName);
            });

            sidebar.add(navButton);
            sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        sidebar.add(Box.createVerticalGlue());

        JButton helpButton = new JButton("Help");
        helpButton.setFont(new Font("Arial", Font.PLAIN, 14));
        helpButton.setForeground(Color.BLACK);
        helpButton.setBackground(new Color(245, 245, 245));
        helpButton.setBorderPainted(false);
        helpButton.setFocusPainted(false);
        helpButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        helpButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        helpButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        sidebar.add(helpButton);

        return sidebar;
    }

    private JPanel createHeaderPanel(User currentUser) {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setPreferredSize(new Dimension(0, 60));
        headerPanel.setBorder(new EmptyBorder(10, 20, 10, 20));

        JTextField searchField = new JTextField("Search products...");
        searchField.setForeground(Color.GRAY);
        searchField.setPreferredSize(new Dimension(300, 30));
        searchField.setBorder(new LineBorder(Color.LIGHT_GRAY, 1, true));
        searchField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (searchField.getText().equals("Search products...")) {
                    searchField.setText("");
                    searchField.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (searchField.getText().isEmpty()) {
                    searchField.setText("Search products...");
                    searchField.setForeground(Color.GRAY);
                }
            }
        });
        headerPanel.add(searchField, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.setBackground(Color.WHITE);

        JLabel lblUser = new JLabel(currentUser.getUsername());
        lblUser.setFont(new Font("Arial", Font.PLAIN, 14));
        lblUser.setForeground(Color.BLACK);

        JButton btnLogout = new JButton("Logout");
        btnLogout.setFont(new Font("Arial", Font.PLAIN, 12));
        btnLogout.setBackground(new Color(255, 69, 0));
        btnLogout.setForeground(Color.WHITE);
        btnLogout.setBorderPainted(false);
        btnLogout.setFocusPainted(false);
        btnLogout.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnLogout.addActionListener(e -> {
            int response = JOptionPane.showConfirmDialog(SupervisorDashboardUI.this,
                            "Are you sure you want to logout?",
                            "Confirm",
                            JOptionPane.YES_NO_OPTION);
            if (response == JOptionPane.YES_OPTION) {
                Authentication.logout();
                LoginUI loginUI = new LoginUI();
                loginUI.setVisible(true);
                dispose();
            }
        });

        rightPanel.add(lblUser);
        rightPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        rightPanel.add(btnLogout);
        headerPanel.add(rightPanel, BorderLayout.EAST);

        return headerPanel;
    }

    private JPanel createProductManagementPanel() {
        JPanel productPanel = new JPanel(new BorderLayout());
        productPanel.setBackground(Color.WHITE);
        productPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filterPanel.setBackground(Color.WHITE);

        JComboBox<String> categoryCombo = new JComboBox<>(new String[]{"All Categories", "Laptop", "Headphone", "Speaker", "Mouse", "Keyboard", "Smartwatch", "SSD", "USB-C Hub", "Webcam", "Phone Case", "Laptop Stand", "Power Bank", "Wireless Charger"});
        categoryCombo.setPreferredSize(new Dimension(150, 30));

        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"All Status", "Active", "Inactive"});
        statusCombo.setPreferredSize(new Dimension(120, 30));

        JTextField priceRangeField = new JTextField("Min - Max");
        priceRangeField.setPreferredSize(new Dimension(100, 30));
        priceRangeField.setForeground(Color.GRAY);
        priceRangeField.setBorder(new LineBorder(Color.LIGHT_GRAY, 1, true));

        JButton addProductButton = new JButton("Add New Product");
        addProductButton.setBackground(new Color(255, 69, 0));
        addProductButton.setForeground(Color.WHITE);
        addProductButton.setBorderPainted(false);
        addProductButton.setFocusPainted(false);
        addProductButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addProductButton.addActionListener(e -> showProductDialog(null));

        filterPanel.add(new JLabel("Category:"));
        filterPanel.add(categoryCombo);
        filterPanel.add(new JLabel("Status:"));
        filterPanel.add(statusCombo);
        filterPanel.add(new JLabel("Price:"));
        filterPanel.add(priceRangeField);
        filterPanel.add(Box.createHorizontalGlue());
        filterPanel.add(addProductButton);

        String[] columns = {"ID", "Product Name", "Price", "Stock", "Brand", "Condition", "Action"};
        productTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 6;
            }
        };

        productTable = new JTable(productTableModel);
        productTable.setRowHeight(60);
        productTable.setFont(new Font("Arial", Font.PLAIN, 14));
        productTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        productTable.getTableHeader().setBackground(new Color(255, 99, 71));
        productTable.getTableHeader().setForeground(Color.WHITE);

        loadProductsForSupervisor();

        productTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        productTable.getColumnModel().getColumn(1).setPreferredWidth(250);
        productTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        productTable.getColumnModel().getColumn(3).setPreferredWidth(80);
        productTable.getColumnModel().getColumn(4).setPreferredWidth(100);
        productTable.getColumnModel().getColumn(5).setPreferredWidth(100);
        productTable.getColumnModel().getColumn(6).setPreferredWidth(150);

        productTable.getColumnModel().getColumn(6).setCellRenderer(new ButtonRenderer());
        productTable.getColumnModel().getColumn(6).setCellEditor(new ButtonEditor(new JCheckBox(), this));

        JScrollPane scrollPane = new JScrollPane(productTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        JPanel paginationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        paginationPanel.setBackground(Color.WHITE);
        paginationPanel.setBorder(new EmptyBorder(10, 0, 0, 0));

        JButton prevButton = new JButton("<");
        prevButton.setEnabled(false);
        JButton nextButton = new JButton(">");
        JLabel pageLabel = new JLabel("1 of 1");
        for (JButton btn : new JButton[]{prevButton, nextButton}) {
            btn.setBackground(Color.WHITE);
            btn.setForeground(Color.BLACK);
            btn.setBorder(new LineBorder(Color.LIGHT_GRAY, 1));
            btn.setFocusPainted(false);
        }
        paginationPanel.add(prevButton);
        paginationPanel.add(new JButton("1"));
        paginationPanel.add(nextButton);
        paginationPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        paginationPanel.add(pageLabel);

        productPanel.add(filterPanel, BorderLayout.NORTH);
        productPanel.add(scrollPane, BorderLayout.CENTER);
        productPanel.add(paginationPanel, BorderLayout.SOUTH);

        return productPanel;
    }
    
    // --- Metode untuk memuat produk sesuai role supervisor (seller atau admin) ---
    // MENGUBAHNYA MENJADI PUBLIC
    public void loadProductsForSupervisor() { // <-- UBAH KE PUBLIC
        productTableModel.setRowCount(0);
        List<FavoritesUI.FavoriteItem> productsToDisplay;
        if (currentUser.getRole().equals("supervisor")) {
            productsToDisplay = ProductRepository.getProductsBySeller(currentUser.getId());
        } else if (currentUser.getRole().equals("admin")) {
            productsToDisplay = ProductRepository.getAllProducts();
        } else {
            productsToDisplay = new ArrayList<>();
            JOptionPane.showMessageDialog(this, "Anda tidak memiliki hak akses untuk melihat produk.", "Akses Ditolak", JOptionPane.WARNING_MESSAGE);
        }
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
        currencyFormat.setMinimumFractionDigits(0);
        for (FavoritesUI.FavoriteItem product : productsToDisplay) {
            String formattedPrice = currencyFormat.format(product.getPrice());
            String views = "N/A";
            
            productTableModel.addRow(new Object[]{
                product.getId(),
                product.getName(),
                formattedPrice,
                product.getStock(),
                product.getBrand(),
                product.getCondition(),
                ""
            });
        }
    }

    public void showProductDialog(Integer productId) {
        String title = (productId == null) ? "Add New Product" : "Edit Product";
        JDialog dialog = new JDialog(this, title, true);
        dialog.setSize(450, 600);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel lblName = new JLabel("Product Name:");
        JTextField txtName = new JTextField(20);
        txtName.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel lblPrice = new JLabel("Price:");
        JTextField txtPrice = new JTextField(20);
        txtPrice.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel lblOriginalPrice = new JLabel("Original Price (optional):");
        JTextField txtOriginalPrice = new JTextField(20);
        txtOriginalPrice.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel lblStock = new JLabel("Stock:");
        JTextField txtStock = new JTextField(20);
        txtStock.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel lblCondition = new JLabel("Condition:");
        JComboBox<String> comboCondition = new JComboBox<>(new String[]{"Baru", "Bekas"});
        comboCondition.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel lblMinOrder = new JLabel("Min. Order:");
        JTextField txtMinOrder = new JTextField(20);
        txtMinOrder.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel lblBrand = new JLabel("Brand:");
        JTextField txtBrand = new JTextField(20);
        txtBrand.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel lblDescription = new JLabel("Description:");
        JTextArea txtDescription = new JTextArea(5, 20);
        txtDescription.setLineWrap(true);
        txtDescription.setWrapStyleWord(true);
        JScrollPane descScrollPane = new JScrollPane(txtDescription);
        descScrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        JLabel lblImage = new JLabel("Product Images:");
        JPanel imageControlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField txtImagePath = new JTextField(20);
        txtImagePath.setEditable(false);
        JButton btnBrowseImage = new JButton("Browse");
        JButton btnManageImages = new JButton("Manage Images");
        
        imageControlPanel.add(txtImagePath);
        imageControlPanel.add(btnBrowseImage);

        List<byte[]> imagesToUpload = new ArrayList<>();
        List<String> imageExtensions = new ArrayList<>();
        
        if (productId != null) { // Mode Edit
            btnBrowseImage.setVisible(false);
            txtImagePath.setVisible(false);
            imageControlPanel.add(btnManageImages);
            FavoritesUI.FavoriteItem productToEdit = ProductRepository.getProductById(productId);
            if (productToEdit != null) {
                txtName.setText(productToEdit.getName());
                txtPrice.setText(String.valueOf(productToEdit.getPrice()));
                if (productToEdit.getOriginalPrice() > 0) {
                    txtOriginalPrice.setText(String.valueOf(productToEdit.getOriginalPrice()));
                }
                txtStock.setText(String.valueOf(productToEdit.getStock()));
                comboCondition.setSelectedItem(productToEdit.getCondition());
                txtMinOrder.setText(productToEdit.getMinOrder());
                txtBrand.setText(productToEdit.getBrand());
                txtDescription.setText(productToEdit.getDescription());
            } else {
                JOptionPane.showMessageDialog(dialog, "Product with ID " + productId + " not found.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else { // Mode Add New Product
            btnManageImages.setVisible(false);
        }


        btnBrowseImage.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setMultiSelectionEnabled(true);
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Image Files", "jpg", "jpeg", "png", "gif"));
            int result = fileChooser.showOpenDialog(dialog);
            if (result == JFileChooser.APPROVE_OPTION) {
                File[] selectedFiles = fileChooser.getSelectedFiles();
                imagesToUpload.clear();
                imageExtensions.clear();
                StringBuilder paths = new StringBuilder();
                for (File file : selectedFiles) {
                    try (FileInputStream fis = new FileInputStream(file)) {
                        byte[] imageData = new byte[(int) file.length()];
                        fis.read(imageData);
                        imagesToUpload.add(imageData);
                        String fileName = file.getName();
                        String extension = "";
                        int i = fileName.lastIndexOf('.');
                        if (i > 0) {
                            extension = fileName.substring(i + 1);
                        }
                        imageExtensions.add(extension);
                        
                        paths.append(file.getName()).append("; ");
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(dialog, "Error reading image file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        imagesToUpload.clear(); 
                        imageExtensions.clear();
                        txtImagePath.setText("");
                        return;
                    }
                }
                txtImagePath.setText(paths.toString().trim());
            }
        });

        btnManageImages.addActionListener(e -> {
            if (productId != null) {
                ProductImageManagerUI imageManagerDialog = new ProductImageManagerUI(this, productId);
                imageManagerDialog.setVisible(true);
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnCancel = new JButton("Cancel");
        JButton btnSave = new JButton("Save");

        btnSave.setBackground(new Color(255, 69, 0));
        btnSave.setForeground(Color.WHITE);
        btnSave.setFocusPainted(false);

        btnCancel.setBackground(new Color(108, 117, 125));
        btnCancel.setForeground(Color.WHITE);
        btnCancel.setFocusPainted(false);

        btnCancel.addActionListener(e -> dialog.dispose());

        btnSave.addActionListener(e -> {
            String name = txtName.getText().trim();
            String priceText = txtPrice.getText().trim();
            String originalPriceText = txtOriginalPrice.getText().trim();
            String stockText = txtStock.getText().trim();
            String condition = (String) comboCondition.getSelectedItem();
            String minOrder = txtMinOrder.getText().trim();
            String brand = txtBrand.getText().trim();
            String description = txtDescription.getText().trim();

            if (name.isEmpty() || priceText.isEmpty() || stockText.isEmpty() || minOrder.isEmpty() || brand.isEmpty() || description.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "All fields except Original Price must be filled!", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                double price = Double.parseDouble(priceText);
                double originalPrice = originalPriceText.isEmpty() ? 0.0 : Double.parseDouble(originalPriceText);
                int stock = Integer.parseInt(stockText);

                if (price <= 0 || stock < 0) {
                    JOptionPane.showMessageDialog(dialog, "Price must be greater than 0 and stock cannot be negative!", "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                if (productId == null && imagesToUpload.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "Please select at least one image for a new product!", "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                try {
                    if (productId == null) { // Add New Product
                        Integer userIdForProduct = currentUser.getId();
                        if (userIdForProduct == null) {
                            JOptionPane.showMessageDialog(dialog, "User ID not found. Please login as a seller.", "Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }

                        int newProductId = ProductRepository.saveProduct(name, description, price, originalPrice, stock, condition, minOrder, brand, userIdForProduct);
                        
                        if (newProductId != -1 && !imagesToUpload.isEmpty()) {
                            for (int i = 0; i < imagesToUpload.size(); i++) {
                                ProductRepository.saveProductImage(newProductId, imagesToUpload.get(i), imageExtensions.get(i), i == 0, i + 1);
                            }
                        }
                        JOptionPane.showMessageDialog(dialog, "Product added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    } else { // Edit Product
                        boolean updateProductSuccess = ProductRepository.updateProduct(productId, name, description, price, originalPrice, stock, condition, minOrder, brand);
                        
                        if (updateProductSuccess) {
                            // Gambar diupdate via ProductImageManagerUI. Disini hanya update detail teksnya.
                            // Jika ada gambar baru yang dipilih di dialog ini, abaikan.
                            // User harus menggunakan "Manage Images" untuk update gambar.
                            JOptionPane.showMessageDialog(dialog, "Product updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(dialog, "Failed to update product.", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                    loadProductsForSupervisor();
                    dialog.dispose();

                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(dialog, "Database Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Price, Original Price, and Stock must be numeric!", "Validation Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        buttonPanel.add(btnCancel);
        buttonPanel.add(btnSave);

        panel.add(lblName);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(txtName);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(lblPrice);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(txtPrice);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(lblOriginalPrice);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(txtOriginalPrice);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(lblStock);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(txtStock);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(lblCondition);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(comboCondition);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(lblMinOrder);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(txtMinOrder);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(lblBrand);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(txtBrand);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(lblDescription);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(descScrollPane);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(lblImage);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(imageControlPanel);


        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private JPanel createOrdersPanel() {
        JPanel ordersPanel = new JPanel(new BorderLayout());
        ordersPanel.setBackground(Color.WHITE);
        ordersPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filterPanel.setBackground(Color.WHITE);

        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"All Status", "Pending", "Processing", "Shipped", "Completed", "Cancelled"});
        statusCombo.setPreferredSize(new Dimension(120, 30));

        filterPanel.add(new JLabel("Status:"));
        filterPanel.add(statusCombo);

        String[] columns = {"", "ID", "Customer", "Date", "Total", "Status", "Action"};
        orderTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0 || column == 6;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Boolean.class : super.getColumnClass(columnIndex);
            }
        };

        orderTable = new JTable(orderTableModel);
        orderTable.setRowHeight(60);
        orderTable.setFont(new Font("Arial", Font.PLAIN, 14));
        orderTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        orderTable.getTableHeader().setBackground(new Color(255, 99, 71));
        orderTable.getTableHeader().setForeground(Color.WHITE);

        addDummyOrders();

        orderTable.getColumnModel().getColumn(0).setPreferredWidth(30);
        orderTable.getColumnModel().getColumn(1).setPreferredWidth(50);
        orderTable.getColumnModel().getColumn(2).setPreferredWidth(150);
        orderTable.getColumnModel().getColumn(3).setPreferredWidth(150);
        orderTable.getColumnModel().getColumn(4).setPreferredWidth(120);
        orderTable.getColumnModel().getColumn(5).setPreferredWidth(100);
        orderTable.getColumnModel().getColumn(6).setPreferredWidth(120);

        orderTable.getColumnModel().getColumn(6).setCellRenderer(new ButtonRenderer());
        orderTable.getColumnModel().getColumn(6).setCellEditor(new ButtonEditor(new JCheckBox(), this, "view"));

        JScrollPane scrollPane = new JScrollPane(orderTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        ordersPanel.add(filterPanel, BorderLayout.NORTH);
        ordersPanel.add(scrollPane, BorderLayout.CENTER);

        return ordersPanel;
    }

    private void addDummyOrders() {
        Object[][] dummyData = {
                {false, 1001, "Budi Santoso", "10-04-2025", "Rp 598.000", "Completed", ""},
                {false, 1002, "Ani Wijaya", "11-04-2025", "Rp 245.000", "Shipped", ""},
                {false, 1003, "Denny Pratama", "12-04-2025", "Rp 450.000", "Processing", ""},
                {false, 1004, "Sinta Dewi", "13-04-2025", "Rp 189.000", "Pending", ""},
                {false, 1005, "Rizki Ahmad", "14-04-2025", "Rp 458.000", "Cancelled", ""}
        };

        for (Object[] row : dummyData) {
            orderTableModel.addRow(row);
        }
    }

    public void showOrderDetailsWithStatusUpdate(int orderId) {
        int row = -1;
        for (int i = 0; i < orderTableModel.getRowCount(); i++) {
            if ((int) orderTableModel.getValueAt(i, 1) == orderId) {
                row = i;
                break;
            }
        }

        if (row != -1) {
            String date = (String) orderTableModel.getValueAt(row, 3);
            String total = (String) orderTableModel.getValueAt(row, 4);
            String status = (String) orderTableModel.getValueAt(row, 5);

            JDialog dialog = new JDialog(this, "Order Details #" + orderId, true);
            dialog.setSize(500, 450);
            dialog.setLocationRelativeTo(this);
            dialog.setLayout(new BorderLayout());

            JPanel headerPanel = new JPanel(new GridLayout(5, 1, 5, 5));
            headerPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
            headerPanel.add(new JLabel("Order ID: " + orderId));
            headerPanel.add(new JLabel("Date: " + date));
            headerPanel.add(new JLabel("Total: " + total));

            JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            statusPanel.add(new JLabel("Status: "));

            StatusComboBox statusCombo = new StatusComboBox(status);
            statusPanel.add(statusCombo);

            JButton updateStatusBtn = new JButton("Update Status");
            updateStatusBtn.setBackground(new Color(255, 69, 0));
            updateStatusBtn.setForeground(Color.WHITE);
            updateStatusBtn.setFocusPainted(false);
            updateStatusBtn.addActionListener(e -> {
                String newStatus = (String) statusCombo.getSelectedItem();
                updateOrderStatus(orderId, newStatus);
                JOptionPane.showMessageDialog(dialog, "Order status updated to " + newStatus, "Success", JOptionPane.INFORMATION_MESSAGE);
            });
            statusPanel.add(updateStatusBtn);

            headerPanel.add(statusPanel);

            String[] columns = {"Product", "Price", "Quantity", "Subtotal"};
            DefaultTableModel itemsModel = new DefaultTableModel(columns, 0);

            Object[][] items;
            switch (orderId) {
                case 1001:
                    items = new Object[][]{{"Sports Shoes", "Rp 299.000", 1, "Rp 299.000"}, {"Formal Shirt", "Rp 199.000", 1, "Rp 199.000"}, {"Sunglasses", "Rp 100.000", 1, "Rp 100.000"}};
                    break;
                case 1002:
                    items = new Object[][]{{"Jeans", "Rp 245.000", 1, "Rp 245.000"}};
                    break;
                case 1003:
                    items = new Object[][]{{"Watch", "Rp 450.000", 1, "Rp 450.000"}};
                    break;
                case 1004:
                    items = new Object[][]{{"Backpack", "Rp 189.000", 1, "Rp 189.000"}};
                    break;
                case 1005:
                    items = new Object[][]{{"Sports Shoes", "Rp 299.000", 1, "Rp 299.000"}, {"Sunglasses", "Rp 159.000", 1, "Rp 159.000"}};
                    break;
                default:
                    items = new Object[][]{{"Product", "Rp 0", 0, "Rp 0"}};
            }

            for (Object[] item : items) {
                itemsModel.addRow(item);
            }

            JTable itemsTable = new JTable(itemsModel);
            itemsTable.setRowHeight(30);
            itemsTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
            itemsTable.getTableHeader().setBackground(new Color(255, 99, 71));
            itemsTable.getTableHeader().setForeground(Color.WHITE);

            JScrollPane scrollPane = new JScrollPane(itemsTable);

            JPanel addressPanel = new JPanel(new BorderLayout());
            addressPanel.setBorder(new EmptyBorder(10, 15, 10, 15));
            JLabel addressLabel = new JLabel("Shipping Address:");
            addressLabel.setFont(new Font("Arial", Font.BOLD, 14));
            JTextArea addressArea = new JTextArea(3, 20);
            addressArea.setText("123 Example St.\nExample District\nCity " + (orderId % 2 == 0 ? "Jakarta" : "Bandung") + "\nPostal Code " + (10000 + orderId));
            addressArea.setEditable(false);
            addressArea.setBackground(Color.WHITE);
            addressArea.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            addressPanel.add(addressLabel, BorderLayout.NORTH);
            addressPanel.add(addressArea, BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton btnPrint = new JButton("Print Invoice");
            btnPrint.setBackground(new Color(255, 99, 71));
            btnPrint.setForeground(Color.WHITE);
            btnPrint.setFocusPainted(false);
            btnPrint.addActionListener(e -> JOptionPane.showMessageDialog(dialog, "Print invoice feature under development", "Info", JOptionPane.INFORMATION_MESSAGE));

            JButton btnClose = new JButton("Close");
            btnClose.setBackground(new Color(108, 117, 125));
            btnClose.setForeground(Color.WHITE);
            btnClose.setFocusPainted(false);
            btnClose.addActionListener(e -> dialog.dispose());

            buttonPanel.add(btnPrint);
            buttonPanel.add(btnClose);

            JPanel mainContentPanel = new JPanel(new BorderLayout());
            mainContentPanel.add(scrollPane, BorderLayout.CENTER);
            mainContentPanel.add(addressPanel, BorderLayout.SOUTH);

            dialog.add(headerPanel, BorderLayout.NORTH);
            dialog.add(mainContentPanel, BorderLayout.CENTER);
            dialog.add(buttonPanel, BorderLayout.SOUTH);
            dialog.setVisible(true);
        }
    }

    public void deleteProduct(int productId) {
        int confirmation = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this product from the database?", "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirmation == JOptionPane.YES_OPTION) {
            try {
                ProductRepository.deleteProduct(productId);
                loadProductsForSupervisor();
                JOptionPane.showMessageDialog(this, "Product deleted successfully from database", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error deleting product: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    public void updateOrderStatus(int orderId, String newStatus) {
        for (int i = 0; i < orderTableModel.getRowCount(); i++) {
            if ((int) orderTableModel.getValueAt(i, 1) == orderId) {
                orderTableModel.setValueAt(newStatus, i, 5);
                break;
            }
        }
    }

    class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
            setBorderPainted(false);
            setFocusPainted(false);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (isSelected) {
                setBackground(table.getSelectionBackground());
            } else {
                setBackground(table.getBackground());
            }

            JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
            panel.setBackground(table.getBackground());

            if (table == productTable) {
                JButton editBtn = new JButton("Edit");
                editBtn.setBackground(new Color(255, 99, 71));
                editBtn.setForeground(Color.WHITE);
                editBtn.setFocusPainted(false);

                JButton deleteBtn = new JButton("Delete");
                deleteBtn.setBackground(new Color(220, 53, 69));
                deleteBtn.setForeground(Color.WHITE);
                deleteBtn.setFocusPainted(false);

                panel.add(editBtn);
                panel.add(deleteBtn);
                return panel;
            } else if (table == orderTable) {
                setText("View Details");
                setBackground(new Color(255, 99, 71));
                setForeground(Color.WHITE);
                return this;
            }

            return panel;
        }
    }

    class ButtonEditor extends DefaultCellEditor {
        protected JButton button;
        protected String buttonType;
        protected int currentId;
        protected SupervisorDashboardUI parent;

        public ButtonEditor(JCheckBox checkBox, SupervisorDashboardUI parent) {
            this(checkBox, parent, "edit-delete");
        }

        public ButtonEditor(JCheckBox checkBox, SupervisorDashboardUI parent, String buttonType) {
            super(checkBox);
            this.parent = parent;
            this.buttonType = buttonType;
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> fireEditingStopped());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            if (table == productTable) {
                currentId = (int) productTableModel.getValueAt(row, 0); 
                JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
                panel.setBackground(table.getBackground());

                JButton editBtn = new JButton("Edit");
                editBtn.setBackground(new Color(255, 99, 71));
                editBtn.setForeground(Color.WHITE);
                editBtn.setFocusPainted(false);
                editBtn.addActionListener(e -> {
                    parent.showProductDialog(currentId);
                });

                JButton deleteBtn = new JButton("Delete");
                deleteBtn.setBackground(new Color(220, 53, 69));
                deleteBtn.setForeground(Color.WHITE);
                deleteBtn.setFocusPainted(false);
                deleteBtn.addActionListener(e -> {
                    parent.deleteProduct(currentId);
                });

                panel.add(editBtn);
                panel.add(deleteBtn);
                return panel;
            } else if (table == orderTable) {
                currentId = (int) table.getValueAt(row, 1);
                button.setText("View Details");
                button.setBackground(new Color(255, 99, 71));
                button.setForeground(Color.WHITE);
                button.addActionListener(e -> parent.showOrderDetailsWithStatusUpdate(currentId));
                return button;
            }

            return new JPanel();
        }

        @Override
        public Object getCellEditorValue() {
            return "";
        }

        @Override
        public boolean stopCellEditing() {
            return super.stopCellEditing();
        }
    }

    class StatusComboBox extends JComboBox<String> {
        public StatusComboBox(String currentStatus) {
            super(new String[]{"Pending", "Processing", "Shipped", "Completed", "Cancelled"});
            setSelectedItem(currentStatus);
            setBackground(Color.WHITE);

            setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (c instanceof JLabel && value != null) {
                        JLabel label = (JLabel) c;
                        String status = value.toString();
                        switch (status) {
                            case "Completed":
                                label.setForeground(new Color(40, 167, 69));
                                break;
                            case "Shipped":
                                label.setForeground(new Color(0, 123, 255));
                                break;
                            case "Processing":
                                label.setForeground(new Color(255, 193, 7));
                                break;
                            case "Cancelled":
                                label.setForeground(new Color(220, 53, 69));
                                break;
                            default:
                                label.setForeground(new Color(108, 117, 125));
                        }
                    }
                    return c;
                }
            });
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SupervisorDashboardUI().setVisible(true));
    }
}