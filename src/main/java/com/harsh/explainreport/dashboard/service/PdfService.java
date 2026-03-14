package com.harsh.explainreport.dashboard.service;

import com.harsh.explainreport.dashboard.exception.PdfScanException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PdfService {

    private static final Pattern WORD_PATTERN = Pattern.compile("\\p{L}{2,}");

    public String extractText(MultipartFile file) {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            validateExtractedText(text);
            return text;
        } catch (PdfScanException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("PDF Read Error");
        }
    }

    private void validateExtractedText(String text) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty()) {
            throw new PdfScanException("The PDF appears to be image-only. Please upload a text-based PDF or enable OCR.");
        }

        int wordCount = 0;
        Matcher matcher = WORD_PATTERN.matcher(trimmed);
        while (matcher.find()) {
            wordCount++;
        }

        int nonWhitespaceCount = 0;
        int letterCount = 0;
        int letterOrDigitCount = 0;
        for (int i = 0; i < trimmed.length(); i++) {
            char ch = trimmed.charAt(i);
            if (!Character.isWhitespace(ch)) {
                nonWhitespaceCount++;
                if (Character.isLetter(ch)) {
                    letterCount++;
                }
                if (Character.isLetterOrDigit(ch)) {
                    letterOrDigitCount++;
                }
            }
        }

        double letterRatio = nonWhitespaceCount == 0 ? 0.0 : (double) letterCount / nonWhitespaceCount;
        double signalRatio = nonWhitespaceCount == 0 ? 0.0 : (double) letterOrDigitCount / nonWhitespaceCount;

        boolean tooShort = wordCount < 5 && letterOrDigitCount < 20;
        boolean tooNoisy = signalRatio < 0.12 && letterOrDigitCount < 60 && letterRatio < 0.08;

        if (tooShort || tooNoisy) {
            throw new PdfScanException("Not able to scan the PDF. Please upload a clearer report.");
        }
    }
}

