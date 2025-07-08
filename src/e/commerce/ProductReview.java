package e.commerce;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.io.File;
import java.net.URL;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream; // Diperlukan untuk konversi gambar ke byte[]

public class ProductReview extends JPanel {

    // --- Konstanta Warna ---
    private static final Color ORANGE_PRIMARY = new Color(255, 102, 0);
    private static final Color ORANGE_LIGHT = new Color(255, 153, 51);
    private static final Color WHITE = Color.WHITE;
    private static final Color GRAY_LIGHT = new Color(248, 248, 248);
    private static final Color GRAY_BORDER = new Color(230, 230, 230);
    private static final Color TEXT_DARK = new Color(51, 51, 51);
    private static final Color TEXT_GRAY = new Color(102, 102, 102);
    private static final Color STAR_GOLD = new Color(255, 193, 7);
    private static final Color STAR_GRAY = new Color(200, 200, 200);

    private ViewController viewController;
    private User currentUser;
    private int productId;
    private String productName;
    private String productImagePath; // Path gambar produk utama
    private double productPrice;
    private String sellerName;

    private JPanel starPanel;
    private JTextArea reviewTextArea;
    private JLabel selectedRatingLabel;
    private JButton submitButton;
    private int selectedRating = 0;

    private JLabel uploadedImagePreview; // Untuk menampilkan pratinjau gambar yang diunggah
    private byte[] selectedReviewImageData; // Untuk menyimpan data gambar sebagai byte array

    public ProductReview(ViewController viewController, int productId, String productName,
                        String productImage, double productPrice, String sellerName) {
        this.viewController = viewController;
        this.currentUser = Authentication.getCurrentUser();
        this.productId = productId;
        this.productName = productName;
        this.productImagePath = productImage; // Path gambar produk utama
        this.productPrice = productPrice;
        this.sellerName = sellerName;
        this.selectedReviewImageData = null; // Inisialisasi null untuk data gambar review

        initializeComponents();
    }

    private void initializeComponents() {
        setLayout(new BorderLayout());
        setBackground(WHITE);
        setBorder(new EmptyBorder(20, 30, 20, 30));

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(WHITE);

        JPanel headerPanel = createHeaderPanel();

        JPanel contentPanel = new JPanel(new BorderLayout(0, 30));
        contentPanel.setBackground(WHITE);
        contentPanel.setBorder(new EmptyBorder(20, 0, 0, 0));

        JPanel productInfoPanel = createProductInfoPanel();
        JPanel reviewFormPanel = createReviewFormPanel();

        contentPanel.add(productInfoPanel, BorderLayout.NORTH);
        contentPanel.add(reviewFormPanel, BorderLayout.CENTER);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(contentPanel, BorderLayout.CENTER);

        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(WHITE);
        headerPanel.setBorder(new EmptyBorder(0, 0, 20, 0));

        JButton backButton = new JButton("← Kembali");
        backButton.setFont(new Font("Arial", Font.BOLD, 14));
        backButton.setForeground(TEXT_DARK);
        backButton.setBackground(WHITE);
        backButton.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        backButton.setBorderPainted(false);
        backButton.setFocusPainted(false);
        backButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backButton.addActionListener(e -> {
            if (viewController != null) {
                viewController.showOrdersView(); // Kembali ke riwayat pesanan
            }
        });

        JLabel titleLabel = new JLabel("Tulis Review Produk");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(TEXT_DARK);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER); // Tengahkan judul

        headerPanel.add(backButton, BorderLayout.WEST);
        headerPanel.add(titleLabel, BorderLayout.CENTER);

        return headerPanel;
    }

    private JPanel createProductInfoPanel() {
        JPanel productPanel = new JPanel(new BorderLayout());
        productPanel.setBackground(WHITE);
        productPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(GRAY_BORDER, 1),
            new EmptyBorder(20, 20, 20, 20)
        ));

        JLabel imageLabel = new JLabel();
        imageLabel.setPreferredSize(new Dimension(100, 100));
        imageLabel.setBackground(GRAY_LIGHT);
        imageLabel.setOpaque(true);
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setVerticalAlignment(SwingConstants.CENTER);
        imageLabel.setText("📦"); // Placeholder sementara
        imageLabel.setFont(new Font("Arial", Font.PLAIN, 40));
        imageLabel.setBorder(BorderFactory.createLineBorder(GRAY_BORDER, 1));

        if (productImagePath != null && !productImagePath.isEmpty()) {
            System.out.println("Product Image Path received: " + productImagePath); // DEBUG
            new SwingWorker<Image, Void>() {
                @Override
                protected Image doInBackground() throws Exception {
                    BufferedImage bImage = null;
                    try {
                        // Coba muat sebagai resource dari classpath (misal: /Resources/Images/product.png)
                        URL imgURL = getClass().getResource(productImagePath);
                        if (imgURL != null) {
                            System.out.println("Attempting to load image from classpath URL: " + imgURL); // DEBUG
                            bImage = ImageIO.read(imgURL);
                        } else {
                            System.out.println("Classpath resource not found. Attempting to load from file system: " + productImagePath); // DEBUG
                            // Jika bukan resource, coba sebagai path file sistem langsung
                            File imgFile = new File(productImagePath);
                            if (imgFile.exists()) {
                                System.out.println("File exists at: " + imgFile.getAbsolutePath()); // DEBUG
                                bImage = ImageIO.read(imgFile);
                            } else {
                                System.out.println("File does NOT exist at: " + imgFile.getAbsolutePath()); // DEBUG
                            }
                        }
                    } catch (IOException e) {
                        System.err.println("Error loading product image from path: " + productImagePath + " - " + e.getMessage());
                        e.printStackTrace(); // Print stack trace for more details
                    }
                    return bImage;
                }

                @Override
                protected void done() {
                    try {
                        Image loadedImage = get(); // Dapatkan hasil dari doInBackground()
                        if (loadedImage != null) {
                            System.out.println("Image loaded successfully!"); // DEBUG
                            Image scaledImage = loadedImage.getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                            imageLabel.setIcon(new ImageIcon(scaledImage));
                            imageLabel.setText(null); // Hapus teks placeholder jika gambar berhasil dimuat
                        } else {
                            System.out.println("Loaded image is null. Displaying 'Gambar tidak ditemukan'."); // DEBUG
                            imageLabel.setText("Gambar tidak ditemukan");
                            imageLabel.setFont(new Font("Arial", Font.PLAIN, 12));
                        }
                    } catch (Exception e) {
                        System.err.println("Error processing loaded image in done(): " + e.getMessage());
                        e.printStackTrace(); // Print stack trace for more details
                        imageLabel.setText("Gambar Error");
                        imageLabel.setFont(new Font("Arial", Font.PLAIN, 12));
                    }
                    imageLabel.revalidate();
                    imageLabel.repaint();
                }
            }.execute();
        } else {
            System.out.println("productImagePath is null or empty. Cannot load image."); // DEBUG
        }

        JPanel detailsPanel = new JPanel();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
        detailsPanel.setBackground(WHITE);
        detailsPanel.setBorder(new EmptyBorder(0, 20, 0, 0));

        JLabel nameLabel = new JLabel(productName);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 18));
        nameLabel.setForeground(TEXT_DARK);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel priceLabel = new JLabel(formatCurrency(productPrice));
        priceLabel.setFont(new Font("Arial", Font.BOLD, 16));
        priceLabel.setForeground(ORANGE_PRIMARY);
        priceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sellerLabel = new JLabel("Oleh: " + sellerName);
        sellerLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        sellerLabel.setForeground(TEXT_GRAY);
        sellerLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        detailsPanel.add(nameLabel);
        detailsPanel.add(Box.createVerticalStrut(5));
        detailsPanel.add(priceLabel);
        detailsPanel.add(Box.createVerticalStrut(5));
        detailsPanel.add(sellerLabel);

        productPanel.add(imageLabel, BorderLayout.WEST);
        productPanel.add(detailsPanel, BorderLayout.CENTER);

        return productPanel;
    }

    private JPanel createReviewFormPanel() {
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(WHITE);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(GRAY_BORDER, 1),
            new EmptyBorder(25, 25, 25, 25)
        ));

        JLabel formTitle = new JLabel("Tulis Review Anda");
        formTitle.setFont(new Font("Arial", Font.BOLD, 20));
        formTitle.setForeground(TEXT_DARK);
        formTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel ratingSection = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 10));
        ratingSection.setBackground(WHITE);
        ratingSection.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel ratingLabel = new JLabel("Berikan Rating:");
        ratingLabel.setFont(new Font("Arial", Font.BOLD, 16));
        ratingLabel.setForeground(TEXT_DARK);

        starPanel = createStarRatingPanel();

        selectedRatingLabel = new JLabel("");
        selectedRatingLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        selectedRatingLabel.setForeground(TEXT_GRAY);

        ratingSection.add(ratingLabel);
        ratingSection.add(Box.createHorizontalStrut(15));
        ratingSection.add(starPanel);
        ratingSection.add(Box.createHorizontalStrut(15));
        ratingSection.add(selectedRatingLabel);

        JLabel reviewLabel = new JLabel("Tulis Review:");
        reviewLabel.setFont(new Font("Arial", Font.BOLD, 16));
        reviewLabel.setForeground(TEXT_DARK);
        reviewLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        reviewTextArea = new JTextArea(6, 50);
        reviewTextArea.setFont(new Font("Arial", Font.PLAIN, 14));
        reviewTextArea.setBackground(WHITE);
        reviewTextArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(GRAY_BORDER, 1),
            new EmptyBorder(10, 10, 10, 10)
        ));
        reviewTextArea.setLineWrap(true);
        reviewTextArea.setWrapStyleWord(true);
        reviewTextArea.setPlaceholder("Bagikan pengalaman Anda dengan produk ini...");

        JScrollPane textScrollPane = new JScrollPane(reviewTextArea);
        textScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        textScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        textScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);

        // --- Bagian Upload Gambar ---
        JLabel uploadImageLabel = new JLabel("Unggah Gambar (Opsional):");
        uploadImageLabel.setFont(new Font("Arial", Font.BOLD, 16));
        uploadImageLabel.setForeground(TEXT_DARK);
        uploadImageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel uploadPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        uploadPanel.setBackground(WHITE);
        uploadPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton selectImageButton = new JButton("Pilih Gambar");
        selectImageButton.setFont(new Font("Arial", Font.BOLD, 12));
        selectImageButton.setForeground(WHITE);
        selectImageButton.setBackground(ORANGE_PRIMARY);
        selectImageButton.setBorder(new EmptyBorder(8, 15, 8, 15));
        selectImageButton.setFocusPainted(false);
        selectImageButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        selectImageButton.addActionListener(e -> selectImageFile());
        selectImageButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                selectImageButton.setBackground(ORANGE_LIGHT);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                selectImageButton.setBackground(ORANGE_PRIMARY);
            }
        });

        uploadedImagePreview = new JLabel("Tidak ada gambar dipilih");
        uploadedImagePreview.setFont(new Font("Arial", Font.ITALIC, 12));
        uploadedImagePreview.setForeground(TEXT_GRAY);
        uploadedImagePreview.setPreferredSize(new Dimension(200, 30));

        uploadPanel.add(selectImageButton);
        uploadPanel.add(uploadedImagePreview);
        // --- Akhir Bagian Upload Gambar ---

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(WHITE);
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        submitButton = new JButton("Kirim Review");
        submitButton.setFont(new Font("Arial", Font.BOLD, 14));
        submitButton.setForeground(WHITE);
        submitButton.setBackground(ORANGE_PRIMARY);
        submitButton.setBorder(new EmptyBorder(12, 25, 12, 25));
        submitButton.setFocusPainted(false);
        submitButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        submitButton.addActionListener(e -> submitReview());

        submitButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                submitButton.setBackground(ORANGE_LIGHT);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                submitButton.setBackground(ORANGE_PRIMARY);
            }
        });

        buttonPanel.add(submitButton);

        formPanel.add(formTitle);
        formPanel.add(Box.createVerticalStrut(20));
        formPanel.add(ratingSection);
        formPanel.add(Box.createVerticalStrut(15));
        formPanel.add(reviewLabel);
        formPanel.add(Box.createVerticalStrut(8));
        formPanel.add(textScrollPane);
        formPanel.add(Box.createVerticalStrut(15));
        formPanel.add(uploadImageLabel);
        formPanel.add(Box.createVerticalStrut(8));
        formPanel.add(uploadPanel);
        formPanel.add(Box.createVerticalStrut(20));
        formPanel.add(buttonPanel);

        return formPanel;
    }

    private JPanel createStarRatingPanel() {
        JPanel starPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        starPanel.setBackground(WHITE);

        for (int i = 1; i <= 5; i++) {
            JLabel star = createStarLabel(i);
            starPanel.add(star);
        }

        return starPanel;
    }

    private JLabel createStarLabel(int rating) {
        JLabel star = new JLabel("★");
        // Menggunakan font "Dialog" yang cenderung lebih baik dalam menampilkan karakter Unicode bintang.
        // Anda juga bisa mencoba "Segoe UI Emoji" jika aplikasi hanya berjalan di Windows.
        star.setFont(new Font("Dialog", Font.PLAIN, 24)); 
        star.setForeground(STAR_GRAY);
        star.setCursor(new Cursor(Cursor.HAND_CURSOR));
        star.putClientProperty("rating", rating);

        star.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectedRating = rating;
                updateStarDisplay();
                updateRatingLabel();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                highlightStars(rating);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                updateStarDisplay();
            }
        });

        return star;
    }

    private void highlightStars(int rating) {
        Component[] stars = starPanel.getComponents();
        for (int i = 0; i < stars.length; i++) {
            JLabel star = (JLabel) stars[i];
            if (i < rating) {
                star.setForeground(STAR_GOLD);
            } else {
                star.setForeground(STAR_GRAY);
            }
        }
    }

    private void updateStarDisplay() {
        highlightStars(selectedRating);
    }

    private void updateRatingLabel() {
        if (selectedRating > 0) {
            String[] ratingTexts = {"", "Sangat Buruk", "Buruk", "Cukup", "Baik", "Sangat Baik"};
            selectedRatingLabel.setText("(" + selectedRating + "/5 - " + ratingTexts[selectedRating] + ")");
        } else {
            selectedRatingLabel.setText("");
        }
    }

    private void selectImageFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Pilih Gambar Testimoni");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Image Files", "png", "jpg", "jpeg", "gif"
        ));

        int userSelection = fileChooser.showOpenDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                BufferedImage bImage = ImageIO.read(selectedFile);
                if (bImage != null) {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    // Gunakan format PNG untuk menulis ke byte array karena lossless dan umum
                    ImageIO.write(bImage, "png", bos); 
                    selectedReviewImageData = bos.toByteArray();

                    // Tampilkan thumbnail di preview
                    Image scaledImage = bImage.getScaledInstance(30, 30, Image.SCALE_SMOOTH);
                    uploadedImagePreview.setIcon(new ImageIcon(scaledImage));
                    uploadedImagePreview.setText(selectedFile.getName()); // Tampilkan nama file
                } else {
                    JOptionPane.showMessageDialog(this, "Tidak dapat membaca file gambar yang dipilih.", "Error Memuat Gambar", JOptionPane.ERROR_MESSAGE);
                    resetImageSelection();
                }
            } catch (IOException ex) {
                System.err.println("Error reading selected image file: " + ex.getMessage());
                JOptionPane.showMessageDialog(this, "Terjadi kesalahan saat membaca file gambar: " + ex.getMessage(), "Error Gambar", JOptionPane.ERROR_MESSAGE);
                resetImageSelection();
            }
        } else {
            resetImageSelection();
        }
    }

    private void resetImageSelection() {
        selectedReviewImageData = null;
        uploadedImagePreview.setText("Tidak ada gambar dipilih");
        uploadedImagePreview.setIcon(null);
    }

    private void submitReview() {
        if (currentUser == null) {
            JOptionPane.showMessageDialog(this, "Anda harus login untuk memberikan review.",
                                        "Login Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (selectedRating == 0) {
            JOptionPane.showMessageDialog(this, "Silakan berikan rating terlebih dahulu.",
                                        "Rating Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String reviewText = reviewTextArea.getText().trim();
        if (reviewText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Silakan tulis review Anda.",
                                        "Review Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // Panggil metode addProductReview dengan byte array gambar
            ProductRepository.addProductReview(currentUser.getId(), productId, selectedRating, reviewText, selectedReviewImageData);
            JOptionPane.showMessageDialog(this, "Review berhasil dikirim!",
                                        "Success", JOptionPane.INFORMATION_MESSAGE);

            // Reset form setelah berhasil
            selectedRating = 0;
            reviewTextArea.setText("");
            resetImageSelection(); // Reset gambar
            updateStarDisplay();
            updateRatingLabel();

            if (viewController != null) {
                viewController.showOrdersView(); // Kembali ke riwayat pesanan setelah submit
            }

        } catch (SQLException e) {
            System.err.println("Error saving review: " + e.getMessage());
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Gagal menyimpan review: " + e.getMessage(), "Error Database", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String formatCurrency(double amount) {
        return String.format("Rp %,.0f", amount);
    }

    private static class JTextArea extends javax.swing.JTextArea {
        private String placeholder;

        public JTextArea(int rows, int columns) {
            super(rows, columns);
        }

        public void setPlaceholder(String placeholder) {
            this.placeholder = placeholder;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (placeholder != null && getText().isEmpty() && !hasFocus()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(TEXT_GRAY);
                g2.setFont(getFont().deriveFont(Font.ITALIC));
                g2.drawString(placeholder, getInsets().left, g2.getFontMetrics().getAscent() + getInsets().top);
                g2.dispose();
            }
        }
    }
}