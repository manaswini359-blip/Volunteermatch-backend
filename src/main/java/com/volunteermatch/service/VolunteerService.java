package com.volunteermatch.service;

import com.volunteermatch.dto.DashboardStatsResponse;
import com.volunteermatch.dto.VolunteerProfileRequest;
import com.volunteermatch.dto.VolunteerResponse;
import com.volunteermatch.entity.Application;
import com.volunteermatch.entity.Event;
import com.volunteermatch.entity.User;
import com.volunteermatch.entity.VolunteerProfile;
import com.volunteermatch.exception.ResourceNotFoundException;
import com.volunteermatch.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VolunteerService {

    private final VolunteerProfileRepository volunteerProfileRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final EventRepository eventRepository;
    private final CertificateRepository certificateRepository;

    @Transactional(readOnly = true)
    public VolunteerResponse getProfileById(Long id) {
        VolunteerProfile profile = volunteerProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Volunteer profile not found with id: " + id));
        return mapToResponse(profile);
    }

    @Transactional(readOnly = true)
    public VolunteerResponse getProfileByUserId(Long userId) {
        VolunteerProfile profile = volunteerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Volunteer profile not found for user: " + userId));
        return mapToResponse(profile);
    }

    @Transactional(readOnly = true)
    public List<VolunteerResponse> getAllVolunteers() {
        return volunteerProfileRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public VolunteerResponse updateProfile(Long id, VolunteerProfileRequest request) {
        VolunteerProfile profile = volunteerProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Volunteer profile not found with id: " + id));

        if (request.getName() != null && profile.getUser() != null) {
            User user = profile.getUser();
            user.setName(request.getName());
            userRepository.save(user);
        }

        if (request.getBio() != null) profile.setBio(request.getBio());
        if (request.getSkills() != null) profile.setSkills(request.getSkills());
        if (request.getInterests() != null) profile.setInterests(request.getInterests());
        if (request.getLocation() != null) profile.setLocation(request.getLocation());
        if (request.getAvailability() != null) profile.setAvailability(request.getAvailability());
        if (request.getExperienceLevel() != null) profile.setExperienceLevel(request.getExperienceLevel());
        if (request.getExperience() != null) profile.setExperience(request.getExperience());
        if (request.getProfileImage() != null) profile.setProfileImage(request.getProfileImage());

        VolunteerProfile updated = volunteerProfileRepository.save(profile);
        return mapToResponse(updated);
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats(Long volunteerId) {
        List<Application> apps = applicationRepository.findByVolunteerIdOrderByAppliedAtDesc(volunteerId);
        long totalApps = apps.size();
        long pendingApps = apps.stream()
                .filter(a -> a.getStatus() == Application.ApplicationStatus.PENDING)
                .count();
        long selectedEvents = apps.stream()
                .filter(a -> a.getStatus() == Application.ApplicationStatus.SELECTED)
                .count();
        long notSelectedEvents = apps.stream()
                .filter(a -> a.getStatus() == Application.ApplicationStatus.REJECTED)
                .count();
        long withdrawnApps = apps.stream()
                .filter(a -> a.getStatus() == Application.ApplicationStatus.WITHDRAWN)
                .count();
        long activeEvents = eventRepository.findByStatusOrderByEventDateAsc(Event.EventStatus.ACTIVE).size();
        long certificates = certificateRepository.findByVolunteerIdOrderByIssuedDateDesc(volunteerId).size();

        return DashboardStatsResponse.builder()
                .availableOpportunities(activeEvents)
                .myApplications(totalApps)
                .pendingApplications(pendingApps)
                .selectedEvents(selectedEvents)
                .notSelectedEvents(notSelectedEvents)
                .withdrawnApplications(withdrawnApps)
                .completedEvents(certificates)
                .totalCertificates(certificates)
                .build();
    }

    public VolunteerResponse mapToResponse(VolunteerProfile profile) {
        return VolunteerResponse.builder()
                .id(profile.getId())
                .userId(profile.getUser() != null ? profile.getUser().getId() : null)
                .name(profile.getUser() != null ? profile.getUser().getName() : "")
                .email(profile.getUser() != null ? profile.getUser().getEmail() : "")
                .bio(profile.getBio())
                .skills(profile.getSkills())
                .interests(profile.getInterests())
                .location(profile.getLocation())
                .availability(profile.getAvailability())
                .profileImage(profile.getProfileImage())
                .experienceLevel(profile.getExperienceLevel() != null ? profile.getExperienceLevel() : "Beginner")
                .experience(profile.getExperience())
                .build();
    }
}
