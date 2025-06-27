package e.commerce;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Kelas ini merepresentasikan antarmuka pengguna untuk jendela obrolan popup.
 * Menampilkan daftar pengguna untuk obrolan, area obrolan, dan bidang input pesan.
 */
public class ChatPopupUI extends JDialog {
    private ViewController viewController;
    private int currentUserId;
    private int activeChatReceiverId = -1;
    private String activeChatReceiverUsername;

    private JList<ProductRepository.ChatUser> userList;
    private DefaultListModel<ProductRepository.ChatUser> userListModel;
    private JTextPane chatArea;
    private JTextField messageInputField;
    private JLabel chatHeaderLabel;
    private JScrollPane chatScrollPane;
    private JPanel chatContentPanel;
    private JPanel userListPanel;

    private Timer refreshTimer;
    private final int REFRESH_INTERVAL_MS = 3000; // Interval refresh pesan dalam milidetik

    /**
     * Renderer sel kustom untuk daftar pengguna obrolan, menampilkan avatar dan informasi pengguna.
     */
    private class UserListCellRenderer extends DefaultListCellRenderer {
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

                    // Gambar lingkaran latar belakang
                    g2d.setColor(new Color(255, 89, 0));
                    g2d.fillOval(0, 0, 35, 35);

                    // Gambar huruf awal
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

            // Menampilkan pesan terakhir
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
            return;
        }

        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(500, 600));

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // Logika positioning ada di UserDashboardUI/SupervisorDashboardUI
            }
        });

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createLineBorder(new Color(230, 230, 230), 1));
        
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

        JButton searchButton = createModernHeaderButton("🔍");
        JButton settingsButton = createModernHeaderButton("⚙");
        JButton minimizeButton = createModernHeaderButton("—");
        minimizeButton.addActionListener(e -> setVisible(false));
        JButton closeButton = createModernHeaderButton("×");
        closeButton.addActionListener(e -> dispose());
        
        headerButtons.add(searchButton);
        headerButtons.add(settingsButton);
        headerButtons.add(minimizeButton);
        headerButtons.add(closeButton);
        headerPanel.add(headerButtons, BorderLayout.EAST);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(Color.WHITE);

        userListPanel = new JPanel(new BorderLayout());
        userListPanel.setBackground(Color.WHITE);
        userListPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        userListPanel.setPreferredSize(new Dimension(200, 0)); 

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.setCellRenderer(new UserListCellRenderer());
        userList.setFixedCellHeight(60);
        userList.setBorder(null);
        userList.setBackground(Color.WHITE);
        userList.setSelectionBackground(new Color(255, 89, 0, 30));

        userList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                ProductRepository.ChatUser selectedUser = userList.getSelectedValue();
                if (selectedUser != null && selectedUser.getId() != -1) {
                    startChatWith(selectedUser.getId(), selectedUser.getUsername());
                } else if (selectedUser != null && selectedUser.getId() == -1) {
                    chatHeaderLabel.setText("Pilih kontak chat");
                    chatArea.setText("<html><body style='font-family: Arial; font-size: 14pt; color: #666; text-align: center; padding: 50px;'>Pilih pengguna di daftar kiri untuk memulai chat.</body></html>");
                    stopRefreshTimer(); 
                }
            }
        });

        JScrollPane userListScrollPane = new JScrollPane(userList);
        userListScrollPane.setBorder(null);
        userListScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        userListScrollPane.setBackground(Color.WHITE);
        userListPanel.add(userListScrollPane, BorderLayout.CENTER);

        chatContentPanel = new JPanel(new BorderLayout());
        chatContentPanel.setBackground(new Color(248, 249, 250));
        chatContentPanel.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(230, 230, 230)));

        JPanel chatHeaderPanel = new JPanel(new BorderLayout());
        chatHeaderPanel.setBackground(Color.WHITE);
        chatHeaderPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(240, 240, 240)),
                new EmptyBorder(15, 20, 15, 20)
        ));

        JPanel chatUserInfo = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        chatUserInfo.setOpaque(false);

        JPanel chatAvatarPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(255, 89, 0));
                g2d.fillOval(0, 0, 35, 35);
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 14));
                FontMetrics fm = g2d.getFontMetrics();
                String initial = (activeChatReceiverUsername != null && !activeChatReceiverUsername.isEmpty()) ?
                        activeChatReceiverUsername.substring(0, 1).toUpperCase() : "?";
                int x = (35 - fm.stringWidth(initial)) / 2;
                int y = ((35 - fm.getHeight()) / 2) + fm.getAscent();
                g2d.drawString(initial, x, y);
                g2d.dispose();
            }
        };
        chatAvatarPanel.setPreferredSize(new Dimension(35, 35));
        chatAvatarPanel.setOpaque(false);

        chatHeaderLabel = new JLabel("Pilih kontak chat");
        chatHeaderLabel.setFont(new Font("Arial", Font.BOLD, 16));
        chatHeaderLabel.setForeground(new Color(33, 33, 33));

        chatUserInfo.add(chatAvatarPanel);
        chatUserInfo.add(chatHeaderLabel);
        chatHeaderPanel.add(chatUserInfo, BorderLayout.WEST);

        chatContentPanel.add(chatHeaderPanel, BorderLayout.NORTH);

        chatArea = new JTextPane();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Arial", Font.PLAIN, 14));
        chatArea.setContentType("text/html");
        chatArea.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        chatArea.putClientProperty(JEditorPane.W3C_LENGTH_UNITS, Boolean.TRUE); 
        chatArea.setEditorKit(JEditorPane.createEditorKitForContentType("text/html"));
        chatArea.setText("<html><body></body></html>");
        chatArea.setBackground(new Color(248, 249, 250));
        chatArea.setBorder(new EmptyBorder(15, 20, 15, 20));

        chatScrollPane = new JScrollPane(chatArea);
        chatScrollPane.setBorder(null);
        chatScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        chatScrollPane.setBackground(new Color(248, 249, 250));
        chatScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        chatScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); 

        chatContentPanel.add(chatScrollPane, BorderLayout.CENTER);

        JPanel messageInputPanel = new JPanel(new BorderLayout(10, 0));
        messageInputPanel.setBorder(new EmptyBorder(15, 20, 20, 20));
        messageInputPanel.setBackground(Color.WHITE);
        messageInputPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(240, 240, 240)),
                new EmptyBorder(15, 20, 20, 20)
        ));

        JPanel inputContainer = new JPanel(new BorderLayout());
        inputContainer.setBackground(new Color(248, 249, 250));
        inputContainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 230, 230), 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        messageInputField = new JTextField();
        messageInputField.setFont(new Font("Arial", Font.PLAIN, 14));
        messageInputField.setBorder(null);
        messageInputField.setBackground(new Color(248, 249, 250));
        messageInputField.addActionListener(e -> sendMessage()); 

        inputContainer.add(messageInputField, BorderLayout.CENTER);

        JButton sendButton = new JButton("Send") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (getModel().isPressed()) {
                    g2d.setColor(new Color(230, 70, 0));
                } else if (getModel().isRollover()) {
                    g2d.setColor(new Color(255, 110, 30));
                } else {
                    g2d.setColor(new Color(255, 89, 0));
                }

                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);

                g2d.setColor(Color.WHITE);
                g2d.setFont(getFont());
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
                g2d.drawString(getText(), x, y);

                g2d.dispose();
            }
        };
        sendButton.setFont(new Font("Arial", Font.BOLD, 13));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        sendButton.setBorderPainted(false);
        sendButton.setContentAreaFilled(false);
        sendButton.setPreferredSize(new Dimension(70, 35));
        sendButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        sendButton.addActionListener(e -> sendMessage());

        messageInputPanel.add(inputContainer, BorderLayout.CENTER);
        messageInputPanel.add(sendButton, BorderLayout.EAST);

        chatContentPanel.add(messageInputPanel, BorderLayout.SOUTH);

        contentPanel.add(userListPanel, BorderLayout.WEST);
        contentPanel.add(chatContentPanel, BorderLayout.CENTER);

        mainPanel.add(contentPanel, BorderLayout.CENTER);
        add(mainPanel);

        loadUsers();
        chatArea.setText("<html><body style='font-family: Arial; font-size: 14pt; color: #666; text-align: center; padding: 50px;'>Pilih pengguna di daftar kiri untuk memulai chat.</body></html>");
    }

    private JButton createModernHeaderButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.PLAIN, 16));
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

        chatContentPanel.getComponent(0).repaint(); 

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
            chatArea.setText("<html><body style='font-family: Arial; font-size: 10pt; color: #666; text-align: center; padding: 50px;'>Pilih pengguna di daftar kiri untuk memulai chat.</body></html>");
            return;
        }

        try {
            List<ProductRepository.ChatMessage> messages = ProductRepository.getChatMessages(currentUserId, activeChatReceiverId);
            StringBuilder sb = new StringBuilder("<html><body style='font-family: Arial; font-size: 10pt; margin: 0; padding: 10px; background-color: #f8f9fa;'>");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

            for (ProductRepository.ChatMessage msg : messages) {
                String timestampStr = msg.getTimestamp().format(formatter);
                String messageHtml;

                // Chat kita sendiri (pengirim adalah currentUserId) - di sebelah kanan, teks putih, latar belakang oranye tema
                if (msg.getSenderId() == currentUserId) {
                    messageHtml = "<div style='margin: 5px 0; text-align: right;'>" + // Margin diperkecil
                                  "<div style='display: inline-block; max-width: 65%; background: #FF5900; color: white; padding: 8px 12px; border-radius: 12px 12px 3px 12px; text-align: left; box-shadow: 0 2px 5px rgba(255,89,0,0.2); word-wrap: break-word; font-size: 9pt; line-height: 1.3;'>" + // Padding, border-radius, font-size, dan line-height disesuaikan
                                  escapeHtml(msg.getMessageText()).replace("\n", "<br>") +
                                  "</div><br>" +
                                  "<span style='font-size: 8px; color: #999; margin-right: 5px; margin-top: 2px; display: inline-block;'>" + timestampStr + "</span>" + // Ukuran font timestamp, margin-top
                                  "</div>";
                } else {
                    // Chat dari lawan (pengirim bukan currentUserId) - di sebelah kiri, teks hitam, latar belakang abu-abu
                    messageHtml = "<div style='margin: 5px 0; text-align: left;'>" + // Margin diperkecil
                                  "<div style='display: inline-block; max-width: 65%; background: #E0E0E0; color: black; padding: 8px 12px; border-radius: 12px 12px 12px 3px; border: 1px solid #D0D0D0; box-shadow: 0 1px 3px rgba(0,0,0,0.05); word-wrap: break-word; font-size: 9pt; line-height: 1.3;'>" + // Padding, border-radius, font-size, dan line-height disesuaikan
                                  escapeHtml(msg.getMessageText()).replace("\n", "<br>") +
                                  "</div><br>" +
                                  "<span style='font-size: 8px; color: #999; margin-left: 5px; margin-top: 2px; display: inline-block;'>" + timestampStr + "</span>" + // Ukuran font timestamp, margin-top
                                  "</div>";
                }
                sb.append(messageHtml);

                if (msg.getReceiverId() == currentUserId && !msg.isRead()) {
                    ProductRepository.markMessagesAsRead(currentUserId, msg.getSenderId());
                }
            }
            sb.append("</body></html>");
            chatArea.setText(sb.toString());

            SwingUtilities.invokeLater(() -> {
                JScrollBar vertical = chatScrollPane.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            });

        } catch (SQLException e) {
            System.err.println("Error loading chat messages: " + e.getMessage());
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
        refreshTimer = new Timer();
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
                chatArea.setText("<html><body style='font-family: Arial; font-size: 14pt; color: #666; text-align: center; padding: 50px;'>Pilih pengguna di daftar kiri untuk memulai chat.</body></html>");
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
                chatArea.setText("<html><body style='font-family: Arial; font-size: 14pt; color: #666; text-align: center; padding: 50px;'>Mulai chat dari halaman produk.</body></html>");
            }
        } else if (userListModel.isEmpty() || (userListModel.size() == 1 && userListModel.getElementAt(0).getId() == -1)){
            chatHeaderLabel.setText("Belum ada chat.");
            chatArea.setText("<html><body style='font-family: Arial; font-size: 14pt; color: #666; text-align: center; padding: 50px;'>Mulai chat dari halaman produk.</body></html>");
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