package com.volunteermatch.service;

import com.volunteermatch.dto.ApplicationResponse;
import com.volunteermatch.dto.ApplyRequest;
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
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final EventRepository eventRepository;
    private final VolunteerProfileRepository volunteerProfileRepository;
    private final AiMatchResultRepository aiMatchResultRepository;
    private final NotificationService notificationService;

    @Transactional
    public ApplicationResponse applyForEvent(Long eventId, ApplyRequest request, Long userId) {
        if (request.getAvailabilityConfirmed() == null || !request.getAvailabilityConfirmed()) {
            throw new BadRequestException("You must confirm your actual availability for this event before submitting your application.");
        }

        if ("NO".equalsIgnoreCase(request.getAvailabilityType())) {
            throw new BadRequestException("You cannot apply if you are not available for this event.");
        }

        VolunteerProfile volunteer = volunteerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Volunteer profile not found for user: " + userId));

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        if (event.getStatus() != Event.EventStatus.ACTIVE) {
            throw new BadRequestException("Cannot apply to an event that is not active");
        }

        if (applicationRepository.existsByEventIdAndVolunteerId(eventId, volunteer.getId())) {
            throw new BadRequestException("You have already applied for this event");
        }

        Application application = Application.builder()
                .event(event)
                .volunteer(volunteer)
                .status(Application.ApplicationStatus.PENDING)
                .availabilityConfirmed(true)
                .availabilityType(request.getAvailabilityType() != null && !request.getAvailabilityType().isBlank() ? request.getAvailabilityType() : "FULL")
                .partialTimeRange(request.getPartialTimeRange())
                .preferredTask(request.getPreferredTask())
                .relevantSkills(request.getRelevantSkills())
                .willingOtherTasks(request.getWillingOtherTasks() != null ? request.getWillingOtherTasks() : true)
                .motivationMessage(request.getMotivationMessage())
                .build();

        Application saved = applicationRepository.save(application);

        // Notify organization of new application
        String orgMessage = String.format("New application received from %s for '%s'.",
                volunteer.getUser().getName(), event.getTitle());
        notificationService.sendNotification(
                event.getOrganization().getUser().getId(),
                orgMessage,
                Notification.NotificationType.APPLICATION
        );

        // Notify volunteer that application is pending
        notificationService.sendNotification(
                userId,
                String.format("Your application for '%s' is submitted and currently under review by %s.",
                        event.getTitle(), event.getOrganization().getOrganizationName()),
                Notification.NotificationType.APPLICATION
        );

        return mapToResponse(saved);
    }

    @Transactional
    public ApplicationResponse updateApplicationStatus(Long applicationId, String statusStr, Long orgUserId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + applicationId));

        Event event = application.getEvent();
        if (!event.getOrganization().getUser().getId().equals(orgUserId)) {
            throw new UnauthorizedException("You are not authorized to update applications for this organization");
        }

        Application.ApplicationStatus status;
        try {
            status = Application.ApplicationStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status. Allowed values: SELECTED, REJECTED, PENDING");
        }

        if (status == Application.ApplicationStatus.SELECTED) {
            // Rule B: Can only select if currently PENDING
            if (application.getStatus() != Application.ApplicationStatus.PENDING) {
                throw new BadRequestException("Cannot select volunteer: Application must be PENDING (current status: " + application.getStatus() + ")");
            }

            // Rule C: Availability confirmed must be true
            if (!Boolean.TRUE.equals(application.getAvailabilityConfirmed())) {
                throw new BadRequestException("Cannot select volunteer: Availability is not confirmed for this event.");
            }

            // Rule D: Event must be ACTIVE
            if (event.getStatus() != Event.EventStatus.ACTIVE) {
                throw new BadRequestException("Cannot select volunteers for an event that is not ACTIVE.");
            }

            // Rule F: Check event capacity limit
            long currentSelected = applicationRepository.countByEventIdAndStatus(event.getId(), Application.ApplicationStatus.SELECTED);
            if (currentSelected >= event.getRequiredVolunteers()) {
                throw new BadRequestException(String.format("Cannot select volunteer: Event capacity limit of %d volunteers has already been reached.",
                        event.getRequiredVolunteers()));
            }

            // Rule H: Set selectedAt timestamp
            application.setSelectedAt(java.time.LocalDateTime.now());
        }

        // Rule G: Update existing application record in place
        application.setStatus(status);
        Application updated = applicationRepository.save(application);

        Long volunteerUserId = application.getVolunteer().getUser().getId();
        if (status == Application.ApplicationStatus.SELECTED) {
            String msg = String.format("Congratulations! You have been selected for '%s' on %s.",
                    event.getTitle(), event.getEventDate());
            notificationService.sendNotification(volunteerUserId, msg, Notification.NotificationType.SELECTION);
        } else if (status == Application.ApplicationStatus.REJECTED) {
            String msg = String.format("Your application for '%s' was not selected this time. Thank you for your support!",
                    event.getTitle());
            notificationService.sendNotification(volunteerUserId, msg, Notification.NotificationType.APPLICATION);
        }

        return mapToResponse(updated);
    }

    @Transactional
    public ApplicationResponse selectVolunteerForEvent(Long eventId, Long volunteerId, Long orgUserId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        if (!event.getOrganization().getUser().getId().equals(orgUserId)) {
            throw new UnauthorizedException("You are not authorized to update applications for this organization");
        }

        // Rule A: Organization can ONLY select a volunteer who has an existing application
        Application application = applicationRepository.findByEventIdAndVolunteerId(eventId, volunteerId)
                .orElseThrow(() -> new BadRequestException("Cannot select: Volunteer has not applied for this event."));

        return updateApplicationStatus(application.getId(), "SELECTED", orgUserId);
    }

    @Transactional
    public ApplicationResponse withdrawApplication(Long applicationId, Long userId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found with id: " + applicationId));

        if (!application.getVolunteer().getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You can only withdraw your own applications");
        }

        boolean wasSelected = application.getStatus() == Application.ApplicationStatus.SELECTED;
        application.setStatus(Application.ApplicationStatus.WITHDRAWN);
        Application updated = applicationRepository.save(application);

        Event event = application.getEvent();
        String volunteerName = application.getVolunteer().getUser().getName();

        // RULE 6: When a selected volunteer withdraws, notify the organization
        String orgMsg = String.format("Alert: %s has withdrawn from '%s'. You may review recommendations to select another volunteer.",
                volunteerName, event.getTitle());
        notificationService.sendNotification(
                event.getOrganization().getUser().getId(),
                orgMsg,
                Notification.NotificationType.WITHDRAWAL
        );

        // Notify volunteer confirmation
        notificationService.sendNotification(
                userId,
                String.format("You have successfully withdrawn from '%s'.", event.getTitle()),
                Notification.NotificationType.WITHDRAWAL
        );

        return mapToResponse(updated);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplicationsByEvent(Long eventId) {
        return applicationRepository.findByEventIdOrderByAppliedAtDesc(eventId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplicationsByVolunteer(Long volunteerId) {
        return applicationRepository.findByVolunteerIdOrderByAppliedAtDesc(volunteerId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplicationsByOrganization(Long orgId) {
        return applicationRepository.findByEventOrganizationIdOrderByAppliedAtDesc(orgId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private ApplicationResponse mapToResponse(Application app) {
        VolunteerProfile vol = app.getVolunteer();
        Event event = app.getEvent();

        Optional<AiMatchResult> matchOpt = aiMatchResultRepository.findByEventIdAndVolunteerId(event.getId(), vol.getId());

        return ApplicationResponse.builder()
                .id(app.getId())
                .eventId(event.getId())
                .eventTitle(event.getTitle())
                .organizationName(event.getOrganization().getOrganizationName())
                .eventDate(event.getEventDate())
                .eventTime(event.getStartTime() + " - " + event.getEndTime())
                .eventLocation(event.getLocation())
                .volunteerId(vol.getId())
                .volunteerName(vol.getUser() != null ? vol.getUser().getName() : "")
                .volunteerEmail(vol.getUser() != null ? vol.getUser().getEmail() : "")
                .volunteerBio(vol.getBio())
                .volunteerSkills(vol.getSkills())
                .volunteerInterests(vol.getInterests())
                .volunteerLocation(vol.getLocation())
                .volunteerAvailability(vol.getAvailability())
                .volunteerExperience(vol.getExperience())
                .volunteerExperienceLevel(vol.getExperienceLevel())
                .volunteerImage(vol.getProfileImage())
                .status(app.getStatus().name())
                .availabilityConfirmed(app.getAvailabilityConfirmed())
                .availabilityType(app.getAvailabilityType())
                .partialTimeRange(app.getPartialTimeRange())
                .preferredTask(app.getPreferredTask())
                .relevantSkills(app.getRelevantSkills())
                .willingOtherTasks(app.getWillingOtherTasks())
                .motivationMessage(app.getMotivationMessage())
                .selectedAt(app.getSelectedAt())
                .appliedAt(app.getAppliedAt())
                .matchScore(matchOpt.map(AiMatchResult::getMatchScore).orElse(null))
                .matchReason(matchOpt.map(AiMatchResult::getReason).orElse(null))
                .build();
    }
}
