package com.volunteermatch.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CertificateRequest {
    @NotNull(message = "Event ID is required")
    private Long eventId;

    @NotNull(message = "Volunteer ID is required")
    private Long volunteerId;
}
