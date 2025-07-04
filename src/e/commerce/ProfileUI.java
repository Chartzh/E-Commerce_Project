package e.commerce;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.io.*;
import java.sql.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class ProfileUI extends JPanel {
    private CardLayout cardLayout;
    private JPanel contentPanel;
    private User currentUser;
    private JButton[] navButtons;
    private Color primaryColor = new Color(255, 102, 0); // Orange to match logo
    private Color backgroundColor = new Color(240, 242, 245); // Lighter background
    private Color lightGrayColor = new Color(225, 228, 232); // Light gray for panels and borders
    private Color darkGrayColor = new Color(108, 117, 125); // For secondary text
    private Color textColor = new Color(33, 37, 41); // Dark text color

    public ProfileUI() {
        currentUser = Authentication.getCurrentUser();
        if (currentUser == null) {
            JOptionPane.showMessageDialog(null, "Tidak ada pengguna yang login!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        setLayout(new BorderLayout());
        setBackground(backgroundColor);
        setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25)); // Increased padding

        // Create modal-like container with subtle shadow
        JPanel modalPanel = new JPanel(new BorderLayout());
        modalPanel.setBackground(Color.WHITE);
        modalPanel.setBorder(new LineBorder(lightGrayColor, 1) {
            @Override
            public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getLineColor());
                g2.drawRoundRect(x, y, width - 1, height - 1, 12, 12); // Rounded corners for the modal
                g2.dispose();
            }
        });
        modalPanel.setPreferredSize(new Dimension(1200, 900)); // Set a preferred size

        // Header with "Account Settings" and close button
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, lightGrayColor), // Softer separator
                BorderFactory.createEmptyBorder(20, 30, 20, 30) // More padding
        ));

        JLabel titleLabel = new JLabel("Pengaturan Akun"); // Translated
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18)); // Slightly larger font
        titleLabel.setForeground(textColor);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JButton closeButton = new JButton("×");
        closeButton.setFont(new Font("Arial", Font.BOLD, 24)); // Larger close icon
        closeButton.setForeground(darkGrayColor);
        closeButton.setBackground(Color.WHITE);
        closeButton.setBorder(BorderFactory.createEmptyBorder(0, 15, 0, 0));
        closeButton.setFocusPainted(false);
        closeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeButton.addActionListener(e -> {
            CardLayout parentLayout = (CardLayout) ProfileUI.this.getParent().getLayout();
            parentLayout.show(ProfileUI.this.getParent(), "Dashboard");
        });
        closeButton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { closeButton.setForeground(primaryColor); }
            public void mouseExited(MouseEvent e) { closeButton.setForeground(darkGrayColor); }
        });
        headerPanel.add(closeButton, BorderLayout.EAST);
        modalPanel.add(headerPanel, BorderLayout.NORTH);

        // Main content with sidebar and content area
        JPanel mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBackground(Color.WHITE);

        // Navigation sidebar
        JPanel navPanel = createNavigationPanel();
        navPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, lightGrayColor)); // Softer separator
        mainContainer.add(navPanel, BorderLayout.WEST);

        // Content area with CardLayout
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(Color.WHITE);

        ProfilePanel profilePanel = new ProfilePanel(currentUser);
        contentPanel.add(profilePanel, "Profile");

        // NEW: Add AddressPanel
        AddressPanel addressPanel = new AddressPanel(currentUser);
        contentPanel.add(addressPanel, "Addresses");


        // Updated navigation items to match the image
        contentPanel.add(createPlaceholderPanel("Pembayaran"), "Payment"); // Translated
        contentPanel.add(createPlaceholderPanel("Keamanan"), "Security"); // Translated
        contentPanel.add(createPlaceholderPanel("Perjanjian Hukum"), "Legal Agreement"); // Translated

        mainContainer.add(contentPanel, BorderLayout.CENTER);
        modalPanel.add(mainContainer, BorderLayout.CENTER);

        // Center the modal panel using GridBagLayout with proper constraints
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(backgroundColor);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH; // Make modalPanel fill available space
        gbc.weightx = 1.0; // Allow modalPanel to take all extra horizontal space
        gbc.weighty = 1.0; // Allow modalPanel to take all extra vertical space
        centerPanel.add(modalPanel, gbc); // Add with constraints
        
        add(centerPanel, BorderLayout.CENTER);
    }

    private JPanel createNavigationPanel() {
        JPanel navPanel = new JPanel();
        navPanel.setLayout(new BoxLayout(navPanel, BoxLayout.Y_AXIS));
        navPanel.setBackground(Color.WHITE);
        navPanel.setPreferredSize(new Dimension(220, 0)); // Slightly wider sidebar
        navPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0)); 

        // Profile Info Panel (still kept for initial display, though not in the navigation buttons directly)
        JPanel profileInfoPanel = new JPanel(new GridBagLayout());
        profileInfoPanel.setBackground(Color.WHITE);
        profileInfoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        profileInfoPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        profileInfoPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 15)); // Adjusted padding

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 5, 0);
        gbc.anchor = GridBagConstraints.CENTER;

        JLabel navProfileImageLabel = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Shape circle = new Ellipse2D.Double(0, 0, getWidth(), getHeight());
                g2d.setClip(circle);
                if (getIcon() != null) {
                    getIcon().paintIcon(this, g2d, 0, 0);
                } else {
                    String initials = getInitials(currentUser.getUsername());
                    Font font = new Font("Arial", Font.BOLD, 24);
                    g2d.setFont(font);
                    g2d.setColor(new Color(73, 80, 87));
                    FontMetrics metrics = g2d.getFontMetrics(font);
                    int x = (getWidth() - metrics.stringWidth(initials)) / 2;
                    int y = ((getHeight() - metrics.getHeight()) / 2) + metrics.getAscent();
                    g2d.drawString(initials, x, y);
                }
                g2d.dispose();
            }
        };
        navProfileImageLabel.setPreferredSize(new Dimension(60, 60));
        navProfileImageLabel.setHorizontalAlignment(JLabel.CENTER);
        navProfileImageLabel.setBackground(lightGrayColor);
        navProfileImageLabel.setOpaque(false);
        loadProfilePictureForNav(navProfileImageLabel); // Load profile pic here
        profileInfoPanel.add(navProfileImageLabel, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        JLabel navUsernameLabel = new JLabel(currentUser.getUsername());
        navUsernameLabel.setFont(new Font("Arial", Font.BOLD, 16));
        navUsernameLabel.setForeground(textColor);
        profileInfoPanel.add(navUsernameLabel, gbc);

        navPanel.add(profileInfoPanel);
        navPanel.add(Box.createVerticalStrut(10)); // Space between profile info and buttons
       
        String[] navItems = {"Profil", "Alamat", "Pembayaran", "Keamanan", "Perjanjian Hukum"}; // Translated
        navButtons = new JButton[navItems.length];

        for (int i = 0; i < navItems.length; i++) {
            JButton button = new JButton(navItems[i]);
            navButtons[i] = button;
            button.setAlignmentX(Component.LEFT_ALIGNMENT);
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45)); // Taller buttons
            button.setFont(new Font("Arial", Font.PLAIN, 15)); // Slightly larger font
            button.setForeground(i == 0 ? primaryColor : textColor);
            button.setBackground(i == 0 ? new Color(255, 245, 230) : Color.WHITE); // Lighter orange for selected
            button.setBorder(BorderFactory.createCompoundBorder(
                    i == 0 ? BorderFactory.createMatteBorder(0, 0, 0, 4, primaryColor) : null, // Thicker indicator
                    BorderFactory.createEmptyBorder(10, 25, 10, 15) // More padding
            ));
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.setFocusPainted(false);
            button.setCursor(new Cursor(Cursor.HAND_CURSOR));

            final String panelName = navItems[i].equals("Profil") ? "Profile" : // Map translated to English panel names
                                     navItems[i].equals("Alamat") ? "Addresses" :
                                     navItems[i].equals("Pembayaran") ? "Payment" :
                                     navItems[i].equals("Keamanan") ? "Security" : "Legal Agreement";
            final int index = i;

            // Add hover effects
            button.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    if (navButtons[index].getBackground().equals(Color.WHITE)) {
                        navButtons[index].setBackground(new Color(245, 245, 245)); // Slight hover background
                    }
                }
                public void mouseExited(MouseEvent e) {
                    if (navButtons[index].getBackground().equals(new Color(245, 245, 245))) {
                        navButtons[index].setBackground(Color.WHITE); // Restore original background
                    }
                }
            });

            button.addActionListener(e -> {
                for (int j = 0; j < navButtons.length; j++) {
                    navButtons[j].setForeground(textColor);
                    navButtons[j].setBackground(Color.WHITE);
                    navButtons[j].setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 15));
                }
                navButtons[index].setForeground(primaryColor);
                navButtons[index].setBackground(new Color(255, 245, 230));
                navButtons[index].setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 0, 4, primaryColor),
                        BorderFactory.createEmptyBorder(10, 25, 10, 15)
                ));
                cardLayout.show(contentPanel, panelName);
            });
            navPanel.add(button);
        }

        navPanel.add(Box.createVerticalGlue()); // Pushes buttons to the top

        return navPanel;
    }

    private void loadProfilePictureForNav(JLabel label) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            conn = DatabaseConnection.getConnection();
            String query = "SELECT profile_picture FROM profile WHERE user_id = ?"; // Mengambil dari tabel profile
            stmt = conn.prepareStatement(query);
            stmt.setInt(1, currentUser.getId());
            rs = stmt.executeQuery();
            if (rs.next()) {
                byte[] imgData = rs.getBytes("profile_picture");
                if (imgData != null) {
                    ImageIcon originalIcon = new ImageIcon(imgData);
                    Image image = originalIcon.getImage().getScaledInstance(label.getPreferredSize().width, label.getPreferredSize().height, Image.SCALE_SMOOTH);
                    label.setIcon(new ImageIcon(image));
                } else {
                    label.setIcon(null);
                    label.setBackground(lightGrayColor);
                }
            }
        } catch (Exception e) {
            System.err.println("Gagal memuat foto profil untuk navigasi: " + e.getMessage());
            e.printStackTrace();
            label.setIcon(null);
            label.setBackground(lightGrayColor);
        } finally {
            DatabaseConnection.closeConnection(conn, stmt, rs);
        }
    }

    private JPanel createPlaceholderPanel(String title) {
        JPanel panel = new JPanel(new GridBagLayout()); // Use GridBagLayout for centering
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        JLabel label = new JLabel("<html><center>Ini adalah bagian <b>" + title + "</b>.<br>Konten segera hadir!</center></html>"); // Translated
        label.setFont(new Font("Arial", Font.ITALIC, 16));
        label.setForeground(darkGrayColor);
        panel.add(label);
        return panel;
    }

    private String getInitials(String name) {
        if (name == null || name.isEmpty()) return "U";
        StringBuilder initials = new StringBuilder();
        String[] parts = name.split(" ");
        if (parts.length > 0 && !parts[0].isEmpty()) {
            initials.append(parts[0].charAt(0));
        }
        if (parts.length > 1 && !parts[1].isEmpty()) {
            initials.append(parts[1].charAt(0));
        }
        return initials.toString().toUpperCase();
    }


    private class ProfilePanel extends JPanel {
        private JLabel profileImageLabel;
        private JLabel bannerImageLabel;
        private JTextField txtFullName, txtPhone, txtEmail, txtNik;
        private JButton btnSave;
        private ImageIcon profileImage; 
        private ImageIcon bannerImage;  
        private File selectedFile;      
        private File selectedBannerFile; 
        private User user;

        public ProfilePanel(User user) {
            this.user = user;
            setLayout(new BorderLayout());
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createEmptyBorder(0, 40, 30, 40));

            JPanel scrollableContent = new JPanel();
            scrollableContent.setLayout(new BoxLayout(scrollableContent, BoxLayout.Y_AXIS));
            scrollableContent.setBackground(Color.WHITE);

            JPanel topSectionContainer = new JPanel(new BorderLayout());
            topSectionContainer.setBackground(Color.WHITE);
            topSectionContainer.setAlignmentX(Component.CENTER_ALIGNMENT);
            topSectionContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 250));


            JLayeredPane layeredPane = new JLayeredPane();
            int layeredPaneHeight = 180 + (100 / 2) + 15 + 20; 
            layeredPane.setPreferredSize(new Dimension(1, layeredPaneHeight)); 
            layeredPane.setMinimumSize(new Dimension(1, layeredPaneHeight));   
            layeredPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, layeredPaneHeight)); 

            // 1. Banner Image (Layer 0, paling bawah)
            JPanel bannerPanel = new JPanel(new BorderLayout());
            bannerPanel.setBackground(new Color(150, 180, 200)); 
            bannerImageLabel = new JLabel();
            bannerImageLabel.setHorizontalAlignment(SwingConstants.CENTER);
            bannerImageLabel.setOpaque(true);
            bannerPanel.add(bannerImageLabel, BorderLayout.CENTER);
            layeredPane.add(bannerPanel, JLayeredPane.DEFAULT_LAYER);

            // 2. Profile Image (Layer 1, di atas banner)
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
            profileImageContainer.setPreferredSize(new Dimension(100, 100));
            profileImageContainer.setLayout(new BorderLayout());
            profileImageContainer.setBackground(lightGrayColor);
            profileImageContainer.setBorder(new LineBorder(Color.WHITE, 3, true)); 
            
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
                        String initials = ProfileUI.this.getInitials(user.getUsername());
                        Font font = new Font("Arial", Font.BOLD, 36);
                        g2d.setFont(font);
                        g2d.setColor(new Color(73, 80, 87));
                        FontMetrics metrics = g2d.getFontMetrics(font);
                        int x = (getWidth() - metrics.stringWidth(initials)) / 2;
                        int y = ((getHeight() - metrics.getHeight()) / 2) + metrics.getAscent();
                        g2d.drawString(initials, x, y);
                    }
                    g2d.dispose();
                }
            };
            profileImageLabel.setHorizontalAlignment(JLabel.CENTER);
            profileImageLabel.setOpaque(false);

            profileImageContainer.add(profileImageLabel, BorderLayout.CENTER);
            layeredPane.add(profileImageContainer, JLayeredPane.PALETTE_LAYER);

            // 3. Label "Change Profile Photo" (Layer 2, di atas foto profil)
            JLabel changeProfilePhotoLabel = new JLabel("Ubah Foto Profil"); // Translated
            changeProfilePhotoLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            changeProfilePhotoLabel.setForeground(primaryColor.darker());
            changeProfilePhotoLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            changeProfilePhotoLabel.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setDialogTitle("Pilih Gambar Profil"); // Translated
                    fileChooser.setFileFilter(new FileNameExtensionFilter("File gambar", "jpg", "jpeg", "png", "gif")); // Translated
                    if (fileChooser.showOpenDialog(ProfilePanel.this) == JFileChooser.APPROVE_OPTION) {
                        selectedFile = fileChooser.getSelectedFile();
                        try (FileInputStream fis = new FileInputStream(selectedFile)) {
                            byte[] imageData = Files.readAllBytes(selectedFile.toPath());
                            ImageIcon originalIcon = new ImageIcon(imageData);
                            Image image = originalIcon.getImage().getScaledInstance(
                                profileImageContainer.getWidth(), profileImageContainer.getHeight(), Image.SCALE_SMOOTH);
                            profileImage = new ImageIcon(image);
                            profileImageLabel.setIcon(profileImage);
                            user.setProfilePicture(imageData); 
                            profileImageLabel.repaint();
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(ProfilePanel.this,
                                    "Error memuat gambar: " + ex.getMessage(), // Translated
                                    "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
                public void mouseEntered(MouseEvent e) { changeProfilePhotoLabel.setText("<html><u>Ubah Foto Profil</u></html>"); } // Translated
                public void mouseExited(MouseEvent e) { changeProfilePhotoLabel.setText("Ubah Foto Profil"); } // Translated
            });
            layeredPane.add(changeProfilePhotoLabel, JLayeredPane.PALETTE_LAYER);


            layeredPane.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    int layeredPaneWidth = layeredPane.getWidth();
                    
                    bannerPanel.setBounds(0, 0, layeredPaneWidth, 180);
                    loadBannerPicture(bannerPanel.getWidth(), bannerPanel.getHeight());

                    int profileX = (layeredPaneWidth - 100) / 2; 
                    int profileY = 180 - (100 / 2); 
                    profileImageContainer.setBounds(profileX, profileY, 100, 100);
                    loadProfilePicture(profileImageContainer.getWidth(), profileImageContainer.getHeight());

                    int labelY = profileY + 100 + 5; 
                    changeProfilePhotoLabel.setBounds(profileX - 20, labelY, 140, 20); 
                    
                    layeredPane.revalidate();
                    layeredPane.repaint();
                }
            });


            topSectionContainer.add(layeredPane, BorderLayout.CENTER);
            scrollableContent.add(topSectionContainer);

            scrollableContent.add(Box.createVerticalStrut(20));

            txtFullName = new JTextField();
            scrollableContent.add(createFormField("Nama Lengkap", user.getUsername(), "Budi Sabudi", txtFullName)); 

            txtEmail = new JTextField();
            scrollableContent.add(createFormField("Alamat Email", user.getEmail(), "budi.sabudi@example.com", txtEmail));

            txtNik = new JTextField();
            scrollableContent.add(createFormField("NIK", user.getNik(), "1234567890123456", txtNik));

            txtPhone = new JTextField();
            String phoneNumber = user.getPhone();
            if (phoneNumber != null && phoneNumber.startsWith("+")) {
                phoneNumber = phoneNumber.substring(phoneNumber.indexOf(" ") + 1);
            }
            scrollableContent.add(createFormField("Nomor Telepon", phoneNumber, "08123456789", txtPhone));
            
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.setBackground(Color.WHITE);

            btnSave = new JButton("Simpan Perubahan"); // Translated
            btnSave.setFont(new Font("Arial", Font.BOLD, 15));
            btnSave.setForeground(Color.WHITE);
            btnSave.setBackground(primaryColor);
            btnSave.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(primaryColor, 1, true), 
                    BorderFactory.createEmptyBorder(10, 25, 10, 25) 
            ));
            btnSave.setFocusPainted(false);
            btnSave.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnSave.addActionListener(e -> updateProfile());
            btnSave.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { btnSave.setBackground(primaryColor.darker()); }
                public void mouseExited(MouseEvent e) { btnSave.setBackground(primaryColor); }
            });

            buttonPanel.add(btnSave);
            scrollableContent.add(buttonPanel);

            JScrollPane scrollPane = new JScrollPane(scrollableContent);
            scrollPane.setBorder(null);
            scrollPane.getVerticalScrollBar().setUnitIncrement(16);
            scrollPane.setViewportBorder(null);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

            add(scrollPane, BorderLayout.CENTER);

            SwingUtilities.invokeLater(() -> {
                layeredPane.getComponentListeners()[0].componentResized(
                    new ComponentEvent(layeredPane, ComponentEvent.COMPONENT_RESIZED));
                
                setupPlaceholder(txtFullName, "Budi Sabudi");
                setupPlaceholder(txtEmail, "budi.sabudi@example.com");
                setupPlaceholder(txtNik, "1234567890123456");
                setupPlaceholder(txtPhone, "08123456789");
            });
        }

        private JPanel createFormField(String labelText, String initialValue, String placeholder, JTextField textField) {
            JPanel panel = new JPanel(new BorderLayout(0, 5));
            panel.setBackground(Color.WHITE);
            panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

            JLabel lbl = new JLabel(labelText);
            lbl.setFont(new Font("Arial", Font.PLAIN, 13));
            lbl.setForeground(darkGrayColor);
            panel.add(lbl, BorderLayout.NORTH);

            textField.setText(initialValue);
            textField.setFont(new Font("Arial", Font.PLAIN, 15));
            textField.setBorder(new LineBorder(lightGrayColor, 1) { 
                @Override
                public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(getLineColor());
                    g2.drawRoundRect(x, y, width - 1, height - 1, 8, 8);
                    g2.dispose();
                }
            });
            setupPlaceholder(textField, placeholder);

            panel.add(textField, BorderLayout.CENTER);
            return panel;
        }

        private void setupPlaceholder(JTextComponent component, String placeholder) {
            component.putClientProperty("placeholder", placeholder);
            component.putClientProperty("originalForeground", component.getForeground());

            if (component.getText().isEmpty() || component.getText().equals(placeholder)) {
                component.setText(placeholder);
                component.setForeground(darkGrayColor);
                component.putClientProperty("showingPlaceholder", true);
            } else {
                component.putClientProperty("showingPlaceholder", false);
                component.setForeground((Color) component.getClientProperty("originalForeground"));
            }

            component.addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent e) {
                    if (Boolean.TRUE.equals(component.getClientProperty("showingPlaceholder"))) {
                        component.setText("");
                        component.setForeground((Color) component.getClientProperty("originalForeground"));
                        component.putClientProperty("showingPlaceholder", false);
                    }
                }

                @Override
                public void focusLost(FocusEvent e) {
                    if (component.getText().isEmpty()) {
                        component.setText(placeholder);
                        component.setForeground(darkGrayColor);
                        component.putClientProperty("showingPlaceholder", true);
                    }
                }
            });
        }

        private void loadBannerPicture(int targetWidth, int targetHeight) {
            if (user.getBannerPicture() != null && bannerImage != null) {
                Image image = bannerImage.getImage().getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
                bannerImageLabel.setIcon(new ImageIcon(image));
                return;
            }

            Connection conn = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try {
                conn = DatabaseConnection.getConnection();
                String query = "SELECT banner_picture FROM profile WHERE user_id = ?";
                stmt = conn.prepareStatement(query);
                stmt.setInt(1, user.getId());
                rs = stmt.executeQuery();
                if (rs.next()) {
                    byte[] imgData = rs.getBytes("banner_picture");
                    if (imgData != null) {
                        ImageIcon originalIcon = new ImageIcon(imgData);
                        Image image = originalIcon.getImage().getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
                        bannerImage = new ImageIcon(image); 
                        bannerImageLabel.setIcon(bannerImage);
                        user.setBannerPicture(imgData); 
                    } else {
                        bannerImageLabel.setIcon(null);
                        bannerImageLabel.setBackground(new Color(150, 180, 200));
                        user.setBannerPicture(null);
                    }
                }
            }
            catch (SQLException e) {
                System.err.println("Database error memuat gambar banner: " + e.getMessage()); // Translated
                e.printStackTrace();
                bannerImageLabel.setIcon(null);
                bannerImageLabel.setBackground(new Color(150, 180, 200));
            } catch (Exception e) {
                System.err.println("Gagal memuat gambar banner: " + e.getMessage()); // Translated
                e.printStackTrace();
                bannerImageLabel.setIcon(null);
                bannerImageLabel.setBackground(new Color(150, 180, 200));
            } finally {
                DatabaseConnection.closeConnection(conn, stmt, rs);
            }
        }

        private void loadProfilePicture(int targetWidth, int targetHeight) {
            if (user.getProfilePicture() != null && profileImage != null) {
                Image image = profileImage.getImage().getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
                profileImageLabel.setIcon(new ImageIcon(image));
                return; 
            }

            Connection conn = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try {
                conn = DatabaseConnection.getConnection();
                String query = "SELECT profile_picture FROM profile WHERE user_id = ?";
                stmt = conn.prepareStatement(query);
                stmt.setInt(1, user.getId());
                rs = stmt.executeQuery();
                if (rs.next()) {
                    byte[] imgData = rs.getBytes("profile_picture");
                    if (imgData != null) {
                        ImageIcon originalIcon = new ImageIcon(imgData);
                        Image image = originalIcon.getImage().getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
                        profileImage = new ImageIcon(image); 
                        profileImageLabel.setIcon(profileImage);
                        user.setProfilePicture(imgData); 
                    } else {
                        profileImageLabel.setIcon(null);
                        profileImageLabel.setBackground(lightGrayColor);
                        profileImageLabel.repaint();
                        user.setProfilePicture(null); 
                    }
                }
            }
            catch (SQLException e) {
                System.err.println("Database error memuat foto profil: " + e.getMessage()); // Translated
                e.printStackTrace();
                profileImageLabel.setIcon(null);
                profileImageLabel.setBackground(lightGrayColor);
            } catch (Exception e) {
                System.err.println("Gagal memuat foto profil: " + e.getMessage()); // Translated
                e.printStackTrace();
                profileImageLabel.setIcon(null);
                profileImageLabel.setBackground(lightGrayColor);
            } finally {
                DatabaseConnection.closeConnection(conn, stmt, rs);
            }
        }
        
        private void updateProfile() {
            String fullName = (Boolean.TRUE.equals(txtFullName.getClientProperty("showingPlaceholder"))) ? "" : txtFullName.getText().trim();
            String email = (Boolean.TRUE.equals(txtEmail.getClientProperty("showingPlaceholder"))) ? "" : txtEmail.getText().trim();
            String nik = (Boolean.TRUE.equals(txtNik.getClientProperty("showingPlaceholder"))) ? "" : txtNik.getText().trim();
            String phone = (Boolean.TRUE.equals(txtPhone.getClientProperty("showingPlaceholder"))) ? "" : txtPhone.getText().trim();

            if (fullName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nama Lengkap tidak boleh kosong.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (email.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Alamat Email tidak boleh kosong.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (!email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
                JOptionPane.showMessageDialog(this, "Format email tidak valid!", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!phone.matches("\\d{10,13}")) {
                JOptionPane.showMessageDialog(this, "Nomor telepon harus 10-13 digit angka!", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (!nik.matches("\\d{16}")) {
                JOptionPane.showMessageDialog(this, "NIK harus 16 digit angka!", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }


            Connection conn = null;
            PreparedStatement stmtUsers = null;
            PreparedStatement stmtProfile = null;

            try {
                conn = DatabaseConnection.getConnection();
                conn.setAutoCommit(false); 

                boolean usersUpdated = false;
                boolean profileUpdated = false;

                String updateUserSql = "UPDATE users SET username = ?, email = ? WHERE id = ?";
                stmtUsers = conn.prepareStatement(updateUserSql);
                stmtUsers.setString(1, fullName);
                stmtUsers.setString(2, email);
                stmtUsers.setInt(3, user.getId());
                int resultUsers = stmtUsers.executeUpdate();
                if (resultUsers > 0) {
                    usersUpdated = true;
                    user.setUsername(fullName);
                    user.setEmail(email);
                }

                StringBuilder updateProfileSqlBuilder = new StringBuilder("UPDATE profile SET nik = ?, phone = ?");
                boolean updateProfilePic = (selectedFile != null);
                boolean updateBannerPic = (selectedBannerFile != null);

                if (updateProfilePic) {
                    updateProfileSqlBuilder.append(", profile_picture = ?");
                }
                if (updateBannerPic) {
                    updateProfileSqlBuilder.append(", banner_picture = ?");
                }
                updateProfileSqlBuilder.append(" WHERE user_id = ?");
                
                stmtProfile = conn.prepareStatement(updateProfileSqlBuilder.toString());
                int paramIndex = 1;
                stmtProfile.setString(paramIndex++, nik);
                stmtProfile.setString(paramIndex++, phone);

                if (updateProfilePic) {
                    try (FileInputStream fis = new FileInputStream(selectedFile)) {
                        stmtProfile.setBinaryStream(paramIndex++, fis, (int) selectedFile.length());
                    }
                }
                if (updateBannerPic) {
                    try (FileInputStream fisBanner = new FileInputStream(selectedBannerFile)) {
                        stmtProfile.setBinaryStream(paramIndex++, fisBanner, (int) selectedBannerFile.length());
                    }
                }
                stmtProfile.setInt(paramIndex++, user.getId()); 

                int resultProfile = stmtProfile.executeUpdate();
                if (resultProfile > 0) {
                    profileUpdated = true;
                    user.setNik(nik);
                    user.setPhone(phone);

                    if (updateProfilePic) {
                        user.setProfilePicture(Files.readAllBytes(selectedFile.toPath()));
                    }
                    if (updateBannerPic) {
                        user.setBannerPicture(Files.readAllBytes(selectedBannerFile.toPath()));
                    }
                }

                if (usersUpdated || profileUpdated) {
                    conn.commit();
                    JOptionPane.showMessageDialog(this, "Profil berhasil diperbarui!", "Sukses", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    conn.rollback(); 
                    JOptionPane.showMessageDialog(this, "Gagal memperbarui profil. Tidak ada perubahan yang terdeteksi atau ID tidak ditemukan.", "Error", JOptionPane.ERROR_MESSAGE);
                }

            } catch (SQLException | IOException e) {
                try {
                    if (conn != null) conn.rollback(); 
                } catch (SQLException ex) {
                    System.err.println("Rollback gagal: " + ex.getMessage()); // Translated
                }
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Terjadi kesalahan saat memperbarui profil: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            } finally {
                try {
                    if (stmtUsers != null) stmtUsers.close();
                    if (stmtProfile != null) stmtProfile.close();
                    if (conn != null) conn.setAutoCommit(true); 
                } catch (SQLException e) {
                    System.err.println("Error menutup sumber daya: " + e.getMessage()); // Translated
                }
                DatabaseConnection.closeConnection(conn, null, null); 
            }
        }
    }

    private class AddressPanel extends JPanel {
        private User user;
        private JPanel addressListPanel;
        private JScrollPane scrollPane;

        private Color primaryColor = new Color(255, 102, 0);
        private Color backgroundColor = new Color(240, 242, 245);
        private Color lightGrayColor = new Color(225, 228, 232);
        private Color darkGrayColor = new Color(108, 117, 125);
        private Color textColor = new Color(33, 37, 41);

        public AddressPanel(User user) {
            this.user = user;
            setLayout(new BorderLayout());
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

            JLabel title = new JLabel("Alamat Anda"); // Translated
            title.setFont(new Font("Arial", Font.BOLD, 20));
            title.setForeground(textColor);
            add(title, BorderLayout.NORTH);

            addressListPanel = new JPanel();
            addressListPanel.setLayout(new BoxLayout(addressListPanel, BoxLayout.Y_AXIS));
            addressListPanel.setBackground(Color.WHITE);

            scrollPane = new JScrollPane(addressListPanel);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            scrollPane.getVerticalScrollBar().setUnitIncrement(16);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
            add(scrollPane, BorderLayout.CENTER);

            JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            southPanel.setBackground(Color.WHITE);

            JButton addAddressButton = new JButton("Tambah Alamat Baru"); // Translated
            addAddressButton.setFont(new Font("Arial", Font.BOLD, 14));
            addAddressButton.setForeground(Color.WHITE);
            addAddressButton.setBackground(primaryColor);
            addAddressButton.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(primaryColor, 1, true),
                    BorderFactory.createEmptyBorder(10, 20, 10, 20)
            ));
            addAddressButton.setFocusPainted(false);
            addAddressButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            addAddressButton.addActionListener(e -> showAddAddressDialog());
            southPanel.add(addAddressButton);

            add(southPanel, BorderLayout.SOUTH);

            loadAddresses();
        }

        private void loadAddresses() {
            addressListPanel.removeAll();
            List<Address> addresses = getAddressesFromDatabase(user.getId());
            if (addresses.isEmpty()) {
                JLabel noAddressLabel = new JLabel("Tidak ada alamat ditemukan. Klik 'Tambah Alamat Baru' untuk menambahkannya."); // Translated
                noAddressLabel.setFont(new Font("Arial", Font.ITALIC, 14));
                noAddressLabel.setForeground(darkGrayColor);
                addressListPanel.add(noAddressLabel);
            } else {
                for (Address address : addresses) {
                    addressListPanel.add(createAddressCard(address));
                    addressListPanel.add(Box.createVerticalStrut(10)); 
                }
            }
            addressListPanel.revalidate();
            addressListPanel.repaint();
        }

        private JPanel createAddressCard(Address address) {
            JPanel card = new JPanel(new BorderLayout(10, 10));
            card.setBackground(new Color(250, 250, 250));
            card.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(lightGrayColor, 1, true),
                    BorderFactory.createEmptyBorder(15, 15, 15, 15)
            ));

            JLabel addressNameLabel = new JLabel("<html><b>" + address.getLabel() + "</b></html>");
            addressNameLabel.setFont(new Font("Arial", Font.BOLD, 16));
            addressNameLabel.setForeground(textColor);
            card.add(addressNameLabel, BorderLayout.NORTH);

            JTextArea addressDetails = new JTextArea();
            addressDetails.setEditable(false);
            addressDetails.setLineWrap(true);
            addressDetails.setWrapStyleWord(true);
            addressDetails.setBackground(new Color(250, 250, 250));
            addressDetails.setFont(new Font("Arial", Font.PLAIN, 14));
            addressDetails.setForeground(darkGrayColor);
            
            // Format alamat untuk ditampilkan, sertakan kecamatan dan kelurahan
            addressDetails.setText(
                address.getFullAddress() + "\n" + 
                address.getKelurahan() + ", " + address.getKecamatan() + "\n" + 
                address.getCity() + ", " + address.getProvince() + " - " + address.getPostalCode()
            );
            card.add(addressDetails, BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
            buttonPanel.setBackground(new Color(250, 250, 250));

            JButton editButton = new JButton("Edit"); // Translated
            styleMiniButton(editButton, primaryColor);
            editButton.addActionListener(e -> showEditAddressDialog(address));
            buttonPanel.add(editButton);

            JButton deleteButton = new JButton("Hapus"); // Translated
            styleMiniButton(deleteButton, new Color(220, 53, 69)); 
            deleteButton.addActionListener(e -> deleteAddress(address.getId()));
            buttonPanel.add(deleteButton);

            card.add(buttonPanel, BorderLayout.SOUTH);

            return card;
        }

        private void styleMiniButton(JButton button, Color bgColor) {
            button.setFont(new Font("Arial", Font.PLAIN, 12));
            button.setForeground(Color.WHITE);
            button.setBackground(bgColor);
            button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(bgColor.darker(), 1, true),
                    BorderFactory.createEmptyBorder(5, 10, 5, 10)
            ));
            button.setFocusPainted(false);
            button.setCursor(new Cursor(Cursor.HAND_CURSOR));
            button.addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { button.setBackground(bgColor.darker()); }
                public void mouseExited(MouseEvent e) { button.setBackground(bgColor); }
            });
        }


        private void showAddAddressDialog() {
            JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Tambah Alamat Baru", true); // Translated title
            dialog.setLayout(new BorderLayout());
            dialog.setBackground(Color.WHITE);
            dialog.setSize(500, 650); // Increased height for new fields
            dialog.setLocationRelativeTo(this);

            JPanel formPanel = new JPanel(new GridBagLayout());
            formPanel.setBackground(Color.WHITE);
            formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(8, 5, 8, 5);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;

            JTextField txtLabel = new JTextField();
            JTextArea txtFullAddress = new JTextArea(3, 20);
            txtFullAddress.setLineWrap(true);
            txtFullAddress.setWrapStyleWord(true);
            JScrollPane spAddress = new JScrollPane(txtFullAddress);
            spAddress.setBorder(new LineBorder(lightGrayColor, 1));
            
            // --- REVISI: Ambil data dari database untuk JComboBox ---
            JComboBox<String> cmbProvince = new JComboBox<>();
            cmbProvince.addItem("Pilih Provinsi"); // Default placeholder
            for (String p : getProvincesFromDB()) {
                cmbProvince.addItem(p);
            }

            JComboBox<String> cmbCity = new JComboBox<>();
            cmbCity.addItem("Pilih Kota/Kabupaten"); // Default placeholder
            cmbCity.setEnabled(false);

            JComboBox<String> cmbKecamatan = new JComboBox<>(); // NEW
            cmbKecamatan.addItem("Pilih Kecamatan"); // Default placeholder
            cmbKecamatan.setEnabled(false);

            JComboBox<String> cmbKelurahan = new JComboBox<>(); // NEW
            cmbKelurahan.addItem("Pilih Kelurahan"); // Default placeholder
            cmbKelurahan.setEnabled(false);

            JComboBox<String> cmbPostalCode = new JComboBox<>();
            cmbPostalCode.addItem("Pilih Kode Pos"); // Default placeholder
            cmbPostalCode.setEnabled(false);

            // Country field removed, hardcoded later
            // JComboBox<String> cmbCountry = new JComboBox<>(getCountries()); 

            // Add change listeners for dropdowns (cascading logic)
            cmbProvince.addActionListener(e -> {
                String selectedProvince = (String) cmbProvince.getSelectedItem();
                cmbCity.removeAllItems();
                cmbCity.addItem("Pilih Kota/Kabupaten");
                cmbKecamatan.removeAllItems();
                cmbKecamatan.addItem("Pilih Kecamatan");
                cmbKelurahan.removeAllItems();
                cmbKelurahan.addItem("Pilih Kelurahan");
                cmbPostalCode.removeAllItems();
                cmbPostalCode.addItem("Pilih Kode Pos");

                cmbCity.setEnabled(false);
                cmbKecamatan.setEnabled(false);
                cmbKelurahan.setEnabled(false);
                cmbPostalCode.setEnabled(false);

                if (selectedProvince != null && !selectedProvince.equals("Pilih Provinsi")) {
                    populateCitiesInDialog(cmbCity, selectedProvince);
                    cmbCity.setEnabled(true);
                }
            });

            cmbCity.addActionListener(e -> {
                String selectedProvince = (String) cmbProvince.getSelectedItem();
                String selectedCity = (String) cmbCity.getSelectedItem();
                cmbKecamatan.removeAllItems();
                cmbKecamatan.addItem("Pilih Kecamatan");
                cmbKelurahan.removeAllItems();
                cmbKelurahan.addItem("Pilih Kelurahan");
                cmbPostalCode.removeAllItems();
                cmbPostalCode.addItem("Pilih Kode Pos");

                cmbKecamatan.setEnabled(false);
                cmbKelurahan.setEnabled(false);
                cmbPostalCode.setEnabled(false);

                if (selectedProvince != null && !selectedProvince.equals("Pilih Provinsi") &&
                    selectedCity != null && !selectedCity.equals("Pilih Kota/Kabupaten")) {
                    populateKecamatansInDialog(cmbKecamatan, selectedProvince, selectedCity);
                    cmbKecamatan.setEnabled(true);
                }
            });

            cmbKecamatan.addActionListener(e -> { // NEW Listener
                String selectedProvince = (String) cmbProvince.getSelectedItem();
                String selectedCity = (String) cmbCity.getSelectedItem();
                String selectedKecamatan = (String) cmbKecamatan.getSelectedItem();
                cmbKelurahan.removeAllItems();
                cmbKelurahan.addItem("Pilih Kelurahan");
                cmbPostalCode.removeAllItems();
                cmbPostalCode.addItem("Pilih Kode Pos");

                cmbKelurahan.setEnabled(false);
                cmbPostalCode.setEnabled(false);

                if (selectedProvince != null && !selectedProvince.equals("Pilih Provinsi") &&
                    selectedCity != null && !selectedCity.equals("Pilih Kota/Kabupaten") &&
                    selectedKecamatan != null && !selectedKecamatan.equals("Pilih Kecamatan")) {
                    populateKelurahansInDialog(cmbKelurahan, selectedProvince, selectedCity, selectedKecamatan);
                    cmbKelurahan.setEnabled(true);
                }
            });

            cmbKelurahan.addActionListener(e -> { // NEW Listener
                String selectedProvince = (String) cmbProvince.getSelectedItem();
                String selectedCity = (String) cmbCity.getSelectedItem();
                String selectedKecamatan = (String) cmbKecamatan.getSelectedItem();
                String selectedKelurahan = (String) cmbKelurahan.getSelectedItem();
                cmbPostalCode.removeAllItems();
                cmbPostalCode.addItem("Pilih Kode Pos");

                cmbPostalCode.setEnabled(false);

                if (selectedProvince != null && !selectedProvince.equals("Pilih Provinsi") &&
                    selectedCity != null && !selectedCity.equals("Pilih Kota/Kabupaten") &&
                    selectedKecamatan != null && !selectedKecamatan.equals("Pilih Kecamatan") &&
                    selectedKelurahan != null && !selectedKelurahan.equals("Pilih Kelurahan")) {
                    populatePostalCodesInDialog(cmbPostalCode, selectedProvince, selectedCity, selectedKecamatan, selectedKelurahan);
                    cmbPostalCode.setEnabled(true);
                }
            });

            int row = 0;
            gbc.gridx = 0; gbc.gridy = row++;
            formPanel.add(new JLabel("Label Alamat (Contoh: Rumah, Kantor):"), gbc); // Translated
            gbc.gridy = row++;
            formPanel.add(txtLabel, gbc);

            gbc.gridy = row++;
            formPanel.add(new JLabel("Nama Jalan:"), gbc); // Translated
            gbc.gridy = row++;
            formPanel.add(spAddress, gbc);

            gbc.gridy = row++;
            formPanel.add(new JLabel("Provinsi:"), gbc); // Translated
            gbc.gridy = row++;
            formPanel.add(cmbProvince, gbc);

            gbc.gridy = row++;
            formPanel.add(new JLabel("Kota/Kabupaten:"), gbc); // Translated
            gbc.gridy = row++;
            formPanel.add(cmbCity, gbc);

            gbc.gridy = row++;
            formPanel.add(new JLabel("Kecamatan:"), gbc); // NEW Translated
            gbc.gridy = row++;
            formPanel.add(cmbKecamatan, gbc);

            gbc.gridy = row++;
            formPanel.add(new JLabel("Kelurahan:"), gbc); // NEW Translated
            gbc.gridy = row++;
            formPanel.add(cmbKelurahan, gbc);

            gbc.gridy = row++;
            formPanel.add(new JLabel("Kode Pos:"), gbc); // Translated
            gbc.gridy = row++;
            formPanel.add(cmbPostalCode, gbc);

            // Country field removed
            // gbc.gridy = row++;
            // formPanel.add(new JLabel("Country:"), gbc);
            // gbc.gridy = row++;
            // formPanel.add(cmbCountry, gbc);
            
            setupPlaceholder(txtLabel, "Contoh: Rumah, Kantor"); // Translated placeholder
            setupPlaceholder(txtFullAddress, "Nama jalan, nomor rumah/gedung, RT/RW, blok, dll."); // Translated placeholder


            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            buttonPanel.setBackground(Color.WHITE);
            JButton saveButton = new JButton("Tambah Alamat"); // Translated
            styleMiniButton(saveButton, primaryColor);
            saveButton.addActionListener(e -> {
                // Check if user is supervisor and already has an address
                if (user.getRole() != null && "supervisor".equalsIgnoreCase(user.getRole())) {
                    if (getAddressesFromDatabase(user.getId()).size() >= 1) {
                        JOptionPane.showMessageDialog(dialog, "Pengguna dengan role 'supervisor' hanya dapat memiliki satu alamat.", "Peringatan", JOptionPane.WARNING_MESSAGE);
                        return; // Stop adding address
                    }
                }

                String label = getTextFieldValue(txtLabel);
                String fullAddress = getTextAreaValue(txtFullAddress);
                String province = (String) cmbProvince.getSelectedItem();
                String city = (String) cmbCity.getSelectedItem();
                String kecamatan = (String) cmbKecamatan.getSelectedItem(); // NEW
                String kelurahan = (String) cmbKelurahan.getSelectedItem();   // NEW
                String postalCode = (String) cmbPostalCode.getSelectedItem();
                String country = "Indonesia"; // Hardcoded as per requirement

                if (label.isEmpty() || fullAddress.isEmpty() || province == null || province.equals("Pilih Provinsi") ||
                    city == null || city.equals("Pilih Kota/Kabupaten") || kecamatan == null || kecamatan.equals("Pilih Kecamatan") || 
                    kelurahan == null || kelurahan.equals("Pilih Kelurahan") || 
                    postalCode == null || postalCode.equals("Pilih Kode Pos")) {
                    JOptionPane.showMessageDialog(dialog, "Semua kolom harus diisi!", "Validasi Input", JOptionPane.ERROR_MESSAGE); // Translated
                    return;
                }

                Address newAddress = new Address(
                    -1, user.getId(), label, fullAddress, city, province, postalCode, country, kecamatan, kelurahan
                );
                addAddressToDatabase(newAddress);
                dialog.dispose();
                loadAddresses();
            });
            JButton cancelButton = new JButton("Batal"); // Translated
            styleMiniButton(cancelButton, darkGrayColor);
            cancelButton.addActionListener(e -> dialog.dispose());
            buttonPanel.add(saveButton);
            buttonPanel.add(cancelButton);

            dialog.add(new JScrollPane(formPanel), BorderLayout.CENTER);
            dialog.add(buttonPanel, BorderLayout.SOUTH);
            dialog.setVisible(true);
        }

        private void showEditAddressDialog(Address address) {
            JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Edit Alamat", true); // Translated title
            dialog.setLayout(new BorderLayout());
            dialog.setBackground(Color.WHITE);
            dialog.setSize(500, 650); // Increased height
            dialog.setLocationRelativeTo(this);

            JPanel formPanel = new JPanel(new GridBagLayout());
            formPanel.setBackground(Color.WHITE);
            formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(8, 5, 8, 5);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;

            JTextField txtLabel = new JTextField(address.getLabel());
            JTextArea txtFullAddress = new JTextArea(address.getFullAddress(), 3, 20);
            txtFullAddress.setLineWrap(true);
            txtFullAddress.setWrapStyleWord(true);
            JScrollPane spAddress = new JScrollPane(txtFullAddress);
            spAddress.setBorder(new LineBorder(lightGrayColor, 1));
            
            // --- REVISI: Ambil data dari database untuk JComboBox ---
            JComboBox<String> cmbProvince = new JComboBox<>();
            cmbProvince.addItem("Pilih Provinsi"); // Default placeholder
            for (String p : getProvincesFromDB()) {
                cmbProvince.addItem(p);
            }

            JComboBox<String> cmbCity = new JComboBox<>();
            cmbCity.addItem("Pilih Kota/Kabupaten"); // Default placeholder
            
            JComboBox<String> cmbKecamatan = new JComboBox<>(); // NEW
            cmbKecamatan.addItem("Pilih Kecamatan"); // Default placeholder

            JComboBox<String> cmbKelurahan = new JComboBox<>(); // NEW
            cmbKelurahan.addItem("Pilih Kelurahan"); // Default placeholder

            JComboBox<String> cmbPostalCode = new JComboBox<>();
            cmbPostalCode.addItem("Pilih Kode Pos"); // Default placeholder

            // Country field removed
            // JComboBox<String> cmbCountry = new JComboBox<>(getCountries());

            // Set initial values for dropdowns and populate cascading
            if (address.getProvince() != null) {
                cmbProvince.setSelectedItem(address.getProvince());
                populateCitiesInDialog(cmbCity, address.getProvince());
                if (address.getCity() != null) {
                    cmbCity.setSelectedItem(address.getCity());
                    populateKecamatansInDialog(cmbKecamatan, address.getProvince(), address.getCity()); // NEW
                    if (address.getKecamatan() != null) { // NEW
                        cmbKecamatan.setSelectedItem(address.getKecamatan());
                        populateKelurahansInDialog(cmbKelurahan, address.getProvince(), address.getCity(), address.getKecamatan()); // NEW
                        if (address.getKelurahan() != null) { // NEW
                            cmbKelurahan.setSelectedItem(address.getKelurahan());
                            populatePostalCodesInDialog(cmbPostalCode, address.getProvince(), address.getCity(), address.getKecamatan(), address.getKelurahan()); // Modified
                            if (address.getPostalCode() != null) {
                                cmbPostalCode.setSelectedItem(address.getPostalCode());
                            }
                        }
                    }
                }
            }

            cmbProvince.addActionListener(e -> {
                String selectedProvince = (String) cmbProvince.getSelectedItem();
                cmbCity.removeAllItems();
                cmbCity.addItem("Pilih Kota/Kabupaten");
                cmbKecamatan.removeAllItems(); 
                cmbKecamatan.addItem("Pilih Kecamatan");
                cmbKelurahan.removeAllItems(); 
                cmbKelurahan.addItem("Pilih Kelurahan");
                cmbPostalCode.removeAllItems();
                cmbPostalCode.addItem("Pilih Kode Pos");

                cmbCity.setEnabled(false);
                cmbKecamatan.setEnabled(false);
                cmbKelurahan.setEnabled(false);
                cmbPostalCode.setEnabled(false);

                if (selectedProvince != null && !selectedProvince.equals("Pilih Provinsi")) {
                    populateCitiesInDialog(cmbCity, selectedProvince);
                    cmbCity.setEnabled(true);
                }
            });

            cmbCity.addActionListener(e -> {
                String selectedProvince = (String) cmbProvince.getSelectedItem();
                String selectedCity = (String) cmbCity.getSelectedItem();
                cmbKecamatan.removeAllItems();
                cmbKecamatan.addItem("Pilih Kecamatan");
                cmbKelurahan.removeAllItems();
                cmbKelurahan.addItem("Pilih Kelurahan");
                cmbPostalCode.removeAllItems();
                cmbPostalCode.addItem("Pilih Kode Pos");

                cmbKecamatan.setEnabled(false);
                cmbKelurahan.setEnabled(false);
                cmbPostalCode.setEnabled(false);

                if (selectedProvince != null && !selectedProvince.equals("Pilih Provinsi") &&
                    selectedCity != null && !selectedCity.equals("Pilih Kota/Kabupaten")) {
                    populateKecamatansInDialog(cmbKecamatan, selectedProvince, selectedCity);
                    cmbKecamatan.setEnabled(true);
                }
            });

            cmbKecamatan.addActionListener(e -> { // NEW Listener
                String selectedProvince = (String) cmbProvince.getSelectedItem();
                String selectedCity = (String) cmbCity.getSelectedItem();
                String selectedKecamatan = (String) cmbKecamatan.getSelectedItem();
                cmbKelurahan.removeAllItems();
                cmbKelurahan.addItem("Pilih Kelurahan");
                cmbPostalCode.removeAllItems();
                cmbPostalCode.addItem("Pilih Kode Pos");

                cmbKelurahan.setEnabled(false);
                cmbPostalCode.setEnabled(false);

                if (selectedProvince != null && !selectedProvince.equals("Pilih Provinsi") &&
                    selectedCity != null && !selectedCity.equals("Pilih Kota/Kabupaten") &&
                    selectedKecamatan != null && !selectedKecamatan.equals("Pilih Kecamatan")) {
                    populateKelurahansInDialog(cmbKelurahan, selectedProvince, selectedCity, selectedKecamatan);
                    cmbKelurahan.setEnabled(true);
                }
            });

            cmbKelurahan.addActionListener(e -> { // NEW Listener
                String selectedProvince = (String) cmbProvince.getSelectedItem();
                String selectedCity = (String) cmbCity.getSelectedItem();
                String selectedKecamatan = (String) cmbKecamatan.getSelectedItem();
                String selectedKelurahan = (String) cmbKelurahan.getSelectedItem();
                cmbPostalCode.removeAllItems();
                cmbPostalCode.addItem("Pilih Kode Pos");

                cmbPostalCode.setEnabled(false);

                if (selectedProvince != null && !selectedProvince.equals("Pilih Provinsi") &&
                    selectedCity != null && !selectedCity.equals("Pilih Kota/Kabupaten") &&
                    selectedKecamatan != null && !selectedKecamatan.equals("Pilih Kecamatan") &&
                    selectedKelurahan != null && !selectedKelurahan.equals("Pilih Kelurahan")) {
                    populatePostalCodesInDialog(cmbPostalCode, selectedProvince, selectedCity, selectedKecamatan, selectedKelurahan);
                    cmbPostalCode.setEnabled(true);
                }
            });


            int row = 0;
            gbc.gridx = 0; gbc.gridy = row++;
            formPanel.add(new JLabel("Label Alamat (Contoh: Rumah, Kantor):"), gbc); // Translated
            gbc.gridy = row++;
            formPanel.add(txtLabel, gbc);

            gbc.gridy = row++;
            formPanel.add(new JLabel("Nama Jalan:"), gbc); // Translated
            gbc.gridy = row++;
            formPanel.add(spAddress, gbc);

            gbc.gridy = row++;
            formPanel.add(new JLabel("Provinsi:"), gbc); // Translated
            gbc.gridy = row++;
            formPanel.add(cmbProvince, gbc);

            gbc.gridy = row++;
            formPanel.add(new JLabel("Kota/Kabupaten:"), gbc); // Translated
            gbc.gridy = row++;
            formPanel.add(cmbCity, gbc);

            gbc.gridy = row++;
            formPanel.add(new JLabel("Kecamatan:"), gbc); // NEW Translated
            gbc.gridy = row++;
            formPanel.add(cmbKecamatan, gbc);

            gbc.gridy = row++;
            formPanel.add(new JLabel("Kelurahan:"), gbc); // NEW Translated
            gbc.gridy = row++;
            formPanel.add(cmbKelurahan, gbc);

            gbc.gridy = row++;
            formPanel.add(new JLabel("Kode Pos:"), gbc); // Translated
            gbc.gridy = row++;
            formPanel.add(cmbPostalCode, gbc);

            // Country field removed
            // gbc.gridy = row++;
            // formPanel.add(new JLabel("Country:"), gbc);
            // gbc.gridy = row++;
            // formPanel.add(cmbCountry, gbc);

            setupPlaceholder(txtLabel, "Contoh: Rumah, Kantor"); // Translated
            setupPlaceholder(txtFullAddress, "Nama jalan, nomor rumah/gedung, RT/RW, blok, dll."); // Translated


            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            buttonPanel.setBackground(Color.WHITE);
            JButton saveButton = new JButton("Simpan Perubahan"); // Translated
            styleMiniButton(saveButton, primaryColor);
            saveButton.addActionListener(e -> {
                String label = getTextFieldValue(txtLabel);
                String fullAddress = getTextAreaValue(txtFullAddress);
                String province = (String) cmbProvince.getSelectedItem();
                String city = (String) cmbCity.getSelectedItem();
                String kecamatan = (String) cmbKecamatan.getSelectedItem(); // NEW
                String kelurahan = (String) cmbKelurahan.getSelectedItem(); // NEW
                String postalCode = (String) cmbPostalCode.getSelectedItem();
                String country = "Indonesia"; // Hardcoded

                if (label.isEmpty() || fullAddress.isEmpty() || province == null || province.equals("Pilih Provinsi") ||
                    city == null || city.equals("Pilih Kota/Kabupaten") || kecamatan == null || kecamatan.equals("Pilih Kecamatan") || 
                    kelurahan == null || kelurahan.equals("Pilih Kelurahan") || 
                    postalCode == null || postalCode.equals("Pilih Kode Pos")) {
                    JOptionPane.showMessageDialog(dialog, "Semua kolom harus diisi!", "Validasi Input", JOptionPane.ERROR_MESSAGE); // Translated
                    return;
                }

                address.setLabel(label);
                address.setFullAddress(fullAddress);
                address.setCity(city);
                address.setProvince(province);
                address.setKecamatan(kecamatan); // NEW
                address.setKelurahan(kelurahan); // NEW
                address.setPostalCode(postalCode);
                address.setCountry(country);

                updateAddressInDatabase(address);
                dialog.dispose();
                loadAddresses();
            });
            JButton cancelButton = new JButton("Batal"); // Translated
            styleMiniButton(cancelButton, darkGrayColor);
            cancelButton.addActionListener(e -> dialog.dispose());
            buttonPanel.add(saveButton);
            buttonPanel.add(cancelButton);

            dialog.add(new JScrollPane(formPanel), BorderLayout.CENTER);
            dialog.add(buttonPanel, BorderLayout.SOUTH);
            dialog.setVisible(true);
        }

        private String getTextFieldValue(JTextField textField) {
            return (Boolean.TRUE.equals(textField.getClientProperty("showingPlaceholder"))) ? "" : textField.getText().trim();
        }

        private String getTextAreaValue(JTextArea textArea) {
            return (Boolean.TRUE.equals(textArea.getClientProperty("showingPlaceholder"))) ? "" : textArea.getText().trim();
        }

        private void setupPlaceholder(JTextComponent component, String placeholder) {
            component.putClientProperty("placeholder", placeholder);
            component.putClientProperty("originalForeground", component.getForeground());

            if (component instanceof JTextField) {
                if (((JTextField)component).getText().isEmpty()) {
                    component.setText(placeholder);
                    component.setForeground(darkGrayColor);
                    component.putClientProperty("showingPlaceholder", true);
                } else {
                    component.putClientProperty("showingPlaceholder", false);
                    component.setForeground((Color) component.getClientProperty("originalForeground"));
                }
            } else if (component instanceof JTextArea) {
                if (((JTextArea)component).getText().isEmpty()) {
                    component.setText(placeholder);
                    component.setForeground(darkGrayColor);
                    component.putClientProperty("showingPlaceholder", true);
                } else {
                    component.putClientProperty("showingPlaceholder", false);
                    component.setForeground((Color) component.getClientProperty("originalForeground"));
                }
            }

            component.addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent e) {
                    if (Boolean.TRUE.equals(component.getClientProperty("showingPlaceholder"))) {
                        component.setText("");
                        component.setForeground((Color) component.getClientProperty("originalForeground"));
                        component.putClientProperty("showingPlaceholder", false);
                    }
                }

                @Override
                public void focusLost(FocusEvent e) {
                    if (component.getText().isEmpty()) {
                        component.setText(placeholder);
                        component.setForeground(darkGrayColor);
                        component.putClientProperty("showingPlaceholder", true);
                    }
                }
            });
        }

        // NEW: Metode untuk mengambil provinsi dari tbl_kodepos
        private String[] getProvincesFromDB() {
            List<String> provinces = new ArrayList<>();
            Connection conn = null;
            PreparedStatement pstmt = null;
            ResultSet rs = null;
            try {
                conn = DatabaseConnection.getConnection();
                String query = "SELECT DISTINCT provinsi FROM tbl_kodepos ORDER BY provinsi";
                pstmt = conn.prepareStatement(query);
                rs = pstmt.executeQuery();
                while (rs.next()) {
                    provinces.add(rs.getString("provinsi"));
                }
            } catch (SQLException e) {
                System.err.println("Error memuat provinsi dari tbl_kodepos: " + e.getMessage()); // Translated
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error memuat provinsi dari kodepos: " + e.getMessage(), "Error Database", JOptionPane.ERROR_MESSAGE);
            } finally {
                DatabaseConnection.closeConnection(conn, pstmt, rs);
            }
            return provinces.toArray(new String[0]);
        }

        // NEW: Metode untuk mengambil kota/kabupaten dari tbl_kodepos
        private void populateCitiesInDialog(JComboBox<String> cmbCity, String selectedProvince) {
            cmbCity.removeAllItems();
            cmbCity.addItem("Pilih Kota/Kabupaten"); // Default placeholder
            if (selectedProvince == null || selectedProvince.isEmpty() || selectedProvince.equals("Pilih Provinsi")) return;

            Connection conn = null;
            PreparedStatement pstmt = null;
            ResultSet rs = null;
            try {
                conn = DatabaseConnection.getConnection();
                String query = "SELECT DISTINCT kabupaten FROM tbl_kodepos WHERE provinsi = ? ORDER BY kabupaten";
                pstmt = conn.prepareStatement(query);
                pstmt.setString(1, selectedProvince);
                rs = pstmt.executeQuery();
                while (rs.next()) {
                    cmbCity.addItem(rs.getString("kabupaten"));
                }
            } catch (SQLException e) {
                System.err.println("Error memuat kota/kabupaten dari tbl_kodepos: " + e.getMessage()); // Translated
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error memuat kota/kabupaten dari kodepos: " + e.getMessage(), "Error Database", JOptionPane.ERROR_MESSAGE);
            } finally {
                DatabaseConnection.closeConnection(conn, pstmt, rs);
            }
        }

        // NEW METHOD: populateKecamatansInDialog
        private void populateKecamatansInDialog(JComboBox<String> cmbKecamatan, String selectedProvince, String selectedCity) {
            cmbKecamatan.removeAllItems();
            cmbKecamatan.addItem("Pilih Kecamatan"); // Default placeholder
            if (selectedProvince == null || selectedProvince.equals("Pilih Provinsi") || 
                selectedCity == null || selectedCity.equals("Pilih Kota/Kabupaten")) return;

            Connection conn = null;
            PreparedStatement pstmt = null;
            ResultSet rs = null;
            try {
                conn = DatabaseConnection.getConnection();
                String query = "SELECT DISTINCT kecamatan FROM tbl_kodepos WHERE provinsi = ? AND kabupaten = ? ORDER BY kecamatan";
                pstmt = conn.prepareStatement(query);
                pstmt.setString(1, selectedProvince);
                pstmt.setString(2, selectedCity);
                rs = pstmt.executeQuery();
                while (rs.next()) {
                    cmbKecamatan.addItem(rs.getString("kecamatan"));
                }
            } catch (SQLException e) {
                System.err.println("Error memuat kecamatan dari tbl_kodepos: " + e.getMessage()); // Translated
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error memuat kecamatan dari kodepos: " + e.getMessage(), "Error Database", JOptionPane.ERROR_MESSAGE);
            } finally {
                DatabaseConnection.closeConnection(conn, pstmt, rs);
            }
        }

        // NEW METHOD: populateKelurahansInDialog
        private void populateKelurahansInDialog(JComboBox<String> cmbKelurahan, String selectedProvince, String selectedCity, String selectedKecamatan) {
            cmbKelurahan.removeAllItems();
            cmbKelurahan.addItem("Pilih Kelurahan"); // Default placeholder
            if (selectedProvince == null || selectedProvince.equals("Pilih Provinsi") || 
                selectedCity == null || selectedCity.equals("Pilih Kota/Kabupaten") ||
                selectedKecamatan == null || selectedKecamatan.equals("Pilih Kecamatan")) return;

            Connection conn = null;
            PreparedStatement pstmt = null;
            ResultSet rs = null;
            try {
                conn = DatabaseConnection.getConnection();
                String query = "SELECT DISTINCT kelurahan FROM tbl_kodepos WHERE provinsi = ? AND kabupaten = ? AND kecamatan = ? ORDER BY kelurahan";
                pstmt = conn.prepareStatement(query);
                pstmt.setString(1, selectedProvince);
                pstmt.setString(2, selectedCity);
                pstmt.setString(3, selectedKecamatan);
                rs = pstmt.executeQuery();
                while (rs.next()) {
                    cmbKelurahan.addItem(rs.getString("kelurahan"));
                }
            } catch (SQLException e) {
                System.err.println("Error memuat kelurahan dari tbl_kodepos: " + e.getMessage()); // Translated
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error memuat kelurahan dari kodepos: " + e.getMessage(), "Error Database", JOptionPane.ERROR_MESSAGE);
            } finally {
                DatabaseConnection.closeConnection(conn, pstmt, rs);
            }
        }

        // MODIFIED METHOD: populatePostalCodesInDialog to include kecamatan and kelurahan
        private void populatePostalCodesInDialog(JComboBox<String> cmbPostalCode, String selectedProvince, String selectedCity, String selectedKecamatan, String selectedKelurahan) {
            cmbPostalCode.removeAllItems();
            cmbPostalCode.addItem("Pilih Kode Pos"); // Default placeholder
            if (selectedProvince == null || selectedProvince.equals("Pilih Provinsi") || 
                selectedCity == null || selectedCity.equals("Pilih Kota/Kabupaten") || 
                selectedKecamatan == null || selectedKecamatan.equals("Pilih Kecamatan") || 
                selectedKelurahan == null || selectedKelurahan.equals("Pilih Kelurahan")) return;

            Connection conn = null;
            PreparedStatement pstmt = null;
            ResultSet rs = null;
            try {
                conn = DatabaseConnection.getConnection();
                String query = "SELECT DISTINCT kodepos FROM tbl_kodepos WHERE provinsi = ? AND kabupaten = ? AND kecamatan = ? AND kelurahan = ? ORDER BY kodepos";
                pstmt = conn.prepareStatement(query);
                pstmt.setString(1, selectedProvince);
                pstmt.setString(2, selectedCity);
                pstmt.setString(3, selectedKecamatan);
                pstmt.setString(4, selectedKelurahan);
                rs = pstmt.executeQuery();
                while (rs.next()) {
                    cmbPostalCode.addItem(rs.getString("kodepos"));
                }
            } catch (SQLException e) {
                System.err.println("Error memuat kode pos dari tbl_kodepos: " + e.getMessage()); // Translated
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Error memuat kode pos dari kodepos: " + e.getMessage(), "Error Database", JOptionPane.ERROR_MESSAGE);
            } finally {
                DatabaseConnection.closeConnection(conn, pstmt, rs);
            }
        }

        private List<Address> getAddressesFromDatabase(int userId) {
            List<Address> addresses = new ArrayList<>();
            Connection conn = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try {
                conn = DatabaseConnection.getConnection();
                String query = "SELECT id, user_id, label, full_address, city, province, postal_code, country, kecamatan, kelurahan FROM addresses WHERE user_id = ?"; // Modified query
                stmt = conn.prepareStatement(query);
                stmt.setInt(1, userId);
                rs = stmt.executeQuery();
                while (rs.next()) {
                    addresses.add(new Address(
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("label"),
                        rs.getString("full_address"),
                        rs.getString("city"),
                        rs.getString("province"),
                        rs.getString("postal_code"),
                        rs.getString("country"),
                        rs.getString("kecamatan"), // NEW
                        rs.getString("kelurahan")  // NEW
                    ));
                }
            } catch (SQLException e) {
                System.err.println("Error memuat alamat: " + e.getMessage()); // Translated
                e.printStackTrace();
            } finally {
                DatabaseConnection.closeConnection(conn, stmt, rs);
            }
            return addresses;
        }

        private void addAddressToDatabase(Address address) {
            Connection conn = null;
            PreparedStatement stmt = null;
            try {
                conn = DatabaseConnection.getConnection();
                String query = "INSERT INTO addresses (user_id, label, full_address, city, province, postal_code, country, kecamatan, kelurahan) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"; // Modified query
                stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
                stmt.setInt(1, address.getUserId());
                stmt.setString(2, address.getLabel());
                stmt.setString(3, address.getFullAddress());
                stmt.setString(4, address.getCity());
                stmt.setString(5, address.getProvince());
                stmt.setString(6, address.getPostalCode());
                stmt.setString(7, address.getCountry());
                stmt.setString(8, address.getKecamatan()); // NEW
                stmt.setString(9, address.getKelurahan());  // NEW
                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            address.setId(generatedKeys.getInt(1)); 
                        }
                    }
                    JOptionPane.showMessageDialog(this, "Alamat berhasil ditambahkan!", "Sukses", JOptionPane.INFORMATION_MESSAGE); // Translated
                } else {
                    JOptionPane.showMessageDialog(this, "Gagal menambahkan alamat.", "Error", JOptionPane.ERROR_MESSAGE); // Translated
                }
            } catch (SQLException e) {
                System.err.println("Error menambahkan alamat: " + e.getMessage()); // Translated
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error database: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); // Translated
            } finally {
                DatabaseConnection.closeConnection(conn, stmt, null);
            }
        }

        private void updateAddressInDatabase(Address address) {
            Connection conn = null;
            PreparedStatement stmt = null;
            try {
                conn = DatabaseConnection.getConnection();
                String query = "UPDATE addresses SET label = ?, full_address = ?, city = ?, province = ?, postal_code = ?, country = ?, kecamatan = ?, kelurahan = ? WHERE id = ? AND user_id = ?"; // Modified query
                stmt = conn.prepareStatement(query);
                stmt.setString(1, address.getLabel());
                stmt.setString(2, address.getFullAddress());
                stmt.setString(3, address.getCity());
                stmt.setString(4, address.getProvince());
                stmt.setString(5, address.getPostalCode());
                stmt.setString(6, address.getCountry());
                stmt.setString(7, address.getKecamatan()); // NEW
                stmt.setString(8, address.getKelurahan());  // NEW
                stmt.setInt(9, address.getId());
                stmt.setInt(10, address.getUserId());
                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    JOptionPane.showMessageDialog(this, "Alamat berhasil diperbarui!", "Sukses", JOptionPane.INFORMATION_MESSAGE); // Translated
                } else {
                    JOptionPane.showMessageDialog(this, "Gagal memperbarui alamat. Alamat tidak ditemukan atau tidak ada perubahan.", "Error", JOptionPane.ERROR_MESSAGE); // Translated
                }
            } catch (SQLException e) {
                System.err.println("Error memperbarui alamat: " + e.getMessage()); // Translated
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error database: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); // Translated
            } finally {
                DatabaseConnection.closeConnection(conn, stmt, null);
            }
        }

        private void deleteAddress(int addressId) {
            int confirm = JOptionPane.showConfirmDialog(this, "Apakah Anda yakin ingin menghapus alamat ini?", "Konfirmasi Hapus", JOptionPane.YES_NO_OPTION); // Translated
            if (confirm == JOptionPane.YES_OPTION) {
                Connection conn = null;
                PreparedStatement stmt = null;
                try {
                    conn = DatabaseConnection.getConnection();
                    String query = "DELETE FROM addresses WHERE id = ? AND user_id = ?";
                    stmt = conn.prepareStatement(query);
                    stmt.setInt(1, addressId);
                    stmt.setInt(2, user.getId());
                    int affectedRows = stmt.executeUpdate();

                    if (affectedRows > 0) {
                        JOptionPane.showMessageDialog(this, "Alamat berhasil dihapus!", "Sukses", JOptionPane.INFORMATION_MESSAGE); // Translated
                        loadAddresses(); 
                    } else {
                        JOptionPane.showMessageDialog(this, "Gagal menghapus alamat. Alamat tidak ditemukan.", "Error", JOptionPane.ERROR_MESSAGE); // Translated
                    }
                } catch (SQLException e) {
                    System.err.println("Error menghapus alamat: " + e.getMessage()); // Translated
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Error database: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); // Translated
                } finally {
                    DatabaseConnection.closeConnection(conn, stmt, null);
                }
            }
        }
    }

    public static class Address {
        private int id;
        private int userId;
        private String label;
        private String fullAddress;
        private String city;
        private String province;
        private String postalCode;
        private String country;
        private String kecamatan; // NEW
        private String kelurahan; // NEW

        public Address(int id, int userId, String label, String fullAddress, String city, String province, String postalCode, String country) {
            this.id = id;
            this.userId = userId;
            this.label = label;
            this.fullAddress = fullAddress;
            this.city = city;
            this.province = province;
            this.postalCode = postalCode;
            this.country = country;
            // Default empty for new fields if old constructor is used
            this.kecamatan = "";
            this.kelurahan = "";
        }

        // NEW CONSTRUCTOR WITH KECAMATAN AND KELURAHAN
        public Address(int id, int userId, String label, String fullAddress, String city, String province, String postalCode, String country, String kecamatan, String kelurahan) {
            this.id = id;
            this.userId = userId;
            this.label = label;
            this.fullAddress = fullAddress;
            this.city = city;
            this.province = province;
            this.postalCode = postalCode;
            this.country = country;
            this.kecamatan = kecamatan;
            this.kelurahan = kelurahan;
        }

        // Getters
        public int getId() { return id; }
        public int getUserId() { return userId; }
        public String getLabel() { return label; }
        public String getFullAddress() { return fullAddress; }
        public String getCity() { return city; }
        public String getProvince() { return province; }
        public String getPostalCode() { return postalCode; }
        public String getCountry() { return country; }
        public String getKecamatan() { return kecamatan; } // NEW
        public String getKelurahan() { return kelurahan; } // NEW

        // Setters (for updating)
        public void setId(int id) { this.id = id; }
        public void setLabel(String label) { this.label = label; }
        public void setFullAddress(String fullAddress) { this.fullAddress = fullAddress; }
        public void setCity(String city) { this.city = city; }
        public void setProvince(String province) { this.province = province; }
        public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
        public void setCountry(String country) { this.country = country; }
        public void setKecamatan(String kecamatan) { this.kecamatan = kecamatan; } // NEW
        public void setKelurahan(String kelurahan) { this.kelurahan = kelurahan; } // NEW
    }
}