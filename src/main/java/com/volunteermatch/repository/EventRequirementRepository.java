package com.volunteermatch.repository;

import com.volunteermatch.entity.EventRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventRequirementRepository extends JpaRepository<EventRequirement, Long> {
    List<EventRequirement> findByEventId(Long eventId);
    void deleteByEventId(Long eventId);
}
