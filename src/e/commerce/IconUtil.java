package e.commerce;

import javax.swing.*;
import java.awt.*;

public class IconUtil {
    public static void setIcon(JFrame frame) {
        try {
            // Path relatif terhadap classpath (misalnya, file di src/Resources/Images/)
            String iconPath = "/Resources/Images/Quantra-Icon.png";
            System.out.println("Mencoba load ikon dari: " + iconPath);

            // Gunakan ClassLoader untuk memuat resource
            ImageIcon icon = new ImageIcon(IconUtil.class.getResource(iconPath));
            if (icon.getImage() == null) {
                System.err.println("Ikon null, file tidak ditemukan!");
            } else {
                System.out.println("Ikon berhasil di-load!");
                frame.setIconImage(icon.getImage());
            }
        } catch (Exception e) {
            System.err.println("Error loading icon: " + e.getMessage());
        }
    }
}