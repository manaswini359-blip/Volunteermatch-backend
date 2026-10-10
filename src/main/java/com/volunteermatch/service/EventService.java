package com.volunteermatch.service;

import com.volunteermatch.ai.AIService;
import com.volunteermatch.dto.AiAnalysisResponse;
import com.volunteermatch.dto.EventRequest;
import com.volunteermatch.dto.EventResponse;
import com.volunteermatch.entity.*;
import com.volunteermatch.exception.BadRequestException;
import com.volunteermatch.exception.ResourceNotFoundException;
import com.volunteermatch.exception.UnauthorizedException;
import com.volunteermatch.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final OrganizationRepository organizationRepository;
    private final EventRequirementRepository eventRequirementRepository;
    private final ApplicationRepository applicationRepository;
    private final VolunteerProfileRepository volunteerProfileRepository;
    private final AIService aiService;
    private final MatchingService matchingService;

    @Transactional
    public EventResponse createEvent(EventRequest request, Long userId) {
        Organization organization = organizationRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Organization not found for user: " + userId));

        Event event = Event.builder()
                .organization(organization)
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory() != null ? request.getCategory() : "Social Service")
                .location(request.getLocation())
                .eventDate(request.getEventDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .requiredVolunteers(request.getRequiredVolunteers() != null ? request.getRequiredVolunteers() : 5)
                .status(Event.EventStatus.ACTIVE)
                .build();

        Event savedEvent = eventRepository.save(event);

        // Run AI Analysis for event requirements
        List<String> skills = request.getRequiredSkills();
        List<String> tasks = request.getTasks();

        if (skills == null || skills.isEmpty() || tasks == null || tasks.isEmpty()) {
            AiAnalysisResponse aiAnalysis = aiService.analyzeEventDescription(
                    request.getTitle(),
                    request.getDescription(),
                    request.getCategory()
            );

            if (skills == null || skills.isEmpty()) {
                skills = aiAnalysis.getRequiredSkills();
            }
            if (tasks == null || tasks.isEmpty()) {
                tasks = aiAnalysis.getTasks();
            }
        }

        List<EventRequirement> reqEntities = new ArrayList<>();
        if (skills != null) {
            for (String s : skills) {
                if (s == null || s.trim().isEmpty()) continue;
                String trimmed = s.trim();
                if (trimmed.length() > 255) trimmed = trimmed.substring(0, 255);
                reqEntities.add(EventRequirement.builder()
                        .event(savedEvent)
                        .requiredSkill(trimmed)
                        .requirementType(EventRequirement.RequirementType.SKILL)
                        .build());
            }
        }

        if (tasks != null) {
            for (String t : tasks) {
                if (t == null || t.trim().isEmpty()) continue;
                String trimmed = t.trim();
                if (trimmed.length() > 500) trimmed = trimmed.substring(0, 500);
                reqEntities.add(EventRequirement.builder()
                        .event(savedEvent)
                        .requiredTask(trimmed)
                        .requirementType(EventRequirement.RequirementType.TASK)
                        .build());
            }
        }

        eventRequirementRepository.saveAll(reqEntities);
        savedEvent.setRequirements(reqEntities);

        // Pre-compute AI match recommendations
        try {
            matchingService.matchVolunteersForEvent(savedEvent.getId());
        } catch (Exception e) {
            log.warn("Could not pre-calculate matching: {}", e.getMessage());
        }

        return mapToResponse(savedEvent, null);
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getAllActiveEvents(Long currentUserId) {
        return eventRepository.findByStatusOrderByEventDateAsc(Event.EventStatus.ACTIVE)
                .stream()
                .map(e -> mapToResponse(e, currentUserId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EventResponse> getEventsByOrganization(Long orgId, Long currentUserId) {
        return eventRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId)
                .stream()
                .map(e -> mapToResponse(e, currentUserId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EventResponse getEventById(Long eventId, Long currentUserId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));
        return mapToResponse(event, currentUserId);
    }

    @Transactional
    public EventResponse updateEvent(Long eventId, EventRequest request, Long userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        if (!event.getOrganization().getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to update this event");
        }

        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setCategory(request.getCategory());
        event.setLocation(request.getLocation());
        event.setEventDate(request.getEventDate());
        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());
        if (request.getRequiredVolunteers() != null) {
            event.setRequiredVolunteers(request.getRequiredVolunteers());
        }

        Event updated = eventRepository.save(event);
        return mapToResponse(updated, userId);
    }

    @Transactional
    public void deleteEvent(Long eventId, Long userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));

        if (!event.getOrganization().getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to delete this event");
        }

        eventRepository.delete(event);
    }

    public EventResponse mapToResponse(Event event, Long currentUserId) {
        List<EventRequirement> reqs = event.getRequirements() != null ? event.getRequirements() :
                eventRequirementRepository.findByEventId(event.getId());

        List<String> skills = reqs.stream()
                .filter(r -> r.getRequirementType() == EventRequirement.RequirementType.SKILL && r.getRequiredSkill() != null)
                .map(EventRequirement::getRequiredSkill)
                .collect(Collectors.toList());

        List<String> tasks = reqs.stream()
                .filter(r -> r.getRequirementType() == EventRequirement.RequirementType.TASK && r.getRequiredTask() != null)
                .map(EventRequirement::getRequiredTask)
                .collect(Collectors.toList());

        long totalApps = applicationRepository.countByEventId(event.getId());
        long selected = applicationRepository.countByEventIdAndStatus(event.getId(), Application.ApplicationStatus.SELECTED);

        Boolean userApplied = false;
        String appStatus = null;

        if (currentUserId != null) {
            Optional<VolunteerProfile> volProfile = volunteerProfileRepository.findByUserId(currentUserId);
            if (volProfile.isPresent()) {
                Optional<Application> app = applicationRepository.findByEventIdAndVolunteerId(event.getId(), volProfile.get().getId());
                if (app.isPresent()) {
                    userApplied = true;
                    appStatus = app.get().getStatus().name();
                }
            }
        }

        return EventResponse.builder()
                .id(event.getId())
                .organizationId(event.getOrganization().getId())
                .organizationName(event.getOrganization().getOrganizationName())
                .organizationImage(event.getOrganization().getProfileImage())
                .organizationLocation(event.getOrganization().getLocation())
                .title(event.getTitle())
                .description(event.getDescription())
                .category(event.getCategory())
                .location(event.getLocation())
                .eventDate(event.getEventDate())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .requiredVolunteers(event.getRequiredVolunteers())
                .status(event.getStatus().name())
                .createdAt(event.getCreatedAt())
                .requiredSkills(skills)
                .tasks(tasks)
                .totalApplications(totalApps)
                .selectedCount(selected)
                .userHasApplied(userApplied)
                .userApplicationStatus(appStatus)
                .build();
    }
}
