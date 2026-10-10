package com.volunteermatch.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {
    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    private String confirmPassword;

    @NotBlank(message = "Role is required (VOLUNTEER or ORGANIZATION)")
    private String role;

    // For Organization registration
    private String organizationName;
    private String description;
    private String location;

    // For Volunteer registration (optional)
    private String bio;
    private String skills;
    private String interests;
    private String experience;
    private String experienceLevel;
    private String availability;
}
