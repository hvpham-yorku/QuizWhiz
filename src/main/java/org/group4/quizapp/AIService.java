package org.group4.quizapp;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AIService {

    private static final String OPENAI_URL = "https://api.openai.com/v1/completions";
    private static final String API_KEY = "org-McsOmz9NE1QOMsw2LrbtFsyk"; // Replace with your actual OpenAI API key

    public String generateFlashcards(String text) {
        // Summarize the text first
        String summarizedText = summarizeText(text);

        RestTemplate restTemplate = new RestTemplate();

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + API_KEY);
        headers.set("Content-Type", "application/json");

        // Build the request body for flashcard generation
        String requestBody = String.format("{\"model\":\"gpt-4\",\"prompt\":\"Generate flashcards for the following summarized text:\\n%s\",\"max_tokens\":150}", summarizedText);

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        // Make the API call
        ResponseEntity<String> response = restTemplate.exchange(OPENAI_URL, HttpMethod.POST, entity, String.class);

        // Return the response body
        return response.getBody();
    }

    public String generateQuestions(String text) {
        // Summarize the text first
        String summarizedText = summarizeText(text);

        RestTemplate restTemplate = new RestTemplate();

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + API_KEY);
        headers.set("Content-Type", "application/json");

        // Build the request body for question generation
        String requestBody = String.format("{\"model\":\"gpt-4\",\"prompt\":\"Generate quiz questions for the following summarized text:\\n%s\",\"max_tokens\":150}", summarizedText);

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        // Make the API call
        ResponseEntity<String> response = restTemplate.exchange(OPENAI_URL, HttpMethod.POST, entity, String.class);

        // Return the response body
        return response.getBody();
    }

    public String summarizeText(String text) {
        RestTemplate restTemplate = new RestTemplate();

        // Set headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + API_KEY);
        headers.set("Content-Type", "application/json");

        // Build the request body for summarization
        String requestBody = String.format("{\"model\":\"gpt-4\",\"prompt\":\"Summarize the following text:\\n%s\",\"max_tokens\":150}", text);

        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        // Make the API call
        ResponseEntity<String> response = restTemplate.exchange(OPENAI_URL, HttpMethod.POST, entity, String.class);

        // Return the response
        return response.getBody();
    }


}
