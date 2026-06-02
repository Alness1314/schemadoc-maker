package com.alness.schemadoc.data.controller;

import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alness.schemadoc.data.dto.TableSchema;
import com.alness.schemadoc.data.service.DocumentExportService;
import com.alness.schemadoc.data.service.SchemaService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("${api.prefix}/schema-info")
@RequiredArgsConstructor
public class DataController {
    private final SchemaService schemaService;
    private final DocumentExportService documentExportService;

    @GetMapping
    public ResponseEntity<List<TableSchema>> getInfoSchema(
            @RequestParam(name = "schema", required = false) String schema,
            @RequestParam(name = "param", required = false) String legacyParam) {
        String schemaPattern = resolveSchemaPattern(schema, legacyParam);
        List<TableSchema> response = schemaService.getSchema(schemaPattern);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> downloadPdf(
            @RequestParam(name = "schema", required = false) String schema,
            @RequestParam(name = "param", required = false) String legacyParam,
            @RequestParam(name = "headerBgColor", required = false) String headerBackgroundHex,
            @RequestParam(name = "headerTextColor", required = false) String headerTextHex) {
        String schemaPattern = resolveSchemaPattern(schema, legacyParam);
        List<TableSchema> response = schemaService.getSchema(schemaPattern);
        byte[] document = documentExportService.generatePdf(response, schemaPattern, headerBackgroundHex,
            headerTextHex);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(buildFileName(schemaPattern, "pdf"))
                        .build().toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(document);
    }

    @GetMapping(value = "/excel", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> downloadExcel(
            @RequestParam(name = "schema", required = false) String schema,
            @RequestParam(name = "param", required = false) String legacyParam) {
        String schemaPattern = resolveSchemaPattern(schema, legacyParam);
        List<TableSchema> response = schemaService.getSchema(schemaPattern);
        byte[] document = documentExportService.generateExcel(response, schemaPattern);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(buildFileName(schemaPattern, "xlsx"))
                        .build().toString())
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(document);
    }

    private String resolveSchemaPattern(String schema, String legacyParam) {
        if (schema != null && !schema.isBlank()) {
            return schema;
        }
        return legacyParam;
    }

    private String buildFileName(String schemaPattern, String extension) {
        String normalizedSchema = (schemaPattern == null || schemaPattern.isBlank()) ? "all" : schemaPattern;
        return "diccionario-" + normalizedSchema + "." + extension;
    }

}
