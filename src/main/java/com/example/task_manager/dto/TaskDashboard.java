package com.example.task_manager.dto;

import java.util.List;

public class TaskDashboard {
    private final TaskSummary summary;
    private final List<TaskListItem> priorityTasks;
    private final List<TaskListItem> tasks;

    public TaskDashboard(TaskSummary summary, List<TaskListItem> priorityTasks,
                         List<TaskListItem> tasks) {
        this.summary = summary;
        this.priorityTasks = priorityTasks;
        this.tasks = tasks;
    }

    public TaskSummary getSummary() { return summary; }
    public List<TaskListItem> getPriorityTasks() { return priorityTasks; }
    public List<TaskListItem> getTasks() { return tasks; }
}
