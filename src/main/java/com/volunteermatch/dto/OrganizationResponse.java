package com.volunteermatch.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationResponse {
    private Long id;
    private Long userId;
    private String organizationName;
    private String email;
    private String description;
    private String location;
    private String profileImage;
}
