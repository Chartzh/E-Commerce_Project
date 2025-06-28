package e.commerce;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Versi yang disederhanakan dengan satu tombol close di header.
 */
public class ChatPopupUI extends JDialog {
    // Variabel dideklarasikan di sini
    private final ViewController viewController;
    private final int currentUserId;
    private int activeChatReceiverId = -1;
    private String activeChatReceiverUsername;

    private final JList<ProductRepository.ChatUser> userList;
    private final DefaultListModel<ProductRepository.ChatUser> userListModel;
    private final JPanel chatContainerPanel;
    private final JTextField messageInputField;
    private final JLabel chatHeaderLabel;
    private final JScrollPane chatScrollPane;
    private final JPanel chatContentPanel;

    private Timer refreshTimer;
    private final int REFRESH_INTERVAL_MS = 3000;

    // ... (Kelas UserListCellRenderer tidak berubah) ...
    private static class UserListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            ProductRepository.ChatUser user = (ProductRepository.ChatUser) value;

            JPanel panel = new JPanel();
            panel.setLayout(new BorderLayout(10, 5));
            panel.setBorder(new EmptyBorder(12, 15, 12, 15));

            if (isSelected) {
                panel.setBackground(new Color(255, 89, 0, 30));
            } else {
                panel.setBackground(Color.WHITE);
            }

            // Panel Avatar
            JPanel avatarPanel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.setColor(new Color(255, 89, 0));
                    g2d.fillOval(0, 0, 35, 35);
                    g2d.setColor(Color.WHITE);
                    g2d.setFont(new Font("Arial", Font.BOLD, 14));
                    FontMetrics fm = g2d.getFontMetrics();
                    String initial = user.getUsername().isEmpty() ? "" : user.getUsername().substring(0, 1).toUpperCase();
                    int x = (35 - fm.stringWidth(initial)) / 2;
                    int y = ((35 - fm.getHeight()) / 2) + fm.getAscent();
                    g2d.drawString(initial, x, y);
                    g2d.dispose();
                }
            };
            avatarPanel.setPreferredSize(new Dimension(35, 35));
            avatarPanel.setOpaque(false);

            // Panel informasi pengguna
            JPanel userInfoPanel = new JPanel();
            userInfoPanel.setLayout(new BoxLayout(userInfoPanel, BoxLayout.Y_AXIS));
            userInfoPanel.setOpaque(false);

            JLabel nameLabel = new JLabel(user.getUsername());
            nameLabel.setFont(new Font("Arial", Font.BOLD, 13));
            nameLabel.setForeground(Color.BLACK);

            JLabel lastMessageLabel = new JLabel(user.getLastMessage() != null ? user.getLastMessage() : "");
            lastMessageLabel.setFont(new Font("Arial", Font.PLAIN, 11));
            lastMessageLabel.setForeground(Color.GRAY);

            userInfoPanel.add(nameLabel);
            if (user.getId() != -1) {
                userInfoPanel.add(Box.createVerticalStrut(2));
                userInfoPanel.add(lastMessageLabel);
            }

            panel.add(avatarPanel, BorderLayout.WEST);
            panel.add(userInfoPanel, BorderLayout.CENTER);

            return panel;
        }
    }


    public ChatPopupUI(JFrame owner, ViewController viewController) {
        super(owner, "Quantra Chat", false);
        this.viewController = viewController;
        this.currentUserId = (Authentication.getCurrentUser() != null) ? Authentication.getCurrentUser().getId() : -1;

        if (this.currentUserId == -1) {
            JOptionPane.showMessageDialog(this, "ID Pengguna tidak ditemukan. Silakan login kembali.", "Error", JOptionPane.ERROR_MESSAGE);
            SwingUtilities.invokeLater(this::dispose);
            // Inisialisasi darurat
            this.userList = new JList<>();
            this.userListModel = new DefaultListModel<>();
            this.chatContainerPanel = new JPanel();
            this.messageInputField = new JTextField();
            this.chatHeaderLabel = new JLabel();
            this.chatScrollPane = new JScrollPane();
            this.chatContentPanel = new JPanel();
            return;
        }

        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(500, 600));

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 0, 0, 30), 1),
                BorderFactory.createEmptyBorder(1, 1, 1, 1)
        ));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(new EmptyBorder(15, 20, 15, 15));

        JLabel titleLabel = new JLabel("Chat");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setForeground(new Color(33, 33, 33));
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JPanel headerButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        headerButtons.setOpaque(false);

        try {
            int targetWidth = 18;
            int targetHeight = 18;

            java.net.URL iconUrl = getClass().getResource("/resources/images/minimize_icon.png");
            if (iconUrl == null) {
                throw new NullPointerException("Resource tidak ditemukan: /resources/images/minimize_icon.png");
            }
            ImageIcon originalIcon = new ImageIcon(iconUrl);

            Image originalImage = originalIcon.getImage();
            Image resizedImage = originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
            ImageIcon resizedIcon = new ImageIcon(resizedImage);
            JButton closeButton = new JButton(resizedIcon);
            closeButton.setBorderPainted(false);
            closeButton.setContentAreaFilled(false);
            closeButton.setFocusPainted(false);
            closeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
            closeButton.setPreferredSize(new Dimension(targetWidth, targetHeight));

            closeButton.addActionListener(e -> dispose());
            headerButtons.add(closeButton);

        } catch (Exception ex) {
            System.err.println("KRITIS: Gagal memuat ikon dari resource. Pesan Error: " + ex.getMessage());
            JButton closeButtonFallback = new JButton("X");
            closeButtonFallback.addActionListener(e -> dispose());
            headerButtons.add(closeButtonFallback);
        }
        
        headerPanel.add(headerButtons, BorderLayout.EAST);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(Color.WHITE);

        JPanel userListPanel = new JPanel(new BorderLayout());
        userListPanel.setBackground(Color.WHITE);
        userListPanel.setPreferredSize(new Dimension(200, 0));

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.setCellRenderer(new UserListCellRenderer());
        userList.setFixedCellHeight(60);
        userList.setBorder(null);
        userList.setBackground(Color.WHITE);
        userList.setSelectionBackground(new Color(255, 89, 0, 30));
        JScrollPane userListScrollPane = new JScrollPane(userList);
        userListScrollPane.setBorder(null);
        userListPanel.add(userListScrollPane, BorderLayout.CENTER);

        chatContentPanel = new JPanel(new BorderLayout());
        chatContentPanel.setBackground(new Color(248, 249, 250));
        chatContentPanel.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(230, 230, 230)));

        JPanel chatHeaderPanel = new JPanel(new BorderLayout());
        chatHeaderPanel.setBackground(Color.WHITE);
        chatHeaderPanel.setBorder(new EmptyBorder(15, 20, 15, 20));

        chatHeaderLabel = new JLabel("Pilih kontak chat");
        chatHeaderLabel.setFont(new Font("Arial", Font.BOLD, 16));
        chatHeaderPanel.add(chatHeaderLabel, BorderLayout.CENTER);
        chatContentPanel.add(chatHeaderPanel, BorderLayout.NORTH);

        chatContainerPanel = new JPanel();
        chatContainerPanel.setLayout(new BoxLayout(chatContainerPanel, BoxLayout.Y_AXIS));
        chatContainerPanel.setBackground(new Color(248, 249, 250));
        chatContainerPanel.setBorder(new EmptyBorder(10, 15, 10, 15));

        chatScrollPane = new JScrollPane(chatContainerPanel);
        chatScrollPane.setBorder(null);
        chatContentPanel.add(chatScrollPane, BorderLayout.CENTER);

        JPanel messageInputPanel = new JPanel(new BorderLayout(10, 0));
        messageInputPanel.setBorder(new EmptyBorder(15, 20, 20, 20));
        messageInputPanel.setBackground(Color.WHITE);

        messageInputField = new JTextField();
        messageInputField.setFont(new Font("Arial", Font.PLAIN, 14));
        messageInputField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220)),
            new EmptyBorder(5, 10, 5, 10)
        ));
        messageInputField.addActionListener(e -> sendMessage());
        messageInputPanel.add(messageInputField, BorderLayout.CENTER);

        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(e -> sendMessage());
        messageInputPanel.add(sendButton, BorderLayout.EAST);
        chatContentPanel.add(messageInputPanel, BorderLayout.SOUTH);

        contentPanel.add(userListPanel, BorderLayout.WEST);
        contentPanel.add(chatContentPanel, BorderLayout.CENTER);
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        add(mainPanel);

        // Listener ditambahkan setelah semua komponen diinisialisasi
        userList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                ProductRepository.ChatUser selectedUser = userList.getSelectedValue();
                if (selectedUser != null && selectedUser.getId() != -1) {
                    startChatWith(selectedUser.getId(), selectedUser.getUsername());
                } else if (selectedUser != null && selectedUser.getId() == -1) {
                    chatHeaderLabel.setText("Pilih kontak chat");
                    showPlaceholderInChatPanel("Pilih pengguna di daftar kiri untuk memulai chat.");
                    stopRefreshTimer();
                }
            }
        });
        
        loadUsers();
        showPlaceholderInChatPanel("Pilih pengguna di daftar kiri untuk memulai chat.");
    }
    
    // ... (Sisa metode seperti showPlaceholderInChatPanel, createModernHeaderButton, loadUsers, dll. tetap sama) ...
    private void showPlaceholderInChatPanel(String text) {
        chatContainerPanel.removeAll();
        chatContainerPanel.setLayout(new GridBagLayout());

        JLabel placeholderLabel = new JLabel(text);
        placeholderLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        placeholderLabel.setForeground(Color.GRAY);
        
        chatContainerPanel.add(placeholderLabel, new GridBagConstraints());
        chatContainerPanel.revalidate();
        chatContainerPanel.repaint();
    }

    private JButton createModernHeaderButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Dialog", Font.PLAIN, 16));
        button.setForeground(new Color(120, 120, 120));
        button.setBackground(Color.WHITE);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setPreferredSize(new Dimension(30, 30));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setForeground(new Color(255, 89, 0));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setForeground(new Color(120, 120, 120));
            }
        });
        return button;
    }

    private void loadUsers() {
        try {
            userListModel.clear();
            List<ProductRepository.ChatUser> users = ProductRepository.getRecentChatUsersWithLastMessage(currentUserId);
            if (users.isEmpty()) {
                userListModel.addElement(new ProductRepository.ChatUser(-1, "Belum ada chat.", ""));
                userList.setEnabled(false);
            } else {
                for (ProductRepository.ChatUser user : users) {
                    userListModel.addElement(user);
                }
                userList.setEnabled(true);
            }
        } catch (SQLException e) {
            System.err.println("Error loading chat users: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Gagal memuat daftar kontak chat. Cek koneksi database.", "Error Database", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void startChatWith(int receiverId, String receiverUsername) {
        if (receiverId == currentUserId) {
            JOptionPane.showMessageDialog(this, "Tidak bisa chat dengan diri sendiri.", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        this.activeChatReceiverId = receiverId;
        this.activeChatReceiverUsername = receiverUsername;
        chatHeaderLabel.setText(receiverUsername);
        loadChatMessages();
        startRefreshTimer();

        for (int i = 0; i < userListModel.getSize(); i++) {
            if (userListModel.getElementAt(i).getId() == receiverId) {
                userList.setSelectedIndex(i);
                break;
            }
        }
        setVisible(true);
        messageInputField.requestFocusInWindow();
    }
    
    private void loadChatMessages() {
        if (activeChatReceiverId == -1) {
            showPlaceholderInChatPanel("Pilih pengguna untuk memulai chat.");
            return;
        }

        try {
            chatContainerPanel.removeAll();
            chatContainerPanel.setLayout(new BoxLayout(chatContainerPanel, BoxLayout.Y_AXIS));

            List<ProductRepository.ChatMessage> messages = ProductRepository.getChatMessages(currentUserId, activeChatReceiverId);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

            for (ProductRepository.ChatMessage msg : messages) {
                String timestampStr = msg.getTimestamp().format(formatter);
                String cleanMessage = escapeHtml(msg.getMessageText());
                boolean isSender = msg.getSenderId() == currentUserId;
                
                JPanel bubbleWrapper = new JPanel(new BorderLayout());
                bubbleWrapper.setOpaque(false);
                bubbleWrapper.setBorder(new EmptyBorder(2, 0, 2, 0));

                JPanel contentPanel = new JPanel();
                contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
                contentPanel.setOpaque(false);

                JTextArea textBubble = new JTextArea(cleanMessage);
                textBubble.setEditable(false);
                textBubble.setLineWrap(true);
                textBubble.setWrapStyleWord(true);
                textBubble.setFont(new Font("Arial", Font.PLAIN, 12));
                textBubble.setBorder(new EmptyBorder(8, 12, 8, 12));
                
                JLabel timestampLabel = new JLabel(timestampStr);
                timestampLabel.setFont(new Font("Arial", Font.PLAIN, 10));
                timestampLabel.setForeground(Color.GRAY);
                timestampLabel.setBorder(new EmptyBorder(2,5,0,5));

                if (isSender) {
                    textBubble.setBackground(new Color(255, 89, 0));
                    textBubble.setForeground(Color.WHITE);
                    contentPanel.setAlignmentX(Component.RIGHT_ALIGNMENT);
                    timestampLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
                    bubbleWrapper.add(contentPanel, BorderLayout.EAST);
                } else {
                    textBubble.setBackground(new Color(233, 233, 235));
                    textBubble.setForeground(Color.BLACK);
                    contentPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
                    timestampLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                    bubbleWrapper.add(contentPanel, BorderLayout.WEST);
                }

                contentPanel.add(textBubble);
                contentPanel.add(timestampLabel);
                
                chatContainerPanel.add(bubbleWrapper);

                if (msg.getReceiverId() == currentUserId && !msg.isRead()) {
                    ProductRepository.markMessagesAsRead(currentUserId, msg.getSenderId());
                }
            }

            chatContainerPanel.add(Box.createVerticalGlue());
            chatContainerPanel.revalidate();
            chatContainerPanel.repaint();

            SwingUtilities.invokeLater(() -> {
                JScrollBar vertical = chatScrollPane.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            });

        } catch (SQLException e) {
            System.err.println("Error loading chat messages: " + e.getMessage());
            showPlaceholderInChatPanel("Gagal memuat pesan chat.");
            JOptionPane.showMessageDialog(this, "Gagal memuat pesan chat. Cek koneksi database.", "Error Database", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void sendMessage() {
        String messageText = messageInputField.getText().trim();
        if (activeChatReceiverId == -1) {
            JOptionPane.showMessageDialog(this, "Pilih pengguna untuk mengirim pesan.", "Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (messageText.isEmpty()) {
            return;
        }

        try {
            boolean success = ProductRepository.sendMessage(currentUserId, activeChatReceiverId, messageText);
            if (success) {
                messageInputField.setText("");
                loadChatMessages();
                loadUsers();
            } else {
                JOptionPane.showMessageDialog(this, "Gagal mengirim pesan.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            System.err.println("Error sending message: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Gagal mengirim pesan. Error database.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void startRefreshTimer() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
        }
        refreshTimer = new Timer("ChatRefreshTimer", true);
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (activeChatReceiverId != -1 && isVisible()) {
                    SwingUtilities.invokeLater(() -> {
                        loadChatMessages();
                        loadUsers();
                    });
                }
            }
        }, REFRESH_INTERVAL_MS, REFRESH_INTERVAL_MS);
    }

    public void stopRefreshTimer() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
            refreshTimer = null;
        }
    }

    public void toggleVisibility() {
        if (isVisible()) {
            setVisible(false);
            stopRefreshTimer();
        } else {
            loadUsers();
            setVisible(true);
            if (activeChatReceiverId != -1) {
                startRefreshTimer();
            } else {
                chatHeaderLabel.setText("Pilih kontak chat");
                showPlaceholderInChatPanel("Pilih pengguna di daftar kiri untuk memulai chat.");
            }
            if (getOwner() != null) {
                int x = getOwner().getX() + getOwner().getWidth() - getWidth() - 20;
                int y = getOwner().getY() + getOwner().getHeight() - getHeight() - 20;
                setLocation(x, y);
            }
            if (activeChatReceiverId == -1) {
                userList.clearSelection();
            }
        }
    }

    public void openChatWithDefaultUser() {
        if (activeChatReceiverId == -1 && !userListModel.isEmpty()) {
            ProductRepository.ChatUser firstUser = userListModel.getElementAt(0);
            if (firstUser.getId() != -1) {
                startChatWith(firstUser.getId(), firstUser.getUsername());
            } else {
                chatHeaderLabel.setText("Belum ada chat.");
                showPlaceholderInChatPanel("Mulai chat dari halaman produk.");
            }
        } else if (userListModel.isEmpty() || (userListModel.size() == 1 && userListModel.getElementAt(0).getId() == -1)) {
            chatHeaderLabel.setText("Belum ada chat.");
            showPlaceholderInChatPanel("Mulai chat dari halaman produk.");
        }
        toggleVisibility();
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    @Override
    public void dispose() {
        stopRefreshTimer();
        super.dispose();
    }
}