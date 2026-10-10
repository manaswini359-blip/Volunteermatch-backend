package com.volunteermatch.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceResponse {
    private Long id;
    private Long eventId;
    private String eventTitle;
    private Long volunteerId;
    private String volunteerName;
    private String volunteerEmail;
    private String status;
    private LocalDateTime checkInTime;
    private Boolean certificateIssued;
}
