package e.commerce;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

public class SupervisorDashboardUI extends JFrame {
    private JPanel contentPanel;
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private JButton btnHamburger, btnBack;
    private JPopupMenu hamburgerMenu;
    private DefaultTableModel productTableModel;
    private JTable productTable;
    private DefaultTableModel orderTableModel;
    private JTable orderTable;
    
    public SupervisorDashboardUI() {
        User currentUser = Authentication.getCurrentUser();
        if (currentUser == null) {
            LoginUI loginUI = new LoginUI();
            loginUI.setVisible(true);
            dispose();
            return;
        }
        
        setTitle("E-Commerce App - Supervisor Dashboard");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Main layout
        setLayout(new BorderLayout());
        
        // Create main panel with CardLayout
        mainPanel = new JPanel();
        cardLayout = new CardLayout();
        mainPanel.setLayout(cardLayout);
        
        // Create header panel
        JPanel headerPanel = createHeaderPanel(currentUser);
        
        // Create product management panel
        JPanel productPanel = createProductManagementPanel();
        
        // Create profile panel
        ProfileUI profilePanel = new ProfileUI();
        
        // Create orders panel
        JPanel ordersPanel = createOrdersPanel();
        
        // Add panels to the card layout
        mainPanel.add(productPanel, "Products");
        mainPanel.add(profilePanel, "Profile");
        mainPanel.add(ordersPanel, "Orders");
        
        // Add components to frame
        add(headerPanel, BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);
        
        // Create hamburger menu
        createHamburgerMenu();
    }
    
    private JPanel createHeaderPanel(User currentUser) {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(33, 37, 41));
        headerPanel.setPreferredSize(new Dimension(getWidth(), 60));
        headerPanel.setBorder(new EmptyBorder(10, 20, 10, 20));
        
        // Logo
        JLabel lblLogo = new JLabel("Quantra - Supervisor");
        lblLogo.setFont(new Font("Arial", Font.BOLD, 24));
        lblLogo.setForeground(Color.WHITE);
        headerPanel.add(lblLogo, BorderLayout.WEST);
        
        // Welcome message
        JLabel lblWelcome = new JLabel("Selamat Datang, Supervisor " + currentUser.getUsername());
        lblWelcome.setFont(new Font("Arial", Font.PLAIN, 14));
        lblWelcome.setForeground(Color.WHITE);
        lblWelcome.setHorizontalAlignment(SwingConstants.CENTER);
        headerPanel.add(lblWelcome, BorderLayout.CENTER);
        
        // Hamburger button
        btnHamburger = new JButton("☰");
        btnHamburger.setFont(new Font("Arial", Font.BOLD, 20));
        btnHamburger.setForeground(Color.WHITE);
        btnHamburger.setBackground(new Color(33, 37, 41));
        btnHamburger.setBorderPainted(false);
        btnHamburger.setFocusPainted(false);
        btnHamburger.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Back button (initially not visible)
        btnBack = new JButton("< Kembali");
        btnBack.setFont(new Font("Arial", Font.BOLD, 14));
        btnBack.setForeground(Color.WHITE);
        btnBack.setBackground(new Color(33, 37, 41));
        btnBack.setBorderPainted(false);
        btnBack.setFocusPainted(false);
        btnBack.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnBack.setVisible(false);
        
        // Action listener for back button
        btnBack.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(mainPanel, "Products");
                btnHamburger.setVisible(true);
                btnBack.setVisible(false);
            }
        });
        
        // Panel for hamburger and back buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(new Color(33, 37, 41));
        buttonPanel.add(btnBack);
        buttonPanel.add(btnHamburger);
        
        headerPanel.add(buttonPanel, BorderLayout.EAST);
        
        return headerPanel;
    }
    
    private void createHamburgerMenu() {
        // Buat custom popup menu dengan style yang lebih baik
        hamburgerMenu = new JPopupMenu() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(52, 58, 64));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
            }
        };
        
        // Set properti menu
        hamburgerMenu.setBorder(new CompoundBorder(
            new LineBorder(new Color(40, 44, 52), 1, true),
            new EmptyBorder(5, 0, 5, 0)
        ));
        
        // Products menu item with icon
        JMenuItem menuProducts = createMenuItem("Kelola Produk", "\uf07a");  // Unicode for shopping cart icon
        menuProducts.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(mainPanel, "Products");
                btnHamburger.setVisible(true);
                btnBack.setVisible(false);
            }
        });
        
        // Orders menu item with icon
        JMenuItem menuOrders = createMenuItem("Lihat Pesanan", "\uf0ae");  // Unicode for tasks icon
        menuOrders.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(mainPanel, "Orders");
                btnHamburger.setVisible(true);
                btnBack.setVisible(false);
            }
        });
        
        // Profile menu item with icon
        JMenuItem menuProfile = createMenuItem("Profil", "\uf007");  // Unicode for user icon
        menuProfile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(mainPanel, "Profile");
                btnHamburger.setVisible(false);
                btnBack.setVisible(true);
            }
        });
        
        // Logout menu item with icon
        JMenuItem menuLogout = createMenuItem("Logout", "\uf08b");  // Unicode for sign-out icon
        menuLogout.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int response = JOptionPane.showConfirmDialog(SupervisorDashboardUI.this,
                        "Apakah Anda yakin ingin keluar?",
                        "Konfirmasi",
                        JOptionPane.YES_NO_OPTION);
                
                if (response == JOptionPane.YES_OPTION) {
                    Authentication.logout();
                    LoginUI loginUI = new LoginUI();
                    loginUI.setVisible(true);
                    dispose();
                }
            }
        });
        
        // Add menu items to popup menu
        hamburgerMenu.add(menuProducts);
        
        // Separator
        JSeparator separator1 = new JSeparator();
        separator1.setBackground(new Color(60, 65, 72));
        separator1.setForeground(new Color(70, 75, 82));
        hamburgerMenu.add(separator1);
        
        hamburgerMenu.add(menuOrders);
        
        // Separator
        JSeparator separator2 = new JSeparator();
        separator2.setBackground(new Color(60, 65, 72));
        separator2.setForeground(new Color(70, 75, 82));
        hamburgerMenu.add(separator2);
        
        hamburgerMenu.add(menuProfile);
        
        // Separator for logout
        JSeparator separator3 = new JSeparator();
        separator3.setBackground(new Color(60, 65, 72));
        separator3.setForeground(new Color(70, 75, 82));
        hamburgerMenu.add(separator3);
        
        hamburgerMenu.add(menuLogout);
        
        // Hamburger button action
        btnHamburger.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                hamburgerMenu.show(btnHamburger, -hamburgerMenu.getPreferredSize().width + btnHamburger.getWidth(), btnHamburger.getHeight());
            }
        });
    }
    
    private JMenuItem createMenuItem(String text, String iconCode) {
        // Buat panel untuk layout item menu
        JPanel itemPanel = new JPanel(new BorderLayout());
        itemPanel.setBackground(new Color(52, 58, 64));
        itemPanel.setBorder(new EmptyBorder(8, 15, 8, 30));
        
        // Icon (Catatan: dalam implementasi sebenarnya, gunakan font icon seperti FontAwesome)
        JLabel iconLabel = new JLabel(iconCode);
        iconLabel.setForeground(Color.WHITE);
        iconLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        iconLabel.setPreferredSize(new Dimension(20, 16));
        
        // Text
        JLabel textLabel = new JLabel(text);
        textLabel.setForeground(Color.WHITE);
        textLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        textLabel.setBorder(new EmptyBorder(0, 10, 0, 0));
        
        itemPanel.add(iconLabel, BorderLayout.WEST);
        itemPanel.add(textLabel, BorderLayout.CENTER);
        
        // Buat menu item custom
        JMenuItem menuItem = new JMenuItem("") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (getModel().isArmed()) {
                    g2d.setColor(new Color(75, 83, 91));  // Hover background
                } else {
                    g2d.setColor(new Color(52, 58, 64));  // Normal background
                }
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        
        menuItem.setLayout(new BorderLayout());
        menuItem.add(itemPanel);
        menuItem.setBorderPainted(false);
        menuItem.setBorder(null);
        menuItem.setPreferredSize(new Dimension(180, 36));
        
        return menuItem;
    }
    
    private JPanel createProductManagementPanel() {
        JPanel productPanel = new JPanel(new BorderLayout());
        productPanel.setBackground(new Color(245, 245, 245));
        
        // Title panel
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(new Color(245, 245, 245));
        titlePanel.setBorder(new EmptyBorder(20, 20, 10, 20));
        
        JLabel lblTitle = new JLabel("Manajemen Produk");
        lblTitle.setFont(new Font("Arial", Font.BOLD, 22));
        titlePanel.add(lblTitle, BorderLayout.WEST);
        
        // Create "Add Product" button
        JButton btnAddProduct = new JButton("Tambah Produk Baru");
        btnAddProduct.setFont(new Font("Arial", Font.PLAIN, 14));
        btnAddProduct.setBackground(new Color(40, 167, 69));
        btnAddProduct.setForeground(Color.WHITE);
        btnAddProduct.setFocusPainted(false);
        btnAddProduct.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAddProduct.setBorder(new EmptyBorder(8, 15, 8, 15));
        
        btnAddProduct.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showProductDialog(null); // null indicates adding a new product
            }
        });
        
        titlePanel.add(btnAddProduct, BorderLayout.EAST);
        
        // Table of products
        String[] columns = {"ID", "Nama Produk", "Harga", "Stok", "Aksi"};
        productTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 4; // Only action column is editable
            }
        };
        
        productTable = new JTable(productTableModel);
        productTable.setRowHeight(40);
        productTable.setFont(new Font("Arial", Font.PLAIN, 14));
        productTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        productTable.getTableHeader().setBackground(new Color(33, 37, 41));
        productTable.getTableHeader().setForeground(Color.WHITE);
        
        // Add some dummy products for demonstration
        addDummyProducts();
        
        // Create button column for Edit and Delete actions
        productTable.getColumnModel().getColumn(4).setCellRenderer(new ButtonRenderer());
        productTable.getColumnModel().getColumn(4).setCellEditor(new ButtonEditor(new JCheckBox(), this));
        
        // Set column widths
        productTable.getColumnModel().getColumn(0).setPreferredWidth(50);   // ID
        productTable.getColumnModel().getColumn(1).setPreferredWidth(200);  // Name
        productTable.getColumnModel().getColumn(2).setPreferredWidth(100);  // Price
        productTable.getColumnModel().getColumn(3).setPreferredWidth(80);   // Stock
        productTable.getColumnModel().getColumn(4).setPreferredWidth(150);  // Actions
        
        JScrollPane scrollPane = new JScrollPane(productTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        
        // Add components to product panel
        productPanel.add(titlePanel, BorderLayout.NORTH);
        productPanel.add(scrollPane, BorderLayout.CENTER);
        
        return productPanel;
    }
    
    private void addDummyProducts() {
        Object[][] dummyData = {
            {1, "Sepatu Sport", "Rp 299.000", 25, ""},
            {2, "Kemeja Formal", "Rp 199.000", 40, ""},
            {3, "Jam Tangan", "Rp 450.000", 15, ""},
            {4, "Celana Jeans", "Rp 245.000", 30, ""},
            {5, "Tas Ransel", "Rp 189.000", 20, ""},
            {6, "Kacamata Hitam", "Rp 159.000", 18, ""}
        };
        
        for (Object[] row : dummyData) {
            productTableModel.addRow(row);
        }
    }
    
    private JPanel createOrdersPanel() {
        JPanel ordersPanel = new JPanel(new BorderLayout());
        ordersPanel.setBackground(new Color(245, 245, 245));
        
        // Title panel
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(new Color(245, 245, 245));
        titlePanel.setBorder(new EmptyBorder(20, 20, 10, 20));
        
        JLabel lblTitle = new JLabel("Daftar Pesanan");
        lblTitle.setFont(new Font("Arial", Font.BOLD, 22));
        titlePanel.add(lblTitle, BorderLayout.WEST);
        
        // Table of orders
        String[] columns = {"ID", "Pembeli", "Tanggal", "Total", "Status", "Aksi"};
        orderTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5; // Only action column is editable
            }
        };
        
        orderTable = new JTable(orderTableModel);
        orderTable.setRowHeight(40);
        orderTable.setFont(new Font("Arial", Font.PLAIN, 14));
        orderTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        orderTable.getTableHeader().setBackground(new Color(33, 37, 41));
        orderTable.getTableHeader().setForeground(Color.WHITE);
        
        // Add some dummy orders for demonstration
        addDummyOrders();
        
        // Create button column for View Details action
        orderTable.getColumnModel().getColumn(5).setCellRenderer(new ButtonRenderer());
        orderTable.getColumnModel().getColumn(5).setCellEditor(new ButtonEditor(new JCheckBox(), this, "view"));
        
        // Set column widths
        orderTable.getColumnModel().getColumn(0).setPreferredWidth(50);    // ID
        orderTable.getColumnModel().getColumn(1).setPreferredWidth(150);   // Customer
        orderTable.getColumnModel().getColumn(2).setPreferredWidth(150);   // Date
        orderTable.getColumnModel().getColumn(3).setPreferredWidth(120);   // Total
        orderTable.getColumnModel().getColumn(4).setPreferredWidth(100);   // Status
        orderTable.getColumnModel().getColumn(5).setPreferredWidth(120);   // Actions
        
        JScrollPane scrollPane = new JScrollPane(orderTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        
        // Add components to order panel
        ordersPanel.add(titlePanel, BorderLayout.NORTH);
        ordersPanel.add(scrollPane, BorderLayout.CENTER);
        
        return ordersPanel;
    }
    
    private void addDummyOrders() {
        Object[][] dummyData = {
            {1001, "Budi Santoso", "10-04-2025", "Rp 598.000", "Selesai", ""},
            {1002, "Ani Wijaya", "11-04-2025", "Rp 245.000", "Dikirim", ""},
            {1003, "Denny Pratama", "12-04-2025", "Rp 450.000", "Diproses", ""},
            {1004, "Sinta Dewi", "13-04-2025", "Rp 189.000", "Menunggu", ""},
            {1005, "Rizki Ahmad", "14-04-2025", "Rp 458.000", "Dibatalkan", ""}
        };
        
        for (Object[] row : dummyData) {
            orderTableModel.addRow(row);
        }
    }
    
    public void showProductDialog(Integer productId) {
        String title = (productId == null) ? "Tambah Produk Baru" : "Edit Produk";
        
        JDialog dialog = new JDialog(this, title, true);
        dialog.setSize(400, 350);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Form components
        JLabel lblName = new JLabel("Nama Produk:");
        JTextField txtName = new JTextField(20);
        txtName.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        
        JLabel lblPrice = new JLabel("Harga:");
        JTextField txtPrice = new JTextField(20);
        txtPrice.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        
        JLabel lblStock = new JLabel("Stok:");
        JTextField txtStock = new JTextField(20);
        txtStock.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        
        JLabel lblDescription = new JLabel("Deskripsi:");
        JTextArea txtDescription = new JTextArea(5, 20);
        JScrollPane descScrollPane = new JScrollPane(txtDescription);
        descScrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        
        // If editing existing product, populate the form
        if (productId != null) {
            int row = findProductRowById(productId);
            if (row != -1) {
                txtName.setText((String) productTableModel.getValueAt(row, 1));
                // Remove "Rp " prefix and convert to plain number
                String priceStr = ((String) productTableModel.getValueAt(row, 2))
                        .replace("Rp ", "")
                        .replace(".", "");
                txtPrice.setText(priceStr);
                txtStock.setText(productTableModel.getValueAt(row, 3).toString());
                txtDescription.setText("Deskripsi produk " + productTableModel.getValueAt(row, 1));
            }
        }
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnCancel = new JButton("Batal");
        JButton btnSave = new JButton("Simpan");
        
        btnSave.setBackground(new Color(40, 167, 69));
        btnSave.setForeground(Color.WHITE);
        btnSave.setFocusPainted(false);
        
        btnCancel.setBackground(new Color(108, 117, 125));
        btnCancel.setForeground(Color.WHITE);
        btnCancel.setFocusPainted(false);
        
        btnCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        });
        
        btnSave.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Validate inputs
                String name = txtName.getText().trim();
                String priceText = txtPrice.getText().trim();
                String stockText = txtStock.getText().trim();
                
                if (name.isEmpty() || priceText.isEmpty() || stockText.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, 
                            "Semua field harus diisi!", 
                            "Validasi Error", 
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                try {
                    double price = Double.parseDouble(priceText);
                    int stock = Integer.parseInt(stockText);
                    
                    if (price <= 0 || stock < 0) {
                        JOptionPane.showMessageDialog(dialog, 
                                "Harga harus lebih dari 0 dan stok tidak boleh negatif!", 
                                "Validasi Error", 
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    
                    // Format price for display
                    String formattedPrice = "Rp " + String.format("%,.0f", price).replace(",", ".");
                    
                    if (productId == null) {
                        // Add new product
                        int newId = productTableModel.getRowCount() + 1;
                        productTableModel.addRow(new Object[]{newId, name, formattedPrice, stock, ""});
                    } else {
                        // Update existing product
                        int row = findProductRowById(productId);
                        if (row != -1) {
                            productTableModel.setValueAt(name, row, 1);
                            productTableModel.setValueAt(formattedPrice, row, 2);
                            productTableModel.setValueAt(stock, row, 3);
                        }
                    }
                    
                    dialog.dispose();
                    
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(dialog, 
                            "Harga dan stok harus berupa angka!", 
                            "Validasi Error", 
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        
        buttonPanel.add(btnCancel);
        buttonPanel.add(btnSave);
        
        // Add components to panel with spacing
        panel.add(lblName);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(txtName);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        
        panel.add(lblPrice);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(txtPrice);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        
        panel.add(lblStock);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(txtStock);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        
        panel.add(lblDescription);
        panel.add(Box.createRigidArea(new Dimension(0, 5)));
        panel.add(descScrollPane);
        
        // Add components to dialog
        dialog.add(panel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        
        dialog.setVisible(true);
    }
    
    private int findProductRowById(int id) {
        for (int i = 0; i < productTableModel.getRowCount(); i++) {
            if ((int) productTableModel.getValueAt(i, 0) == id) {
                return i;
            }
        }
        return -1;
    }
    
    public void showOrderDetails(int orderId) {
        int row = -1;
        for (int i = 0; i < orderTableModel.getRowCount(); i++) {
            if ((int) orderTableModel.getValueAt(i, 0) == orderId) {
                row = i;
                break;
            }
        }
        
        if (row != -1) {
            String customer = (String) orderTableModel.getValueAt(row, 1);
            String date = (String) orderTableModel.getValueAt(row, 2);
            String total = (String) orderTableModel.getValueAt(row, 3);
            String status = (String) orderTableModel.getValueAt(row, 4);
            
            JDialog dialog = new JDialog(this, "Detail Pesanan #" + orderId, true);
            dialog.setSize(500, 400);
            dialog.setLocationRelativeTo(this);
            dialog.setLayout(new BorderLayout());
            
            JPanel headerPanel = new JPanel(new GridLayout(5, 1, 5, 5));
            headerPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
            headerPanel.add(new JLabel("ID Pesanan: " + orderId));
            headerPanel.add(new JLabel("Pembeli: " + customer));
            headerPanel.add(new JLabel("Tanggal: " + date));
            headerPanel.add(new JLabel("Total: " + total));
            
            // Create status panel with color indication
            JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            JLabel statusLabel = new JLabel("Status: ");
            JLabel statusValue = new JLabel(status);
            
            // Set status color based on value
            Color statusColor;
            switch (status) {
                case "Selesai":
                    statusColor = new Color(40, 167, 69); // Green
                    break;
                case "Dikirim":
                    statusColor = new Color(0, 123, 255); // Blue
                    break;
                case "Diproses":
                    statusColor = new Color(255, 193, 7); // Yellow
                    break;
                case "Dibatalkan":
                    statusColor = new Color(220, 53, 69); // Red
                    break;
                default:
                    statusColor = new Color(108, 117, 125); // Gray
            }
            statusValue.setForeground(statusColor);
            statusValue.setFont(new Font("Arial", Font.BOLD, 12));
            
            statusPanel.add(statusLabel);
            statusPanel.add(statusValue);
            headerPanel.add(statusPanel);
            
            // Create order items table
            String[] columns = {"Produk", "Harga", "Jumlah", "Subtotal"};
            DefaultTableModel itemsModel = new DefaultTableModel(columns, 0);
            
            // Add dummy order items
            Object[][] items = {
                {"Sepatu Sport", "Rp 299.000", 1, "Rp 299.000"},
                {"Kemeja Formal", "Rp 199.000", 1, "Rp 199.000"},
                {"Kacamata Hitam", "Rp 159.000", 1, "Rp 159.000"}
            };
            
            for (Object[] item : items) {
                itemsModel.addRow(item);
            }
            
            JTable itemsTable = new JTable(itemsModel);
            itemsTable.setRowHeight(30);
            itemsTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
            itemsTable.getTableHeader().setBackground(new Color(33, 37, 41));
            itemsTable.getTableHeader().setForeground(Color.WHITE);
            
            JScrollPane scrollPane = new JScrollPane(itemsTable);
            
            // Button panel
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton btnClose = new JButton("Tutup");
            btnClose.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    dialog.dispose();
                }
            });
            buttonPanel.add(btnClose);
            
            // Add components to dialog
            dialog.add(headerPanel, BorderLayout.NORTH);
            dialog.add(scrollPane, BorderLayout.CENTER);
            dialog.add(buttonPanel, BorderLayout.SOUTH);
            // Add components to dialog
            dialog.add(headerPanel, BorderLayout.NORTH);
            dialog.add(scrollPane, BorderLayout.CENTER);
            dialog.add(buttonPanel, BorderLayout.SOUTH);
            
            dialog.setVisible(true);
        }
    }
    
    public void deleteProduct(int productId) {
        int row = findProductRowById(productId);
        if (row != -1) {
            int confirmation = JOptionPane.showConfirmDialog(
                this,
                "Apakah Anda yakin ingin menghapus produk ini?",
                "Konfirmasi Hapus",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            
            if (confirmation == JOptionPane.YES_OPTION) {
                productTableModel.removeRow(row);
                JOptionPane.showMessageDialog(
                    this,
                    "Produk berhasil dihapus",
                    "Sukses",
                    JOptionPane.INFORMATION_MESSAGE
                );
            }
        }
    }
    
    public void updateOrderStatus(int orderId, String newStatus) {
        for (int i = 0; i < orderTableModel.getRowCount(); i++) {
            if ((int) orderTableModel.getValueAt(i, 0) == orderId) {
                orderTableModel.setValueAt(newStatus, i, 4);
                break;
            }
        }
    }
    
    // Class untuk render tombol pada tabel
    class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
            setBorderPainted(false);
            setFocusPainted(false);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            if (isSelected) {
                setBackground(table.getSelectionBackground());
            } else {
                setBackground(table.getBackground());
            }
            
            // Panel untuk menyimpan tombol-tombol
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
            panel.setBackground(table.getBackground());
            
            // Mengatur tampilan sesuai dengan jenis tabel
            if (table == productTable) {
                JButton editBtn = new JButton("Edit");
                editBtn.setBackground(new Color(0, 123, 255));
                editBtn.setForeground(Color.WHITE);
                editBtn.setFocusPainted(false);
                
                JButton deleteBtn = new JButton("Hapus");
                deleteBtn.setBackground(new Color(220, 53, 69));
                deleteBtn.setForeground(Color.WHITE);
                deleteBtn.setFocusPainted(false);
                
                panel.add(editBtn);
                panel.add(deleteBtn);
                
                return panel;
            } else if (table == orderTable) {
                setText("Lihat Detail");
                setBackground(new Color(0, 123, 255));
                setForeground(Color.WHITE);
                return this;
            }
            
            return panel;
        }
    }
    
    // Class untuk editor tombol pada tabel
    class ButtonEditor extends DefaultCellEditor {
        protected JButton button;
        protected String buttonType;
        protected int currentId;
        protected SupervisorDashboardUI parent;
        protected boolean isPushed;
        
        public ButtonEditor(JCheckBox checkBox, SupervisorDashboardUI parent) {
            this(checkBox, parent, "edit-delete");
        }
        
        public ButtonEditor(JCheckBox checkBox, SupervisorDashboardUI parent, String buttonType) {
            super(checkBox);
            this.parent = parent;
            this.buttonType = buttonType;
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    fireEditingStopped();
                }
            });
        }
        
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            isPushed = true;
            
            if (table == productTable) {
                currentId = (int) table.getValueAt(row, 0);
                
                // Panel untuk menyimpan tombol-tombol
                JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
                panel.setBackground(table.getBackground());
                
                JButton editBtn = new JButton("Edit");
                editBtn.setBackground(new Color(0, 123, 255));
                editBtn.setForeground(Color.WHITE);
                editBtn.setFocusPainted(false);
                editBtn.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        parent.showProductDialog(currentId);
                    }
                });
                
                JButton deleteBtn = new JButton("Hapus");
                deleteBtn.setBackground(new Color(220, 53, 69));
                deleteBtn.setForeground(Color.WHITE);
                deleteBtn.setFocusPainted(false);
                deleteBtn.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        parent.deleteProduct(currentId);
                    }
                });
                
                panel.add(editBtn);
                panel.add(deleteBtn);
                
                return panel;
            } else if (table == orderTable) {
                currentId = (int) table.getValueAt(row, 0);
                button.setText("Lihat Detail");
                button.setBackground(new Color(0, 123, 255));
                button.setForeground(Color.WHITE);
                return button;
            }
            
            return new JPanel();
        }
        
        @Override
        public Object getCellEditorValue() {
            isPushed = false;
            return "";
        }
        
        @Override
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
        
        @Override
        protected void fireEditingStopped() {
            super.fireEditingStopped();
            
            if (isPushed) {
                if (buttonType.equals("view")) {
                    parent.showOrderDetails(currentId);
                }
            }
        }
    }
    
    // Inner class untuk status combobox dalam dialog pesanan
    class StatusComboBox extends JComboBox<String> {
        public StatusComboBox(String currentStatus) {
            super(new String[]{"Menunggu", "Diproses", "Dikirim", "Selesai", "Dibatalkan"});
            
            setSelectedItem(currentStatus);
            setBackground(Color.WHITE);
            
            // Mengubah tampilan item berdasarkan statusnya
            setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, 
                        int index, boolean isSelected, boolean cellHasFocus) {
                    Component c = super.getListCellRendererComponent(
                            list, value, index, isSelected, cellHasFocus);
                    
                    if (c instanceof JLabel && value != null) {
                        JLabel label = (JLabel) c;
                        String status = value.toString();
                        
                        // Set warna teks sesuai status
                        switch (status) {
                            case "Selesai":
                                label.setForeground(new Color(40, 167, 69)); // Hijau
                                break;
                            case "Dikirim":
                                label.setForeground(new Color(0, 123, 255)); // Biru
                                break;
                            case "Diproses":
                                label.setForeground(new Color(255, 193, 7)); // Kuning
                                break;
                            case "Dibatalkan":
                                label.setForeground(new Color(220, 53, 69)); // Merah
                                break;
                            default:
                                label.setForeground(new Color(108, 117, 125)); // Abu-abu
                        }
                    }
                    return c;
                }
            });
        }
    }
    
    // Menambahkan fungsi untuk memperbaharui dialog detail pesanan dengan opsi update status
    public void showOrderDetailsWithStatusUpdate(int orderId) {
        int row = -1;
        for (int i = 0; i < orderTableModel.getRowCount(); i++) {
            if ((int) orderTableModel.getValueAt(i, 0) == orderId) {
                row = i;
                break;
            }
        }
        
        if (row != -1) {
            String customer = (String) orderTableModel.getValueAt(row, 1);
            String date = (String) orderTableModel.getValueAt(row, 2);
            String total = (String) orderTableModel.getValueAt(row, 3);
            String status = (String) orderTableModel.getValueAt(row, 4);
            
            JDialog dialog = new JDialog(this, "Detail Pesanan #" + orderId, true);
            dialog.setSize(500, 450);
            dialog.setLocationRelativeTo(this);
            dialog.setLayout(new BorderLayout());
            
            JPanel headerPanel = new JPanel(new GridLayout(5, 1, 5, 5));
            headerPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
            headerPanel.add(new JLabel("ID Pesanan: " + orderId));
            headerPanel.add(new JLabel("Pembeli: " + customer));
            headerPanel.add(new JLabel("Tanggal: " + date));
            headerPanel.add(new JLabel("Total: " + total));
            
            // Panel untuk status pesanan dengan dropdown untuk update
            JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
            statusPanel.add(new JLabel("Status: "));
            
            // Dropdown untuk status
            StatusComboBox statusCombo = new StatusComboBox(status);
            statusPanel.add(statusCombo);
            
            // Button untuk update status
            JButton updateStatusBtn = new JButton("Update Status");
            updateStatusBtn.setBackground(new Color(0, 123, 255));
            updateStatusBtn.setForeground(Color.WHITE);
            updateStatusBtn.setFocusPainted(false);
            updateStatusBtn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String newStatus = (String) statusCombo.getSelectedItem();
                    updateOrderStatus(orderId, newStatus);
                    JOptionPane.showMessageDialog(dialog, 
                            "Status pesanan berhasil diperbarui menjadi " + newStatus, 
                            "Sukses", 
                            JOptionPane.INFORMATION_MESSAGE);
                }
            });
            statusPanel.add(updateStatusBtn);
            
            headerPanel.add(statusPanel);
            
            // Create order items table
            String[] columns = {"Produk", "Harga", "Jumlah", "Subtotal"};
            DefaultTableModel itemsModel = new DefaultTableModel(columns, 0);
            
            // Add dummy order items based on order ID
            Object[][] items;
            
            switch (orderId) {
                case 1001:
                    items = new Object[][]{
                        {"Sepatu Sport", "Rp 299.000", 1, "Rp 299.000"},
                        {"Kemeja Formal", "Rp 199.000", 1, "Rp 199.000"},
                        {"Kacamata Hitam", "Rp 100.000", 1, "Rp 100.000"}
                    };
                    break;
                case 1002:
                    items = new Object[][]{
                        {"Celana Jeans", "Rp 245.000", 1, "Rp 245.000"}
                    };
                    break;
                case 1003:
                    items = new Object[][]{
                        {"Jam Tangan", "Rp 450.000", 1, "Rp 450.000"}
                    };
                    break;
                case 1004:
                    items = new Object[][]{
                        {"Tas Ransel", "Rp 189.000", 1, "Rp 189.000"}
                    };
                    break;
                case 1005:
                    items = new Object[][]{
                        {"Sepatu Sport", "Rp 299.000", 1, "Rp 299.000"},
                        {"Tas Ransel", "Rp 159.000", 1, "Rp 159.000"}
                    };
                    break;
                default:
                    items = new Object[][]{
                        {"Produk", "Rp 0", 0, "Rp 0"}
                    };
            }
            
            for (Object[] item : items) {
                itemsModel.addRow(item);
            }
            
            JTable itemsTable = new JTable(itemsModel);
            itemsTable.setRowHeight(30);
            itemsTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
            itemsTable.getTableHeader().setBackground(new Color(33, 37, 41));
            itemsTable.getTableHeader().setForeground(Color.WHITE);
            
            JScrollPane scrollPane = new JScrollPane(itemsTable);
            
            // Panel untuk informasi alamat pengiriman
            JPanel addressPanel = new JPanel(new BorderLayout());
            addressPanel.setBorder(new EmptyBorder(10, 15, 10, 15));
            
            JLabel addressLabel = new JLabel("Alamat Pengiriman:");
            addressLabel.setFont(new Font("Arial", Font.BOLD, 14));
            
            JTextArea addressArea = new JTextArea(3, 20);
            addressArea.setText("Jl. Contoh No. 123\nKecamatan Contoh\nKota " + 
                    (orderId % 2 == 0 ? "Jakarta" : "Bandung") + 
                    "\nKode Pos " + (10000 + orderId));
            addressArea.setEditable(false);
            addressArea.setBackground(new Color(245, 245, 245));
            addressArea.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            
            addressPanel.add(addressLabel, BorderLayout.NORTH);
            addressPanel.add(addressArea, BorderLayout.CENTER);
            
            // Button panel
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton btnPrint = new JButton("Cetak Invoice");
            btnPrint.setBackground(new Color(40, 167, 69));
            btnPrint.setForeground(Color.WHITE);
            btnPrint.setFocusPainted(false);
            btnPrint.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JOptionPane.showMessageDialog(dialog, 
                            "Fitur cetak invoice dalam pengembangan", 
                            "Informasi", 
                            JOptionPane.INFORMATION_MESSAGE);
                }
            });
            
            JButton btnClose = new JButton("Tutup");
            btnClose.setBackground(new Color(108, 117, 125));
            btnClose.setForeground(Color.WHITE);
            btnClose.setFocusPainted(false);
            btnClose.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    dialog.dispose();
                }
            });
            
            buttonPanel.add(btnPrint);
            buttonPanel.add(btnClose);
            
            // Panel untuk konten utama
            JPanel mainContentPanel = new JPanel(new BorderLayout());
            mainContentPanel.add(scrollPane, BorderLayout.CENTER);
            mainContentPanel.add(addressPanel, BorderLayout.SOUTH);
            
            // Add components to dialog
            dialog.add(headerPanel, BorderLayout.NORTH);
            dialog.add(mainContentPanel, BorderLayout.CENTER);
            dialog.add(buttonPanel, BorderLayout.SOUTH);
            
            dialog.setVisible(true);
        }
    }
}