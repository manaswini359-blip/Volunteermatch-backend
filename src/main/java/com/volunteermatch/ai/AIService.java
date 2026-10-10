package com.volunteermatch.ai;

import com.volunteermatch.dto.AiAnalysisResponse;
import com.volunteermatch.entity.VolunteerProfile;

import java.util.List;

public interface AIService {
    AiAnalysisResponse analyzeEventDescription(String title, String description, String category);
    
    String generateMatchExplanation(String eventTitle,
                                   String eventDescription,
                                   List<String> eventSkills,
                                   List<String> eventTasks,
                                   VolunteerProfile profile,
                                   int matchScore,
                                   List<String> matchedSkills,
                                   List<String> matchedInterests);
}
