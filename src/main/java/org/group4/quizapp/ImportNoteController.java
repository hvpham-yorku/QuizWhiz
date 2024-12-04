package org.group4.quizapp;


import jakarta.servlet.http.HttpSession;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.ai.chat.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Controller
public class ImportNoteController {

    @Value("${spring.datasource.url}")
    private String databaseUrl;

    @Value("${spring.datasource.username}")
    private String databaseUsername;

    @Value("${spring.datasource.password}")
    private String databasePassword;

    @Value("${openai.api.key}")
    private String openAiApiKey;

    private final ChatClient chatClient;

    @Autowired
    public ImportNoteController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }


    @GetMapping("/upload-notes")
    public String uploadNotes(HttpSession session, Model model) {
        if (session.getAttribute("username") == null) {
            return "redirect:/login";
        }
        return "Import-Notes-Page";
    }

    @PostMapping("/upload-notes")
    public String handleUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("action") String action,
            HttpSession session,
            Model model) {

        Long userId = (Long) session.getAttribute("id");
        if (userId == null) {
            return "redirect:/login";
        }

        if (file.isEmpty()) {
            model.addAttribute("errorMessage", "Please select a file to upload.");
            return "Import-Notes-Page";
        }

        try {
            String fileContent;
            if (file.getOriginalFilename().endsWith(".pdf")) {
                fileContent = extractTextFromPdf(file);
                System.out.println("Extracted Notes: " + fileContent);



            } else if (file.getOriginalFilename().endsWith(".docx")) {
                fileContent = extractTextFromDocx(file);
            } else {
                fileContent = new String(file.getBytes());
            }

            if (fileContent == null || fileContent.trim().isEmpty()) {
                model.addAttribute("errorMessage", "The uploaded file contains no readable text.");
                return "Import-Notes-Page";
            }

            List<Question> generatedQuestions = generateQuestionsFromNotes(fileContent, action);

            if (generatedQuestions.isEmpty()) {
                model.addAttribute("errorMessage", "No questions could be generated.");
                return "Import-Notes-Page";
            }

            saveQuestionsToDatabase(generatedQuestions, userId);

            model.addAttribute("successMessage", "Questions have been added to your bank.");
        } catch (IOException e) {
            model.addAttribute("errorMessage", "Error processing the file: " + e.getMessage());
        } catch (SQLException e) {
            model.addAttribute("errorMessage", "Database error: " + e.getMessage());
        }

        return "Import-Notes-Page";
    }

    private String extractTextFromPdf(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            return pdfStripper.getText(document);
        }
    }

    private String extractTextFromDocx(MultipartFile file) throws IOException {
        try (XWPFDocument document = new XWPFDocument(file.getInputStream())) {
            XWPFWordExtractor extractor = new XWPFWordExtractor(document);
            return extractor.getText();
        }
    }

    private List<Question> generateQuestionsFromNotes(String notes, String type) {
        // Prompt for AI to create question
        String prompt = createPromptForType(notes, type);

        // Prompt being sent to AI
        Prompt chatPrompt = new Prompt(List.of(
                new SystemMessage("You are an assistant for generating quiz questions."),
                new UserMessage(prompt)
        ));

        try {
            // Call the AI API
            String aiResponse = chatClient.call(chatPrompt)
                    .getResult()
                    .getOutput()
                    .getContent();

            // Parse the response into Question objects
            return parseGeneratedQuestions(aiResponse, type);
        } catch (Exception e) {
            //Debugging messages
            e.printStackTrace();
            System.err.println("Error during AI API call: " + e.getMessage());
            return new ArrayList<>();
        }
    }



    private String createPromptForType(String notes, String type) {
        switch (type.toLowerCase()) {
            case "flashcards":
                return String.format(
                        "Based on the following notes, generate 4 flashcard-style questions. " +
                                "Each question should have a concise question and a precise answer. Use the format:\n" +
                                "Question: [Your question text]\nAnswer: [Your answer text]\n\nNotes:\n%s", notes);


            case "multiple choice":
                return String.format(
                        "Based on the following notes, generate 4 multiple-choice questions. " +
                                "Each question should have 4 options labeled (a), (b), (c), (d), and one correct answer. Use the format:\n" +
                                "Question: [Your question text]\nOptions:\n(a) [Option 1]\n(b) [Option 2]\n(c) [Option 3]\n(d) [Option 4]\nAnswer: [Correct answer]\n\nNotes:\n%s", notes);

            case "fill in the blank":
                return String.format(
                        "Based on the following notes, generate 4 fill-in-the-blank questions. " +
                                "Each question should omit a key term or phrase, which will be the answer. Use the format:\n" +
                                "Question: [Your question text with a blank]\nAnswer: [Correct answer]\n\nNotes:\n%s", notes);

            case "true or false":
                return String.format(
                        "Based on the following notes, generate 4 true or false questions. " +
                                "Each question should be a statement that is either true or false. Use the format:\n" +
                                "Statement: [Your statement]\nAnswer: [True/False]\n\nNotes:\n%s", notes);

            default:
                return "Invalid question type selected.";
        }
    }

    private List<Question> parseGeneratedQuestions(String aiResponse, String type) {
        List<Question> questions = new ArrayList<>();
        if (aiResponse == null || aiResponse.isEmpty()) {
            System.out.println("AI response is empty or null.");
            return questions;
        }

        String[] blocks = aiResponse.split("\n\n");

        for (String block : blocks) {
            Question question = new Question();
            question.setType(type);

            try {
                switch (type.toLowerCase()) {
                    case "flashcards":
                        String[] qaParts = block.split("Answer:");
                        if (qaParts.length < 2) continue;
                        question.setQuestionText(qaParts[0].replace("Question:", "").trim());
                        question.setAnswer(qaParts[1].trim());
                        break;

                    case "multiple choice":
                        String[] mcParts = block.split("Options:");
                        if (mcParts.length < 2) continue;
                        question.setQuestionText(mcParts[0].replace("Question:", "").trim());

                        String[] optionParts = mcParts[1].split("Answer:");
                        if (optionParts.length < 2) continue;

                        String[] options = optionParts[0].split("\\) ");
                        List<String> optionList = new ArrayList<>();
                        for (String opt : options) {
                            if (!opt.trim().isEmpty() && opt.length() > 2) {
                                optionList.add(opt.substring(2).trim());
                            }
                        }
                        question.setOptions(optionList);
                        question.setAnswer(optionParts[1].trim());
                        break;

                    case "fill in the blank":
                        String[] fibParts = block.split("Answer:");
                        if (fibParts.length < 2) continue;
                        question.setQuestionText(fibParts[0].replace("Question:", "").trim());
                        question.setAnswer(fibParts[1].trim());
                        break;

                    case "true or false":
                        String[] tfParts = block.split("Answer:");
                        if (tfParts.length < 2) continue;
                        question.setQuestionText(tfParts[0].replace("Statement:", "").trim());
                        question.setAnswer(tfParts[1].trim());
                        break;

                    default:
                        continue;
                }

                question.setDescription("Automatically generated question summary.");
                question.addTag(type);

                questions.add(question);

            } catch (Exception e) {
                System.out.println("Error parsing block: " + block);
                e.printStackTrace();
            }
        }

        return questions;
    }

    private void saveQuestionsToDatabase(List<Question> questions, Long userId) throws SQLException {
        try (Connection connection = DriverManager.getConnection(databaseUrl, databaseUsername, databasePassword)) {
            String query = "INSERT INTO questions (user_id, question_text, answer, description, tags, type, options) VALUES (?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
                for (Question question : questions) {
                    preparedStatement.setLong(1, userId);
                    preparedStatement.setString(2, question.getQuestionText());
                    preparedStatement.setString(3, question.getAnswer());
                    preparedStatement.setString(4, question.getDescription());
                    preparedStatement.setString(5, String.join(",", question.getTags()));
                    preparedStatement.setString(6, question.getType());
                    preparedStatement.setString(7, String.join(",", question.getOptions()));
                    preparedStatement.addBatch();
                }
                preparedStatement.executeBatch();
            }
        }
    }
}