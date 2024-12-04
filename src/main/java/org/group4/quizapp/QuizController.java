package org.group4.quizapp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/create-quiz")
public class QuizController {

    @Value("${spring.datasource.url}")
    private String databaseUrl;

    @Value("${spring.datasource.username}")
    private String databaseUsername;

    @Value("${spring.datasource.password}")
    private String databasePassword;

    // Show the quiz creation form
    @GetMapping
    public String showQuizCreationForm(Model model, HttpSession session) {
        Long userId = (Long) session.getAttribute("id");

        if (userId == null) {
            return "redirect:/login";
        }

        List<Question> questionBank = new ArrayList<>();
        List<Quiz> quizzes = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(databaseUrl, databaseUsername, databasePassword)) {
            // Fetch questions
            String questionQuery = "SELECT id, question_text, type FROM questions WHERE user_id = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(questionQuery)) {
                preparedStatement.setLong(1, userId);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        Question question = new Question();
                        question.setId(resultSet.getLong("id"));
                        question.setQuestionText(resultSet.getString("question_text"));
                        question.setType(resultSet.getString("type"));
                        questionBank.add(question);
                    }
                }
            }

            // Fetch quizzes belonging to the user
            String quizQuery = "SELECT id, quiz_name, public FROM quizzes WHERE user_id = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(quizQuery)) {
                preparedStatement.setLong(1, userId);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        Quiz quiz = new Quiz();
                        quiz.setId(resultSet.getLong("id"));
                        quiz.setName(resultSet.getString("quiz_name"));
                        quiz.setPublic(resultSet.getBoolean("public"));
                        quizzes.add(quiz);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "error-page";
        }

        model.addAttribute("questionBank", questionBank);
        model.addAttribute("quizzes", quizzes);

        return "quiz-creation";
    }

    // Create a new quiz
    @PostMapping
    public String createQuiz(
            @RequestParam("quizName") String quizName,
            @RequestParam(value = "selectedQuestions", required = false) List<Long> selectedQuestionIds,
            HttpSession session) {

        Long userId = (Long) session.getAttribute("id");

        if (userId == null) {
            return "redirect:/login";
        }

        if (selectedQuestionIds == null || selectedQuestionIds.isEmpty()) {
            return "redirect:/create-quiz?error=no-questions-selected";
        }

        try (Connection connection = DriverManager.getConnection(databaseUrl, databaseUsername, databasePassword)) {
            // Insert quiz
            String insertQuizQuery = "INSERT INTO quizzes (user_id, quiz_name, public) VALUES (?, ?, ?) RETURNING id";
            long quizId;
            try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuizQuery)) {
                preparedStatement.setLong(1, userId);
                preparedStatement.setString(2, quizName);
                preparedStatement.setBoolean(3, false); // Set default visibility to private
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        quizId = resultSet.getLong(1);
                    } else {
                        throw new SQLException("Failed to insert quiz, no ID obtained.");
                    }
                }
            }

            // Insert selected questions
            String insertQuizQuestionsQuery = "INSERT INTO quiz_questions (quiz_id, question_id) VALUES (?, ?)";
            try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuizQuestionsQuery)) {
                for (Long questionId : selectedQuestionIds) {
                    preparedStatement.setLong(1, quizId);
                    preparedStatement.setLong(2, questionId);
                    preparedStatement.addBatch();
                }
                preparedStatement.executeBatch();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "error-page";
        }

        return "redirect:/create-quiz";
    }

    // View quiz details (accessible to the owner, or anyone if public)
    @GetMapping("/quiz-details")
    public String viewQuiz(@RequestParam("id") Long quizId, Model model, HttpSession session) {
        Long userId = (Long) session.getAttribute("id");
        Quiz quiz = null;
        List<Question> questions = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(databaseUrl, databaseUsername, databasePassword)) {
            // Fetch quiz details
            String quizQuery = "SELECT id, quiz_name, public, user_id FROM quizzes WHERE id = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(quizQuery)) {
                preparedStatement.setLong(1, quizId);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        quiz = new Quiz();
                        quiz.setId(resultSet.getLong("id"));
                        quiz.setName(resultSet.getString("quiz_name"));
                        quiz.setPublic(resultSet.getBoolean("public"));
                        quiz.setUserId(resultSet.getLong("user_id"));  // Store the owner user ID
                    } else {
                        return "redirect:/create-quiz";  // Or a 404 page
                    }
                }
            }

            // Check if the user is logged in and if they are the owner of the quiz or if the quiz is public
            if (quiz != null && (quiz.isPublic() || (userId != null && userId.equals(quiz.getUserId())))) {
                // Fetch the questions for this quiz (accessible to the owner or if public)
                String questionQuery = """
                SELECT q.id, q.question_text, q.answer, q.description, q.tags, q.type, q.options
                FROM questions q
                INNER JOIN quiz_questions qq ON q.id = qq.question_id
                WHERE qq.quiz_id = ?
            """;
                try (PreparedStatement preparedStatement = connection.prepareStatement(questionQuery)) {
                    preparedStatement.setLong(1, quizId);
                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                        while (resultSet.next()) {
                            Question question = new Question();
                            question.setId(resultSet.getLong("id"));
                            question.setQuestionText(resultSet.getString("question_text"));
                            question.setAnswer(resultSet.getString("answer"));
                            question.setDescription(resultSet.getString("description"));

                            // Parse tags and options
                            String tagsString = resultSet.getString("tags");
                            if (tagsString != null && !tagsString.isEmpty()) {
                                question.setTags(Arrays.asList(tagsString.split(",")));
                            }

                            question.setType(resultSet.getString("type"));
                            if ("MultipleChoice".equalsIgnoreCase(question.getType())) {
                                String optionsString = resultSet.getString("options");
                                if (optionsString != null && !optionsString.isEmpty()) {
                                    question.setOptions(Arrays.asList(optionsString.split(",")));
                                }
                            }

                            questions.add(question);
                        }
                    }
                }
            } else {
                return "redirect:/login";  // Redirect to login if not the owner and quiz is private
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "error-page";
        }

        model.addAttribute("quiz", quiz);
        model.addAttribute("questions", questions);

        return "quiz-view";
    }


    // Toggle the public/private status of a quiz
    @PostMapping("/quiz/{quizId}/toggle-public")
    @ResponseBody
    public ResponseEntity<?> toggleQuizVisibility(@PathVariable("quizId") Long quizId, HttpSession session) {
        Long userId = (Long) session.getAttribute("id");

        if (userId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", "Unauthorized"));
        }

        try (Connection connection = DriverManager.getConnection(databaseUrl, databaseUsername, databasePassword)) {
            // Fetch current public status
            String fetchStatusQuery = "SELECT public FROM quizzes WHERE id = ? AND user_id = ?";
            boolean currentStatus = false;
            try (PreparedStatement preparedStatement = connection.prepareStatement(fetchStatusQuery)) {
                preparedStatement.setLong(1, quizId);
                preparedStatement.setLong(2, userId);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        currentStatus = resultSet.getBoolean("public");
                    } else {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", "Quiz not found or you're not the owner"));
                    }
                }
            }

            // Toggle public status
            String updateStatusQuery = "UPDATE quizzes SET public = ? WHERE id = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(updateStatusQuery)) {
                preparedStatement.setBoolean(1, !currentStatus);
                preparedStatement.setLong(2, quizId);
                int rowsUpdated = preparedStatement.executeUpdate();
                if (rowsUpdated > 0) {
                    return ResponseEntity.ok(Map.of("success", true, "newStatus", !currentStatus ? "public" : "private"));
                } else {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("success", false, "message", "Failed to update visibility"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("success", false, "message", "Database error"));
        }
    }
}