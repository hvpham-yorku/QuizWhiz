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

    // Display the Create Question form and list all questions for the logged-in user
    @GetMapping
    public String showCreateQuestionForm(Model model, HttpSession session) {
        Long userId = (Long) session.getAttribute("id");

        // Redirect to login if user is not authenticated
        if (userId == null) {
            return "redirect:/login";
        }

        model.addAttribute("questionForm", new Question());
        List<Question> questions = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(databaseUrl, databaseUsername, databasePassword)) {
            String query = """
                SELECT id, question_text, answer, description, tags, question_type, options 
                FROM questions 
                WHERE user_id = ?
            """;
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setLong(1, userId);

                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {

                        Question question = mapResultSetToQuestion(resultSet);
                        questions.add(question);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();

            return "error-page"; // Redirect to an error page on database issues

        }

        model.addAttribute("questions", questions);
        return "create-question";
    }

    // Handle the creation of a new question
    @PostMapping
    public String createQuestion(@RequestParam("type") String type,
                                 @RequestParam("questionText") String questionText,
                                 @RequestParam("description") String description,
                                 @RequestParam(value = "answer", required = false) String answer,
                                 @RequestParam("tags") String tags,
                                 @RequestParam(value = "options", required = false) String options,
                                 HttpSession session) {

        Long userId = (Long) session.getAttribute("id");

        if (userId == null) {
            return "redirect:/login";
        }

        // Building the question based on the input
        Question question = buildQuestion(type, questionText, description, answer, tags, options, userId);

        try (Connection connection = DriverManager.getConnection(databaseUrl, databaseUsername, databasePassword)) {
            String query = """
                INSERT INTO questions 
                (user_id, question_text, answer, description, tags, question_type, options) 
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {

                preparedStatement.setLong(1, question.getUserId());
                preparedStatement.setString(2, question.getQuestionText());
                preparedStatement.setString(3, question.getAnswer());
                preparedStatement.setString(4, question.getDescription());
                preparedStatement.setString(5, String.join(",", question.getTags()));
                preparedStatement.setString(6, question.getType());
                preparedStatement.setString(7, question.getOptions() != null ? String.join(",", question.getOptions()) : null);
                preparedStatement.executeUpdate();

            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "error-page";
        }

        return "redirect:/create-question";
    }

    // Delete a question by ID
    @PostMapping("/delete")
    public String deleteQuestion(@RequestParam("id") Long id) {
        if (id == null) {
            return "error-page";
        }

        try (Connection connection = DriverManager.getConnection(databaseUrl, databaseUsername, databasePassword)) {
            String query = "DELETE FROM questions WHERE id = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setLong(1, id);
                int rowsAffected = preparedStatement.executeUpdate();

                if (rowsAffected == 0) {
                    System.err.println("No question found with id: " + id);
                    return "error-page";
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "error-page";
        }

        return "redirect:/create-question";
    }

    // Helper method to map ResultSet to a Question object
    private Question mapResultSetToQuestion(ResultSet resultSet) throws SQLException {
        String type = resultSet.getString("question_type");
        Question question = new Question();

        if (type.equals("Multiple Choice") || type.equals("Multi Select")) {
            question = new QuestionMultipleOption();
            String optionsString = resultSet.getString("options");
            if (optionsString != null) {
                question.setOptions(Arrays.asList(optionsString.split(",")));
            }
        }

        question.setId(resultSet.getLong("id"));
        question.setQuestionText(resultSet.getString("question_text"));
        if (!type.equals("True Or False")){
            question.setAnswer(resultSet.getString("answer").replace("True,", "").replace("False,", ""));
        }
        else{
            question.setAnswer(resultSet.getString("answer").replace(",", ""));
        }
        question.setDescription(resultSet.getString("description"));
        question.setTags(new ArrayList<>(Arrays.asList(resultSet.getString("tags").split(","))));
        question.setType(type);
        return question;
    }

    // Helper method to build a Question object based on the input
    private Question buildQuestion(String type, String questionText, String description, String answer,
                                   String tags, String options, Long userId) {
        Question question = new Question();

        if (type.equals("Multiple Choice") || type.equals("Multi Select")) {
            question = new QuestionMultipleOption();
            if (options != null && !options.trim().isEmpty()) {
                question.setOptions(Arrays.asList(options.split(",")));
            }
        }

        question.setUserId(userId);
        question.setQuestionText(questionText);
        question.setDescription(description);
        question.setTags(Arrays.asList(tags.split(",")));
        question.setAnswer(answer);
        question.setType(type);
        return question;
    }
}
