# schemadoc-maker

API Spring Boot para consultar metadatos JDBC y descargar un diccionario de datos en JSON, PDF y Excel.

## Endpoints

Base path: usa el prefijo configurado en application.yml mediante PREFIX.

### 1. Consultar esquema en JSON

GET /{PREFIX}/schema-info?schema=dbo

Tambien acepta el parametro legado param:

GET /{PREFIX}/schema-info?param=dbo

### 2. Descargar PDF

GET /{PREFIX}/schema-info/pdf?schema=dbo

Respuesta:

- Content-Type: application/pdf
- Content-Disposition: attachment

El PDF genera una seccion por tabla, repite el encabezado de columnas cuando una tabla se parte entre paginas y evita forzar el corte tardio de filas.

### 3. Descargar Excel

GET /{PREFIX}/schema-info/excel?schema=dbo

Respuesta:

- Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
- Content-Disposition: attachment

El Excel genera una hoja de diccionario con bloques por tabla, encabezados visuales y fila superior congelada para facilitar la navegacion.