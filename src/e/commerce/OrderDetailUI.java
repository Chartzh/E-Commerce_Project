// Buat file baru: OrderDetailUI.java di package e.commerce

package e.commerce;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.List;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import e.commerce.ProductRepository.Order;
import e.commerce.ProductRepository.OrderItem;

public class OrderDetailUI extends JPanel {

    private ViewController viewController;
    private User currentUser;
    private int orderId;
    private Order currentOrder; // Objek Order lengkap yang akan ditampilkan

    // --- Konstanta Warna (Sesuaikan dengan yang ada di OrderHistory atau buat konstanta sentral) ---
    private static final Color ORANGE_PRIMARY = new Color(255, 102, 0);
    private static final Color WHITE = Color.WHITE;
    private static final Color GRAY_LIGHT = new Color(248, 248, 248);
    private static final Color GRAY_BORDER = new Color(230, 230, 230);
    private static final Color TEXT_DARK = new Color(51, 51, 51);
    private static final Color SUBTEXT_GRAY = new Color(102, 102, 102);
    private static final Color SUCCESS_GREEN = new Color(40, 167, 69);
    private static final Color CANCEL_RED = new Color(220, 53, 69);
    private static final Color PROCESSING_BLUE = new Color(0, 123, 255);
    private static final Color PENDING_YELLOW = new Color(255, 193, 7);
    private static final Color SELLER_TEXT_COLOR = new Color(70, 130, 180);

    public OrderDetailUI(ViewController viewController, int orderId) {
        this.viewController = viewController;
        this.orderId = orderId;
        this.currentUser = Authentication.getCurrentUser();

        if (this.currentUser == null) {
            System.err.println("OrderDetailUI: Tidak ada user yang login.");
            JOptionPane.showMessageDialog(this, "Anda harus login untuk melihat detail pesanan.", "Error", JOptionPane.ERROR_MESSAGE);
            if (this.viewController != null) {
                this.viewController.showDashboardView();
            }
            return;
        }

        setLayout(new BorderLayout());
        setBackground(GRAY_LIGHT);
        setBorder(new EmptyBorder(30, 40, 30, 40));

        loadOrderDetails(); // Muat detail pesanan dari database
        if (currentOrder != null) {
            createComponents();
        } else {
            add(new JLabel("Gagal memuat detail pesanan.", SwingConstants.CENTER), BorderLayout.CENTER);
        }
    }

    private void loadOrderDetails() {
        System.out.println("DEBUG OrderDetailUI: Memuat detail pesanan untuk orderId: " + orderId);
        try {
            // UBAH: Gunakan getOrderById() untuk mengambil pesanan spesifik
            currentOrder = ProductRepository.getOrderById(orderId);

            if (currentOrder == null) {
                System.err.println("DEBUG OrderDetailUI: Pesanan dengan ID " + orderId + " tidak ditemukan oleh getOrderById.");
            } else {
                System.out.println("DEBUG OrderDetailUI: Pesanan ditemukan: " + currentOrder.getOrderNumber());
                // VERIFIKASI RELEVANSI UNTUK SELLER (OPSIONAL TAPI DISARANKAN)
                // Jika ingin memastikan seller hanya bisa melihat pesanan yang relevan dengan produknya
                if (currentUser.getRole().equals("supervisor")) {
                    List<OrderItem> sellerItems = ProductRepository.getSellerSpecificOrderItemsForOrder(orderId, currentUser.getId());
                    if (sellerItems.isEmpty()) {
                        // Jika tidak ada item dari seller ini di pesanan ini, anggap tidak relevan
                        currentOrder = null; // Set null agar UI menunjukkan error
                        System.err.println("DEBUG OrderDetailUI: Pesanan ID " + orderId + " tidak mengandung produk dari seller ini (" + currentUser.getId() + ").");
                        JOptionPane.showMessageDialog(this, "Anda tidak memiliki akses untuk melihat detail pesanan ini.", "Akses Ditolak", JOptionPane.ERROR_MESSAGE);
                    } else {
                        // Opsional: Perbarui item di currentOrder agar hanya menampilkan item seller ini
                        currentOrder.setItems(sellerItems);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error memuat detail pesanan dari database di OrderDetailUI (SQL Exception): " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Gagal memuat detail pesanan: " + e.getMessage(), "Error Database", JOptionPane.ERROR_MESSAGE);
            currentOrder = null; // Pastikan null agar pesan error tampil
        }
    }

    private void createComponents() {
        JScrollPane scrollPane = new JScrollPane(createMainContentPanel());
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(GRAY_LIGHT);
        add(scrollPane, BorderLayout.CENTER);

        // Header kembali ke riwayat pesanan
        JPanel topHeaderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topHeaderPanel.setBackground(WHITE);
        topHeaderPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        JButton backButton = new JButton("← Kembali ke Riwayat Pesanan");
        backButton.setFont(new Font("Arial", Font.BOLD, 14));
        backButton.setForeground(TEXT_DARK);
        backButton.setBackground(WHITE);
        backButton.setBorderPainted(false);
        backButton.setFocusPainted(false);
        backButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backButton.addActionListener(e -> {
            if (viewController != null) {
                viewController.showOrdersView(); // Kembali ke halaman riwayat pesanan
            }
        });
        topHeaderPanel.add(backButton);
        add(topHeaderPanel, BorderLayout.NORTH);
    }

    private JPanel createMainContentPanel() {
        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setBackground(GRAY_LIGHT);
        mainContent.setBorder(new EmptyBorder(0, 0, 0, 0));

        // Ringkasan Pesanan
        mainContent.add(createOrderSummaryPanel());
        mainContent.add(Box.createVerticalStrut(20));

        // Detail Produk
        mainContent.add(createProductDetailsPanel());
        mainContent.add(Box.createVerticalStrut(20));

        // Alamat Pengiriman
        mainContent.add(createShippingAddressPanel());
        mainContent.add(Box.createVerticalStrut(20));

        // Detail Pembayaran
        mainContent.add(createPaymentDetailsPanel());

        return mainContent;
    }

    private JPanel createOrderSummaryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(GRAY_BORDER, 1),
                new EmptyBorder(20, 20, 20, 20)
        ));

        JLabel title = new JLabel("Ringkasan Pesanan");
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setForeground(TEXT_DARK);
        panel.add(title, BorderLayout.NORTH);

        JPanel details = new JPanel(new GridLayout(0, 2, 10, 10));
        details.setBackground(WHITE);
        details.setBorder(new EmptyBorder(15, 0, 0, 0));

        details.add(createDetailRow("Nomor Pesanan:", currentOrder.getOrderNumber()));
        details.add(createDetailRow("Tanggal Pesanan:", currentOrder.getOrderDate().format(DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("id", "ID")))));
        details.add(createDetailRow("Status Pesanan:", mapDbStatusToDisplay(currentOrder.getOrderStatus())));
        details.add(createDetailRow("Total Jumlah:", formatCurrency(currentOrder.getTotalAmount())));
        details.add(createDetailRow("Jasa Pengiriman:", currentOrder.getShippingServiceName()));
        details.add(createDetailRow("Biaya Pengiriman:", formatCurrency(currentOrder.getShippingCost()))); // Asumsi ada getShippingCost() di Order
        details.add(createDetailRow("Perkiraan Tiba:", currentOrder.getDeliveryEstimate() != null ? currentOrder.getDeliveryEstimate() : "N/A"));

        panel.add(details, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createProductDetailsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(GRAY_BORDER, 1),
                new EmptyBorder(20, 20, 20, 20)
        ));

        JLabel title = new JLabel("Detail Produk");
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setForeground(TEXT_DARK);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(15));

        if (currentOrder.getItems() != null && !currentOrder.getItems().isEmpty()) {
            for (OrderItem item : currentOrder.getItems()) {
                panel.add(createProductItemPanel(item));
                panel.add(Box.createVerticalStrut(10));
            }
        } else {
            JLabel noItems = new JLabel("Tidak ada item dalam pesanan ini.");
            noItems.setFont(new Font("Arial", Font.ITALIC, 14));
            noItems.setForeground(SUBTEXT_GRAY);
            noItems.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(noItems);
        }
        return panel;
    }

    private JPanel createProductItemPanel(OrderItem item) {
        JPanel itemPanel = new JPanel(new BorderLayout(10, 0));
        itemPanel.setBackground(GRAY_LIGHT); // Background sedikit abu-abu untuk setiap item
        itemPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Gambar Produk
        JLabel imageLabel = new JLabel();
        imageLabel.setPreferredSize(new Dimension(80, 80));
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setVerticalAlignment(SwingConstants.CENTER);

        if (item.getLoadedImage() != null) {
            Image mainImage = item.getLoadedImage();
            Image scaledImage = mainImage.getScaledInstance(
                    imageLabel.getPreferredSize().width,
                    imageLabel.getPreferredSize().height,
                    Image.SCALE_SMOOTH);
            imageLabel.setIcon(new ImageIcon(scaledImage));
        } else {
            imageLabel.setIcon(null);
            imageLabel.setText("No Image");
        }
        itemPanel.add(imageLabel, BorderLayout.WEST);

        // Detail Produk (Nama, Seller, Qty, Harga)
        JPanel detailsPanel = new JPanel();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
        detailsPanel.setBackground(GRAY_LIGHT);
        detailsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel nameLabel = new JLabel("<html><b>" + item.getProductName() + "</b></html>");
        nameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        nameLabel.setForeground(TEXT_DARK);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sellerLabel = new JLabel("<html><span style='color: " + String.format("#%06X", (SELLER_TEXT_COLOR.getRGB() & 0xFFFFFF)) + ";'>Oleh: " + (item.getSellerName() != null ? item.getSellerName() : "Penjual") + "</span></html>");
        sellerLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        sellerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel qtyPriceLabel = new JLabel(String.format("Ukuran: S &nbsp;&nbsp; Qty: %d &nbsp;&nbsp; Harga: %s",
                item.getQuantity(), formatCurrency(item.getPricePerItem())));
        qtyPriceLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        qtyPriceLabel.setForeground(TEXT_DARK);
        qtyPriceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        detailsPanel.add(nameLabel);
        detailsPanel.add(sellerLabel);
        detailsPanel.add(qtyPriceLabel);

        itemPanel.add(detailsPanel, BorderLayout.CENTER);

        // Total Harga per Item
        JLabel totalItemPriceLabel = new JLabel(formatCurrency(item.getPricePerItem() * item.getQuantity()));
        totalItemPriceLabel.setFont(new Font("Arial", Font.BOLD, 15));
        totalItemPriceLabel.setForeground(ORANGE_PRIMARY);
        itemPanel.add(totalItemPriceLabel, BorderLayout.EAST);

        return itemPanel;
    }


    private JPanel createShippingAddressPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(GRAY_BORDER, 1),
                new EmptyBorder(20, 20, 20, 20)
        ));

        JLabel title = new JLabel("Alamat Pengiriman");
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setForeground(TEXT_DARK);
        panel.add(title, BorderLayout.NORTH);

        JPanel details = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
        details.setBackground(WHITE);
        details.setBorder(new EmptyBorder(15, 0, 0, 0));
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));

        // Asumsi currentOrder.getFullShippingAddress() mengembalikan string lengkap
        JLabel addressLabel = new JLabel("<html>" + (currentOrder.getFullShippingAddress() != null ? currentOrder.getFullShippingAddress().replace(", ", "<br>") : "N/A") + "</html>");
        addressLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        addressLabel.setForeground(TEXT_DARK);
        addressLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        details.add(addressLabel);

        // Anda mungkin ingin menambahkan nama penerima dan nomor telepon di sini
        // currentOrder tidak menyimpan nama/telepon, tapi Authentication.getCurrentUser() punya.
        // Asumsi username adalah nama penerima dan phone adalah nomor telepon
        JLabel recipientInfo = new JLabel("<html><br>Penerima: <b>" + currentUser.getUsername() + "</b><br>Telepon: " + (currentUser.getPhone() != null ? currentUser.getPhone() : "N/A") + "</html>");
        recipientInfo.setFont(new Font("Arial", Font.PLAIN, 14));
        recipientInfo.setForeground(TEXT_DARK);
        recipientInfo.setAlignmentX(Component.LEFT_ALIGNMENT);
        details.add(recipientInfo);


        panel.add(details, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createPaymentDetailsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(GRAY_BORDER, 1),
                new EmptyBorder(20, 20, 20, 20)
        ));

        JLabel title = new JLabel("Detail Pembayaran");
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setForeground(TEXT_DARK);
        panel.add(title, BorderLayout.NORTH);

        JPanel details = new JPanel(new GridLayout(0, 2, 10, 10));
        details.setBackground(WHITE);
        details.setBorder(new EmptyBorder(15, 0, 0, 0));

        details.add(createDetailRow("Metode Pembayaran:", currentOrder.getPaymentMethod()));
        details.add(createDetailRow("Total Pembelian:", formatCurrency(calculateTotalPurchase()))); // Total tanpa ongkir
        details.add(createDetailRow("Diskon:", formatCurrency(calculateTotalDiscount()))); // Diskon dari produk
        details.add(createDetailRow("Biaya Pengiriman:", formatCurrency(currentOrder.getShippingCost())));
        details.add(createDetailRow("Total Bayar:", formatCurrency(currentOrder.getTotalAmount())));


        panel.add(details, BorderLayout.CENTER);
        return panel;
    }

    // Helper untuk membuat baris detail
    private JPanel createDetailRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(WHITE);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Arial", Font.PLAIN, 14));
        lbl.setForeground(SUBTEXT_GRAY);
        row.add(lbl, BorderLayout.WEST);

        JLabel val = new JLabel("<html><b>" + value + "</b></html>");
        val.setFont(new Font("Arial", Font.BOLD, 14));
        val.setForeground(TEXT_DARK);
        val.setHorizontalAlignment(SwingConstants.RIGHT);
        row.add(val, BorderLayout.EAST);
        return row;
    }

    // Metode bantuan untuk menghitung total pembelian (MRP - Diskon Produk)
    private double calculateTotalPurchase() {
        double total = 0;
        if (currentOrder != null && currentOrder.getItems() != null) {
            for (OrderItem item : currentOrder.getItems()) {
                total += item.getPricePerItem() * item.getQuantity();
            }
        }
        return total;
    }

    // Metode bantuan untuk menghitung total diskon
    private double calculateTotalDiscount() {
        double totalDiscount = 0;
        if (currentOrder != null && currentOrder.getItems() != null) {
            for (OrderItem item : currentOrder.getItems()) {
                totalDiscount += (item.getOriginalPricePerItem() - item.getPricePerItem()) * item.getQuantity();
            }
        }
        return totalDiscount;
    }

    // Mapping status dari DB ke tampilan (disalin dari OrderHistory)
    private String mapDbStatusToDisplay(String dbStatus) {
        if (dbStatus == null) return "N/A";
        return switch (dbStatus.toLowerCase()) {
            case "processing" -> "Dikemas"; // UBAH: "Dalam Proses" jadi "Dikemas"
            case "shipped" -> "Dikirim";
            case "delivered" -> "Selesai";
            case "cancelled" -> "Dibatalkan";
            case "pending payment" -> "Belum Bayar";
            default -> dbStatus;
        };
    }

    // Format mata uang (disalin dari OrderHistory)
    private String formatCurrency(double amount) {
        return String.format(Locale.forLanguageTag("id-ID"), "Rp %,.2f", amount);
    }
}