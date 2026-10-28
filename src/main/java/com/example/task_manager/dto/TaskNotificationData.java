package com.example.task_manager.dto;

import com.example.task_manager.entity.Task;

import java.time.LocalDate;
import java.util.List;

/** Immutable task data captured for a post-commit notification. */
public record TaskNotificationData(
        Long userId,
        String title,
        String description,
        String status,
        String priority,
        LocalDate dueDate,
        List<String> tagNames) {

    public TaskNotificationData {
        title = normalize(title);
        description = normalize(description);
        tagNames = tagNames == null ? List.of() : tagNames.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public static TaskNotificationData from(Task task, List<String> tagNames) {
        return new TaskNotificationData(task.getUserId(), task.getTitle(), task.getDescription(),
                task.getStatus(), task.getPriority(), task.getDueDate(), tagNames);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
