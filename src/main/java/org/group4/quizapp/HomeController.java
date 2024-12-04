package org.group4.quizapp;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController {

    @Value("${spring.datasource.url}")
    private String databaseUrl;

    @Value("${spring.datasource.username}")
    private String databaseUsername;

    @Value("${spring.datasource.password}")
    private String databasePassword;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/home")
    public String showHomePage(HttpSession session, Model model) {
        String username = (String) session.getAttribute("username");
        String email = (String) session.getAttribute("userEmail");
        Long userId = (Long) session.getAttribute("id");

        if (username == null) {
            return "redirect:/login"; // Redirect to log in if session is invalid
        }

        model.addAttribute("welcomeMessage", "Welcome back, " + username + "!");

        List<Quiz> quizzes = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(databaseUrl, databaseUsername, databasePassword)) {
            // Fetch quizzes belonging to the user
            String quizQuery = "SELECT id, quiz_name FROM quizzes WHERE user_id = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(quizQuery)) {
                preparedStatement.setLong(1, userId);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        Quiz quiz = new Quiz();
                        quiz.setId(resultSet.getLong("id"));
                        quiz.setName(resultSet.getString("quiz_name"));
                        quizzes.add(quiz);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "error-page";
        }

        model.addAttribute("quizzes", quizzes);


        return "Home-Page"; // Ensure this matches your Thymeleaf template name
    }
  
  @GetMapping("/import-notes")
    public String showImportNotesPage() {
        return "Import-Notes-Page"; // Ensure this matches your Thymeleaf template for importing notes
    }
}

