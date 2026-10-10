package com.volunteermatch.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiAnalysisResponse {
    private List<String> requiredSkills;
    private List<String> tasks;
    private List<String> interests;
    private String summary;
}
