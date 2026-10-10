package com.volunteermatch.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "event_requirements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventRequirement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    @JsonIgnore
    private Event event;

    @Column(name = "required_skill", length = 255)
    private String requiredSkill;

    @Column(name = "required_task", length = 500)
    private String requiredTask;

    @Enumerated(EnumType.STRING)
    @Column(name = "requirement_type", length = 100)
    private RequirementType requirementType;

    public enum RequirementType {
        SKILL,
        TASK,
        INTEREST
    }
}
