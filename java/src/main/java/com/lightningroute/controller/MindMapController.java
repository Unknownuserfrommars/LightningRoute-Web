package com.lightningroute.controller;

import com.lightningroute.model.MindMap;
import com.lightningroute.model.OpenAIRequest;
import com.lightningroute.service.FileProcessingService;
import com.lightningroute.service.OpenAIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/mindmap")
@RequiredArgsConstructor
public class MindMapController {

    private final OpenAIService openAIService;
    private final FileProcessingService fileProcessingService;

    /**
     * Generate mind map from text input
     * 
     * @param request The OpenAI request containing text and model
     * @return The generated mind map
     */
    @PostMapping("/generate")
    public ResponseEntity<MindMap> generateMindMap(@RequestBody OpenAIRequest request) {
        try {
            MindMap mindMap = openAIService.generateMindMap(request.getText());
            return ResponseEntity.ok(mindMap);
        } catch (Exception e) {
            log.error("Error generating mind map", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * Generate mind map from uploaded file
     * 
     * @param file The uploaded file
     * @return The generated mind map
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MindMap> generateMindMapFromFile(@RequestParam("file") MultipartFile file) {
        try {
            String extractedText = fileProcessingService.processFile(file);
            MindMap mindMap = openAIService.generateMindMap(extractedText);
            return ResponseEntity.ok(mindMap);
        } catch (IOException e) {
            log.error("Error processing file", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(null);
        } catch (Exception e) {
            log.error("Error generating mind map from file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    /**
     * Service status endpoint
     * 
     * @return Service status message
     */
    @GetMapping("/status")
    public ResponseEntity<String> serviceStatus() {
        return ResponseEntity.ok("Mind Map API is running!");
    }
}
