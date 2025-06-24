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

// Import untuk JFreeChart
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import org.jfree.chart.ui.RectangleInsets;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

// Missing JFreeChart imports
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import org.jfree.chart.util.Rotation;
import java.awt.BasicStroke;
import javax.swing.border.Border;

// IMPOR BARU UNTUK GRAFIK GARIS (tetap ada jika nanti ada grafik garis lain)
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.category.BarRenderer; // BARU: Untuk grafik batang
import java.util.Locale; // Import for currency formatting

public class ManagerDashboardUI extends JFrame {
    private JPanel mainPanel;
    private CardLayout cardLayout;
    private JTable userTable;
    private DefaultTableModel userTableModel;
    private JButton[] navButtons;

    // Label untuk menampilkan data dashboard
    private JLabel lblTotalUsersValue;
    private JLabel lblTotalProductsValue;
    private JLabel lblTotalTransactionsValue; // Label for Total Transactions COUNT (kept as is)
    private JLabel lblTotalRevenueValue; // NEW Label for Total Transactions IN RUPIAH (repurposed lblJuneRevenueValue)

    public ManagerDashboardUI() {
        User currentUser = Authentication.getCurrentUser();
        if (currentUser == null || (!currentUser.getRole().equalsIgnoreCase("admin") && !currentUser.getRole().equalsIgnoreCase("op_manager"))) {
            JOptionPane.showMessageDialog(this, "Akses ditolak! Hanya admin atau Operation Manager yang dapat mengakses halaman ini.", "Error", JOptionPane.ERROR_MESSAGE);
            LoginUI loginUI = new LoginUI();
            loginUI.setVisible(true);
            dispose();
            return;
        }

        setTitle("Quantra - Operation Manager Dashboard");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());

        getContentPane().setBackground(Color.WHITE);

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

        loadUserData();
        refreshDashboardData();
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(245, 245, 245));
        sidebar.setPreferredSize(new Dimension(200, 0));
        sidebar.setBorder(new EmptyBorder(20, 10, 20, 10));

        // Logo
        JLabel lblLogo = new JLabel();
        ImageIcon logoIcon = new ImageIcon(getClass().getResource("/Resources/Images/Logo.png"));
        Image scaledImage = logoIcon.getImage().getScaledInstance(100, 35, Image.SCALE_SMOOTH);
        lblLogo.setIcon(new ImageIcon(scaledImage));
        lblLogo.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(lblLogo);
        sidebar.add(Box.createRigidArea(new Dimension(0, 30)));

        // Navigation items
        String[] navItems = {"Dashboard", "User Management", "Profile", "Settings"};
        String[] panelNames = {"Dashboard", "UserManagement", "Profile", "Profile"};
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

            if (navItems[i].equals("Dashboard")) {
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

                if (panelName.equals("UserManagement")) {
                    loadUserData();
                }
                if (panelName.equals("Dashboard")) {
                    refreshDashboardData();
                }
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
        headerPanel.setOpaque(true);
        headerPanel.setPreferredSize(new Dimension(0, 60));
        headerPanel.setBorder(new EmptyBorder(10, 20, 10, 20));

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

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.setBackground(Color.WHITE);
        rightPanel.setOpaque(true);
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
            int response = JOptionPane.showConfirmDialog(ManagerDashboardUI.this,
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

    private void refreshDashboardData() {
        int totalUsers = getTotalUserCount();
        int totalProducts = ProductRepository.getTotalProductCount();
        // This is the count of transactions (kept as is)
        int totalTransactionsCount = ProductRepository.getTotalOrderCount();
        // Get total revenue from the database
        double totalRevenue = getTotalRevenue();

        lblTotalUsersValue.setText(String.valueOf(totalUsers));
        lblTotalProductsValue.setText(String.valueOf(totalProducts));
        // Update the label for transaction count (which is the third card)
        lblTotalTransactionsValue.setText(String.valueOf(totalTransactionsCount));

        // Update the label that was "Pendapatan Juni 2025" to show Total Transaksi in Rupiah
        // Format the revenue as Rupiah
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        lblTotalRevenueValue.setText(currencyFormat.format(totalRevenue));
    }

    // New method to get total revenue from the database
    private double getTotalRevenue() {
        double totalRevenue = 0.0;
        // Assuming your 'orders' table has a 'total_amount' column
        String query = "SELECT SUM(total_amount) FROM orders";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            stmt = conn.prepareStatement(query);
            rs = stmt.executeQuery();

            if (rs.next()) {
                totalRevenue = rs.getDouble(1);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error mengambil total pendapatan: " + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        } finally {
            DatabaseConnection.closeConnection(conn, stmt, rs);
        }

        return totalRevenue;
    }

    private PieDataset createDataset() {
        DefaultPieDataset dataset = new DefaultPieDataset();
        dataset.setValue("Admin", getTotalUserCountByRole("admin"));
        dataset.setValue("Seller", getTotalUserCountByRole("supervisor"));
        dataset.setValue("Operation Manager", getTotalUserCountByRole("op_manager"));
        dataset.setValue("User", getTotalUserCountByRole("user"));
        return dataset;
    }

    private JFreeChart createPieChartInternal() {
        PieDataset dataset = createDataset();
        JFreeChart chart = ChartFactory.createPieChart(
                "Distribusi Pengguna",
                dataset,
                false,
                true,
                false
        );

        chart.setBackgroundPaint(Color.WHITE);
        chart.setBorderVisible(false);

        chart.getTitle().setFont(new Font("Inter", Font.PLAIN, 20));
        chart.getTitle().setPaint(new Color(30, 30, 30));
        chart.getTitle().setPosition(RectangleEdge.TOP);
        chart.getTitle().setHorizontalAlignment(HorizontalAlignment.LEFT);

        PiePlot plot = (PiePlot) chart.getPlot();

        Color adminColor = new Color(99, 102, 241);
        Color sellerColor = new Color(16, 185, 129);
        Color opManagerColor = new Color(255, 193, 7);
        Color userColor = new Color(245, 158, 11);

        plot.setSectionPaint("Admin", adminColor);
        plot.setSectionPaint("Seller", sellerColor);
        plot.setSectionPaint("Operation Manager", opManagerColor);
        plot.setSectionPaint("User", userColor);

        plot.setOutlineVisible(false);
        plot.setShadowPaint(null);
        plot.setBackgroundPaint(Color.WHITE);
        plot.setBackgroundAlpha(0.0f);

        plot.setLabelFont(new Font("Inter", Font.PLAIN, 11));
        plot.setLabelPaint(new Color(75, 85, 99));
        plot.setLabelBackgroundPaint(new Color(255, 255, 255, 200));
        plot.setLabelOutlinePaint(null);
        plot.setLabelShadowPaint(null);
        plot.setLabelLinkPaint(new Color(209, 213, 219));
        plot.setLabelLinkStroke(new BasicStroke(1.0f));

        plot.setLabelGenerator(new StandardPieSectionLabelGenerator(
            "{0}: {2}\n({1})",
            NumberFormat.getNumberInstance(),
            new DecimalFormat("0.0%")
        ));

        plot.setCircular(true);
        plot.setInteriorGap(0.02);
        plot.setStartAngle(90);
        plot.setDirection(Rotation.CLOCKWISE);

        plot.setLabelGap(0.05);
        plot.setMaximumLabelWidth(0.20);

        plot.setExplodePercent("Admin", 0.02);

        plot.setNoDataMessage("Belum ada data pengguna");
        plot.setNoDataMessageFont(new Font("Inter", Font.ITALIC, 14));
        plot.setNoDataMessagePaint(new Color(156, 163, 175));

        try {
            chart.setPadding(new RectangleInsets(20, 30, 20, 30));
        } catch (Exception e) {
            System.out.println("Chart padding tidak dapat diset - versi JFreeChart mungkin lama");
        }

        return chart;
    }

    private ChartPanel createModernChartPanel(JFreeChart chart) {
        ChartPanel chartPanel = new ChartPanel(chart) {
            @Override
            public void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                g2.setColor(Color.WHITE);
                g2.fillRect(0, 0, getWidth(), getHeight());

                super.paintComponent(g2);
                g2.dispose();
            }
        };

        chartPanel.setBackground(Color.WHITE);
        chartPanel.setOpaque(true);
        chartPanel.setBorder(null);

        chartPanel.setDomainZoomable(false);
        chartPanel.setRangeZoomable(false);
        chartPanel.setPopupMenu(null);

        return chartPanel;
    }

    private JPanel createModernChartWrapper() {
        final JFreeChart chart = createPieChartInternal();

        JPanel chartWrapperPanel = new JPanel(new BorderLayout());
        chartWrapperPanel.setBackground(Color.WHITE);
        chartWrapperPanel.setOpaque(true);

        chartWrapperPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(229, 231, 235), 1),
            BorderFactory.createEmptyBorder(24, 24, 24, 24)
        ));

        chartWrapperPanel.setPreferredSize(new Dimension(480, 320));

        ChartPanel chartPanel = createModernChartPanel(chart);
        chartWrapperPanel.add(chartPanel, BorderLayout.CENTER);

        return chartWrapperPanel;
    }

    class RoundedBorder implements Border {
        private int radius;
        private Color color;

        public RoundedBorder(int radius, Color color) {
            this.radius = radius;
            this.color = color;
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(1, 1, 1, 1);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2.dispose();
        }
    }

    private JPanel createAdminDashboardPanel() {
        JPanel dashboardPanel = new JPanel(new BorderLayout());
        dashboardPanel.setBackground(Color.WHITE);
        dashboardPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel summaryCardsPanel = new JPanel(new GridLayout(1, 4, 20, 0));
        summaryCardsPanel.setBackground(Color.WHITE);

        lblTotalUsersValue = new JLabel("0");
        lblTotalProductsValue = new JLabel("0");
        lblTotalTransactionsValue = new JLabel("0"); // This remains for the transaction count card
        lblTotalRevenueValue = new JLabel("0"); // This is the new label for total revenue in Rupiah

        summaryCardsPanel.add(createSummaryCard("Total Pengguna", lblTotalUsersValue, new Color(255, 99, 71)));
        summaryCardsPanel.add(createSummaryCard("Total Produk", lblTotalProductsValue, new Color(255, 99, 71)));
        // Card for Total Transactions (Count) - Keep as is
        summaryCardsPanel.add(createSummaryCard("Total Transaksi", lblTotalTransactionsValue, new Color(255, 99, 71)));
        // Card for Total Transaksi (Rupiah) - Repurpose the original "Pendapatan Juni 2025" slot
        // The image_c362e2.png shows a green card with "Pendapatan Juni 2025"
        summaryCardsPanel.add(createSummaryCard("Total Transaksi", lblTotalRevenueValue, new Color(76, 175, 80))); // Green color from image

        dashboardPanel.add(summaryCardsPanel, BorderLayout.NORTH);

        JPanel chartAndDetailsPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        chartAndDetailsPanel.setBackground(Color.WHITE);
        chartAndDetailsPanel.setOpaque(true);
        chartAndDetailsPanel.setBorder(new EmptyBorder(20, 0, 0, 0));

        chartAndDetailsPanel.add(createModernChartWrapper());
        chartAndDetailsPanel.add(createMonthlySalesTrendChartPanel());

        dashboardPanel.add(chartAndDetailsPanel, BorderLayout.CENTER);

        return dashboardPanel;
    }

    private JPanel createSummaryCard(String title, JLabel valueLabel, Color color) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(color);
        card.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        card.setPreferredSize(new Dimension(200, 100));
        card.setMaximumSize(new Dimension(200, 100));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 16));
        lblTitle.setForeground(Color.WHITE);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        valueLabel.setFont(new Font("Arial", Font.BOLD, 28));
        valueLabel.setForeground(Color.WHITE);
        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(lblTitle);
        card.add(Box.createVerticalStrut(10));
        card.add(valueLabel);
        return card;
    }

    // Metode untuk membuat panel grafik batang Pendapatan Bulanan
    private JPanel createMonthlySalesTrendChartPanel() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        // --- BAGIAN PENGAMBILAN DATA DARI DATABASE ---
        // PASTIKAN NAMA TABEL ('orders') DAN KOLOM ('order_date', 'total_amount') SESUAI DENGAN DATABASE ANDA
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 // Kueri ini untuk MySQL atau PostgreSQL. Sesuaikan jika Anda menggunakan SQL Server (FORMAT(order_date, 'yyyy-MM'))
                 "SELECT DATE_FORMAT(order_date, '%Y-%m') AS sales_month, SUM(total_amount) AS monthly_revenue FROM orders GROUP BY sales_month ORDER BY sales_month"
             );
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String month = rs.getString("sales_month");
                double revenue = rs.getDouble("monthly_revenue");
                dataset.addValue(revenue, "Pendapatan", month);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            // Tampilkan pesan error di konsol atau di UI jika gagal mengambil data
            System.err.println("Error fetching monthly sales data from database: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Gagal memuat data penjualan bulanan dari database.\n" + e.getMessage(),
                    "Error Database",
                    JOptionPane.ERROR_MESSAGE);
        }
        // --- AKHIR BAGIAN PENGAMBILAN DATA DARI DATABASE ---

        JFreeChart barChart = ChartFactory.createBarChart(
                "Pendapatan Bulanan",
                "Bulan",
                "Pendapatan (Rp)",
                dataset,
                org.jfree.chart.plot.PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        barChart.setBackgroundPaint(Color.WHITE);
        barChart.getTitle().setFont(new Font("Inter", Font.BOLD, 18));
        barChart.getTitle().setPaint(new Color(30, 30, 30));

        CategoryPlot plot = (CategoryPlot) barChart.getPlot();
        plot.setBackgroundPaint(new Color(245, 245, 245));
        plot.setOutlineVisible(false);

        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(new Color(200, 200, 200));
        plot.setRangeGridlinesVisible(true);
        plot.setDomainGridlinesVisible(true);

        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setDrawBarOutline(false);

        // --- WARNA BATANG SENADA DENGAN APLIKASI (menggunakan warna oranye utama) ---
        // Ensure solid color, no gradient
        renderer.setSeriesPaint(0, new Color(255, 99, 71)); // Using your app's main orange color
        renderer.setMaximumBarWidth(0.3); // Membuat batang lebih ramping (30% dari lebar kategori)
        // --- AKHIR WARNA BATANG ---

        // To make bars left-aligned and solid color:
        // Set item margin to 0.0 and adjust category and lower margins to simulate left alignment.
        plot.getDomainAxis().setCategoryMargin(0.0); // No gap between categories for left alignment effect
        plot.getDomainAxis().setLowerMargin(0.0); // Start bars immediately from the left
        plot.getDomainAxis().setUpperMargin(0.9); // Push bars to the left by increasing upper margin
        renderer.setItemMargin(0.0); // Ensure bars are tightly packed if multiple series exist (not applicable here, but good practice)

        plot.getDomainAxis().setTickLabelFont(new Font("Arial", Font.PLAIN, 12));
        plot.getDomainAxis().setLabelFont(new Font("Arial", Font.BOLD, 14));
        plot.getRangeAxis().setTickLabelFont(new Font("Arial", Font.PLAIN, 12));
        plot.getRangeAxis().setLabelFont(new Font("Arial", Font.BOLD, 14));
        plot.getRangeAxis().setTickLabelPaint(new Color(75, 85, 99));
        
        plot.getDomainAxis().setAxisLineVisible(false);
        plot.getRangeAxis().setAxisLineVisible(false);

        ChartPanel chartPanel = createModernChartPanel(barChart);
        chartPanel.setPreferredSize(new Dimension(600, 350));

        JPanel chartWrapper = new JPanel(new BorderLayout());
        chartWrapper.setBackground(Color.WHITE);
        chartWrapper.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(229, 231, 235), 1),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        chartWrapper.add(chartPanel, BorderLayout.CENTER);
        return chartWrapper;
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

    private int getTotalUserCount() {
        int count = 0;
        String query = "SELECT COUNT(*) FROM users";

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseConnection.getConnection();
            stmt = conn.prepareStatement(query);
            rs = stmt.executeQuery();

            if (rs.next()) {
                count = rs.getInt(1);
            }

        } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                    "Error mengambil total pengguna: " + e.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        } finally {
            DatabaseConnection.closeConnection(conn, stmt, rs);
        }

        return count;
    }

    private JPanel createUserManagementPanel() {
        JPanel userManagementPanel = new JPanel(new BorderLayout());
        userManagementPanel.setBackground(Color.WHITE);
        userManagementPanel.setOpaque(true);
        userManagementPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(Color.WHITE);
        titlePanel.setOpaque(true);
        JLabel lblTitle = new JLabel("Kelola Pengguna");
        lblTitle.setFont(new Font("Arial", Font.BOLD, 20));
        titlePanel.add(lblTitle, BorderLayout.WEST);

        JPanel actionPanel = new JPanel(new BorderLayout());
        actionPanel.setBackground(Color.WHITE);
        actionPanel.setOpaque(true);
        actionPanel.setBorder(new EmptyBorder(10, 0, 10, 0));

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

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.setBackground(Color.WHITE);
        searchPanel.setOpaque(true);
        searchPanel.add(searchField);
        searchPanel.add(searchButton);

        JPanel addPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        addPanel.setBackground(Color.WHITE);
        addPanel.setOpaque(true);
        addPanel.add(addUserButton);

        actionPanel.add(searchPanel, BorderLayout.WEST);
        actionPanel.add(addPanel, BorderLayout.EAST);

        String[] columnNames = {"ID", "Username", "Email", "Role", "Status", "Aksi"};
        userTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5;
            }
            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 5) {
                    return JPanel.class;
                }
                return super.getColumnClass(column);
            }
        };

        userTable = new JTable(userTableModel);
        userTable.setRowHeight(40);
        userTable.setFont(new Font("Arial", Font.PLAIN, 14));
        userTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        userTable.getTableHeader().setBackground(new Color(255, 99, 71));
        userTable.getTableHeader().setForeground(Color.WHITE);

        userTable.getColumnModel().getColumn(5).setCellRenderer(new ButtonRenderer());
        userTable.getColumnModel().getColumn(5).setCellEditor(new ButtonEditor(new JCheckBox()));

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
            String query = "SELECT id, username, email, role, is_verified FROM users ORDER BY id";
            PreparedStatement stmt = conn.prepareStatement(query);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Object[] row = new Object[6];
                row[0] = rs.getInt("id");
                row[1] = rs.getString("username");
                row[2] = rs.getString("email");
                String role = rs.getString("role");
                if (role.equalsIgnoreCase("supervisor")) {
                    row[3] = "Seller";
                } else if (role.equalsIgnoreCase("op_manager")) {
                    row[3] = "Operation Manager";
                } else {
                    row[3] = role;
                }
                row[4] = rs.getBoolean("is_verified") ? "Aktif" : "Nonaktif";
                row[5] = "Aksi";
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
            String query = "SELECT id, username, email, role, is_verified FROM users WHERE username LIKE ? OR email LIKE ? OR role LIKE ? ORDER BY id";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, "%" + keyword + "%");
            stmt.setString(2, "%" + keyword + "%");
            stmt.setString(3, "%" + keyword + "%");
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Object[] row = new Object[6];
                row[0] = rs.getInt("id");
                row[1] = rs.getString("username");
                row[2] = rs.getString("email");
                String role = rs.getString("role");
                if (role.equalsIgnoreCase("supervisor")) {
                    row[3] = "Seller";
                } else if (role.equalsIgnoreCase("op_manager")) {
                    row[3] = "Operation Manager";
                } else {
                    row[3] = role;
                }
                row[4] = rs.getBoolean("is_verified") ? "Aktif" : "Nonaktif";
                row[5] = "Aksi";
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
        addUserDialog.setSize(400, 350);
        addUserDialog.setLocationRelativeTo(this);
        addUserDialog.setLayout(new BorderLayout());

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new GridLayout(6, 2, 10, 10));
        formPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel lblUsername = new JLabel("Username:");
        JTextField txtUsername = new JTextField(20);

        JLabel lblEmail = new JLabel("Email:");
        JTextField txtEmail = new JTextField(20);

        JLabel lblPassword = new JLabel("Password:");
        JPasswordField txtPassword = new JPasswordField(20);

        JLabel lblRole = new JLabel("Role:");
        String[] internalRoles = {"user", "supervisor", "op_manager", "admin"};
        JComboBox<String> cmbRole = new JComboBox<>(internalRoles);
        cmbRole.setRenderer(new BasicComboBoxRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value != null) {
                    if (value.toString().equalsIgnoreCase("supervisor")) {
                        setText("Seller");
                    } else if (value.toString().equalsIgnoreCase("op_manager")) {
                        setText("Operation Manager");
                    } else {
                        setText(value.toString());
                    }
                } else {
                    setText("");
                }
                return this;
            }
        });

        JLabel lblIsVerified = new JLabel("Status Aktif:");
        JCheckBox chkIsVerified = new JCheckBox();
        chkIsVerified.setSelected(true);

        formPanel.add(lblUsername);
        formPanel.add(txtUsername);
        formPanel.add(lblEmail);
        formPanel.add(txtEmail);
        formPanel.add(lblPassword);
        formPanel.add(txtPassword);
        formPanel.add(lblRole);
        formPanel.add(cmbRole);
        formPanel.add(lblIsVerified);
        formPanel.add(chkIsVerified);

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
                boolean isVerified = chkIsVerified.isSelected();

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

                    String insertQuery = "INSERT INTO users (username, email, password, role, is_verified) VALUES (?, ?, ?, ?, ?)";
                    PreparedStatement insertStmt = conn.prepareStatement(insertQuery);
                    insertStmt.setString(1, username);
                    insertStmt.setString(2, email);
                    insertStmt.setString(3, hashedPassword);
                    insertStmt.setString(4, role);
                    insertStmt.setBoolean(5, isVerified);

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
        private JButton editRoleButton;
        private JButton toggleStatusButton;

        public ButtonRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 5, 0));

            editRoleButton = new JButton("Edit");
            editRoleButton.setBackground(new Color(255, 99, 71));
            editRoleButton.setForeground(Color.WHITE);
            editRoleButton.setFocusPainted(false);
            editRoleButton.setBorderPainted(false);
            editRoleButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

            toggleStatusButton = new JButton();
            toggleStatusButton.setForeground(Color.WHITE);
            toggleStatusButton.setFocusPainted(false);
            toggleStatusButton.setBorderPainted(false);
            toggleStatusButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

            add(editRoleButton);
            add(toggleStatusButton);
            setBackground(Color.WHITE);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            String status = (String) table.getModel().getValueAt(row, 4);
            if ("Aktif".equals(status)) {
                toggleStatusButton.setText("Nonaktifkan");
                toggleStatusButton.setBackground(new Color(220, 53, 69));
            } else {
                toggleStatusButton.setText("Aktifkan");
                toggleStatusButton.setBackground(new Color(40, 167, 69));
            }
            return this;
        }
    }

    class ButtonEditor extends DefaultCellEditor {
        private JPanel panel;
        private JButton editRoleButton;
        private JButton toggleStatusButton;
        private String action = "";
        private boolean isPushed;
        private int selectedRow;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);

            panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));

            editRoleButton = new JButton("Edit");
            editRoleButton.setBackground(new Color(255, 99, 71));
            editRoleButton.setForeground(Color.WHITE);
            editRoleButton.setFocusPainted(false);
            editRoleButton.setBorderPainted(false);

            toggleStatusButton = new JButton();
            toggleStatusButton.setForeground(Color.WHITE);
            toggleStatusButton.setFocusPainted(false);
            toggleStatusButton.setBorderPainted(false);

            editRoleButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    action = "Edit";
                    isPushed = true;
                    fireEditingStopped();
                }
            });

            toggleStatusButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    action = "ToggleStatus";
                    isPushed = true;
                    fireEditingStopped();
                }
            });

            panel.add(editRoleButton);
            panel.add(toggleStatusButton);
            panel.setBackground(Color.WHITE);
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            selectedRow = row;
            isPushed = false;

            String status = (String) table.getModel().getValueAt(row, 4);
            if ("Aktif".equals(status)) {
                toggleStatusButton.setText("Nonaktifkan");
                toggleStatusButton.setBackground(new Color(220, 53, 69));
            } else {
                toggleStatusButton.setText("Aktifkan");
                toggleStatusButton.setBackground(new Color(40, 167, 69));
            }

            return panel;
        }

        @Override
        public Object getCellEditorValue() {
            if (isPushed) {
                int userId = (int) userTable.getValueAt(selectedRow, 0);

                if (action.equals("Edit")) {
                    showEditUserDialog(userId, selectedRow);
                } else if (action.equals("ToggleStatus")) {
                    boolean currentStatus = ((String) userTable.getValueAt(selectedRow, 4)).equals("Aktif");
                    toggleUserStatus(userId, currentStatus);
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
        editUserDialog.setSize(500, 300);
        editUserDialog.setLocationRelativeTo(this);
        editUserDialog.setLayout(new BorderLayout());

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        String username = (String) userTable.getValueAt(row, 1);
        String email = (String) userTable.getValueAt(row, 2);
        String roleFromTable = (String) userTable.getValueAt(row, 3);
        String actualRole;
        if (roleFromTable.equalsIgnoreCase("Seller")) {
            actualRole = "supervisor";
        } else if (roleFromTable.equalsIgnoreCase("Operation Manager")) {
            actualRole = "op_manager";
        } else {
            actualRole = roleFromTable;
        }

        boolean isVerified = ((String) userTable.getValueAt(row, 4)).equals("Aktif");

        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("ID Pengguna:"), gbc);
        gbc.gridx = 1;
        JTextField txtUserId = new JTextField(String.valueOf(userId));
        txtUserId.setEditable(false);
        formPanel.add(txtUserId, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        JTextField txtUsername = new JTextField(username);
        txtUsername.setEditable(false);
        formPanel.add(txtUsername, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(new JLabel("Email:"), gbc);
        gbc.gridx = 1;
        JTextField txtEmail = new JTextField(email);
        txtEmail.setEditable(false);
        formPanel.add(txtEmail, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        formPanel.add(new JLabel("Role:"), gbc);
        gbc.gridx = 1;
        String[] internalRoles = {"user", "supervisor", "op_manager", "admin"};
        JComboBox<String> cmbRole = new JComboBox<>(internalRoles);
        cmbRole.setSelectedItem(actualRole);
        cmbRole.setRenderer(new BasicComboBoxRenderer() {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value != null) {
                    if (value.toString().equalsIgnoreCase("supervisor")) {
                        setText("Seller");
                    } else if (value.toString().equalsIgnoreCase("op_manager")) {
                        setText("Operation Manager");
                    } else {
                        setText(value.toString());
                    }
                } else {
                    setText("");
                }
                return this;
            }
        });
        formPanel.add(cmbRole, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        formPanel.add(new JLabel("Status Aktif:"), gbc);
        gbc.gridx = 1;
        JCheckBox chkIsVerified = new JCheckBox();
        chkIsVerified.setSelected(isVerified);
        formPanel.add(chkIsVerified, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.weighty = 1.0;
        formPanel.add(Box.createVerticalGlue(), gbc);


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
                String updatedRole = (String) cmbRole.getSelectedItem();
                boolean updatedIsVerified = chkIsVerified.isSelected();

                String roleToSave;
                if (updatedRole.equalsIgnoreCase("Seller")) {
                    roleToSave = "supervisor";
                } else if (updatedRole.equalsIgnoreCase("Operation Manager")) {
                    roleToSave = "op_manager";
                } else {
                    roleToSave = updatedRole;
                }

                try {
                    Connection conn = DatabaseConnection.getConnection();

                    String updateQuery = "UPDATE users SET role = ?, is_verified = ? WHERE id = ?";
                    PreparedStatement updateStmt = conn.prepareStatement(updateQuery);
                    updateStmt.setString(1, roleToSave);
                    updateStmt.setBoolean(2, updatedIsVerified);
                    updateStmt.setInt(3, userId);

                    int result = updateStmt.executeUpdate();

                    if (result > 0) {
                        userTable.setValueAt(updatedRole, row, 3);
                        userTable.setValueAt(updatedIsVerified ? "Aktif" : "Nonaktif", row, 4);

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

    private void toggleUserStatus(int userId, boolean currentStatus) {
        String actionMessage = currentStatus ? "menonaktifkan" : "mengaktifkan";
        String confirmMessage = "Apakah Anda yakin ingin " + actionMessage + " pengguna ini?";
        int confirm = JOptionPane.showConfirmDialog(this,
                confirmMessage,
                "Konfirmasi Aksi",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                boolean success = ProductRepository.updateUserVerificationStatus(userId, !currentStatus);

                if (success) {
                    JOptionPane.showMessageDialog(this,
                            "Pengguna berhasil " + (currentStatus ? "dinonaktifkan" : "diaktifkan") + "!",
                            "Sukses",
                            JOptionPane.INFORMATION_MESSAGE);
                    loadUserData();
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Gagal " + actionMessage + " pengguna!",
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

            String sellerQuery = "SELECT COUNT(*) FROM users WHERE role = 'supervisor'";
            PreparedStatement sellerStmt = conn.prepareStatement(sellerQuery);
            ResultSet sellerRs = sellerStmt.executeQuery();
            sellerRs.next();
            int sellerCount = sellerRs.getInt(1);

            String opManagerQuery = "SELECT COUNT(*) FROM users WHERE role = 'op_manager'";
            PreparedStatement opManagerStmt = conn.prepareStatement(opManagerQuery);
            ResultSet opManagerRs = opManagerStmt.executeQuery();
            opManagerRs.next();
            int opManagerCount = opManagerRs.getInt(1);

            String userQuery = "SELECT COUNT(*) FROM users WHERE role = 'user'";
            PreparedStatement userStmt = conn.prepareStatement(userQuery);
            ResultSet userRs = userStmt.executeQuery();
            userRs.next();
            int userCount = userRs.getInt(1);

            String activeQuery = "SELECT COUNT(*) FROM users WHERE is_verified = TRUE";
            PreparedStatement activeStmt = conn.prepareStatement(activeQuery);
            ResultSet activeRs = activeStmt.executeQuery();
            activeRs.next();
            int activeCount = activeRs.getInt(1);

            String inactiveQuery = "SELECT COUNT(*) FROM users WHERE is_verified = FALSE";
            PreparedStatement inactiveStmt = conn.prepareStatement(inactiveQuery);
            ResultSet inactiveRs = inactiveStmt.executeQuery();
            inactiveRs.next();
            int inactiveCount = inactiveRs.getInt(1);

            JOptionPane.showMessageDialog(this,
                    "Statistik Pengguna:\n\n" +
                    "Total Pengguna: " + totalUsers + "\n" +
                    "Jumlah Aktif: " + activeCount + "\n" +
                    "Jumlah Nonaktif: " + inactiveCount + "\n" +
                    "Jumlah Admin: " + adminCount + "\n" +
                    "Jumlah Seller: " + sellerCount + "\n" +
                    "Jumlah Operation Manager: " + opManagerCount + "\n" +
                    "Jumlah User: " + userCount,
                    "Statistik Pengguna",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error database: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportUserData() {
        try {
            StringBuilder csvData = new StringBuilder();
            csvData.append("ID,Username,Email,Role,Status\n");

            for (int i = 0; i < userTable.getRowCount(); i++) {
                csvData.append(userTable.getValueAt(i, 0)).append(",");
                csvData.append(userTable.getValueAt(i, 1)).append(",");
                csvData.append(userTable.getValueAt(i, 2)).append(",");
                String roleForExport = (String) userTable.getValueAt(i, 3);
                if (roleForExport.equalsIgnoreCase("Seller")) {
                     roleForExport = "supervisor";
                } else if (roleForExport.equalsIgnoreCase("Operation Manager")) {
                    roleForExport = "op_manager";
                }
                csvData.append(roleForExport).append(",");
                csvData.append(userTable.getValueAt(i, 4)).append("\n");
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
        SwingUtilities.invokeLater(() -> new ManagerDashboardUI().setVisible(true));
    }
}