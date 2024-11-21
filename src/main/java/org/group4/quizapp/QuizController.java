package org.group4.quizapp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    // Fetch and display the user's quizzes in addition to their question bank
    @GetMapping
    public String showQuizCreationForm(Model model, HttpSession session) {
        // Fetch user ID from session
        Long userId = (Long) session.getAttribute("id");

        if (userId == null) {
            return "redirect:/login"; // Redirect to login page if user is not logged in
        }

        List<Question> questionBank = new ArrayList<>();
        List<Quiz> quizzes = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(databaseUrl, databaseUsername, databasePassword)) {
            // Fetch questions belonging to the user
            String questionQuery = "SELECT id, question_text FROM questions WHERE user_id = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(questionQuery)) {
                preparedStatement.setLong(1, userId);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        Question question = new Question();
                        question.setId(resultSet.getLong("id"));
                        question.setQuestionText(resultSet.getString("question_text"));
                        questionBank.add(question);
                    }
                }
            }

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
            return "error-page"; // Show an error page if something goes wrong
        }

        // Add question bank and quizzes to the model
        model.addAttribute("questionBank", questionBank);
        model.addAttribute("quizzes", quizzes);

        return "quiz-creation"; // Return the Thymeleaf quiz creation template
    }


    @PostMapping
    public String createQuiz(
            @RequestParam("quizName") String quizName,
            @RequestParam(value = "selectedQuestions", required = false) List<Long> selectedQuestionIds,
            HttpSession session) {

        Long userId = (Long) session.getAttribute("id");

        if (userId == null) {
            return "redirect:/login";
        }

        // Check if no questions are selected
        if (selectedQuestionIds == null || selectedQuestionIds.isEmpty()) {
            return "redirect:/create-quiz?error=no-questions-selected";
        }

        try (Connection connection = DriverManager.getConnection(databaseUrl, databaseUsername, databasePassword)) {
            // Insert quiz into the quizzes table
            String insertQuizQuery = "INSERT INTO quizzes (user_id, quiz_name) VALUES (?, ?) RETURNING id";
            long quizId;
            try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuizQuery)) {
                preparedStatement.setLong(1, userId);
                preparedStatement.setString(2, quizName);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        quizId = resultSet.getLong(1); // Retrieve the generated quiz ID
                    } else {
                        throw new SQLException("Failed to insert quiz, no ID obtained.");
                    }
                }
            }

            // Insert selected questions into the quiz_questions table
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
            return "error-page"; // Show error page if something goes wrong
        }

        return "redirect:/create-quiz";
    }
    @GetMapping("/quiz-details")
    public String viewQuiz(
            @RequestParam("id") Long quizId,
            Model model,
            HttpSession session) {
        Long userId = (Long) session.getAttribute("id");

        if (userId == null) {
            return "redirect:/login";
        }

        Quiz quiz = null;
        List<Question> questions = new ArrayList<>();

        try (Connection connection = DriverManager.getConnection(databaseUrl, databaseUsername, databasePassword)) {
            // Fetch quiz details
            String quizQuery = "SELECT id, quiz_name FROM quizzes WHERE id = ? AND user_id = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(quizQuery)) {
                preparedStatement.setLong(1, quizId);
                preparedStatement.setLong(2, userId);
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (resultSet.next()) {
                        quiz = new Quiz();
                        quiz.setId(resultSet.getLong("id"));
                        quiz.setName(resultSet.getString("quiz_name"));
                    } else {
                        return "redirect:/create-quiz"; // Redirect if quiz not found or unauthorized
                    }
                }
            }

            // Fetch questions with all details for the quiz
            String questionQuery = """
            SELECT q.id, q.question_text, q.answer, q.description, q.tags
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
                        question.setTags(Collections.singletonList(resultSet.getString("tags")));
                        questions.add(question);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "error-page";
        }

        model.addAttribute("quiz", quiz);
        model.addAttribute("questions", questions);

        return "quiz-view"; // Return the Thymeleaf template for viewing the quiz
    }


}
