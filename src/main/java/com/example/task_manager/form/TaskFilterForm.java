package com.example.task_manager.form;

import java.time.LocalDate;

public class TaskFilterForm {
    private String title;
    private String status;
    private String priority;
    private Long tagId;
    private LocalDate dueDate;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public Long getTagId() { return tagId; }
    public void setTagId(Long tagId) { this.tagId = tagId; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public boolean hasConditions() {
        return (title != null && !title.isBlank())
                || (status != null && !status.isBlank())
                || (priority != null && !priority.isBlank())
                || tagId != null
                || dueDate != null;
    }
}
