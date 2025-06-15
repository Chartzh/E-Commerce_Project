package e.commerce;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ChatFloatingButton extends JPanel {
    private JLabel chatIconLabel;
    private ChatPopupUI chatPopup;
    private JFrame ownerFrame;
    private ViewController viewController; // <--- TAMBAH INI

    // REVISI KONSTRUKTOR: Menerima JFrame ownerFrame DAN ViewController
    public ChatFloatingButton(JFrame ownerFrame, ViewController viewController) { // <--- UBAH DI SINI
        this.ownerFrame = ownerFrame;
        this.viewController = viewController; // <--- SIMPAN REFERENSI VIEWCROLLER
        setOpaque(false);
        setLayout(new BorderLayout());
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        ImageIcon chatIcon = new ImageIcon(getClass().getClassLoader().getResource("Resources/Images/chat_icon_bubble.png"));
        if (chatIcon.getImageLoadStatus() != MediaTracker.COMPLETE) {
            System.err.println("Gagal memuat chat_icon_bubble.png, menggunakan teks.");
            chatIconLabel = new JLabel("Chat");
        } else {
            Image scaledImage = chatIcon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
            chatIconLabel = new JLabel(new ImageIcon(scaledImage));
        }

        chatIconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        chatIconLabel.setVerticalAlignment(SwingConstants.CENTER);

        JPanel buttonShapePanel = new JPanel();
        buttonShapePanel.setLayout(new BorderLayout());
        buttonShapePanel.setOpaque(false);
        buttonShapePanel.setPreferredSize(new Dimension(50, 50));
        buttonShapePanel.add(chatIconLabel, BorderLayout.CENTER);

        add(buttonShapePanel, BorderLayout.CENTER);

        buttonShapePanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (chatPopup == null) {
                    // TERUSKAN viewControler SEBAGAI ARGUMEN KEDUA
                    chatPopup = new ChatPopupUI(ownerFrame, viewController); // <--- UBAH DI SINI
                }
                chatPopup.toggleVisibility();
            }
        });

        setPreferredSize(new Dimension(50, 50));
    }

    public void setLocationBasedOnParent(int parentWidth, int parentHeight) {
        int x = parentWidth - getPreferredSize().width - 30;
        int y = parentHeight - getPreferredSize().height - 60;
        setLocation(x, y);
    }
}