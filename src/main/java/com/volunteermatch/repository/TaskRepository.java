package com.volunteermatch.repository;

import com.volunteermatch.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByVolunteerIdOrderByAssignedAtDesc(Long volunteerId);
    List<Task> findByEventIdOrderByAssignedAtDesc(Long eventId);
    List<Task> findByEventIdAndVolunteerId(Long eventId, Long volunteerId);
}
