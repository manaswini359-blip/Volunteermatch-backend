package com.volunteermatch.dto;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventResponse {
    private Long id;
    private Long organizationId;
    private String organizationName;
    private String organizationImage;
    private String organizationLocation;
    private String title;
    private String description;
    private String category;
    private String location;
    private LocalDate eventDate;
    private String startTime;
    private String endTime;
    private Integer requiredVolunteers;
    private String status;
    private LocalDateTime createdAt;
    private List<String> requiredSkills;
    private List<String> tasks;
    private List<String> interests;
    private long totalApplications;
    private long selectedCount;
    private Boolean userHasApplied;
    private String userApplicationStatus;
}
