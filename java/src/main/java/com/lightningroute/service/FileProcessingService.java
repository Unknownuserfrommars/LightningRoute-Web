package com.lightningroute.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileProcessingService {

    /**
     * Process the uploaded file and extract text based on file type
     * 
     * @param file The uploaded file
     * @return Extracted text from the file
     * @throws IOException If there's an error processing the file
     */
    public String processFile(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new IOException("Invalid file name");
        }
        
        // Create a temp file to process
        Path tempPath = Files.createTempFile("mind-map-", fileName);
        File tempFile = tempPath.toFile();
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(file.getBytes());
        }
        
        String contentType = file.getContentType();
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        
        try {
            // Process different file types
            if ("pdf".equals(extension)) {
                return extractTextFromPdf(tempFile);
            } else if ("docx".equals(extension)) {
                return extractTextFromDocx(tempFile);
            } else if ("txt".equals(extension)) {
                return extractTextFromTxt(tempFile);
            } else if (contentType != null && contentType.startsWith("image/")) {
                return extractTextFromImage(tempFile);
            } else {
                throw new IOException("Unsupported file type: " + contentType);
            }
        } finally {
            // Clean up the temp file
            try {
                Files.deleteIfExists(tempPath);
            } catch (IOException e) {
                log.warn("Failed to delete temporary file: {}", tempPath, e);
            }
        }
    }
    
    private String extractTextFromPdf(File file) throws IOException {
        try (PDDocument document = PDDocument.load(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
    
    private String extractTextFromDocx(File file) throws IOException {
        try (XWPFDocument document = new XWPFDocument(Files.newInputStream(file.toPath()))) {
            XWPFWordExtractor extractor = new XWPFWordExtractor(document);
            return extractor.getText();
        }
    }
    
    private String extractTextFromTxt(File file) throws IOException {
        return new String(Files.readAllBytes(file.toPath()));
    }
    
    private String extractTextFromImage(File file) throws IOException {
        Tesseract tesseract = new Tesseract();
        try {
            // Set tessdata path if needed in production
            // tesseract.setDatapath("/path/to/tessdata");
            return tesseract.doOCR(file);
        } catch (TesseractException e) {
            throw new IOException("Failed to perform OCR on image", e);
        }
    }
}
