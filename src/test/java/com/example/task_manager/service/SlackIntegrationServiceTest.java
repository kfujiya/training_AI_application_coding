package com.example.task_manager.service;

import com.example.task_manager.dto.SlackOAuthAccessResponse;
import com.example.task_manager.entity.SlackConnection;
import com.example.task_manager.entity.SlackInstallation;
import com.example.task_manager.entity.User;
import com.example.task_manager.repository.SlackConnectionRepository;
import com.example.task_manager.repository.SlackInstallationRepository;
import com.example.task_manager.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SlackIntegrationServiceTest {
    private final SlackInstallationRepository installations = mock(SlackInstallationRepository.class);
    private final SlackConnectionRepository connections = mock(SlackConnectionRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final SlackTokenEncryptionService encryption = mock(SlackTokenEncryptionService.class);
    private final SlackIntegrationService service = new SlackIntegrationService(installations, connections, users, encryption);

    @Test
    void validOAuthResponseIsStoredForTheAuthenticatedUser() {
        User user = mock(User.class);
        when(user.getUserId()).thenReturn(1L);
        when(users.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(connections.findByWorkspaceIdAndSlackUserId("T1", "U1")).thenReturn(Optional.empty());
        when(connections.findByUserId(1L)).thenReturn(Optional.empty());
        when(encryption.encrypt("xoxb-secret")).thenReturn("encrypted");
        SlackInstallation installation = new SlackInstallation();
        when(installations.findByWorkspaceId("T1")).thenReturn(Optional.empty());
        when(installations.save(any())).thenReturn(installation);

        service.connect("user@example.com", response("xoxb-secret", "T1", "U1"));

        verify(installations).save(any(SlackInstallation.class));
        verify(connections).save(any(SlackConnection.class));
        verify(encryption).encrypt("xoxb-secret");
    }

    @Test
    void duplicateSlackAccountIsRejectedBeforeTokenStorage() {
        User user = mock(User.class);
        when(user.getUserId()).thenReturn(2L);
        when(users.findByEmail("other@example.com")).thenReturn(Optional.of(user));
        SlackConnection existing = mock(SlackConnection.class);
        when(existing.getUserId()).thenReturn(1L);
        when(connections.findByWorkspaceIdAndSlackUserId("T1", "U1")).thenReturn(Optional.of(existing));

        assertThrows(SlackIntegrationService.SlackAccountAlreadyConnectedException.class,
                () -> service.connect("other@example.com", response("xoxb-secret", "T1", "U1")));
        verifyNoInteractions(encryption, installations);
    }

    private SlackOAuthAccessResponse response(String token, String workspace, String slackUser) {
        SlackOAuthAccessResponse response = new SlackOAuthAccessResponse();
        response.setOk(true);
        response.setAccessToken(token);
        response.setBotUserId("B1");
        SlackOAuthAccessResponse.Team team = new SlackOAuthAccessResponse.Team();
        team.setId(workspace);
        response.setTeam(team);
        SlackOAuthAccessResponse.AuthedUser authedUser = new SlackOAuthAccessResponse.AuthedUser();
        authedUser.setId(slackUser);
        response.setAuthedUser(authedUser);
        return response;
    }
}
