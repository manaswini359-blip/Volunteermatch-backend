package com.volunteermatch.repository;

import com.volunteermatch.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByEventIdOrderByAppliedAtDesc(Long eventId);
    List<Application> findByVolunteerIdOrderByAppliedAtDesc(Long volunteerId);
    Optional<Application> findByEventIdAndVolunteerId(Long eventId, Long volunteerId);
    boolean existsByEventIdAndVolunteerId(Long eventId, Long volunteerId);
    long countByEventIdAndStatus(Long eventId, Application.ApplicationStatus status);
    long countByEventId(Long eventId);
    List<Application> findByEventOrganizationIdOrderByAppliedAtDesc(Long organizationId);
}
