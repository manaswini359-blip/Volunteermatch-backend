package com.volunteermatch.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApplyRequest {
    @NotNull(message = "Availability confirmation is required")
    private Boolean availabilityConfirmed;

    private String availabilityType;
    private String partialTimeRange;
    private String preferredTask;
    private String relevantSkills;
    private Boolean willingOtherTasks;
    private String motivationMessage;
}
