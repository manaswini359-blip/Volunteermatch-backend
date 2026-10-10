package com.volunteermatch.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskResponse {
    private Long id;
    private Long eventId;
    private String eventTitle;
    private Long volunteerId;
    private String volunteerName;
    private String taskName;
    private String status;
    private LocalDateTime assignedAt;
}
