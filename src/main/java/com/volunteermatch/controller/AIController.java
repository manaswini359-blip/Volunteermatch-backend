package com.volunteermatch.controller;

import com.volunteermatch.ai.AIService;
import com.volunteermatch.dto.AiAnalysisRequest;
import com.volunteermatch.dto.AiAnalysisResponse;
import com.volunteermatch.dto.AiMatchResponse;
import com.volunteermatch.dto.ApiResponse;
import com.volunteermatch.service.MatchingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {

    private final AIService aiService;
    private final MatchingService matchingService;

    @PostMapping("/analyze-event")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public ResponseEntity<ApiResponse<AiAnalysisResponse>> analyzeEvent(@Valid @RequestBody AiAnalysisRequest request) {
        try {
            AiAnalysisResponse response = aiService.analyzeEventDescription(
                    request.getTitle(),
                    request.getDescription(),
                    request.getCategory()
            );

            if (response == null || response.getRequiredSkills() == null || response.getRequiredSkills().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("AI could not extract required skills from the description. Please provide more details on event activities."));
            }

            return ResponseEntity.ok(ApiResponse.success("AI analysis completed successfully", response));
        } catch (Exception ex) {
            log.error("AI Event Analysis failed: {}", ex.getMessage(), ex);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("AI requirement extraction failed: " + ex.getMessage()));
        }
    }

    @GetMapping("/events/{eventId}/recommendations")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public ResponseEntity<ApiResponse<List<AiMatchResponse>>> getRecommendations(@PathVariable Long eventId) {
        List<AiMatchResponse> list = matchingService.matchVolunteersForEvent(eventId);
        return ResponseEntity.ok(ApiResponse.success("AI volunteer recommendations generated", list));
    }

    @PostMapping("/match-volunteers")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public ResponseEntity<ApiResponse<List<AiMatchResponse>>> matchVolunteers(@RequestBody Map<String, Long> payload) {
        Long eventId = payload.get("eventId");
        List<AiMatchResponse> list = matchingService.matchVolunteersForEvent(eventId);
        return ResponseEntity.ok(ApiResponse.success("AI volunteer recommendations generated", list));
    }
}
