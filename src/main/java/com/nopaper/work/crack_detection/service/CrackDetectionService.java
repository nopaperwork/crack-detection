/**
 * 
 */
package com.nopaper.work.crack_detection.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.nopaper.work.crack_detection.config.CrackDetectionProperties;
import com.nopaper.work.crack_detection.model.CrackAnalysisResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrackDetectionService {
    
    private final CrackDetectionProperties properties;
    
    public CrackAnalysisResult detectCracks(MultipartFile file) throws IOException {
        long startTime = System.currentTimeMillis();
        
        // Save uploaded file temporarily
        Path tempFile = Files.createTempFile("crack-detection-", ".jpg");
        file.transferTo(tempFile.toFile());
        
        try {
            // Read image
            Mat originalImage = opencv_imgcodecs.imread(tempFile.toString());
            if (originalImage.empty()) {
                throw new IOException("Failed to read image");
            }
            
            // Process image
            Mat processedImage = processImage(originalImage);
            
            // Detect cracks
            List<CrackAnalysisResult.CrackRegion> crackRegions = findCrackRegions(processedImage);
            
            // Calculate statistics
            double totalArea = calculateTotalArea(crackRegions);
            double imageArea = originalImage.rows() * originalImage.cols();
            double crackPercentage = (totalArea / imageArea) * 100;
            
            // Determine severity
            String severity = determineSeverity(crackPercentage);
            
            // Draw results on original image
            Mat resultImage = drawCrackRegions(originalImage, crackRegions);
            
            // Convert to base64
            String base64Image = matToBase64(resultImage);
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            // Clean up
            originalImage.release();
            processedImage.release();
            resultImage.release();
            
            return CrackAnalysisResult.builder()
                .cracksDetected(!crackRegions.isEmpty())
                .crackCount(crackRegions.size())
                .totalCrackArea(totalArea)
                .crackPercentage(crackPercentage)
                .severity(severity)
                .crackRegions(crackRegions)
                .processedImageBase64(base64Image)
                .processingTimeMs(processingTime)
                .build();
                
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
    
    private Mat processImage(Mat image) {
        // Convert to grayscale
        Mat gray = new Mat();
        opencv_imgproc.cvtColor(image, gray, opencv_imgproc.COLOR_BGR2GRAY);
        
        // Apply Gaussian blur
        Mat blurred = new Mat();
        Size kernelSize = new Size(properties.getBlurKernelSize(), properties.getBlurKernelSize());
        opencv_imgproc.GaussianBlur(gray, blurred, kernelSize, 0);
        
        // Apply adaptive threshold
        Mat thresh = new Mat();
        opencv_imgproc.adaptiveThreshold(
            blurred, thresh, 255,
            opencv_imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            opencv_imgproc.THRESH_BINARY_INV,
            11, 2
        );
        
        // Apply Canny edge detection
        Mat edges = new Mat();
        opencv_imgproc.Canny(
            blurred, edges,
            properties.getCannyLowThreshold(),
            properties.getCannyHighThreshold()
        );
        
        // Morphological operations
        Mat morphKernel = opencv_imgproc.getStructuringElement(
            opencv_imgproc.MORPH_RECT,
            new Size(properties.getMorphologyKernelSize(), properties.getMorphologyKernelSize())
        );
        
        Mat dilated = new Mat();
        opencv_imgproc.dilate(edges, dilated, morphKernel, new Point(-1, -1),
            properties.getDilationIterations(), opencv_core.BORDER_CONSTANT, opencv_imgproc.morphologyDefaultBorderValue());
        
        Mat eroded = new Mat();
        opencv_imgproc.erode(dilated, eroded, morphKernel, new Point(-1, -1),
            properties.getErosionIterations(), opencv_core.BORDER_CONSTANT, opencv_imgproc.morphologyDefaultBorderValue());
        
        // Clean up intermediate Mats
        gray.release();
        blurred.release();
        thresh.release();
        edges.release();
        morphKernel.release();
        dilated.release();
        
        return eroded;
    }
    
    private List<CrackAnalysisResult.CrackRegion> findCrackRegions(Mat processedImage) {
        List<CrackAnalysisResult.CrackRegion> regions = new ArrayList<>();
        
        MatVector contours = new MatVector();
        Mat hierarchy = new Mat();
        
        opencv_imgproc.findContours(
            processedImage,
            contours,
            hierarchy,
            opencv_imgproc.RETR_EXTERNAL,
            opencv_imgproc.CHAIN_APPROX_SIMPLE
        );
        
        for (int i = 0; i < contours.size(); i++) {
            Mat contour = contours.get(i);
            double area = opencv_imgproc.contourArea(contour);
            
            if (area >= properties.getMinCrackArea()) {
                Rect boundingRect = opencv_imgproc.boundingRect(contour);
                
                regions.add(CrackAnalysisResult.CrackRegion.builder()
                    .x(boundingRect.x())
                    .y(boundingRect.y())
                    .width(boundingRect.width())
                    .height(boundingRect.height())
                    .area(area)
                    .build());
            }
            contour.release();
        }
        
        contours.close();
        hierarchy.release();
        
        return regions;
    }
    
    private double calculateTotalArea(List<CrackAnalysisResult.CrackRegion> regions) {
        return regions.stream()
            .mapToDouble(CrackAnalysisResult.CrackRegion::getArea)
            .sum();
    }
    
    private String determineSeverity(double crackPercentage) {
        if (crackPercentage < 1) return "Low";
        if (crackPercentage < 5) return "Medium";
        return "High";
    }
    
    private Mat drawCrackRegions(Mat image, List<CrackAnalysisResult.CrackRegion> regions) {
        Mat result = image.clone();
        
        Scalar red = new Scalar(0, 0, 255, 0);
        
        for (CrackAnalysisResult.CrackRegion region : regions) {
            opencv_imgproc.rectangle(
                result,
                new Point(region.getX(), region.getY()),
                new Point(region.getX() + region.getWidth(), region.getY() + region.getHeight()),
                red,
                2,
                opencv_imgproc.LINE_8,
                0
            );
        }
        
        red.close();
        return result;
    }
    
    private String matToBase64(Mat mat) throws IOException {
        Path tempOutput = Files.createTempFile("result-", ".png");
        opencv_imgcodecs.imwrite(tempOutput.toString(), mat);
        
        byte[] imageBytes = Files.readAllBytes(tempOutput);
        Files.deleteIfExists(tempOutput);
        
        return Base64.getEncoder().encodeToString(imageBytes);
    }
}