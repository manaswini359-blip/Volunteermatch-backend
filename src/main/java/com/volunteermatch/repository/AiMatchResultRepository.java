package com.volunteermatch.repository;

import com.volunteermatch.entity.AiMatchResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiMatchResultRepository extends JpaRepository<AiMatchResult, Long> {
    List<AiMatchResult> findByEventIdOrderByMatchScoreDesc(Long eventId);
    Optional<AiMatchResult> findByEventIdAndVolunteerId(Long eventId, Long volunteerId);
    void deleteByEventId(Long eventId);
}
