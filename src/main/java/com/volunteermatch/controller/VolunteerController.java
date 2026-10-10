package com.volunteermatch.controller;

import com.volunteermatch.dto.ApiResponse;
import com.volunteermatch.dto.DashboardStatsResponse;
import com.volunteermatch.dto.VolunteerProfileRequest;
import com.volunteermatch.dto.VolunteerResponse;
import com.volunteermatch.security.UserPrincipal;
import com.volunteermatch.service.VolunteerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/volunteers")
@RequiredArgsConstructor
public class VolunteerController {

    private final VolunteerService volunteerService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<VolunteerResponse>>> getAllVolunteers() {
        List<VolunteerResponse> volunteers = volunteerService.getAllVolunteers();
        return ResponseEntity.ok(ApiResponse.success(volunteers));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VolunteerResponse>> getVolunteerById(@PathVariable Long id) {
        VolunteerResponse response = volunteerService.getProfileById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<VolunteerResponse>> getMyProfile(@AuthenticationPrincipal UserPrincipal currentUser) {
        VolunteerResponse response = volunteerService.getProfileByUserId(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<VolunteerResponse>> updateMyProfile(@AuthenticationPrincipal UserPrincipal currentUser,
                                                                         @RequestBody VolunteerProfileRequest request) {
        VolunteerResponse profile = volunteerService.getProfileByUserId(currentUser.getId());
        VolunteerResponse response = volunteerService.updateProfile(profile.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VolunteerResponse>> updateProfile(@PathVariable Long id,
                                                                       @RequestBody VolunteerProfileRequest request) {
        VolunteerResponse response = volunteerService.updateProfile(id, request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", response));
    }

    @GetMapping("/dashboard-stats")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStats(@AuthenticationPrincipal UserPrincipal currentUser) {
        VolunteerResponse profile = volunteerService.getProfileByUserId(currentUser.getId());
        DashboardStatsResponse stats = volunteerService.getDashboardStats(profile.getId());
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
