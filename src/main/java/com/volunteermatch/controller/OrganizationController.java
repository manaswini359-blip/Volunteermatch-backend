package com.volunteermatch.controller;

import com.volunteermatch.dto.ApiResponse;
import com.volunteermatch.dto.DashboardStatsResponse;
import com.volunteermatch.dto.OrganizationRequest;
import com.volunteermatch.dto.OrganizationResponse;
import com.volunteermatch.security.UserPrincipal;
import com.volunteermatch.service.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrganizationResponse>> getOrganizationById(@PathVariable Long id) {
        OrganizationResponse response = organizationService.getProfileById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<OrganizationResponse>> getMyProfile(@AuthenticationPrincipal UserPrincipal currentUser) {
        OrganizationResponse response = organizationService.getProfileByUserId(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<OrganizationResponse>> updateProfile(@PathVariable Long id,
                                                                          @RequestBody OrganizationRequest request) {
        OrganizationResponse response = organizationService.updateProfile(id, request);
        return ResponseEntity.ok(ApiResponse.success("Organization profile updated successfully", response));
    }

    @GetMapping("/dashboard-stats")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStats(@AuthenticationPrincipal UserPrincipal currentUser) {
        OrganizationResponse org = organizationService.getProfileByUserId(currentUser.getId());
        DashboardStatsResponse stats = organizationService.getDashboardStats(org.getId());
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
