package e.commerce;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.io.*;
import java.sql.*;

public class ProfileUI extends JPanel {
    private CardLayout cardLayout;
    private JPanel contentPanel;
    private User currentUser;
    private JButton[] navButtons;
    private Color primaryColor = new Color(255, 102, 0); // Orange to match logo
    private Color backgroundColor = new Color(248, 249, 250); // Light background
    private Color lightGrayColor = new Color(233, 236, 239); // Light gray for panels
    private Color textColor = new Color(33, 37, 41); // Dark text color

    public ProfileUI() {
        currentUser = Authentication.getCurrentUser();
        if (currentUser == null) {
            JOptionPane.showMessageDialog(null, "No user logged in!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        setLayout(new BorderLayout());
        setBackground(backgroundColor);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Create modal-like container
        JPanel modalPanel = new JPanel(new BorderLayout());
        modalPanel.setBackground(Color.WHITE);
        modalPanel.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(222, 226, 230), 1, true),
            BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));

        // Header with "Account Settings" and close button
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(222, 226, 230)),
            BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));

        JLabel titleLabel = new JLabel("Account Settings");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setForeground(textColor);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JButton closeButton = new JButton("×");
        closeButton.setFont(new Font("Arial", Font.BOLD, 20));
        closeButton.setForeground(new Color(108, 117, 125));
        closeButton.setBackground(Color.WHITE);
        closeButton.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        closeButton.setFocusPainted(false);
        closeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeButton.addActionListener(e -> {
            CardLayout parentLayout = (CardLayout) ProfileUI.this.getParent().getLayout();
            parentLayout.show(ProfileUI.this.getParent(), "Dashboard");
        });
        headerPanel.add(closeButton, BorderLayout.EAST);
        modalPanel.add(headerPanel, BorderLayout.NORTH);

        // Main content with sidebar and content area
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBackground(Color.WHITE);
        
        // Navigation sidebar
        JPanel navPanel = createNavigationPanel();
        navPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(222, 226, 230)));
        mainContainer.add(navPanel, BorderLayout.WEST);

        // Content area with CardLayout
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(Color.WHITE);

        ProfilePanel profilePanel = new ProfilePanel(currentUser);
        contentPanel.add(profilePanel, "Profile");

        contentPanel.add(createPlaceholderPanel("Account & Payment"), "Account & Payment");
        contentPanel.add(createPlaceholderPanel("Security"), "Security");
        contentPanel.add(createPlaceholderPanel("Legal Agreements"), "Legal Agreements");
        contentPanel.add(createPlaceholderPanel("Contact Us"), "Contact Us");

        mainContainer.add(contentPanel, BorderLayout.CENTER);
        modalPanel.add(mainContainer, BorderLayout.CENTER);
        add(modalPanel, BorderLayout.CENTER);
    }

    private JPanel createNavigationPanel() {
        JPanel navPanel = new JPanel();
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        navPanel.setBackground(Color.WHITE);
        navPanel.setPreferredSize(new Dimension(200, 0));

        String[] navItems = {"Profile", "Account & Payment", "Security", "Legal Agreements", "Contact Us"};
        navButtons = new JButton[navItems.length];

        for (int i = 0; i < navItems.length; i++) {
            JButton button = new JButton(navItems[i]);
            navButtons[i] = button;
            button.setAlignmentX(Component.LEFT_ALIGNMENT);
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            button.setFont(new Font("Arial", Font.PLAIN, 14));
            button.setForeground(i == 0 ? primaryColor : textColor);
            button.setBackground(i == 0 ? new Color(255, 235, 220) : Color.WHITE); // Lighter orange for selected
            button.setBorder(BorderFactory.createCompoundBorder(
                i == 0 ? BorderFactory.createMatteBorder(0, 0, 0, 3, primaryColor) : null,
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
            ));
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.setFocusPainted(false);
            button.setCursor(new Cursor(Cursor.HAND_CURSOR));
            final String panelName = navItems[i];
            final int index = i;
            button.addActionListener(e -> {
                for (int j = 0; j < navButtons.length; j++) {
                    navButtons[j].setForeground(textColor);
                    navButtons[j].setBackground(Color.WHITE);
                    navButtons[j].setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
                }
                navButtons[index].setForeground(primaryColor);
                navButtons[index].setBackground(new Color(255, 235, 220));
                navButtons[index].setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 0, 3, primaryColor),
                    BorderFactory.createEmptyBorder(10, 15, 10, 15)
                ));
                cardLayout.show(contentPanel, panelName);
            });
            navPanel.add(button);
        }

        return navPanel;
    }

    private JPanel createPlaceholderPanel(String title) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JLabel label = new JLabel("Section: " + title);
        label.setFont(new Font("Arial", Font.PLAIN, 16));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    private class ProfilePanel extends JPanel {
        private JLabel profileImageLabel;
        private JTextField txtUsername, txtPhone;
        private JTextArea txtAddress;
        private JButton btnSave;
        private ImageIcon profileImage;
        private File selectedFile;
        private User user;
        private JPanel[] locationButtons = new JPanel[4];

        public ProfilePanel(User user) {
            this.user = user;
            setLayout(new BorderLayout());
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

            JPanel mainPanel = new JPanel();
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
            mainPanel.setBackground(Color.WHITE);

            // Profile Image Section
            JPanel imagePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            imagePanel.setBackground(Color.WHITE);
            
            JPanel profileImageContainer = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.setColor(getBackground());
                    g2d.fillOval(0, 0, getWidth(), getHeight());
                    g2d.dispose();
                }
            };
            profileImageContainer.setPreferredSize(new Dimension(80, 80));
            profileImageContainer.setLayout(new BorderLayout());
            profileImageContainer.setBackground(new Color(222, 226, 230));
            
            profileImageLabel = new JLabel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    Shape circle = new Ellipse2D.Double(0, 0, getWidth(), getHeight());
                    g2d.setClip(circle);
                    if (getIcon() != null) {
                        getIcon().paintIcon(this, g2d, 0, 0);
                    } else {
                        g2d.setColor(getBackground());
                        g2d.fillOval(0, 0, getWidth(), getHeight());
                    }
                    g2d.dispose();
                }
            };
            profileImageLabel.setHorizontalAlignment(JLabel.CENTER);
            profileImageLabel.setOpaque(false);
            loadProfilePicture();
            
            profileImageContainer.add(profileImageLabel, BorderLayout.CENTER);
            imagePanel.add(profileImageContainer);
            mainPanel.add(imagePanel);
            
            // Username field
            JPanel usernamePanel = new JPanel(new BorderLayout(0, 5));
            usernamePanel.setBackground(Color.WHITE);
            usernamePanel.setBorder(BorderFactory.createEmptyBorder(20, 0, 10, 0));
            
            JLabel lblUsername = new JLabel("Username");
            lblUsername.setFont(new Font("Arial", Font.PLAIN, 12));
            lblUsername.setForeground(new Color(108, 117, 125));
            
            txtUsername = new JTextField();
            txtUsername.setText(user.getUsername());
            txtUsername.setFont(new Font("Arial", Font.PLAIN, 14));
            txtUsername.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(206, 212, 218), 1, true),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
            ));
            
            usernamePanel.add(lblUsername, BorderLayout.NORTH);
            usernamePanel.add(txtUsername, BorderLayout.CENTER);
            mainPanel.add(usernamePanel);
            
            // Phone number
            JPanel phonePanel = new JPanel(new BorderLayout(0, 5));
            phonePanel.setBackground(Color.WHITE);
            phonePanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
            
            JLabel lblPhone = new JLabel("Phone Number");
            lblPhone.setFont(new Font("Arial", Font.PLAIN, 12));
            lblPhone.setForeground(new Color(108, 117, 125));
            
            txtPhone = new JTextField();
            // Remove any country code
            String phoneNumber = user.getPhone();
            if (phoneNumber != null && phoneNumber.contains("+")) {
                phoneNumber = phoneNumber.substring(phoneNumber.indexOf(" ") + 1);
            }
            txtPhone.setText(phoneNumber);
            txtPhone.setFont(new Font("Arial", Font.PLAIN, 14));
            txtPhone.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(206, 212, 218), 1, true),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
            ));
            
            phonePanel.add(lblPhone, BorderLayout.NORTH);
            phonePanel.add(txtPhone, BorderLayout.CENTER);
            mainPanel.add(phonePanel);
            
            // Address field
            JPanel addressPanel = new JPanel(new BorderLayout(0, 5));
            addressPanel.setBackground(Color.WHITE);
            addressPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
            
            JLabel lblAddress = new JLabel("Address");
            lblAddress.setFont(new Font("Arial", Font.PLAIN, 12));
            lblAddress.setForeground(new Color(108, 117, 125));
            
            txtAddress = new JTextArea();
            txtAddress.setText(user.getAddress() != null ? user.getAddress() : "2, Chief Ekenezu Street, Ubeji, Ayigu, Delta State");
            txtAddress.setFont(new Font("Arial", Font.PLAIN, 14));
            txtAddress.setLineWrap(true);
            txtAddress.setWrapStyleWord(true);
            txtAddress.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(206, 212, 218), 1, true),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
            ));
            
            addressPanel.add(lblAddress, BorderLayout.NORTH);
            addressPanel.add(txtAddress, BorderLayout.CENTER);
            mainPanel.add(addressPanel);
            
            // Default Delivery Location
            JPanel deliveryLocationPanel = new JPanel(new BorderLayout(0, 10));
            deliveryLocationPanel.setBackground(Color.WHITE);
            deliveryLocationPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 15, 0));
            
            JLabel lblDeliveryLocation = new JLabel("Default Delivery Location");
            lblDeliveryLocation.setFont(new Font("Arial", Font.PLAIN, 14));
            lblDeliveryLocation.setForeground(textColor);
            
            JPanel locationButtonsPanel = new JPanel(new GridLayout(1, 4, 10, 0));
            locationButtonsPanel.setBackground(Color.WHITE);
            locationButtonsPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
            
            for (int i = 0; i < 4; i++) {
                final int index = i;
                JPanel locationButton = new JPanel(new BorderLayout()) {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2d = (Graphics2D) g.create();
                        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2d.setColor(getBackground());
                        g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                        g2d.dispose();
                    }
                };
                locationButton.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                locationButton.setBackground(i == 0 ? primaryColor : new Color(248, 249, 250)); // First button is selected (orange)
                locationButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
                
                // Add icon image (placeholder)
                JPanel iconPanel = new JPanel() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        Graphics2D g2d = (Graphics2D) g.create();
                        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2d.setColor(index == 0 ? Color.WHITE : new Color(173, 181, 189));
                        
                        // Different icon for each location
                        if (index == 0) {
                            // House icon
                            int[] xPoints = {5, 12, 19};
                            int[] yPoints = {10, 3, 10};
                            g2d.fillPolygon(xPoints, yPoints, 3);
                            g2d.fillRect(7, 10, 10, 7);
                        } else if (index == 1) {
                            // Office icon
                            g2d.fillRect(5, 5, 14, 12);
                            g2d.setColor(index == 0 ? primaryColor : Color.WHITE);
                            g2d.fillRect(8, 8, 3, 3);
                            g2d.fillRect(13, 8, 3, 3);
                            g2d.fillRect(8, 13, 8, 2);
                        } else if (index == 2) {
                            // Shop icon
                            g2d.fillRect(3, 7, 18, 10);
                            g2d.fillRect(7, 3, 10, 4);
                        } else {
                            // Other icon
                            g2d.fillOval(5, 5, 14, 10);
                            g2d.fillRect(10, 15, 4, 5);
                        }
                        g2d.dispose();
                    }
                };
                iconPanel.setPreferredSize(new Dimension(24, 20));
                iconPanel.setOpaque(false);
                
                JLabel locationLabel = new JLabel("Location " + (i + 1));
                locationLabel.setFont(new Font("Arial", Font.BOLD, 11));
                locationLabel.setForeground(i == 0 ? Color.WHITE : new Color(108, 117, 125));
                locationLabel.setHorizontalAlignment(SwingConstants.CENTER);
                
                locationButton.add(iconPanel, BorderLayout.WEST);
                locationButton.add(locationLabel, BorderLayout.CENTER);
                
                locationButton.addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseClicked(java.awt.event.MouseEvent evt) {
                        for (int j = 0; j < locationButtons.length; j++) {
                            locationButtons[j].setBackground(j == index ? primaryColor : new Color(248, 249, 250));
                            ((JLabel)locationButtons[j].getComponent(1)).setForeground(j == index ? Color.WHITE : new Color(108, 117, 125));
                        }
                    }
                });
                
                locationButtons[i] = locationButton;
                locationButtonsPanel.add(locationButton);
            }
            
            deliveryLocationPanel.add(lblDeliveryLocation, BorderLayout.NORTH);
            deliveryLocationPanel.add(locationButtonsPanel, BorderLayout.CENTER);
            
            // Add panel with round corners and border
            JPanel locationContainerPanel = new JPanel(new BorderLayout());
            locationContainerPanel.setBackground(Color.WHITE);
            locationContainerPanel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(222, 226, 230), 1, true),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
            ));
            locationContainerPanel.add(deliveryLocationPanel);
            
            mainPanel.add(locationContainerPanel);
            mainPanel.add(Box.createVerticalStrut(15));
            
            // Save button
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.setBackground(Color.WHITE);
            
            btnSave = new JButton("Save");
            btnSave.setFont(new Font("Arial", Font.BOLD, 14));
            btnSave.setForeground(Color.WHITE);
            btnSave.setBackground(primaryColor);
            btnSave.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
            btnSave.setFocusPainted(false);
            btnSave.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnSave.addActionListener(e -> updateProfile());
            
            buttonPanel.add(btnSave);
            mainPanel.add(buttonPanel);
            
            JScrollPane scrollPane = new JScrollPane(mainPanel);
            scrollPane.setBorder(null);
            scrollPane.getVerticalScrollBar().setUnitIncrement(16);
            scrollPane.setViewportBorder(null);
            
            add(scrollPane, BorderLayout.CENTER);
            
            // Add change photo capability
            profileImageContainer.setCursor(new Cursor(Cursor.HAND_CURSOR));
            profileImageContainer.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent evt) {
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setDialogTitle("Select Profile Picture");
                    fileChooser.setFileFilter(new FileNameExtensionFilter("Image files", "jpg", "jpeg", "png", "gif"));
                    if (fileChooser.showOpenDialog(ProfilePanel.this) == JFileChooser.APPROVE_OPTION) {
                        selectedFile = fileChooser.getSelectedFile();
                        try {
                            ImageIcon originalIcon = new ImageIcon(selectedFile.getPath());
                            Image image = originalIcon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
                            profileImage = new ImageIcon(image);
                            profileImageLabel.setIcon(profileImage);
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(ProfilePanel.this, 
                                "Error loading image: " + ex.getMessage(), 
                                "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            });
        }

        private void loadProfilePicture() {
            Connection conn = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try {
                conn = DatabaseConnection.getConnection();
                String query = "SELECT profile_picture FROM users WHERE id = ?";
                stmt = conn.prepareStatement(query);
                stmt.setInt(1, user.getId());
                rs = stmt.executeQuery();
                if (rs.next() && rs.getBytes("profile_picture") != null) {
                    byte[] imgData = rs.getBytes("profile_picture");
                    ImageIcon originalIcon = new ImageIcon(imgData);
                    Image image = originalIcon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
                    profileImage = new ImageIcon(image);
                    profileImageLabel.setIcon(profileImage);
                } else {
                    // Default profile image placeholder
                    profileImageLabel.setBackground(new Color(222, 226, 230));
                    // Draw initial letters
                    String initials = getInitials(user.getUsername());
                    JLabel initialLabel = new JLabel(initials);
                    initialLabel.setFont(new Font("Arial", Font.BOLD, 30));
                    initialLabel.setForeground(new Color(73, 80, 87));
                    initialLabel.setHorizontalAlignment(SwingConstants.CENTER);
                    profileImageLabel.add(initialLabel);
                }
            } catch (Exception e) {
                System.err.println("Failed to load profile picture: " + e.getMessage());
                // Set default profile image
                profileImageLabel.setBackground(new Color(222, 226, 230));
                String initials = getInitials(user.getUsername());
                JLabel initialLabel = new JLabel(initials);
                initialLabel.setFont(new Font("Arial", Font.BOLD, 30));
                initialLabel.setForeground(new Color(73, 80, 87));
                initialLabel.setHorizontalAlignment(SwingConstants.CENTER);
                profileImageLabel.add(initialLabel);
            } finally {
                DatabaseConnection.closeConnection(conn, stmt, rs);
            }
        }
        
        private String getInitials(String name) {
            if (name == null || name.isEmpty()) return "U";
            String[] parts = name.split(" ");
            if (parts.length == 1) return String.valueOf(parts[0].charAt(0)).toUpperCase();
            return (String.valueOf(parts[0].charAt(0)) + String.valueOf(parts[1].charAt(0))).toUpperCase();
        }
        
        private void updateProfile() {
            String username = txtUsername.getText().trim();
            String phone = txtPhone.getText().trim();
            String address = txtAddress.getText().trim();
            
            if (username.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter your username.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (phone.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter your phone number.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (address.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter your address.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Update user information in database
            Connection conn = null;
            PreparedStatement stmt = null;
            try {
                conn = DatabaseConnection.getConnection();
                String query;
                if (selectedFile != null) {
                    query = "UPDATE users SET username = ?, phone = ?, address = ?, profile_picture = ? WHERE id = ?";
                    stmt = conn.prepareStatement(query);
                    FileInputStream fis = new FileInputStream(selectedFile);
                    stmt.setString(1, username);
                    stmt.setString(2, phone);
                    stmt.setString(3, address);
                    stmt.setBinaryStream(4, fis, (int) selectedFile.length());
                    stmt.setInt(5, user.getId());
                } else {
                    query = "UPDATE users SET username = ?, phone = ?, address = ? WHERE id = ?";
                    stmt = conn.prepareStatement(query);
                    stmt.setString(1, username);
                    stmt.setString(2, phone);
                    stmt.setString(3, address);
                    stmt.setInt(4, user.getId());
                }
                int result = stmt.executeUpdate();
                if (result > 0) {
                    user.setUsername(username);
                    user.setPhone(phone);
                    user.setAddress(address);
                    JOptionPane.showMessageDialog(this, "Profile updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to update profile.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException | FileNotFoundException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error updating profile: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            } finally {
                DatabaseConnection.closeConnection(conn, stmt, null);
            }
        }
    }
}