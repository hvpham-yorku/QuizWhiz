package org.group4.quizapp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Controller
@RequestMapping("/create-question")
public class QuestionController {

    @Value("${spring.datasource.url}")
    private String databaseUrl;

    @Value("${spring.datasource.username}")
    private String databaseUsername;

    @Value("${spring.datasource.password}")
    private String databasePassword;

    // Method to show the form for creating a new question and display user's existing questions
    @GetMapping
    public String showCreateQuestionForm(Model model, HttpSession session) {
        // Fetch user ID from session
        Long userId = (Long) session.getAttribute("id");

        if (userId == null) {
            return "redirect:/login"; // Redirect to login page if user is not logged in
        }

        model.addAttribute("questionForm", new Question());  // Empty form for new question

        List<Question> questions = new ArrayList<>();

        // Fetch user's questions from the database
        try (Connection connection = DriverManager.getConnection(databaseUrl, databaseUsername, databasePassword)) {
            String query = "SELECT id, question_text, answer, description, tags FROM questions WHERE user_id = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setLong(1, userId);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        Question question = new Question();
                        question.setId(resultSet.getLong("id"));  // Set the ID for each question
                        question.setQuestionText(resultSet.getString("question_text"));
                        question.setAnswer(resultSet.getString("answer"));
                        question.setDescription(resultSet.getString("description"));
                        // Convert the comma-separated tags back into a list
                        String tagsString = resultSet.getString("tags");
                        if (tagsString != null && !tagsString.isEmpty()) {
                            question.setTags(new ArrayList<>(Arrays.asList(tagsString.split(","))));
                        }
                        questions.add(question);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "error-page"; // Show an error page if something goes wrong
        }

        model.addAttribute("questions", questions); // Add questions to model
        return "create-question"; // Return to the page to display the form and existing questions
    }

    // Method to handle the creation of a new question
    @PostMapping
    public String createQuestion(@ModelAttribute("questionForm") Question questionForm, HttpSession session) {
        // Get user ID from session
        Long userId = (Long) session.getAttribute("id");
        if (userId == null) {
            return "redirect:/login";  // Redirect to login page if user is not logged in
        }

        questionForm.setUserId(userId);

        // Insert the new question into the database
        try (Connection connection = DriverManager.getConnection(databaseUrl, databaseUsername, databasePassword)) {
            String query = "INSERT INTO questions (user_id, question_text, answer, description, tags) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setLong(1, questionForm.getUserId());
                preparedStatement.setString(2, questionForm.getQuestionText());
                preparedStatement.setString(3, questionForm.getAnswer());
                preparedStatement.setString(4, questionForm.getDescription());

                // Convert the tags list into a comma-separated string and store it
                if (questionForm.getTags() != null && !questionForm.getTags().isEmpty()) {
                    preparedStatement.setString(5, String.join(",", questionForm.getTags()));
                } else {
                    preparedStatement.setString(5, ""); // If no tags, store an empty string
                }

                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "error-page"; // Show an error page if something goes wrong
        }

        // Redirect back to the same page after adding the new question
        return "redirect:/create-question";
    }
}
