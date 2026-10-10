package com.volunteermatch.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TaskStatusRequest {
    @NotBlank(message = "Status is required (ASSIGNED, IN_PROGRESS, COMPLETED)")
    private String status;
}
