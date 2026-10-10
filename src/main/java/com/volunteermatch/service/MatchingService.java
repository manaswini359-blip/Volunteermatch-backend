package com.volunteermatch.service;

import com.volunteermatch.ai.AIService;
import com.volunteermatch.dto.AiMatchResponse;
import com.volunteermatch.entity.*;
import com.volunteermatch.exception.ResourceNotFoundException;
import com.volunteermatch.repository.AiMatchResultRepository;
import com.volunteermatch.repository.ApplicationRepository;
import com.volunteermatch.repository.EventRepository;
import com.volunteermatch.repository.VolunteerProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingService {

    private final EventRepository eventRepository;
    private final VolunteerProfileRepository volunteerProfileRepository;
    private final AiMatchResultRepository aiMatchResultRepository;
    private final ApplicationRepository applicationRepository;
    private final AIService aiService;

    @Transactional
    public List<AiMatchResponse> matchVolunteersForEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        List<VolunteerProfile> allVolunteers = volunteerProfileRepository.findAll();
        List<EventRequirement> requirements = event.getRequirements();

        List<String> eventSkills = requirements.stream()
                .filter(r -> r.getRequirementType() == EventRequirement.RequirementType.SKILL && r.getRequiredSkill() != null)
                .map(EventRequirement::getRequiredSkill)
                .collect(Collectors.toList());

        List<String> eventTasks = requirements.stream()
                .filter(r -> r.getRequiredTask() != null)
                .map(EventRequirement::getRequiredTask)
                .collect(Collectors.toList());

        List<AiMatchResponse> results = new ArrayList<>();

        for (VolunteerProfile volunteer : allVolunteers) {
            boolean hasIncomplete = (volunteer.getSkills() == null || volunteer.getSkills().isBlank())
                    && (volunteer.getInterests() == null || volunteer.getInterests().isBlank());

            List<String> matchedSkills = findMatchedTokens(volunteer.getSkills(), eventSkills);
            List<String> matchedInterests = findMatchedTokens(volunteer.getInterests(), Collections.singletonList(event.getCategory()));

            int finalScore;
            String explanation;

            if (hasIncomplete) {
                finalScore = 0;
                explanation = "Profile has insufficient information (no skills or interests specified). Please complete your profile to receive tailored recommendations.";
            } else {
                double skillScore = calculateOverlapRatio(volunteer.getSkills(), eventSkills);
                double interestScore = calculateOverlapRatio(volunteer.getInterests(), Collections.singletonList(event.getCategory()));
                double experienceScore = calculateExperienceScore(volunteer.getExperience(), volunteer.getExperienceLevel());
                double locationScore = calculateLocationRelevance(volunteer.getLocation(), event.getLocation());

                // Transparent weighted scoring: Skills 50%, Interests 25%, Experience 15%, Location 10%
                double rawScore = (skillScore * 0.50) + (interestScore * 0.25) + (experienceScore * 0.15) + (locationScore * 0.10);
                finalScore = (int) Math.round(rawScore * 100);

                explanation = aiService.generateMatchExplanation(
                        event.getTitle(),
                        event.getDescription(),
                        eventSkills,
                        eventTasks,
                        volunteer,
                        finalScore,
                        matchedSkills,
                        matchedInterests
                );
            }

            // Persist or Update AiMatchResult
            AiMatchResult matchResult = aiMatchResultRepository.findByEventIdAndVolunteerId(eventId, volunteer.getId())
                    .orElse(AiMatchResult.builder()
                            .event(event)
                            .volunteer(volunteer)
                            .build());

            matchResult.setMatchScore(finalScore);
            matchResult.setMatchedSkills(String.join(", ", matchedSkills));
            matchResult.setMatchedInterests(String.join(", ", matchedInterests));
            matchResult.setReason(explanation);
            aiMatchResultRepository.save(matchResult);

            // Check if volunteer has applied
            Optional<Application> applicationOpt = applicationRepository.findByEventIdAndVolunteerId(eventId, volunteer.getId());

            results.add(AiMatchResponse.builder()
                    .volunteerId(volunteer.getId())
                    .userId(volunteer.getUser() != null ? volunteer.getUser().getId() : null)
                    .volunteerName(volunteer.getUser() != null ? volunteer.getUser().getName() : "")
                    .volunteerEmail(volunteer.getUser() != null ? volunteer.getUser().getEmail() : "")
                    .bio(volunteer.getBio())
                    .skills(volunteer.getSkills())
                    .interests(volunteer.getInterests())
                    .experience(volunteer.getExperience())
                    .experienceLevel(volunteer.getExperienceLevel())
                    .hasIncompleteProfile(hasIncomplete)
                    .location(volunteer.getLocation())
                    .availability(volunteer.getAvailability())
                    .profileImage(volunteer.getProfileImage())
                    .matchScore(finalScore)
                    .matchedSkills(matchedSkills)
                    .matchedInterests(matchedInterests)
                    .reason(explanation)
                    .applicationId(applicationOpt.map(Application::getId).orElse(null))
                    .applicationStatus(applicationOpt.map(a -> a.getStatus().name()).orElse("NOT_APPLIED"))
                    .availabilityConfirmed(applicationOpt.map(Application::getAvailabilityConfirmed).orElse(false))
                    .build());
        }

        // Sort descending by match score
        results.sort((a, b) -> Integer.compare(b.getMatchScore(), a.getMatchScore()));
        return results;
    }

    private double calculateExperienceScore(String experience, String experienceLevel) {
        double score = 0.0;
        if (experienceLevel != null && !experienceLevel.isBlank()) {
            String lvl = experienceLevel.trim().toLowerCase();
            if (lvl.contains("experienced") || lvl.contains("lead")) {
                score = 0.8;
            } else if (lvl.contains("intermediate")) {
                score = 0.6;
            } else if (lvl.contains("beginner")) {
                score = 0.3;
            }
        }
        if (experience != null && !experience.isBlank()) {
            // Additional points for specific volunteer background details
            score = Math.min(1.0, score + 0.2);
        }
        return score;
    }

    private List<String> findMatchedTokens(String userListStr, List<String> targetList) {
        if (userListStr == null || targetList == null || targetList.isEmpty()) {
            return Collections.emptyList();
        }
        String[] tokens = userListStr.toLowerCase().split("[,;|/]");
        Set<String> matches = new LinkedHashSet<>();

        for (String target : targetList) {
            if (target == null) continue;
            String targetLower = target.toLowerCase().trim();
            for (String token : tokens) {
                String t = token.trim();
                if (t.isEmpty()) continue;
                if (targetLower.contains(t) || t.contains(targetLower)) {
                    matches.add(target);
                }
            }
        }
        return new ArrayList<>(matches);
    }

    private double calculateOverlapRatio(String userStr, List<String> targetList) {
        if (targetList == null || targetList.isEmpty()) {
            return 0.0;
        }
        if (userStr == null || userStr.isBlank()) {
            return 0.0; // Do not treat missing optional info as a match
        }
        List<String> matched = findMatchedTokens(userStr, targetList);
        if (matched.isEmpty()) {
            return 0.0;
        }
        return Math.min(1.0, (double) matched.size() / targetList.size());
    }

    private double calculateAvailabilityRelevance(String availability, Event event) {
        if (availability == null || availability.isBlank()) return 0.5;
        String availLower = availability.toLowerCase();

        String dayOfWeek = event.getEventDate() != null ? event.getEventDate().getDayOfWeek().name().toLowerCase() : "";
        boolean isWeekend = dayOfWeek.contains("saturday") || dayOfWeek.contains("sunday");

        if (availLower.contains("weekend") && isWeekend) return 0.95;
        if (availLower.contains(dayOfWeek)) return 0.95;
        if (availLower.contains("anytime") || availLower.contains("flexible")) return 0.90;

        return 0.50;
    }

    private double calculateLocationRelevance(String volunteerLocation, String eventLocation) {
        if (volunteerLocation == null || eventLocation == null) return 0.5;
        String vLoc = volunteerLocation.toLowerCase().trim();
        String eLoc = eventLocation.toLowerCase().trim();

        // Check common city names
        String[] commonCities = {"mumbai", "pune", "bangalore", "delhi", "hyderabad", "chennai", "kolkata"};
        for (String city : commonCities) {
            if (vLoc.contains(city) && eLoc.contains(city)) {
                return 1.0;
            }
        }

        if (eLoc.contains(vLoc) || vLoc.contains(eLoc)) {
            return 0.9;
        }
        return 0.4;
    }
}
