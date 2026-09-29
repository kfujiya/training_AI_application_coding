package com.example.task_manager.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity @Table(name = "tags", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "name"}))
public class Tag {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "tag_id") private Long tagId;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(nullable = false, length = 50) private String name;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    public Tag() {}
    @PrePersist void onCreate() { createdAt = LocalDateTime.now(); }
    public Long getTagId() { return tagId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
