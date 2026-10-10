package com.volunteermatch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceRequest {
    @NotNull(message = "Event ID is required")
    private Long eventId;

    @NotNull(message = "Volunteer ID is required")
    private Long volunteerId;

    @NotBlank(message = "Status is required (PRESENT or ABSENT)")
    private String status;
}
