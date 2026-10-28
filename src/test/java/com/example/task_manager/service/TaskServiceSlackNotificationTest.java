package com.example.task_manager.service;

import com.example.task_manager.entity.Task;
import com.example.task_manager.entity.User;
import com.example.task_manager.dto.TaskNotificationData;
import com.example.task_manager.form.TaskForm;
import com.example.task_manager.repository.TagRepository;
import com.example.task_manager.repository.TaskRepository;
import com.example.task_manager.repository.TaskTagRepository;
import com.example.task_manager.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceSlackNotificationTest {
    @Mock private TaskRepository taskRepository;
    @Mock private TaskTagRepository taskTagRepository;
    @Mock private UserRepository userRepository;
    @Mock private TagRepository tagRepository;
    @Mock private SlackNotificationService slackNotificationService;

    private TaskService taskService;
    private User user;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository, taskTagRepository, userRepository, tagRepository, slackNotificationService);
        user = mock(User.class);
        when(user.getUserId()).thenReturn(10L);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void createNotifiesAfterTransactionCommit() {
        TaskForm form = form("登録タスク", "TODO");
        TransactionSynchronizationManager.initSynchronization();

        taskService.create(form, "user@example.com");

        verifyNoInteractions(slackNotificationService);
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }
        verify(slackNotificationService).notifyTaskCreated(any(TaskNotificationData.class));
    }

    @Test
    void updateNotifiesCompletedWhenStatusChangesToDone() {
        Task task = new Task();
        task.setStatus("IN_PROGRESS");
        when(taskRepository.findByTaskIdAndUserId(1L, 10L)).thenReturn(Optional.of(task));

        taskService.update(1L, form("完了タスク", "DONE"), "user@example.com");

        verify(slackNotificationService).notifyTaskCompleted(any(TaskNotificationData.class), any(TaskNotificationData.class));
        verify(slackNotificationService, never()).notifyTaskUpdated(any(TaskNotificationData.class), any(TaskNotificationData.class));
    }

    private TaskForm form(String title, String status) {
        TaskForm form = new TaskForm();
        form.setTitle(title);
        form.setStatus(status);
        form.setPriority("MEDIUM");
        return form;
    }
}
