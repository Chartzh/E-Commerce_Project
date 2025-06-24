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
            JOptionPane.showMessageDialog(null, "No user logged in!", "Error", JOptionPane.ERROR_MESSAGE);
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

        JLabel titleLabel = new JLabel("Account Settings");
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
        contentPanel.add(createPlaceholderPanel("Payment"), "Payment");
        contentPanel.add(createPlaceholderPanel("Security"), "Security");
        contentPanel.add(createPlaceholderPanel("Legal Agreement"), "Legal Agreement"); // Renamed to singular

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
        // Mengurangi padding atas karena profileInfoPanel sudah dihapus
        navPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0)); // Padding atas disesuaikan

        // --- START PERUBAHAN: Menghilangkan profileInfoPanel (foto profil dan nama) dari sidebar ---
        // Kode berikut dihapus/dikomentari:
        
        JPanel profileInfoPanel = new JPanel(new GridBagLayout());
        profileInfoPanel.setBackground(Color.WHITE);
        profileInfoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        profileInfoPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        profileInfoPanel.setBorder(BorderFactory.createEmptyBorder(0, 25, 20, 15));

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
        loadProfilePictureForNav(navProfileImageLabel);

        profileInfoPanel.add(navProfileImageLabel, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        JLabel navUsernameLabel = new JLabel(currentUser.getUsername());
        navUsernameLabel.setFont(new Font("Arial", Font.BOLD, 16));
        navUsernameLabel.setForeground(textColor);
        profileInfoPanel.add(navUsernameLabel, gbc);

        navPanel.add(profileInfoPanel);
        navPanel.add(Box.createVerticalStrut(10)); // Space between profile info and buttons
       
        // --- END PERUBAHAN: Menghilangkan profileInfoPanel ---


        // NEW: Add "Addresses" to navigation
        String[] navItems = {"Profile", "Addresses", "Payment", "Security", "Legal Agreement"};
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

            final String panelName = navItems[i];
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

    // Metode loadProfilePictureForNav tidak lagi diperlukan karena foto profil di sidebar sudah dihapus
    // Jika masih ada kebutuhan untuk memuat foto profil di tempat lain di ProfileUI (selain di ProfilePanel),
    // Anda mungkin perlu membuat versi umum dari metode ini di kelas ProfileUI.
    
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
            System.err.println("Failed to load profile picture for navigation: " + e.getMessage());
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
        JLabel label = new JLabel("<html><center>This is the <b>" + title + "</b> section.<br>Content coming soon!</center></html>");
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
        // REMOVED: txtCity, txtProvince, txtPostalCode, txtCountry, txtAddress
        private JTextField txtFullName, txtPhone, txtEmail, txtNik;
        private JButton btnSave;
        private ImageIcon profileImage; // Menyimpan ImageIcon untuk foto profil
        private ImageIcon bannerImage;  // Menyimpan ImageIcon untuk banner
        private File selectedFile;      // File foto profil yang dipilih
        private File selectedBannerFile; // File banner yang dipilih
        private User user;

        public ProfilePanel(User user) {
            this.user = user;
            setLayout(new BorderLayout());
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createEmptyBorder(0, 40, 30, 40));

            // --- Kontainer Utama untuk Semua Konten Scrollable ---
            JPanel scrollableContent = new JPanel();
            scrollableContent.setLayout(new BoxLayout(scrollableContent, BoxLayout.Y_AXIS));
            scrollableContent.setBackground(Color.WHITE);

            // --- Container for Banner and Profile Photo (Top Section) ---
            JPanel topSectionContainer = new JPanel(new BorderLayout());
            topSectionContainer.setBackground(Color.WHITE);
            topSectionContainer.setAlignmentX(Component.CENTER_ALIGNMENT);
            // setMaximumSize untuk topSectionContainer tetap memungkinkan pelebaran horizontal
            topSectionContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 250));


            JLayeredPane layeredPane = new JLayeredPane();
            // --- START PERUBAHAN: Atur ukuran layeredPane agar melebar penuh ---
            int layeredPaneHeight = 180 + (100 / 2) + 15 + 20; 
            layeredPane.setPreferredSize(new Dimension(1, layeredPaneHeight)); // Lebar minimum 1, tinggi tetap
            layeredPane.setMinimumSize(new Dimension(1, layeredPaneHeight));   // Lebar minimum 1, tinggi tetap
            layeredPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, layeredPaneHeight)); // Lebar maksimum tak terbatas, tinggi tetap
            // --- END PERUBAHAN ---

            // 1. Banner Image (Layer 0, paling bawah)
            JPanel bannerPanel = new JPanel(new BorderLayout());
            bannerPanel.setBackground(new Color(150, 180, 200)); // Default banner color (simulating cloudy blue)
            // bannerPanel.setBounds akan diatur dinamis oleh ComponentListener
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
            profileImageContainer.setBorder(new LineBorder(Color.WHITE, 3, true)); // White border around profile pic
            // profileImageContainer.setBounds akan diatur dinamis oleh ComponentListener
            
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
            JLabel changeProfilePhotoLabel = new JLabel("Change Profile Photo");
            changeProfilePhotoLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            changeProfilePhotoLabel.setForeground(primaryColor.darker());
            changeProfilePhotoLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            changeProfilePhotoLabel.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setDialogTitle("Select Profile Picture");
                    fileChooser.setFileFilter(new FileNameExtensionFilter("Image files", "jpg", "jpeg", "png", "gif"));
                    if (fileChooser.showOpenDialog(ProfilePanel.this) == JFileChooser.APPROVE_OPTION) {
                        selectedFile = fileChooser.getSelectedFile();
                        try (FileInputStream fis = new FileInputStream(selectedFile)) {
                            byte[] imageData = Files.readAllBytes(selectedFile.toPath());
                            ImageIcon originalIcon = new ImageIcon(imageData);
                            // Skala gambar sesuai ukuran profileImageContainer
                            Image image = originalIcon.getImage().getScaledInstance(
                                profileImageContainer.getWidth(), profileImageContainer.getHeight(), Image.SCALE_SMOOTH);
                            profileImage = new ImageIcon(image);
                            profileImageLabel.setIcon(profileImage);
                            user.setProfilePicture(imageData); // Update user object
                            profileImageLabel.repaint();
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(ProfilePanel.this,
                                    "Error loading image: " + ex.getMessage(),
                                    "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
                public void mouseEntered(MouseEvent e) { changeProfilePhotoLabel.setText("<html><u>Change Profile Photo</u></html>"); }
                public void mouseExited(MouseEvent e) { changeProfilePhotoLabel.setText("Change Profile Photo"); }
            });
            // changeProfilePhotoLabel.setBounds akan diatur dinamis oleh ComponentListener
            layeredPane.add(changeProfilePhotoLabel, JLayeredPane.PALETTE_LAYER);


            // --- START PERUBAHAN: ComponentListener untuk mengatur ulang posisi komponen di layeredPane ---
            layeredPane.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    int layeredPaneWidth = layeredPane.getWidth();
                    
                    // Atur ulang bounds untuk bannerPanel
                    bannerPanel.setBounds(0, 0, layeredPaneWidth, 180);
                    // Panggil ulang loadBannerPicture dengan ukuran baru
                    loadBannerPicture(bannerPanel.getWidth(), bannerPanel.getHeight());

                    // Atur ulang bounds untuk profileImageContainer
                    int profileX = (layeredPaneWidth - 100) / 2; // Tengah horizontal
                    int profileY = 180 - (100 / 2); // Posisi Y: Tinggi banner (180) - setengah tinggi foto profil (100/2)
                    profileImageContainer.setBounds(profileX, profileY, 100, 100);
                    // Panggil ulang loadProfilePicture dengan ukuran baru
                    loadProfilePicture(profileImageContainer.getWidth(), profileImageContainer.getHeight());

                    // Atur ulang bounds untuk changeProfilePhotoLabel
                    int labelY = profileY + 100 + 5; // Di bawah foto profil
                    changeProfilePhotoLabel.setBounds(profileX - 20, labelY, 140, 20); // Lebar 140 agar teks terlihat penuh
                    
                    layeredPane.revalidate();
                    layeredPane.repaint();
                }
            });
            // --- END PERUBAHAN ---


            topSectionContainer.add(layeredPane, BorderLayout.CENTER);
            scrollableContent.add(topSectionContainer);

            scrollableContent.add(Box.createVerticalStrut(20));

            // --- Input Fields ---
            // Full Name field (sesuai Nama Lengkap di gambar) - This belongs to users table
            txtFullName = new JTextField();
            scrollableContent.add(createFormField("Nama Lengkap", user.getUsername(), "Budi Sabudi", txtFullName)); 

            // Email field (sesuai Alamat Email di gambar) - This belongs to users table
            txtEmail = new JTextField();
            scrollableContent.add(createFormField("Alamat Email", user.getEmail(), "budi.sabudi@example.com", txtEmail));

            // NIK field (NEW - for profile table)
            txtNik = new JTextField();
            scrollableContent.add(createFormField("NIK", user.getNik(), "1234567890123456", txtNik));

            // Phone number (sesuai Nomor Telepon di gambar) - This belongs to profile table
            txtPhone = new JTextField();
            String phoneNumber = user.getPhone();
            if (phoneNumber != null && phoneNumber.startsWith("+")) {
                phoneNumber = phoneNumber.substring(phoneNumber.indexOf(" ") + 1);
            }
            scrollableContent.add(createFormField("Nomor Telepon", phoneNumber, "08123456789", txtPhone));
            
            // REMOVED: Grid for City, Province, Postal Code, Country
            // REMOVED: Address field

            // Save button
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.setBackground(Color.WHITE);

            btnSave = new JButton("Save Changes"); // More descriptive text
            btnSave.setFont(new Font("Arial", Font.BOLD, 15));
            btnSave.setForeground(Color.WHITE);
            btnSave.setBackground(primaryColor);
            btnSave.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(primaryColor, 1, true), // Border for consistency
                    BorderFactory.createEmptyBorder(10, 25, 10, 25) // More padding
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

            // load images after components are fully initialized and layout is stable
            SwingUtilities.invokeLater(() -> {
                // Panggil ComponentListener secara manual agar bounds teratur saat pertama kali
                // Ini akan memicu pemanggilan loadBannerPicture dan loadProfilePicture dengan ukuran yang tepat
                layeredPane.getComponentListeners()[0].componentResized(
                    new ComponentEvent(layeredPane, ComponentEvent.COMPONENT_RESIZED));
                
                // Placeholder setup di sini sudah tepat
                setupPlaceholder(txtFullName, "Budi Sabudi");
                setupPlaceholder(txtEmail, "budi.sabudi@example.com");
                setupPlaceholder(txtNik, "1234567890123456");
                setupPlaceholder(txtPhone, "08123456789");
            });
        }

        // Helper method to create a consistent form field with placeholder
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
            textField.setBorder(new LineBorder(lightGrayColor, 1) { // Softer border, rounded corners
                @Override
                public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(getLineColor());
                    g2.drawRoundRect(x, y, width - 1, height - 1, 8, 8);
                    g2.dispose();
                }
            });
            // Apply placeholder to JTextField
            setupPlaceholder(textField, placeholder);

            panel.add(textField, BorderLayout.CENTER);
            return panel;
        }

        // Helper method for JTextArea with placeholder - REMOVED from ProfilePanel, kept for AddressPanel
        // private JPanel createTextAreaField(String labelText, String initialValue, String placeholder, JTextArea textArea) {
        //     JPanel panel = new JPanel(new BorderLayout(0, 5));
        //     panel.setBackground(Color.WHITE);
        //     panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        //     JLabel lbl = new JLabel(labelText);
        //     lbl.setFont(new Font("Arial", Font.PLAIN, 13));
        //     lbl.setForeground(darkGrayColor);
        //     panel.add(lbl, BorderLayout.NORTH);

        //     textArea.setText(initialValue);
        //     textArea.setFont(new Font("Arial", Font.PLAIN, 15));
        //     textArea.setLineWrap(true);
        //     textArea.setWrapStyleWord(true);
        //     textArea.setBorder(new LineBorder(lightGrayColor, 1) { // Softer border, rounded corners
        //         @Override
        //         public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        //             Graphics2D g2 = (Graphics2D) g.create();
        //             g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        //             g2.setColor(getLineColor());
        //             g2.drawRoundRect(x, y, width - 1, height - 1, 8, 8);
        //             g2.dispose();
        //         }
        //     });
        //     JScrollPane scroll = new JScrollPane(textArea);
        //     scroll.setBorder(BorderFactory.createEmptyBorder());
        //     scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        //     scroll.setPreferredSize(new Dimension(scroll.getPreferredSize().width, 80));

        //     // Apply placeholder to JTextArea
        //     setupPlaceholder(textArea, placeholder);

        //     panel.add(scroll, BorderLayout.CENTER);
        //     return panel;
        // }

        // --- Placeholder Logic ---
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

        // Method to load banner picture from database, now accepts targetWidth and targetHeight
        private void loadBannerPicture(int targetWidth, int targetHeight) {
            // Jika data gambar sudah ada di objek user, cukup skala ulang ImageIcon yang ada
            if (user.getBannerPicture() != null && bannerImage != null) {
                Image image = bannerImage.getImage().getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
                bannerImageLabel.setIcon(new ImageIcon(image));
                return; // Tidak perlu query DB lagi
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
                        bannerImage = new ImageIcon(image); // Simpan ImageIcon yang baru dibuat
                        bannerImageLabel.setIcon(bannerImage);
                        user.setBannerPicture(imgData); // Simpan juga ke objek user
                    } else {
                        bannerImageLabel.setIcon(null);
                        bannerImageLabel.setBackground(new Color(150, 180, 200));
                        user.setBannerPicture(null); // Pastikan null di objek user
                    }
                }
            }
            catch (SQLException e) {
                System.err.println("Database error loading banner picture: " + e.getMessage());
                e.printStackTrace();
                bannerImageLabel.setIcon(null);
                bannerImageLabel.setBackground(new Color(150, 180, 200));
            } catch (Exception e) {
                System.err.println("Failed to load banner picture: " + e.getMessage());
                e.printStackTrace();
                bannerImageLabel.setIcon(null);
                bannerImageLabel.setBackground(new Color(150, 180, 200));
            } finally {
                DatabaseConnection.closeConnection(conn, stmt, rs);
            }
        }

        // Method to load profile picture from database, now accepts targetWidth and targetHeight
        private void loadProfilePicture(int targetWidth, int targetHeight) {
            // Jika data gambar sudah ada di objek user, cukup skala ulang ImageIcon yang ada
            if (user.getProfilePicture() != null && profileImage != null) {
                Image image = profileImage.getImage().getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
                profileImageLabel.setIcon(new ImageIcon(image));
                return; // Tidak perlu query DB lagi
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
                        profileImage = new ImageIcon(image); // Simpan ImageIcon yang baru dibuat
                        profileImageLabel.setIcon(profileImage);
                        user.setProfilePicture(imgData); // Simpan juga ke objek user
                    } else {
                        profileImageLabel.setIcon(null);
                        profileImageLabel.setBackground(lightGrayColor);
                        profileImageLabel.repaint();
                        user.setProfilePicture(null); // Pastikan null di objek user
                    }
                }
            }
            catch (SQLException e) {
                System.err.println("Database error loading profile picture: " + e.getMessage());
                e.printStackTrace();
                profileImageLabel.setIcon(null);
                profileImageLabel.setBackground(lightGrayColor);
            } catch (Exception e) {
                System.err.println("Failed to load profile picture: " + e.getMessage());
                e.printStackTrace();
                profileImageLabel.setIcon(null);
                profileImageLabel.setBackground(lightGrayColor);
            } finally {
                DatabaseConnection.closeConnection(conn, stmt, rs);
            }
        }
        
        private void updateProfile() {
            // Mengambil teks dari JTextField dan JTextArea, memastikan tidak ada placeholder yang tersimpan
            String fullName = (Boolean.TRUE.equals(txtFullName.getClientProperty("showingPlaceholder"))) ? "" : txtFullName.getText().trim();
            String email = (Boolean.TRUE.equals(txtEmail.getClientProperty("showingPlaceholder"))) ? "" : txtEmail.getText().trim();
            String nik = (Boolean.TRUE.equals(txtNik.getClientProperty("showingPlaceholder"))) ? "" : txtNik.getText().trim();
            String phone = (Boolean.TRUE.equals(txtPhone.getClientProperty("showingPlaceholder"))) ? "" : txtPhone.getText().trim();
            // REMOVED: city, province, postalCode, country, address

            // Validasi input
            if (fullName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nama Lengkap tidak boleh kosong.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (email.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Alamat Email tidak boleh kosong.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Validasi format email
            if (!email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$")) {
                JOptionPane.showMessageDialog(this, "Format email tidak valid!", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Validasi nomor telepon (misalnya, hanya angka, 10-13 digit)
            if (!phone.matches("\\d{10,13}")) {
                JOptionPane.showMessageDialog(this, "Nomor telepon harus 10-13 digit angka!", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Validasi NIK (misalnya, 16 digit angka)
            if (!nik.matches("\\d{16}")) {
                JOptionPane.showMessageDialog(this, "NIK harus 16 digit angka!", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }


            Connection conn = null;
            PreparedStatement stmtUsers = null;
            PreparedStatement stmtProfile = null;

            try {
                conn = DatabaseConnection.getConnection();
                conn.setAutoCommit(false); // Start transaction

                boolean usersUpdated = false;
                boolean profileUpdated = false;

                // 1. Update users table (username, email)
                String updateUserSql = "UPDATE users SET username = ?, email = ? WHERE id = ?";
                stmtUsers = conn.prepareStatement(updateUserSql);
                stmtUsers.setString(1, fullName);
                stmtUsers.setString(2, email);
                stmtUsers.setInt(3, user.getId());
                int resultUsers = stmtUsers.executeUpdate();
                if (resultUsers > 0) {
                    usersUpdated = true;
                    // Update current user object for users table fields
                    user.setUsername(fullName);
                    user.setEmail(email);
                }

                // 2. Update profile table (nik, phone, pictures)
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
                // REMOVED: city, province, postalCode, country, address parameters

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
                stmtProfile.setInt(paramIndex++, user.getId()); // WHERE user_id = ?

                int resultProfile = stmtProfile.executeUpdate();
                if (resultProfile > 0) {
                    profileUpdated = true;
                    // Update current user object for profile table fields
                    user.setNik(nik);
                    user.setPhone(phone);
                    // REMOVED: city, province, postalCode, country, address updates

                    if (updateProfilePic) {
                        user.setProfilePicture(Files.readAllBytes(selectedFile.toPath()));
                    }
                    if (updateBannerPic) {
                        user.setBannerPicture(Files.readAllBytes(selectedBannerFile.toPath()));
                    }
                }

                // Commit transaction if at least one update was successful or both executed without error
                if (usersUpdated || profileUpdated) {
                    conn.commit();
                    JOptionPane.showMessageDialog(this, "Profil berhasil diperbarui!", "Sukses", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    conn.rollback(); // Rollback if no rows were affected (shouldn't happen with proper logic)
                    JOptionPane.showMessageDialog(this, "Gagal memperbarui profil. Tidak ada perubahan yang terdeteksi atau ID tidak ditemukan.", "Error", JOptionPane.ERROR_MESSAGE);
                }

            } catch (SQLException | IOException e) {
                try {
                    if (conn != null) conn.rollback(); // Rollback on error
                } catch (SQLException ex) {
                    System.err.println("Rollback failed: " + ex.getMessage());
                }
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Terjadi kesalahan saat memperbarui profil: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            } finally {
                // Close statements and connection
                try {
                    if (stmtUsers != null) stmtUsers.close();
                    if (stmtProfile != null) stmtProfile.close();
                    if (conn != null) conn.setAutoCommit(true); // Restore auto-commit
                } catch (SQLException e) {
                    System.err.println("Error closing resources: " + e.getMessage());
                }
                DatabaseConnection.closeConnection(conn, null, null); // Manual close as statements handled above
            }
        }
    }

    // NEW: Address Panel class
    private class AddressPanel extends JPanel {
        private User user;
        private JPanel addressListPanel;
        private JScrollPane scrollPane;

        // Colors from parent
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

            JLabel title = new JLabel("Your Addresses");
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

            JButton addAddressButton = new JButton("Add New Address");
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
                JLabel noAddressLabel = new JLabel("No addresses found. Click 'Add New Address' to add one.");
                noAddressLabel.setFont(new Font("Arial", Font.ITALIC, 14));
                noAddressLabel.setForeground(darkGrayColor);
                addressListPanel.add(noAddressLabel);
            } else {
                for (Address address : addresses) {
                    addressListPanel.add(createAddressCard(address));
                    addressListPanel.add(Box.createVerticalStrut(10)); // Space between cards
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
            addressDetails.setText(address.getFullAddress());
            card.add(addressDetails, BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
            buttonPanel.setBackground(new Color(250, 250, 250));

            JButton editButton = new JButton("Edit");
            styleMiniButton(editButton, primaryColor);
            editButton.addActionListener(e -> showEditAddressDialog(address));
            buttonPanel.add(editButton);

            JButton deleteButton = new JButton("Delete");
            styleMiniButton(deleteButton, new Color(220, 53, 69)); // Red for delete
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
            JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Add New Address", true);
            dialog.setLayout(new BorderLayout());
            dialog.setBackground(Color.WHITE);
            dialog.setSize(500, 600);
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

            JComboBox<String> cmbProvince = new JComboBox<>(getProvinces());
            JComboBox<String> cmbCity = new JComboBox<>();
            JComboBox<String> cmbPostalCode = new JComboBox<>();
            JComboBox<String> cmbCountry = new JComboBox<>(getCountries());

            cmbProvince.addActionListener(e -> {
                String selectedProvince = (String) cmbProvince.getSelectedItem();
                if (selectedProvince != null && !selectedProvince.isEmpty()) {
                    populateCities(cmbCity, selectedProvince);
                } else {
                    cmbCity.removeAllItems();
                }
                cmbPostalCode.removeAllItems(); // Clear postal codes when province changes
            });

            cmbCity.addActionListener(e -> {
                String selectedCity = (String) cmbCity.getSelectedItem();
                if (selectedCity != null && !selectedCity.isEmpty()) {
                    populatePostalCodes(cmbPostalCode, (String) cmbProvince.getSelectedItem(), selectedCity);
                } else {
                    cmbPostalCode.removeAllItems();
                }
            });

            // Initial population
            if (getProvinces().length > 0) {
                cmbProvince.setSelectedIndex(0); // Select first province to trigger city loading
            }


            int row = 0;
            gbc.gridx = 0; gbc.gridy = row++;
            formPanel.add(new JLabel("Address Label (e.g., Home, Office):"), gbc);
            gbc.gridy = row++;
            formPanel.add(txtLabel, gbc);

            gbc.gridy = row++;
            formPanel.add(new JLabel("Full Address:"), gbc);
            gbc.gridy = row++;
            formPanel.add(spAddress, gbc);

            gbc.gridy = row++;
            formPanel.add(new JLabel("Province:"), gbc);
            gbc.gridy = row++;
            formPanel.add(cmbProvince, gbc);

            gbc.gridy = row++;
            formPanel.add(new JLabel("City/Kabupaten:"), gbc);
            gbc.gridy = row++;
            formPanel.add(cmbCity, gbc);

            gbc.gridy = row++;
            formPanel.add(new JLabel("Postal Code:"), gbc);
            gbc.gridy = row++;
            formPanel.add(cmbPostalCode, gbc);

            gbc.gridy = row++;
            formPanel.add(new JLabel("Country:"), gbc);
            gbc.gridy = row++;
            formPanel.add(cmbCountry, gbc);
            
            // Set up placeholders for text fields in dialog
            setupPlaceholder(txtLabel, "e.g., Home, Office");
            setupPlaceholder(txtFullAddress, "Street, Building, etc.");


            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            buttonPanel.setBackground(Color.WHITE);
            JButton saveButton = new JButton("Save Address");
            styleMiniButton(saveButton, primaryColor);
            saveButton.addActionListener(e -> {
                String label = getTextFieldValue(txtLabel);
                String fullAddress = getTextAreaValue(txtFullAddress);
                String province = (String) cmbProvince.getSelectedItem();
                String city = (String) cmbCity.getSelectedItem();
                String postalCode = (String) cmbPostalCode.getSelectedItem();
                String country = (String) cmbCountry.getSelectedItem();

                if (label.isEmpty() || fullAddress.isEmpty() || province == null || province.isEmpty() ||
                    city == null || city.isEmpty() || postalCode == null || postalCode.isEmpty() ||
                    country == null || country.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "All fields must be filled.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                Address newAddress = new Address(
                    -1, user.getId(), label, fullAddress, city, province, postalCode, country
                );
                addAddressToDatabase(newAddress);
                dialog.dispose();
                loadAddresses();
            });
            JButton cancelButton = new JButton("Cancel");
            styleMiniButton(cancelButton, darkGrayColor);
            cancelButton.addActionListener(e -> dialog.dispose());
            buttonPanel.add(saveButton);
            buttonPanel.add(cancelButton);

            dialog.add(new JScrollPane(formPanel), BorderLayout.CENTER);
            dialog.add(buttonPanel, BorderLayout.SOUTH);
            dialog.setVisible(true);
        }

        private void showEditAddressDialog(Address address) {
            JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Edit Address", true);
            dialog.setLayout(new BorderLayout());
            dialog.setBackground(Color.WHITE);
            dialog.setSize(500, 600);
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

            JComboBox<String> cmbProvince = new JComboBox<>(getProvinces());
            JComboBox<String> cmbCity = new JComboBox<>();
            JComboBox<String> cmbPostalCode = new JComboBox<>();
            JComboBox<String> cmbCountry = new JComboBox<>(getCountries());

            // Set initial values for dropdowns
            cmbProvince.setSelectedItem(address.getProvince());
            populateCities(cmbCity, address.getProvince());
            cmbCity.setSelectedItem(address.getCity());
            populatePostalCodes(cmbPostalCode, address.getProvince(), address.getCity());
            cmbPostalCode.setSelectedItem(address.getPostalCode());
            cmbCountry.setSelectedItem(address.getCountry());


            cmbProvince.addActionListener(e -> {
                String selectedProvince = (String) cmbProvince.getSelectedItem();
                if (selectedProvince != null && !selectedProvince.isEmpty()) {
                    populateCities(cmbCity, selectedProvince);
                } else {
                    cmbCity.removeAllItems();
                }
                cmbPostalCode.removeAllItems();
            });

            cmbCity.addActionListener(e -> {
                String selectedCity = (String) cmbCity.getSelectedItem();
                if (selectedCity != null && !selectedCity.isEmpty()) {
                    populatePostalCodes(cmbPostalCode, (String) cmbProvince.getSelectedItem(), selectedCity);
                } else {
                    cmbPostalCode.removeAllItems();
                }
            });

            int row = 0;
            gbc.gridx = 0; gbc.gridy = row++;
            formPanel.add(new JLabel("Address Label (e.g., Home, Office):"), gbc);
            gbc.gridy = row++;
            formPanel.add(txtLabel, gbc);

            gbc.gridy = row++;
            formPanel.add(new JLabel("Full Address:"), gbc);
            gbc.gridy = row++;
            formPanel.add(spAddress, gbc);

            gbc.gridy = row++;
            formPanel.add(new JLabel("Province:"), gbc);
            gbc.gridy = row++;
            formPanel.add(cmbProvince, gbc);

            gbc.gridy = row++;
            formPanel.add(new JLabel("City/Kabupaten:"), gbc);
            gbc.gridy = row++;
            formPanel.add(cmbCity, gbc);

            gbc.gridy = row++;
            formPanel.add(new JLabel("Postal Code:"), gbc);
            gbc.gridy = row++;
            formPanel.add(cmbPostalCode, gbc);

            gbc.gridy = row++;
            formPanel.add(new JLabel("Country:"), gbc);
            gbc.gridy = row++;
            formPanel.add(cmbCountry, gbc);

            // Set up placeholders (even for existing values, if they are empty)
            setupPlaceholder(txtLabel, "e.g., Home, Office");
            setupPlaceholder(txtFullAddress, "Street, Building, etc.");


            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            buttonPanel.setBackground(Color.WHITE);
            JButton saveButton = new JButton("Save Changes");
            styleMiniButton(saveButton, primaryColor);
            saveButton.addActionListener(e -> {
                String label = getTextFieldValue(txtLabel);
                String fullAddress = getTextAreaValue(txtFullAddress);
                String province = (String) cmbProvince.getSelectedItem();
                String city = (String) cmbCity.getSelectedItem();
                String postalCode = (String) cmbPostalCode.getSelectedItem();
                String country = (String) cmbCountry.getSelectedItem();

                if (label.isEmpty() || fullAddress.isEmpty() || province == null || province.isEmpty() ||
                    city == null || city.isEmpty() || postalCode == null || postalCode.isEmpty() ||
                    country == null || country.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "All fields must be filled.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                address.setLabel(label);
                address.setFullAddress(fullAddress);
                address.setCity(city);
                address.setProvince(province);
                address.setPostalCode(postalCode);
                address.setCountry(country);

                updateAddressInDatabase(address);
                dialog.dispose();
                loadAddresses();
            });
            JButton cancelButton = new JButton("Cancel");
            styleMiniButton(cancelButton, darkGrayColor);
            cancelButton.addActionListener(e -> dialog.dispose());
            buttonPanel.add(saveButton);
            buttonPanel.add(cancelButton);

            dialog.add(new JScrollPane(formPanel), BorderLayout.CENTER);
            dialog.add(buttonPanel, BorderLayout.SOUTH);
            dialog.setVisible(true);
        }

        // Helper method to retrieve text from JTextField, handling placeholder
        private String getTextFieldValue(JTextField textField) {
            return (Boolean.TRUE.equals(textField.getClientProperty("showingPlaceholder"))) ? "" : textField.getText().trim();
        }

        // Helper method to retrieve text from JTextArea, handling placeholder
        private String getTextAreaValue(JTextArea textArea) {
            return (Boolean.TRUE.equals(textArea.getClientProperty("showingPlaceholder"))) ? "" : textArea.getText().trim();
        }

        // --- Placeholder Logic (replicated for AddressPanel dialogs) ---
        private void setupPlaceholder(JTextComponent component, String placeholder) {
            component.putClientProperty("placeholder", placeholder);
            component.putClientProperty("originalForeground", component.getForeground());

            // Initial setup: if empty, show placeholder
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


        // --- Database Operations for Address ---

        // Metode ini akan diubah untuk menyediakan lebih banyak data provinsi
        private String[] getProvinces() {
            return new String[]{
                "", // Pilihan default kosong
                "Aceh",
                "Bali",
                "Banten",
                "Bengkulu",
                "Daerah Istimewa Yogyakarta",
                "DKI Jakarta",
                "Gorontalo",
                "Jambi",
                "Jawa Barat",
                "Jawa Tengah",
                "Jawa Timur",
                "Kalimantan Barat",
                "Kalimantan Selatan",
                "Kalimantan Tengah",
                "Kalimantan Timur",
                "Kalimantan Utara",
                "Kepulauan Bangka Belitung",
                "Kepulauan Riau",
                "Lampung",
                "Maluku",
                "Maluku Utara",
                "Nusa Tenggara Barat",
                "Nusa Tenggara Timur",
                "Papua",
                "Papua Barat",
                "Riau",
                "Sulawesi Barat",
                "Sulawesi Selatan",
                "Sulawesi Tengah",
                "Sulawesi Tenggara",
                "Sulawesi Utara",
                "Sumatera Barat",
                "Sumatera Selatan",
                "Sumatera Utara"
            };
        }

        // Metode ini akan diubah untuk menyediakan lebih banyak data kota/kabupaten
        private String[] getCities(String province) {
            switch (province) {
                case "Aceh":
                    return new String[]{"", "Banda Aceh", "Sabang", "Lhokseumawe", "Langsa", "Meulaboh", "Takengon", "Sigli"};
                case "Bali":
                    return new String[]{"", "Denpasar", "Badung", "Gianyar", "Buleleng", "Karangasem", "Jembrana", "Tabanan", "Klungkung", "Bangli"};
                case "Banten":
                    return new String[]{"", "Serang", "Tangerang", "Cilegon", "South Tangerang", "Pandeglang", "Lebak"};
                case "Bengkulu":
                    return new String[]{"", "Bengkulu City", "Rejang Lebong", "North Bengkulu", "South Bengkulu", "Seluma", "Mukomuko"};
                case "Daerah Istimewa Yogyakarta":
                    return new String[]{"", "Yogyakarta", "Sleman", "Bantul", "Kulon Progo", "Gunung Kidul"};
                case "DKI Jakarta":
                    return new String[]{"", "Jakarta Pusat", "Jakarta Barat", "Jakarta Timur", "Jakarta Utara", "Jakarta Selatan", "Kepulauan Seribu"};
                case "Gorontalo":
                    return new String[]{"", "Gorontalo City", "Boalemo", "Bone Bolango", "Gorontalo (Kabupaten)", "Pohuwato"};
                case "Jambi":
                    return new String[]{"", "Jambi City", "Batang Hari", "Muaro Jambi", "Sarolangun", "Tanjung Jabung Barat", "Tebo"};
                case "Jawa Barat":
                    return new String[]{"", "Bandung", "Bekasi", "Bogor", "Depok", "Tasikmalaya", "Cirebon", "Sukabumi", "Garut", "Karawang", "Purwakarta", "Majalengka", "Sumedang", "Indramayu", "Subang", "Cianjur", "Ciamis", "Kuningan"};
                case "Jawa Tengah":
                    return new String[]{"", "Semarang", "Surakarta", "Magelang", "Pekalongan", "Salatiga", "Tegal", "Cilacap", "Purwokerto", "Kudus", "Demak", "Jepara", "Pati", "Rembang", "Blora", "Grobogan", "Klaten", "Boyolali", "Sragen", "Wonogiri", "Kebumen", "Purworejo", "Wonosobo", "Banjarnegara", "Banyumas", "Brebes", "Pemalang", "Batang", "Kendal", "Temanggung"};
                case "Jawa Timur":
                    return new String[]{"", "Surabaya", "Malang", "Kediri", "Madiun", "Pasuruan", "Mojokerto", "Banyuwangi", "Jember", "Blitar", "Bondowoso", "Gresik", "Lamongan", "Lumajang", "Nganjuk", "Ngawi", "Pacitan", "Pamekasan", "Probolinggo", "Sidoarjo", "Situbondo", "Sumenep", "Trenggalek", "Tuban", "Tulungagung"};
                case "Kalimantan Barat":
                    return new String[]{"", "Pontianak", "Singkawang", "Sanggau", "Ketapang", "Sambas", "Mempawah", "Sintang"};
                case "Kalimantan Selatan":
                    return new String[]{"", "Banjarmasin", "Banjarbaru", "Tanah Laut", "Kotabaru", "Barito Kuala", "Tabalong", "Hulu Sungai Selatan"};
                case "Kalimantan Tengah":
                    return new String[]{"", "Palangka Raya", "Kotawaringin Barat", "Kotawaringin Timur", "Kapuas", "Barito Selatan", "Seruyan"};
                case "Kalimantan Timur":
                    return new String[]{"", "Samarinda", "Balikpapan", "Bontang", "Kutai Kartanegara", "Berau", "Paser", "Penajam Paser Utara"};
                case "Kalimantan Utara":
                    return new String[]{"", "Tanjung Selor", "Tarakan", "Nunukan", "Malinau", "Tana Tidung"};
                case "Kepulauan Bangka Belitung":
                    return new String[]{"", "Pangkal Pinang", "Bangka", "Belitung", "Bangka Tengah", "Bangka Selatan"};
                case "Kepulauan Riau":
                    return new String[]{"", "Tanjung Pinang", "Batam", "Bintan", "Karimun", "Natuna"};
                case "Lampung":
                    return new String[]{"", "Bandar Lampung", "Metro", "Lampung Selatan", "Lampung Tengah", "Lampung Utara", "Pringsewu"};
                case "Maluku":
                    return new String[]{"", "Ambon", "Tual", "Central Maluku", "Buru", "Seram Bagian Barat"};
                case "Maluku Utara":
                    return new String[]{"", "Ternate", "Tidore Islands", "Halmahera Barat", "Halmahera Utara", "Morotai"};
                case "Nusa Tenggara Barat":
                    return new String[]{"", "Mataram", "Bima", "Lombok Barat", "Lombok Tengah", "Sumbawa"};
                case "Nusa Tenggara Timur":
                    return new String[]{"", "Kupang", "Flores Timur", "Sumba Barat Daya", "Timor Tengah Selatan", "Belu"};
                case "Papua":
                    return new String[]{"", "Jayapura", "Merauke", "Biak Numfor", "Timika", "Manokwari (Papua)"}; // Manokwari could be here or Papua Barat depending on specific splits
                case "Papua Barat":
                    return new String[]{"", "Manokwari", "Sorong", "Fakfak", "Kaimana", "Raja Ampat"};
                case "Riau":
                    return new String[]{"", "Pekanbaru", "Dumai", "Kampar", "Indragiri Hilir", "Siak", "Pelalawan"};
                case "Sulawesi Barat":
                    return new String[]{"", "Mamuju", "Majene", "Polewali Mandar", "Mamasa", "Pasangkayu"};
                case "Sulawesi Selatan":
                    return new String[]{"", "Makassar", "Parepare", "Palopo", "Gowa", "Bone", "Bulukumba", "Maros", "Pangkep"};
                case "Sulawesi Tengah":
                    return new String[]{"", "Palu", "Donggala", "Poso", "Morowali", "Sigi"};
                case "Sulawesi Tenggara":
                    return new String[]{"", "Kendari", "Bau-Bau", "Muna", "Kolaka", "Konawe"};
                case "Sulawesi Utara":
                    return new String[]{"", "Manado", "Bitung", "Tomohon", "Minahasa", "Kotamobagu", "Bolaang Mongondow"};
                case "Sumatera Barat":
                    return new String[]{"", "Padang", "Bukittinggi", "Payakumbuh", "Agam", "Lima Puluh Kota", "Pasaman", "Pesisir Selatan"};
                case "Sumatera Selatan":
                    return new String[]{"", "Palembang", "Pagar Alam", "Muara Enim", "Lubuklinggau", "Banyuasin", "Ogan Komering Ilir"};
                case "Sumatera Utara":
                    return new String[]{"", "Medan", "Pematangsiantar", "Tebing Tinggi", "Deli Serdang", "Langkat", "Simalungun", "Asahan", "Karo"};
                default:
                    return new String[]{""};
            }
        }

        // Metode ini akan diubah untuk menyediakan lebih banyak data kode pos
        private String[] getPostalCodes(String province, String city) {
            // Ini adalah contoh yang diperluas. Data nyata akan sangat panjang.
            // Sebisa mungkin, kelompokkan berdasarkan kota terlebih dahulu.
            // Contoh struktur:
            // if (province.equals("Jawa Barat")) {
            //    if (city.equals("Bandung")) { return new String[]{"", "40111", "40121", ...}; }
            //    if (city.equals("Bekasi")) { return new String[]{"", "17111", "17121", ...}; }
            // }

            // Jawa Barat
            if ("Jawa Barat".equals(province)) {
                if ("Bandung".equals(city)) return new String[]{"", "40111", "40121", "40131", "40132", "40141", "40151", "40161", "40171", "40172", "40175", "40211", "40212", "40221", "40222", "40231", "40241", "40251", "40252", "40261", "40271", "40272", "40285", "40286"};
                if ("Bekasi".equals(city)) return new String[]{"", "17111", "17112", "17113", "17114", "17115", "17116", "17117", "17118", "17119", "17121", "17122", "17123", "17124", "17125", "17126", "17127", "17128", "17129", "17131", "17132", "17133", "17134", "17135", "17136", "17137", "17138", "17139", "17411", "17412", "17413", "17414", "17415", "17416", "17417", "17418", "17419", "17510", "17520", "17530"};
                if ("Bogor".equals(city)) return new String[]{"", "16110", "16111", "16120", "16130", "16140", "16150", "16160", "16310", "16320", "16330", "16340", "16350", "16360", "16370", "16380", "16390"};
                if ("Depok".equals(city)) return new String[]{"", "16411", "16412", "16413", "16414", "16415", "16416", "16417", "16418", "16419", "16421", "16422", "16423", "16424", "16425", "16426", "16427", "16428", "16429", "16431", "16432", "16433", "16434", "16435", "16436", "16437", "16438", "16439"};
                if ("Cirebon".equals(city)) return new String[]{"", "45111", "45112", "45113", "45114", "45115", "45121", "45122", "45123", "45124", "45125"};
                if ("Tasikmalaya".equals(city)) return new String[]{"", "46111", "46112", "46113", "46114", "46115", "46121", "46122", "46123"};
                if ("Sukabumi".equals(city)) return new String[]{"", "43111", "43112", "43113", "43114", "43115", "43121", "43122", "43123"};
                if ("Garut".equals(city)) return new String[]{"", "44111", "44112", "44113", "44114", "44115", "44121", "44122"};
                if ("Karawang".equals(city)) return new String[]{"", "41311", "41312", "41313", "41314", "41315"};
                if ("Purwakarta".equals(city)) return new String[]{"", "41111", "41112", "41113", "41114"};
                if ("Majalengka".equals(city)) return new String[]{"", "45411", "45412"};
                if ("Sumedang".equals(city)) return new String[]{"", "45311", "45312"};
                if ("Indramayu".equals(city)) return new String[]{"", "45211", "45212"};
                if ("Subang".equals(city)) return new String[]{"", "41211", "41212"};
                if ("Cianjur".equals(city)) return new String[]{"", "43211", "43212"};
                if ("Ciamis".equals(city)) return new String[]{"", "46211", "46212"};
                if ("Kuningan".equals(city)) return new String[]{"", "45511", "45512"};

            // DKI Jakarta
            } else if ("DKI Jakarta".equals(province)) {
                if ("Jakarta Pusat".equals(city)) return new String[]{"", "10110", "10120", "10130", "10140", "10150", "10160", "10170", "10310", "10320", "10330", "10340", "10350", "10410", "10420", "10430", "10440", "10450", "10460", "10470", "10510", "10520", "10530", "10540", "10550", "10560", "10570", "10610", "10620", "10630", "10640", "10650", "10660", "10670", "10680", "10710", "10720", "10730", "10740", "10750"};
                if ("Jakarta Barat".equals(city)) return new String[]{"", "11110", "11120", "11130", "11140", "11150", "11160", "11170", "11180", "11210", "11220", "11230", "11240", "11250", "11260", "11270", "11280", "11310", "11320", "11330", "11340", "11350", "11410", "11420", "11430", "11440", "11450", "11460", "11470", "11480", "11510", "11520", "11530", "11540", "11550", "11560", "11570", "11580", "11610", "11620", "11630", "11640", "11650", "11710", "11720", "11730", "11740", "11750"};
                if ("Jakarta Timur".equals(city)) return new String[]{"", "13110", "13120", "13130", "13140", "13150", "13210", "13220", "13230", "13240", "13250", "13310", "13320", "13330", "13340", "13410", "13420", "13430", "13440", "13510", "13520", "13530", "13540", "13550", "13610", "13620", "13630", "13640", "13650", "13710", "13720", "13730", "13740", "13750", "13760", "13770", "13810", "13820", "13830", "13840", "13850", "13860", "13870", "13910", "13920", "13930", "13940", "13950", "13960", "13970"};
                if ("Jakarta Utara".equals(city)) return new String[]{"", "14110", "14120", "14130", "14140", "14150", "14210", "14220", "14230", "14240", "14250", "14260", "14270", "14310", "14320", "14330", "14340", "14350", "14410", "14420", "14430", "14440", "14450", "14460", "14470", "14510", "14520"};
                if ("Jakarta Selatan".equals(city)) return new String[]{"", "12110", "12120", "12130", "12140", "12150", "12160", "12170", "12180", "12190", "12210", "12220", "12230", "12240", "12250", "12260", "12270", "12280", "12290", "12310", "12320", "12330", "12340", "12350", "12410", "12420", "12430", "12440", "12450", "12510", "12520", "12530", "12540", "12550", "12560", "12610", "12620", "12630", "12640", "12650", "12710", "12720", "12730", "12740", "12750", "12760", "12770", "12780", "12790", "12810", "12820", "12830", "12840", "12850", "12860", "12870", "12910", "12920", "12930", "12940", "12950", "12960", "12970", "12980"};

            // Jawa Tengah
            } else if ("Jawa Tengah".equals(province)) {
                if ("Semarang".equals(city)) return new String[]{"", "50111", "50112", "50113", "50114", "50115", "50116", "50121", "50122", "50123", "50124", "50125", "50126", "50131", "50132", "50133", "50134", "50135", "50136", "50137", "50138", "50139", "50141", "50142", "50143", "50144", "50145", "50146", "50147", "50148", "50149", "50211", "50212", "50213", "50214", "50215", "50216", "50217", "50218", "50219", "50221", "50222", "50223", "50224", "50225", "50226", "50227", "50228", "50229", "50231", "50232", "50233", "50234", "50235", "50236", "50237", "50238", "50239", "50241", "50242", "50243", "50244", "50245", "50246", "50247", "50248", "50249", "50519"};
                if ("Surakarta".equals(city)) return new String[]{"", "57111", "57112", "57113", "57114", "57115", "57116", "57117", "57118", "57119", "57121", "57122", "57123", "57124", "57125", "57126", "57127", "57128", "57129", "57131", "57132", "57133", "57134", "57135", "57136", "57137", "57138", "57139", "57141", "57142", "57143", "57144", "57145", "57146", "57147", "57148", "57149", "57151", "57152", "57153", "57154", "57155", "57156", "57157", "57158", "57159"};
                if ("Magelang".equals(city)) return new String[]{"", "56110", "56111", "56112", "56113", "56114", "56115", "56116", "56117", "56118", "56119", "56121", "56122", "56123"};
                // ... dan seterusnya untuk kota-kota lain di Jawa Tengah
            

            // Jawa Timur
            } else if ("Jawa Timur".equals(province)) {
                if ("Surabaya".equals(city)) return new String[]{"", "60111", "60112", "60113", "60114", "60115", "60116", "60117", "60118", "60119", "60121", "60122", "60123", "60124", "60125", "60126", "60127", "60128", "60129", "60131", "60132", "60133", "60134", "60135", "60136", "60137", "60138", "60139", "60141", "60142", "60143", "60144", "60145", "60146", "60147", "60148", "60149", "60151", "60152", "60153", "60154", "60155", "60156", "60157", "60158", "60159", "60161", "60162", "60163", "60164", "60165", "60166", "60167", "60168", "60169", "60171", "60172", "60173", "60174", "60175", "60176", "60177", "60178", "60179", "60181", "60182", "60183", "60184", "60185", "60186", "60187", "60188", "60189", "60191", "60192", "60193", "60194", "60195", "60196", "60197", "60198", "60199", "60211", "60212", "60213", "60214", "60215", "60216", "60217", "60218", "60219", "60221", "60222", "60223", "60224", "60225", "60226", "60227", "60228", "60229", "60231", "60232", "60233", "60234", "60235", "60236", "60237", "60238", "60239", "60241", "60242", "60243", "60244", "60245", "60246", "60247", "60248", "60249", "60251", "60252", "60253", "60254", "60255", "60256", "60257", "60258", "60259", "60261", "60262", "60263", "60264", "60265", "60266", "60267", "60268", "60269", "60271", "60272", "60273", "60274", "60275", "60276", "60277", "60278", "60279", "60281", "60282", "60283", "60284", "60285", "60286", "60287", "60288", "60289", "60291", "60292", "60293", "60294", "60295", "60296", "60297", "60298", "60299"};
                if ("Malang".equals(city)) return new String[]{"", "65111", "65112", "65113", "65114", "65115", "65116", "65117", "65118", "65119", "65121", "65122", "65123", "65124", "65125", "65131", "65132", "65133", "65134", "65135", "65136", "65137", "65138", "65139", "65141", "65142", "65143", "65144", "65145", "65146", "65147", "65148", "65149", "65151", "65152", "65153", "65154", "65155", "65156", "65157", "65158", "65159"};
                // ... dan seterusnya untuk kota-kota lain di Jawa Timur
            

            // Bali
            } else if ("Bali".equals(province)) {
                if ("Denpasar".equals(city)) return new String[]{"", "80111", "80113", "80114", "80115", "80116", "80117", "80118", "80119", "80211", "80221", "80223", "80224", "80225", "80226", "80227", "80228", "80231", "80232", "80233", "80234", "80235", "80236", "80237", "80238", "80239", "80361"};
                if ("Badung".equals(city)) return new String[]{"", "80351", "80352", "80353", "80361", "80362"};
                // ...
            }
            return new String[]{""};
           }
        

        private String[] getCountries() {
            return new String[]{"", "Indonesia", "Malaysia", "Singapore", "Thailand"};
        }
        
        private void populateCities(JComboBox<String> cmbCity, String selectedProvince) {
            cmbCity.removeAllItems();
            for (String city : getCities(selectedProvince)) {
                cmbCity.addItem(city);
            }
        }

        private void populatePostalCodes(JComboBox<String> cmbPostalCode, String selectedProvince, String selectedCity) {
            cmbPostalCode.removeAllItems();
            for (String postalCode : getPostalCodes(selectedProvince, selectedCity)) {
                cmbPostalCode.addItem(postalCode);
            }
        }


        private List<Address> getAddressesFromDatabase(int userId) {
            List<Address> addresses = new ArrayList<>();
            Connection conn = null;
            PreparedStatement stmt = null;
            ResultSet rs = null;
            try {
                conn = DatabaseConnection.getConnection();
                String query = "SELECT id, user_id, label, full_address, city, province, postal_code, country FROM addresses WHERE user_id = ?";
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
                        rs.getString("country")
                    ));
                }
            } catch (SQLException e) {
                System.err.println("Error loading addresses: " + e.getMessage());
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
                String query = "INSERT INTO addresses (user_id, label, full_address, city, province, postal_code, country) VALUES (?, ?, ?, ?, ?, ?, ?)";
                stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
                stmt.setInt(1, address.getUserId());
                stmt.setString(2, address.getLabel());
                stmt.setString(3, address.getFullAddress());
                stmt.setString(4, address.getCity());
                stmt.setString(5, address.getProvince());
                stmt.setString(6, address.getPostalCode());
                stmt.setString(7, address.getCountry());
                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            address.setId(generatedKeys.getInt(1)); // Set the ID for the new address object
                        }
                    }
                    JOptionPane.showMessageDialog(this, "Address added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to add address.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException e) {
                System.err.println("Error adding address: " + e.getMessage());
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } finally {
                DatabaseConnection.closeConnection(conn, stmt, null);
            }
        }

        private void updateAddressInDatabase(Address address) {
            Connection conn = null;
            PreparedStatement stmt = null;
            try {
                conn = DatabaseConnection.getConnection();
                String query = "UPDATE addresses SET label = ?, full_address = ?, city = ?, province = ?, postal_code = ?, country = ? WHERE id = ? AND user_id = ?";
                stmt = conn.prepareStatement(query);
                stmt.setString(1, address.getLabel());
                stmt.setString(2, address.getFullAddress());
                stmt.setString(3, address.getCity());
                stmt.setString(4, address.getProvince());
                stmt.setString(5, address.getPostalCode());
                stmt.setString(6, address.getCountry());
                stmt.setInt(7, address.getId());
                stmt.setInt(8, address.getUserId());
                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    JOptionPane.showMessageDialog(this, "Address updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to update address. Address not found or no changes.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException e) {
                System.err.println("Error updating address: " + e.getMessage());
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } finally {
                DatabaseConnection.closeConnection(conn, stmt, null);
            }
        }

        private void deleteAddress(int addressId) {
            int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this address?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
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
                        JOptionPane.showMessageDialog(this, "Address deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                        loadAddresses(); // Reload the list after deletion
                    } else {
                        JOptionPane.showMessageDialog(this, "Failed to delete address. Address not found.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (SQLException e) {
                    System.err.println("Error deleting address: " + e.getMessage());
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Database error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    DatabaseConnection.closeConnection(conn, stmt, null);
                }
            }
        }
    }

    // NEW: Address Model Class
    public static class Address {
        private int id;
        private int userId;
        private String label;
        private String fullAddress;
        private String city;
        private String province;
        private String postalCode;
        private String country;

        public Address(int id, int userId, String label, String fullAddress, String city, String province, String postalCode, String country) {
            this.id = id;
            this.userId = userId;
            this.label = label;
            this.fullAddress = fullAddress;
            this.city = city;
            this.province = province;
            this.postalCode = postalCode;
            this.country = country;
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

        // Setters (for updating)
        public void setId(int id) { this.id = id; }
        public void setLabel(String label) { this.label = label; }
        public void setFullAddress(String fullAddress) { this.fullAddress = fullAddress; }
        public void setCity(String city) { this.city = city; }
        public void setProvince(String province) { this.province = province; }
        public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
        public void setCountry(String country) { this.country = country; }
    }
}