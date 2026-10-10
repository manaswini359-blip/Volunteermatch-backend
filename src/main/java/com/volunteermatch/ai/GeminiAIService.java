package com.volunteermatch.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.volunteermatch.dto.AiAnalysisResponse;
import com.volunteermatch.entity.VolunteerProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Slf4j
@Primary
@Service("geminiAIService")
public class GeminiAIService implements AIService {

    private final AIService fallbackService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Value("${ai.provider:mock}")
    private String aiProvider;

    @Value("${ai.api-key:}")
    private String apiKey;

    @Value("${ai.model:gemini-1.5-flash}")
    private String modelName;

    public GeminiAIService(@Qualifier("mockAIService") AIService fallbackService, ObjectMapper objectMapper) {
        this.fallbackService = fallbackService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public AiAnalysisResponse analyzeEventDescription(String title, String description, String category) {
        // If provider is mock or API key is not configured, seamlessly use local rule-based AI engine
        if (!"gemini".equalsIgnoreCase(aiProvider) || apiKey == null || apiKey.trim().isEmpty()) {
            log.info("AI provider is '{}' (external API key not active). Using local AI extraction engine.", aiProvider);
            return fallbackService.analyzeEventDescription(title, description, category);
        }

        try {
            String prompt = String.format("""
                You are an AI Event Analyst for VolunteerMatch.
                Analyze the following volunteer event and extract required skills, tasks, and relevant interests.
                Return ONLY a valid JSON object with this exact structure:
                {
                  "requiredSkills": ["Skill 1", "Skill 2"],
                  "tasks": ["Task 1", "Task 2"],
                  "interests": ["Interest 1", "Interest 2"],
                  "summary": "Brief summary of requirements"
                }

                Event Title: %s
                Category: %s
                Description: %s

                Guidelines:
                - Extract clear required skills (e.g. Communication, Registration, Crowd Management, Event Coordination).
                - Extract distinct operational tasks/roles (e.g. Registration Desk, Donor Guidance, Crowd Management).
                - Ensure at least 1-4 relevant skills and 1-4 operational tasks are extracted whenever the description specifies activities.
                - DO NOT include conversational text or markdown code fences. Output valid JSON only.
                """, title != null ? title : "", category != null ? category : "", description != null ? description : "");

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(Map.of("text", prompt)))
                    )
            ));

            String url = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                    modelName, apiKey);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode candidates = root.path("candidates");
                if (candidates.isArray() && !candidates.isEmpty()) {
                    String rawText = candidates.get(0).path("content").path("parts").get(0).path("text").asText();
                    String jsonText = cleanJsonFence(rawText);
                    AiAnalysisResponse parsed = objectMapper.readValue(jsonText, AiAnalysisResponse.class);
                    if (parsed.getRequiredSkills() != null && !parsed.getRequiredSkills().isEmpty()) {
                        return parsed;
                    }
                }
                throw new RuntimeException("AI API response did not contain required skill extractions.");
            } else {
                log.error("Gemini API returned HTTP status {}: {}", response.statusCode(), response.body());
                throw new RuntimeException("AI API returned status " + response.statusCode() + " (" + response.body() + ")");
            }
        } catch (Exception ex) {
            log.error("AI API call failed: {}", ex.getMessage());
            // Requirement 9: show clear error instead of silently returning empty result
            throw new RuntimeException("External AI API failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public String generateMatchExplanation(String eventTitle,
                                           String eventDescription,
                                           List<String> eventSkills,
                                           List<String> eventTasks,
                                           VolunteerProfile profile,
                                           int matchScore,
                                           List<String> matchedSkills,
                                           List<String> matchedInterests) {
        if (!"gemini".equalsIgnoreCase(aiProvider) || apiKey == null || apiKey.trim().isEmpty()) {
            return fallbackService.generateMatchExplanation(eventTitle, eventDescription, eventSkills, eventTasks, profile, matchScore, matchedSkills, matchedInterests);
        }

        try {
            String volunteerName = profile.getUser() != null ? profile.getUser().getName() : "Volunteer";
            String prompt = String.format("""
                You are an AI Volunteer Matching Specialist. Write a concise 2-sentence rationale explaining why %s is recommended with a match score of %d%% for event '%s'.
                Matched skills: %s.
                Matched interests: %s.
                Event tasks: %s.
                Keep it professional, human-centered, and clear.
                """, volunteerName, matchScore, eventTitle, matchedSkills, matchedInterests, eventTasks);

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))
            ));

            String url = String.format("https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                    modelName, apiKey);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode candidates = root.path("candidates");
                if (candidates.isArray() && !candidates.isEmpty()) {
                    return candidates.get(0).path("content").path("parts").get(0).path("text").asText().trim();
                }
            }
        } catch (Exception ex) {
            log.warn("Error calling Gemini for explanation: {}. Using fallback explanation.", ex.getMessage());
        }

        return fallbackService.generateMatchExplanation(eventTitle, eventDescription, eventSkills, eventTasks, profile, matchScore, matchedSkills, matchedInterests);
    }

    private String cleanJsonFence(String raw) {
        if (raw == null) return "{}";
        String s = raw.trim();
        int firstBrace = s.indexOf('{');
        int lastBrace = s.lastIndexOf('}');
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            return s.substring(firstBrace, lastBrace + 1);
        }
        if (s.startsWith("```json")) {
            s = s.substring(7);
        } else if (s.startsWith("```")) {
            s = s.substring(3);
        }
        if (s.endsWith("```")) {
            s = s.substring(0, s.length() - 3);
        }
        return s.trim();
    }
}
