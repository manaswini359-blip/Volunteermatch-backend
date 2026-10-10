package com.volunteermatch.controller;

import com.volunteermatch.dto.ApiResponse;
import com.volunteermatch.dto.TaskRequest;
import com.volunteermatch.dto.TaskResponse;
import com.volunteermatch.dto.TaskStatusRequest;
import com.volunteermatch.security.UserPrincipal;
import com.volunteermatch.service.TaskService;
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
public class TaskController {

    private final TaskService taskService;

    @PostMapping("/tasks")
    @PreAuthorize("hasRole('ORGANIZATION')")
    public ResponseEntity<ApiResponse<TaskResponse>> assignTask(@Valid @RequestBody TaskRequest request,
                                                                 @AuthenticationPrincipal UserPrincipal currentUser) {
        TaskResponse response = taskService.assignTask(request, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Task assigned successfully to volunteer", response));
    }

    @PutMapping("/tasks/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTaskStatus(@PathVariable Long id,
                                                                       @Valid @RequestBody TaskStatusRequest request,
                                                                       @AuthenticationPrincipal UserPrincipal currentUser) {
        TaskResponse response = taskService.updateTaskStatus(id, request, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Task status updated to " + request.getStatus(), response));
    }

    @GetMapping("/volunteers/{volunteerId}/tasks")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getTasksForVolunteer(@PathVariable Long volunteerId) {
        List<TaskResponse> list = taskService.getTasksByVolunteer(volunteerId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/events/{eventId}/tasks")
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getTasksForEvent(@PathVariable Long eventId) {
        List<TaskResponse> list = taskService.getTasksByEvent(eventId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }
}
