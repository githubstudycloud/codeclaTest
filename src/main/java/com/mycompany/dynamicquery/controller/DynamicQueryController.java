package com.mycompany.dynamicquery.controller;

import com.mycompany.dynamicquery.entity.DataRecord;
import com.mycompany.dynamicquery.service.DynamicQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/data")
public class DynamicQueryController {

    @Autowired
    private DynamicQueryService dynamicQueryService;

    @PostMapping("/query")
    public ResponseEntity<List<DataRecord>> queryData(@RequestBody Map<String, List<String>> queryParams) {
        try {
            List<DataRecord> results = dynamicQueryService.queryByConditions(queryParams);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/count")
    public ResponseEntity<Integer> countData(@RequestBody Map<String, List<String>> queryParams) {
        try {
            int count = dynamicQueryService.countByConditions(queryParams);
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping
    public ResponseEntity<DataRecord> createRecord(@RequestBody DataRecord record) {
        DataRecord savedRecord = dynamicQueryService.saveRecord(record);
        return ResponseEntity.ok(savedRecord);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DataRecord> updateRecord(@PathVariable Long id, @RequestBody DataRecord record) {
        record.setId(id);
        DataRecord updatedRecord = dynamicQueryService.saveRecord(record);
        return ResponseEntity.ok(updatedRecord);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecord(@PathVariable Long id) {
        dynamicQueryService.deleteRecord(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<DataRecord> getRecord(@PathVariable Long id) {
        DataRecord record = dynamicQueryService.getRecordById(id);
        return record != null ? ResponseEntity.ok(record) : ResponseEntity.notFound().build();
    }

    @GetMapping
    public ResponseEntity<List<DataRecord>> getAllRecords() {
        List<DataRecord> records = dynamicQueryService.getAllRecords();
        return ResponseEntity.ok(records);
    }
}