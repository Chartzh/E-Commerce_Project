package e.commerce;

import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.text.NumberFormat;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.awt.Color; // Tetap perlu ini untuk objek Color.LIGHT_GRAY, Color.WHITE, dll.

// Impor kelas-kelas dari Apache PDFBox
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

public class ExportPdfService {

    private static final String DEFAULT_PDF_FILENAME = "LaporanPenjualan.pdf";
    private User currentUserForExport;

    public ExportPdfService() {
        this(null);
    }

    public ExportPdfService(User currentUser) {
        this.currentUserForExport = currentUser;
        System.out.println("DEBUG ExportPdfService: Initialized with user: " + (currentUser != null ? currentUser.getUsername() : "null"));
    }

    public void exportSalesDataToPdf(String filePath) throws IOException, SQLException {
        String outputPath = (filePath != null && !filePath.isEmpty()) ? filePath : getDefaultPdfPath();
        System.out.println("DEBUG ExportPdfService: Starting PDF export to: " + outputPath);

        PDDocument document = new PDDocument();
        PDPageContentStream contentStream = null;

        try {
            // Ukuran Font dan Tinggi Baris DIKEMBALIKAN KE NILAI RAPINYA
            PDType1Font font = new PDType1Font(FontName.HELVETICA);
            PDType1Font fontBold = new PDType1Font(FontName.HELVETICA_BOLD);
            int fontSize = 8;     // <-- Kembali ke ukuran rapi
            int headerFontSize = 10; // <-- Kembali ke ukuran rapi

            float margin = 30;
            float rowHeight = 15;  // <-- Kembali ke tinggi baris rapi

            PDPage currentPage = new PDPage(PDRectangle.A4);
            document.addPage(currentPage);
            contentStream = new PDPageContentStream(document, currentPage);
            
            float yStart = currentPage.getMediaBox().getHeight() - margin;
            float tableWidth = currentPage.getMediaBox().getWidth() - (2 * margin);
            float yPosition = yStart;

            // --- Bagian Header Laporan (Judul, Nama Seller, Tanggal) ---
            String reportTitle = "Laporan Penjualan E-commerce";
            String subTitle = "Semua Penjualan";
            
            if (currentUserForExport != null) {
                if (currentUserForExport.getRole().equalsIgnoreCase("supervisor")) {
                    reportTitle = "Laporan Penjualan Seller";
                    subTitle = "Oleh: " + currentUserForExport.getUsername();
                } else if (currentUserForExport.getRole().equalsIgnoreCase("admin") || currentUserForExport.getRole().equalsIgnoreCase("op_manager")) {
                    reportTitle = "Laporan Penjualan E-commerce (Global)";
                    subTitle = "Untuk Admin/Manager";
                }
            }

            // Tulis judul utama
            contentStream.beginText();
            contentStream.setNonStrokingColor(0.0f, 0.0f, 0.0f); // <-- KEMBALI KE HITAM, Menggunakan float
            contentStream.setFont(fontBold, 18);
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText(reportTitle);
            contentStream.endText();
            yPosition -= 20;

            // Tulis sub judul (nama seller/view)
            contentStream.beginText();
            contentStream.setNonStrokingColor(0.0f, 0.0f, 0.0f); // <-- KEMBALI KE HITAM, Menggunakan float
            contentStream.setFont(font, 12);
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText(subTitle);
            contentStream.endText();
            yPosition -= 15;

            // Tulis tanggal laporan dibuat
            contentStream.beginText();
            contentStream.setNonStrokingColor(0.0f, 0.0f, 0.0f); // <-- KEMBALI KE HITAM, Menggunakan float
            contentStream.setFont(font, 10);
            String dateFormatted = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("id", "ID"))); // Perbaikan format pattern
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("Tanggal Laporan: " + dateFormatted);
            contentStream.endText();
            yPosition -= 30;

            // Query data dari database
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(getSqlQuery());
                 ResultSet rs = pstmt.executeQuery()) {

                ResultSetMetaData rsmd = rs.getMetaData();
                int columnCount = rsmd.getColumnCount(); 
                List<String> columnNames = new ArrayList<>();
                List<Float> columnWidths = new ArrayList<>();
                
                float[] relativeWidths = {
                    1.0f, // No. Pesanan
                    1.8f, // Tgl Pesanan
                    1.2f, // Total Harga
                    1.2f, // Status Pesanan
                    1.2f, // Layanan Kirim
                    1.0f, // Biaya Kirim
                    2.0f, // Nama Produk
                    0.8f, // Jumlah
                    1.2f  // Harga Item
                };
                
                if (relativeWidths.length != columnCount) {
                    throw new IllegalStateException("Jumlah lebar relatif (" + relativeWidths.length + 
                                                    ") tidak sesuai dengan jumlah kolom SQL (" + columnCount + "). " +
                                                    "Periksa getSqlQuery() dan relativeWidths[] di ExportPdfService.java.");
                }

                float totalRelativeWidths = 0;
                for (float rw : relativeWidths) {
                    totalRelativeWidths += rw;
                }
                float unitWidth = tableWidth / totalRelativeWidths;

                for (int i = 1; i <= columnCount; i++) {
                    String colName = rsmd.getColumnLabel(i); 
                    columnNames.add(colName);
                    columnWidths.add(relativeWidths[i-1] * unitWidth); // Gunakan lebar relatif
                }

                // --- Header Tabel PDF ---
                float currentX = margin;
                yPosition -= rowHeight; 
                
                if (yPosition < margin) { 
                    contentStream.close();
                    currentPage = new PDPage(PDRectangle.A4);
                    document.addPage(currentPage);
                    contentStream = new PDPageContentStream(document, currentPage);
                    yStart = currentPage.getMediaBox().getHeight() - margin;
                    yPosition = yStart - rowHeight; 
                }

                // Gambar header
                for (int i = 0; i < columnCount; i++) {
                    // Warna background header
                    contentStream.setNonStrokingColor(Color.LIGHT_GRAY.getRed() / 255.0f, Color.LIGHT_GRAY.getGreen() / 255.0f, Color.LIGHT_GRAY.getBlue() / 255.0f); // <-- PERBAIKAN WARNA
                    contentStream.addRect(currentX, yPosition, columnWidths.get(i), rowHeight);
                    contentStream.fill();

                    contentStream.setStrokingColor(0.0f, 0.0f, 0.0f); // <-- KEMBALI KE HITAM, Menggunakan float
                    contentStream.setLineWidth(0.5f);
                    contentStream.addRect(currentX, yPosition, columnWidths.get(i), rowHeight);
                    contentStream.stroke();

                    contentStream.beginText();
                    contentStream.setNonStrokingColor(0.0f, 0.0f, 0.0f); // <-- Teks header hitam
                    contentStream.setFont(fontBold, headerFontSize);
                    contentStream.newLineAtOffset(currentX + 3, yPosition + (rowHeight - headerFontSize) / 2);
                    contentStream.showText(trimTextToFitCell(columnNames.get(i), fontBold, headerFontSize, columnWidths.get(i) - 6)); // <-- Mengaktifkan trimTextToFitCell untuk header
                    contentStream.endText();
                    currentX += columnWidths.get(i);
                }

                NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");

                int dataRowsWritten = 0;
                while (rs.next()) {
                    currentX = margin;
                    yPosition -= rowHeight; 

                    if (yPosition < margin) {
                        contentStream.close(); 
                        currentPage = new PDPage(PDRectangle.A4); 
                        document.addPage(currentPage); 
                        contentStream = new PDPageContentStream(document, currentPage); 
                        yStart = currentPage.getMediaBox().getHeight() - margin; 
                        yPosition = yStart - rowHeight; 
                        
                        // Ulangi header di halaman baru
                        float newHeaderY = yPosition + rowHeight;
                        float tempX = margin;
                        for (int i = 0; i < columnCount; i++) {
                            contentStream.setNonStrokingColor(Color.LIGHT_GRAY.getRed() / 255.0f, Color.LIGHT_GRAY.getGreen() / 255.0f, Color.LIGHT_GRAY.getBlue() / 255.0f); // <-- PERBAIKAN WARNA
                            contentStream.addRect(tempX, newHeaderY - rowHeight, columnWidths.get(i), rowHeight);
                            contentStream.fill();

                            contentStream.setStrokingColor(0.0f, 0.0f, 0.0f); // <-- KEMBALI KE HITAM, Menggunakan float
                            contentStream.setLineWidth(0.5f);
                            contentStream.addRect(tempX, newHeaderY - rowHeight, columnWidths.get(i), rowHeight);
                            contentStream.stroke();

                            contentStream.beginText();
                            contentStream.setNonStrokingColor(0.0f, 0.0f, 0.0f); // <-- Teks header hitam
                            contentStream.setFont(fontBold, headerFontSize);
                            contentStream.newLineAtOffset(tempX + 3, newHeaderY - rowHeight + (rowHeight - headerFontSize) / 2);
                            contentStream.showText(trimTextToFitCell(columnNames.get(i), fontBold, headerFontSize, columnWidths.get(i) - 6)); // <-- Mengaktifkan trimTextToFitCell untuk header
                            contentStream.endText();
                            tempX += columnWidths.get(i);
                        }
                        yPosition = newHeaderY - rowHeight;
                    }

                    // Tambahkan warna latar belakang selang-seling
                    if (dataRowsWritten % 2 == 0) { 
                         contentStream.setNonStrokingColor(240.0f / 255.0f, 240.0f / 255.0f, 240.0f / 255.0f); // <-- PERBAIKAN WARNA
                    } else { 
                         contentStream.setNonStrokingColor(1.0f, 1.0f, 1.0f); // <-- PUTIH, Menggunakan float
                    }
                    contentStream.addRect(margin, yPosition, tableWidth, rowHeight);
                    contentStream.fill();


                    currentX = margin; 
                    for (int i = 1; i <= columnCount; i++) {
                        String cellValue = "";
                        Object value = rs.getObject(i);

                        String columnLabel = rsmd.getColumnLabel(i);

                        if (value instanceof java.sql.Date || value instanceof java.sql.Timestamp) {
                            cellValue = dateFormat.format((Date) value);
                        } else if (value instanceof Number) {
                            if (columnLabel.toLowerCase().contains("harga") || columnLabel.toLowerCase().contains("total") || columnLabel.toLowerCase().contains("biaya")) {
                                cellValue = currencyFormat.format(((Number) value).doubleValue());
                            } else {
                                cellValue = String.valueOf(value);
                            }
                        } else if (value != null) {
                            cellValue = value.toString();
                        }

                        contentStream.setStrokingColor(0.0f, 0.0f, 0.0f); // <-- KEMBALI KE HITAM, Menggunakan float
                        contentStream.setLineWidth(0.5f);
                        contentStream.addRect(currentX, yPosition, columnWidths.get(i - 1), rowHeight);
                        contentStream.stroke();

                        contentStream.beginText();
                        contentStream.setNonStrokingColor(0.0f, 0.0f, 0.0f); // <-- Teks data hitam
                        contentStream.setFont(font, fontSize);
                        float textX = currentX + 3; 
                        float textY = yPosition + (rowHeight - fontSize) / 2;
                        
                        // MENGAKTIFKAN KEMBALI trimTextToFitCell() UNTUK DATA
                        contentStream.newLineAtOffset(textX, textY);
                        contentStream.showText(trimTextToFitCell(cellValue, font, fontSize, columnWidths.get(i - 1) - 6)); 
                        contentStream.endText();
                        currentX += columnWidths.get(i - 1);
                    }
                    dataRowsWritten++;
                }
                System.out.println("DEBUG ExportPdfService: " + dataRowsWritten + " data rows written to PDF.");
            }

        } catch (SQLException | IOException e) {
            System.err.println("DEBUG ExportPdfService: Error during PDF export: " + e.getMessage());
            e.printStackTrace();
            throw e; 
        } catch (Throwable e) { 
            System.err.println("DEBUG ExportPdfService: An UNCAUGHT FATAL ERROR/EXCEPTION occurred during PDF export: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Critical PDF export error: " + e.getMessage(), e); 
        } finally {
            if (contentStream != null) {
                try {
                    contentStream.close();
                    System.out.println("DEBUG ExportPdfService: Final contentStream closed.");
                } catch (IOException e) {
                    System.err.println("DEBUG ExportPdfService: Error closing contentStream: " + e.getMessage());
                }
            }
            if (document != null) {
                try {
                    document.save(outputPath);
                    document.close();
                    System.out.println("DEBUG ExportPdfService: PDF document saved and closed.");
                } catch (IOException e) {
                    System.err.println("DEBUG ExportPdfService: Error saving/closing document: " + e.getMessage());
                }
            }
        }
        System.out.println("DEBUG ExportPdfService: PDF export process completed successfully.");
    }

    private String trimTextToFitCell(String text, PDType1Font font, float fontSize, float maxWidth) throws IOException {
        float textWidth = font.getStringWidth(text) / 1000 * fontSize;
        if (textWidth <= maxWidth) {
            return text;
        }

        String ellipsis = "...";
        float ellipsisWidth = font.getStringWidth(ellipsis) / 1000 * fontSize;
        if (ellipsisWidth >= maxWidth) { 
            return ""; 
        }

        StringBuilder trimmedText = new StringBuilder();
        float currentWidth = 0;
        for (char c : text.toCharArray()) {
            float charWidth = font.getStringWidth(String.valueOf(c)) / 1000 * fontSize;
            if (currentWidth + charWidth + ellipsisWidth < maxWidth) {
                trimmedText.append(c);
                currentWidth += charWidth;
            } else {
                break;
            }
        }
        return trimmedText.toString() + ellipsis;
    }

    /**
     * Mendapatkan query SQL untuk mengambil data penjualan berdasarkan peran pengguna.
     * Menggunakan alias kolom yang lebih ramah laporan dan hanya kolom yang penting.
     *
     * @return String query SQL
     */
    private String getSqlQuery() {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        // Kolom 'ID Pesanan' (o.id) DIHILANGKAN
        sql.append("o.order_number AS 'No. Pesanan', ");
        sql.append("DATE_FORMAT(o.order_date, '%Y-%m-%d %H:%i:%s') AS 'Tgl Pesanan', ");
        sql.append("o.total_amount AS 'Total Harga', ");
        sql.append("o.order_status AS 'Status Pesanan', ");
        sql.append("o.shipping_service_name AS 'Layanan Kirim', ");
        sql.append("o.shipping_cost AS 'Biaya Kirim', ");
        sql.append("oi.product_name AS 'Nama Produk', ");
        sql.append("oi.quantity AS 'Jumlah', ");
        sql.append("oi.price_per_item AS 'Harga Item' ");
        sql.append("FROM orders o ");
        sql.append("JOIN order_items oi ON o.id = oi.order_id ");

        // Logika filtering berdasarkan peran pengguna
        if (currentUserForExport != null) {
            if (currentUserForExport.getRole().equalsIgnoreCase("supervisor")) {
                sql.append(" JOIN products p ON oi.product_id = p.product_id ");
                sql.append(" WHERE p.seller_id = ").append(currentUserForExport.getId()).append(" ");
                System.out.println("DEBUG ExportPdfService: Filtering PDF export data by supervisor_id: " + currentUserForExport.getId());
            } else if (currentUserForExport.getRole().equalsIgnoreCase("admin") || currentUserForExport.getRole().equalsIgnoreCase("op_manager")) {
                System.out.println("DEBUG ExportPdfService: Exporting all sales data to PDF for admin/manager role.");
            }
        } else {
            System.out.println("DEBUG ExportPdfService: No user context for PDF export, exporting all data.");
        }

        sql.append("ORDER BY o.order_date DESC, o.order_number ASC"); 

        return sql.toString();
    }

    public String getDefaultPdfPath() {
        String userHome = System.getProperty("user.home");
        return userHome + File.separator + "Downloads" + File.separator + DEFAULT_PDF_FILENAME;
    }
}