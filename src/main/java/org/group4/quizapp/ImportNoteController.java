package org.group4.quizapp;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;


import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;


@Controller

public class ImportNoteController {

    @Autowired
    private AIService aiService;

    @Value("${spring.datasource.url}")
    private String databaseUrl;

    @Value("${spring.datasource.username}")
    private String databaseUsername;

    @Value("${spring.datasource.password}")
    private String databasePassword;

    @PostMapping("/upload-notes")
    public String handleUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam("action") String action,
            HttpSession session,
            Model model) {
        try {
            if (file.isEmpty()) {
                model.addAttribute("errorMessage", "Please select a file to upload.");
                return "Import-Notes-Page";
            }

            String fileContent;
            String fileType = file.getContentType();

            // Determine the file type and extract content
            if ("application/pdf".equals(fileType)) {
                fileContent = extractTextFromPdf(file);
            } else if ("application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(fileType)) {
                fileContent = extractTextFromDocx(file);
            } else if ("text/plain".equals(fileType)) {
                fileContent = new String(file.getBytes());
            } else {
                model.addAttribute("errorMessage", "Unsupported file type. Please upload .txt, .pdf, or .docx files.");
                return "Import-Notes-Page";
            }

            // Save file content to the database
            Long userId = (Long) session.getAttribute("id");
            saveNotesToDatabase(title, fileContent, userId);

            // Process with AI based on action
            if ("flashcards".equals(action)) {
                String flashcards = aiService.generateFlashcards(fileContent);
                model.addAttribute("successMessage", "Flashcards generated successfully!");
                model.addAttribute("flashcards", flashcards);

            } else if ("quiz".equals(action)) {
                String questions = aiService.generateQuestions(fileContent);
                model.addAttribute("successMessage", "Quiz generated successfully!");
                model.addAttribute("questions", questions);

            } else {
                model.addAttribute("errorMessage", "Invalid action. Please select 'Flashcard me' or 'Quiz me'.");
            }

        } catch (IOException e) {
            model.addAttribute("errorMessage", "Error processing the file: " + e.getMessage());
        } catch (SQLException e) {
            model.addAttribute("errorMessage", "Database error: " + e.getMessage());
        }

        return "Import-Notes-Page";
    }

    public String extractTextFromPdf(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            return pdfStripper.getText(document);
        }
    }

    public String extractTextFromDocx(MultipartFile file) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(file.getInputStream())) {
            StringBuilder content = new StringBuilder();
            for (XWPFParagraph paragraph : doc.getParagraphs()) {
                content.append(paragraph.getText()).append("\n");
            }
            return content.toString();
        }
    }

    private void saveNotesToDatabase(String title, String content, Long userId) throws SQLException {
        // Default title if none is provided
        if (title == null || title.isEmpty()) {
            title = "Untitled Notes";
        }

        // Establish connection to the database
        try (Connection connection = DriverManager.getConnection(databaseUrl, databaseUsername, databasePassword)) {
            String query = "INSERT INTO notes (user_id, title, content) VALUES (?, ?, ?)";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setLong(1, userId);
                preparedStatement.setString(2, title);
                preparedStatement.setString(3, content);
                preparedStatement.executeUpdate();
            }
        }
    }
}
