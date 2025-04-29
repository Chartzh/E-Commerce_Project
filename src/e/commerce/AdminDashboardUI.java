package e.commerce;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.table.TableCellRenderer;

public class AdminDashboardUI extends JFrame {
    private JPanel contentPanel;
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private JButton btnHamburger, btnBack;
    private JPopupMenu hamburgerMenu;
    private JTable userTable;
    private DefaultTableModel userTableModel;
    private JComboBox<String> roleComboBox;
    
    public AdminDashboardUI() {
        User currentUser = Authentication.getCurrentUser();
        if (currentUser == null || !currentUser.getRole().equalsIgnoreCase("admin")) {
            JOptionPane.showMessageDialog(this, "Akses ditolak! Hanya admin yang dapat mengakses halaman ini.", "Error", JOptionPane.ERROR_MESSAGE);
            LoginUI loginUI = new LoginUI();
            loginUI.setVisible(true);
            dispose();
            return;
        }
        
        setTitle("E-Commerce App - Admin Dashboard");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Set ikon
        IconUtil.setIcon(this);
        
        // Main layout
        setLayout(new BorderLayout());
        
        // Create main panel with CardLayout
        mainPanel = new JPanel();
        cardLayout = new CardLayout();
        mainPanel.setLayout(cardLayout);
        
        // Create header panel
        JPanel headerPanel = createHeaderPanel(currentUser);
        
        // Create admin dashboard panel
        JPanel dashboardPanel = createAdminDashboardPanel();
        
        // Create user management panel
        JPanel userManagementPanel = createUserManagementPanel();
        
        // Create profile panel
        ProfileUI profilePanel = new ProfileUI();
        
        // Add panels to the card layout
        mainPanel.add(dashboardPanel, "Dashboard");
        mainPanel.add(userManagementPanel, "UserManagement");
        mainPanel.add(profilePanel, "Profile");
        
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
        JLabel lblLogo = new JLabel("Quantra - Admin");
        lblLogo.setFont(new Font("Arial", Font.BOLD, 24));
        lblLogo.setForeground(Color.WHITE);
        headerPanel.add(lblLogo, BorderLayout.WEST);
        
        // Welcome message
        JLabel lblWelcome = new JLabel("Selamat Datang, Admin " + currentUser.getUsername());
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
                cardLayout.show(mainPanel, "Dashboard");
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
        
        // User Management menu item
        JMenuItem menuUserManagement = createMenuItem("Kelola Pengguna", "\uf0c0");  // Unicode for users icon
        menuUserManagement.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(mainPanel, "UserManagement");
                btnHamburger.setVisible(false);
                btnBack.setVisible(true);
                
                // Refresh user data when navigating to the user management page
                loadUserData();
            }
        });
        
        // Profile menu item dengan icon
        JMenuItem menuProfile = createMenuItem("Profil", "\uf007");  // Unicode for user icon
        menuProfile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(mainPanel, "Profile");
                btnHamburger.setVisible(false);
                btnBack.setVisible(true);
            }
        });
        
        // Logout menu item dengan icon
        JMenuItem menuLogout = createMenuItem("Logout", "\uf08b");  // Unicode for sign-out icon
        menuLogout.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int response = JOptionPane.showConfirmDialog(
                        AdminDashboardUI.this,
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
        hamburgerMenu.add(menuUserManagement);
        
        // Separator yang lebih stylish
        JSeparator separator1 = new JSeparator();
        separator1.setBackground(new Color(60, 65, 72));
        separator1.setForeground(new Color(70, 75, 82));
        hamburgerMenu.add(separator1);
        
        hamburgerMenu.add(menuProfile);
        
        // Separator untuk logout
        JSeparator separator2 = new JSeparator();
        separator2.setBackground(new Color(60, 65, 72));
        separator2.setForeground(new Color(70, 75, 82));
        hamburgerMenu.add(separator2);
        
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
    
    // metode untuk menghubungkan stats dengan database
    private int getTotalUserCountByRole(String role) {
    int count = 0;
    String query = "SELECT COUNT(*) FROM users WHERE role = ?";

    Connection conn = null;
    PreparedStatement stmt = null;
    ResultSet rs = null;

    try {
        conn = DatabaseConnection.getConnection();
        stmt = conn.prepareStatement(query);

        // Ini baris pentingnya
        stmt.setString(1, role);

        rs = stmt.executeQuery();

        if (rs.next()) {
            count = rs.getInt(1);
        }

    } catch (SQLException e) {
        e.printStackTrace();
    } finally {
        DatabaseConnection.closeConnection(conn, stmt, rs);
    }

    return count;
}

    
    private JPanel createAdminDashboardPanel() {
        JPanel dashboardPanel = new JPanel();
        dashboardPanel.setLayout(new BorderLayout());
        dashboardPanel.setBackground(new Color(245, 245, 245));
        
        // Title panel
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(new Color(245, 245, 245));
        titlePanel.setBorder(new EmptyBorder(20, 20, 10, 20));
        
        JLabel lblTitle = new JLabel("Dashboard Admin");
        lblTitle.setFont(new Font("Arial", Font.BOLD, 24));
        titlePanel.add(lblTitle, BorderLayout.WEST);
        
        // Stats panel
        JPanel statsPanel = new JPanel(new GridLayout(1, 3, 15, 0));
        statsPanel.setBackground(new Color(245, 245, 245));
        statsPanel.setBorder(new EmptyBorder(10, 20, 20, 20));
        
        // Stat cards
        int totalUserOnly = getTotalUserCountByRole("user");
        statsPanel.add(createStatCard("Total Pengguna", String.valueOf(totalUserOnly), new Color(41, 128, 185)));
        statsPanel.add(createStatCard("Total Produk", "48", new Color(39, 174, 96)));
        statsPanel.add(createStatCard("Total Transaksi", "312", new Color(231, 76, 60)));
        
        // Quick actions panel
        JPanel actionsPanel = new JPanel(new GridLayout(2, 2, 15, 15));
        actionsPanel.setBackground(new Color(245, 245, 245));
        actionsPanel.setBorder(new EmptyBorder(0, 20, 20, 20));
        
        // Quick action buttons
        actionsPanel.add(createActionButton("Kelola Pengguna", "\uf0c0", new Color(52, 152, 219), e -> {
            cardLayout.show(mainPanel, "UserManagement");
            btnHamburger.setVisible(false);
            btnBack.setVisible(true);
            loadUserData();
        }));
        
        actionsPanel.add(createActionButton("Kelola Produk", "\uf07a", new Color(46, 204, 113), e -> {
            JOptionPane.showMessageDialog(AdminDashboardUI.this, 
                    "Fitur kelola produk akan segera tersedia.", 
                    "Info", 
                    JOptionPane.INFORMATION_MESSAGE);
        }));
        
        actionsPanel.add(createActionButton("Laporan Penjualan", "\uf080", new Color(243, 156, 18), e -> {
            JOptionPane.showMessageDialog(AdminDashboardUI.this, 
                    "Fitur laporan penjualan akan segera tersedia.", 
                    "Info", 
                    JOptionPane.INFORMATION_MESSAGE);
        }));
        
        actionsPanel.add(createActionButton("Pengaturan", "\uf013", new Color(149, 165, 166), e -> {
            JOptionPane.showMessageDialog(AdminDashboardUI.this, 
                    "Fitur pengaturan akan segera tersedia.", 
                    "Info", 
                    JOptionPane.INFORMATION_MESSAGE);
        }));
        
        // Add components to dashboard panel
        dashboardPanel.add(titlePanel, BorderLayout.NORTH);
        dashboardPanel.add(statsPanel, BorderLayout.CENTER);
        dashboardPanel.add(actionsPanel, BorderLayout.SOUTH);
        
        return dashboardPanel;
    }
    
    private JPanel createStatCard(String title, String value, Color color) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220), 1));
        
        // Color header
        JPanel headerPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Create gradient background
                GradientPaint gradient = new GradientPaint(
                    0, 0, color, 
                    getWidth(), getHeight(), new Color(color.getRed(), color.getGreen(), color.getBlue(), 200)
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        headerPanel.setPreferredSize(new Dimension(100, 15));
        
        // Content panel
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        // Stats value
        JLabel lblValue = new JLabel(value);
        lblValue.setFont(new Font("Arial", Font.BOLD, 32));
        lblValue.setForeground(color);
        lblValue.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Stats title
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Arial", Font.PLAIN, 14));
        lblTitle.setForeground(new Color(100, 100, 100));
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        contentPanel.add(lblValue);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        contentPanel.add(lblTitle);
        
        // Add components to card
        card.add(headerPanel, BorderLayout.NORTH);
        card.add(contentPanel, BorderLayout.CENTER);
        
        return card;
    }
    
    private JPanel createActionButton(String text, String icon, Color color, ActionListener action) {
        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220), 1));
        
        // Content panel
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(new EmptyBorder(20, 15, 20, 15));
        
        // Icon label
        JLabel lblIcon = new JLabel(icon);
        lblIcon.setFont(new Font("Arial", Font.BOLD, 24));
        lblIcon.setForeground(color);
        lblIcon.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Text label
        JLabel lblText = new JLabel(text);
        lblText.setFont(new Font("Arial", Font.BOLD, 14));
        lblText.setForeground(new Color(80, 80, 80));
        lblText.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        contentPanel.add(lblIcon);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        contentPanel.add(lblText);
        
        buttonPanel.add(contentPanel, BorderLayout.CENTER);
        
        // Add hover effect and click functionality
        buttonPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                buttonPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
                buttonPanel.setBorder(BorderFactory.createLineBorder(color, 2));
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                buttonPanel.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220), 1));
            }
            
            @Override
            public void mouseClicked(MouseEvent e) {
                if (action != null) {
                    action.actionPerformed(new ActionEvent(buttonPanel, ActionEvent.ACTION_PERFORMED, "clicked"));
                }
            }
        });
        
        return buttonPanel;
    }
    
    private JPanel createUserManagementPanel() {
        JPanel userManagementPanel = new JPanel(new BorderLayout());
        userManagementPanel.setBackground(Color.WHITE);
        
        // Title panel
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(Color.WHITE);
        titlePanel.setBorder(new EmptyBorder(20, 20, 10, 20));
        
        JLabel lblTitle = new JLabel("Kelola Pengguna");
        lblTitle.setFont(new Font("Arial", Font.BOLD, 20));
        titlePanel.add(lblTitle, BorderLayout.WEST);
        
        // Search and add user panel
        JPanel actionPanel = new JPanel(new BorderLayout());
        actionPanel.setBackground(Color.WHITE);
        actionPanel.setBorder(new EmptyBorder(0, 20, 10, 20));
        
        // Search field
        JTextField searchField = new JTextField(20);
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        
        JButton searchButton = new JButton("Cari");
        searchButton.setBackground(new Color(52, 152, 219));
        searchButton.setForeground(Color.WHITE);
        searchButton.setFocusPainted(false);
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String keyword = searchField.getText().trim();
                filterUserTable(keyword);
            }
        });
        
        JButton addUserButton = new JButton("Tambah Pengguna");
        addUserButton.setBackground(new Color(46, 204, 113));
        addUserButton.setForeground(Color.WHITE);
        addUserButton.setFocusPainted(false);
        addUserButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showAddUserDialog();
            }
        });
        
        // Panel for search components
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.setBackground(Color.WHITE);
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        
        // Panel for add button
        JPanel addPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        addPanel.setBackground(Color.WHITE);
        addPanel.add(addUserButton);
        
        actionPanel.add(searchPanel, BorderLayout.WEST);
        actionPanel.add(addPanel, BorderLayout.EAST);
        
        // User table
        String[] columnNames = {"ID", "Username", "Email", "Role", "Aksi"};
        userTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 4; // Only action column is editable
            }
        };
        
        userTable = new JTable(userTableModel);
        userTable.setRowHeight(40);
        userTable.setShowVerticalLines(false);
        userTable.setShowHorizontalLines(true);
        userTable.setGridColor(new Color(240, 240, 240));
        userTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        userTable.getTableHeader().setBackground(new Color(240, 240, 240));
        userTable.getTableHeader().setForeground(new Color(50, 50, 50));
        
        // Custom renderer for action buttons
        userTable.getColumnModel().getColumn(4).setCellRenderer(new ButtonRenderer());
        userTable.getColumnModel().getColumn(4).setCellEditor(new ButtonEditor(new JCheckBox()));
        
        JScrollPane scrollPane = new JScrollPane(userTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        
        // Add components to user management panel
        userManagementPanel.add(titlePanel, BorderLayout.NORTH);
        userManagementPanel.add(actionPanel, BorderLayout.CENTER);
        userManagementPanel.add(scrollPane, BorderLayout.SOUTH);
        
        return userManagementPanel;
    }
    
    private void loadUserData() {
        // Clear existing data
        userTableModel.setRowCount(0);
        
        try {
            // Get database connection
            Connection conn = DatabaseConnection.getConnection();
            String query = "SELECT * FROM users ORDER BY id";
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();
            
            // Add data to table model
            while (rs.next()) {
                Object[] row = new Object[5];
                row[0] = rs.getInt("id");
                row[1] = rs.getString("username");
                row[2] = rs.getString("email");
                row[3] = rs.getString("role");
                row[4] = "Aksi"; // Placeholder for action buttons
                userTableModel.addRow(row);
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                    "Error mengambil data pengguna: " + e.getMessage(), 
                    "Database Error", 
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void filterUserTable(String keyword) {
        // Clear existing data
        userTableModel.setRowCount(0);
        
        try {
            // Get database connection
            Connection conn = DatabaseConnection.getConnection();
            String query = "SELECT * FROM users WHERE username LIKE ? OR email LIKE ? OR role LIKE ? ORDER BY id";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, "%" + keyword + "%");
            stmt.setString(2, "%" + keyword + "%");
            stmt.setString(3, "%" + keyword + "%");
            ResultSet rs = stmt.executeQuery();
            
            // Add data to table model
            while (rs.next()) {
                Object[] row = new Object[5];
                row[0] = rs.getInt("id");
                row[1] = rs.getString("username");
                row[2] = rs.getString("email");
                row[3] = rs.getString("role");
                row[4] = "Aksi"; // Placeholder for action buttons
                userTableModel.addRow(row);
            }
            
            rs.close();
            stmt.close();
            
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                    "Error mencari data pengguna: " + e.getMessage(), 
                    "Database Error", 
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void showAddUserDialog() {
        JDialog addUserDialog = new JDialog(this, "Tambah Pengguna", true);
        addUserDialog.setSize(400, 300);
        addUserDialog.setLocationRelativeTo(this);
        addUserDialog.setLayout(new BorderLayout());
        
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new GridLayout(5, 2, 10, 10));
        formPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Username field
        JLabel lblUsername = new JLabel("Username:");
        JTextField txtUsername = new JTextField(20);
        
        // Email field
        JLabel lblEmail = new JLabel("Email:");
        JTextField txtEmail = new JTextField(20);
        
        // Password field
        JLabel lblPassword = new JLabel("Password:");
        JPasswordField txtPassword = new JPasswordField(20);
        
        // Role selection
        JLabel lblRole = new JLabel("Role:");
        String[] roles = {"user", "admin"};
        JComboBox<String> cmbRole = new JComboBox<>(roles);
        
        formPanel.add(lblUsername);
        formPanel.add(txtUsername);
        formPanel.add(lblEmail);
        formPanel.add(txtEmail);
        formPanel.add(lblPassword);
        formPanel.add(txtPassword);
        formPanel.add(lblRole);
        formPanel.add(cmbRole);
        
        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton btnCancel = new JButton("Batal");
        btnCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addUserDialog.dispose();
            }
        });
        
        JButton btnSave = new JButton("Simpan");
        btnSave.setBackground(new Color(46, 204, 113));
        btnSave.setForeground(Color.WHITE);
        btnSave.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = txtUsername.getText().trim();
                String email = txtEmail.getText().trim();
                String password = new String(txtPassword.getPassword());
                String role = (String) cmbRole.getSelectedItem();
                
                if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                    JOptionPane.showMessageDialog(addUserDialog, 
                            "Semua field harus diisi!", 
                            "Error", 
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                try {
                    // Cek apakah username sudah ada
                    Connection conn = DatabaseConnection.getConnection();
                    String checkQuery = "SELECT COUNT(*) FROM users WHERE username = ?";
                    PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
                    checkStmt.setString(1, username);
                    ResultSet rs = checkStmt.executeQuery();
                    rs.next();
                    
                    if (rs.getInt(1) > 0) {
                        JOptionPane.showMessageDialog(addUserDialog, 
                                "Username sudah digunakan!", 
                                "Error", 
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    
                    // Insert user baru
                    String insertQuery = "INSERT INTO users (username, email, password, role) VALUES (?, ?, ?, ?)";
                    PreparedStatement insertStmt = conn.prepareStatement(insertQuery);
                    insertStmt.setString(1, username);
                    insertStmt.setString(2, email);
                    insertStmt.setString(3, password);
                    insertStmt.setString(4, role);
                    
                    int result = insertStmt.executeUpdate();
                    
                    if (result > 0) {
                        JOptionPane.showMessageDialog(addUserDialog, 
                                "Pengguna berhasil ditambahkan!", 
                                "Sukses", 
                                JOptionPane.INFORMATION_MESSAGE);
                        addUserDialog.dispose();
                        loadUserData(); // Refresh table
                    } else {
                        JOptionPane.showMessageDialog(addUserDialog, 
                                "Gagal menambahkan pengguna!", 
                                "Error", 
                                JOptionPane.ERROR_MESSAGE);
                    }
                    
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(addUserDialog, 
                            "Error database: " + ex.getMessage(), 
                            "Error", 
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        
buttonPanel.add(btnCancel);
        buttonPanel.add(btnSave);
        
        addUserDialog.add(formPanel, BorderLayout.CENTER);
        addUserDialog.add(buttonPanel, BorderLayout.SOUTH);
        addUserDialog.setVisible(true);
    }
    
    // Class untuk render tombol aksi di tabel
    class ButtonRenderer extends JPanel implements TableCellRenderer {
        private JButton editButton;
        private JButton deleteButton;
        
        public ButtonRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));
            
            editButton = new JButton("Edit");
            editButton.setPreferredSize(new Dimension(65, 30));
            editButton.setBackground(new Color(52, 152, 219));
            editButton.setForeground(Color.WHITE);
            editButton.setFocusPainted(false);
            editButton.setBorderPainted(false);
            editButton.setFont(new Font("Arial", Font.BOLD, 12));
            
            deleteButton = new JButton("Hapus");
            deleteButton.setPreferredSize(new Dimension(65, 30));
            deleteButton.setBackground(new Color(231, 76, 60));
            deleteButton.setForeground(Color.WHITE);
            deleteButton.setFocusPainted(false);
            deleteButton.setBorderPainted(false);
            deleteButton.setFont(new Font("Arial", Font.BOLD, 12));
            
            add(editButton);
            add(deleteButton);
            setBackground(Color.WHITE);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            return this;
        }
    }
    
    // Class untuk editor tombol aksi di tabel
    class ButtonEditor extends DefaultCellEditor {
        private JPanel panel;
        private JButton editButton;
        private JButton deleteButton;
        private String action = "";
        private boolean isPushed;
        private int selectedRow;
        
        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            
            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
            
            editButton = new JButton("Edit");
            editButton.setPreferredSize(new Dimension(65, 30));
            editButton.setBackground(new Color(52, 152, 219));
            editButton.setForeground(Color.WHITE);
            editButton.setFocusPainted(false);
            editButton.setBorderPainted(false);
            editButton.setFont(new Font("Arial", Font.BOLD, 12));
            
            deleteButton = new JButton("Hapus");
            deleteButton.setPreferredSize(new Dimension(65, 30));
            deleteButton.setBackground(new Color(231, 76, 60));
            deleteButton.setForeground(Color.WHITE);
            deleteButton.setFocusPainted(false);
            deleteButton.setBorderPainted(false);
            deleteButton.setFont(new Font("Arial", Font.BOLD, 12));
            
            editButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    action = "Edit";
                    isPushed = true;
                    fireEditingStopped();
                }
            });
            
            deleteButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    action = "Delete";
                    isPushed = true;
                    fireEditingStopped();
                }
            });
            
            panel.add(editButton);
            panel.add(deleteButton);
            panel.setBackground(Color.WHITE);
        }
        
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            selectedRow = row;
            isPushed = false;
            return panel;
        }
        
        @Override
        public Object getCellEditorValue() {
            if (isPushed) {
                // Get user ID from the first column
                int userId = (int) userTable.getValueAt(selectedRow, 0);
                
                if (action.equals("Edit")) {
                    showEditUserDialog(userId, selectedRow);
                } else if (action.equals("Delete")) {
                    deleteUser(userId, selectedRow);
                }
            }
            isPushed = false;
            return "";
        }
        
        @Override
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
    }
    
    private void showEditUserDialog(int userId, int row) {
        JDialog editUserDialog = new JDialog(this, "Edit Pengguna", true);
        editUserDialog.setSize(400, 320);
        editUserDialog.setLocationRelativeTo(this);
        editUserDialog.setLayout(new BorderLayout());
        
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new GridLayout(5, 2, 10, 10));
        formPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Get user data
        String username = (String) userTable.getValueAt(row, 1);
        String email = (String) userTable.getValueAt(row, 2);
        String role = (String) userTable.getValueAt(row, 3);
        
        // Username field
        JLabel lblUsername = new JLabel("Username:");
        JTextField txtUsername = new JTextField(username);
        
        // Email field
        JLabel lblEmail = new JLabel("Email:");
        JTextField txtEmail = new JTextField(email);
        
        // Password field (optional)
        JLabel lblPassword = new JLabel("Password Baru (opsional):");
        JPasswordField txtPassword = new JPasswordField();
        
        // Role selection
        JLabel lblRole = new JLabel("Role:");
        String[] roles = {"user", "supervisor", "admin"};
        JComboBox<String> cmbRole = new JComboBox<>(roles);
        cmbRole.setSelectedItem(role);
        
        formPanel.add(lblUsername);
        formPanel.add(txtUsername);
        formPanel.add(lblEmail);
        formPanel.add(txtEmail);
        formPanel.add(lblPassword);
        formPanel.add(txtPassword);
        formPanel.add(lblRole);
        formPanel.add(cmbRole);
        
        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton btnCancel = new JButton("Batal");
        btnCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editUserDialog.dispose();
            }
        });
        
        JButton btnSave = new JButton("Simpan");
        btnSave.setBackground(new Color(46, 204, 113));
        btnSave.setForeground(Color.WHITE);
        btnSave.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String updatedUsername = txtUsername.getText().trim();
                String updatedEmail = txtEmail.getText().trim();
                String updatedPassword = new String(txtPassword.getPassword());
                String updatedRole = (String) cmbRole.getSelectedItem();
                
                if (updatedUsername.isEmpty() || updatedEmail.isEmpty()) {
                    JOptionPane.showMessageDialog(editUserDialog, 
                            "Username dan email harus diisi!", 
                            "Error", 
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                try {
                    Connection conn = DatabaseConnection.getConnection();
                    
                    // Cek jika username sudah dipakai pengguna lain
                    String checkQuery = "SELECT COUNT(*) FROM users WHERE username = ? AND id != ?";
                    PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
                    checkStmt.setString(1, updatedUsername);
                    checkStmt.setInt(2, userId);
                    ResultSet rs = checkStmt.executeQuery();
                    rs.next();
                    
                    if (rs.getInt(1) > 0) {
                        JOptionPane.showMessageDialog(editUserDialog, 
                                "Username sudah digunakan oleh pengguna lain!", 
                                "Error", 
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    
                    // Update user data
                    String updateQuery;
                    PreparedStatement updateStmt;
                    
                    if (updatedPassword.isEmpty()) {
                        // Update without changing password
                        updateQuery = "UPDATE users SET username = ?, email = ?, role = ? WHERE id = ?";
                        updateStmt = conn.prepareStatement(updateQuery);
                        updateStmt.setString(1, updatedUsername);
                        updateStmt.setString(2, updatedEmail);
                        updateStmt.setString(3, updatedRole);
                        updateStmt.setInt(4, userId);
                    } else {
                        // Update with new password
                        updateQuery = "UPDATE users SET username = ?, email = ?, password = ?, role = ? WHERE id = ?";
                        updateStmt = conn.prepareStatement(updateQuery);
                        updateStmt.setString(1, updatedUsername);
                        updateStmt.setString(2, updatedEmail);
                        updateStmt.setString(3, updatedPassword);
                        updateStmt.setString(4, updatedRole);
                        updateStmt.setInt(5, userId);
                    }
                    
                    int result = updateStmt.executeUpdate();
                    
                    if (result > 0) {
                        // Update table display
                        userTable.setValueAt(updatedUsername, row, 1);
                        userTable.setValueAt(updatedEmail, row, 2);
                        userTable.setValueAt(updatedRole, row, 3);
                        
                        JOptionPane.showMessageDialog(editUserDialog, 
                                "Data pengguna berhasil diperbarui!", 
                                "Sukses", 
                                JOptionPane.INFORMATION_MESSAGE);
                        editUserDialog.dispose();
                    } else {
                        JOptionPane.showMessageDialog(editUserDialog, 
                                "Gagal memperbarui data pengguna!", 
                                "Error", 
                                JOptionPane.ERROR_MESSAGE);
                    }
                    
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(editUserDialog, 
                            "Error database: " + ex.getMessage(), 
                            "Error", 
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        
        buttonPanel.add(btnCancel);
        buttonPanel.add(btnSave);
        
        editUserDialog.add(formPanel, BorderLayout.CENTER);
        editUserDialog.add(buttonPanel, BorderLayout.SOUTH);
        editUserDialog.setVisible(true);
    }
    
    private void deleteUser(int userId, int row) {
        // Confirm deletion
        int confirm = JOptionPane.showConfirmDialog(this, 
                "Apakah Anda yakin ingin menghapus pengguna ini?", 
                "Konfirmasi Hapus", 
                JOptionPane.YES_NO_OPTION, 
                JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                Connection conn = DatabaseConnection.getConnection();
                String deleteQuery = "DELETE FROM users WHERE id = ?";
                PreparedStatement deleteStmt = conn.prepareStatement(deleteQuery);
                deleteStmt.setInt(1, userId);
                
                int result = deleteStmt.executeUpdate();
                
                if (result > 0) {
                    // Remove row from table
                    userTableModel.removeRow(row);
                    
                    JOptionPane.showMessageDialog(this, 
                            "Pengguna berhasil dihapus!", 
                            "Sukses", 
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, 
                            "Gagal menghapus pengguna!", 
                            "Error", 
                            JOptionPane.ERROR_MESSAGE);
                }
                
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, 
                        "Error database: " + e.getMessage(), 
                        "Error", 
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * Metode untuk mengubah role pengguna secara langsung melalui dropdown di tabel
     */
    private void updateUserRole(int userId, String newRole) {
        try {
            Connection conn = DatabaseConnection.getConnection();
            String updateQuery = "UPDATE users SET role = ? WHERE id = ?";
            PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
            updateStmt.setString(1, newRole);
            updateStmt.setInt(2, userId);
            
            int result = updateStmt.executeUpdate();
            
            if (result > 0) {
                JOptionPane.showMessageDialog(this, 
                        "Role pengguna berhasil diperbarui!", 
                        "Sukses", 
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, 
                        "Gagal memperbarui role pengguna!", 
                        "Error", 
                        JOptionPane.ERROR_MESSAGE);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                    "Error database: " + e.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Menampilkan dialog untuk mengubah role pengguna secara massal
     */
    private void showBulkRoleChangeDialog() {
        JDialog bulkRoleDialog = new JDialog(this, "Ubah Role Massal", true);
        bulkRoleDialog.setSize(350, 200);
        bulkRoleDialog.setLocationRelativeTo(this);
        bulkRoleDialog.setLayout(new BorderLayout());
        
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new GridLayout(2, 2, 10, 10));
        formPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Role selection
        JLabel lblCurrentRole = new JLabel("Role Saat Ini:");
        String[] currentRoles = {"user", "supervisor", "admin", "semua"};
        JComboBox<String> cmbCurrentRole = new JComboBox<>(currentRoles);
        
        JLabel lblNewRole = new JLabel("Role Baru:");
        String[] newRoles = {"user", "supervisor", "admin"};
        JComboBox<String> cmbNewRole = new JComboBox<>(newRoles);
        
        formPanel.add(lblCurrentRole);
        formPanel.add(cmbCurrentRole);
        formPanel.add(lblNewRole);
        formPanel.add(cmbNewRole);
        
        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        JButton btnCancel = new JButton("Batal");
        btnCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                bulkRoleDialog.dispose();
            }
        });
        
        JButton btnApply = new JButton("Terapkan");
        btnApply.setBackground(new Color(46, 204, 113));
        btnApply.setForeground(Color.WHITE);
        btnApply.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String currentRole = (String) cmbCurrentRole.getSelectedItem();
                String newRole = (String) cmbNewRole.getSelectedItem();
                
                if (currentRole.equals(newRole) && !currentRole.equals("semua")) {
                    JOptionPane.showMessageDialog(bulkRoleDialog, 
                            "Role baru harus berbeda dengan role saat ini!", 
                            "Error", 
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                try {
                    Connection conn = DatabaseConnection.getConnection();
                    String updateQuery;
                    PreparedStatement updateStmt;
                    
                    if (currentRole.equals("semua")) {
                        // Update all users
                        updateQuery = "UPDATE users SET role = ?";
                        updateStmt = conn.prepareStatement(updateQuery);
                        updateStmt.setString(1, newRole);
                    } else {
                        // Update specific role
                        updateQuery = "UPDATE users SET role = ? WHERE role = ?";
                        updateStmt = conn.prepareStatement(updateQuery);
                        updateStmt.setString(1, newRole);
                        updateStmt.setString(2, currentRole);
                    }
                    
                    int result = updateStmt.executeUpdate();
                    
                    if (result > 0) {
                        JOptionPane.showMessageDialog(bulkRoleDialog, 
                                result + " pengguna berhasil diperbarui!", 
                                "Sukses", 
                                JOptionPane.INFORMATION_MESSAGE);
                        bulkRoleDialog.dispose();
                        loadUserData(); // Refresh table
                    } else {
                        JOptionPane.showMessageDialog(bulkRoleDialog, 
                                "Tidak ada pengguna yang diperbarui!", 
                                "Info", 
                                JOptionPane.INFORMATION_MESSAGE);
                    }
                    
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(bulkRoleDialog, 
                            "Error database: " + ex.getMessage(), 
                            "Error", 
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        
        buttonPanel.add(btnCancel);
        buttonPanel.add(btnApply);
        
        bulkRoleDialog.add(formPanel, BorderLayout.CENTER);
        bulkRoleDialog.add(buttonPanel, BorderLayout.SOUTH);
        bulkRoleDialog.setVisible(true);
    }
    
    /**
     * Menampilkan statistik pengguna seperti jumlah admin, user biasa, dll
     */
    private void showUserStatistics() {
        try {
            Connection conn = DatabaseConnection.getConnection();
            
            // Get total users
            String totalQuery = "SELECT COUNT(*) FROM users";
            PreparedStatement totalStmt = conn.prepareStatement(totalQuery);
            ResultSet totalRs = totalStmt.executeQuery();
            totalRs.next();
            int totalUsers = totalRs.getInt(1);
            
            // Get admin count
            String adminQuery = "SELECT COUNT(*) FROM users WHERE role = 'admin'";
            PreparedStatement adminStmt = conn.prepareStatement(adminQuery);
            ResultSet adminRs = adminStmt.executeQuery();
            adminRs.next();
            int adminCount = adminRs.getInt(1);
            
            // Get supervisor count
            String supervisorQuery = "SELECT COUNT(*) FROM users WHERE role = 'supervisor'";
            PreparedStatement supervisorStmt = conn.prepareStatement(supervisorQuery);
            ResultSet supervisorRs = supervisorStmt.executeQuery();
            supervisorRs.next();
            int supervisorCount = supervisorRs.getInt(1);
            
            // Get user count
            String userQuery = "SELECT COUNT(*) FROM users WHERE role = 'user'";
            PreparedStatement userStmt = conn.prepareStatement(userQuery);
            ResultSet userRs = userStmt.executeQuery();
            userRs.next();
            int userCount = userRs.getInt(1);
            
            // Display statistics
            JOptionPane.showMessageDialog(this,
                    "Statistik Pengguna:\n\n" +
                    "Total Pengguna: " + totalUsers + "\n" +
                    "Jumlah Admin: " + adminCount + "\n" +
                    "Jumlah Supervisor: " + supervisorCount + "\n" +
                    "Jumlah User: " + userCount,
                    "Statistik Pengguna",
                    JOptionPane.INFORMATION_MESSAGE);
            
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                    "Error mengambil statistik: " + e.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Menambahkan fitur untuk mengekspor data pengguna
     */
    private void exportUserData() {
        try {
            // Create a StringBuilder to store CSV data
            StringBuilder csvData = new StringBuilder();
            csvData.append("ID,Username,Email,Role\n");
            
            // Get data from table
            for (int i = 0; i < userTable.getRowCount(); i++) {
                csvData.append(userTable.getValueAt(i, 0)).append(",");
                csvData.append(userTable.getValueAt(i, 1)).append(",");
                csvData.append(userTable.getValueAt(i, 2)).append(",");
                csvData.append(userTable.getValueAt(i, 3)).append("\n");
            }
            
            // Show file save dialog
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Simpan Data Pengguna");
            fileChooser.setSelectedFile(new java.io.File("user_data.csv"));
            
            int userSelection = fileChooser.showSaveDialog(this);
            
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                java.io.File fileToSave = fileChooser.getSelectedFile();
                
                // Add .csv extension if not present
                if (!fileToSave.getName().toLowerCase().endsWith(".csv")) {
                    fileToSave = new java.io.File(fileToSave.getAbsolutePath() + ".csv");
                }
                
                try (java.io.FileWriter writer = new java.io.FileWriter(fileToSave)) {
                    writer.write(csvData.toString());
                    JOptionPane.showMessageDialog(this, 
                            "Data berhasil diekspor ke " + fileToSave.getName(), 
                            "Ekspor Berhasil", 
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (java.io.IOException e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(this, 
                            "Error menulis file: " + e.getMessage(), 
                            "Error", 
                            JOptionPane.ERROR_MESSAGE);
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, 
                    "Error mengekspor data: " + e.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    
    /**
     * Main method untuk menjalankan aplikasi
     */
    public static void main(String[] args) {
        try {
            // Set Look and Feel
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new AdminDashboardUI().setVisible(true);
            }
        });
    }
}