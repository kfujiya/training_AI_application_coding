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
@Table(name = "slack_installations")
public class SlackInstallation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "slack_installation_id")
    private Long slackInstallationId;

    @Column(name = "workspace_id", nullable = false, unique = true, length = 32)
    private String workspaceId;

    @Column(name = "enterprise_id", length = 32)
    private String enterpriseId;

    @Column(name = "bot_user_id", nullable = false, length = 32)
    private String botUserId;

    @Column(name = "bot_access_token_encrypted", nullable = false, columnDefinition = "text")
    private String botAccessTokenEncrypted;

    @Column(nullable = false, columnDefinition = "text")
    private String scopes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public SlackInstallation() {}

    @PrePersist
    void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getSlackInstallationId() { return slackInstallationId; }
    public String getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(String workspaceId) { this.workspaceId = workspaceId; }
    public String getEnterpriseId() { return enterpriseId; }
    public void setEnterpriseId(String enterpriseId) { this.enterpriseId = enterpriseId; }
    public String getBotUserId() { return botUserId; }
    public void setBotUserId(String botUserId) { this.botUserId = botUserId; }
    public String getBotAccessTokenEncrypted() { return botAccessTokenEncrypted; }
    public void setBotAccessTokenEncrypted(String botAccessTokenEncrypted) { this.botAccessTokenEncrypted = botAccessTokenEncrypted; }
    public String getScopes() { return scopes; }
    public void setScopes(String scopes) { this.scopes = scopes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
