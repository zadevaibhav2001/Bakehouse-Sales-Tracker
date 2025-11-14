package com.example.myapp.service;

import com.example.myapp.dto.Order;
import com.example.myapp.dto.Product;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
public class ExcelExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    /**
     * Generate Excel file for orders
     */
    public byte[] generateOrdersExcel(List<Order> orders) throws IOException {
        log.info("Generating Excel file for {} orders", orders.size());

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Orders");

            // Create header style
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Order ID", "Product Name", "Product Price", "Quantity", "Total Cost", "Order Date", "Created At"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Fill data rows
            int rowNum = 1;
            for (Order order : orders) {
                Row row = sheet.createRow(rowNum++);
                
                row.createCell(0).setCellValue(order.orderId().toString());
                row.createCell(1).setCellValue(order.product().name());
                
                Cell priceCell = row.createCell(2);
                priceCell.setCellValue(order.product().price());
                priceCell.setCellStyle(currencyStyle);
                
                row.createCell(3).setCellValue(order.quantity());
                
                Cell totalCostCell = row.createCell(4);
                totalCostCell.setCellValue(order.totalCost());
                totalCostCell.setCellStyle(currencyStyle);
                
                Cell orderDateCell = row.createCell(5);
                orderDateCell.setCellValue(formatInstant(order.orderDateTime()));
                orderDateCell.setCellStyle(dateStyle);
                
                Cell createdAtCell = row.createCell(6);
                createdAtCell.setCellValue(formatInstant(order.createdAt()));
                createdAtCell.setCellStyle(dateStyle);
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * Generate Excel file for products
     */
    public byte[] generateProductsExcel(List<Product> products) throws IOException {
        log.info("Generating Excel file for {} products", products.size());

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Products");

            // Create header style
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "Name", "Price", "In Stock", "Created At", "Updated At"};
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Fill data rows
            int rowNum = 1;
            for (Product product : products) {
                Row row = sheet.createRow(rowNum++);
                
                row.createCell(0).setCellValue(product.id());
                row.createCell(1).setCellValue(product.name());
                
                Cell priceCell = row.createCell(2);
                priceCell.setCellValue(product.price());
                priceCell.setCellStyle(currencyStyle);
                
                row.createCell(3).setCellValue(product.inStock() ? "Yes" : "No");
                
                Cell createdAtCell = row.createCell(4);
                createdAtCell.setCellValue(formatInstant(product.createdAt()));
                createdAtCell.setCellStyle(dateStyle);
                
                Cell updatedAtCell = row.createCell(5);
                updatedAtCell.setCellValue(formatInstant(product.updatedAt()));
                updatedAtCell.setCellStyle(dateStyle);
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("₹#,##0.00"));
        return style;
    }

    private CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        return style;
    }

    private String formatInstant(Instant instant) {
        return instant != null ? DATE_FORMATTER.format(instant) : "";
    }
}
