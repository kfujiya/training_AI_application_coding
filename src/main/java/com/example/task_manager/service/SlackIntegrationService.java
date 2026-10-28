package com.example.task_manager.service;

import com.example.task_manager.dto.SlackOAuthAccessResponse;
import com.example.task_manager.entity.SlackConnection;
import com.example.task_manager.entity.SlackInstallation;
import com.example.task_manager.entity.User;
import com.example.task_manager.repository.SlackConnectionRepository;
import com.example.task_manager.repository.SlackInstallationRepository;
import com.example.task_manager.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class SlackIntegrationService {
    private final SlackInstallationRepository installationRepository;
    private final SlackConnectionRepository connectionRepository;
    private final UserRepository userRepository;
    private final SlackTokenEncryptionService tokenEncryptionService;

    public SlackIntegrationService(SlackInstallationRepository installationRepository,
                                   SlackConnectionRepository connectionRepository,
                                   UserRepository userRepository,
                                   SlackTokenEncryptionService tokenEncryptionService) {
        this.installationRepository = installationRepository;
        this.connectionRepository = connectionRepository;
        this.userRepository = userRepository;
        this.tokenEncryptionService = tokenEncryptionService;
    }

    @Transactional
    public void connect(String userEmail, SlackOAuthAccessResponse response) {
        validate(response);
        User user = userRepository.findByEmail(userEmail).orElseThrow(SlackUserNotFoundException::new);
        String workspaceId = response.getTeam().getId();
        String slackUserId = response.getAuthedUser().getId();
        Optional<SlackConnection> duplicate = connectionRepository
                .findByWorkspaceIdAndSlackUserId(workspaceId, slackUserId);
        if (duplicate.isPresent() && !duplicate.get().getUserId().equals(user.getUserId())) {
            throw new SlackAccountAlreadyConnectedException();
        }

        SlackInstallation installation = installationRepository.findByWorkspaceId(workspaceId)
                .orElseGet(SlackInstallation::new);
        installation.setWorkspaceId(workspaceId);
        installation.setBotUserId(response.getBotUserId());
        installation.setBotAccessTokenEncrypted(tokenEncryptionService.encrypt(response.getAccessToken()));
        installation.setScopes(response.getScope() == null ? "" : response.getScope());
        SlackInstallation savedInstallation = installationRepository.save(installation);

        SlackConnection connection = connectionRepository.findByUserId(user.getUserId())
                .orElseGet(SlackConnection::new);
        connection.setUserId(user.getUserId());
        connection.setSlackInstallationId(savedInstallation.getSlackInstallationId());
        connection.setWorkspaceId(workspaceId);
        connection.setSlackUserId(slackUserId);
        connection.setDmChannelId(null);
        connection.setStatus(SlackConnection.ACTIVE);
        connectionRepository.save(connection);
    }

    @Transactional(readOnly = true)
    public Optional<SlackConnection> findConnection(String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElseThrow(SlackUserNotFoundException::new);
        return connectionRepository.findByUserId(user.getUserId());
    }

    @Transactional
    public void disconnect(String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElseThrow(SlackUserNotFoundException::new);
        Optional<SlackConnection> connection = connectionRepository.findByUserId(user.getUserId());
        if (connection.isEmpty()) return;
        Long installationId = connection.get().getSlackInstallationId();
        connectionRepository.delete(connection.get());
        if (connectionRepository.countBySlackInstallationId(installationId) == 0) {
            installationRepository.deleteById(installationId);
        }
    }

    private void validate(SlackOAuthAccessResponse response) {
        if (response == null || !response.isOk() || isBlank(response.getAccessToken())
                || isBlank(response.getBotUserId()) || response.getTeam() == null
                || isBlank(response.getTeam().getId()) || response.getAuthedUser() == null
                || isBlank(response.getAuthedUser().getId())) {
            throw new SlackOAuthResponseException();
        }
    }

    private boolean isBlank(String value) { return value == null || value.isBlank(); }

    public static class SlackUserNotFoundException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
    public static class SlackAccountAlreadyConnectedException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
    public static class SlackOAuthResponseException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
