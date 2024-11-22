package org.group4.quizapp;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Controller
public class ImportNoteController {

    @Value("${spring.datasource.url}")
    private String databaseUrl;

    @Value("${spring.datasource.username}")
    private String databaseUsername;

    @Value("${spring.datasource.password}")
    private String databasePassword;

    @GetMapping("/upload-notes")
    public String uploadNotes(HttpSession session, Model model) {
        String username = (String) session.getAttribute("username");
        String email = (String) session.getAttribute("userEmail");

        if (username == null) {
            return "redirect:/login"; // Redirect to log in if session is invalid
        }
        return "Import-Notes-Page";
    }

    @PostMapping("/upload-notes")
    public String handleUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam("action") String action,
            HttpSession session,
            Model model) {

        if (file.isEmpty()) {
            model.addAttribute("errorMessage", "Please select a file to upload.");
            return "Import-Notes-Page"; // Return to the same page if no file is selected
        }

        try {

            String fileContent = new String(file.getBytes());

            // Save the notes to the database
            Long userId = (Long) session.getAttribute("id"); // Ensure user ID is in the session
            saveNotesToDatabase(title, fileContent, userId);


            if ("flashcards".equals(action)) {
                model.addAttribute("successMessage", "Flashcards created successfully from the uploaded notes!");
                // Add flashcard generation
            } else if ("quiz".equals(action)) {
                model.addAttribute("successMessage", "Quiz generated successfully from the uploaded notes!");
                // Add quiz generation
            } else {
                model.addAttribute("errorMessage", "Invalid action. Please select 'Flashcard me' or 'Quiz me'.");
                return "Import-Notes-Page";
            }
        } catch (IOException e) {
            model.addAttribute("errorMessage", "Error processing the file: " + e.getMessage());
        } catch (SQLException e) {
            model.addAttribute("errorMessage", "Database error: " + e.getMessage());
        }

        return "Import-Notes-Page"; // Return to the Import Notes page with success/error messages
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
