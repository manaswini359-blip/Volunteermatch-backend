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
public class TaskRequest {
    @NotNull(message = "Event ID is required")
    private Long eventId;

    @NotNull(message = "Volunteer ID is required")
    private Long volunteerId;

    @NotBlank(message = "Task name is required")
    private String taskName;
}
