package com.example.task_manager.service;

import com.example.task_manager.dto.TaskDashboard;
import com.example.task_manager.entity.Task;
import com.example.task_manager.entity.User;
import com.example.task_manager.form.TaskFilterForm;
import com.example.task_manager.repository.TagRepository;
import com.example.task_manager.repository.TaskRepository;
import com.example.task_manager.repository.TaskTagRepository;
import com.example.task_manager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskDashboardTest {
    @Mock private TaskRepository taskRepository;
    @Mock private TaskTagRepository taskTagRepository;
    @Mock private UserRepository userRepository;
    @Mock private TagRepository tagRepository;
    @Mock private SlackNotificationService slackNotificationService;

    private TaskService taskService;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository, taskTagRepository, userRepository,
                tagRepository, slackNotificationService);
        User user = mock(User.class);
        when(user.getUserId()).thenReturn(1L);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(tagRepository.findAllByUserId(1L)).thenReturn(List.of());
        when(taskTagRepository.findAllByIdTaskIdIn(anyCollection())).thenReturn(List.of());
    }

    @Test
    void calculatesSummaryAndPriorityOrderWithoutPersistedChanges() {
        LocalDate today = LocalDate.now();
        Task overdue = task(1L, "期限超過", "TODO", "LOW", today.minusDays(1));
        Task highSoon = task(2L, "高優先度", "IN_PROGRESS", "HIGH", today.plusDays(2));
        Task completed = task(3L, "完了", "DONE", "HIGH", today.minusDays(3));
        Task noDueDate = task(4L, "期限なし", "TODO", "MEDIUM", null);
        when(taskRepository.findAllByUserId(1L)).thenReturn(List.of(overdue, highSoon, completed, noDueDate));

        TaskDashboard dashboard = taskService.findDashboard("user@example.com", new TaskFilterForm());

        assertEquals(4, dashboard.getSummary().getTotalCount());
        assertEquals(3, dashboard.getSummary().getIncompleteCount());
        assertEquals(1, dashboard.getSummary().getOverdueCount());
        assertEquals(25, dashboard.getSummary().getCompletionRate());
        assertEquals(List.of(1L, 2L), dashboard.getPriorityTasks().stream()
                .map(item -> item.getTaskId()).toList());
    }

    private Task task(Long id, String title, String status, String priority, LocalDate dueDate) {
        Task task = mock(Task.class);
        lenient().when(task.getTaskId()).thenReturn(id);
        lenient().when(task.getTitle()).thenReturn(title);
        lenient().when(task.getStatus()).thenReturn(status);
        lenient().when(task.getPriority()).thenReturn(priority);
        lenient().when(task.getDueDate()).thenReturn(dueDate);
        return task;
    }
}
