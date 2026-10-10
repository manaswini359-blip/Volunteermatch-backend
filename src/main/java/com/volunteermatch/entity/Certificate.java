package com.volunteermatch.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "certificates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "volunteer_id", nullable = false)
    private VolunteerProfile volunteer;

    @Column(name = "certificate_id", nullable = false, unique = true)
    private String certificateId;

    @Column(name = "certificate_url", length = 500)
    private String certificateUrl;

    @Column(name = "issued_date", updatable = false)
    private LocalDateTime issuedDate;

    @PrePersist
    protected void onCreate() {
        this.issuedDate = LocalDateTime.now();
    }
}
