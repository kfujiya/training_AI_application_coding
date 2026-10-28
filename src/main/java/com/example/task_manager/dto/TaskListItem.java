package com.example.task_manager.dto;

import com.example.task_manager.entity.Task;
import java.time.LocalDate;
import java.util.List;

public class TaskListItem {
    private final Task task;
    private final List<String> tagNames;

    public TaskListItem(Task task, List<String> tagNames) {
        this.task = task;
        this.tagNames = tagNames;
    }

    public Task getTask() { return task; }
    public List<String> getTagNames() { return tagNames; }
    public Long getTaskId() { return task.getTaskId(); }
    public String getTitle() { return task.getTitle(); }
    public String getStatus() { return task.getStatus(); }
    public String getPriority() { return task.getPriority(); }
    public LocalDate getDueDate() { return task.getDueDate(); }

    public boolean isCompleted() { return "DONE".equals(task.getStatus()); }
    public boolean isOverdue() {
        return !isCompleted() && task.getDueDate() != null && task.getDueDate().isBefore(LocalDate.now());
    }
    public boolean isDueToday() {
        return !isCompleted() && task.getDueDate() != null && task.getDueDate().equals(LocalDate.now());
    }
    public String getStatusLabel() { return TaskDisplayLabels.status(task.getStatus()); }
    public String getPriorityLabel() { return TaskDisplayLabels.priority(task.getPriority()); }
    public String getPriorityBadgeClass() {
        return TaskDisplayLabels.priorityBadgeClass(task.getPriority());
    }
    public String getStatusBadgeClass() {
        return TaskDisplayLabels.statusBadgeClass(task.getStatus());
    }
    public String getDueStateLabel() {
        if (isCompleted() || task.getDueDate() == null) return task.getDueDate() == null ? "期限未設定" : "期限";
        if (isOverdue()) return "期限超過";
        if (isDueToday()) return "本日期限";
        return "期限";
    }
    public String getDueStateClass() {
        if (isOverdue()) return "text-danger";
        if (isDueToday()) return "text-warning-emphasis";
        return "text-body-secondary";
    }
}
