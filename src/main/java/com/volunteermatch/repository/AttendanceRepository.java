package com.volunteermatch.repository;

import com.volunteermatch.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByEventId(Long eventId);
    List<Attendance> findByVolunteerId(Long volunteerId);
    Optional<Attendance> findByEventIdAndVolunteerId(Long eventId, Long volunteerId);
}
