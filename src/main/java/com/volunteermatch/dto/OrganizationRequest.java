package com.volunteermatch.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationRequest {
    private String organizationName;
    private String description;
    private String location;
    private String profileImage;
}
