package com.example.task_manager.repository;

import com.example.task_manager.entity.SlackInstallation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SlackInstallationRepository extends JpaRepository<SlackInstallation, Long> {
    Optional<SlackInstallation> findByWorkspaceId(String workspaceId);
}
