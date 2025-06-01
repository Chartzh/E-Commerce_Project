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
        /*
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
        */
        // --- END PERUBAHAN: Menghilangkan profileInfoPanel ---


        String[] navItems = {"Profile", "Payment", "Security", "Legal Agreement"};
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
    /*
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
    */

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
        private JTextField txtFullName, txtPhone, txtEmail, txtCity, txtProvince, txtPostalCode, txtCountry, txtNik;
        private JTextArea txtAddress;
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
            
            // Grid for City, Province, Postal Code, Country (sesuai di gambar) - These belong to profile table
            JPanel gridPanel = new JPanel(new GridLayout(2, 2, 20, 10)); // 2 rows, 2 columns, gaps
            gridPanel.setBackground(Color.WHITE);
            gridPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0)); // Padding

            txtCity = new JTextField();
            gridPanel.add(createFormField("Kota/Kabupaten", user.getCity(), "Bekasi", txtCity));

            txtProvince = new JTextField();
            gridPanel.add(createFormField("Provinsi", user.getProvince(), "West Java", txtProvince));

            txtPostalCode = new JTextField();
            gridPanel.add(createFormField("Kode Pos", user.getPostalCode(), "17111", txtPostalCode));

            txtCountry = new JTextField();
            gridPanel.add(createFormField("Negara", user.getCountry(), "Indonesia", txtCountry));

            scrollableContent.add(gridPanel);

            // Address field (sesuai Alamat Lengkap di gambar) - This belongs to profile table
            txtAddress = new JTextArea();
            scrollableContent.add(createTextAreaField("Alamat Lengkap", user.getAddress() != null ? user.getAddress() : "", "2, Chief Ekenezu Street, Ubeji, Ayigu, Delta State", txtAddress));

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
                setupPlaceholder(txtCity, "Bekasi");
                setupPlaceholder(txtProvince, "West Java");
                setupPlaceholder(txtPostalCode, "17111");
                setupPlaceholder(txtCountry, "Indonesia");
                setupPlaceholder(txtAddress, "2, Chief Ekenezu Street, Ubeji, Ayigu, Delta State");
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

        // Helper method for JTextArea with placeholder
        private JPanel createTextAreaField(String labelText, String initialValue, String placeholder, JTextArea textArea) {
            JPanel panel = new JPanel(new BorderLayout(0, 5));
            panel.setBackground(Color.WHITE);
            panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

            JLabel lbl = new JLabel(labelText);
            lbl.setFont(new Font("Arial", Font.PLAIN, 13));
            lbl.setForeground(darkGrayColor);
            panel.add(lbl, BorderLayout.NORTH);

            textArea.setText(initialValue);
            textArea.setFont(new Font("Arial", Font.PLAIN, 15));
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            textArea.setBorder(new LineBorder(lightGrayColor, 1) { // Softer border, rounded corners
                @Override
                public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(getLineColor());
                    g2.drawRoundRect(x, y, width - 1, height - 1, 8, 8);
                    g2.dispose();
                }
            });
            JScrollPane scroll = new JScrollPane(textArea);
            scroll.setBorder(BorderFactory.createEmptyBorder());
            scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            scroll.setPreferredSize(new Dimension(scroll.getPreferredSize().width, 80));

            // Apply placeholder to JTextArea
            setupPlaceholder(textArea, placeholder);

            panel.add(scroll, BorderLayout.CENTER);
            return panel;
        }

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
            String city = (Boolean.TRUE.equals(txtCity.getClientProperty("showingPlaceholder"))) ? "" : txtCity.getText().trim();
            String province = (Boolean.TRUE.equals(txtProvince.getClientProperty("showingPlaceholder"))) ? "" : txtProvince.getText().trim();
            String postalCode = (Boolean.TRUE.equals(txtPostalCode.getClientProperty("showingPlaceholder"))) ? "" : txtPostalCode.getText().trim();
            String country = (Boolean.TRUE.equals(txtCountry.getClientProperty("showingPlaceholder"))) ? "" : txtCountry.getText().trim();
            String address = (Boolean.TRUE.equals(txtAddress.getClientProperty("showingPlaceholder"))) ? "" : txtAddress.getText().trim();

            // Validasi input
            if (fullName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nama Lengkap tidak boleh kosong.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (email.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Alamat Email tidak boleh kosong.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            // Validasi NIK, Phone, Address, City, Province, Postal Code, Country can be added here if mandatory
            // For now, allow empty for these if not critical for initial profile.
            
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

                // 2. Update profile table (nik, address, phone, city, province, postal_code, country, pictures)
                StringBuilder updateProfileSqlBuilder = new StringBuilder("UPDATE profile SET nik = ?, address = ?, phone = ?, city = ?, province = ?, postal_code = ?, country = ?");
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
                stmtProfile.setString(paramIndex++, address);
                stmtProfile.setString(paramIndex++, phone);
                stmtProfile.setString(paramIndex++, city);
                stmtProfile.setString(paramIndex++, province);
                stmtProfile.setString(paramIndex++, postalCode);
                stmtProfile.setString(paramIndex++, country);

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
                    user.setAddress(address);
                    user.setPhone(phone);
                    user.setCity(city);
                    user.setProvince(province);
                    user.setPostalCode(postalCode);
                    user.setCountry(country);

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
}