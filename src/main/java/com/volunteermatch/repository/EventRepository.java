package com.volunteermatch.repository;

import com.volunteermatch.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByOrganizationIdOrderByCreatedAtDesc(Long organizationId);
    List<Event> findByStatusOrderByEventDateAsc(Event.EventStatus status);
    List<Event> findAllByOrderByCreatedAtDesc();
}
