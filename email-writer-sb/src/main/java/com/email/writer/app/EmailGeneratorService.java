package com.email.writer.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class EmailGeneratorService {
    private final WebClient webClient;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    public EmailGeneratorService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public String generateEmailReply(EmailRequest emailRequest){
        // Build Request
        String prompt = buildPrompt(emailRequest);

        // Craft Request
        Map<String, Object> requestBody = Map.of(
                "contents", new Object[] {
                        Map.of("parts", new Object[] {
                            Map.of("text", prompt)
                        })
                }
        );

        // Do request and get response
        // Api call : using api url and key along with the prompt
        //For this we will use WebClient. It enables us to handle asynchronous, non-blocking HTTP requests. Web flux dependency.
        String response = webClient.post()
                .uri(geminiApiUrl + geminiApiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        // Extract response and Return
        return extractResponseContent(response);
    }

    private String extractResponseContent(String response) {
        try{
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);
            return root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
        }catch (Exception e){
            return "ERROR PROCESSING RESPONSE " + e.getMessage();
        }
    }

    private String buildPrompt(EmailRequest emailRequest) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a professional email reply for the email content. Please don't generate a subject line. ");
        if(emailRequest.getTone()!=null && !emailRequest.getTone().isEmpty()){
            prompt.append("Use a ");
            prompt.append(emailRequest.getTone());
            prompt.append(" tone.");
        }
        prompt.append("\nOriginal email: \n");
        prompt.append(emailRequest.getEmailContent());
        return prompt.toString();
    }
}
