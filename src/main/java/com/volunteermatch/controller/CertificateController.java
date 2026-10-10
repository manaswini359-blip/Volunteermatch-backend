package com.volunteermatch.controller;

import com.volunteermatch.dto.ApiResponse;
import com.volunteermatch.dto.CertificateRequest;
import com.volunteermatch.dto.CertificateResponse;
import com.volunteermatch.security.UserPrincipal;
import com.volunteermatch.service.CertificateService;
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
public class CertificateController {

    private final CertificateService certificateService;

    @PostMapping("/certificates")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public ResponseEntity<ApiResponse<CertificateResponse>> issueCertificate(@Valid @RequestBody CertificateRequest request,
                                                                             @AuthenticationPrincipal UserPrincipal currentUser) {
        CertificateResponse response = certificateService.issueCertificate(request, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Certificate issued successfully!", response));
    }

    @GetMapping("/volunteers/{volunteerId}/certificates")
    public ResponseEntity<ApiResponse<List<CertificateResponse>>> getVolunteerCertificates(@PathVariable Long volunteerId) {
        List<CertificateResponse> list = certificateService.getCertificatesByVolunteer(volunteerId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/events/{eventId}/certificates")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public ResponseEntity<ApiResponse<List<CertificateResponse>>> getEventCertificates(@PathVariable Long eventId) {
        List<CertificateResponse> list = certificateService.getCertificatesByEvent(eventId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/certificates/{certificateId}")
    public ResponseEntity<ApiResponse<CertificateResponse>> getCertificateById(@PathVariable String certificateId) {
        CertificateResponse response = certificateService.getCertificateById(certificateId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
