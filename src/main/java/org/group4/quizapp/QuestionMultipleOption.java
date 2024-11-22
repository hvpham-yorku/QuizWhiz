package org.group4.quizapp;

import java.util.List;

public class QuestionMultipleOption extends Question{

    private List options; // List of options for the question

    // Constructor
    public QuestionMultipleOption(Long id, String questionText, String answer, String description, List<String> tags, Long userId, List<String> options, String type) {
        setId(id);
        setQuestionText(questionText);
        setAnswer(answer);
        setDescription(description);
        setTags(tags);
        setUserId(userId);
        setType(type);
        this.options = options;
    }

    public QuestionMultipleOption() {

    }

    // Getter and setter for options
    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }
}