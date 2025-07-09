package e.commerce;

import com.toedter.calendar.JDateChooser;
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
import java.sql.Timestamp;
import javax.swing.table.TableCellRenderer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

// Import untuk JFreeChart
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;
import org.jfree.chart.ui.RectangleInsets;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import org.jfree.chart.util.Rotation;
import java.awt.BasicStroke;
import javax.swing.border.Border;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import java.util.Locale;
import e.commerce.ProductRepository.SupportTicket;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;


public class ManagerDashboardUI extends JFrame {
    private JPanel mainPanel;
    private CardLayout cardLayout;
    private JButton[] navButtons;

    // Komponen untuk User Management
    private JTable userTable;
    private DefaultTableModel userTableModel;

    // Komponen untuk Dashboard
    private JLabel lblTotalUsersValue;
    private JLabel lblTotalProductsValue;
    private JLabel lblTotalTransactionsValue;
    private JLabel lblTotalRevenueValue;
    private ExportExcelService exportService;
    private ExportPdfService exportPdfService;
    private JLabel exportStatusLabel;

    // --- Komponen untuk Coupon Management (Dilebur ke sini) ---
    private JTable couponTable;
    private DefaultTableModel couponTableModel;
    private JTextField couponCodeField;
    private JComboBox<String> couponTypeComboBox;
    private JSpinner couponValueSpinner;
    private JSpinner couponMinPurchaseSpinner;
    private JSpinner couponMaxDiscountSpinner;
    private JSpinner couponUsageLimitUserSpinner;
    private JSpinner couponTotalUsageLimitSpinner;
    private JDateChooser couponValidFromChooser;
    private JDateChooser couponValidUntilChooser;
    private JCheckBox couponActiveCheckBox;
    private JButton couponSaveButton;
    private JButton couponClearButton;
    private JLabel couponFormTitleLabel;
    private Coupon selectedCoupon = null;
    // --- Akhir Komponen Coupon Management ---
    
    private JTable ticketManagementTable;
    private DefaultTableModel ticketTableModel;

    public ManagerDashboardUI() {
        User currentUser = Authentication.getCurrentUser();
        if (currentUser == null || (!currentUser.getRole().equalsIgnoreCase("admin") && !currentUser.getRole().equalsIgnoreCase("op_manager"))) {
            JOptionPane.showMessageDialog(this, "Akses ditolak! Hanya admin atau Operation Manager yang dapat mengakses halaman ini.", "Error", JOptionPane.ERROR_MESSAGE);
            new LoginUI().setVisible(true);
            dispose();
            return;
        }

        exportService = new ExportExcelService(currentUser);
        exportPdfService = new ExportPdfService(currentUser);

        setTitle("Quantra - Operation Manager Dashboard");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);
        IconUtil.setIcon(this);

        JPanel sidebar = createSidebar();
        mainPanel = new JPanel();
        cardLayout = new CardLayout();
        mainPanel.setLayout(cardLayout);

        JPanel headerPanel = createHeaderPanel(currentUser);
        JPanel dashboardPanel = createAdminDashboardPanel();
        
        // --- REVISI 1: Membungkus Panel Dashboard dengan JScrollPane ---
        JScrollPane dashboardScrollPane = new JScrollPane(dashboardPanel);
        dashboardScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        dashboardScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        dashboardScrollPane.setBorder(null); // Menghilangkan border default
        dashboardScrollPane.getVerticalScrollBar().setUnitIncrement(16); // Scroll lebih mulus
        
        JPanel userManagementPanel = createUserManagementPanel();
        JPanel couponManagementPanel = createCouponManagementPanel(); 
        JPanel ticketManagementPanel = createTicketManagementPanel();

        ProfileUI profilePanel = new ProfileUI();

        // --- REVISI 1: Menambahkan ScrollPane ke CardLayout, bukan panelnya langsung ---
        mainPanel.add(dashboardScrollPane, "Dashboard");
        mainPanel.add(userManagementPanel, "UserManagement");
        mainPanel.add(couponManagementPanel, "CouponManagement");
        mainPanel.add(ticketManagementPanel, "TicketManagement");
        mainPanel.add(profilePanel, "Profile");

        JPanel contentWrapper = new JPanel(new BorderLayout());
        contentWrapper.add(headerPanel, BorderLayout.NORTH);
        contentWrapper.add(mainPanel, BorderLayout.CENTER);

        add(sidebar, BorderLayout.WEST);
        add(contentWrapper, BorderLayout.CENTER);

        loadUserData();
        loadCouponsData();
        refreshDashboardData();
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(245, 245, 245));
        sidebar.setPreferredSize(new Dimension(220, 0)); // Lebarkan sedikit sidebar
        sidebar.setBorder(new EmptyBorder(20, 10, 20, 10));

        JLabel lblLogo = new JLabel();
        ImageIcon logoIcon = new ImageIcon(getClass().getResource("/Resources/Images/Logo.png"));
        Image scaledImage = logoIcon.getImage().getScaledInstance(100, 35, Image.SCALE_SMOOTH);
        lblLogo.setIcon(new ImageIcon(scaledImage));
        lblLogo.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(lblLogo);
        sidebar.add(Box.createRigidArea(new Dimension(0, 30)));

        // Tambahkan "Coupon Management" ke navigasi
        String[] navItems = {"Dashboard", "Manajemen Pengguna", "Manajemen Kupon", "Manajemen Pengaduan", "Profil"};
        String[] panelNames = {"Dashboard", "UserManagement", "CouponManagement", "TicketManagement", "Profile"};
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

            if (i == 0) { // Set Dashboard sebagai default aktif
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

                // Refresh data jika diperlukan
                if (panelName.equals("UserManagement")) loadUserData();
                if (panelName.equals("Dashboard")) refreshDashboardData();
                if (panelName.equals("CouponManagement")) loadCouponsData();
            });

            sidebar.add(navButton);
            sidebar.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        sidebar.add(Box.createVerticalGlue());
        // ... (Tombol Help, dll)
        return sidebar;
    }


    private JPanel createCouponManagementPanel() {
        JPanel mainCouponPanel = new JPanel(new BorderLayout(0, 0));
        mainCouponPanel.setBackground(new Color(248, 250, 252));

        // Header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(229, 231, 235)),
            new EmptyBorder(24, 32, 24, 32)
        ));

        JLabel mainTitle = new JLabel("Manajemen Kupon");
        mainTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        mainTitle.setForeground(new Color(17, 24, 39));
        headerPanel.add(mainTitle, BorderLayout.WEST);

        JPanel contentPanel = new JPanel(new BorderLayout(24, 0));
        contentPanel.setBackground(new Color(248, 250, 252));
        contentPanel.setBorder(new EmptyBorder(24, 32, 32, 32));

        JPanel tablePanel = createCouponTablePanel();
        JPanel formPanel = createCouponFormPanel();
        JScrollPane formScrollPane = new JScrollPane(formPanel);
        formScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        formScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        formScrollPane.setBorder(null);
        contentPanel.add(tablePanel, BorderLayout.CENTER);
        contentPanel.add(formScrollPane, BorderLayout.EAST);
        mainCouponPanel.add(headerPanel, BorderLayout.NORTH);
        mainCouponPanel.add(contentPanel, BorderLayout.CENTER);

        return mainCouponPanel;
    }

    private JPanel createCouponTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 20));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(12, new Color(229, 231, 235)),
            new EmptyBorder(24, 24, 24, 24)
        ));

        JLabel titleLabel = new JLabel("Daftar Kupon Tersedia");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(new Color(17, 24, 39));
        panel.add(titleLabel, BorderLayout.NORTH);

        String[] columnNames = {"ID", "Kode", "Tipe", "Nilai", "Aktif", "Berlaku Hingga"};
        couponTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        couponTable = new JTable(couponTableModel);

        couponTable.setRowHeight(45);
        couponTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        couponTable.setBackground(Color.WHITE);

        couponTable.setSelectionBackground(new Color(255, 69, 0, 50));
        couponTable.setSelectionForeground(Color.BLACK);
        couponTable.setGridColor(new Color(230, 230, 230));

        couponTable.setShowVerticalLines(false);
        couponTable.setIntercellSpacing(new Dimension(0, 1));
        couponTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JTableHeader header = couponTable.getTableHeader();
        header.setBackground(new Color(255, 69, 0));
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setPreferredSize(new Dimension(0, 40));
        header.setBorder(BorderFactory.createEmptyBorder());

        couponTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                setBorder(new EmptyBorder(8, 12, 8, 12)); 
                if (!isSelected) {
                    setBackground(Color.WHITE);
                }
                return c;
            }
        });


        couponTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && couponTable.getSelectedRow() != -1) {
                int selectedRow = couponTable.getSelectedRow();
                int couponId = (int) couponTableModel.getValueAt(selectedRow, 0);
                loadCouponDataToForm(couponId);
            }
        });

        JScrollPane scrollPane = new JScrollPane(couponTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(229, 231, 235), 1));
        scrollPane.setBackground(Color.WHITE);
        scrollPane.getViewport().setBackground(Color.WHITE);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createCouponFormPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new RoundedBorder(12, new Color(229, 231, 235)));

        JPanel formContainer = new JPanel();
        formContainer.setLayout(new BoxLayout(formContainer, BoxLayout.Y_AXIS));
        formContainer.setBackground(Color.WHITE);
        formContainer.setBorder(new EmptyBorder(24, 24, 24, 24));

        couponFormTitleLabel = new JLabel("Tambah Kupon Baru");
        couponFormTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        couponFormTitleLabel.setForeground(new Color(17, 24, 39));
        couponFormTitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        couponCodeField = createStyledTextField();
        couponTypeComboBox = createStyledComboBox(new String[]{"percentage", "fixed"});
        couponValueSpinner = createStyledSpinner(new SpinnerNumberModel(0.0, 0.0, 10000000.0, 1000.0));
        couponMinPurchaseSpinner = createStyledSpinner(new SpinnerNumberModel(0.0, 0.0, 100000000.0, 5000.0));
        couponMaxDiscountSpinner = createStyledSpinner(new SpinnerNumberModel(0.0, 0.0, 10000000.0, 1000.0));
        couponUsageLimitUserSpinner = createStyledSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
        couponTotalUsageLimitSpinner = createStyledSpinner(new SpinnerNumberModel(1, 1, 100000, 10));

        couponValidFromChooser = createStyledDateChooser();
        couponValidUntilChooser = createStyledDateChooser();

        couponActiveCheckBox = new JCheckBox("Aktifkan Kupon Ini", true);
        couponActiveCheckBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        couponActiveCheckBox.setBackground(Color.WHITE);
        couponActiveCheckBox.setForeground(new Color(17, 24, 39));
        couponActiveCheckBox.setFocusPainted(false);

        formContainer.add(couponFormTitleLabel);
        formContainer.add(Box.createVerticalStrut(24));

        formContainer.add(createFormField("Kode Kupon:", couponCodeField));
        formContainer.add(createFormField("Tipe Diskon:", couponTypeComboBox));
        formContainer.add(createFormField("Nilai Diskon:", couponValueSpinner));
        formContainer.add(createFormField("Min. Pembelian (Rp):", couponMinPurchaseSpinner));
        formContainer.add(createFormField("Maks. Diskon (Rp):", couponMaxDiscountSpinner));
        formContainer.add(createFormField("Limit/Pengguna:", couponUsageLimitUserSpinner));
        formContainer.add(createFormField("Total Kuota:", couponTotalUsageLimitSpinner));
        formContainer.add(createFormField("Berlaku Dari:", couponValidFromChooser));
        formContainer.add(createFormField("Berlaku Sampai:", couponValidUntilChooser));

        formContainer.add(Box.createVerticalStrut(16));
        formContainer.add(couponActiveCheckBox);

        formContainer.add(Box.createVerticalGlue());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonPanel.setBackground(Color.WHITE);

        couponClearButton = createStyledButton("Bersihkan", false);
        couponClearButton.addActionListener(e -> clearCouponForm());

        couponSaveButton = createStyledButton("Simpan Kupon", true);
        couponSaveButton.addActionListener(e -> saveCoupon());

        buttonPanel.add(couponClearButton);
        buttonPanel.add(Box.createHorizontalStrut(12));
        buttonPanel.add(couponSaveButton);

        formContainer.add(buttonPanel);
        panel.add(formContainer);

        return panel;
    }

    private JPanel createFormField(String labelText, JComponent component) {
        JPanel fieldPanel = new JPanel(new BorderLayout(0, 8));
        fieldPanel.setBackground(Color.WHITE);
        fieldPanel.setBorder(new EmptyBorder(0, 0, 16, 0));

        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        label.setForeground(new Color(55, 65, 81));

        fieldPanel.add(label, BorderLayout.NORTH);
        fieldPanel.add(component, BorderLayout.CENTER);

        return fieldPanel;
    }

    private JTextField createStyledTextField() {
        JTextField field = new JTextField();
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(209, 213, 219), 1),
            new EmptyBorder(10, 12, 10, 12)
        ));
        field.setBackground(Color.WHITE);
        field.setPreferredSize(new Dimension(0, 40));

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(255, 69, 0), 2),
                    new EmptyBorder(9, 11, 9, 11)
                ));
            }

            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(209, 213, 219), 1),
                    new EmptyBorder(10, 12, 10, 12)
                ));
            }
        });

        return field;
    }

    private JComboBox<String> createStyledComboBox(String[] items) {
        JComboBox<String> comboBox = new JComboBox<>(items);
        comboBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        comboBox.setBorder(BorderFactory.createLineBorder(new Color(209, 213, 219), 1));
        comboBox.setBackground(Color.WHITE);
        comboBox.setPreferredSize(new Dimension(0, 40));
        return comboBox;
    }

    private JSpinner createStyledSpinner(SpinnerModel model) {
        JSpinner spinner = new JSpinner(model);
        spinner.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        spinner.setBorder(BorderFactory.createLineBorder(new Color(209, 213, 219), 1));
        spinner.setPreferredSize(new Dimension(0, 40));

        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JTextField textField = ((JSpinner.DefaultEditor) editor).getTextField();
            textField.setBorder(new EmptyBorder(0, 8, 0, 8));
            textField.setBackground(Color.WHITE);
        }

        return spinner;
    }

    private JDateChooser createStyledDateChooser() {
        JDateChooser chooser = new JDateChooser();
        chooser.setDateFormatString("yyyy-MM-dd");
        chooser.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        chooser.setBorder(BorderFactory.createLineBorder(new Color(209, 213, 219), 1));
        chooser.setPreferredSize(new Dimension(0, 40));
        chooser.setBackground(Color.WHITE);
        return chooser;
    }

    private JButton createStyledButton(String text, boolean isPrimary) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setPreferredSize(new Dimension(120, 40));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        if (isPrimary) {
            button.setBackground(new Color(255, 69, 0));
            button.setForeground(Color.WHITE);

            button.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    button.setBackground(new Color(234, 58, 0));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    button.setBackground(new Color(255, 69, 0));
                }
            });
        } else {
            button.setBackground(new Color(243, 244, 246));
            button.setForeground(new Color(75, 85, 99));
            button.setBorder(BorderFactory.createLineBorder(new Color(209, 213, 219), 1));

            button.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    button.setBackground(new Color(229, 231, 235));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    button.setBackground(new Color(243, 244, 246));
                }
            });
        }

        return button;
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
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(color);
            g2d.setStroke(new BasicStroke(1));
            g2d.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2d.dispose();
        }
    }

    private void loadCouponsData() {
        couponTableModel.setRowCount(0);
        try {
            List<Coupon> coupons = ProductRepository.getAllCoupons();
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy");
            for (Coupon coupon : coupons) {
                couponTableModel.addRow(new Object[]{
                    coupon.getId(),
                    coupon.getCouponCode(),
                    coupon.getDiscountType(),
                    coupon.getDiscountValue(),
                    coupon.isActive() ? "Ya" : "Tidak",
                    sdf.format(coupon.getValidUntil())
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Gagal memuat data kupon: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadCouponDataToForm(int couponId) {
        try {
            selectedCoupon = ProductRepository.getCouponById(couponId);
            if (selectedCoupon != null) {
                couponFormTitleLabel.setText("Edit Kupon: " + selectedCoupon.getCouponCode());
                couponCodeField.setText(selectedCoupon.getCouponCode());
                couponTypeComboBox.setSelectedItem(selectedCoupon.getDiscountType());
                couponValueSpinner.setValue(selectedCoupon.getDiscountValue());
                couponMinPurchaseSpinner.setValue(selectedCoupon.getMinPurchaseAmount());
                couponMaxDiscountSpinner.setValue(selectedCoupon.getMaxDiscountAmount());
                couponUsageLimitUserSpinner.setValue(selectedCoupon.getUsageLimitPerUser());
                couponTotalUsageLimitSpinner.setValue(selectedCoupon.getTotalUsageLimit());
                couponValidFromChooser.setDate(new Date(selectedCoupon.getValidFrom().getTime()));
                couponValidUntilChooser.setDate(new Date(selectedCoupon.getValidUntil().getTime()));
                couponActiveCheckBox.setSelected(selectedCoupon.isActive());
                couponSaveButton.setText("Update Kupon");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Gagal memuat detail kupon: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearCouponForm() {
        selectedCoupon = null;
        couponFormTitleLabel.setText("Tambah Kupon Baru");
        couponCodeField.setText("");
        couponTypeComboBox.setSelectedIndex(0);
        couponValueSpinner.setValue(0.0);
        couponMinPurchaseSpinner.setValue(0.0);
        couponMaxDiscountSpinner.setValue(0.0);
        couponUsageLimitUserSpinner.setValue(1);
        couponTotalUsageLimitSpinner.setValue(100);
        couponValidFromChooser.setDate(null);
        couponValidUntilChooser.setDate(null);
        couponActiveCheckBox.setSelected(true);
        couponTable.clearSelection();
        couponSaveButton.setText("Simpan Kupon");
    }

    private void saveCoupon() {
        String code = couponCodeField.getText().trim().toUpperCase();
        if (code.isEmpty() || couponValidFromChooser.getDate() == null || couponValidUntilChooser.getDate() == null) {
            JOptionPane.showMessageDialog(this, "Kode dan Tanggal Berlaku harus diisi.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (couponValidFromChooser.getDate().after(couponValidUntilChooser.getDate())) {
            JOptionPane.showMessageDialog(this, "Tanggal mulai tidak boleh setelah tanggal berakhir.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String type = (String) couponTypeComboBox.getSelectedItem();
        double value = (double) couponValueSpinner.getValue();
        double minPurchase = (double) couponMinPurchaseSpinner.getValue();
        double maxDiscount = (double) couponMaxDiscountSpinner.getValue();
        int limitPerUser = (int) couponUsageLimitUserSpinner.getValue();
        int totalLimit = (int) couponTotalUsageLimitSpinner.getValue();
        Timestamp validFrom = new Timestamp(couponValidFromChooser.getDate().getTime());
        Timestamp validUntil = new Timestamp(couponValidUntilChooser.getDate().getTime());
        boolean isActive = couponActiveCheckBox.isSelected();

        try {
            if (selectedCoupon == null) {
                ProductRepository.createCoupon(code, type, value, minPurchase, maxDiscount, limitPerUser, totalLimit, validFrom, validUntil, isActive);
                JOptionPane.showMessageDialog(this, "Kupon baru berhasil disimpan!", "Sukses", JOptionPane.INFORMATION_MESSAGE);
            } else {
                ProductRepository.updateCoupon(selectedCoupon.getId(), code, type, value, minPurchase, maxDiscount, limitPerUser, totalLimit, validFrom, validUntil, isActive);
                JOptionPane.showMessageDialog(this, "Kupon berhasil diperbarui!", "Sukses", JOptionPane.INFORMATION_MESSAGE);
            }
            loadCouponsData();
            clearCouponForm();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Gagal menyimpan kupon: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
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
        int totalTransactionsCount = ProductRepository.getTotalOrderCount();
        double totalRevenue = getTotalRevenue();

        lblTotalUsersValue.setText(String.valueOf(totalUsers));
        lblTotalProductsValue.setText(String.valueOf(totalProducts));
        lblTotalTransactionsValue.setText(String.valueOf(totalTransactionsCount));

        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
        lblTotalRevenueValue.setText(currencyFormat.format(totalRevenue));
    }

    private double getTotalRevenue() {
        double totalRevenue = 0.0;
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
        dataset.setValue("Seller", getTotalUserCountByRole("seller"));
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
            new EmptyBorder(24, 24, 24, 24)
        ));

        chartWrapperPanel.setPreferredSize(new Dimension(480, 320));

        ChartPanel chartPanel = createModernChartPanel(chart);
        chartWrapperPanel.add(chartPanel, BorderLayout.CENTER);

        return chartWrapperPanel;
    }
    
    private JPanel createMonthlySalesLineChartPanel() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
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
        }

        JFreeChart lineChart = ChartFactory.createLineChart(
                "Tren Penjualan Bulanan",
                "Bulan",
                "Pendapatan (Rp)",
                dataset,
                org.jfree.chart.plot.PlotOrientation.VERTICAL,
                false, 
                true,
                false
        );

        lineChart.setBackgroundPaint(Color.WHITE);
        lineChart.getTitle().setFont(new Font("Inter", Font.BOLD, 18));
        lineChart.getTitle().setPaint(new Color(30, 30, 30));

        CategoryPlot plot = (CategoryPlot) lineChart.getPlot();
        plot.setBackgroundPaint(new Color(245, 245, 245));
        plot.setOutlineVisible(false);
        plot.setRangeGridlinePaint(new Color(200, 200, 200));
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);

        // --- REVISI 2: Memperbaiki format angka pada Line Chart ---
        org.jfree.chart.axis.NumberAxis rangeAxisLine = (org.jfree.chart.axis.NumberAxis) plot.getRangeAxis();
        DecimalFormat decimalFormatLine = new DecimalFormat("#,##0");
        rangeAxisLine.setNumberFormatOverride(decimalFormatLine);

        org.jfree.chart.renderer.category.LineAndShapeRenderer renderer = (org.jfree.chart.renderer.category.LineAndShapeRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, new Color(74, 110, 255)); 
        renderer.setSeriesStroke(0, new BasicStroke(2.5f)); 
        renderer.setSeriesShapesVisible(0, true); 

        plot.getDomainAxis().setTickLabelFont(new Font("Arial", Font.PLAIN, 12));
        plot.getRangeAxis().setTickLabelFont(new Font("Arial", Font.PLAIN, 12));

        ChartPanel chartPanel = createModernChartPanel(lineChart);
        chartPanel.setPreferredSize(new Dimension(600, 350));

        JPanel chartWrapper = new JPanel(new BorderLayout());
        chartWrapper.setBackground(Color.WHITE);
        chartWrapper.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(229, 231, 235), 1),
            new EmptyBorder(15, 15, 15, 15)
        ));
        chartWrapper.add(chartPanel, BorderLayout.CENTER);
        return chartWrapper;
    }

    
    private JPanel createAdminDashboardPanel() {
        JPanel dashboardPanel = new JPanel(new BorderLayout());
        dashboardPanel.setBackground(Color.WHITE);
        dashboardPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel summaryCardsPanel = new JPanel(new GridLayout(1, 4, 20, 0));
        summaryCardsPanel.setBackground(Color.WHITE);

        lblTotalUsersValue = new JLabel("0");
        lblTotalProductsValue = new JLabel("0");
        lblTotalTransactionsValue = new JLabel("0");
        lblTotalRevenueValue = new JLabel("0");

        summaryCardsPanel.add(createSummaryCard("Total Pengguna", lblTotalUsersValue, new Color(255, 99, 71)));
        summaryCardsPanel.add(createSummaryCard("Total Produk", lblTotalProductsValue, new Color(255, 99, 71)));
        summaryCardsPanel.add(createSummaryCard("Total Transaksi", lblTotalTransactionsValue, new Color(255, 99, 71)));
        summaryCardsPanel.add(createSummaryCard("Total Pendapatan", lblTotalRevenueValue, new Color(76, 175, 80)));

        JPanel exportControlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        exportControlPanel.setBackground(Color.WHITE);
        exportControlPanel.setBorder(new EmptyBorder(10, 0, 10, 0));
        JButton exportButton = new JButton("Ekspor Laporan");
        exportButton.setBackground(new Color(74, 110, 255));
        exportButton.setForeground(Color.WHITE);
        exportButton.setBorderPainted(false);
        exportButton.setFocusPainted(false);
        exportButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        exportButton.addActionListener(e -> showExportOptionDialog());
        exportStatusLabel = new JLabel("Siap untuk ekspor.");
        exportStatusLabel.setForeground(Color.BLACK);
        exportStatusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        exportControlPanel.add(exportButton);
        exportControlPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        exportControlPanel.add(exportStatusLabel);

        JPanel topPanelWrapper = new JPanel(new BorderLayout());
        topPanelWrapper.setBackground(Color.WHITE);
        topPanelWrapper.add(summaryCardsPanel, BorderLayout.NORTH);
        topPanelWrapper.add(exportControlPanel, BorderLayout.CENTER);
        dashboardPanel.add(topPanelWrapper, BorderLayout.NORTH);

        JPanel chartAndDetailsPanel = new JPanel(new BorderLayout(0, 20));
        chartAndDetailsPanel.setBackground(Color.WHITE);
        chartAndDetailsPanel.setOpaque(true);
        chartAndDetailsPanel.setBorder(new EmptyBorder(20, 0, 0, 0));
        JPanel pieChartPanel = createModernChartWrapper();
        JPanel barChartPanel = createMonthlySalesTrendChartPanel();

        JSplitPane topSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, pieChartPanel, barChartPanel);
        topSplitPane.setResizeWeight(0.5);
        topSplitPane.setBorder(null);     
        topSplitPane.setBackground(Color.WHITE);
        topSplitPane.setContinuousLayout(true); 

        JPanel bottomChartPanel = createMonthlySalesLineChartPanel();
        chartAndDetailsPanel.add(topSplitPane, BorderLayout.NORTH);
        chartAndDetailsPanel.add(bottomChartPanel, BorderLayout.CENTER);


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

    private JPanel createMonthlySalesTrendChartPanel() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
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
            System.err.println("Error fetching monthly sales data from database: " + e.getMessage());
            JOptionPane.showMessageDialog(this,
                    "Gagal memuat data penjualan bulanan dari database.\n" + e.getMessage(),
                    "Error Database",
                    JOptionPane.ERROR_MESSAGE);
        }

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

        // --- REVISI 2: Memperbaiki format angka pada Bar Chart ---
        org.jfree.chart.axis.NumberAxis rangeAxis = (org.jfree.chart.axis.NumberAxis) plot.getRangeAxis();
        DecimalFormat decimalFormat = new DecimalFormat("#,##0");
        rangeAxis.setNumberFormatOverride(decimalFormat);
        
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setDrawBarOutline(false);

        renderer.setSeriesPaint(0, new Color(255, 99, 71));
        renderer.setMaximumBarWidth(0.3);

        plot.getDomainAxis().setCategoryMargin(0.0);
        plot.getDomainAxis().setLowerMargin(0.0);
        plot.getDomainAxis().setUpperMargin(0.9);
        renderer.setItemMargin(0.0);

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

    private void showExportOptionDialog() {
        String[] options = {"Excel", "PDF"};
        int choice = JOptionPane.showOptionDialog(
            this,
            "Pilih format ekspor:",
            "Pilihan Ekspor Laporan",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0] 
        );

        if (choice == 0) { // Excel
            exportSalesData(ExportFormat.EXCEL);
        } else if (choice == 1) { // PDF
            exportSalesData(ExportFormat.PDF);
        } else {
            exportStatusLabel.setForeground(Color.BLACK);
            exportStatusLabel.setText("Ekspor dibatalkan.");
        }
    }
    
    private enum ExportFormat {
        EXCEL, PDF
    }

    private void exportSalesData(ExportFormat format) {
        String defaultFileName;
        String fileExtension;
        String dialogTitle;
        
        if (format == ExportFormat.EXCEL) {
            defaultFileName = "LaporanPenjualan.xlsx";
            fileExtension = "xlsx";
            dialogTitle = "Simpan Laporan Penjualan (Excel)";
        } else { 
            defaultFileName = "LaporanPenjualan.pdf";
            fileExtension = "pdf";
            dialogTitle = "Simpan Laporan Penjualan (PDF)";
        }

        String userHome = System.getProperty("user.home");
        File defaultFile = new File(userHome + File.separator + "Downloads" + File.separator + defaultFileName);

        File parentDir = defaultFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            boolean dirsCreated = parentDir.mkdirs();
            System.out.println("DEBUG: Parent directories created: " + dirsCreated + " at " + parentDir.getAbsolutePath());
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(dialogTitle);
        fileChooser.setSelectedFile(defaultFile);
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(format.name() + " Files (*." + fileExtension + ")", fileExtension));

        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith("." + fileExtension)) {
                fileToSave = new File(fileToSave.getAbsolutePath() + "." + fileExtension);
            }
            String finalPath = fileToSave.getAbsolutePath();
            System.out.println("DEBUG: User selected path: " + finalPath);

            exportStatusLabel.setForeground(Color.BLACK); 
            exportStatusLabel.setText("Mengekspor data penjualan ke " + format.name() + "... Mohon tunggu.");
            
            new SwingWorker<Void, Void>() {
                String message;
                Color msgColor;

                @Override
                protected Void doInBackground() throws Exception {
                    try {
                        System.out.println("DEBUG: SwingWorker doInBackground started. Calling exportService for " + format.name() + ".");
                        if (format == ExportFormat.EXCEL) {
                            exportService.exportSalesDataToExcel(finalPath);
                        } else {
                            exportPdfService.exportSalesDataToPdf(finalPath);
                        }
                        message = "Data berhasil diekspor ke:<br>" + finalPath;
                        msgColor = new Color(40, 167, 69); // SUCCESS_GREEN
                        System.out.println("DEBUG: Export successful in SwingWorker.");
                    } catch (SQLException | IOException ex) {
                        message = "Gagal mengekspor data: " + ex.getMessage();
                        msgColor = new Color(220, 53, 69); // CANCEL_RED
                        System.err.println("DEBUG: Export failed in SwingWorker.");
                        ex.printStackTrace(); 
                    } catch (Throwable ex) { 
                        message = "Terjadi kesalahan kritis saat ekspor ke " + format.name() + ": " + ex.getMessage();
                        msgColor = new Color(220, 53, 69); // CANCEL_RED
                        System.err.println("DEBUG: Critical error in SwingWorker during PDF export: " + ex.getClass().getName());
                        ex.printStackTrace();
                    }
                    return null;
                }

                @Override
                protected void done() {
                    if (message == null) {
                        message = "Terjadi kesalahan tidak terduga, pesan status kosong.";
                        msgColor = new Color(220, 53, 69);
                        System.err.println("DEBUG: Message in SwingWorker.done() was unexpectedly null.");
                    }
                    exportStatusLabel.setForeground(msgColor);
                    exportStatusLabel.setText("<html>" + message + "</html>");
                    System.out.println("DEBUG: SwingWorker done() finished. Label updated.");
                }
            }.execute();
        } else {
            exportStatusLabel.setForeground(Color.BLACK);
            exportStatusLabel.setText("Ekspor dibatalkan.");
            System.out.println("DEBUG: Export cancelled by user.");
        }
    }

    public static void main(String[] args) {
            SwingUtilities.invokeLater(() -> new ManagerDashboardUI().setVisible(true));
        }

    private JPanel createTicketManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setBackground(new Color(250, 250, 250));

        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(224, 224, 224)),
            new EmptyBorder(15, 20, 15, 20)
        ));

        // Title
        JLabel titleLabel = new JLabel("Manajemen Pengaduan");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(new Color(13, 13, 13));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        headerPanel.add(titleLabel, BorderLayout.CENTER);

        // Refresh button in header
        JButton refreshButton = createModernButton("Refresh Data", new Color(255, 69, 0));
        refreshButton.addActionListener(e -> loadAllTickets());
        headerPanel.add(refreshButton, BorderLayout.EAST);

        headerPanel.add(Box.createHorizontalStrut(refreshButton.getPreferredSize().width), BorderLayout.WEST);

        panel.add(headerPanel, BorderLayout.NORTH);

        // Main Content
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(new EmptyBorder(25, 25, 25, 25));
        mainPanel.setBackground(new Color(250, 250, 250));

        // Table setup
        String[] columnNames = {"ID", "User ID", "Subjek", "Status", "Tanggal Masuk"};
        ticketTableModel = new DefaultTableModel(columnNames, 0) {
            @Override 
            public boolean isCellEditable(int row, int column) { 
                return false; 
            }
        };

        ticketManagementTable = new JTable(ticketTableModel);
        setupModernTable(ticketManagementTable);

        ticketManagementTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = ticketManagementTable.getSelectedRow();
                    if (row != -1) {
                        int ticketId = (int) ticketTableModel.getValueAt(row, 0);
                        showTicketDetailDialogForManager(ticketId);
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(ticketManagementTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        panel.add(mainPanel, BorderLayout.CENTER);

        loadAllTickets();
        return panel;
    }

    private void setupModernTable(JTable table) {
        table.setRowHeight(45);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(new Color(255, 69, 0, 50));
        table.setSelectionForeground(Color.BLACK);
        table.setGridColor(new Color(230, 230, 230));
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 1));

        JTableHeader header = table.getTableHeader();
        header.setBackground(new Color(255, 69, 0));
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setPreferredSize(new Dimension(0, 40));
        header.setBorder(BorderFactory.createEmptyBorder());

        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(1).setPreferredWidth(80);
        table.getColumnModel().getColumn(2).setPreferredWidth(200);
        table.getColumnModel().getColumn(3).setPreferredWidth(120);
        table.getColumnModel().getColumn(4).setPreferredWidth(150);

        table.getColumnModel().getColumn(3).setCellRenderer(new StatusCellRenderer());
    }

    private JButton createModernButton(String text, Color backgroundColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setBackground(backgroundColor);
        button.setForeground(backgroundColor.equals(Color.WHITE) ? new Color(50, 50, 50) : Color.WHITE);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (backgroundColor.equals(Color.WHITE)) {
                    button.setBackground(new Color(240, 240, 240));
                } else {
                    button.setBackground(backgroundColor.darker());
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(backgroundColor);
            }
        });

        return button;
    }

    private class StatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (!isSelected) {
                String status = value.toString().toLowerCase();
                switch (status) {
                    case "dibuka":
                    case "open":
                    case "terbuka":
                        setBackground(new Color(254, 243, 199));
                        setForeground(new Color(146, 64, 14));
                        break;
                    case "selesai":
                    case "closed":
                    case "tertutup":
                        setBackground(new Color(220, 252, 231));
                        setForeground(new Color(22, 101, 52));
                        break;
                    case "dibalas manajer":
                    case "pending":
                        setBackground(new Color(255, 69, 0, 30));
                        setForeground(new Color(255, 69, 0));
                        break;
                    default:
                        setBackground(Color.WHITE);
                        setForeground(Color.BLACK);
                }
            }

            setFont(new Font("Segoe UI", Font.BOLD, 12));
            setHorizontalAlignment(SwingConstants.CENTER);
            setBorder(new EmptyBorder(8, 12, 8, 12));

            return c;
        }
    }

    private void loadAllTickets() {
        ticketTableModel.setRowCount(0);
        try {
            List<SupportTicket> tickets = ProductRepository.getAllTickets();
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, HH:mm");
            for (SupportTicket ticket : tickets) {
                ticketTableModel.addRow(new Object[]{
                    ticket.getId(), 
                    ticket.getUserId(), 
                    ticket.getSubject(), 
                    ticket.getStatus(), 
                    sdf.format(ticket.getCreatedAt())
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Gagal memuat tiket: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showTicketDetailDialogForManager(int ticketId) {
        try {
            SupportTicket ticket = ProductRepository.getTicketById(ticketId);
            if (ticket == null) return;

            JDialog dialog = new JDialog(this, "Detail Tiket #" + ticketId, true);
            dialog.setSize(550, 650);
            dialog.setLocationRelativeTo(this);
            dialog.setLayout(new BorderLayout(15, 15));
            dialog.getContentPane().setBackground(new Color(250, 250, 250));

            JPanel headerPanel = new JPanel();
            headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
            headerPanel.setBackground(new Color(250, 250, 250));
            headerPanel.setBorder(new EmptyBorder(20, 20, 10, 20));

            JLabel titleLabel = new JLabel("Detail Tiket #" + ticketId);
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
            titleLabel.setForeground(new Color(255, 69, 0));
            titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            JPanel infoPanel = new JPanel(new GridLayout(3, 2, 10, 10));
            infoPanel.setBackground(new Color(250, 250, 250));
            infoPanel.setBorder(new EmptyBorder(15, 0, 0, 0));

            infoPanel.add(createInfoLabel("ID Pengguna:", String.valueOf(ticket.getUserId())));
            infoPanel.add(new JLabel());
            infoPanel.add(createInfoLabel("Subjek:", ticket.getSubject()));
            infoPanel.add(new JLabel());
            infoPanel.add(createInfoLabel("Status Saat Ini:", ticket.getStatus()));
            infoPanel.add(new JLabel());

            headerPanel.add(titleLabel);
            headerPanel.add(infoPanel);

            JTextArea messageArea = new JTextArea(ticket.getMessage());
            messageArea.setEditable(false);
            messageArea.setLineWrap(true);
            messageArea.setWrapStyleWord(true);
            messageArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            messageArea.setBackground(Color.WHITE);
            messageArea.setBorder(new EmptyBorder(15, 15, 15, 15));

            JScrollPane messageScroll = new JScrollPane(messageArea);
            messageScroll.setBorder(createModernTitledBorder("Pesan dari Pengguna"));
            messageScroll.setPreferredSize(new Dimension(0, 150));

            JTextArea replyArea = new JTextArea(ticket.getManagerReply() != null ? ticket.getManagerReply() : "");
            replyArea.setLineWrap(true);
            replyArea.setWrapStyleWord(true);
            replyArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            replyArea.setBackground(Color.WHITE);
            replyArea.setBorder(new EmptyBorder(15, 15, 15, 15));

            JScrollPane replyScroll = new JScrollPane(replyArea);
            replyScroll.setBorder(createModernTitledBorder("Tulis Balasan Anda"));
            replyScroll.setPreferredSize(new Dimension(0, 150));

            JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
            controlPanel.setBackground(new Color(250, 250, 250));
            controlPanel.setBorder(new EmptyBorder(10, 20, 20, 20));

            JLabel statusLabel = new JLabel("Ubah Status:");
            statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            statusLabel.setForeground(new Color(70, 70, 70));

            JComboBox<String> statusComboBox = new JComboBox<>(new String[]{"Dibuka", "Dibalas Manajer", "Selesai"});
            statusComboBox.setSelectedItem(ticket.getStatus());
            statusComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            statusComboBox.setBackground(Color.WHITE);
            statusComboBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
            ));

            JButton saveButton = createModernButton("Simpan & Balas", new Color(255, 69, 0));
            saveButton.addActionListener(e -> {
                try {
                    String newStatus = (String) statusComboBox.getSelectedItem();
                    String replyText = replyArea.getText().trim();
                    ProductRepository.updateTicketStatusAndReply(ticketId, newStatus, replyText);
                    JOptionPane.showMessageDialog(dialog, "Tiket berhasil diperbarui.", "Sukses", JOptionPane.INFORMATION_MESSAGE);
                    loadAllTickets();
                    dialog.dispose();
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(dialog, "Gagal memperbarui tiket.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            });

            controlPanel.add(statusLabel);
            controlPanel.add(statusComboBox);
            controlPanel.add(saveButton);

            JPanel centerPanel = new JPanel(new GridLayout(2, 1, 0, 15));
            centerPanel.setBackground(new Color(250, 250, 250));
            centerPanel.setBorder(new EmptyBorder(0, 20, 0, 20));
            centerPanel.add(messageScroll);
            centerPanel.add(replyScroll);

            dialog.add(headerPanel, BorderLayout.NORTH);
            dialog.add(centerPanel, BorderLayout.CENTER);
            dialog.add(controlPanel, BorderLayout.SOUTH);
            dialog.setVisible(true);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Gagal membuka detail tiket.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel createInfoLabel(String label, String value) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setBackground(new Color(250, 250, 250));

        JLabel labelComponent = new JLabel(label);
        labelComponent.setFont(new Font("Segoe UI", Font.BOLD, 14));
        labelComponent.setForeground(new Color(70, 70, 70));

        JLabel valueComponent = new JLabel(value);
        valueComponent.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        valueComponent.setForeground(new Color(100, 100, 100));

        panel.add(labelComponent);
        panel.add(Box.createHorizontalStrut(10));
        panel.add(valueComponent);

        return panel;
    }

    private javax.swing.border.TitledBorder createModernTitledBorder(String title) {
        javax.swing.border.TitledBorder border = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
            title
        );
        border.setTitleFont(new Font("Segoe UI", Font.BOLD, 14));
        border.setTitleColor(new Color(255, 69, 0));
        return border;
    }
}