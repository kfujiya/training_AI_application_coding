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
}
