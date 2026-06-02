package com.alness.schemadoc.data.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.alness.schemadoc.data.dto.ColumnInfo;
import com.alness.schemadoc.data.dto.TableSchema;
import com.alness.schemadoc.data.service.DocumentExportService;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

@Service
public class DocumentExportServiceImpl implements DocumentExportService {
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final java.awt.Color DEFAULT_HEADER_BACKGROUND_COLOR = new java.awt.Color(51, 51, 51);
    private static final java.awt.Color DEFAULT_HEADER_TEXT_COLOR = java.awt.Color.WHITE;
    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^[0-9a-fA-F]{6}$");

    @Override
    public byte[] generatePdf(List<TableSchema> schema, String schemaPattern, String headerBackgroundHex,
            String headerTextHex) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            java.awt.Color headerBackgroundColor = parseColorOrDefault(headerBackgroundHex,
                    DEFAULT_HEADER_BACKGROUND_COLOR);
            java.awt.Color headerTextColor = parseColorOrDefault(headerTextHex, DEFAULT_HEADER_TEXT_COLOR);

            Document document = new Document(PageSize.A4, 36, 36, 54, 36);
            PdfWriter.getInstance(document, outputStream);
            document.open();

            addPdfTitle(document, schemaPattern, headerBackgroundColor, headerTextColor);

            for (int index = 0; index < schema.size(); index++) {
                TableSchema tableSchema = schema.get(index);
                addPdfTableSection(document, tableSchema, headerBackgroundColor, headerTextColor);
                if (index < schema.size() - 1) {
                    document.newPage();
                }
            }

            document.close();
            return outputStream.toByteArray();
        } catch (DocumentException | IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible generar el PDF del diccionario de datos", exception);
        }
    }

    @Override
    public byte[] generateExcel(List<TableSchema> schema, String schemaPattern) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XSSFCellStyle titleStyle = createTitleStyle(workbook);
            XSSFCellStyle tableHeaderStyle = createTableHeaderStyle(workbook);
            XSSFCellStyle columnHeaderStyle = createColumnHeaderStyle(workbook);
            XSSFCellStyle contentStyle = createContentStyle(workbook);
            XSSFCellStyle booleanStyle = createBooleanStyle(workbook);

            XSSFSheet sheet = workbook.createSheet("Diccionario");
            sheet.setDisplayGridlines(false);
            sheet.createFreezePane(0, 3);

            int rowIndex = 0;
            var titleRow = sheet.createRow(rowIndex++);
            titleRow.setHeightInPoints(28);
            createCell(titleRow, 0, buildDocumentTitle(schemaPattern), titleStyle);

            var metadataRow = sheet.createRow(rowIndex++);
            createCell(metadataRow, 0, "Generado: " + LocalDateTime.now().format(TIMESTAMP_FORMAT), contentStyle);

            rowIndex++;

            for (TableSchema tableSchema : schema) {
                var tableRow = sheet.createRow(rowIndex++);
                tableRow.setHeightInPoints(22);
                createCell(tableRow, 0, "Tabla: " + tableSchema.getTableName(), tableHeaderStyle);

                var headerRow = sheet.createRow(rowIndex++);
                createCell(headerRow, 0, "Columna", columnHeaderStyle);
                createCell(headerRow, 1, "Tipo", columnHeaderStyle);
                createCell(headerRow, 2, "Permite nulos", columnHeaderStyle);

                for (ColumnInfo columnInfo : tableSchema.getTableInfo()) {
                    var dataRow = sheet.createRow(rowIndex++);
                    createCell(dataRow, 0, columnInfo.getColumName(), contentStyle);
                    createCell(dataRow, 1, columnInfo.getDataType(), contentStyle);
                    createCell(dataRow, 2, columnInfo.isAllowNulls() ? "Si" : "No", booleanStyle);
                }

                rowIndex++;
            }

            sheet.setRepeatingRows(org.apache.poi.ss.util.CellRangeAddress.valueOf("1:3"));
            sheet.setColumnWidth(0, 24 * 256);
            sheet.setColumnWidth(1, 22 * 256);
            sheet.setColumnWidth(2, 18 * 256);

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible generar el Excel del diccionario de datos", exception);
        }
    }

    private void addPdfTitle(Document document, String schemaPattern, java.awt.Color headerBackgroundColor,
            java.awt.Color headerTextColor) throws DocumentException {
        Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD, headerTextColor);
        Font subtitleFont = new Font(Font.HELVETICA, 10, Font.NORMAL, headerTextColor);

        PdfPTable titleTable = new PdfPTable(1);
        titleTable.setWidthPercentage(100);
        titleTable.setSpacingAfter(18f);

        titleTable.addCell(createPdfTitleCell(buildDocumentTitle(schemaPattern), titleFont,
                headerBackgroundColor));
        titleTable.addCell(createPdfTitleCell("Generado: " + LocalDateTime.now().format(TIMESTAMP_FORMAT), subtitleFont,
                headerBackgroundColor));

        document.add(titleTable);
    }

    private void addPdfTableSection(Document document, TableSchema tableSchema, java.awt.Color headerBackgroundColor,
            java.awt.Color headerTextColor) throws DocumentException {
        Font tableTitleFont = new Font(Font.HELVETICA, 12, Font.BOLD, headerTextColor);
        PdfPTable tableTitle = new PdfPTable(1);
        tableTitle.setWidthPercentage(100);
        tableTitle.setSpacingBefore(6f);
        tableTitle.setSpacingAfter(10f);
        tableTitle.addCell(createPdfTitleCell("Tabla: " + tableSchema.getTableName(), tableTitleFont,
                headerBackgroundColor));
        document.add(tableTitle);

        PdfPTable pdfTable = new PdfPTable(new float[] { 3.5f, 2.5f, 1.5f });
        pdfTable.setWidthPercentage(100);
        pdfTable.setHeaderRows(1);
        pdfTable.setSplitRows(true);
        pdfTable.setSplitLate(false);
        pdfTable.setKeepTogether(false);

        pdfTable.addCell(createPdfHeaderCell("Columna", headerBackgroundColor, headerTextColor));
        pdfTable.addCell(createPdfHeaderCell("Tipo", headerBackgroundColor, headerTextColor));
        pdfTable.addCell(createPdfHeaderCell("Permite nulos", headerBackgroundColor, headerTextColor));

        for (ColumnInfo columnInfo : tableSchema.getTableInfo()) {
            pdfTable.addCell(createPdfBodyCell(columnInfo.getColumName()));
            pdfTable.addCell(createPdfBodyCell(columnInfo.getDataType()));
            pdfTable.addCell(createPdfBodyCell(columnInfo.isAllowNulls() ? "Si" : "No"));
        }

        document.add(pdfTable);
    }

    private PdfPCell createPdfHeaderCell(String value, java.awt.Color headerBackgroundColor,
            java.awt.Color headerTextColor) {
        Font font = new Font(Font.HELVETICA, 10, Font.BOLD, headerTextColor);
        PdfPCell cell = new PdfPCell(new Phrase(value, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(headerBackgroundColor);
        cell.setPadding(8);
        return cell;
    }

    private PdfPCell createPdfTitleCell(String value, Font font, java.awt.Color backgroundColor) {
        PdfPCell cell = new PdfPCell(new Phrase(value, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBackgroundColor(backgroundColor);
        cell.setBorderWidth(0);
        cell.setPadding(8);
        return cell;
    }

    private PdfPCell createPdfBodyCell(String value) {
        Font font = new Font(Font.HELVETICA, 9, Font.NORMAL);
        PdfPCell cell = new PdfPCell(new Phrase(value == null ? "" : value, font));
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6);
        cell.setMinimumHeight(18f);
        return cell;
    }

    private XSSFCellStyle createTitleStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        XSSFFont font = workbook.createFont();
        font.setBold(true);
        font.setFontHeight(16);
        style.setFont(font);
        return style;
    }

    private XSSFCellStyle createTableHeaderStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        XSSFFont font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        font.setFontHeight(12);
        style.setFont(font);
        return style;
    }

    private XSSFCellStyle createColumnHeaderStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);

        XSSFFont font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private XSSFCellStyle createContentStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        return style;
    }

    private XSSFCellStyle createBooleanStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = createContentStyle(workbook);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private void createCell(org.apache.poi.ss.usermodel.Row row, int columnIndex, String value, XSSFCellStyle style) {
        var cell = row.createCell(columnIndex);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
    }

    private String buildDocumentTitle(String schemaPattern) {
        if (schemaPattern == null || schemaPattern.isBlank()) {
            return "Diccionario de datos";
        }
        return "Diccionario de datos - schema " + schemaPattern;
    }

    private java.awt.Color parseColorOrDefault(String colorHex, java.awt.Color fallbackColor) {
        if (colorHex == null || colorHex.isBlank()) {
            return fallbackColor;
        }

        String normalizedColor = colorHex.trim();
        if (normalizedColor.startsWith("#")) {
            normalizedColor = normalizedColor.substring(1);
        }

        if (!HEX_COLOR_PATTERN.matcher(normalizedColor).matches()) {
            return fallbackColor;
        }

        try {
            return new java.awt.Color(Integer.parseInt(normalizedColor, 16));
        } catch (NumberFormatException exception) {
            return fallbackColor;
        }
    }
}