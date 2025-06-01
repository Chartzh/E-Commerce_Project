package e.commerce;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.swing.border.EmptyBorder;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class FavoritesUI extends JPanel {
    private List<FavoriteItem> favoriteItems;
    private JPanel favoriteProductsPanel;

    public FavoritesUI() {
        initializeDummyData();
        initializeUI();
    }

    private void initializeDummyData() {
        favoriteItems = new ArrayList<>();
        // Memastikan jalur gambar sesuai dengan struktur proyek Anda
        // Misalnya, jika gambar Anda ada di 'YourProject/src/Resources/Images/'
        favoriteItems.add(new FavoriteItem(1, "Wireless Headphone", 249000, 299000, 1, "#FFB6C1", "src/Resources/Images/wireless_headphone.jpg"));
        favoriteItems.add(new FavoriteItem(2, "Bluetooth Speaker", 189000, 219000, 2, "#ADD8E6", "src/Resources/Images/bluetooth_speaker.jpg"));
        favoriteItems.add(new FavoriteItem(3, "Gaming Mouse", 329000, 399000, 1, "#90EE90", "src/Resources/Images/gaming_mouse.jpg"));
        favoriteItems.add(new FavoriteItem(4, "Mechanical Keyboard", 450000, 499000, 1, "#FFFACD", "src/Resources/Images/mechanical_keyboard.jpg"));
        favoriteItems.add(new FavoriteItem(5, "Smartwatch X200", 1800000, 2100000, 5, "#D8BFD8", "src/Resources/Images/smartwatch.jpg"));
        favoriteItems.add(new FavoriteItem(6, "Portable SSD 1TB", 950000, 1200000, 3, "#B0E0E6", "src/Resources/Images/ssd.jpg"));
        favoriteItems.add(new FavoriteItem(7, "USB-C Hub Multiport", 300000, 350000, 10, "#FFDAB9", "src/Resources/Images/usb_hub.jpg"));
        favoriteItems.add(new FavoriteItem(8, "Webcam Full HD", 400000, 480000, 7, "#C0C0C0", "src/Resources/Images/webcam.jpg"));
    }

    private void initializeUI() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        // Header Section
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // Main Content
        JPanel contentPanel = createContentPanel();
        add(contentPanel, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(new EmptyBorder(20, 20, 10, 20));

        // Title
        JLabel titleLabel = new JLabel("My Favorites");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(Color.BLACK);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JButton continueShoppingBtn = new JButton("Continue Shopping");
        continueShoppingBtn.setBackground(Color.WHITE);
        continueShoppingBtn.setForeground(new Color(255, 69, 0));
        continueShoppingBtn.setBorderPainted(false);
        continueShoppingBtn.setFocusPainted(false);
        continueShoppingBtn.setFont(new Font("Arial", Font.BOLD, 14));
        continueShoppingBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        continueShoppingBtn.addActionListener(e -> {
            // Logika untuk beralih kembali ke dashboard
            // Anda mungkin perlu menyesuaikan nama "Dashboard" ini
            Container parent = getParent();
            if (parent != null && parent.getLayout() instanceof CardLayout) {
                CardLayout layout = (CardLayout) parent.getLayout();
                layout.show(parent, "Dashboard");
            }
        });

        headerPanel.add(continueShoppingBtn, BorderLayout.EAST);

        return headerPanel;
    }

    private JPanel createContentPanel() {
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(Color.WHITE);

        if (favoriteItems.isEmpty()) {
            // Empty state jika tidak ada item favorit
            JPanel emptyPanel = createEmptyStatePanel();
            contentPanel.add(emptyPanel, BorderLayout.CENTER);
        } else {
            // Grid untuk menampilkan produk (4 kolom per baris)
            favoriteProductsPanel = new JPanel(new GridLayout(0, 4, 20, 20)); // 0 baris (otomatis), 4 kolom, gap 20px
            favoriteProductsPanel.setBackground(Color.WHITE);
            favoriteProductsPanel.setBorder(new EmptyBorder(10, 20, 20, 20));

            refreshFavoritesList(); // Memuat ulang daftar produk

            JScrollPane scrollPane = new JScrollPane(favoriteProductsPanel);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setBorder(null); // Menghilangkan border default scroll pane
            scrollPane.getVerticalScrollBar().setUnitIncrement(16); // Mempercepat scrolling

            contentPanel.add(scrollPane, BorderLayout.CENTER);
        }

        return contentPanel;
    }

    private JPanel createEmptyStatePanel() {
        JPanel emptyPanel = new JPanel(new BorderLayout());
        emptyPanel.setBackground(Color.WHITE);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(Color.WHITE);

        // Ikon hati besar
        JLabel heartIcon = new JLabel("♡");
        heartIcon.setFont(new Font("Arial", Font.PLAIN, 80));
        heartIcon.setForeground(Color.LIGHT_GRAY);
        heartIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Judul kosong
        JLabel emptyTitle = new JLabel("No Favorites Yet");
        emptyTitle.setFont(new Font("Arial", Font.BOLD, 24));
        emptyTitle.setForeground(Color.GRAY);
        emptyTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Deskripsi kosong
        JLabel emptyDesc = new JLabel("Start adding products to your favorites!");
        emptyDesc.setFont(new Font("Arial", Font.PLAIN, 16));
        emptyDesc.setForeground(Color.LIGHT_GRAY);
        emptyDesc.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Menambahkan komponen ke panel tengah
        centerPanel.add(Box.createVerticalGlue()); // Untuk menengahkan vertikal
        centerPanel.add(heartIcon);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 20))); // Spasi
        centerPanel.add(emptyTitle);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Spasi
        centerPanel.add(emptyDesc);
        centerPanel.add(Box.createVerticalGlue()); // Untuk menengahkan vertikal

        emptyPanel.add(centerPanel, BorderLayout.CENTER);
        return emptyPanel;
    }

    private void refreshFavoritesList() {
        favoriteProductsPanel.removeAll(); // Hapus semua produk yang ada

        for (FavoriteItem item : favoriteItems) {
            JPanel productCard = createFavoriteProductCard(item);
            favoriteProductsPanel.add(productCard);
        }

        favoriteProductsPanel.revalidate(); // Validasi ulang layout
        favoriteProductsPanel.repaint();    // Gambar ulang panel
    }

    private JPanel createFavoriteProductCard(FavoriteItem item) {
        JPanel card = new JPanel();
        card.setLayout(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230), 1));
        // Sesuaikan preferred size card agar proporsional dengan 4 kolom dan area gambar 4:3
        card.setPreferredSize(new Dimension(240, 300)); // Lebar 240, Tinggi 300 (Contoh: 180 untuk gambar, sisa untuk info)

        // Product Image Panel
        JPanel imagePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR); // Perhalus skala gambar

                if (item.getImage() == null) {
                    // Fallback ke background gradient jika gambar null atau gagal dimuat
                    GradientPaint gradient = new GradientPaint(
                            0, 0, item.getBgColor(),
                            getWidth(), getHeight(), item.getBgColor().darker());
                    g2d.setPaint(gradient);
                    g2d.fillRect(0, 0, getWidth(), getHeight());

                    g2d.setColor(Color.WHITE);
                    g2d.setFont(new Font("Arial", Font.BOLD, 14));
                    FontMetrics fm = g2d.getFontMetrics();
                    String text = "IMG";
                    int textWidth = fm.stringWidth(text);
                    int textHeight = fm.getHeight();
                    g2d.drawString(text, (getWidth() - textWidth) / 2, (getHeight() + textHeight) / 2 - fm.getDescent());
                } else {
                    // Logika untuk cropping gambar agar mengisi penuh panel sambil mempertahankan rasio asli
                    double panelAspectRatio = (double) getWidth() / getHeight();
                    double imageAspectRatio = (double) item.getImage().getWidth(this) / item.getImage().getHeight(this);

                    int sourceX = 0;
                    int sourceY = 0;
                    int sourceWidth = item.getImage().getWidth(this);
                    int sourceHeight = item.getImage().getHeight(this);
                    int destX = 0;
                    int destY = 0;
                    int destWidth = getWidth();
                    int destHeight = getHeight();

                    if (imageAspectRatio > panelAspectRatio) {
                        // Gambar lebih lebar dari panel, crop bagian samping kiri dan kanan
                        int scaledHeight = getHeight();
                        int originalImageWidth = item.getImage().getWidth(this);
                        int originalImageHeight = item.getImage().getHeight(this);
                        
                        // Hitung lebar sumber yang perlu diambil agar proporsional dengan tinggi panel
                        sourceWidth = (int) (originalImageHeight * panelAspectRatio);
                        sourceX = (originalImageWidth - sourceWidth) / 2; // Tengahkan crop horizontally
                        sourceHeight = originalImageHeight; // Ambil tinggi penuh dari gambar asli

                    } else if (imageAspectRatio < panelAspectRatio) {
                        // Gambar lebih tinggi dari panel, crop bagian atas dan bawah
                        int scaledWidth = getWidth();
                        int originalImageWidth = item.getImage().getWidth(this);
                        int originalImageHeight = item.getImage().getHeight(this);

                        // Hitung tinggi sumber yang perlu diambil agar proporsional dengan lebar panel
                        sourceHeight = (int) (originalImageWidth / panelAspectRatio);
                        sourceY = (originalImageHeight - sourceHeight) / 2; // Tengahkan crop vertically
                        sourceWidth = originalImageWidth; // Ambil lebar penuh dari gambar asli
                    }
                    // Jika rasio sama, sourceX, sourceY, sourceWidth, sourceHeight tetap sama dengan dimensi gambar asli
                    // dan destX, destY, destWidth, destHeight tetap sama dengan dimensi panel

                    g2d.drawImage(item.getImage(),
                            destX, destY, destWidth, destHeight, // Area tujuan di panel
                            sourceX, sourceY, sourceX + sourceWidth, sourceY + sourceHeight, // Area sumber dari gambar asli
                            this);
                }
            }
        };
        // Mengatur preferred size untuk imagePanel agar memiliki rasio 4:3
        imagePanel.setPreferredSize(new Dimension(240, 180)); // Rasio 4:3

        // Heart/Remove button overlay
        JPanel overlayPanel = new JPanel();
        overlayPanel.setLayout(new BorderLayout());
        overlayPanel.setOpaque(false); // Buat panel transparan agar komponen di bawahnya terlihat

        JPanel heartPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        heartPanel.setOpaque(false); // Transparan juga
        heartPanel.setBorder(new EmptyBorder(10, 10, 10, 10)); // Padding

        JButton heartButton = new JButton("♥");
        heartButton.setFont(new Font("Arial", Font.BOLD, 18));
        heartButton.setForeground(new Color(255, 69, 0)); // Warna hati merah oranye
        heartButton.setBackground(Color.WHITE);
        heartButton.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        heartButton.setFocusPainted(false);
        heartButton.setCursor(new Cursor(Cursor.HAND_CURSOR)); // Ubah kursor saat di hover
        heartButton.setToolTipText("Remove from favorites");

        // Efek hover dan aksi klik pada tombol hati
        heartButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                heartButton.setBackground(new Color(255, 240, 240)); // Warna latar belakang saat hover
            }

            @Override
            public void mouseExited(MouseEvent e) {
                heartButton.setBackground(Color.WHITE); // Kembali ke warna semula
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                int result = JOptionPane.showConfirmDialog(
                        FavoritesUI.this,
                        "Remove '" + item.getName() + "' from favorites?",
                        "Remove Favorite",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );

                if (result == JOptionPane.YES_OPTION) {
                    removeFavoriteItem(item); // Panggil metode untuk menghapus item
                }
            }
        });

        heartPanel.add(heartButton);
        overlayPanel.add(heartPanel, BorderLayout.NORTH);

        // Menggabungkan panel gambar dan panel overlay
        JPanel imageContainer = new JPanel();
        imageContainer.setLayout(new OverlayLayout(imageContainer));
        imageContainer.add(overlayPanel);
        imageContainer.add(imagePanel);

        // Product Info Panel
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(Color.WHITE);
        infoPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Label Nama Produk
        JLabel nameLabel = new JLabel(item.getName());
        nameLabel.setFont(new Font("Arial", Font.BOLD, 16));
        nameLabel.setForeground(Color.BLACK);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Label Deskripsi Produk (stok dan harga asli)
        JLabel descLabel = new JLabel("<html>" + item.getDescription() + "</html>");
        descLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        descLabel.setForeground(Color.GRAY);
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Label Harga Produk
        JLabel priceLabel = new JLabel(String.format("Rp %,.0f", item.getPrice()));
        priceLabel.setFont(new Font("Arial", Font.BOLD, 18));
        priceLabel.setForeground(new Color(255, 89, 0)); // Warna harga
        priceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Panel Tombol Aksi
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 5));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Tombol "Add to Cart"
        JButton addToCartButton = new JButton("Add to Cart");
        addToCartButton.setBackground(new Color(255, 89, 0));
        addToCartButton.setForeground(Color.WHITE);
        addToCartButton.setBorderPainted(false);
        addToCartButton.setFocusPainted(false);
        addToCartButton.setFont(new Font("Arial", Font.BOLD, 12));
        addToCartButton.setBorder(new EmptyBorder(8, 16, 8, 16));
        addToCartButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        addToCartButton.addActionListener(e -> {
            JOptionPane.showMessageDialog(
                    FavoritesUI.this,
                    "'" + item.getName() + "' added to cart!",
                    "Added to Cart",
                    JOptionPane.INFORMATION_MESSAGE
            );
        });

        // Tombol "View Details"
        JButton viewButton = new JButton("View Details");
        viewButton.setBackground(Color.WHITE);
        viewButton.setForeground(new Color(255, 89, 0));
        viewButton.setBorder(BorderFactory.createLineBorder(new Color(255, 89, 0), 1));
        viewButton.setFocusPainted(false);
        viewButton.setFont(new Font("Arial", Font.BOLD, 12));
        viewButton.setBorder(new EmptyBorder(8, 16, 8, 16));
        viewButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        viewButton.addActionListener(e -> {
            showProductDetails(item); // Panggil metode untuk menampilkan detail produk
        });

        buttonPanel.add(addToCartButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(10, 0))); // Spasi antar tombol
        buttonPanel.add(viewButton);

        // Menambahkan komponen ke infoPanel
        infoPanel.add(nameLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        infoPanel.add(descLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        infoPanel.add(priceLabel);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        infoPanel.add(buttonPanel);

        // Menambahkan imageContainer dan infoPanel ke card
        card.add(imageContainer, BorderLayout.CENTER);
        card.add(infoPanel, BorderLayout.SOUTH);

        // Efek hover pada card
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBorder(BorderFactory.createLineBorder(new Color(255, 89, 0), 2)); // Border saat di hover
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230), 1)); // Border normal
            }
        });

        return card;
    }

    private void removeFavoriteItem(FavoriteItem item) {
        favoriteItems.remove(item);

        // Jika daftar favorit kosong, tampilkan kembali panel empty state
        if (favoriteItems.isEmpty()) {
            removeAll(); // Hapus semua komponen
            add(createHeaderPanel(), BorderLayout.NORTH);
            add(createContentPanel(), BorderLayout.CENTER); // Buat ulang content panel (akan menampilkan empty state)
            revalidate();
            repaint();
        } else {
            // Jika tidak kosong, cukup refresh daftar produk
            refreshFavoritesList();
            // Re-render seluruh UI untuk memastikan panel atas dan bawah terupdate
            removeAll();
            add(createHeaderPanel(), BorderLayout.NORTH);
            add(createContentPanel(), BorderLayout.CENTER);
            revalidate();
            repaint();
        }
    }

    private void showProductDetails(FavoriteItem item) {
        // Membuat dialog modal untuk detail produk
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Product Details", true);
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(this); // Tengahkan dialog relatif terhadap FavoritesUI

        JPanel dialogPanel = new JPanel();
        dialogPanel.setLayout(new BoxLayout(dialogPanel, BoxLayout.Y_AXIS));
        dialogPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        dialogPanel.setBackground(Color.WHITE);

        // Informasi produk di dalam dialog
        JLabel nameLabel = new JLabel(item.getName());
        nameLabel.setFont(new Font("Arial", Font.BOLD, 20));
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel descLabel = new JLabel("<html><center>" + item.getDescription() + "</center></html>");
        descLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel priceLabel = new JLabel(String.format("Rp %,.0f", item.getPrice()));
        priceLabel.setFont(new Font("Arial", Font.BOLD, 18));
        priceLabel.setForeground(new Color(255, 89, 0));
        priceLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton closeButton = new JButton("Close");
        closeButton.setBackground(new Color(255, 89, 0));
        closeButton.setForeground(Color.WHITE);
        closeButton.setBorderPainted(false);
        closeButton.setFocusPainted(false);
        closeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        closeButton.addActionListener(e -> dialog.dispose()); // Menutup dialog saat tombol ditekan

        dialogPanel.add(nameLabel);
        dialogPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        dialogPanel.add(descLabel);
        dialogPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        dialogPanel.add(priceLabel);
        dialogPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        dialogPanel.add(closeButton);

        dialog.add(dialogPanel);
        dialog.setVisible(true);
    }

    // Inner class untuk merepresentasikan item favorit
    private static class FavoriteItem {
        private int id;
        private String name;
        private double price;
        private double originalPrice;
        private int stock;
        private Color bgColor;
        private String imagePath; // Jalur gambar sebagai String
        private Image image; // Objek Image yang akan dimuat

        // Constructor untuk FavoriteItem
        public FavoriteItem(int id, String name, double price, double originalPrice, int stock, String hexColor, String imagePath) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.originalPrice = originalPrice;
            this.stock = stock;
            this.bgColor = Color.decode(hexColor); // Mengubah hex string ke Color
            this.imagePath = imagePath;
            loadImage(); // Panggil metode untuk memuat gambar saat objek dibuat
        }

        // Metode untuk memuat gambar dari jalur yang diberikan
        private void loadImage() {
            try {
                // Mencoba memuat gambar sebagai resource dari classpath (cocok untuk JAR)
                java.net.URL imgURL = getClass().getClassLoader().getResource(imagePath);
                if (imgURL != null) {
                    this.image = ImageIO.read(imgURL);
                    System.out.println("Gambar berhasil dimuat dari classpath: " + imagePath);
                } else {
                    // Jika tidak ditemukan di classpath, coba muat sebagai file dari sistem file
                    File imgFile = new File(imagePath);
                    if (imgFile.exists()) {
                        this.image = ImageIO.read(imgFile);
                        System.out.println("Gambar berhasil dimuat dari file sistem: " + imagePath);
                    } else {
                        System.err.println("Gambar tidak ditemukan di: " + imagePath);
                    }
                }
            } catch (IOException e) {
                System.err.println("Gagal memuat gambar dari " + imagePath + ": " + e.getMessage());
                this.image = null; // Pastikan gambar null jika gagal dimuat
            }
        }

        // Getters untuk properti FavoriteItem
        public int getId() { return id; }
        public String getName() { return name; }
        public double getPrice() { return price; }
        public double getOriginalPrice() { return originalPrice; }
        public int getStock() { return stock; }
        public Color getBgColor() { return bgColor; }
        public String getImagePath() { return imagePath; }
        public Image getImage() { return image; }

        // Menggabungkan deskripsi produk (stok dan harga asli jika ada)
        public String getDescription() {
            StringBuilder description = new StringBuilder();
            description.append("Stok: ").append(stock);
            if (originalPrice > price) {
                description.append(" (Harga Normal: Rp ").append(String.format("%,.0f", originalPrice)).append(")");
            }
            return description.toString();
        }
    }
}