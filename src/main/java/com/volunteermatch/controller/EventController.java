package com.volunteermatch.controller;

import com.volunteermatch.dto.ApiResponse;
import com.volunteermatch.dto.EventRequest;
import com.volunteermatch.dto.EventResponse;
import com.volunteermatch.security.UserPrincipal;
import com.volunteermatch.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<EventResponse>>> getAllEvents(@AuthenticationPrincipal UserPrincipal currentUser) {
        Long userId = currentUser != null ? currentUser.getId() : null;
        List<EventResponse> events = eventService.getAllActiveEvents(userId);
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> getEventById(@PathVariable Long id,
                                                                  @AuthenticationPrincipal UserPrincipal currentUser) {
        Long userId = currentUser != null ? currentUser.getId() : null;
        EventResponse event = eventService.getEventById(id, userId);
        return ResponseEntity.ok(ApiResponse.success(event));
    }

    @GetMapping("/organization/{orgId}")
    public ResponseEntity<ApiResponse<List<EventResponse>>> getEventsByOrganization(@PathVariable Long orgId,
                                                                                   @AuthenticationPrincipal UserPrincipal currentUser) {
        Long userId = currentUser != null ? currentUser.getId() : null;
        List<EventResponse> events = eventService.getEventsByOrganization(orgId, userId);
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    @PostMapping
    @PreAuthorize("hasRole('ORGANIZATION')")
    public ResponseEntity<ApiResponse<EventResponse>> createEvent(@Valid @RequestBody EventRequest request,
                                                                 @AuthenticationPrincipal UserPrincipal currentUser) {
        EventResponse event = eventService.createEvent(request, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Event created successfully", event));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public ResponseEntity<ApiResponse<EventResponse>> updateEvent(@PathVariable Long id,
                                                                 @Valid @RequestBody EventRequest request,
                                                                 @AuthenticationPrincipal UserPrincipal currentUser) {
        EventResponse event = eventService.updateEvent(id, request, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Event updated successfully", event));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public ResponseEntity<ApiResponse<Void>> deleteEvent(@PathVariable Long id,
                                                        @AuthenticationPrincipal UserPrincipal currentUser) {
        eventService.deleteEvent(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Event deleted successfully", null));
    }
}
