package org.group4.quizapp;

import java.util.ArrayList;
import java.util.List;

public class Question {
    private Long id;
    private String questionText;
    private String answer;
    private String description;
    private List<String> tags = new ArrayList<>(); // Initialize as an empty list
    private Long userId; // Add userId field to associate the question with a user

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void addTag(String tag) {
        this.tags.add(tag);
    }
}
