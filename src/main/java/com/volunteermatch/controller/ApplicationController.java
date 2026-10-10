package com.volunteermatch.controller;

import com.volunteermatch.dto.ApiResponse;
import com.volunteermatch.dto.ApplicationResponse;
import com.volunteermatch.dto.ApplyRequest;
import com.volunteermatch.dto.StatusUpdateRequest;
import com.volunteermatch.security.UserPrincipal;
import com.volunteermatch.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping("/events/{eventId}/apply")
    @PreAuthorize("hasRole('VOLUNTEER')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> applyForEvent(@PathVariable Long eventId,
                                                                         @Valid @RequestBody ApplyRequest request,
                                                                         @AuthenticationPrincipal UserPrincipal currentUser) {
        ApplicationResponse response = applicationService.applyForEvent(eventId, request, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Application submitted successfully! It is now pending organization review.", response));
    }

    @GetMapping("/events/{eventId}/applications")
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> getApplicationsForEvent(@PathVariable Long eventId) {
        List<ApplicationResponse> list = applicationService.getApplicationsByEvent(eventId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/volunteers/{volunteerId}/applications")
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> getApplicationsForVolunteer(@PathVariable Long volunteerId) {
        List<ApplicationResponse> list = applicationService.getApplicationsByVolunteer(volunteerId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/organizations/{orgId}/applications")
    public ResponseEntity<ApiResponse<List<ApplicationResponse>>> getApplicationsForOrganization(@PathVariable Long orgId) {
        List<ApplicationResponse> list = applicationService.getApplicationsByOrganization(orgId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PutMapping("/applications/{id}/status")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> updateApplicationStatus(@PathVariable Long id,
                                                                                   @Valid @RequestBody StatusUpdateRequest request,
                                                                                   @AuthenticationPrincipal UserPrincipal currentUser) {
        ApplicationResponse response = applicationService.updateApplicationStatus(id, request.getStatus(), currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Application status updated to " + request.getStatus(), response));
    }

    @PostMapping("/events/{eventId}/select-volunteer/{volunteerId}")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> selectVolunteerForEvent(@PathVariable Long eventId,
                                                                                   @PathVariable Long volunteerId,
                                                                                   @AuthenticationPrincipal UserPrincipal currentUser) {
        ApplicationResponse response = applicationService.selectVolunteerForEvent(eventId, volunteerId, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Volunteer selected successfully for the event.", response));
    }

    @PutMapping("/applications/{id}/withdraw")
    @PreAuthorize("hasRole('VOLUNTEER')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> withdrawApplication(@PathVariable Long id,
                                                                               @AuthenticationPrincipal UserPrincipal currentUser) {
        ApplicationResponse response = applicationService.withdrawApplication(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Successfully withdrawn from the event.", response));
    }
}
