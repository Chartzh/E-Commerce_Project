package e.commerce;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.sql.*;

public class SupervisorDashboardUI extends JFrame {
    private JPanel mainPanel;
    private CardLayout cardLayout;
    private DefaultTableModel productTableModel;
    private JTable productTable;
    private DefaultTableModel orderTableModel;
    private JTable orderTable;
    private JButton[] navButtons;
    private User currentUser;

    private JComboBox<String> orderStatusFilterCombo;

    private static final Color ORANGE_PRIMARY = new Color(255, 102, 0);
    private static final Color ORANGE_LIGHT = new Color(255, 153, 51);
    private static final Color WHITE = Color.WHITE;
    private static final Color GRAY_LIGHT = new Color(248, 248, 248);
    private static final Color GRAY_BORDER = new Color(230, 230, 230);
    private static final Color TEXT_DARK = new Color(51, 51, 51);
    private static final Color SUBTEXT_GRAY = new Color(102, 102, 102);
    private static final Color SUCCESS_GREEN = new Color(40, 167, 69);
    private static final Color CANCEL_RED = new Color(220, 53, 69);
    private static final Color PROCESSING_BLUE = new Color(0, 123, 255);
    private static final Color PENDING_YELLOW = new Color(255, 193, 7);
    private static final Color GRAY_TEXT_COLOR = new Color(120, 120, 120);
    private static final Color BLUE_PRIMARY = new Color(74, 110, 255);
    private static final Color SELLER_TEXT_COLOR = new Color(70, 130, 180);

    public SupervisorDashboardUI() {
        currentUser = Authentication.getCurrentUser();
        if (currentUser == null || (!currentUser.getRole().equals("supervisor") && !currentUser.getRole().equals("admin"))) {
            LoginUI loginUI = new LoginUI();
            loginUI.setVisible(true);
            dispose();
            return;
        }

        setTitle("Quantra - Dashboard " + currentUser.getRole());
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
        
        mainPanel.add(productPanel, "Produk");
        mainPanel.add(ordersPanel, "Pesanan");
        mainPanel.add(profilePanel, "Profil");

        JPanel contentWrapper = new JPanel(new BorderLayout());
        contentWrapper.add(headerPanel, BorderLayout.NORTH);
        contentWrapper.add(mainPanel, BorderLayout.CENTER);

        add(sidebar, BorderLayout.WEST);
        add(contentWrapper, BorderLayout.CENTER);

        cardLayout.show(mainPanel, "Produk");
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(245, 245, 245));
        sidebar.setPreferredSize(new Dimension(200, 0));
        sidebar.setBorder(new EmptyBorder(20, 10, 20, 10));

        JLabel lblLogo = new JLabel("Quantra");
        lblLogo.setFont(new Font("Arial", Font.BOLD, 20));
        lblLogo.setForeground(ORANGE_PRIMARY);
        lblLogo.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(lblLogo);
        sidebar.add(Box.createRigidArea(new Dimension(0, 30)));

        String[] navItems = {"Produk Saya", "Pesanan", "Profil", "Logout"};
        String[] panelNames = {"Produk", "Pesanan", "Profil", "LogoutAction"};
        navButtons = new JButton[navItems.length];

        for (int i = 0; i < navItems.length; i++) {
            JButton navButton = new JButton(navItems[i]);
            navButton.setFont(new Font("Arial", Font.PLAIN, 14));
            navButton.setForeground(TEXT_DARK);
            navButton.setBackground(GRAY_LIGHT);
            navButton.setBorderPainted(false);
            navButton.setFocusPainted(false);
            navButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            navButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            navButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

            final String panelName = panelNames[i];
            final int index = i;

            if (navItems[i].equals("Logout")) {
                navButton.setBackground(CANCEL_RED);
                navButton.setForeground(WHITE);
                navButton.addActionListener(e -> {
                    int response = JOptionPane.showConfirmDialog(SupervisorDashboardUI.this,
                                    "Apakah Anda yakin ingin logout?",
                                    "Konfirmasi Logout",
                                    JOptionPane.YES_NO_OPTION);
                    if (response == JOptionPane.YES_OPTION) {
                        Authentication.logout();
                        LoginUI loginUI = new LoginUI();
                        loginUI.setVisible(true);
                        dispose();
                    }
                });
            } else {
                navButton.addActionListener(e -> {
                    for (JButton btn : navButtons) {
                        if (!btn.getText().equals("Logout")) {
                            btn.setBackground(GRAY_LIGHT);
                            btn.setForeground(TEXT_DARK);
                        }
                    }
                    navButtons[index].setBackground(ORANGE_PRIMARY);
                    navButtons[index].setForeground(WHITE);

                    cardLayout.show(mainPanel, panelName);
                    if (panelName.equals("Produk")) {
                        loadProductsForSupervisor();
                    } else if (panelName.equals("Pesanan")) {
                        loadOrdersForSeller();
                    }
                });

                if (navItems[i].equals("Produk Saya")) {
                    navButton.setBackground(ORANGE_PRIMARY);
                    navButton.setForeground(WHITE);
                }
            }

            navButtons[i] = navButton;
            sidebar.add(navButton);
            sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        sidebar.add(Box.createVerticalGlue());

        return sidebar;
    }

    private JPanel createHeaderPanel(User currentUser) {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setPreferredSize(new Dimension(0, 60));
        headerPanel.setBorder(new EmptyBorder(10, 20, 10, 20));

        JTextField searchField = new JTextField("Cari produk...");
        searchField.setForeground(GRAY_TEXT_COLOR);
        searchField.setPreferredSize(new Dimension(300, 30));
        searchField.setBorder(new LineBorder(GRAY_BORDER, 1, true));
        searchField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (searchField.getText().equals("Cari produk...")) {
                    searchField.setText("");
                    searchField.setForeground(TEXT_DARK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (searchField.getText().isEmpty()) {
                    searchField.setText("Cari produk...");
                    searchField.setForeground(GRAY_TEXT_COLOR);
                }
            }
        });
        headerPanel.add(searchField, BorderLayout.WEST);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.setBackground(Color.WHITE);

        JLabel lblUser = new JLabel(currentUser.getUsername());
        lblUser.setFont(new Font("Arial", Font.PLAIN, 14));
        lblUser.setForeground(TEXT_DARK);

        rightPanel.add(lblUser);
        headerPanel.add(rightPanel, BorderLayout.EAST);

        return headerPanel;
    }

    private JPanel createProductManagementPanel() {
        JPanel productPanel = new JPanel(new BorderLayout());
        productPanel.setBackground(Color.WHITE);
        productPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filterPanel.setBackground(Color.WHITE);

        JComboBox<String> categoryCombo = new JComboBox<>(new String[]{"Semua Kategori", "Laptop", "Headphone", "Speaker", "Mouse", "Keyboard", "Smartwatch", "SSD", "USB-C Hub", "Webcam", "Casing HP", "Dudukan Laptop", "Power Bank", "Pengisi Daya Nirkabel"});
        categoryCombo.setPreferredSize(new Dimension(150, 30));

        JComboBox<String> statusCombo = new JComboBox<>(new String[]{"Semua Status", "Aktif", "Tidak Aktif"});
        statusCombo.setPreferredSize(new Dimension(120, 30));

        JTextField priceRangeField = new JTextField("Min - Maks");
        priceRangeField.setPreferredSize(new Dimension(100, 30));
        priceRangeField.setForeground(GRAY_TEXT_COLOR);
        priceRangeField.setBorder(new LineBorder(GRAY_BORDER, 1, true));

        JButton addProductButton = new JButton("Tambah Produk Baru");
        addProductButton.setBackground(ORANGE_PRIMARY);
        addProductButton.setForeground(WHITE);
        addProductButton.setBorderPainted(false);
        addProductButton.setFocusPainted(false);
        addProductButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addProductButton.addActionListener(e -> showProductDialog(null));

        filterPanel.add(new JLabel("Kategori:"));
        filterPanel.add(categoryCombo);
        filterPanel.add(new JLabel("Status:"));
        filterPanel.add(statusCombo);
        filterPanel.add(new JLabel("Harga:"));
        filterPanel.add(priceRangeField);
        filterPanel.add(Box.createHorizontalGlue());
        filterPanel.add(addProductButton);

        String[] columns = {"ID", "Nama Produk", "Harga", "Stok", "Merek", "Kondisi", "Aksi"};
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
        productTable.getTableHeader().setBackground(ORANGE_LIGHT);
        productTable.getTableHeader().setForeground(WHITE);

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
        JLabel pageLabel = new JLabel("1 dari 1");
        for (JButton btn : new JButton[]{prevButton, nextButton}) {
            btn.setBackground(WHITE);
            btn.setForeground(TEXT_DARK);
            btn.setBorder(new LineBorder(GRAY_BORDER, 1));
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

    public void loadProductsForSupervisor() {
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
        String title = (productId == null) ? "Tambah Produk Baru" : "Edit Produk";
        JDialog dialog = new JDialog(this, title, true);
        dialog.setSize(450, 600);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel lblName = new JLabel("Nama Produk:");
        JTextField txtName = new JTextField(20);
        txtName.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel lblPrice = new JLabel("Harga:");
        JTextField txtPrice = new JTextField(20);
        txtPrice.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel lblOriginalPrice = new JLabel("Harga Asli (opsional):");
        JTextField txtOriginalPrice = new JTextField(20);
        txtOriginalPrice.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel lblStock = new JLabel("Stok:");
        JTextField txtStock = new JTextField(20);
        txtStock.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel lblCondition = new JLabel("Kondisi:");
        JComboBox<String> comboCondition = new JComboBox<>(new String[]{"Baru", "Bekas"});
        comboCondition.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel lblMinOrder = new JLabel("Pesanan Minimum:");
        JTextField txtMinOrder = new JTextField(20);
        txtMinOrder.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel lblBrand = new JLabel("Merek:");
        JTextField txtBrand = new JTextField(20);
        txtBrand.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JLabel lblDescription = new JLabel("Deskripsi:");
        JTextArea txtDescription = new JTextArea(5, 20);
        txtDescription.setLineWrap(true);
        txtDescription.setWrapStyleWord(true);
        JScrollPane descScrollPane = new JScrollPane(txtDescription);
        descScrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        JLabel lblImage = new JLabel("Gambar Produk:");
        JPanel imageControlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField txtImagePath = new JTextField(20);
        txtImagePath.setEditable(false);
        JButton btnBrowseImage = new JButton("Jelajahi");
        JButton btnManageImages = new JButton("Kelola Gambar");

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
                JOptionPane.showMessageDialog(dialog, "Produk dengan ID " + productId + " tidak ditemukan.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else { // Mode Add New Product
            btnManageImages.setVisible(false);
        }


        btnBrowseImage.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setMultiSelectionEnabled(true);
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("File Gambar", "jpg", "jpeg", "png", "gif"));
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
                        JOptionPane.showMessageDialog(dialog, "Error membaca file gambar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
        JButton btnCancel = new JButton("Batal");
        JButton btnSave = new JButton("Simpan");

        btnSave.setBackground(ORANGE_PRIMARY);
        btnSave.setForeground(WHITE);
        btnSave.setFocusPainted(false);

        btnCancel.setBackground(new Color(108, 117, 125));
        btnCancel.setForeground(WHITE);
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
                JOptionPane.showMessageDialog(dialog, "Semua kolom kecuali Harga Asli harus diisi!", "Error Validasi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                double price = Double.parseDouble(priceText);
                double originalPrice = originalPriceText.isEmpty() ? 0.0 : Double.parseDouble(originalPriceText);
                int stock = Integer.parseInt(stockText);

                if (price <= 0 || stock < 0) {
                    JOptionPane.showMessageDialog(dialog, "Harga harus lebih besar dari 0 dan stok tidak boleh negatif!", "Error Validasi", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (productId == null && imagesToUpload.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "Mohon pilih setidaknya satu gambar untuk produk baru!", "Error Validasi", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                try {
                    if (productId == null) { // Add New Product
                        Integer userIdForProduct = currentUser.getId();
                        if (userIdForProduct == null) {
                            JOptionPane.showMessageDialog(dialog, "ID Pengguna tidak ditemukan. Mohon login sebagai penjual.", "Error", JOptionPane.ERROR_MESSAGE);
                            return;
                        }

                        int newProductId = ProductRepository.saveProduct(name, description, price, originalPrice, stock, condition, minOrder, brand, userIdForProduct);

                        if (newProductId != -1 && !imagesToUpload.isEmpty()) {
                            for (int i = 0; i < imagesToUpload.size(); i++) {
                                ProductRepository.saveProductImage(newProductId, imagesToUpload.get(i), imageExtensions.get(i), i == 0, i + 1);
                            }
                        }
                        JOptionPane.showMessageDialog(dialog, "Produk berhasil ditambahkan!", "Sukses", JOptionPane.INFORMATION_MESSAGE);
                    } else { // Edit Product
                        boolean updateProductSuccess = ProductRepository.updateProduct(productId, name, description, price, originalPrice, stock, condition, minOrder, brand);

                        if (updateProductSuccess) {
                            JOptionPane.showMessageDialog(dialog, "Produk berhasil diperbarui!", "Sukses", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(dialog, "Gagal memperbarui produk.", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                    loadProductsForSupervisor();
                    dialog.dispose();

                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(dialog, "Error Database: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Harga, Harga Asli, dan Stok harus berupa angka!", "Error Validasi", JOptionPane.ERROR_MESSAGE);
            }
        });

        buttonPanel.add(btnCancel);
        buttonPanel.add(btnSave);

        panel.add(lblName); panel.add(Box.createRigidArea(new Dimension(0, 5))); panel.add(txtName);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(lblPrice); panel.add(Box.createRigidArea(new Dimension(0, 5))); panel.add(txtPrice);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(lblOriginalPrice); panel.add(Box.createRigidArea(new Dimension(0, 5))); panel.add(txtOriginalPrice);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(lblStock); panel.add(Box.createRigidArea(new Dimension(0, 5))); panel.add(txtStock);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(lblCondition); panel.add(Box.createRigidArea(new Dimension(0, 5))); panel.add(comboCondition);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(lblMinOrder); panel.add(Box.createRigidArea(new Dimension(0, 5))); panel.add(txtMinOrder);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(lblBrand); panel.add(Box.createRigidArea(new Dimension(0, 5))); panel.add(txtBrand);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(lblDescription); panel.add(Box.createRigidArea(new Dimension(0, 5))); panel.add(descScrollPane);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(lblImage); panel.add(Box.createRigidArea(new Dimension(0, 5))); panel.add(imageControlPanel);


        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    public void deleteProduct(int productId) {
        int confirmation = JOptionPane.showConfirmDialog(this, "Apakah Anda yakin ingin menghapus produk ini dari database?", "Konfirmasi Hapus", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirmation == JOptionPane.YES_OPTION) {
            try {
                ProductRepository.deleteProduct(productId);
                loadProductsForSupervisor();
                JOptionPane.showMessageDialog(this, "Produk berhasil dihapus dari database", "Sukses", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error menghapus produk: " + e.getMessage(), "Error Database", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    private JPanel createOrdersPanel() {
        JPanel ordersPanel = new JPanel(new BorderLayout());
        ordersPanel.setBackground(Color.WHITE);
        ordersPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filterPanel.setBackground(Color.WHITE);

        orderStatusFilterCombo = new JComboBox<>(new String[]{"Semua Status", "Menunggu Pembayaran", "Dikemas", "Dikirim", "Selesai", "Dibatalkan"});
        orderStatusFilterCombo.setPreferredSize(new Dimension(180, 30));
        orderStatusFilterCombo.addActionListener(e -> loadOrdersForSeller());

        filterPanel.add(new JLabel("Status:"));
        filterPanel.add(orderStatusFilterCombo);

        String[] columns = {"ID Pesanan", "Pelanggan", "Tanggal Pesanan", "Total Harga", "Status", "Aksi"};
        orderTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5;
            }
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? Integer.class : String.class;
            }
        };

        orderTable = new JTable(orderTableModel);
        orderTable.setRowHeight(60);
        orderTable.setFont(new Font("Arial", Font.PLAIN, 14));
        orderTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        orderTable.getTableHeader().setBackground(ORANGE_LIGHT);
        orderTable.getTableHeader().setForeground(WHITE);
        orderTable.setSelectionBackground(GRAY_LIGHT);

        orderTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        orderTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        orderTable.getColumnModel().getColumn(2).setPreferredWidth(120);
        orderTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        orderTable.getColumnModel().getColumn(4).setPreferredWidth(120);
        orderTable.getColumnModel().getColumn(5).setPreferredWidth(150);

        orderTable.getColumnModel().getColumn(4).setCellRenderer(new StatusTableCellRenderer());
        orderTable.getColumnModel().getColumn(5).setCellRenderer(new OrderActionButtonRenderer());
        orderTable.getColumnModel().getColumn(5).setCellEditor(new OrderActionButtonEditor(new JCheckBox(), this));

        loadOrdersForSeller();

        JScrollPane scrollPane = new JScrollPane(orderTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        ordersPanel.add(filterPanel, BorderLayout.NORTH);
        ordersPanel.add(scrollPane, BorderLayout.CENTER);

        return ordersPanel;
    }

    public void loadOrdersForSeller() {
        orderTableModel.setRowCount(0);
        String selectedStatus = (String) orderStatusFilterCombo.getSelectedItem();
        String dbStatusFilter = mapDisplayStatusToDbForSupervisor(selectedStatus);

        try {
            List<ProductRepository.Order> orders = ProductRepository.getOrdersForSeller(currentUser.getId());
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("in", "ID"));
            currencyFormat.setMinimumFractionDigits(0);

            for (ProductRepository.Order order : orders) {
                String orderStatusDisplay = mapDbStatusToDisplayForSupervisor(order.getOrderStatus());

                if (!dbStatusFilter.equals("Semua Status") && !order.getOrderStatus().equalsIgnoreCase(dbStatusFilter)) {
                    continue;
                }

                String formattedTotal = currencyFormat.format(order.getTotalAmount());
                String formattedDate = order.getOrderDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

                String customerName = getCustomerUsername(order.getUserId());
                if (customerName == null) customerName = "Pelanggan Tidak Dikenal";


                orderTableModel.addRow(new Object[]{
                    order.getId(),
                    customerName,
                    formattedDate,
                    formattedTotal,
                    orderStatusDisplay,
                    order.getOrderStatus()
                });
            }
        } catch (SQLException e) {
            System.err.println("Error memuat pesanan untuk seller: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Gagal memuat pesanan: " + e.getMessage(), "Error Database", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private String getCustomerUsername(int userId) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        String username = null;
        try {
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT username FROM users WHERE id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                username = rs.getString("username");
            }
        } finally {
            DatabaseConnection.closeConnection(conn, stmt, rs);
        }
        return username;
    }

    private String mapDisplayStatusToDbForSupervisor(String displayStatus) {
        return switch (displayStatus) {
            case "Menunggu Pembayaran" -> "Pending Payment";
            case "Dikemas" -> "Processing";
            case "Dikirim" -> "Shipped";
            case "Selesai" -> "Delivered";
            case "Dibatalkan" -> "Cancelled";
            case "Semua Status" -> "Semua Status";
            default -> displayStatus;
        };
    }

    private String mapDbStatusToDisplayForSupervisor(String dbStatus) {
        return switch (dbStatus) {
            case "Pending Payment" -> "Menunggu Pembayaran";
            case "Processing" -> "Dikemas";
            case "Shipped" -> "Dikirim";
            case "Delivered" -> "Selesai";
            case "Cancelled" -> "Dibatalkan";
            default -> dbStatus;
        };
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
                editBtn.setBackground(ORANGE_PRIMARY);
                editBtn.setForeground(WHITE);
                editBtn.setFocusPainted(false);

                JButton deleteBtn = new JButton("Hapus");
                deleteBtn.setBackground(CANCEL_RED);
                deleteBtn.setForeground(WHITE);
                deleteBtn.setFocusPainted(false);

                panel.add(editBtn);
                panel.add(deleteBtn);
                return panel;
            }
            return panel;
        }
    }

    class ButtonEditor extends DefaultCellEditor {
        protected JButton button;
        protected SupervisorDashboardUI parent;
        protected int currentProductId;

        public ButtonEditor(JCheckBox checkBox, SupervisorDashboardUI parent) {
            super(checkBox);
            this.parent = parent;
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> fireEditingStopped());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            if (table == productTable) {
                currentProductId = (int) productTableModel.getValueAt(row, 0);
                JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
                panel.setBackground(table.getBackground());

                JButton editBtn = new JButton("Edit");
                editBtn.setBackground(ORANGE_PRIMARY);
                editBtn.setForeground(WHITE);
                editBtn.setFocusPainted(false);
                editBtn.addActionListener(e -> {
                    parent.showProductDialog(currentProductId);
                });

                JButton deleteBtn = new JButton("Hapus");
                deleteBtn.setBackground(CANCEL_RED);
                deleteBtn.setForeground(WHITE);
                deleteBtn.setFocusPainted(false);
                deleteBtn.addActionListener(e -> {
                    parent.deleteProduct(currentProductId);
                });

                panel.add(editBtn);
                panel.add(deleteBtn);
                return panel;
            }
            return new JPanel();
        }

        @Override
        public Object getCellEditorValue() {
            return "";
        }
    }

    class StatusTableCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setFont(new Font("Arial", Font.BOLD, 12));
            label.setOpaque(true);

            if (value != null) {
                String status = value.toString();
                switch (status) {
                    case "Selesai":
                        label.setBackground(SUCCESS_GREEN);
                        label.setForeground(WHITE);
                        break;
                    case "Dikirim":
                        label.setBackground(PROCESSING_BLUE);
                        label.setForeground(WHITE);
                        break;
                    case "Dikemas":
                        label.setBackground(PENDING_YELLOW);
                        label.setForeground(TEXT_DARK);
                        break;
                    case "Dibatalkan":
                        label.setBackground(CANCEL_RED);
                        label.setForeground(WHITE);
                        break;
                    case "Menunggu Pembayaran":
                        label.setBackground(GRAY_BORDER);
                        label.setForeground(TEXT_DARK);
                        break;
                    default:
                        label.setBackground(GRAY_LIGHT);
                        label.setForeground(TEXT_DARK);
                }
            }
            label.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            return label;
        }
    }

    class OrderActionButtonRenderer extends JPanel implements TableCellRenderer {
        private JButton viewDetailsBtn;
        // private JButton updateStatusBtn; // Dihilangkan dari renderer karena editor akan menanganinya
        private JComboBox<String> statusDropdown; // Menambahkan dropdown di renderer

        public OrderActionButtonRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));
            setOpaque(true);

            viewDetailsBtn = new JButton("Lihat Detail");
            viewDetailsBtn.setBackground(BLUE_PRIMARY);
            viewDetailsBtn.setForeground(WHITE);
            viewDetailsBtn.setFont(new Font("Arial", Font.PLAIN, 12));
            viewDetailsBtn.setFocusPainted(false);

            // updateStatusBtn = new JButton("Update Status"); // Dihilangkan
            // updateStatusBtn.setBackground(ORANGE_PRIMARY);
            // updateStatusBtn.setForeground(WHITE);
            // updateStatusBtn.setFont(new Font("Arial", Font.PLAIN, 12));
            // updateStatusBtn.setFocusPainted(false);

            // Inisialisasi dropdown di renderer (tidak interaktif)
            statusDropdown = new StatusComboBox("");
            statusDropdown.setFont(new Font("Arial", Font.PLAIN, 12));
            statusDropdown.setBackground(Color.WHITE);
            statusDropdown.setForeground(TEXT_DARK); // Default text color for dropdown
            statusDropdown.setEnabled(false); // Selalu nonaktif di renderer
            statusDropdown.setFocusable(false); // Tidak bisa fokus


            add(viewDetailsBtn);
            add(statusDropdown);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (isSelected) {
                setBackground(table.getSelectionBackground());
            } else {
                setBackground(table.getBackground());
            }
            String orderStatusDb = (String) value;

            viewDetailsBtn.setVisible(true);
            statusDropdown.setVisible(true);

            statusDropdown.setSelectedItem(mapDbStatusToDisplayForSupervisor(orderStatusDb));

            // Nonaktifkan dropdown jika status sudah final
            if (orderStatusDb.equals("Delivered") || orderStatusDb.equals("Cancelled")) {
                statusDropdown.setEnabled(false);
            } else {
                statusDropdown.setEnabled(true);
            }

            return this;
        }
    }

    class OrderActionButtonEditor extends DefaultCellEditor {
        protected JButton button;
        protected SupervisorDashboardUI parent;
        protected int currentOrderId;
        private JComboBox<String> statusChooser;
        private JTable currentTable; // Field untuk menyimpan referensi tabel saat ini

        public OrderActionButtonEditor(JCheckBox checkBox, SupervisorDashboardUI parent) {
            super(checkBox);
            this.parent = parent;
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> fireEditingStopped());

            statusChooser = new StatusComboBox("");
            statusChooser.setFont(new Font("Arial", Font.PLAIN, 12));
            statusChooser.setBackground(Color.WHITE);
            statusChooser.setCursor(new Cursor(Cursor.HAND_CURSOR));

            statusChooser.addActionListener(e -> {
                // Gunakan currentTable dan editingRow yang telah disimpan
                String newStatusDisplay = (String) statusChooser.getSelectedItem();
                // Pastikan currentTable tidak null dan editingRow valid
                if (currentTable == null || editingRow == -1) {
                    JOptionPane.showMessageDialog(parent, "Terjadi kesalahan: Baris tabel tidak teridentifikasi.", "Error Internal", JOptionPane.ERROR_MESSAGE);
                    fireEditingStopped();
                    return;
                }

                String currentStatusDb = (String) currentTable.getValueAt(editingRow, 5); // Menggunakan editingRow
                String newStatusDb = mapDisplayStatusToDbForSupervisor(newStatusDisplay);

                if (newStatusDb != null && !newStatusDb.equals(currentStatusDb)) {
                    // Validasi khusus: hanya bisa mengubah dari "Dikemas" atau "Menunggu Pembayaran" atau "Dikirim"
                    boolean isValidChange = false;
                    if (currentStatusDb.equals("Processing")) { // Dari "Dikemas"
                        if (newStatusDb.equals("Shipped") || newStatusDb.equals("Cancelled")) {
                            isValidChange = true;
                        }
                    } else if (currentStatusDb.equals("Pending Payment")) { // Dari "Menunggu Pembayaran"
                        if (newStatusDb.equals("Processing") || newStatusDb.equals("Cancelled")) {
                            isValidChange = true;
                        }
                    } else if (currentStatusDb.equals("Shipped")) { // Dari "Dikirim"
                        if (newStatusDb.equals("Delivered")) {
                            isValidChange = true;
                        }
                    } else if (currentStatusDb.equals("Delivered") || currentStatusDb.equals("Cancelled")) {
                        // Tidak bisa diubah jika sudah selesai atau dibatalkan
                        isValidChange = false;
                    } else {
                        // Status lain yang tidak diharapkan atau tidak diizinkan diubah oleh supervisor
                        isValidChange = false;
                    }

                    if (!isValidChange) {
                         JOptionPane.showMessageDialog(parent, "Status tidak bisa diubah dari '" + mapDbStatusToDisplayForSupervisor(currentStatusDb) + "' ke '" + newStatusDisplay + "'.", "Aksi Tidak Diizinkan", JOptionPane.WARNING_MESSAGE);
                         // Reset dropdown ke status sebelumnya
                         statusChooser.setSelectedItem(mapDbStatusToDisplayForSupervisor(currentStatusDb));
                         return;
                    }


                    int confirm = JOptionPane.showConfirmDialog(parent,
                            "Apakah Anda yakin ingin mengubah status pesanan #" + currentOrderId + " menjadi " + newStatusDisplay + "?",
                            "Konfirmasi Update Status", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        try {
                            ProductRepository.updateOrderStatus(currentOrderId, newStatusDb);
                            parent.loadOrdersForSeller();
                            JOptionPane.showMessageDialog(parent, "Status pesanan berhasil diperbarui.", "Sukses", JOptionPane.INFORMATION_MESSAGE);
                        } catch (SQLException ex) {
                            JOptionPane.showMessageDialog(parent, "Error saat memperbarui status: " + ex.getMessage(), "Error Database", JOptionPane.ERROR_MESSAGE);
                            ex.printStackTrace();
                        }
                    }
                }
                fireEditingStopped();
            });
        }

        // Field untuk menyimpan baris yang sedang diedit
        private int editingRow;

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.currentTable = table; // Simpan referensi tabel
            this.editingRow = row;     // Simpan nomor baris

            currentOrderId = (int) table.getValueAt(row, 0);
            String currentStatusDb = (String) table.getValueAt(row, 5); // Menggunakan 'row' yang benar

            JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
            panel.setBackground(table.getBackground());

            JButton viewDetailsBtn = new JButton("Lihat Detail");
            viewDetailsBtn.setBackground(BLUE_PRIMARY);
            viewDetailsBtn.setForeground(WHITE);
            viewDetailsBtn.setFont(new Font("Arial", Font.PLAIN, 12));
            viewDetailsBtn.setFocusPainted(false);
            viewDetailsBtn.addActionListener(e -> {
                showOrderDetailsForSupervisor(currentOrderId);
            });
            panel.add(viewDetailsBtn);

            // Tentukan status yang tersedia untuk dropdown berdasarkan status saat ini
            statusChooser.removeAllItems(); // Bersihkan item lama
            String[] availableStatusesForDropdown;

            if (currentStatusDb.equals("Delivered") || currentStatusDb.equals("Cancelled")) {
                statusChooser.setEnabled(false);
                availableStatusesForDropdown = new String[]{mapDbStatusToDisplayForSupervisor(currentStatusDb)};
            } else if (currentStatusDb.equals("Processing")) { // Jika status "Dikemas"
                statusChooser.setEnabled(true);
                availableStatusesForDropdown = new String[]{"Dikemas", "Dikirim", "Dibatalkan"};
            } else if (currentStatusDb.equals("Pending Payment")) { // Jika status "Menunggu Pembayaran"
                statusChooser.setEnabled(true);
                availableStatusesForDropdown = new String[]{"Menunggu Pembayaran", "Dikemas", "Dibatalkan"};
            } else if (currentStatusDb.equals("Shipped")) { // Jika status "Dikirim"
                statusChooser.setEnabled(true);
                availableStatusesForDropdown = new String[]{"Dikirim", "Selesai"};
            } else { // Status lain atau default
                statusChooser.setEnabled(true);
                availableStatusesForDropdown = new String[]{"Menunggu Pembayaran", "Dikemas", "Dikirim", "Selesai", "Dibatalkan"};
            }

            for(String status : availableStatusesForDropdown) {
                statusChooser.addItem(status);
            }
            statusChooser.setSelectedItem(mapDbStatusToDisplayForSupervisor(currentStatusDb));

            panel.add(statusChooser);

            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            return "";
        }

        public void showOrderDetailsForSupervisor(int orderId) {
            try {
                ProductRepository.Order order = ProductRepository.getOrderById(orderId);

                if (order == null) {
                    JOptionPane.showMessageDialog(parent, "Detail pesanan tidak ditemukan.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                List<ProductRepository.OrderItem> sellerSpecificItems = ProductRepository.getSellerSpecificOrderItemsForOrder(orderId, currentUser.getId());
                if(sellerSpecificItems.isEmpty() && currentUser.getRole().equals("supervisor")) {
                    JOptionPane.showMessageDialog(parent, "Pesanan ini tidak mengandung produk yang Anda jual. Akses Ditolak.", "Akses Ditolak", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                JDialog detailDialog = new JDialog(parent, "Detail Pesanan #" + order.getOrderNumber(), true);
                detailDialog.setSize(700, 600);
                detailDialog.setLocationRelativeTo(parent);
                detailDialog.setLayout(new BorderLayout());

                OrderDetailUI orderDetailUIPanel = new OrderDetailUI(null, orderId);
                detailDialog.add(orderDetailUIPanel, BorderLayout.CENTER);

                JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                JButton closeButton = new JButton("Tutup");
                closeButton.setBackground(new Color(108, 117, 125));
                closeButton.setForeground(Color.WHITE);
                closeButton.setFocusPainted(false);
                closeButton.addActionListener(e -> detailDialog.dispose());
                bottomPanel.add(closeButton);
                detailDialog.add(bottomPanel, BorderLayout.SOUTH);

                detailDialog.setVisible(true);

            } catch (SQLException e) {
                JOptionPane.showMessageDialog(parent, "Gagal memuat detail pesanan: " + e.getMessage(), "Error Database", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    class StatusComboBox extends JComboBox<String> {
        public StatusComboBox(String currentStatus) {
            super(); // Kosongkan super() karena item akan ditambahkan di editor
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
                            case "Selesai":
                                label.setForeground(SUCCESS_GREEN);
                                break;
                            case "Dikirim":
                                label.setForeground(PROCESSING_BLUE);
                                break;
                            case "Dikemas":
                                label.setForeground(PENDING_YELLOW);
                                break;
                            case "Dibatalkan":
                                label.setForeground(CANCEL_RED);
                                break;
                            case "Menunggu Pembayaran":
                                label.setForeground(GRAY_TEXT_COLOR);
                                break;
                            default:
                                label.setForeground(TEXT_DARK);
                        }
                    }
                    return c;
                }
            });
        }
    }

    private JPanel createAnalyticsPanel() {
        JPanel analyticsPanel = new JPanel(new BorderLayout());
        analyticsPanel.setBackground(Color.WHITE);
        analyticsPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Halaman Analitik (Under Development)", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(TEXT_DARK);
        analyticsPanel.add(titleLabel, BorderLayout.CENTER);

        return analyticsPanel;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new SupervisorDashboardUI().setVisible(true);
        });
    }
}