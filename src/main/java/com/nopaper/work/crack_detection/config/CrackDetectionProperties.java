/**
 * @package com.nopaper.work.crack_detection_service.config -> crack-detection-service
 * @author saikatbarman
 * @date 2025 09-Nov-2025 1:34:38 pm
 * @git 
 */
package com.nopaper.work.crack_detection.config;

/**
 * 
 */
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "crack.detection")
public class CrackDetectionProperties {
    
    private int thresholdOffset = 127;
    private int thresholdBlockSize = 100;
    private int blurKernelSize = 5;
    private int cannyLowThreshold = 50;
    private int cannyHighThreshold = 150;
    private int morphologyKernelSize = 3;
    private int minCrackArea = 100;
    private int dilationIterations = 2;
    private int erosionIterations = 1;
    private List<String> supportedFormats = List.of("jpg", "jpeg", "png", "bmp");
    private String outputFormat = "png";
    private int processingThreads = 4;
    private double gaussianSigma = 2.0;
    
    public boolean isSupportedFormat(String format) {
        return supportedFormats.contains(format.toLowerCase());
    }
}