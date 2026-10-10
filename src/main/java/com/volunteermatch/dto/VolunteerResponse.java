package com.volunteermatch.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VolunteerResponse {
    private Long id;
    private Long userId;
    private String name;
    private String email;
    private String bio;
    private String skills;
    private String interests;
    private String location;
    private String availability;
    private String profileImage;
    private String experienceLevel;
    private String experience;
}
