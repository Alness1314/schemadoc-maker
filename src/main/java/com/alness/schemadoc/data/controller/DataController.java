package com.alness.schemadoc.data.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alness.schemadoc.data.dto.TableSchema;
import com.alness.schemadoc.data.service.SchemaService;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("${api.prefix}/schema-info")
@RequiredArgsConstructor
public class DataController {
    private final SchemaService schemaService;

    @GetMapping
    public ResponseEntity<List<TableSchema>> getInfoSchema(@RequestParam String param) {
        List<TableSchema> response = schemaService.getSchema(param);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

}
