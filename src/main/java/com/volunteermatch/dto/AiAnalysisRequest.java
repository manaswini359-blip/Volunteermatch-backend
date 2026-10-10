package com.volunteermatch.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiAnalysisRequest {
    @NotBlank(message = "Event description is required")
    private String description;

    private String title;
    private String category;
}
