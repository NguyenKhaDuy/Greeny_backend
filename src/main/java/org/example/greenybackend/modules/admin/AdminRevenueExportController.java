package org.example.greenybackend.modules.admin;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.greenybackend.modules.admin.dto.RevenueBreakdownResponse;
import org.example.greenybackend.modules.admin.dto.RevenueDashboardResponse;
import org.example.greenybackend.modules.admin.dto.RevenueProductContributionResponse;
import org.example.greenybackend.modules.admin.dto.RevenueRecentOrderResponse;
import org.example.greenybackend.modules.admin.dto.RevenueSlowProductResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminRevenueExportController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final AdminAnalyticsService analyticsService;

    public AdminRevenueExportController(AdminAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/admin/revenue/export/excel")
    public ResponseEntity<byte[]> exportRevenueExcel(
            @RequestParam(required = false, defaultValue = "14") Integer dashboardDays,
            @RequestParam(required = false, defaultValue = "14_days") String revenueRange,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate revenueStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate revenueEnd
    ) throws IOException {
        RevenueDashboardResponse dashboard = analyticsService.getRevenueDashboard(
                dashboardDays,
                revenueRange,
                revenueStart,
                revenueEnd
        );

        byte[] workbook = buildWorkbook(dashboard);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename("bao-cao-doanh-thu.xlsx").build());
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        return ResponseEntity.ok()
                .headers(headers)
                .body(workbook);
    }

    private byte[] buildWorkbook(RevenueDashboardResponse dashboard) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Báo cáo doanh thu");
            sheet.setDefaultColumnWidth(20);

            CellStyle titleStyle = titleStyle(workbook);
            CellStyle sectionStyle = sectionStyle(workbook);
            CellStyle headerStyle = headerStyle(workbook);
            CellStyle labelStyle = labelStyle(workbook);
            CellStyle valueStyle = valueStyle(workbook);

            int rowIndex = 0;
            Row titleRow = sheet.createRow(rowIndex++);
            titleRow.setHeightInPoints(24);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Báo cáo doanh thu Greeny");
            titleCell.setCellStyle(titleStyle);

            rowIndex = addPair(sheet, rowIndex, "Khoảng thời gian", dashboard.rangeLabel(), labelStyle, valueStyle);
            rowIndex = addPair(sheet, rowIndex, "Từ ngày", formatDate(dashboard.rangeStart()), labelStyle, valueStyle);
            rowIndex = addPair(sheet, rowIndex, "Đến ngày", formatDate(dashboard.rangeEnd()), labelStyle, valueStyle);
            rowIndex = addPair(sheet, rowIndex, "Kỳ so sánh", valueOrDefault(dashboard.comparisonLabel(), "Chưa có dữ liệu"), labelStyle, valueStyle);
            rowIndex++;

            rowIndex = addSection(sheet, rowIndex, "Chỉ số chính", sectionStyle);
            rowIndex = addTableHeader(sheet, rowIndex, headerStyle, "Chỉ số", "Giá trị", "Ghi chú");
            rowIndex = addTableRow(sheet, rowIndex, valueStyle, "Tổng doanh thu", formatMoney(dashboard.rangeRevenue()), trendLabel(dashboard.totalRevenueTrend()));
            rowIndex = addTableRow(sheet, rowIndex, valueStyle, "Hôm nay", formatMoney(dashboard.todayRevenue()), trendLabel(dashboard.todayRevenueTrend()));
            rowIndex = addTableRow(sheet, rowIndex, valueStyle, "Tháng này", formatMoney(dashboard.monthRevenue()), trendLabel(dashboard.monthRevenueTrend()));
            rowIndex = addTableRow(sheet, rowIndex, valueStyle, "Giá trị đơn trung bình", formatMoney(dashboard.rangeAverageOrderValue()), trendLabel(dashboard.averageOrderValueTrend()));
            rowIndex = addTableRow(sheet, rowIndex, valueStyle, "Đơn hủy", String.valueOf(safeInteger(dashboard.rangeCancelledOrders())), trendLabel(dashboard.cancelledOrdersTrend()));
            rowIndex++;

            rowIndex = addSection(sheet, rowIndex, "Phân bổ trạng thái đơn hàng", sectionStyle);
            rowIndex = addTableHeader(sheet, rowIndex, headerStyle, "Trạng thái", "Số đơn", "Doanh thu", "Tỉ trọng");
            for (RevenueBreakdownResponse status : dashboard.statusBreakdown()) {
                rowIndex = addTableRow(sheet, rowIndex, valueStyle, status.label(), String.valueOf(safeInteger(status.count())),
                        formatMoney(status.revenue()), safeInteger(status.percent()) + "%");
            }
            rowIndex++;

            rowIndex = addSection(sheet, rowIndex, "Doanh thu theo phương thức thanh toán", sectionStyle);
            rowIndex = addTableHeader(sheet, rowIndex, headerStyle, "Phương thức", "Số đơn", "Doanh thu", "Tỉ trọng");
            if (dashboard.paymentMethodBreakdown().isEmpty()) {
                rowIndex = addTableRow(sheet, rowIndex, valueStyle, "Chưa có dữ liệu", "", "", "");
            } else {
                for (RevenueBreakdownResponse method : dashboard.paymentMethodBreakdown()) {
                    rowIndex = addTableRow(sheet, rowIndex, valueStyle, method.label(), String.valueOf(safeInteger(method.count())),
                            formatMoney(method.revenue()), safeInteger(method.percent()) + "%");
                }
            }
            rowIndex++;

            rowIndex = addSection(sheet, rowIndex, "Top sản phẩm đóng góp doanh thu", sectionStyle);
            rowIndex = addTableHeader(sheet, rowIndex, headerStyle, "Sản phẩm", "Đã bán", "Tồn kho", "Doanh thu", "Đóng góp");
            for (RevenueProductContributionResponse product : dashboard.topRevenueProducts()) {
                rowIndex = addTableRow(sheet, rowIndex, valueStyle, valueOrDefault(product.plantTitle(), "Chưa có tên sản phẩm"),
                        String.valueOf(safeInteger(product.soldQuantity())), String.valueOf(safeInteger(product.stockQuantity())),
                        formatMoney(product.revenue()), safeInteger(product.contributionPercent()) + "%");
            }
            rowIndex++;

            rowIndex = addSection(sheet, rowIndex, "Sản phẩm bán chậm / tồn cao", sectionStyle);
            rowIndex = addTableHeader(sheet, rowIndex, headerStyle, "Sản phẩm", "Biến thể", "SKU", "Tồn", "Bán", "Gợi ý");
            for (RevenueSlowProductResponse product : dashboard.slowProducts()) {
                rowIndex = addTableRow(sheet, rowIndex, valueStyle, valueOrDefault(product.plantTitle(), "Chưa có tên sản phẩm"),
                        valueOrDefault(product.variantName(), "Chưa có dữ liệu"), valueOrDefault(product.sku(), "Chưa có SKU"),
                        String.valueOf(safeInteger(product.stockQuantity())), String.valueOf(safeInteger(product.soldQuantity())),
                        valueOrDefault(product.recommendation(), "Theo dõi thêm"));
            }
            rowIndex++;

            rowIndex = addSection(sheet, rowIndex, "Top giá trị đơn", sectionStyle);
            rowIndex = addTableHeader(sheet, rowIndex, headerStyle, "Mã đơn", "Khách hàng", "Thời gian", "Tổng tiền");
            for (RevenueRecentOrderResponse order : dashboard.recentPaidOrderSummaries()) {
                rowIndex = addTableRow(sheet, rowIndex, valueStyle, valueOrDefault(order.orderId(), "Chưa có mã"),
                        valueOrDefault(order.customerName(), "Khách hàng"), formatDateTime(order.paidAt()), formatMoney(order.totalPrice()));
            }

            for (int column = 0; column < 6; column++) {
                sheet.autoSizeColumn(column);
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private int addSection(Sheet sheet, int rowIndex, String title, CellStyle style) {
        Row row = sheet.createRow(rowIndex++);
        Cell cell = row.createCell(0);
        cell.setCellValue(title);
        cell.setCellStyle(style);
        return rowIndex;
    }

    private int addPair(Sheet sheet, int rowIndex, String label, String value, CellStyle labelStyle, CellStyle valueStyle) {
        Row row = sheet.createRow(rowIndex++);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(labelStyle);
        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value);
        valueCell.setCellStyle(valueStyle);
        return rowIndex;
    }

    private int addTableHeader(Sheet sheet, int rowIndex, CellStyle style, String... values) {
        Row row = sheet.createRow(rowIndex++);
        for (int index = 0; index < values.length; index++) {
            Cell cell = row.createCell(index);
            cell.setCellValue(values[index]);
            cell.setCellStyle(style);
        }
        return rowIndex;
    }

    private int addTableRow(Sheet sheet, int rowIndex, CellStyle style, String... values) {
        Row row = sheet.createRow(rowIndex++);
        for (int index = 0; index < values.length; index++) {
            Cell cell = row.createCell(index);
            cell.setCellValue(values[index]);
            cell.setCellStyle(style);
        }
        return rowIndex;
    }

    private CellStyle titleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        font.setColor(IndexedColors.GREEN.getIndex());
        style.setFont(font);
        return style;
    }

    private CellStyle sectionStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle headerStyle(Workbook workbook) {
        CellStyle style = borderedStyle(workbook);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle labelStyle(Workbook workbook) {
        CellStyle style = borderedStyle(workbook);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle valueStyle(Workbook workbook) {
        return borderedStyle(workbook);
    }

    private CellStyle borderedStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        return style;
    }

    private String formatMoney(BigDecimal value) {
        BigDecimal safeValue = value == null ? BigDecimal.ZERO : value;
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator('.');
        DecimalFormat formatter = new DecimalFormat("#,##0", symbols);
        return formatter.format(safeValue.setScale(0, RoundingMode.HALF_UP)) + " ₫";
    }

    private String formatDate(LocalDate value) {
        return value == null ? "Chưa có dữ liệu" : DATE_FORMATTER.format(value);
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "Chưa có dữ liệu" : DATE_TIME_FORMATTER.format(value);
    }

    private String trendLabel(org.example.greenybackend.modules.admin.dto.DashboardTrendResponse trend) {
        return trend == null ? "Chưa có dữ liệu" : valueOrDefault(trend.label(), "Chưa có dữ liệu");
    }

    private int safeInteger(Integer value) {
        return value == null ? 0 : value;
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
