package com.volunteermatch.dto;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationResponse {
    private Long id;
    private Long eventId;
    private String eventTitle;
    private String organizationName;
    private LocalDate eventDate;
    private String eventTime;
    private String eventLocation;
    
    private Long volunteerId;
    private String volunteerName;
    private String volunteerEmail;
    private String volunteerBio;
    private String volunteerSkills;
    private String volunteerInterests;
    private String volunteerLocation;
    private String volunteerAvailability;
    private String volunteerImage;

    private String status;
    private Boolean availabilityConfirmed;
    private String availabilityType;
    private String partialTimeRange;
    private String preferredTask;
    private String relevantSkills;
    private Boolean willingOtherTasks;
    private String motivationMessage;
    private LocalDateTime selectedAt;
    private LocalDateTime appliedAt;

    private String volunteerExperience;
    private String volunteerExperienceLevel;

    private Integer matchScore;
    private String matchReason;
}
