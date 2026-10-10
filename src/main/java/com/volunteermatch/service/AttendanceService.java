package com.volunteermatch.service;

import com.volunteermatch.dto.AttendanceRequest;
import com.volunteermatch.dto.AttendanceResponse;
import com.volunteermatch.entity.*;
import com.volunteermatch.exception.BadRequestException;
import com.volunteermatch.exception.ResourceNotFoundException;
import com.volunteermatch.exception.UnauthorizedException;
import com.volunteermatch.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EventRepository eventRepository;
    private final VolunteerProfileRepository volunteerProfileRepository;
    private final ApplicationRepository applicationRepository;
    private final CertificateRepository certificateRepository;
    private final NotificationService notificationService;

    @Transactional
    public AttendanceResponse markAttendance(AttendanceRequest request, Long orgUserId) {
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + request.getEventId()));

        if (!event.getOrganization().getUser().getId().equals(orgUserId)) {
            throw new UnauthorizedException("You are not authorized to mark attendance for this event");
        }

        VolunteerProfile volunteer = volunteerProfileRepository.findById(request.getVolunteerId())
                .orElseThrow(() -> new ResourceNotFoundException("Volunteer not found with id: " + request.getVolunteerId()));

        // Check that volunteer was selected
        Application application = applicationRepository.findByEventIdAndVolunteerId(event.getId(), volunteer.getId())
                .orElseThrow(() -> new BadRequestException("Volunteer has not applied for this event"));

        if (application.getStatus() != Application.ApplicationStatus.SELECTED) {
            throw new BadRequestException("Attendance can only be recorded for SELECTED volunteers (current status: " + application.getStatus() + ")");
        }

        Attendance.AttendanceStatus status;
        try {
            status = Attendance.AttendanceStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid attendance status. Allowed: PRESENT, ABSENT");
        }

        Attendance attendance = attendanceRepository.findByEventIdAndVolunteerId(event.getId(), volunteer.getId())
                .orElse(Attendance.builder()
                        .event(event)
                        .volunteer(volunteer)
                        .build());

        attendance.setStatus(status);
        Attendance saved = attendanceRepository.save(attendance);

        if (status == Attendance.AttendanceStatus.PRESENT) {
            notificationService.sendNotification(
                    volunteer.getUser().getId(),
                    String.format("Your attendance for '%s' has been marked as PRESENT. Thank you for your service!", event.getTitle()),
                    Notification.NotificationType.ATTENDANCE
            );
        }

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendanceByEvent(Long eventId) {
        return attendanceRepository.findByEventId(eventId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendanceByVolunteer(Long volunteerId) {
        return attendanceRepository.findByVolunteerId(volunteerId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private AttendanceResponse mapToResponse(Attendance att) {
        boolean certExists = certificateRepository.existsByEventIdAndVolunteerId(att.getEvent().getId(), att.getVolunteer().getId());
        return AttendanceResponse.builder()
                .id(att.getId())
                .eventId(att.getEvent().getId())
                .eventTitle(att.getEvent().getTitle())
                .volunteerId(att.getVolunteer().getId())
                .volunteerName(att.getVolunteer().getUser() != null ? att.getVolunteer().getUser().getName() : "")
                .volunteerEmail(att.getVolunteer().getUser() != null ? att.getVolunteer().getUser().getEmail() : "")
                .status(att.getStatus().name())
                .checkInTime(att.getCheckInTime())
                .certificateIssued(certExists)
                .build();
    }
}
