package com.volunteermatch.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "volunteer_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VolunteerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(columnDefinition = "TEXT")
    private String skills;

    @Column(columnDefinition = "TEXT")
    private String interests;

    private String location;

    private String availability;

    @Column(name = "profile_image", length = 500)
    private String profileImage;

    @Column(name = "experience_level", length = 50)
    @Builder.Default
    private String experienceLevel = "Beginner";

    @Column(columnDefinition = "TEXT")
    private String experience;
}
