package e.commerce;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

import e.commerce.ProductRepository.Order;
import e.commerce.ProductRepository.OrderItem;

public class OrderHistory extends JPanel {

    // --- Konstanta Warna ---
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

    private JTable orderTable;
    private DefaultTableModel tableModel;
    private JLabel totalPriceLabel;
    private User currentUser;
    private ViewController viewController;

    // PATH UNTUK IKON KALENDER
    private static final String CALENDAR_ICON_PATH = "/Resources/Images/calendar_icon.png";

    public OrderHistory(ViewController viewController) {
        this.viewController = viewController;
        this.currentUser = Authentication.getCurrentUser();
        if (this.currentUser == null) {
            System.err.println("OrderHistory: Tidak ada user yang login. Tidak dapat memuat riwayat.");
            JOptionPane.showMessageDialog(this, "Anda harus login untuk melihat riwayat pesanan.", "Error", JOptionPane.ERROR_MESSAGE);
            if (this.viewController != null) {
                this.viewController.showDashboardView();
            }
            return;
        }

        initializeComponents();
        populateData();
    }

    private void initializeComponents() {
        setLayout(new BorderLayout());
        setBackground(WHITE);
        setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        JPanel headerPanel = createHeaderPanel();
        JPanel filterPanel = createFilterPanel();

        JPanel topSection = new JPanel(new BorderLayout());
        topSection.setBackground(WHITE);
        topSection.add(headerPanel, BorderLayout.NORTH);
        topSection.add(filterPanel, BorderLayout.CENTER);

        JPanel tablePanel = createTablePanel();
        JPanel footerPanel = createFooterPanel();

        add(topSection, BorderLayout.NORTH);
        add(tablePanel, BorderLayout.CENTER);
        add(footerPanel, BorderLayout.SOUTH);
    }

private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(WHITE);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 30, 0));

        // Tombol "Kembali" di kiri
        JButton backButton = new JButton("← Kembali"); // Ubah teks tombol
        backButton.setFont(new Font("Arial", Font.BOLD, 14));
        backButton.setForeground(TEXT_DARK);
        backButton.setBackground(WHITE);
        backButton.setBorderPainted(false);
        backButton.setFocusPainted(false);
        backButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backButton.addActionListener(e -> {
            if (viewController != null) {
                viewController.showDashboardView();
            }
        });
        headerPanel.add(backButton, BorderLayout.WEST); // Letakkan di WEST


        JLabel titleLabel = new JLabel("Riwayat Pesanan");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 32));
        titleLabel.setForeground(TEXT_DARK);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER); // Tengahkan judul
        headerPanel.add(titleLabel, BorderLayout.CENTER); // Letakkan di CENTER

        // Tambahkan panel kosong ke timur jika tidak ada lagi kontrol di sana
        headerPanel.add(Box.createRigidArea(new Dimension(10, 0)), BorderLayout.EAST);

        return headerPanel;
    }

    private JPanel createFilterPanel() {
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        filterPanel.setBackground(WHITE);
        filterPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 30, 0));

        String[] filters = {"Semua Pesanan", "Belum Bayar", "Dikemas", "Dikirim", "Selesai", "Dibatalkan"};
        ButtonGroup filterGroup = new ButtonGroup();

        for (int i = 0; i < filters.length; i++) {
            JToggleButton filterBtn = new JToggleButton(filters[i]);
            filterBtn.setFont(new Font("Arial", Font.PLAIN, 14));
            filterBtn.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
            filterBtn.setFocusPainted(false);

            filterBtn.setContentAreaFilled(false);
            filterBtn.setOpaque(true);


            filterBtn.addActionListener(e -> {
                updateFilterButtons(filterGroup, (JToggleButton) e.getSource());
                String filterText = ((JToggleButton) e.getSource()).getText();
                populateData(filterText);
            });

            if (i == 0) {
                filterBtn.setSelected(true);
                filterBtn.setBackground(ORANGE_PRIMARY);
                filterBtn.setForeground(WHITE);
                filterBtn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 3, 0, ORANGE_PRIMARY),
                    BorderFactory.createEmptyBorder(12, 20, 9, 20)
                ));
            } else {
                filterBtn.setBackground(WHITE);
                filterBtn.setForeground(GRAY_TEXT_COLOR);
            }

            filterGroup.add(filterBtn);
            filterPanel.add(filterBtn);
            if (i < filters.length - 1) {
                filterPanel.add(Box.createHorizontalStrut(25));
            }
        }

        return filterPanel;
    }

    private void updateFilterButtons(ButtonGroup group, JToggleButton activeButton) {
        for (AbstractButton button : java.util.Collections.list(group.getElements())) {
            button.setBackground(WHITE);
            button.setForeground(GRAY_TEXT_COLOR);
            button.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
            if (button instanceof JToggleButton) {
                ((JToggleButton) button).setContentAreaFilled(false);
                button.setOpaque(true);
            }
        }

        activeButton.setBackground(ORANGE_PRIMARY);
        activeButton.setForeground(WHITE);
        activeButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 3, 0, ORANGE_PRIMARY),
            BorderFactory.createEmptyBorder(12, 20, 9, 20)
        ));
        if (activeButton instanceof JToggleButton) {
            ((JToggleButton) activeButton).setContentAreaFilled(false);
            activeButton.setOpaque(true);
        }
    }

    private JPanel createTablePanel() {
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(WHITE);

        String[] columnNames = {"Detail Pesanan", "Produk", "Status", "Aksi"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 3;
            }
            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 0 || column == 2 || column == 3) {
                    return String.class;
                } else if (column == 1) {
                    return List.class;
                }
                return Object.class;
            }
        };

        orderTable = new JTable(tableModel);
        orderTable.setFont(new Font("Arial", Font.PLAIN, 14));
        orderTable.setRowHeight(120);
        orderTable.setBackground(WHITE);
        orderTable.setGridColor(GRAY_BORDER);
        orderTable.setSelectionBackground(GRAY_LIGHT);
        orderTable.setShowVerticalLines(false);
        orderTable.setShowHorizontalLines(true);
        orderTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 15));
        orderTable.getTableHeader().setForeground(TEXT_DARK);
        orderTable.getTableHeader().setBackground(GRAY_LIGHT);
        orderTable.getTableHeader().setBorder(BorderFactory.createEmptyBorder());
        orderTable.setIntercellSpacing(new Dimension(0, 0));

        orderTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        orderTable.getColumnModel().getColumn(1).setPreferredWidth(450);
        orderTable.getColumnModel().getColumn(2).setPreferredWidth(150);
        orderTable.getColumnModel().getColumn(3).setPreferredWidth(180);

        orderTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                label.setHorizontalAlignment(SwingConstants.LEFT);
                label.setVerticalAlignment(SwingConstants.TOP);
                label.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                return label;
            }
        });

        orderTable.getColumnModel().getColumn(1).setCellRenderer(new ProductCellRenderer());
        orderTable.getColumnModel().getColumn(2).setCellRenderer(new StatusCellRenderer());
        orderTable.getColumnModel().getColumn(3).setCellRenderer(new ActionCellRenderer());
        orderTable.getColumnModel().getColumn(3).setCellEditor(new ActionCellEditor(orderTable));

        JScrollPane scrollPane = new JScrollPane(orderTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(GRAY_BORDER));
        scrollPane.setBackground(WHITE);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        tablePanel.add(scrollPane, BorderLayout.CENTER);

        return tablePanel;
    }

    private JPanel createFooterPanel() {
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setBackground(WHITE);
        footerPanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        leftPanel.setBackground(WHITE);

        leftPanel.add(Box.createHorizontalStrut(20));


        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.setBackground(WHITE);

        totalPriceLabel = new JLabel("Total Harga: Rp 0.00");
        totalPriceLabel.setFont(new Font("Arial", Font.BOLD, 16));
        totalPriceLabel.setForeground(TEXT_DARK);

        rightPanel.add(totalPriceLabel);

        footerPanel.add(leftPanel, BorderLayout.WEST);
        footerPanel.add(rightPanel, BorderLayout.EAST);

        return footerPanel;
    }

    private void populateData() {
        populateData("Semua Pesanan");
    }

    private void populateData(String filterStatus) {
        tableModel.setRowCount(0);
        double grandTotal = 0;

        try {
            List<Order> orders = ProductRepository.getOrdersForUser(currentUser.getId());

            for (Order order : orders) {
                String dbOrderStatus = order.getOrderStatus();

                // --- Lakukan Pengecekan Status Otomatis (Dikirim -> Selesai) ---
                if ("Shipped".equalsIgnoreCase(dbOrderStatus)) {
                    LocalDateTime orderCreatedAt = getOrderCreatedAtFromDb(order.getId());
                    if (orderCreatedAt != null) {
                        Duration duration = Duration.between(orderCreatedAt, LocalDateTime.now());
                        if (duration.toDays() >= 10) {
                            ProductRepository.updateOrderStatus(order.getId(), "Delivered");
                            order.setOrderStatus("Delivered");
                            dbOrderStatus = "Delivered";
                            System.out.println("DEBUG: Pesanan ID " + order.getId() + " otomatis menjadi Selesai karena sudah lebih dari 10 hari.");
                        }
                    }
                }
                // --- Akhir Pengecekan Status Otomatis ---


                String displayStatus = mapDbStatusToDisplay(dbOrderStatus);

                if (!filterStatus.equals("Semua Pesanan")) {
                    if (filterStatus.equals("Belum Bayar")) {
                        if (!"Pending Payment".equalsIgnoreCase(dbOrderStatus)) {
                            continue;
                        }
                    } else if (!displayStatus.equalsIgnoreCase(filterStatus)) {
                        continue;
                    }
                }

                String orderDateFormatted = order.getOrderDate().format(DateTimeFormatter.ofPattern("dd 'MMMM' 20yy", new Locale("id", "ID")));
                String orderDetailsHtml = String.format(
                    "<html><div style='padding: 10px;'>" +
                    "<div style='font-weight: bold; font-size: 14px; margin-bottom: 5px;'>Pesanan : #%s</div>" +
                    "<div style='color: #666; font-size: 12px;'>Pembayaran Pesanan : %s</div>" +
                    "<div style='color: #666; font-size: 12px; margin-top: 5px;'>Metode Pembayaran: %s</div>" +
                    "</div></html>",
                    order.getOrderNumber(), orderDateFormatted, order.getPaymentMethod()
                );

                List<OrderItem> orderItems = order.getItems();

                String statusAndDeliveryDetails = dbOrderStatus + "|" + order.getDeliveryEstimate();
                Object[] rowData = {
                    orderDetailsHtml,
                    orderItems, // List of OrderItem
                    statusAndDeliveryDetails,
                    // Untuk Aksi, kita perlu meneruskan objek Order agar ActionCellEditor dapat mengakses detail
                    order
                };
                tableModel.addRow(rowData);
                grandTotal += order.getTotalAmount();
            }
        } catch (SQLException e) {
            System.err.println("Error memuat riwayat pesanan: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal memuat riwayat pesanan: " + e.getMessage(), "Error Database", JOptionPane.ERROR_MESSAGE);
        }

        totalPriceLabel.setText("Total Harga: " + formatCurrency(grandTotal));
    }

    // Metode untuk mendapatkan created_at dari database (dipindahkan ke sini dari ActionCellEditor)
    private LocalDateTime getOrderCreatedAtFromDb(int orderId) throws SQLException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        LocalDateTime createdAt = null;
        try {
            conn = DatabaseConnection.getConnection();
            String sql = "SELECT created_at FROM orders WHERE id = ?";
            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, orderId);
            rs = stmt.executeQuery();
            if (rs.next()) {
                Timestamp timestamp = rs.getTimestamp("created_at");
                if (timestamp != null) {
                    createdAt = timestamp.toLocalDateTime();
                }
            }
        } finally {
            DatabaseConnection.closeConnection(conn, stmt, rs);
        }
        return createdAt;
    }

    private String getColorForStatusHtml(String status) {
        if (status == null) return "#333";
        return switch (status.toLowerCase()) {
            case "selesai" -> String.format("#%06X", (SUCCESS_GREEN.getRGB() & 0xFFFFFF));
            case "dibatalkan" -> String.format("#%06X", (CANCEL_RED.getRGB() & 0xFFFFFF));
            case "belum bayar" -> String.format("#%06X", (PENDING_YELLOW.getRGB() & 0xFFFFFF));
            case "dikemas", "dikirim" -> String.format("#%06X", (PROCESSING_BLUE.getRGB() & 0xFFFFFF));
            default -> "#333";
        };
    }

    private String mapDbStatusToDisplay(String dbStatus) {
        if (dbStatus == null) return "N/A";
        return switch (dbStatus.toLowerCase()) {
            case "processing" -> "Dikemas";
            case "shipped" -> "Dikirim";
            case "delivered" -> "Selesai";
            case "cancelled" -> "Dibatalkan";
            case "pending payment" -> "Belum Bayar";
            default -> dbStatus;
        };
    }

    private String mapDisplayStatusToDb(String displayStatus) {
        if (displayStatus == null) return null;
        return switch (displayStatus.toLowerCase()) {
            case "dikemas" -> "Processing";
            case "dikirim" -> "Shipped";
            case "selesai" -> "Delivered";
            case "dibatalkan" -> "Cancelled";
            case "belum bayar" -> "Pending Payment";
            default -> displayStatus;
        };
    }

    private String formatCurrency(double amount) {
        return String.format(Locale.forLanguageTag("id-ID"), "Rp %,.2f", amount);
    }

    class ProductCellRenderer extends JPanel implements TableCellRenderer {
        private final JLabel imageLabel;
        private final JPanel textPanel;

        public ProductCellRenderer() {
            setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));
            setOpaque(false);

            imageLabel = new JLabel();
            imageLabel.setPreferredSize(new Dimension(80, 80));
            imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
            imageLabel.setVerticalAlignment(SwingConstants.CENTER);

            textPanel = new JPanel();
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
            textPanel.setOpaque(false);
            textPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            textPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

            add(imageLabel);
            add(textPanel);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            @SuppressWarnings("unchecked")
            List<OrderItem> items = (List<OrderItem>) value;

            textPanel.removeAll();
            imageLabel.setIcon(null);
            imageLabel.setText("");

            if (items != null && !items.isEmpty()) {
                OrderItem firstItem = items.get(0);

                Image mainImage = firstItem.getLoadedImage();

                if (mainImage != null) {
                    Image scaledImage = mainImage.getScaledInstance(
                            imageLabel.getPreferredSize().width,
                            imageLabel.getPreferredSize().height,
                            Image.SCALE_SMOOTH);
                    imageLabel.setIcon(new ImageIcon(scaledImage));
                } else {
                    imageLabel.setText("N/A");
                    imageLabel.setFont(new Font("Arial", Font.ITALIC, 10));
                    imageLabel.setForeground(GRAY_TEXT_COLOR);
                }

                JLabel firstProductInfoLabel = new JLabel(String.format(
                    "<html><b>%s</b><br>" +
                    "<span style='color: %s; font-size: 11px;'>Oleh: %s</span><br>" +
                    "<span style='font-size: 12px;'>" +
                    "Ukuran: S &nbsp;&nbsp; Qty: %d &nbsp;&nbsp; <span style='font-weight: bold;'>Harga %s</span>" +
                    "</span></html>",
                    firstItem.getProductName(),
                    String.format("#%06X", (SELLER_TEXT_COLOR.getRGB() & 0xFFFFFF)),
                    (firstItem.getSellerName() != null ? firstItem.getSellerName() : "Penjual"),
                    firstItem.getQuantity(),
                    formatCurrency(firstItem.getPricePerItem())
                ));
                firstProductInfoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                firstProductInfoLabel.setVerticalAlignment(SwingConstants.TOP);
                textPanel.add(firstProductInfoLabel);

                if (items.size() > 1) {
                    JLabel otherProductsLabel = new JLabel("<html><span style='color: #666; font-size: 11px;'>+" + (items.size() - 1) + " lainnya</span></html>");
                    otherProductsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                    textPanel.add(otherProductsLabel);
                }

            } else {
                imageLabel.setText("N/A");
                imageLabel.setFont(new Font("Arial", Font.ITALIC, 10));
                imageLabel.setForeground(GRAY_TEXT_COLOR);

                JLabel noProductLabel = new JLabel("Tidak ada item produk.");
                noProductLabel.setFont(new Font("Arial", Font.ITALIC, 12));
                noProductLabel.setForeground(GRAY_TEXT_COLOR);
                textPanel.add(noProductLabel);
            }

            if (isSelected) {
                setBackground(table.getSelectionBackground());
                textPanel.setBackground(table.getSelectionBackground());
            } else {
                setBackground(table.getBackground());
                textPanel.setBackground(table.getBackground());
            }
            return this;
        }
    }


    // Custom cell renderer untuk kolom status
    class StatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setHorizontalAlignment(SwingConstants.LEFT);
            label.setVerticalAlignment(SwingConstants.TOP);
            label.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            label.setOpaque(false);

            if (value != null) {
                String combinedValue = (String) value;
                String[] parts = combinedValue.split("\\|");
                String dbOrderStatus = parts[0];
                String deliveryEstimate = (parts.length > 1 && !parts[1].equals("null")) ? parts[1] : "N/A";

                String displayStatus = mapDbStatusToDisplay(dbOrderStatus);
                String textColorHex = getColorForStatusHtml(displayStatus);

                String formattedDeliveryDate = "N/A";
                if (!deliveryEstimate.equals("N/A")) {
                    try {
                        String[] days = deliveryEstimate.replaceAll("[^0-9-]", "").split("-");
                        if (days.length == 2) {
                            int minDays = Integer.parseInt(days[0]);
                            int maxDays = Integer.parseInt(days[1]);
                            LocalDate minDate = LocalDate.now().plusDays(minDays);
                            LocalDate maxDate = LocalDate.now().plusDays(maxDays);
                            formattedDeliveryDate = minDate.format(DateTimeFormatter.ofPattern("dd MMM")) + " - " +
                                                    maxDate.format(DateTimeFormatter.ofPattern("dd MMM"));
                        } else {
                            formattedDeliveryDate = deliveryEstimate;
                        }
                    } catch (NumberFormatException ex) {
                        formattedDeliveryDate = deliveryEstimate;
                    }
                } else {
                    formattedDeliveryDate = LocalDate.now().plusDays(3).format(DateTimeFormatter.ofPattern("dd MMMM 20yy", new Locale("id", "ID")));
                }


                String displayText = String.format(
                    "<html><div style='padding: 0px;'>" +
                    "<div style='margin-bottom: 5px; color: %s;'>Status</div>" +
                    "<div style='color: %s; font-weight: bold;'>%s</div>" +
                    "<div style='margin-top: 10px; color: %s; font-size: 11px;'>Perkiraan Pengiriman</div>" +
                    "<div style='color: %s; font-weight: bold;'>%s</div>" +
                    "</div></html>",
                    String.format("#%06X", (SUBTEXT_GRAY.getRGB() & 0xFFFFFF)),
                    textColorHex,
                    displayStatus,
                    String.format("#%06X", (SUBTEXT_GRAY.getRGB() & 0xFFFFFF)),
                    String.format("#%06X", (TEXT_DARK.getRGB() & 0xFFFFFF)),
                    formattedDeliveryDate
                );

                label.setText(displayText);
            }
            if (isSelected) {
                setBackground(table.getSelectionBackground());
            } else {
                setBackground(table.getBackground());
            }
            return label;
        }
    }

    class ActionCellRenderer extends JPanel implements TableCellRenderer {
        private JButton detailButton;
        private JButton buyNowBtn;
        private JButton cancelOrderBtn;
        private JButton confirmPaymentBtn;
        private JButton orderReceivedBtn;
        private JButton reviewProductBtn; 

        public ActionCellRenderer() {
            setLayout(new FlowLayout(FlowLayout.LEFT, 5, 15));
            setOpaque(false);

            detailButton = new JButton("Detail");
            detailButton.setFont(new Font("Arial", Font.PLAIN, 12));
            detailButton.setBackground(BLUE_PRIMARY);
            detailButton.setForeground(WHITE);
            detailButton.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
            detailButton.setFocusPainted(false);

            buyNowBtn = new JButton("Beli Lagi");
            buyNowBtn.setFont(new Font("Arial", Font.BOLD, 12));
            buyNowBtn.setBackground(ORANGE_PRIMARY);
            buyNowBtn.setForeground(WHITE);
            buyNowBtn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
            buyNowBtn.setFocusPainted(false);

            cancelOrderBtn = new JButton("Batalkan Pesanan");
            cancelOrderBtn.setFont(new Font("Arial", Font.PLAIN, 12));
            cancelOrderBtn.setBackground(CANCEL_RED);
            cancelOrderBtn.setForeground(WHITE);
            cancelOrderBtn.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
            cancelOrderBtn.setFocusPainted(false);

            confirmPaymentBtn = new JButton("Konfirmasi Bayar");
            confirmPaymentBtn.setFont(new Font("Arial", Font.BOLD, 11));
            confirmPaymentBtn.setBackground(ORANGE_PRIMARY);
            confirmPaymentBtn.setForeground(WHITE);
            confirmPaymentBtn.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
            confirmPaymentBtn.setFocusPainted(false);

            orderReceivedBtn = new JButton("Pesanan Diterima");
            orderReceivedBtn.setFont(new Font("Arial", Font.BOLD, 11));
            orderReceivedBtn.setBackground(SUCCESS_GREEN);
            orderReceivedBtn.setForeground(WHITE);
            orderReceivedBtn.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
            orderReceivedBtn.setFocusPainted(false);

            reviewProductBtn = new JButton("Beri Review");
            reviewProductBtn.setFont(new Font("Arial", Font.BOLD, 12));
            reviewProductBtn.setBackground(ORANGE_PRIMARY);
            reviewProductBtn.setForeground(WHITE);
            reviewProductBtn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
            reviewProductBtn.setFocusPainted(false);


            add(detailButton);
            add(buyNowBtn);
            add(cancelOrderBtn);
            add(confirmPaymentBtn);
            add(orderReceivedBtn);
            add(reviewProductBtn);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            Order order = (Order) value; // Value adalah objek Order
            String status = order.getOrderStatus(); // Ambil status dari objek Order
            
            detailButton.setVisible(true);
            buyNowBtn.setVisible(false);
            cancelOrderBtn.setVisible(false);
            confirmPaymentBtn.setVisible(false);
            orderReceivedBtn.setVisible(false);
            reviewProductBtn.setVisible(false); // Sembunyikan secara default

            if ("Delivered".equalsIgnoreCase(status)) { // Status di DB
                buyNowBtn.setVisible(true);
                boolean hasReviewedAnyItem = false;
                if (currentUser != null && order.getItems() != null && !order.getItems().isEmpty()) {
                    for (OrderItem item : order.getItems()) {
                        try {
                            if (ProductRepository.hasUserReviewedProduct(currentUser.getId(), item.getProductId())) {
                                hasReviewedAnyItem = true;
                                break;
                            }
                        } catch (SQLException e) {
                            System.err.println("Error checking review status for product " + item.getProductName() + ": " + e.getMessage());
                            hasReviewedAnyItem = true; // Anggap sudah direview untuk menghindari error berulang
                        }
                    }
                }
                
                if (!hasReviewedAnyItem) {
                    reviewProductBtn.setVisible(true); // Tampilkan jika belum ada item yang direview
                } else {
                    reviewProductBtn.setVisible(false); // Sembunyikan jika setidaknya ada satu item yang direview
                }
                // --- Akhir Perbaikan ---

            } else if ("Shipped".equalsIgnoreCase(status)) {
                orderReceivedBtn.setVisible(true);
            } else if ("Processing".equalsIgnoreCase(status)) {
                cancelOrderBtn.setVisible(true);
            } else if ("Pending Payment".equalsIgnoreCase(status)) {
                confirmPaymentBtn.setVisible(true);
                cancelOrderBtn.setVisible(true);
            } else if ("Cancelled".equalsIgnoreCase(status)) {
                buyNowBtn.setVisible(true);
            }

            if (isSelected) {
                setBackground(table.getSelectionBackground());
            } else {
                setBackground(table.getBackground());
            }
            return this;
        }
    }

    class ActionCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final ActionCellRenderer renderer;
        private final JTable table;
        private int currentRow;

        public ActionCellEditor(JTable table) {
            this.table = table;
            renderer = new ActionCellRenderer();

            renderer.detailButton.addActionListener(e -> {
                Order order = (Order) table.getModel().getValueAt(currentRow, 3); // Ambil objek Order
                if (order != null) {
                    if (viewController != null) {
                        viewController.showOrderDetailView(order.getId());
                    } else {
                        JOptionPane.showMessageDialog(OrderHistory.this, "Menampilkan detail untuk pesanan: " + order.getOrderNumber() + " (ID: " + order.getId() + ")", "Detail Pesanan", JOptionPane.INFORMATION_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(OrderHistory.this, "Detail pesanan tidak ditemukan.", "Error", JOptionPane.ERROR_MESSAGE);
                }
                fireEditingStopped();
            });


            renderer.buyNowBtn.addActionListener(e -> {
                Order order = (Order) table.getModel().getValueAt(currentRow, 3);
                JOptionPane.showMessageDialog(OrderHistory.this, "Beli Lagi diklik untuk pesanan: " + order.getOrderNumber() + "!", "Aksi Pesanan", JOptionPane.INFORMATION_MESSAGE);
                fireEditingStopped();
            });

            renderer.cancelOrderBtn.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(OrderHistory.this, "Apakah Anda yakin ingin membatalkan pesanan ini?", "Konfirmasi Pembatalan", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    Order order = (Order) table.getModel().getValueAt(currentRow, 3);
                    try {
                        if (order != null) {
                            LocalDateTime orderCreatedAt = getOrderCreatedAtFromDb(order.getId());
                            if (orderCreatedAt == null) {
                                JOptionPane.showMessageDialog(OrderHistory.this, "Tanggal pembuatan pesanan tidak ditemukan. Tidak dapat membatalkan.", "Error", JOptionPane.ERROR_MESSAGE);
                                fireEditingStopped();
                                return;
                            }

                            Duration duration = Duration.between(orderCreatedAt, LocalDateTime.now());
                            if (duration.toHours() >= 1) {
                                JOptionPane.showMessageDialog(OrderHistory.this, "Pesanan tidak dapat dibatalkan. Pembatalan hanya diperbolehkan dalam 1 jam setelah pesanan masuk status 'Dikemas'.", "Pembatasan Pembatalan", JOptionPane.WARNING_MESSAGE);
                                fireEditingStopped();
                                return;
                            }

                            ProductRepository.updateOrderStatus(order.getId(), "Cancelled");
                            populateData();
                            JOptionPane.showMessageDialog(OrderHistory.this, "Pesanan " + order.getOrderNumber() + " telah dibatalkan.", "Pesanan Dibatalkan", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(OrderHistory.this, "Pesanan tidak ditemukan di database.", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (SQLException ex) {
                        System.err.println("Error membatalkan pesanan: " + ex.getMessage());
                        JOptionPane.showMessageDialog(OrderHistory.this, "Gagal membatalkan pesanan: " + ex.getMessage(), "Error Database", JOptionPane.ERROR_MESSAGE);
                    }
                }
                fireEditingStopped();
            });

            renderer.confirmPaymentBtn.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(OrderHistory.this, "Apakah Anda yakin sudah melakukan pembayaran dan ingin mengonfirmasi?", "Konfirmasi Pembayaran", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    Order order = (Order) table.getModel().getValueAt(currentRow, 3);
                    try {
                        if (order != null) {
                            ProductRepository.updateOrderStatus(order.getId(), "Processing");
                            populateData();
                            JOptionPane.showMessageDialog(OrderHistory.this, "Pembayaran untuk pesanan " + order.getOrderNumber() + " telah dikonfirmasi. Pesanan akan segera diproses.", "Pembayaran Dikonfirmasi", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(OrderHistory.this, "Pesanan tidak ditemukan di database.", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (SQLException ex) {
                        System.err.println("Error mengonfirmasi pembayaran: " + ex.getMessage());
                        JOptionPane.showMessageDialog(OrderHistory.this, "Gagal mengonfirmasi pembayaran: " + ex.getMessage(), "Error Database", JOptionPane.ERROR_MESSAGE);
                    }
                }
                fireEditingStopped();
            });

            renderer.orderReceivedBtn.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(OrderHistory.this, "Apakah Anda yakin sudah menerima pesanan ini?", "Konfirmasi Penerimaan", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    Order order = (Order) table.getModel().getValueAt(currentRow, 3);
                    try {
                        if (order != null) {
                            ProductRepository.updateOrderStatus(order.getId(), "Delivered");
                            populateData();
                            JOptionPane.showMessageDialog(OrderHistory.this, "Pesanan " + order.getOrderNumber() + " telah ditandai sebagai diterima.", "Penerimaan Pesanan", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(OrderHistory.this, "Pesanan tidak ditemukan.", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (SQLException ex) {
                        System.err.println("Error mengonfirmasi penerimaan pesanan: " + ex.getMessage());
                        JOptionPane.showMessageDialog(OrderHistory.this, "Gagal mengonfirmasi penerimaan pesanan: " + ex.getMessage(), "Error Database", JOptionPane.ERROR_MESSAGE);
                    }
                }
                fireEditingStopped();
            });

            // Aksi untuk tombol "Beri Review"
            renderer.reviewProductBtn.addActionListener(e -> {
                Order order = (Order) table.getModel().getValueAt(currentRow, 3);
                try {
                    if (order != null && order.getItems() != null && !order.getItems().isEmpty()) {
                        // Untuk kesederhanaan, review item pertama di order
                        OrderItem firstItem = order.getItems().get(0);
                        int productId = firstItem.getProductId();
                        String productName = firstItem.getProductName();
                        // Asumsi productImage di OrderItem adalah path atau bisa dikonversi ke path
                        // atau Anda perlu memuatnya dari database di ProductReview
                        // Untuk saat ini, kita akan mencoba mendapatkan path dari OrderItem.
                        // Jika ProductImage adalah BLOB di OrderItem, Anda tidak bisa langsung
                        // mengirimnya sebagai String path. Anda mungkin perlu mengubah
                        // OrderItem untuk menyimpan path juga atau mengubah ProductReview
                        // untuk menerima BLOB. Untuk konsistensi, saya asumsikan
                        // productImagePath di ProductReview adalah string path.
                        String productImage = ""; // Default kosong
                        if (firstItem.getLoadedImage() != null) {
                            // Jika OrderItem memuat image sebagai BLOB, kita tidak bisa langsung
                            // mendapatkan path-nya. Perlu mekanisme berbeda untuk mendapatkan path
                            // atau membuat ProductReview menerima BLOB.
                            // Untuk kasus ini, kita akan mengirim path kosong, dan ProductReview
                            // akan mencoba mengambil gambar dari path jika ada.
                            // Jika gambar produk disimpan sebagai BLOB di DB dan di OrderItem,
                            // maka Anda perlu mengambil BLOB itu di OrderReview untuk ditampilkan.
                            // Untuk saat ini, kita biarkan kosong jika tidak ada path.
                            // Atau, jika Anda punya path standar, gunakan itu.
                        }
                        
                        double productPrice = firstItem.getPricePerItem();
                        String sellerName = firstItem.getSellerName();

                        if (viewController != null) {
                            viewController.showProductReviewView(productId, productName, productImage, productPrice, sellerName);
                        } else {
                            JOptionPane.showMessageDialog(OrderHistory.this, "Membuka halaman review untuk produk: " + productName, "Review Produk", JOptionPane.INFORMATION_MESSAGE);
                        }
                    } else {
                        JOptionPane.showMessageDialog(OrderHistory.this, "Tidak ada produk untuk direview pada pesanan ini.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) { // Catch semua Exception untuk debugging
                    System.err.println("Error saat menyiapkan review produk: " + ex.getMessage());
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(OrderHistory.this, "Gagal memuat detail produk untuk review: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
                fireEditingStopped();
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            currentRow = row;
            // Penting: ActionCellRenderer sekarang menerima Order object, bukan String status
            // Jadi panggil getTableCellRendererComponent dengan objek Order yang sebenarnya
            renderer.getTableCellRendererComponent(table, value, isSelected, true, row, column);
            return renderer;
        }

        @Override
        public Object getCellEditorValue() {
            return ""; // Tidak perlu mengembalikan nilai
        }
        
        // Metode ini mungkin tidak lagi relevan jika orderNumber diambil langsung dari objek Order
        // Tapi tetap ada jika digunakan di tempat lain yang memparsing HTML
        private String getOrderNumberFromRow(int row) {
            String orderDetailsHtml = (String) table.getModel().getValueAt(row, 0);
            String orderNumber = "";
            int startIndex = orderDetailsHtml.indexOf("#") + 1;
            int endIndex = orderDetailsHtml.indexOf("<", startIndex);
            if (startIndex > 0 && endIndex > startIndex) {
                orderNumber = orderDetailsHtml.substring(startIndex, endIndex).trim();
            }
            return orderNumber;
        }

        private LocalDateTime getOrderCreatedAtFromDb(int orderId) throws SQLException {
            Connection conn = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            LocalDateTime createdAt = null;
            try {
                conn = DatabaseConnection.getConnection();
                String sql = "SELECT created_at FROM orders WHERE id = ?";
                stmt = conn.prepareStatement(sql);
                stmt.setInt(1, orderId);
                rs = stmt.executeQuery();
                if (rs.next()) {
                    Timestamp timestamp = rs.getTimestamp("created_at");
                    if (timestamp != null) {
                        createdAt = timestamp.toLocalDateTime();
                    }
                }
            } finally {
                DatabaseConnection.closeConnection(conn, stmt, rs);
            }
            return createdAt;
        }
    }
}