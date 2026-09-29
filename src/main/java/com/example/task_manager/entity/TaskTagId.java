package com.example.task_manager.entity;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class TaskTagId implements Serializable {
    private Long taskId;
    private Long tagId;
    protected TaskTagId() {}
    public TaskTagId(Long taskId, Long tagId) { this.taskId = taskId; this.tagId = tagId; }
    public Long getTaskId() { return taskId; }
    public Long getTagId() { return tagId; }
    @Override public boolean equals(Object o) { if (this == o) return true; if (!(o instanceof TaskTagId x)) return false; return Objects.equals(taskId, x.taskId) && Objects.equals(tagId, x.tagId); }
    @Override public int hashCode() { return Objects.hash(taskId, tagId); }
}
