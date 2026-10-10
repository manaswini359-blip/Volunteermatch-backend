package com.volunteermatch.service;

import com.volunteermatch.dto.TaskRequest;
import com.volunteermatch.dto.TaskResponse;
import com.volunteermatch.dto.TaskStatusRequest;
import com.volunteermatch.entity.*;
import com.volunteermatch.exception.BadRequestException;
import com.volunteermatch.exception.ResourceNotFoundException;
import com.volunteermatch.exception.UnauthorizedException;
import com.volunteermatch.repository.ApplicationRepository;
import com.volunteermatch.repository.EventRepository;
import com.volunteermatch.repository.TaskRepository;
import com.volunteermatch.repository.VolunteerProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final EventRepository eventRepository;
    private final VolunteerProfileRepository volunteerProfileRepository;
    private final ApplicationRepository applicationRepository;
    private final NotificationService notificationService;

    @Transactional
    public TaskResponse assignTask(TaskRequest request, Long orgUserId) {
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + request.getEventId()));

        if (!event.getOrganization().getUser().getId().equals(orgUserId)) {
            throw new UnauthorizedException("You are not authorized to assign tasks for this organization");
        }

        VolunteerProfile volunteer = volunteerProfileRepository.findById(request.getVolunteerId())
                .orElseThrow(() -> new ResourceNotFoundException("Volunteer not found with id: " + request.getVolunteerId()));

        // RULE 8: Only selected volunteers can receive task assignments
        Application application = applicationRepository.findByEventIdAndVolunteerId(event.getId(), volunteer.getId())
                .orElseThrow(() -> new BadRequestException("Volunteer has not applied for this event"));

        if (application.getStatus() != Application.ApplicationStatus.SELECTED) {
            throw new BadRequestException("Tasks can only be assigned to volunteers who have been SELECTED for this event (current status: " + application.getStatus() + ")");
        }

        Task task = Task.builder()
                .event(event)
                .volunteer(volunteer)
                .taskName(request.getTaskName())
                .status(Task.TaskStatus.ASSIGNED)
                .build();

        Task saved = taskRepository.save(task);

        // Notify volunteer
        notificationService.sendNotification(
                volunteer.getUser().getId(),
                String.format("You have been assigned to task: '%s' for event '%s'.",
                        task.getTaskName(), event.getTitle()),
                Notification.NotificationType.TASK
        );

        return mapToResponse(saved);
    }

    @Transactional
    public TaskResponse updateTaskStatus(Long taskId, TaskStatusRequest request, Long userId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));

        // Either the assigned volunteer or the organization can update task status
        boolean isVolunteer = task.getVolunteer().getUser().getId().equals(userId);
        boolean isOrg = task.getEvent().getOrganization().getUser().getId().equals(userId);

        if (!isVolunteer && !isOrg) {
            throw new UnauthorizedException("You do not have permission to update this task");
        }

        Task.TaskStatus status;
        try {
            status = Task.TaskStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid task status: " + request.getStatus());
        }

        task.setStatus(status);
        Task updated = taskRepository.save(task);

        if (isVolunteer && status == Task.TaskStatus.COMPLETED) {
            notificationService.sendNotification(
                    task.getEvent().getOrganization().getUser().getId(),
                    String.format("%s marked task '%s' as COMPLETED for '%s'.",
                            task.getVolunteer().getUser().getName(), task.getTaskName(), task.getEvent().getTitle()),
                    Notification.NotificationType.TASK
            );
        }

        return mapToResponse(updated);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByVolunteer(Long volunteerId) {
        return taskRepository.findByVolunteerIdOrderByAssignedAtDesc(volunteerId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByEvent(Long eventId) {
        return taskRepository.findByEventIdOrderByAssignedAtDesc(eventId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private TaskResponse mapToResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .eventId(task.getEvent().getId())
                .eventTitle(task.getEvent().getTitle())
                .volunteerId(task.getVolunteer().getId())
                .volunteerName(task.getVolunteer().getUser() != null ? task.getVolunteer().getUser().getName() : "")
                .taskName(task.getTaskName())
                .status(task.getStatus().name())
                .assignedAt(task.getAssignedAt())
                .build();
    }
}
