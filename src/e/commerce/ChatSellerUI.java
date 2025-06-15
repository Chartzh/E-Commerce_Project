package e.commerce;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ChatSellerUI extends JPanel {
    private ViewController viewController;
    private int currentUserId; // ID pembeli yang sedang login
    private int sellerId;      // ID penjual yang diajak chat
    private String sellerUsername; // Username penjual yang diajak chat

    private JTextPane chatArea;
    private JTextField messageInputField;
    private JLabel chatHeaderLabel; // Untuk menampilkan nama penjual
    private JScrollPane chatScrollPane;

    private Timer refreshTimer; // Timer untuk polling pesan baru

    public ChatSellerUI(ViewController viewController, int sellerId, String sellerUsername) {
        this.viewController = viewController;
        this.sellerId = sellerId;
        this.sellerUsername = sellerUsername;
        this.currentUserId = Authentication.getCurrentUser().getId(); // Asumsi currentUser sudah diset

        if (this.currentUserId == this.sellerId) {
            // Ini untuk kasus seller melihat chatnya sendiri.
            // Anda bisa menangani ini, misalnya, dengan mengarahkan ke halaman inbox seller,
            // atau mencegah chat diri sendiri. Untuk demo, kita akan tampilkan pesan.
            setLayout(new BorderLayout());
            JLabel errorLabel = new JLabel("Tidak bisa chat dengan diri sendiri.", SwingConstants.CENTER);
            errorLabel.setFont(new Font("Arial", Font.BOLD, 16));
            errorLabel.setForeground(Color.RED);
            add(errorLabel, BorderLayout.CENTER);
            return; // Hentikan inisialisasi lebih lanjut
        }

        setLayout(new BorderLayout(10, 10));
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // --- Panel Kanan: Area Chat ---
        JPanel chatPanel = new JPanel(new BorderLayout(5, 5));
        chatPanel.setBackground(Color.WHITE);
        chatPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        chatHeaderLabel = new JLabel("Chat dengan " + sellerUsername, SwingConstants.CENTER);
        chatHeaderLabel.setFont(new Font("Arial", Font.BOLD, 18));
        chatHeaderLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
        chatHeaderLabel.setBackground(new Color(230, 230, 230));
        chatHeaderLabel.setOpaque(true);
        chatPanel.add(chatHeaderLabel, BorderLayout.NORTH);

        chatArea = new JTextPane();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Arial", Font.PLAIN, 14));
        chatArea.setContentType("text/html");
        chatArea.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        chatArea.setText("<html><body></body></html>");

        chatScrollPane = new JScrollPane(chatArea);
        chatScrollPane.setBorder(null);
        chatScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        chatPanel.add(chatScrollPane, BorderLayout.CENTER);

        JPanel messageInputPanel = new JPanel(new BorderLayout(5, 5));
        messageInputPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        messageInputPanel.setBackground(Color.WHITE);

        messageInputField = new JTextField();
        messageInputField.setFont(new Font("Arial", Font.PLAIN, 14));
        messageInputField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.GRAY),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        messageInputField.addActionListener(e -> sendMessage());

        JButton sendButton = new JButton("Kirim");
        sendButton.setFont(new Font("Arial", Font.BOLD, 14));
        sendButton.setBackground(new Color(255, 89, 0));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        sendButton.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        sendButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        sendButton.addActionListener(e -> sendMessage());

        messageInputPanel.add(messageInputField, BorderLayout.CENTER);
        messageInputPanel.add(sendButton, BorderLayout.EAST);

        chatPanel.add(messageInputPanel, BorderLayout.SOUTH);

        add(chatPanel, BorderLayout.CENTER);

        loadChatMessages(); // Muat pesan saat UI diinisialisasi
        startRefreshTimer(); // Mulai timer refresh pesan
    }

    private void loadChatMessages() {
        try {
            // Gunakan currentUserId sebagai userId1 dan sellerId sebagai userId2
            List<ProductRepository.ChatMessage> messages = ProductRepository.getChatMessages(currentUserId, sellerId); //
            StringBuilder sb = new StringBuilder("<html><body style='font-family: Arial; font-size: 14pt;'>");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

            for (ProductRepository.ChatMessage msg : messages) {
                String senderName = msg.getSenderUsername();
                String timestampStr = msg.getTimestamp().format(formatter);
                String messageHtml;

                if (msg.getSenderId() == currentUserId) {
                    // Pesan Anda (warna biru, rata kanan)
                    messageHtml = "<p style='text-align: right; color: blue; margin: 2px 0;'>" +
                                  "<b>Anda</b> <span style='font-size: 0.8em; color: gray;'>" + timestampStr + "</span><br>" +
                                  "<span style='background-color: #E0F2F7; padding: 5px 10px; border-radius: 10px; display: inline-block; max-width: 80%; word-wrap: break-word;'>" +
                                  msg.getMessageText().replace("\n", "<br>") +
                                  "</span></p>";
                } else {
                    // Pesan penjual (warna hitam, rata kiri)
                    messageHtml = "<p style='text-align: left; color: black; margin: 2px 0;'>" +
                                  "<b>" + senderName + "</b> <span style='font-size: 0.8em; color: gray;'>" + timestampStr + "</span><br>" +
                                  "<span style='background-color: #F0F0F0; padding: 5px 10px; border-radius: 10px; display: inline-block; max-width: 80%; word-wrap: break-word;'>" +
                                  msg.getMessageText().replace("\n", "<br>") +
                                  "</span></p>";
                }
                sb.append(messageHtml);

                // Tandai pesan yang baru diterima dari penjual sebagai sudah dibaca
                if (msg.getReceiverId() == currentUserId && !msg.isRead()) {
                    ProductRepository.markMessagesAsRead(currentUserId, msg.getSenderId()); //
                }
            }
            sb.append("</body></html>");
            chatArea.setText(sb.toString());

            // Gulir otomatis ke bagian bawah
            JScrollBar vertical = chatScrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());

        } catch (SQLException e) {
            System.err.println("Error loading chat messages: " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Gagal memuat pesan chat.", "Error Database", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sendMessage() {
        String messageText = messageInputField.getText().trim();
        if (messageText.isEmpty()) {
            return; // Jangan kirim pesan kosong
        }

        try {
            boolean success = ProductRepository.sendMessage(currentUserId, sellerId, messageText); //
            if (success) {
                messageInputField.setText(""); // Kosongkan input field
                loadChatMessages(); // Muat ulang pesan untuk menampilkan pesan yang baru dikirim
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
                SwingUtilities.invokeLater(() -> loadChatMessages());
            }
        }, 5000, 5000); // Polling setiap 5 detik
    }

    public void stopRefreshTimer() {
        if (refreshTimer != null) {
            refreshTimer.cancel();
            refreshTimer = null;
        }
    }
}