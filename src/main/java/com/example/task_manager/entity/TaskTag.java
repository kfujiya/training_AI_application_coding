package com.example.task_manager.entity;

import jakarta.persistence.*;

@Entity @Table(name = "task_tags")
public class TaskTag {
    @EmbeddedId private TaskTagId id;
    protected TaskTag() {}
    public TaskTag(Long taskId, Long tagId) { this.id = new TaskTagId(taskId, tagId); }
    public TaskTagId getId() { return id; }
    public Long getTaskId() { return id.getTaskId(); }
    public Long getTagId() { return id.getTagId(); }
}
