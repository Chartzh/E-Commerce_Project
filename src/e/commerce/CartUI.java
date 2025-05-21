package e.commerce;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.CompoundBorder;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class CartUI extends JPanel {
    private JPanel cartItemsContainer;
    private JLabel totalPriceLabel;
    private JLabel selectedItemsLabel;
    private JLabel totalSavingsLabel;
    private double totalPrice = 0;
    private double totalSavings = 0;
    private List<CartItem> cartItems;
    private JPanel contentPanel;
    private int selectedItemCount = 0;
    private JButton checkoutButton;
    
    // Constants for column widths
    private static final double COL_CHECKBOX_WIDTH = 0.05;
    private static final double COL_PRODUCT_WIDTH = 0.40;
    private static final double COL_PRICE_WIDTH = 0.20;
    private static final double COL_QUANTITY_WIDTH = 0.15;
    //private static final double COL_SUBTOTAL_WIDTH = 0.15;
    private static final double COL_ACTION_WIDTH = 0.05;

    // Item class to represent products in cart
    static class CartItem {
        private int id;
        private String name;
        private double price;
        private double originalPrice;
        private int quantity;
        private String imageColor;
        private String imagePath;  // Path to local image file
        private boolean isSelected;

        public CartItem(int id, String name, double price, double originalPrice, int quantity, String imageColor) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.originalPrice = originalPrice;
            this.quantity = quantity;
            this.imageColor = imageColor;
            this.imagePath = null;
            this.isSelected = true; // Default selected
        }
        
        public CartItem(int id, String name, double price, double originalPrice, int quantity, String imageColor, String imagePath) {
            this(id, name, price, originalPrice, quantity, imageColor);
            this.imagePath = imagePath;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public double getPrice() {
            return price;
        }
        
        public double getOriginalPrice() {
            return originalPrice;
        }

        public int getQuantity() {
            return quantity;
        }
        
        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }
        
        public String getImageColor() {
            return imageColor;
        }
        
        public String getImagePath() {
            return imagePath;
        }
        
        public boolean hasImage() {
            return imagePath != null && !imagePath.isEmpty();
        }
        
        public double getTotal() {
            return price * quantity;
        }
        
        public double getSavings() {
            return (originalPrice - price) * quantity;
        }
        
        public boolean isSelected() {
            return isSelected;
        }
        
        public void setSelected(boolean selected) {
            this.isSelected = selected;
        }
    }

    public CartUI() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        
        // Initialize with dummy data
        initDummyData();
        
        // Create UI components
        createComponents();
    }
    
    private void initDummyData() {
        cartItems = new ArrayList<>();
        // Add some dummy products with different colors and discounted prices
        cartItems.add(new CartItem(1, "Wireless Headphone ", 249000, 299000, 1, "#FFB6C1", "src/Resources/Images/wireless_headphone.jpg"));
        cartItems.add(new CartItem(2, "Bluetooth Speaker    ", 189000, 219000, 2, "#ADD8E6", "src/Resources/Images/bluetooth_speaker.jpg"));
        cartItems.add(new CartItem(3, "Gaming Mouse           ", 329000, 399000, 1, "#90EE90", "src/Resources/Images/gaming_mouse.jpg"));
        cartItems.add(new CartItem(4, "Mechanical Keyboard", 450000, 499000, 1, "#FFFACD", "src/Resources/Images/mechanical_keyboard.jpg"));
    }
    
    private void createComponents() {
        // Create summary panel first so totalPriceLabel is initialized
        JPanel summaryPanel = createSummaryPanel();
        
        // Create header panel
        JPanel headerPanel = createHeaderPanel();
        
        // Create content panel with cart items
        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(new EmptyBorder(0, 20, 20, 20));
        
        // Create cart items container
        createCartItemsContainer();
        
        // Add scroll pane
        JScrollPane scrollPane = new JScrollPane(cartItemsContainer);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(Color.WHITE);
        contentPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Add components to main panel
        add(headerPanel, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);
        add(summaryPanel, BorderLayout.SOUTH);
        
        // Initial calculation
        updateTotals();
    }
    
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(new EmptyBorder(15, 20, 15, 20));
        
        JLabel titleLabel = new JLabel("Shopping Cart");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.BLACK);
        
        JButton continueShoppingBtn = new JButton("Continue Shopping");
        continueShoppingBtn.setBackground(Color.WHITE);
        continueShoppingBtn.setForeground(new Color(255, 69, 0));
        continueShoppingBtn.setBorderPainted(false);
        continueShoppingBtn.setFocusPainted(false);
        continueShoppingBtn.setFont(new Font("Arial", Font.BOLD, 14));
        continueShoppingBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        continueShoppingBtn.addActionListener(e -> {
            // Switch back to dashboard
            Container parent = getParent();
            if (parent != null && parent.getLayout() instanceof CardLayout) {
                CardLayout layout = (CardLayout) parent.getLayout();
                layout.show(parent, "Dashboard");
            }
        });
        
        // Panel for header information
        JPanel headerInfoPanel = new JPanel(new BorderLayout());
        headerInfoPanel.setBackground(Color.WHITE);
        
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(continueShoppingBtn, BorderLayout.EAST);
        
        // Column headers
        JPanel columnHeadersPanel = new JPanel(new GridLayout(1, 1));
        columnHeadersPanel.setBackground(new Color(249, 249, 249));
        columnHeadersPanel.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(230, 230, 230)),
                new EmptyBorder(12, 20, 12, 20)
        ));
        
        // Create a panel with GridBagLayout for better control
        JPanel headerLabelsPanel = new JPanel(new GridBagLayout());
        headerLabelsPanel.setBackground(new Color(249, 249, 249));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        
        // Select all checkbox
        gbc.weightx = COL_CHECKBOX_WIDTH;
        JCheckBox selectAllBox = new JCheckBox("Select All");
        selectAllBox.setBackground(new Color(249, 249, 249));
        selectAllBox.setFont(new Font("Arial", Font.BOLD, 12));
        selectAllBox.setSelected(true);
        selectAllBox.addActionListener(e -> {
            boolean selected = selectAllBox.isSelected();
            for (CartItem item : cartItems) {
                item.setSelected(selected);
            }
            refreshCartItems();
            updateTotals();
        });
        headerLabelsPanel.add(selectAllBox, gbc);
        
        // Product label
        gbc.gridx = 1;
        gbc.weightx = COL_PRODUCT_WIDTH;
        JLabel productLabel = new JLabel("Product");
        productLabel.setFont(new Font("Arial", Font.BOLD, 12));
        headerLabelsPanel.add(productLabel, gbc);
        
        // Price label
        gbc.gridx = 2;
        gbc.weightx = COL_PRICE_WIDTH;
        JLabel priceLabel = new JLabel("Price");
        priceLabel.setFont(new Font("Arial", Font.BOLD, 12));
        headerLabelsPanel.add(priceLabel, gbc);
        
        // Quantity label
        gbc.gridx = 3;
        gbc.weightx = COL_QUANTITY_WIDTH;
        JLabel quantityLabel = new JLabel("Quantity");
        quantityLabel.setFont(new Font("Arial", Font.BOLD, 12));
        headerLabelsPanel.add(quantityLabel, gbc);
        
        // Subtotal label
        /*gbc.gridx = 4;
        gbc.weightx = COL_SUBTOTAL_WIDTH;
        JLabel subtotalLabel = new JLabel("Subtotal");
        subtotalLabel.setFont(new Font("Arial", Font.BOLD, 12));
        headerLabelsPanel.add(subtotalLabel, gbc);*/
        
        // Action label
        gbc.gridx = 5;
        gbc.weightx = COL_ACTION_WIDTH;
        gbc.anchor = GridBagConstraints.CENTER;
        JLabel actionLabel = new JLabel("Action");
        actionLabel.setFont(new Font("Arial", Font.BOLD, 12));
        headerLabelsPanel.add(actionLabel, gbc);
        
        columnHeadersPanel.add(headerLabelsPanel);
        
        // Add column headers below the main header with some space
        JPanel completeHeaderPanel = new JPanel(new BorderLayout());
        completeHeaderPanel.setBackground(Color.WHITE);
        completeHeaderPanel.add(headerPanel, BorderLayout.NORTH);
        completeHeaderPanel.add(Box.createVerticalStrut(10), BorderLayout.CENTER);
        completeHeaderPanel.add(columnHeadersPanel, BorderLayout.SOUTH);
        
        return completeHeaderPanel;
    }
    
    private void createCartItemsContainer() {
        cartItemsContainer = new JPanel();
        cartItemsContainer.setLayout(new BoxLayout(cartItemsContainer, BoxLayout.Y_AXIS));
        cartItemsContainer.setBackground(Color.WHITE);
        
        refreshCartItems();
    }
    
    private JPanel createCartItemPanel(CartItem item) {
        JPanel itemPanel = new JPanel(new GridBagLayout());
        itemPanel.setBackground(Color.WHITE);
        itemPanel.setBorder(new EmptyBorder(15, 20, 15, 20));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 5, 0, 5);
        gbc.gridy = 0;
        
        // Checkbox for selection
        gbc.gridx = 0;
        gbc.weightx = COL_CHECKBOX_WIDTH;
        JCheckBox selectBox = new JCheckBox();
        selectBox.setBackground(Color.WHITE);
        selectBox.setSelected(item.isSelected());
        selectBox.addActionListener(e -> {
            item.setSelected(selectBox.isSelected());
            updateTotals();
        });
        itemPanel.add(selectBox, gbc);
        
        // Product image and name
        gbc.gridx = 1;
        gbc.weightx = COL_PRODUCT_WIDTH;
        JPanel productInfoPanel = new JPanel(new BorderLayout(15, 0));
        productInfoPanel.setBackground(Color.WHITE);
        
        // Product image - now supports loading from file
        JPanel imagePanel;
        if (item.hasImage()) {
            imagePanel = new ImagePanel(item.getImagePath(), item.getImageColor());
        } else {
            imagePanel = new ImagePanel(item.getImageColor());
        }
        imagePanel.setPreferredSize(new Dimension(80, 80));
        productInfoPanel.add(imagePanel, BorderLayout.WEST);
        
        // Product name and details
        JPanel namePanel = new JPanel();
        namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.Y_AXIS));
        namePanel.setBackground(Color.WHITE);
        
        JLabel nameLabel = new JLabel(item.getName());
        nameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel skuLabel = new JLabel("SKU: QT" + item.getId() + String.format("%04d", item.getId() * 1000));
        skuLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        skuLabel.setForeground(Color.GRAY);
        skuLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        namePanel.add(nameLabel);
        namePanel.add(Box.createVerticalStrut(5));
        namePanel.add(skuLabel);
        
        productInfoPanel.add(namePanel, BorderLayout.CENTER);
        itemPanel.add(productInfoPanel, gbc);
        
        // Price
        gbc.gridx = 2;
        gbc.weightx = COL_PRICE_WIDTH;
        JPanel pricePanel = new JPanel();
        pricePanel.setLayout(new BoxLayout(pricePanel, BoxLayout.Y_AXIS));
        pricePanel.setBackground(Color.WHITE);
        
        JLabel currentPriceLabel = new JLabel(formatCurrency(item.getPrice()));
        currentPriceLabel.setFont(new Font("Arial", Font.BOLD, 14));
        currentPriceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Only show original price if it's different
        if (item.getPrice() < item.getOriginalPrice()) {
            JLabel originalPriceLabel = new JLabel(formatCurrency(item.getOriginalPrice()));
            originalPriceLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            originalPriceLabel.setForeground(Color.GRAY);
            originalPriceLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            
            // Strikethrough the original price
            originalPriceLabel.setText("<html><strike>" + formatCurrency(item.getOriginalPrice()) + "</strike></html>");
            
            pricePanel.add(currentPriceLabel);
            pricePanel.add(Box.createVerticalStrut(3));
            pricePanel.add(originalPriceLabel);
        } else {
            pricePanel.add(currentPriceLabel);
        }
        
        itemPanel.add(pricePanel, gbc);
        
        // Quantity
        gbc.gridx = 3;
        gbc.weightx = COL_QUANTITY_WIDTH;
        JPanel quantityPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        quantityPanel.setBackground(Color.WHITE);
        
        JButton minusButton = new JButton("-");
        minusButton.setFont(new Font("Arial", Font.BOLD, 14)); // lebih kecil
        minusButton.setPreferredSize(new Dimension(60, 45));   // lebih lebar
        minusButton.setFocusPainted(false);
        minusButton.setBackground(Color.WHITE);
        minusButton.setBorderPainted(false);
        
        JTextField quantityField = new JTextField(String.valueOf(item.getQuantity()), 2);
        quantityField.setHorizontalAlignment(JTextField.CENTER);
        quantityField.setFont(new Font("Arial", Font.PLAIN, 14));
        quantityField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        
        JButton plusButton = new JButton("+");
        plusButton.setFont(new Font("Arial", Font.BOLD, 14));
        plusButton.setPreferredSize(new Dimension(60, 45));
        plusButton.setFocusPainted(false);
        plusButton.setBackground(Color.WHITE);
        plusButton.setBorderPainted(false);
        
        // Add quantity change listeners
        minusButton.addActionListener(e -> {
            if (item.getQuantity() > 1) {
                item.setQuantity(item.getQuantity() - 1);
                quantityField.setText(String.valueOf(item.getQuantity()));
                refreshCartItems();
                updateTotals();
            }
        });
        
        plusButton.addActionListener(e -> {
            item.setQuantity(item.getQuantity() + 1);
            quantityField.setText(String.valueOf(item.getQuantity()));
            refreshCartItems();
            updateTotals();
        });
        
        quantityField.addActionListener(e -> {
            try {
                int newQuantity = Integer.parseInt(quantityField.getText().trim());
                if (newQuantity > 0) {
                    item.setQuantity(newQuantity);
                } else {
                    item.setQuantity(1);
                    quantityField.setText("1");
                }
                refreshCartItems();
                updateTotals();
            } catch (NumberFormatException ex) {
                quantityField.setText(String.valueOf(item.getQuantity()));
            }
        });
        
        quantityPanel.add(minusButton);
        quantityPanel.add(quantityField);
        quantityPanel.add(plusButton);
        
        itemPanel.add(quantityPanel, gbc);
        
        // Remove button
        gbc.gridx = 5;
        gbc.weightx = COL_ACTION_WIDTH;
        gbc.anchor = GridBagConstraints.CENTER;
        JButton removeButton = new JButton("×");
        removeButton.setFont(new Font("Arial", Font.BOLD, 18));
        removeButton.setForeground(new Color(150, 150, 150));
        removeButton.setBorderPainted(false);
        removeButton.setContentAreaFilled(false);
        removeButton.setFocusPainted(false);
        removeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        removeButton.addActionListener(e -> {
            cartItems.remove(item);
            refreshCartItems();
            updateTotals();
        });
        itemPanel.add(removeButton, gbc);
        
        return itemPanel;
    }
    
    private JPanel createSummaryPanel() {
        JPanel summaryPanel = new JPanel(new BorderLayout(30, 0));
        summaryPanel.setBackground(Color.WHITE);
        summaryPanel.setBorder(new CompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(230, 230, 230)),
            new EmptyBorder(20, 20, 20, 20)
        ));
        
        // Cart summary on left side
        JPanel cartSummaryPanel = new JPanel();
        cartSummaryPanel.setLayout(new BoxLayout(cartSummaryPanel, BoxLayout.Y_AXIS));
        cartSummaryPanel.setBackground(Color.WHITE);
        
        JLabel summaryTitle = new JLabel("Cart Summary");
        summaryTitle.setFont(new Font("Arial", Font.BOLD, 16));
        summaryTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel detailsPanel = new JPanel(new GridLayout(3, 1, 5, 10));
        detailsPanel.setBackground(Color.WHITE);
        detailsPanel.setBorder(new EmptyBorder(15, 0, 0, 0));
        detailsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Selected items count - Using fixed width labels for alignment
        JPanel selectedItemsPanel = new JPanel(new BorderLayout());
        selectedItemsPanel.setBackground(Color.WHITE);
        JLabel selectedItemsTextLabel = new JLabel("Selected Items:");
        selectedItemsTextLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        selectedItemsTextLabel.setPreferredSize(new Dimension(150, 20));
        selectedItemsLabel = new JLabel("0 items");
        selectedItemsLabel.setFont(new Font("Arial", Font.BOLD, 14));
        selectedItemsLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        selectedItemsPanel.add(selectedItemsTextLabel, BorderLayout.WEST);
        selectedItemsPanel.add(selectedItemsLabel, BorderLayout.EAST);
        
        // Total savings - Using fixed width labels for alignment
        JPanel savingsPanel = new JPanel(new BorderLayout());
        savingsPanel.setBackground(Color.WHITE);
        JLabel savingsTextLabel = new JLabel("Total Savings:");
        savingsTextLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        savingsTextLabel.setPreferredSize(new Dimension(150, 20));
        totalSavingsLabel = new JLabel("Rp 0");
        totalSavingsLabel.setFont(new Font("Arial", Font.BOLD, 14));
        totalSavingsLabel.setForeground(new Color(76, 175, 80)); // Green for savings
        totalSavingsLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        savingsPanel.add(savingsTextLabel, BorderLayout.WEST);
        savingsPanel.add(totalSavingsLabel, BorderLayout.EAST);
        
        // Total price - Using fixed width labels for alignment
        JPanel totalPanel = new JPanel(new BorderLayout());
        totalPanel.setBackground(Color.WHITE);
        JLabel totalTextLabel = new JLabel("Total Price:");
        totalTextLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        totalTextLabel.setPreferredSize(new Dimension(150, 20));
        totalPriceLabel = new JLabel("Rp 0");
        totalPriceLabel.setFont(new Font("Arial", Font.BOLD, 16));
        totalPriceLabel.setForeground(new Color(255, 69, 0));
        totalPriceLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        totalPanel.add(totalTextLabel, BorderLayout.WEST);
        totalPanel.add(totalPriceLabel, BorderLayout.EAST);
        
        detailsPanel.add(selectedItemsPanel);
        detailsPanel.add(savingsPanel);
        detailsPanel.add(totalPanel);
        
        cartSummaryPanel.add(summaryTitle);
        cartSummaryPanel.add(detailsPanel);
        
        // Notes or additional information
        JPanel notesPanel = new JPanel();
        notesPanel.setLayout(new BoxLayout(notesPanel, BoxLayout.Y_AXIS));
        notesPanel.setBackground(Color.WHITE);
        notesPanel.setBorder(new EmptyBorder(15, 0, 0, 0));
        notesPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel taxLabel = new JLabel("* Prices include taxes");
        taxLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        taxLabel.setForeground(Color.GRAY);
        taxLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JLabel shippingLabel = new JLabel("* Shipping calculated at checkout");
        shippingLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        shippingLabel.setForeground(Color.GRAY);
        shippingLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        notesPanel.add(taxLabel);
        notesPanel.add(Box.createVerticalStrut(5));
        notesPanel.add(shippingLabel);
        
        cartSummaryPanel.add(notesPanel);
        
        // Checkout button on right side
        JPanel checkoutPanel = new JPanel(new BorderLayout());
        checkoutPanel.setBackground(Color.WHITE);
        
        checkoutButton = new JButton("Proceed to Checkout");
        checkoutButton.setBackground(new Color(255, 69, 0));
        checkoutButton.setForeground(Color.WHITE);
        checkoutButton.setBorderPainted(false);
        checkoutButton.setFocusPainted(false);
        checkoutButton.setFont(new Font("Arial", Font.BOLD, 14));
        checkoutButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        checkoutButton.setPreferredSize(new Dimension(190, 40)); // Reduced size
        checkoutButton.addActionListener(e -> {
            int selectedCount = getSelectedItemCount();
            if (selectedCount == 0) {
                JOptionPane.showMessageDialog(this, 
                    "Please select at least one item to checkout", 
                    "Checkout", 
                    JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Checkout process initiated\nTotal: " + totalPriceLabel.getText() + 
                    "\nItems: " + selectedCount, 
                    "Checkout", 
                    JOptionPane.INFORMATION_MESSAGE);
            }
        });
        
        checkoutPanel.add(checkoutButton, BorderLayout.EAST);
        
        summaryPanel.add(cartSummaryPanel, BorderLayout.WEST);
        summaryPanel.add(checkoutPanel, BorderLayout.EAST);
        
        return summaryPanel;
    }
    
    private void updateTotals() {
        totalPrice = 0;
        totalSavings = 0;
        selectedItemCount = 0;
        
        for (CartItem item : cartItems) {
            if (item.isSelected()) {
                totalPrice += item.getTotal();
                totalSavings += item.getSavings();
                selectedItemCount += item.getQuantity();
            }
        }
        
        // Update labels
        totalPriceLabel.setText(formatCurrency(totalPrice));
        totalSavingsLabel.setText(formatCurrency(totalSavings));
        selectedItemsLabel.setText(selectedItemCount + " items");
        
        // Update checkout button state
        updateCheckoutButtonState();
    }
    
    private void updateCheckoutButtonState() {
        if (selectedItemCount == 0) {
            checkoutButton.setBackground(new Color(200, 200, 200)); // Gray
            checkoutButton.setEnabled(false);
        } else {
            checkoutButton.setBackground(new Color(255, 69, 0)); // Orange
            checkoutButton.setEnabled(true);
        }
    }
    
    private int getSelectedItemCount() {
        int count = 0;
        for (CartItem item : cartItems) {
            if (item.isSelected()) {
                count += item.getQuantity();
            }
        }
        return count;
    }
    
    private String formatCurrency(double amount) {
        return String.format("Rp %.0f", amount);
    }
    
    // Custom panel for product image that can load from file or use color
    static class ImagePanel extends JPanel {
        private String colorHex;
        private BufferedImage image;
        private boolean hasImage;
        
        // Constructor for color-only display
        public ImagePanel(String colorHex) {
            this.colorHex = colorHex;
            this.hasImage = false;
            setPreferredSize(new Dimension(80, 80));
        }
        
        // Constructor for image from file
        public ImagePanel(String imagePath, String fallbackColor) {
            this.colorHex = fallbackColor;
            this.hasImage = false;
            setPreferredSize(new Dimension(80, 80));
            
            // Try to load the image
            try {
                File imageFile = new File(imagePath);
                if (imageFile.exists()) {
                    image = ImageIO.read(imageFile);
                    hasImage = true;
                }
            } catch (IOException e) {
                System.err.println("Could not load image: " + imagePath);
                e.printStackTrace();
                // Will fall back to color display
            }
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            if (hasImage && image != null) {
                // Draw image with rounded corners
                int width = getWidth() - 10;
                int height = getHeight() - 10;
                
                // Create a rounded clipping shape
                Shape roundedRect = new java.awt.geom.RoundRectangle2D.Float(5, 5, width, height, 10, 10);
                g2d.setClip(roundedRect);
                
                // Draw the image scaled to fit
                g2d.drawImage(image, 5, 5, width, height, null);
                
                // Reset clip (no border drawn)
                g2d.setClip(null);
            } else {
                // Draw colored rectangle with rounded corners
                int width = getWidth() - 10;
                int height = getHeight() - 10;
                
                // Parse color from hex
                Color color;
                try {
                    color = Color.decode(colorHex);
                } catch (NumberFormatException e) {
                    color = new Color(200, 200, 200); // Default gray if invalid color
                }
                
                // Create a rounded rectangle
                Shape roundedRect = new java.awt.geom.RoundRectangle2D.Float(5, 5, width, height, 10, 10);
                
                // Fill with color
                g2d.setColor(color);
                g2d.fill(roundedRect);
            }
        }
    }
    
    // Method to ensure all product images are aligned properly
    private void alignProductImages() {
        for (Component component : cartItemsContainer.getComponents()) {
            if (component instanceof JPanel) {
                JPanel itemPanel = (JPanel) component;
                
                // Find the product info panel in the item panel
                for (Component itemComponent : itemPanel.getComponents()) {
                    if (itemComponent instanceof JPanel && 
                        ((JPanel)itemComponent).getLayout() instanceof BorderLayout) {
                        JPanel productInfoPanel = (JPanel) itemComponent;
                        
                        // Ensure image panel has consistent size
                        Component[] components = productInfoPanel.getComponents();
                        for (Component productComponent : components) {
                            if (productComponent instanceof ImagePanel) {
                                productComponent.setPreferredSize(new Dimension(80, 80));
                                productComponent.setMinimumSize(new Dimension(80, 80));
                                productComponent.setMaximumSize(new Dimension(80, 80));
                            }
                        }
                        
                        break;
                    }
                }
            }
        }
    }
    
    // Method to load image from file with proper error handling
    private BufferedImage loadImage(String path) {
        try {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                return ImageIO.read(file);
            } else {
                System.err.println("Image file does not exist: " + path);
                return null;
            }
        } catch (IOException e) {
            System.err.println("Error loading image: " + path);
            e.printStackTrace();
            return null;
        }
    }
    
    // Main method for testing
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Shopping Cart");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1000, 700);
            
            CartUI cartUI = new CartUI();
            frame.add(cartUI);
            
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
    
    // Method to ensure consistent column alignment
    private void ensureColumnAlignment() {
        Component[] components = cartItemsContainer.getComponents();
        for (Component comp : components) {
            if (comp instanceof JPanel) {
                JPanel panel = (JPanel) comp;
                if (panel.getLayout() instanceof GridBagLayout && panel.getComponentCount() >= 6) {
                    GridBagLayout layout = (GridBagLayout) panel.getLayout();
                    
                    // Make sure each column has the same width across all rows
                    GridBagConstraints gbc = new GridBagConstraints();
                    gbc.fill = GridBagConstraints.HORIZONTAL;
                    
                    // Checkbox column
                    gbc.gridx = 0;
                    gbc.weightx = COL_CHECKBOX_WIDTH;
                    layout.setConstraints(panel.getComponent(0), gbc);
                    
                    // Product column
                    gbc.gridx = 1;
                    gbc.weightx = COL_PRODUCT_WIDTH;
                    layout.setConstraints(panel.getComponent(1), gbc);
                    
                    // Price column
                    gbc.gridx = 2;
                    gbc.weightx = COL_PRICE_WIDTH;
                    layout.setConstraints(panel.getComponent(2), gbc);
                    
                    // Quantity column
                    gbc.gridx = 3;
                    gbc.weightx = COL_QUANTITY_WIDTH;
                    layout.setConstraints(panel.getComponent(3), gbc);
                    
                    // Subtotal column
                    /*gbc.gridx = 4;
                    gbc.weightx = COL_SUBTOTAL_WIDTH;
                    layout.setConstraints(panel.getComponent(4), gbc);*/
                    
                    // Action column
                    gbc.gridx = 5;
                    gbc.weightx = COL_ACTION_WIDTH;
                    layout.setConstraints(panel.getComponent(5), gbc);
                }
            }
        }
    }
    
    private void refreshCartItems() {
        cartItemsContainer.removeAll();
        
        for (CartItem item : cartItems) {
            JPanel itemPanel = createCartItemPanel(item);
            cartItemsContainer.add(itemPanel);
            // Add a separator
            JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
            separator.setForeground(new Color(230, 230, 230));
            cartItemsContainer.add(separator);
        }
        
        // Add empty panel to push items to the top
        JPanel fillerPanel = new JPanel();
        fillerPanel.setBackground(Color.WHITE);
        cartItemsContainer.add(fillerPanel);
        
        // Ensure proper alignment of columns
        ensureColumnAlignment();
        
        // Ensure consistent image sizes
        alignProductImages();
        
        cartItemsContainer.revalidate();
        cartItemsContainer.repaint();
    }
}