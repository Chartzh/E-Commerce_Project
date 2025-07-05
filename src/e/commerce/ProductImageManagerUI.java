package e.commerce;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import java.io.ByteArrayInputStream; // PASTIKAN IMPORT INI ADA
import javax.imageio.ImageIO; // PASTIKAN IMPORT INI ADA
import java.util.Comparator;

public class ProductImageManagerUI extends JDialog {
    private int productId;
    private JPanel imagePanelContainer;
    private JScrollPane scrollPane;
    private SellerDashboardUI parentDashboard;

    private static class LoadedImageData {
        public int imageId;
        public byte[] data;
        public String extension;
        public boolean isMain;
        public int orderIndex;

        public LoadedImageData(int imageId, byte[] data, String extension, boolean isMain, int orderIndex) {
            this.imageId = imageId;
            this.data = data;
            this.extension = extension;
            this.isMain = isMain;
            this.orderIndex = orderIndex;
        }

        public Image getImage() {
            if (data == null) return null;
            try (ByteArrayInputStream bis = new ByteArrayInputStream(data)) {
                // Baris ini yang Anda laporkan error
                return ImageIO.read(bis); 
            } catch (IOException e) {
                System.err.println("Error loading image from bytes: " + e.getMessage());
                return null;
            }
        }
    }

    private List<LoadedImageData> currentImages;

    public ProductImageManagerUI(SellerDashboardUI parent, int productId) {
        super(parent, "Manage Product Images for Product ID: " + productId, true);
        this.parentDashboard = parent;
        this.productId = productId;
        this.currentImages = new ArrayList<>();

        setSize(700, 500);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());
        
        createUI();
        loadImagesFromDatabase();
    }

    private void createUI() {
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.setBorder(new EmptyBorder(10, 10, 5, 10));
        headerPanel.setBackground(Color.WHITE);
        JLabel titleLabel = new JLabel("Images for Product ID: " + productId);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        headerPanel.add(titleLabel);
        add(headerPanel, BorderLayout.NORTH);

        imagePanelContainer = new JPanel();
        imagePanelContainer.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));
        imagePanelContainer.setBackground(new Color(240, 240, 240));
        imagePanelContainer.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        scrollPane = new JScrollPane(imagePanelContainer);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        add(scrollPane, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        controlPanel.setBorder(new EmptyBorder(5, 10, 10, 10));
        controlPanel.setBackground(Color.WHITE);

        JButton btnAddImage = new JButton("Add New Image");
        btnAddImage.setBackground(new Color(40, 167, 69));
        btnAddImage.setForeground(Color.WHITE);
        btnAddImage.setFocusPainted(false);
        btnAddImage.addActionListener(e -> addNewImage());

        JButton btnDone = new JButton("Done");
        btnDone.setBackground(new Color(0, 123, 255));
        btnDone.setForeground(Color.WHITE);
        btnDone.setFocusPainted(false);
        btnDone.addActionListener(e -> dispose());

        controlPanel.add(btnAddImage);
        controlPanel.add(btnDone);
        add(controlPanel, BorderLayout.SOUTH);
    }

    private void loadImagesFromDatabase() {
        currentImages.clear();
        imagePanelContainer.removeAll();
        
        try {
            List<Object[]> rawImageDataFromDB = ProductRepository.getAllProductImagesDetails(productId);

            if (rawImageDataFromDB != null) {
                for (Object[] row : rawImageDataFromDB) {
                    currentImages.add(new LoadedImageData(
                        (int) row[0],
                        (byte[]) row[1],
                        (String) row[2],
                        (boolean) row[3],
                        (int) row[4]
                    ));
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading product images: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
        
        refreshImageDisplay();
    }

    private void refreshImageDisplay() {
        imagePanelContainer.removeAll();
        
        if (currentImages.isEmpty()) {
            JLabel noImagesLabel = new JLabel("No images available. Click 'Add New Image' to upload.");
            noImagesLabel.setForeground(Color.GRAY);
            imagePanelContainer.add(noImagesLabel);
        } else {
            currentImages.sort(Comparator.comparingInt(img -> img.orderIndex));

            for (LoadedImageData imageData : currentImages) {
                imagePanelContainer.add(createImageThumbnailPanel(imageData));
            }
        }
        imagePanelContainer.revalidate();
        imagePanelContainer.repaint();
        parentDashboard.loadProductsForSupervisor();
    }

    private JPanel createImageThumbnailPanel(LoadedImageData imageData) {
        JPanel thumbnailPanel = new JPanel(new BorderLayout());
        thumbnailPanel.setPreferredSize(new Dimension(120, 150));
        thumbnailPanel.setBorder(BorderFactory.createLineBorder(imageData.isMain ? Color.BLUE : Color.LIGHT_GRAY, imageData.isMain ? 2 : 1));
        thumbnailPanel.setBackground(Color.WHITE);

        Image originalImage = imageData.getImage();
        if (originalImage != null) {
            ImageIcon icon = new ImageIcon(originalImage.getScaledInstance(100, 100, Image.SCALE_SMOOTH));
            JLabel lblImage = new JLabel(icon);
            lblImage.setHorizontalAlignment(SwingConstants.CENTER);
            thumbnailPanel.add(lblImage, BorderLayout.CENTER);
        } else {
            JLabel lblPlaceholder = new JLabel("Error");
            lblPlaceholder.setHorizontalAlignment(SwingConstants.CENTER);
            thumbnailPanel.add(lblPlaceholder, BorderLayout.CENTER);
        }

        JPanel controlButtonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 2));
        controlButtonsPanel.setBackground(Color.WHITE);

        JButton btnDelete = new JButton("X");
        btnDelete.setMargin(new Insets(1, 4, 1, 4));
        btnDelete.setBackground(Color.RED);
        btnDelete.setForeground(Color.WHITE);
        btnDelete.setFocusPainted(false);
        btnDelete.setFont(new Font("Arial", Font.BOLD, 10));
        btnDelete.addActionListener(e -> deleteImage(imageData.imageId));

        JButton btnSetMain = new JButton("Main");
        btnSetMain.setMargin(new Insets(1, 4, 1, 4));
        btnSetMain.setBackground(imageData.isMain ? Color.GRAY : Color.BLUE);
        btnSetMain.setForeground(Color.WHITE);
        btnSetMain.setFocusPainted(false);
        btnSetMain.setFont(new Font("Arial", Font.PLAIN, 10));
        btnSetMain.setEnabled(!imageData.isMain);
        btnSetMain.addActionListener(e -> setMainImage(imageData.imageId));
        
        controlButtonsPanel.add(btnSetMain);
        controlButtonsPanel.add(btnDelete);
        thumbnailPanel.add(controlButtonsPanel, BorderLayout.SOUTH);

        return thumbnailPanel;
    }

    private void addNewImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Image Files", "jpg", "jpeg", "png", "gif"));
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try (FileInputStream fis = new FileInputStream(selectedFile)) {
                byte[] imageData = new byte[(int) selectedFile.length()];
                fis.read(imageData);
                String fileName = selectedFile.getName();
                String extension = "";
                int i = fileName.lastIndexOf('.');
                if (i > 0) {
                    extension = fileName.substring(i + 1);
                }

                int newOrderIndex = 1;
                if (!currentImages.isEmpty()) {
                    int maxOrderIndex = currentImages.stream().mapToInt(img -> img.orderIndex).max().orElse(0);
                    newOrderIndex = maxOrderIndex + 1;
                }

                boolean isMain = currentImages.isEmpty(); 

                int newImageId = ProductRepository.saveProductImage(productId, imageData, extension, isMain, newOrderIndex);
                if (newImageId != -1) {
                    JOptionPane.showMessageDialog(this, "Image added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    loadImagesFromDatabase(); 
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to save image to database.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (IOException | SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error adding image: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    private void deleteImage(int imageId) {
        int confirmation = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this image?", "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirmation == JOptionPane.YES_OPTION) {
            try {
                if (currentImages.size() == 1) {
                    if (currentImages.get(0).imageId == imageId) {
                         JOptionPane.showMessageDialog(this, "Produk harus memiliki setidaknya satu gambar. Tidak dapat menghapus gambar terakhir.", "Validasi", JOptionPane.WARNING_MESSAGE);
                         return;
                    }
                }
                
                ProductRepository.deleteProductImageById(imageId);
                JOptionPane.showMessageDialog(this, "Image deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadImagesFromDatabase();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error deleting image: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }

    private void setMainImage(int imageId) {
        try {
            ProductRepository.setMainProductImage(productId, imageId);
            JOptionPane.showMessageDialog(this, "Main image set successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            loadImagesFromDatabase();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error setting main image: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }
}