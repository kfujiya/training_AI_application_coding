package com.example.task_manager.service;

import com.example.task_manager.entity.Task;
import com.example.task_manager.entity.User;
import com.example.task_manager.entity.Tag;
import com.example.task_manager.entity.TaskTag;
import com.example.task_manager.dto.TaskListItem;
import com.example.task_manager.dto.TaskDashboard;
import com.example.task_manager.dto.TaskSummary;
import com.example.task_manager.dto.TaskNotificationData;
import com.example.task_manager.form.TaskFilterForm;
import com.example.task_manager.form.TaskForm;
import com.example.task_manager.repository.TaskRepository;
import com.example.task_manager.repository.TaskTagRepository;
import com.example.task_manager.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Comparator;
import java.time.LocalDate;
import java.util.stream.Collectors;

@Service
public class TaskService {
    private final TaskRepository taskRepository;
    private final TaskTagRepository taskTagRepository;
    private final UserRepository userRepository;
    private final com.example.task_manager.repository.TagRepository tagRepository;
    private final SlackNotificationService slackNotificationService;

    public TaskService(TaskRepository taskRepository, TaskTagRepository taskTagRepository,
                       UserRepository userRepository,
                       com.example.task_manager.repository.TagRepository tagRepository,
                       SlackNotificationService slackNotificationService) {
        this.taskRepository = taskRepository;
        this.taskTagRepository = taskTagRepository;
        this.userRepository = userRepository;
        this.tagRepository = tagRepository;
        this.slackNotificationService = slackNotificationService;
    }

    public List<Task> findAllOwnedBy(String email) {
        return taskRepository.findAllByUserId(currentUser(email).getUserId());
    }

    public List<TaskListItem> findAllOwnedBy(String email, TaskFilterForm filter) {
        return findDashboard(email, filter).getTasks();
    }

    public TaskDashboard findDashboard(String email, TaskFilterForm filter) {
        User user = currentUser(email);
        List<Task> tasks = taskRepository.findAllByUserId(user.getUserId());
        List<Tag> ownedTags = tagRepository.findAllByUserId(user.getUserId());
        Set<Long> ownedTagIds = ownedTags.stream().map(Tag::getTagId).collect(Collectors.toSet());
        Map<Long, String> tagNames = ownedTags.stream().collect(Collectors.toMap(Tag::getTagId, Tag::getName));
        Map<Long, List<Long>> taskTagIds = new HashMap<>();
        if (!tasks.isEmpty()) {
            for (TaskTag taskTag : taskTagRepository.findAllByIdTaskIdIn(tasks.stream().map(Task::getTaskId).toList())) {
                taskTagIds.computeIfAbsent(taskTag.getTaskId(), ignored -> new ArrayList<>()).add(taskTag.getTagId());
            }
        }
        List<TaskListItem> allItems = tasks.stream()
                .map(task -> new TaskListItem(task, taskTagIds.getOrDefault(task.getTaskId(), List.of()).stream()
                        .map(tagNames::get).filter(java.util.Objects::nonNull).toList()))
                .toList();
        List<TaskListItem> filteredItems = filter.getTagId() != null && !ownedTagIds.contains(filter.getTagId())
                ? List.of()
                : allItems.stream()
                .filter(item -> matches(item.getTask(), filter))
                .filter(item -> filter.getTagId() == null
                        || taskTagIds.getOrDefault(item.getTaskId(), List.of()).contains(filter.getTagId()))
                .toList();

        LocalDate today = LocalDate.now();
        int completedCount = (int) tasks.stream().filter(task -> "DONE".equals(task.getStatus())).count();
        int incompleteCount = tasks.size() - completedCount;
        int overdueCount = (int) tasks.stream().filter(task -> !"DONE".equals(task.getStatus())
                && task.getDueDate() != null && task.getDueDate().isBefore(today)).count();
        int dueTodayCount = (int) tasks.stream().filter(task -> !"DONE".equals(task.getStatus())
                && today.equals(task.getDueDate())).count();
        int completionRate = tasks.isEmpty() ? 0 : (int) Math.round(completedCount * 100.0 / tasks.size());
        TaskSummary summary = new TaskSummary(tasks.size(), incompleteCount, overdueCount,
                dueTodayCount, completedCount, completionRate);

        List<TaskListItem> priorityItems = allItems.stream()
                .filter(item -> !item.isCompleted())
                .map(item -> new PriorityItem(item, priorityRank(item.getTask(), today)))
                .filter(item -> item.rank() < 99)
                .sorted(Comparator.comparingInt(PriorityItem::rank)
                        .thenComparing(item -> item.item().getDueDate(), Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(item -> item.item().getTitle(), String.CASE_INSENSITIVE_ORDER))
                .limit(5)
                .map(PriorityItem::item)
                .toList();
        return new TaskDashboard(summary, priorityItems, filteredItems);
    }

    private int priorityRank(Task task, LocalDate today) {
        if (task.getDueDate() != null && task.getDueDate().isBefore(today)) return 0;
        boolean high = "HIGH".equals(task.getPriority());
        boolean dueSoon = task.getDueDate() != null && !task.getDueDate().isAfter(today.plusDays(7));
        if (high && dueSoon) return 1;
        if (high) return 2;
        if (dueSoon) return 3;
        return 99;
    }

    private record PriorityItem(TaskListItem item, int rank) { }

    public Task findOwnedById(Long taskId, String email) {
        return taskRepository.findByTaskIdAndUserId(taskId, currentUser(email).getUserId())
                .orElseThrow(TaskNotFoundException::new);
    }

    @Transactional
    public Task create(TaskForm form, String email) {
        User user = currentUser(email);
        Task task = new Task();
        task.setUserId(user.getUserId());
        apply(form, task);
        Task saved = taskRepository.save(task);
        List<String> tagNames = applyTags(saved, form.getTagIds(), user);
        TaskNotificationData data = TaskNotificationData.from(saved, tagNames);
        notifyAfterCommit(() -> slackNotificationService.notifyTaskCreated(data));
        return saved;
    }

    @Transactional
    public Task update(Long taskId, TaskForm form, String email) {
        Task task = findOwnedById(taskId, email);
        User user = currentUser(email);
        TaskNotificationData before = TaskNotificationData.from(task, findOwnedTagNames(taskId, email));
        boolean completed = "DONE".equals(form.getStatus()) && !"DONE".equals(task.getStatus());
        apply(form, task);
        Task saved = taskRepository.save(task);
        List<String> tagNames = applyTags(saved, form.getTagIds(), user);
        TaskNotificationData after = TaskNotificationData.from(saved, tagNames);
        notifyAfterCommit(() -> {
            if (completed) slackNotificationService.notifyTaskCompleted(before, after);
            else slackNotificationService.notifyTaskUpdated(before, after);
        });
        return saved;
    }

    @Transactional
    public void delete(Long taskId, String email) {
        Task task = findOwnedById(taskId, email);
        taskTagRepository.deleteAllByIdTaskId(task.getTaskId());
        taskRepository.delete(task);
    }

    public List<Long> findOwnedTagIds(Long taskId, String email) {
        findOwnedById(taskId, email);
        return taskTagRepository.findAllByIdTaskId(taskId).stream().map(TaskTag::getTagId).toList();
    }

    public List<String> findOwnedTagNames(Long taskId, String email) {
        User user = currentUser(email);
        findOwnedById(taskId, email);
        Map<Long, String> names = tagRepository.findAllByUserId(user.getUserId()).stream()
                .collect(Collectors.toMap(Tag::getTagId, Tag::getName));
        return taskTagRepository.findAllByIdTaskId(taskId).stream().map(TaskTag::getTagId)
                .map(names::get).filter(java.util.Objects::nonNull).toList();
    }

    private List<String> applyTags(Task task, List<Long> tagIds, User user) {
        List<Long> selected = tagIds == null ? List.of() : tagIds.stream().distinct().toList();
        List<Tag> tags = selected.isEmpty() ? List.of() : tagRepository.findAllByTagIdInAndUserId(selected, user.getUserId());
        if (tags.size() != selected.size()) throw new InvalidTagSelectionException();
        taskTagRepository.deleteAllByIdTaskId(task.getTaskId());
        for (Tag tag : tags) taskTagRepository.save(new TaskTag(task.getTaskId(), tag.getTagId()));
        return tags.stream().map(Tag::getName).toList();
    }

    private boolean matches(Task task, TaskFilterForm filter) {
        return (filter.getTitle() == null || filter.getTitle().isBlank()
                || task.getTitle().toLowerCase().contains(filter.getTitle().trim().toLowerCase()))
                && (filter.getStatus() == null || filter.getStatus().isBlank() || filter.getStatus().equals(task.getStatus()))
                && (filter.getPriority() == null || filter.getPriority().isBlank() || filter.getPriority().equals(task.getPriority()))
                && (filter.getDueDate() == null || filter.getDueDate().equals(task.getDueDate()));
    }

    private void apply(TaskForm form, Task task) {
        task.setTitle(form.getTitle());
        task.setDescription(form.getDescription());
        task.setStatus(form.getStatus());
        task.setPriority(form.getPriority());
        task.setDueDate(form.getDueDate());
    }

    private User currentUser(String email) {
        return userRepository.findByEmail(email).orElseThrow(TaskNotFoundException::new);
    }

    private void notifyAfterCommit(Runnable notification) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            notification.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notification.run();
            }
        });
    }

    public static class TaskNotFoundException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    public static class InvalidTagSelectionException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
