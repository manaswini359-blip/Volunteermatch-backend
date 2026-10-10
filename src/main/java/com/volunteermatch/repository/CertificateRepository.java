package com.volunteermatch.repository;

import com.volunteermatch.entity.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    List<Certificate> findByVolunteerIdOrderByIssuedDateDesc(Long volunteerId);
    List<Certificate> findByEventId(Long eventId);
    Optional<Certificate> findByCertificateId(String certificateId);
    Optional<Certificate> findByEventIdAndVolunteerId(Long eventId, Long volunteerId);
    boolean existsByEventIdAndVolunteerId(Long eventId, Long volunteerId);
}
