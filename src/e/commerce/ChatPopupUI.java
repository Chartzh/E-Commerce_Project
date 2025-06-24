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

    // REVISI KONSTRUKTOR: Menerima JFrame owner DAN ViewController secara terpisah
    public ChatPopupUI(JFrame owner, ViewController viewController) { // <--- UBAH DI SINI
        super(owner, "Quantra Chat", false);
        this.viewController = viewController; // Simpan ViewController yang diterima
        this.currentUserId = Authentication.getCurrentUser().getId();

        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));
        setLayout(new BorderLayout());
        setMinimumSize(new Dimension(450, 550));

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // Logika positioning ada di UserDashboardUI/SupervisorDashboardUI
            }
        });

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(5, 5));
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(255, 89, 0));
        headerPanel.setBorder(new EmptyBorder(5, 10, 5, 5));

        JLabel titleLabel = new JLabel("Chat");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JPanel headerButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        headerButtons.setOpaque(false);

        JButton minimizeButton = createHeaderButton("—");
        minimizeButton.addActionListener(e -> setVisible(false));
        headerButtons.add(minimizeButton);

        JButton closeButton = createHeaderButton("X");
        closeButton.addActionListener(e -> dispose());
        headerButtons.add(closeButton);
        headerPanel.add(headerButtons, BorderLayout.EAST);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerSize(5);
        splitPane.setBackground(Color.WHITE);

        userListPanel = new JPanel(new BorderLayout());
        userListPanel.setBackground(new Color(240, 240, 240));
        userListPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        JLabel userListTitle = new JLabel("Kontak Chat", SwingConstants.CENTER);
        userListTitle.setFont(new Font("Arial", Font.BOLD, 14));
        userListTitle.setBorder(new EmptyBorder(5, 0, 5, 0));
        userListPanel.add(userListTitle, BorderLayout.NORTH);

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.setFont(new Font("Arial", Font.PLAIN, 14));
        userList.setFixedCellHeight(30);
        userList.setBorder(null);
        userList.setBackground(new Color(245, 245, 245));

        userList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                ProductRepository.ChatUser selectedUser = userList.getSelectedValue();
                if (selectedUser != null && selectedUser.getId() != -1) {
                    startChatWith(selectedUser.getId(), selectedUser.getUsername());
                }
            }
        });

        JScrollPane userListScrollPane = new JScrollPane(userList);
        userListScrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        userListScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        userListPanel.add(userListScrollPane, BorderLayout.CENTER);
        splitPane.setLeftComponent(userListPanel);

        chatContentPanel = new JPanel(new BorderLayout(5, 5));
        chatContentPanel.setBackground(Color.WHITE);
        chatContentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        chatHeaderLabel = new JLabel("Pilih kontak chat", SwingConstants.CENTER);
        chatHeaderLabel.setFont(new Font("Arial", Font.BOLD, 16));
        chatHeaderLabel.setBorder(new EmptyBorder(0, 0, 5, 0));
        chatHeaderLabel.setForeground(new Color(255, 89, 0));
        chatContentPanel.add(chatHeaderLabel, BorderLayout.NORTH);

        chatArea = new JTextPane();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Arial", Font.PLAIN, 13));
        chatArea.setContentType("text/html");
        chatArea.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        chatArea.setText("<html><body></body></html>");

        chatScrollPane = new JScrollPane(chatArea);
        chatScrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        chatScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        chatContentPanel.add(chatScrollPane, BorderLayout.CENTER);

        JPanel messageInputPanel = new JPanel(new BorderLayout(5, 5));
        messageInputPanel.setBorder(new EmptyBorder(5, 0, 0, 0));
        messageInputPanel.setBackground(Color.WHITE);

        messageInputField = new JTextField();
        messageInputField.setFont(new Font("Arial", Font.PLAIN, 13));
        messageInputField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY),
            BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        messageInputField.addActionListener(e -> sendMessage());

        JButton sendButton = new JButton("Kirim");
        sendButton.setFont(new Font("Arial", Font.BOLD, 12));
        sendButton.setBackground(new Color(255, 89, 0));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        sendButton.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        sendButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        sendButton.addActionListener(e -> sendMessage());

        messageInputPanel.add(messageInputField, BorderLayout.CENTER);
        messageInputPanel.add(sendButton, BorderLayout.EAST);

        chatContentPanel.add(messageInputPanel, BorderLayout.SOUTH);
        splitPane.setRightComponent(chatContentPanel);

        splitPane.setDividerLocation(120);

        mainPanel.add(splitPane, BorderLayout.CENTER);
        add(mainPanel);

        loadUsers();
    }

    private JButton createHeaderButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(255, 89, 0));
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setMargin(new Insets(0, 5, 0, 5));
        button.setOpaque(true);

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(255, 120, 40));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(255, 89, 0));
            }
        });
        return button;
    }

    private void loadUsers() {
        try {
            userListModel.clear();
            List<ProductRepository.ChatUser> users = ProductRepository.getRecentChatUsers(currentUserId);
            if (users.isEmpty()) {
                userListModel.addElement(new ProductRepository.ChatUser(-1, "Belum ada chat."));
                userList.setEnabled(false);
            } else {
                for (ProductRepository.ChatUser user : users) {
                    userListModel.addElement(user);
                }
                userList.setEnabled(true);
            }
        } catch (SQLException e) {
            System.err.println("Error loading chat users: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Gagal memuat daftar kontak chat.", "Error Database", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void startChatWith(int receiverId, String receiverUsername) {
        if (receiverId == currentUserId) {
            JOptionPane.showMessageDialog(this, "Tidak bisa chat dengan diri sendiri.", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        this.activeChatReceiverId = receiverId;
        this.activeChatReceiverUsername = receiverUsername;
        chatHeaderLabel.setText("Chat dengan " + receiverUsername);
        loadChatMessages();
        startRefreshTimer();

        for (int i = 0; i < userListModel.getSize(); i++) {
            if (userListModel.getElementAt(i).getId() == receiverId) {
                userList.setSelectedIndex(i);
                break;
            }
        }
        setVisible(true);
    }

    private void loadChatMessages() {
        if (activeChatReceiverId == -1) {
            chatArea.setText("<html><body>Pilih pengguna di daftar kiri untuk memulai chat.</body></html>");
            return;
        }

        try {
            List<ProductRepository.ChatMessage> messages = ProductRepository.getChatMessages(currentUserId, activeChatReceiverId);
            StringBuilder sb = new StringBuilder("<html><body style='font-family: Arial; font-size: 13pt; margin: 5px;'>");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

            for (ProductRepository.ChatMessage msg : messages) {
                String senderName = msg.getSenderUsername();
                String timestampStr = msg.getTimestamp().format(formatter);
                String messageHtml;

                if (msg.getSenderId() == currentUserId) {
                    messageHtml = "<p style='text-align: right; color: #4CAF50; margin: 3px 0;'>" +
                                  "<span style='font-size: 0.8em; color: gray;'>" + timestampStr + "</span> <b>Anda</b><br>" +
                                  "<span style='background-color: #E8F5E9; padding: 6px 12px; border-radius: 15px; display: inline-block; max-width: 80%; word-wrap: break-word; text-align: left;'>" +
                                  msg.getMessageText().replace("\n", "<br>") +
                                  "</span></p>";
                } else {
                    messageHtml = "<p style='text-align: left; color: #2196F3; margin: 3px 0;'>" +
                                  "<b>" + senderName + "</b> <span style='font-size: 0.8em; color: gray;'>" + timestampStr + "</span><br>" +
                                  "<span style='background-color: #E3F2FD; padding: 6px 12px; border-radius: 15px; display: inline-block; max-width: 80%; word-wrap: break-word;'>" +
                                  msg.getMessageText().replace("\n", "<br>") +
                                  "</span></p>";
                }
                sb.append(messageHtml);

                if (msg.getReceiverId() == currentUserId && !msg.isRead()) {
                    ProductRepository.markMessagesAsRead(currentUserId, msg.getSenderId());
                }
            }
            sb.append("</body></html>");
            chatArea.setText(sb.toString());

            JScrollBar vertical = chatScrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());

        } catch (SQLException e) {
            System.err.println("Error loading chat messages: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Gagal memuat pesan chat.", "Error Database", JOptionPane.ERROR_MESSAGE);
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
                if (activeChatReceiverId != -1) {
                    SwingUtilities.invokeLater(() -> loadChatMessages());
                }
            }
        }, 3000, 3000);
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
            }
            if (getOwner() != null) {
                int x = getOwner().getX() + getOwner().getWidth() - getWidth() - 20;
                int y = getOwner().getY() + getOwner().getHeight() - getHeight() - 20;
                setLocation(x, y);
            }
        }
    }

    public void openChatWithDefaultUser() {
        if (activeChatReceiverId == -1 && !userListModel.isEmpty()) {
            ProductRepository.ChatUser firstUser = userListModel.getElementAt(0);
            startChatWith(firstUser.getId(), firstUser.getUsername());
        } else if (userListModel.isEmpty()){
            chatHeaderLabel.setText("Belum ada chat.");
            chatArea.setText("<html><body>Mulai chat dari halaman produk.</body></html>");
        }
        toggleVisibility();
    }
}