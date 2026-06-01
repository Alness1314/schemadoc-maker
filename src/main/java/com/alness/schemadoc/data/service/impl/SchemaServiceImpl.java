package com.alness.schemadoc.data.service.impl;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.alness.schemadoc.data.dto.ColumnInfo;
import com.alness.schemadoc.data.dto.TableSchema;
import com.alness.schemadoc.data.service.SchemaService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SchemaServiceImpl implements SchemaService {
    private final DataSource dataSource;

    /**
     * Obtiene todas las tablas "reales" (TABLE) y sus columnas del catálogo JDBC.
     * 
     * @param schemaPattern opcional: "dbo" (SQL Server), "public" (PostgreSQL),
     *                      etc. Si es null, trae todas.
     */
    @Override
    public List<TableSchema> getSchema(String schemaPattern) {
        Map<String, List<ColumnInfo>> tables = new LinkedHashMap<>();

        try (var conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();

            // Algunos motores usan "catalog" y "schema" de forma distinta:
            String catalog = conn.getCatalog(); // puede ser null (depende del motor)
            // Si tu motor requiere un catálogo distinto, podrías parametrizarlo.

            // 1) Listar las tablas
            try (ResultSet rsTables = meta.getTables(
                    catalog,
                    schemaPattern,
                    "%",
                    new String[] { "TABLE" })) {

                while (rsTables.next()) {
                    String tableSchema = rsTables.getString("TABLE_SCHEM");
                    String tableName = rsTables.getString("TABLE_NAME");

                    // Puedes filtrar esquemas del sistema aquí si lo deseas
                    // (por ejemplo, excluir "INFORMATION_SCHEMA", "pg_catalog", etc.)
                    if (isSystemSchema(tableSchema))
                        continue;

                    tables.put(tableName, new ArrayList<>());
                }
            }

            // 2) Por cada tabla, listar columnas
            for (String tableName : tables.keySet()) {
                try (ResultSet rsCols = meta.getColumns(
                        catalog, schemaPattern, tableName, "%")) {

                    List<ColumnInfo> cols = new ArrayList<>();
                    while (rsCols.next()) {
                        String colName = rsCols.getString("COLUMN_NAME");
                        String typeName = rsCols.getString("TYPE_NAME"); // ej: nvarchar, int, uniqueidentifier...
                        int size = rsCols.getInt("COLUMN_SIZE");
                        int decimalDigits = rsCols.getInt("DECIMAL_DIGITS");
                        int nullable = rsCols.getInt("NULLABLE");

                        String formattedType = formatType(typeName, size, decimalDigits);
                        boolean allowsNull = (nullable == DatabaseMetaData.columnNullable);

                        cols.add(new ColumnInfo(colName, formattedType, allowsNull));
                    }
                    tables.put(tableName, cols);
                }
            }

        } catch (SQLException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error leyendo metadatos JDBC", e);
        }

        // 3) Mapear al DTO final
        List<TableSchema> result = new ArrayList<>();
        tables.forEach((t, cols) -> result.add(new TableSchema(t, cols)));
        return result;
    }

    private boolean isSystemSchema(String schema) {
        if (schema == null)
            return false;
        String s = schema.toLowerCase(Locale.ROOT);
        return s.startsWith("information_schema") ||
                s.startsWith("pg_catalog") ||
                s.equals("sys");
    }

    private String formatType(String typeName, int size, int scale) {
        if (typeName == null)
            return null;
        String t = typeName.toLowerCase(Locale.ROOT);

        // Tipos que suelen llevar longitud
        Set<String> withLength = Set.of("varchar", "nvarchar", "char", "nchar", "varbinary", "binary", "bit varying",
                "character varying");

        // Tipos que suelen llevar precisión/escala
        Set<String> withPrecisionScale = Set.of("decimal", "numeric", "number");

        if (withPrecisionScale.contains(t)) {
            if (size > 0 && scale > 0)
                return typeName + "(" + size + "," + scale + ")";
            if (size > 0)
                return typeName + "(" + size + ")";
            return typeName; // sin precisión disponible
        }

        if (withLength.contains(t) && size > 0)
            return typeName + "(" + size + ")";

        // Para uniqueidentifier, datetime, date, timestamp, int, bigint, etc. sin
        // sufijo
        return typeName;
    }

}
