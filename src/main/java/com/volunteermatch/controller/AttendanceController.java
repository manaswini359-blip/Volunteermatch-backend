package com.volunteermatch.controller;

import com.volunteermatch.dto.ApiResponse;
import com.volunteermatch.dto.AttendanceRequest;
import com.volunteermatch.dto.AttendanceResponse;
import com.volunteermatch.security.UserPrincipal;
import com.volunteermatch.service.AttendanceService;
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
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/attendance")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public ResponseEntity<ApiResponse<AttendanceResponse>> markAttendance(@Valid @RequestBody AttendanceRequest request,
                                                                         @AuthenticationPrincipal UserPrincipal currentUser) {
        AttendanceResponse response = attendanceService.markAttendance(request, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Attendance marked as " + request.getStatus(), response));
    }

    @GetMapping("/events/{eventId}/attendance")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getEventAttendance(@PathVariable Long eventId) {
        List<AttendanceResponse> list = attendanceService.getAttendanceByEvent(eventId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/volunteers/{volunteerId}/attendance")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> getVolunteerAttendance(@PathVariable Long volunteerId) {
        List<AttendanceResponse> list = attendanceService.getAttendanceByVolunteer(volunteerId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }
}
