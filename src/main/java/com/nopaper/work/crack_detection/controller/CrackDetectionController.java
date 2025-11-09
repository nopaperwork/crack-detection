/**
 * 
 */
package com.nopaper.work.crack_detection.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.nopaper.work.crack_detection.model.CrackAnalysisResult;
import com.nopaper.work.crack_detection.service.CrackDetectionService;

/**
 * 
 */
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/crack-detection")
@RequiredArgsConstructor
@Tag(name = "Crack Detection", description = "API for detecting cracks in images")
public class CrackDetectionController {
    
    private final CrackDetectionService crackDetectionService;
    
    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Check if the service is running")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Crack Detection Service");
        response.put("version", "1.0.0");
        return ResponseEntity.ok(response);
    }
    
    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Analyze image", description = "Detect cracks in uploaded image")
    public ResponseEntity<?> analyzeCracks(@RequestParam("image") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Please upload an image file"));
            }
            
            log.info("Processing image: {}", file.getOriginalFilename());
            CrackAnalysisResult result = crackDetectionService.detectCracks(file);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error processing image", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to process image: " + e.getMessage()));
        }
    }
}