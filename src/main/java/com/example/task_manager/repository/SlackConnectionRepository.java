package com.example.task_manager.repository;

import com.example.task_manager.entity.SlackConnection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SlackConnectionRepository extends JpaRepository<SlackConnection, Long> {
    Optional<SlackConnection> findByUserId(Long userId);
    Optional<SlackConnection> findByWorkspaceIdAndSlackUserId(String workspaceId, String slackUserId);
    long countBySlackInstallationId(Long slackInstallationId);
}
