package e.commerce;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;
import java.util.Random;

public class EmailSender {
    private static final String FROM_EMAIL = "quantraecommerce@gmail.com"; // Ganti dengan email Anda
    private static final String PASSWORD = "kyprudxuoindmwru"; // Ganti dengan password aplikasi email Anda
    
    // Metode untuk menghasilkan kode OTP acak 6 digit
    public static String generateOTP() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000); // 6 digit
        return String.valueOf(otp);
    }
    
    // Metode untuk mengirim email dengan kode OTP
    public static boolean sendOTPEmail(String toEmail, String otp) {
        // Konfigurasi properti SMTP untuk Gmail
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        
        // Buat sesi email
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, PASSWORD);
            }
        });
        
        try {
            // Buat pesan email
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Kode OTP untuk Verifikasi E-Commerce App");
            
            // Membuat konten email yang menarik
            String emailContent = 
                "<div style='font-family: Arial, sans-serif; padding: 20px; max-width: 600px; margin: 0 auto; border: 1px solid #ddd; border-radius: 5px;'>" +
                "<h2 style='color: #2b2b2b; text-align: center;'>Verifikasi Email E-Commerce App</h2>" +
                "<p style='font-size: 16px; color: #2b2b2b;'>Halo,</p>" +
                "<p style='font-size: 16px; color: #2b2b2b;'>Terima kasih telah mendaftar di aplikasi E-Commerce kami. Untuk menyelesaikan proses pendaftaran, masukkan kode OTP berikut:</p>" +
                "<div style='background-color: #f4f4f4; padding: 15px; text-align: center; margin: 20px 0;'>" +
                "<h1 style='font-size: 32px; letter-spacing: 5px; margin: 0; color: #2b2b2b;'>" + otp + "</h1>" +
                "</div>" +
                "<p style='font-size: 16px; color: #2b2b2b;'>Kode OTP ini akan kedaluwarsa dalam 5 menit.</p>" +
                "<p style='font-size: 16px; color: #2b2b2b;'>Jika Anda tidak melakukan pendaftaran, abaikan email ini.</p>" +
                "<p style='font-size: 16px; color: #2b2b2b;'>Terima kasih,<br>Tim E-Commerce App</p>" +
                "</div>";
            
            // Set konten email sebagai HTML
            message.setContent(emailContent, "text/html; charset=utf-8");
            
            // Kirim email
            Transport.send(message);
            return true;
        } catch (MessagingException e) {
            e.printStackTrace();
            return false;
        }
    }
}