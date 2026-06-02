package com.alness.schemadoc.data.service;

import java.util.List;

import com.alness.schemadoc.data.dto.TableSchema;

public interface DocumentExportService {
    byte[] generatePdf(List<TableSchema> schema, String schemaPattern, String headerBackgroundHex,
            String headerTextHex);

    byte[] generateExcel(List<TableSchema> schema, String schemaPattern);
}