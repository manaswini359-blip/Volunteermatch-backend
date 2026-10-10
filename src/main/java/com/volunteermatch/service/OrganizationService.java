package com.volunteermatch.service;

import com.volunteermatch.dto.DashboardStatsResponse;
import com.volunteermatch.dto.OrganizationRequest;
import com.volunteermatch.dto.OrganizationResponse;
import com.volunteermatch.entity.Application;
import com.volunteermatch.entity.Event;
import com.volunteermatch.entity.Organization;
import com.volunteermatch.exception.ResourceNotFoundException;
import com.volunteermatch.repository.ApplicationRepository;
import com.volunteermatch.repository.EventRepository;
import com.volunteermatch.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final EventRepository eventRepository;
    private final ApplicationRepository applicationRepository;

    @Transactional(readOnly = true)
    public OrganizationResponse getProfileById(Long id) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + id));
        return mapToResponse(org);
    }

    @Transactional(readOnly = true)
    public OrganizationResponse getProfileByUserId(Long userId) {
        Organization org = organizationRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found for user id: " + userId));
        return mapToResponse(org);
    }

    @Transactional
    public OrganizationResponse updateProfile(Long id, OrganizationRequest request) {
        Organization org = organizationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + id));

        if (request.getOrganizationName() != null) org.setOrganizationName(request.getOrganizationName());
        if (request.getDescription() != null) org.setDescription(request.getDescription());
        if (request.getLocation() != null) org.setLocation(request.getLocation());
        if (request.getProfileImage() != null) org.setProfileImage(request.getProfileImage());

        Organization updated = organizationRepository.save(org);
        return mapToResponse(updated);
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats(Long organizationId) {
        List<Event> orgEvents = eventRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId);
        long totalEvents = orgEvents.size();
        long activeEvents = orgEvents.stream()
                .filter(e -> e.getStatus() == Event.EventStatus.ACTIVE)
                .count();

        List<Application> orgApps = applicationRepository.findByEventOrganizationIdOrderByAppliedAtDesc(organizationId);
        long totalApplications = orgApps.size();
        long selectedVolunteers = orgApps.stream()
                .filter(a -> a.getStatus() == Application.ApplicationStatus.SELECTED)
                .count();

        return DashboardStatsResponse.builder()
                .totalEvents(totalEvents)
                .activeEvents(activeEvents)
                .totalApplications(totalApplications)
                .selectedVolunteers(selectedVolunteers)
                .build();
    }

    public OrganizationResponse mapToResponse(Organization org) {
        return OrganizationResponse.builder()
                .id(org.getId())
                .userId(org.getUser() != null ? org.getUser().getId() : null)
                .organizationName(org.getOrganizationName())
                .email(org.getUser() != null ? org.getUser().getEmail() : "")
                .description(org.getDescription())
                .location(org.getLocation())
                .profileImage(org.getProfileImage())
                .build();
    }
}
