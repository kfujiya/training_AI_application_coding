package com.example.task_manager.service;

import com.example.task_manager.dto.TaskNotificationData;
import com.example.task_manager.entity.SlackConnection;
import com.example.task_manager.entity.SlackInstallation;
import com.example.task_manager.repository.SlackConnectionRepository;
import com.example.task_manager.repository.SlackInstallationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SlackNotificationServiceTest {
    private SlackConnectionRepository connections;
    private SlackInstallationRepository installations;
    private SlackTokenEncryptionService encryption;
    private SlackWebApiClient api;
    private SlackNotificationService service;
    private SlackConnection connection;

    @BeforeEach
    void setUp() {
        connections = mock(SlackConnectionRepository.class);
        installations = mock(SlackInstallationRepository.class);
        encryption = mock(SlackTokenEncryptionService.class);
        api = mock(SlackWebApiClient.class);
        service = new SlackNotificationService(connections, installations, encryption, api);
        connection = new SlackConnection();
        connection.setUserId(10L);
        connection.setSlackInstallationId(20L);
        connection.setSlackUserId("U1");
        connection.setStatus(SlackConnection.ACTIVE);
        SlackInstallation installation = mock(SlackInstallation.class);
        when(connections.findByUserId(10L)).thenReturn(Optional.of(connection));
        when(installations.findById(20L)).thenReturn(Optional.of(installation));
        when(installation.getBotAccessTokenEncrypted()).thenReturn("encrypted");
        when(encryption.decrypt("encrypted")).thenReturn("xoxb-token");
    }

    @Test
    void createUsesDisplayLabelsAndShowsUnsetValues() throws Exception {
        connection.setDmChannelId("D1");
        service.notifyTaskCreated(data("task", "TODO", "HIGH", LocalDate.of(2026, 11, 5), List.of("PC")));

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(api).postMessage(eq("xoxb-token"), eq("D1"), message.capture());
        String text = message.getValue();
        assertTrue(text.contains("【タスク登録】task"));
        assertTrue(text.contains("ステータス: 未着手"));
        assertTrue(text.contains("優先度: 高"));
        assertTrue(text.contains("期限: 2026/11/05"));
        assertTrue(text.contains("タグ: PC"));
    }

    @Test
    void updateNotifiesOnlyChangedFieldsAndCompletionIsDistinct() throws Exception {
        connection.setDmChannelId("D1");
        TaskNotificationData before = data("task", "TODO", "MEDIUM", null, List.of("旧タグ"));
        TaskNotificationData after = data("new task", "IN_PROGRESS", "HIGH", LocalDate.of(2026, 11, 5), List.of("新タグ"));
        service.notifyTaskUpdated(before, after);

        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(api).postMessage(eq("xoxb-token"), eq("D1"), message.capture());
        String text = message.getValue();
        assertTrue(text.contains("タスク名: task → new task"));
        assertTrue(text.contains("ステータス: 未着手 → 進行中"));
        assertTrue(text.contains("優先度: 中 → 高"));
        assertTrue(text.contains("期限: 未設定 → 2026/11/05"));
        assertTrue(text.contains("タグ: 旧タグ → 新タグ"));

        reset(api);
        service.notifyTaskCompleted(before, data("task", "DONE", "MEDIUM", null, List.of("旧タグ")));
        verify(api).postMessage(eq("xoxb-token"), eq("D1"), contains("【タスク完了！】task"));
        verify(api, never()).postMessage(anyString(), anyString(), contains("変更内容"));
    }

    @Test
    void unchangedUpdateDoesNotSend() {
        connection.setDmChannelId("D1");
        TaskNotificationData data = data("task", "TODO", "MEDIUM", null, List.of());
        service.notifyTaskUpdated(data, data);
        verifyNoInteractions(api);
    }

    @Test
    void failureDoesNotEscapeAndUnlinkedOwnerIsSkipped() throws Exception {
        connection.setDmChannelId("D1");
        doThrow(new IOException("network failure")).when(api).postMessage(anyString(), anyString(), anyString());
        assertDoesNotThrow(() -> service.notifyTaskCreated(data("task", "TODO", "LOW", null, List.of())));

        reset(api);
        when(connections.findByUserId(10L)).thenReturn(Optional.empty());
        service.notifyTaskCreated(data("task", "TODO", "LOW", null, List.of()));
        verifyNoInteractions(api);
    }

    private TaskNotificationData data(String title, String status, String priority, LocalDate dueDate, List<String> tags) {
        return new TaskNotificationData(10L, title, null, status, priority, dueDate, tags);
    }
}
