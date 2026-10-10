package com.volunteermatch.service;

import com.volunteermatch.dto.CertificateRequest;
import com.volunteermatch.dto.CertificateResponse;
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
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CertificateService {

    private final CertificateRepository certificateRepository;
    private final EventRepository eventRepository;
    private final VolunteerProfileRepository volunteerProfileRepository;
    private final AttendanceRepository attendanceRepository;
    private final TaskRepository taskRepository;
    private final NotificationService notificationService;

    @Transactional
    public CertificateResponse issueCertificate(CertificateRequest request, Long orgUserId) {
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + request.getEventId()));

        if (!event.getOrganization().getUser().getId().equals(orgUserId)) {
            throw new UnauthorizedException("You are not authorized to issue certificates for this organization");
        }

        VolunteerProfile volunteer = volunteerProfileRepository.findById(request.getVolunteerId())
                .orElseThrow(() -> new ResourceNotFoundException("Volunteer not found with id: " + request.getVolunteerId()));

        // RULE 9: Only attendance-marked participants (PRESENT) are eligible for certificates
        Attendance attendance = attendanceRepository.findByEventIdAndVolunteerId(event.getId(), volunteer.getId())
                .orElseThrow(() -> new BadRequestException("Volunteer attendance has not been recorded for this event."));

        if (attendance.getStatus() != Attendance.AttendanceStatus.PRESENT) {
            throw new BadRequestException("Certificates can only be issued to volunteers whose attendance is marked as PRESENT (current: " + attendance.getStatus() + ")");
        }

        if (certificateRepository.existsByEventIdAndVolunteerId(event.getId(), volunteer.getId())) {
            throw new BadRequestException("Certificate has already been issued to this volunteer for this event.");
        }

        String certId = "VM-CERT-2026-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Certificate cert = Certificate.builder()
                .event(event)
                .volunteer(volunteer)
                .certificateId(certId)
                .certificateUrl("/api/certificates/" + certId + "/download")
                .build();

        Certificate saved = certificateRepository.save(cert);

        // Notify volunteer
        notificationService.sendNotification(
                volunteer.getUser().getId(),
                String.format("Your Certificate of Appreciation for '%s' has been issued! Certificate ID: %s.",
                        event.getTitle(), certId),
                Notification.NotificationType.CERTIFICATE
        );

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CertificateResponse> getCertificatesByVolunteer(Long volunteerId) {
        return certificateRepository.findByVolunteerIdOrderByIssuedDateDesc(volunteerId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CertificateResponse> getCertificatesByEvent(Long eventId) {
        return certificateRepository.findByEventId(eventId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CertificateResponse getCertificateById(String certificateId) {
        Certificate cert = certificateRepository.findByCertificateId(certificateId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found with ID: " + certificateId));
        return mapToResponse(cert);
    }

    private CertificateResponse mapToResponse(Certificate cert) {
        List<Task> tasks = taskRepository.findByEventIdAndVolunteerId(cert.getEvent().getId(), cert.getVolunteer().getId());
        String roleTask = tasks.isEmpty() ? "Community Volunteer" : tasks.get(0).getTaskName();

        return CertificateResponse.builder()
                .id(cert.getId())
                .certificateId(cert.getCertificateId())
                .eventId(cert.getEvent().getId())
                .eventTitle(cert.getEvent().getTitle())
                .organizationName(cert.getEvent().getOrganization().getOrganizationName())
                .eventDate(cert.getEvent().getEventDate())
                .volunteerId(cert.getVolunteer().getId())
                .volunteerName(cert.getVolunteer().getUser() != null ? cert.getVolunteer().getUser().getName() : "")
                .volunteerEmail(cert.getVolunteer().getUser() != null ? cert.getVolunteer().getUser().getEmail() : "")
                .roleTask(roleTask)
                .certificateUrl(cert.getCertificateUrl())
                .issuedDate(cert.getIssuedDate())
                .build();
    }
}
