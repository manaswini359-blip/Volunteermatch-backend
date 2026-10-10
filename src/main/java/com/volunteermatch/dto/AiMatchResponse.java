package com.volunteermatch.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiMatchResponse {
    private Long volunteerId;
    private Long userId;
    private String volunteerName;
    private String volunteerEmail;
    private String bio;
    private String skills;
    private String interests;
    private String location;
    private String availability;
    private String profileImage;
    
    private Integer matchScore;
    private List<String> matchedSkills;
    private List<String> matchedInterests;
    private String reason;

    private String experience;
    private String experienceLevel;
    private Boolean hasIncompleteProfile;

    private Long applicationId;
    private String applicationStatus;
    private Boolean availabilityConfirmed;
}
