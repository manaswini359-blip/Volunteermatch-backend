package com.volunteermatch.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "applications", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"event_id", "volunteer_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "volunteer_id", nullable = false)
    private VolunteerProfile volunteer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.PENDING;

    @Column(name = "availability_confirmed", nullable = false)
    @Builder.Default
    private Boolean availabilityConfirmed = false;

    @Column(name = "availability_type", length = 50)
    @Builder.Default
    private String availabilityType = "FULL";

    @Column(name = "partial_time_range", length = 100)
    private String partialTimeRange;

    @Column(name = "preferred_task", length = 255)
    private String preferredTask;

    @Column(name = "relevant_skills", columnDefinition = "TEXT")
    private String relevantSkills;

    @Column(name = "willing_other_tasks")
    @Builder.Default
    private Boolean willingOtherTasks = true;

    @Column(name = "motivation_message", columnDefinition = "TEXT")
    private String motivationMessage;

    @Column(name = "selected_at")
    private LocalDateTime selectedAt;

    @Column(name = "applied_at", updatable = false)
    private LocalDateTime appliedAt;

    @PrePersist
    protected void onCreate() {
        this.appliedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = ApplicationStatus.PENDING;
        }
    }

    public enum ApplicationStatus {
        PENDING,
        SELECTED,
        REJECTED,
        WITHDRAWN
    }
}
