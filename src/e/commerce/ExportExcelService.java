package e.commerce;

import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.io.File;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExportExcelService {

    private static final String DEFAULT_EXCEL_FILENAME = "LaporanPenjualan.xlsx";

    // Field untuk menyimpan objek User yang sedang login
    private User currentUserForExport; 

    // Konstruktor default (tetap pertahankan)
    public ExportExcelService() {
        this(null); 
    }

    // Konstruktor baru yang menerima objek User
    public ExportExcelService(User currentUser) { 
        this.currentUserForExport = currentUser;
        System.out.println("DEBUG ExportExcelService: Initialized with user: " + (currentUser != null ? currentUser.getUsername() : "null"));
    }

    public void exportSalesDataToExcel(String filePath) throws SQLException, IOException {
        String outputPath = (filePath != null && !filePath.isEmpty()) ? filePath : getDefaultExcelPath();
        System.out.println("DEBUG ExportExcelService: Starting export to: " + outputPath);

        try (Connection conn = DatabaseConnection.getConnection(); 
             // Query sekarang didapatkan dari getSqlQuery() yang sudah ada logika filter
             PreparedStatement pstmt = conn.prepareStatement(getSqlQuery());
             ResultSet rs = pstmt.executeQuery()) {

            System.out.println("DEBUG ExportExcelService: Database query executed. Fetched ResultSet.");
            
            Workbook workbook = new XSSFWorkbook(); 
            Sheet sheet = workbook.createSheet("Laporan Penjualan"); 

            CellStyle dateCellStyle = workbook.createCellStyle();
            CreationHelper createHelper = workbook.getCreationHelper();
            dateCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-MM-dd HH:mm:ss"));

            ResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();
            List<String> columnNames = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                columnNames.add(rsmd.getColumnLabel(i));
            }

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < columnNames.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columnNames.get(i));
            }
            System.out.println("DEBUG ExportExcelService: Excel header row created.");

            int rowNum = 1;
            int dataRowsWritten = 0; 
            while (rs.next()) {
                Row dataRow = sheet.createRow(rowNum++);
                for (int i = 1; i <= columnCount; i++) {
                    Cell cell = dataRow.createCell(i - 1);
                    Object value = rs.getObject(i);

                    if (value instanceof java.sql.Date || value instanceof java.sql.Timestamp) {
                        cell.setCellValue((java.util.Date) value);
                        cell.setCellStyle(dateCellStyle);
                    } else if (value instanceof Number) {
                        cell.setCellValue(((Number) value).doubleValue());
                    } else if (value != null) {
                        cell.setCellValue(value.toString());
                    } else {
                        cell.setCellValue("");
                    }
                }
                dataRowsWritten++;
            }
            System.out.println("DEBUG ExportExcelService: " + dataRowsWritten + " data rows written to Excel sheet.");

            for (int i = 0; i < columnCount; i++) {
                sheet.autoSizeColumn(i);
            }
            System.out.println("DEBUG ExportExcelService: Columns auto-sized.");

            System.out.println("DEBUG ExportExcelService: Writing workbook to file at: " + outputPath);
            try (FileOutputStream outputStream = new FileOutputStream(outputPath)) {
                workbook.write(outputStream);
                System.out.println("DEBUG ExportExcelService: Workbook written to FileOutputStream.");
            } finally {
                workbook.close(); 
                System.out.println("DEBUG ExportExcelService: Workbook closed.");
            }

            System.out.println("DEBUG ExportExcelService: Export process completed successfully.");

        } catch (SQLException e) {
            System.err.println("DEBUG ExportExcelService: Database Error during export: " + e.getMessage());
            e.printStackTrace();
            throw e; 
        } catch (IOException e) {
            System.err.println("DEBUG ExportExcelService: File I/O Error during export: " + e.getMessage());
            e.printStackTrace();
            throw e; 
        } catch (Throwable e) { 
            System.err.println("DEBUG ExportExcelService: An UNCAUGHT FATAL ERROR/EXCEPTION occurred: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            throw new SQLException("Critical export error: " + e.getMessage(), e); 
        }
    }

    /**
     * Mendapatkan query SQL untuk mengambil data penjualan berdasarkan peran pengguna.
     * - Admin/Operation Manager: Mengambil semua data penjualan.
     * - Supervisor (Seller): Mengambil data penjualan yang terkait dengan produk yang dia jual.
     *
     * @return String query SQL
     */
    private String getSqlQuery() {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append("o.id AS order_id, ");
        sql.append("o.order_number, ");
        sql.append("o.order_date, ");
        sql.append("o.total_amount, ");
        sql.append("o.shipping_service_name, ");
        sql.append("o.shipping_cost, ");
        sql.append("o.payment_method, ");
        sql.append("o.order_status, ");
        sql.append("o.delivery_estimate, ");
        sql.append("o.created_at AS order_created_at, ");
        sql.append("oi.id AS item_id, ");
        sql.append("oi.product_name, ");
        sql.append("oi.quantity, ");
        sql.append("oi.price_per_item ");
        sql.append("FROM orders o ");
        sql.append("JOIN order_items oi ON o.id = oi.order_id ");

        // Logika filtering berdasarkan peran pengguna
        if (currentUserForExport != null) {
            if (currentUserForExport.getRole().equalsIgnoreCase("supervisor")) {
                // Untuk supervisor, hanya tampilkan pesanan yang berisi produk mereka
                sql.append(" JOIN products p ON oi.product_id = p.product_id ");
                sql.append(" WHERE p.seller_id = ").append(currentUserForExport.getId()).append(" ");
                System.out.println("DEBUG ExportExcelService: Filtering export data by supervisor_id: " + currentUserForExport.getId());
            } else if (currentUserForExport.getRole().equalsIgnoreCase("admin") || currentUserForExport.getRole().equalsIgnoreCase("op_manager")) {
                // Untuk admin atau operation manager, tidak ada WHERE clause, akan mengambil semua data
                System.out.println("DEBUG ExportExcelService: Exporting all sales data for admin/manager role.");
            }
        } else {
            System.out.println("DEBUG ExportExcelService: No user context for export, exporting all data.");
        }

        sql.append("ORDER BY o.order_date DESC, o.order_number ASC"); 

        // Batasi hasil untuk debugging (hapus baris ini setelah debugging selesai untuk mengekspor semua data)
        // sql.append(" LIMIT 10"); // Pastikan ini dikomentari atau dihapus saat ingin semua data

        return sql.toString();
    }

    public String getDefaultExcelPath() {
        String userHome = System.getProperty("user.home");
        String finalPath = userHome + File.separator + "Downloads" + File.separator + DEFAULT_EXCEL_FILENAME;
        return finalPath;
    }
}