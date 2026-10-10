package com.volunteermatch.dto;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CertificateResponse {
    private Long id;
    private String certificateId;
    private Long eventId;
    private String eventTitle;
    private String organizationName;
    private LocalDate eventDate;
    private Long volunteerId;
    private String volunteerName;
    private String volunteerEmail;
    private String roleTask;
    private String certificateUrl;
    private LocalDateTime issuedDate;
}
