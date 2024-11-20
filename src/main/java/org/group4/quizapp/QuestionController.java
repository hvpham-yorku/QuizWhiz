package org.group4.quizapp;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Controller
public class QuestionController {

    private List<Question> questions = new ArrayList<>();

    @GetMapping("/create-question")
    public String showQuestionForm(Model model) {
        model.addAttribute("questions", questions);
        return "create-question";
    }

    @PostMapping("/submit-question")
    public String submitQuestion(
            @RequestParam("questionText") String questionText,
            @RequestParam("answer") String answer,
            @RequestParam("desc") String desc,
            @RequestParam("tags") String tagsInput,
            Model model) {

        // Split tags by comma and trim whitespace
        ArrayList<String> tags = new ArrayList<>(Arrays.asList(tagsInput.split(",")));
        tags.replaceAll(String::trim);

        // Create and add the new question
        Question newQuestion = new Question(questionText, answer, desc, tags);
        questions.add(newQuestion);

        // Add updated questions to the model
        model.addAttribute("questions", questions);

        return "create-question";
    }
}
