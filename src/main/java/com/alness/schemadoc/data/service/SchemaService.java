package com.alness.schemadoc.data.service;

import java.util.List;

import com.alness.schemadoc.data.dto.TableSchema;

public interface SchemaService {
    public List<TableSchema> getSchema(String schemaPattern);
}
