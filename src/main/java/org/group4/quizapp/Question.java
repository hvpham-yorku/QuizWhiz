package org.group4.quizapp;

import java.util.ArrayList;

public class Question {

    private String questionText;
    private String answer;
    private String desc; // New description attribute
    private ArrayList<String> tags; // New tags attribute

    public Question(String questionText, String answer, String desc, ArrayList<String> tags) {
        this.questionText = questionText;
        this.answer = answer;
        this.desc = desc;
        this.tags = tags;
    }

    // Getters and Setters
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

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public ArrayList<String> getTags() {
        return tags;
    }

    public void setTags(ArrayList<String> tags) {
        this.tags = tags;
    }
}
