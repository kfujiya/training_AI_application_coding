package com.example.task_manager.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "slack_connections")
public class SlackConnection {
    public static final String ACTIVE = "ACTIVE";
    public static final String REVOKED = "REVOKED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "slack_connection_id")
    private Long slackConnectionId;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "slack_installation_id", nullable = false)
    private Long slackInstallationId;

    @Column(name = "workspace_id", nullable = false, length = 32)
    private String workspaceId;

    @Column(name = "slack_user_id", nullable = false, length = 32)
    private String slackUserId;

    @Column(name = "dm_channel_id", length = 32)
    private String dmChannelId;

    @Column(nullable = false, length = 20)
    private String status = ACTIVE;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public SlackConnection() {}

    @PrePersist
    void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getSlackConnectionId() { return slackConnectionId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getSlackInstallationId() { return slackInstallationId; }
    public void setSlackInstallationId(Long slackInstallationId) { this.slackInstallationId = slackInstallationId; }
    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }
    public String getSlackUserId() { return slackUserId; }
    public void setSlackUserId(String slackUserId) { this.slackUserId = slackUserId; }
    public String getDmChannelId() { return dmChannelId; }
    public void setDmChannelId(String dmChannelId) { this.dmChannelId = dmChannelId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
