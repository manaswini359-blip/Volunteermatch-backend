package com.volunteermatch.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String token;
    private String tokenType;
    private Long id;
    private String name;
    private String email;
    private String role;
    private Long profileId; // volunteer_profile id or organization id
    private String profileImage;
    private String organizationName;
}
