package com.volunteermatch.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsResponse {
    // Org stats
    private Long totalEvents;
    private Long activeEvents;
    private Long totalApplications;
    private Long selectedVolunteers;
    
    // Volunteer stats
    private Long availableOpportunities;
    private Long myApplications; // Applications Submitted (total)
    private Long pendingApplications;
    private Long selectedEvents;
    private Long notSelectedEvents; // Rejected
    private Long withdrawnApplications;
    private Long completedEvents;
    private Long totalCertificates;
}
