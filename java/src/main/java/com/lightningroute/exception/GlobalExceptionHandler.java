package com.lightningroute.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the application
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle general exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception ex) {
        return createErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR, 
                "An unexpected error occurred", 
                ex.getMessage());
    }
    
    /**
     * Handle file size limit exceeded
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxSizeException(MaxUploadSizeExceededException ex) {
        return createErrorResponse(
                HttpStatus.PAYLOAD_TOO_LARGE, 
                "File size exceeds the maximum limit", 
                "Please upload a smaller file (maximum 10MB)");
    }
    
    /**
     * Handle invalid file format
     */
    @ExceptionHandler(InvalidFileFormatException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidFileFormatException(InvalidFileFormatException ex) {
        return createErrorResponse(
                HttpStatus.BAD_REQUEST, 
                "Invalid file format", 
                ex.getMessage());
    }
    
    /**
     * Handle OpenAI API errors
     */
    @ExceptionHandler(OpenAIApiException.class)
    public ResponseEntity<Map<String, Object>> handleOpenAIApiException(OpenAIApiException ex) {
        return createErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE, 
                "OpenAI API error", 
                ex.getMessage());
    }
    
    /**
     * Create a standardized error response
     */
    private ResponseEntity<Map<String, Object>> createErrorResponse(HttpStatus status, String error, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        
        return new ResponseEntity<>(body, status);
    }
    
    /**
     * Custom exception for invalid file formats
     */
    public static class InvalidFileFormatException extends RuntimeException {
        public InvalidFileFormatException(String message) {
            super(message);
        }
    }
    
    /**
     * Custom exception for OpenAI API errors
     */
    public static class OpenAIApiException extends RuntimeException {
        public OpenAIApiException(String message) {
            super(message);
        }
        
        public OpenAIApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
