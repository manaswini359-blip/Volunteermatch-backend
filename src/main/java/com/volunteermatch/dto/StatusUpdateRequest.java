package com.volunteermatch.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StatusUpdateRequest {
    @NotBlank(message = "Status is required (SELECTED, REJECTED)")
    private String status;
}
