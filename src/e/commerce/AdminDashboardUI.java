package e.commerce;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.table.TableCellRenderer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class AdminDashboardUI extends JFrame {
    private JPanel mainPanel;
    private CardLayout cardLayout;
    private JTable userTable;
    private DefaultTableModel userTableModel;
    private JButton[] navButtons; // To track sidebar buttons

    public AdminDashboardUI() {
        User currentUser = Authentication.getCurrentUser();
        if (currentUser == null || !currentUser.getRole().equalsIgnoreCase("admin")) {
            JOptionPane.showMessageDialog(this, "Akses ditolak! Hanya admin yang dapat mengakses halaman ini.", "Error", JOptionPane.ERROR_MESSAGE);
            LoginUI loginUI = new LoginUI();
            loginUI.setVisible(true);
            dispose();
            return;
        }

        setTitle("Quantra - Admin Dashboard");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());
        
        // Set ikon
        IconUtil.setIcon(this);

        // Sidebar
        JPanel sidebar = createSidebar();

        // Main content area
        mainPanel = new JPanel();
        cardLayout = new CardLayout();
        mainPanel.setLayout(cardLayout);

        // Header
        JPanel headerPanel = createHeaderPanel(currentUser);

        // Dashboard panel
        JPanel dashboardPanel = createAdminDashboardPanel();

        // User management panel
        JPanel userManagementPanel = createUserManagementPanel();

        // Profile panel
        ProfileUI profilePanel = new ProfileUI();

        mainPanel.add(dashboardPanel, "Dashboard");
        mainPanel.add(userManagementPanel, "UserManagement");
        mainPanel.add(profilePanel, "Profile");

        // Content wrapper with header and main panel
        JPanel contentWrapper = new JPanel(new BorderLayout());
        contentWrapper.add(headerPanel, BorderLayout.NORTH);
        contentWrapper.add(mainPanel, BorderLayout.CENTER);

        add(sidebar, BorderLayout.WEST);
        add(contentWrapper, BorderLayout.CENTER);
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(245, 245, 245));
        sidebar.setPreferredSize(new Dimension(200, 0));
        sidebar.setBorder(new EmptyBorder(20, 10, 20, 10));

        // Logo
        JLabel lblLogo = new JLabel("QUANTRA");
        lblLogo.setFont(new Font("Arial", Font.BOLD, 20));
        lblLogo.setForeground(new Color(255, 99, 71));
        lblLogo.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(lblLogo);
        sidebar.add(Box.createRigidArea(new Dimension(0, 30)));

        // Navigation items
        String[] navItems = {"Dashboard", "User Management", "Profile", "Settings"};
        String[] panelNames = {"Dashboard", "UserManagement", "Profile", "Profile"};
        navButtons = new JButton[navItems.length]; // Initialize array to store buttons

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

            // Initially highlight "Dashboard"
            if (navItems[i].equals("Dashboard")) {
                navButton.setBackground(new Color(255, 99, 71));
                navButton.setForeground(Color.WHITE);
            }

            navButtons[i] = navButton; // Store button in array

            final String panelName = panelNames[i];
            final int index = i;
            navButton.addActionListener(e -> {
                // Update button colors
                for (JButton btn : navButtons) {
                    btn.setBackground(new Color(245, 245, 245));
                    btn.setForeground(Color.BLACK);
                }
                navButtons[index].setBackground(new Color(255, 99, 71));
                navButtons[index].setForeground(Color.WHITE);

                // Switch panel
                cardLayout.show(mainPanel, panelName);
                
                // Refresh user data if navigating to UserManagement
                if (panelName.equals("UserManagement")) {
                    loadUserData();
                }
            });

            sidebar.add(navButton);
            sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        // Spacer to push Help to bottom
        sidebar.add(Box.createVerticalGlue());

        // Help button
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

        // Search bar
        JTextField searchField = new JTextField("Search...");
        searchField.setForeground(Color.GRAY);
        searchField.setPreferredSize(new Dimension(300, 30));
        searchField.setBorder(new LineBorder(Color.LIGHT_GRAY, 1, true));
        searchField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (searchField.getText().equals("Search...")) {
                    searchField.setText("");
                    searchField.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (searchField.getText().isEmpty()) {
                    searchField.setText("Search...");
                    searchField.setForeground(Color.GRAY);
                }
            }
        });
        headerPanel.add(searchField, BorderLayout.WEST);

        // Right side (user profile)
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
            int response = JOptionPane.showConfirmDialog(AdminDashboardUI.this,
                    "Apakah Anda yakin ingin logout?",
                    "Konfirmasi",
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

    private int getTotalUserCountByRole(String role) {
        int count = 0;
        String query = "SELECT COUNT(*) FROM users WHERE role = ?";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            stmt = conn.prepareStatement(query);
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
        JPanel dashboardPanel = new JPanel(new BorderLayout());
        dashboardPanel.setBackground(Color.WHITE);
        dashboardPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Title panel
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(Color.WHITE);

        JLabel lblTitle = new JLabel("Dashboard Admin");
        lblTitle.setFont(new Font("Arial", Font.BOLD, 24));
        titlePanel.add(lblTitle, BorderLayout.WEST);

        // Stats panel
        JPanel statsPanel = new JPanel(new GridLayout(1, 3, 15, 0));
        statsPanel.setBackground(Color.WHITE);
        statsPanel.setBorder(new EmptyBorder(20, 0, 20, 0));

        // Stat cards
        int totalUserOnly = getTotalUserCountByRole("user");
        statsPanel.add(createStatCard("Total Pengguna", String.valueOf(totalUserOnly), new Color(255, 99, 71)));
        statsPanel.add(createStatCard("Total Produk", "48", new Color(255, 99, 71)));
        statsPanel.add(createStatCard("Total Transaksi", "312", new Color(255, 99, 71)));

        dashboardPanel.add(titlePanel, BorderLayout.NORTH);
        dashboardPanel.add(statsPanel, BorderLayout.CENTER);

        return dashboardPanel;
    }

    private JPanel createStatCard(String title, String value, Color color) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220), 1));

        // Header with color
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(color);
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

        card.add(headerPanel, BorderLayout.NORTH);
        card.add(contentPanel, BorderLayout.CENTER);

        return card;
    }

    private JPanel createUserManagementPanel() {
        JPanel userManagementPanel = new JPanel(new BorderLayout());
        userManagementPanel.setBackground(Color.WHITE);
        userManagementPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Title panel
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(Color.WHITE);

        JLabel lblTitle = new JLabel("Kelola Pengguna");
        lblTitle.setFont(new Font("Arial", Font.BOLD, 20));
        titlePanel.add(lblTitle, BorderLayout.WEST);

        // Search and add user panel
        JPanel actionPanel = new JPanel(new BorderLayout());
        actionPanel.setBackground(Color.WHITE);
        actionPanel.setBorder(new EmptyBorder(10, 0, 10, 0));

        // Search field
        JTextField searchField = new JTextField(20);
        searchField.setBorder(new LineBorder(Color.LIGHT_GRAY, 1, true));
        searchField.setPreferredSize(new Dimension(200, 30));

        JButton searchButton = new JButton("Cari");
        searchButton.setBackground(new Color(255, 99, 71));
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
        addUserButton.setBackground(new Color(255, 99, 71));
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
        userTable.setFont(new Font("Arial", Font.PLAIN, 14));
        userTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        userTable.getTableHeader().setBackground(new Color(255, 99, 71));
        userTable.getTableHeader().setForeground(Color.WHITE);

        // Custom renderer for action buttons
        userTable.getColumnModel().getColumn(4).setCellRenderer(new ButtonRenderer());
        userTable.getColumnModel().getColumn(4).setCellEditor(new ButtonEditor(new JCheckBox()));

        JScrollPane scrollPane = new JScrollPane(userTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        userManagementPanel.add(titlePanel, BorderLayout.NORTH);
        userManagementPanel.add(actionPanel, BorderLayout.CENTER);
        userManagementPanel.add(scrollPane, BorderLayout.SOUTH);

        return userManagementPanel;
    }

    private void loadUserData() {
        userTableModel.setRowCount(0);

        try {
            Connection conn = DatabaseConnection.getConnection();
            String query = "SELECT * FROM users ORDER BY id";
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Object[] row = new Object[5];
                row[0] = rs.getInt("id");
                row[1] = rs.getString("username");
                row[2] = rs.getString("email");
                row[3] = rs.getString("role");
                row[4] = "Aksi";
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
        userTableModel.setRowCount(0);

        try {
            Connection conn = DatabaseConnection.getConnection();
            String query = "SELECT * FROM users WHERE username LIKE ? OR email LIKE ? OR role LIKE ? ORDER BY id";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, "%" + keyword + "%");
            stmt.setString(2, "%" + keyword + "%");
            stmt.setString(3, "%" + keyword + "%");
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Object[] row = new Object[5];
                row[0] = rs.getInt("id");
                row[1] = rs.getString("username");
                row[2] = rs.getString("email");
                row[3] = rs.getString("role");
                row[4] = "Aksi";
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

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return password;
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

        JLabel lblUsername = new JLabel("Username:");
        JTextField txtUsername = new JTextField(20);

        JLabel lblEmail = new JLabel("Email:");
        JTextField txtEmail = new JTextField(20);

        JLabel lblPassword = new JLabel("Password:");
        JPasswordField txtPassword = new JPasswordField(20);

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

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton btnCancel = new JButton("Batal");
        btnCancel.setBackground(new Color(108, 117, 125));
        btnCancel.setForeground(Color.WHITE);
        btnCancel.setFocusPainted(false);
        btnCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addUserDialog.dispose();
            }
        });

        JButton btnSave = new JButton("Simpan");
        btnSave.setBackground(new Color(255, 99, 71));
        btnSave.setForeground(Color.WHITE);
        btnSave.setFocusPainted(false);
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

                String hashedPassword = hashPassword(password);

                try {
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

                    String insertQuery = "INSERT INTO users (username, email, password, role) VALUES (?, ?, ?, ?)";
                    PreparedStatement insertStmt = conn.prepareStatement(insertQuery);
                    insertStmt.setString(1, username);
                    insertStmt.setString(2, email);
                    insertStmt.setString(3, hashedPassword);
                    insertStmt.setString(4, role);

                    int result = insertStmt.executeUpdate();

                    if (result > 0) {
                        JOptionPane.showMessageDialog(addUserDialog,
                                "Pengguna berhasil ditambahkan!",
                                "Sukses",
                                JOptionPane.INFORMATION_MESSAGE);
                        addUserDialog.dispose();
                        loadUserData();
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

    class ButtonRenderer extends JPanel implements TableCellRenderer {
        private JButton editButton;
        private JButton deleteButton;

        public ButtonRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));

            editButton = new JButton("Edit");
            editButton.setBackground(new Color(255, 99, 71));
            editButton.setForeground(Color.WHITE);
            editButton.setFocusPainted(false);
            editButton.setBorderPainted(false);

            deleteButton = new JButton("Hapus");
            deleteButton.setBackground(new Color(220, 53, 69));
            deleteButton.setForeground(Color.WHITE);
            deleteButton.setFocusPainted(false);
            deleteButton.setBorderPainted(false);

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
            editButton.setBackground(new Color(255, 99, 71));
            editButton.setForeground(Color.WHITE);
            editButton.setFocusPainted(false);
            editButton.setBorderPainted(false);

            deleteButton = new JButton("Hapus");
            deleteButton.setBackground(new Color(220, 53, 69));
            deleteButton.setForeground(Color.WHITE);
            deleteButton.setFocusPainted(false);
            deleteButton.setBorderPainted(false);

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
        editUserDialog.setSize(400, 250);
        editUserDialog.setLocationRelativeTo(this);
        editUserDialog.setLayout(new BorderLayout());

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new GridLayout(4, 2, 10, 10));
        formPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        String username = (String) userTable.getValueAt(row, 1);
        String email = (String) userTable.getValueAt(row, 2);
        String role = (String) userTable.getValueAt(row, 3);

        JLabel lblUsername = new JLabel("Username:");
        JTextField txtUsername = new JTextField(username);

        JLabel lblEmail = new JLabel("Email:");
        JTextField txtEmail = new JTextField(email);

        JLabel lblRole = new JLabel("Role:");
        String[] roles = {"user", "supervisor", "admin"};
        JComboBox<String> cmbRole = new JComboBox<>(roles);
        cmbRole.setSelectedItem(role);

        formPanel.add(lblUsername);
        formPanel.add(txtUsername);
        formPanel.add(lblEmail);
        formPanel.add(txtEmail);
        formPanel.add(lblRole);
        formPanel.add(cmbRole);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton btnCancel = new JButton("Batal");
        btnCancel.setBackground(new Color(108, 117, 125));
        btnCancel.setForeground(Color.WHITE);
        btnCancel.setFocusPainted(false);
        btnCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editUserDialog.dispose();
            }
        });

        JButton btnSave = new JButton("Simpan");
        btnSave.setBackground(new Color(255, 99, 71));
        btnSave.setForeground(Color.WHITE);
        btnSave.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String updatedUsername = txtUsername.getText().trim();
                String updatedEmail = txtEmail.getText().trim();
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

                    String updateQuery = "UPDATE users SET username = ?, email = ?, role = ? WHERE id = ?";
                    PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
                    updateStmt.setString(1, updatedUsername);
                    updateStmt.setString(2, updatedEmail);
                    updateStmt.setString(3, updatedRole);
                    updateStmt.setInt(4, userId);

                    int result = updateStmt.executeUpdate();

                    if (result > 0) {
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

    private void showBulkRoleChangeDialog() {
        JDialog bulkRoleDialog = new JDialog(this, "Ubah Role Massal", true);
        bulkRoleDialog.setSize(350, 200);
        bulkRoleDialog.setLocationRelativeTo(this);
        bulkRoleDialog.setLayout(new BorderLayout());

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new GridLayout(2, 2, 10, 10));
        formPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

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

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton btnCancel = new JButton("Batal");
        btnCancel.setBackground(new Color(108, 117, 125));
        btnCancel.setForeground(Color.WHITE);
        btnCancel.setFocusPainted(false);
        btnCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                bulkRoleDialog.dispose();
            }
        });

        JButton btnApply = new JButton("Terapkan");
        btnApply.setBackground(new Color(255, 99, 71));
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
                        updateQuery = "UPDATE users SET role = ?";
                        updateStmt = conn.prepareStatement(updateQuery);
                        updateStmt.setString(1, newRole);
                    } else {
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
                        loadUserData();
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

    private void showUserStatistics() {
        try {
            Connection conn = DatabaseConnection.getConnection();

            String totalQuery = "SELECT COUNT(*) FROM users";
            PreparedStatement totalStmt = conn.prepareStatement(totalQuery);
            ResultSet totalRs = totalStmt.executeQuery();
            totalRs.next();
            int totalUsers = totalRs.getInt(1);

            String adminQuery = "SELECT COUNT(*) FROM users WHERE role = 'admin'";
            PreparedStatement adminStmt = conn.prepareStatement(adminQuery);
            ResultSet adminRs = adminStmt.executeQuery();
            adminRs.next();
            int adminCount = adminRs.getInt(1);

            String supervisorQuery = "SELECT COUNT(*) FROM users WHERE role = 'supervisor'";
            PreparedStatement supervisorStmt = conn.prepareStatement(supervisorQuery);
            ResultSet supervisorRs = supervisorStmt.executeQuery();
            supervisorRs.next();
            int supervisorCount = supervisorRs.getInt(1);

            String userQuery = "SELECT COUNT(*) FROM users WHERE role = 'user'";
            PreparedStatement userStmt = conn.prepareStatement(userQuery);
            ResultSet userRs = userStmt.executeQuery();
            userRs.next();
            int userCount = userRs.getInt(1);

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

    private void exportUserData() {
        try {
            StringBuilder csvData = new StringBuilder();
            csvData.append("ID,Username,Email,Role\n");

            for (int i = 0; i < userTable.getRowCount(); i++) {
                csvData.append(userTable.getValueAt(i, 0)).append(",");
                csvData.append(userTable.getValueAt(i, 1)).append(",");
                csvData.append(userTable.getValueAt(i, 2)).append(",");
                csvData.append(userTable.getValueAt(i, 3)).append("\n");
            }

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Simpan Data Pengguna");
            fileChooser.setSelectedFile(new java.io.File("user_data.csv"));

            int userSelection = fileChooser.showSaveDialog(this);

            if (userSelection == JFileChooser.APPROVE_OPTION) {
                java.io.File fileToSave = fileChooser.getSelectedFile();

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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AdminDashboardUI().setVisible(true));
    }
}