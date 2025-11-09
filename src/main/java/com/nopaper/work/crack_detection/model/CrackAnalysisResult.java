/**
 * 
 */
package com.nopaper.work.crack_detection.model;

/**
 * 
 */

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrackAnalysisResult {
    private boolean cracksDetected;
    private int crackCount;
    private double totalCrackArea;
    private double crackPercentage;
    private String severity;
    private List<CrackRegion> crackRegions;
    private String processedImageBase64;
    private long processingTimeMs;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CrackRegion {
        private int x;
        private int y;
        private int width;
        private int height;
        private double area;
    }
}