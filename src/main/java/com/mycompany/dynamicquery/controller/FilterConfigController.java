package com.mycompany.dynamicquery.controller;

import com.mycompany.dynamicquery.entity.FilterConfig;
import com.mycompany.dynamicquery.service.FilterConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/filter-config")
public class FilterConfigController {

    @Autowired
    private FilterConfigService filterConfigService;

    @PostMapping
    public ResponseEntity<?> createConfig(@RequestBody FilterConfig config) {
        try {
            FilterConfig savedConfig = filterConfigService.saveConfig(config);
            return ResponseEntity.ok(savedConfig);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateConfig(@PathVariable Long id, @RequestBody FilterConfig config) {
        try {
            config.setId(id);
            FilterConfig updatedConfig = filterConfigService.saveConfig(config);
            return ResponseEntity.ok(updatedConfig);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConfig(@PathVariable Long id) {
        filterConfigService.deleteConfig(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<FilterConfig> getConfig(@PathVariable Long id) {
        FilterConfig config = filterConfigService.getConfigById(id);
        return config != null ? ResponseEntity.ok(config) : ResponseEntity.notFound().build();
    }

    @GetMapping
    public ResponseEntity<List<FilterConfig>> getAllConfigs() {
        List<FilterConfig> configs = filterConfigService.getAllConfigs();
        return ResponseEntity.ok(configs);
    }

    @GetMapping("/active")
    public ResponseEntity<List<FilterConfig>> getActiveConfigs() {
        List<FilterConfig> configs = filterConfigService.getAllActiveConfigs();
        return ResponseEntity.ok(configs);
    }

    @GetMapping("/examples")
    public ResponseEntity<List<String>> getExpressionExamples() {
        List<String> examples = filterConfigService.getExpressionExamples();
        return ResponseEntity.ok(examples);
    }

    @PostMapping("/validate")
    public ResponseEntity<String> validateExpression(@RequestBody String expression) {
        try {
            filterConfigService.validateFilterExpression(expression);
            return ResponseEntity.ok("表达式格式正确");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}