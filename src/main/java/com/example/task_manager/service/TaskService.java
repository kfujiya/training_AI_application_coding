package com.example.task_manager.service;

import com.example.task_manager.entity.Task;
import com.example.task_manager.entity.User;
import com.example.task_manager.entity.Tag;
import com.example.task_manager.entity.TaskTag;
import com.example.task_manager.dto.TaskListItem;
import com.example.task_manager.form.TaskFilterForm;
import com.example.task_manager.form.TaskForm;
import com.example.task_manager.repository.TaskRepository;
import com.example.task_manager.repository.TaskTagRepository;
import com.example.task_manager.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Collectors;

@Service
public class TaskService {
    private final TaskRepository taskRepository;
    private final TaskTagRepository taskTagRepository;
    private final UserRepository userRepository;
    private final com.example.task_manager.repository.TagRepository tagRepository;

    public TaskService(TaskRepository taskRepository, TaskTagRepository taskTagRepository,
                       UserRepository userRepository,
                       com.example.task_manager.repository.TagRepository tagRepository) {
        this.taskRepository = taskRepository;
        this.taskTagRepository = taskTagRepository;
        this.userRepository = userRepository;
        this.tagRepository = tagRepository;
    }

    public List<Task> findAllOwnedBy(String email) {
        return taskRepository.findAllByUserId(currentUser(email).getUserId());
    }

    public List<TaskListItem> findAllOwnedBy(String email, TaskFilterForm filter) {
        User user = currentUser(email);
        List<Task> tasks = taskRepository.findAllByUserId(user.getUserId());
        List<Tag> ownedTags = tagRepository.findAllByUserId(user.getUserId());
        Set<Long> ownedTagIds = ownedTags.stream().map(Tag::getTagId).collect(Collectors.toSet());
        if (filter.getTagId() != null && !ownedTagIds.contains(filter.getTagId())) return List.of();
        tasks = tasks.stream().filter(task -> matches(task, filter)).toList();
        Map<Long, String> tagNames = ownedTags.stream().collect(Collectors.toMap(Tag::getTagId, Tag::getName));
        Map<Long, List<Long>> taskTagIds = new HashMap<>();
        if (!tasks.isEmpty()) {
            for (TaskTag taskTag : taskTagRepository.findAllByIdTaskIdIn(tasks.stream().map(Task::getTaskId).toList())) {
                taskTagIds.computeIfAbsent(taskTag.getTaskId(), ignored -> new ArrayList<>()).add(taskTag.getTagId());
            }
        }
        return tasks.stream().filter(task -> filter.getTagId() == null
                        || taskTagIds.getOrDefault(task.getTaskId(), List.of()).contains(filter.getTagId()))
                .map(task -> new TaskListItem(task, taskTagIds.getOrDefault(task.getTaskId(), List.of()).stream()
                        .map(tagNames::get).filter(java.util.Objects::nonNull).toList()))
                .toList();
    }

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
        applyTags(saved, form.getTagIds(), user);
        return saved;
    }

    @Transactional
    public Task update(Long taskId, TaskForm form, String email) {
        Task task = findOwnedById(taskId, email);
        apply(form, task);
        Task saved = taskRepository.save(task);
        applyTags(saved, form.getTagIds(), currentUser(email));
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

    private void applyTags(Task task, List<Long> tagIds, User user) {
        List<Long> selected = tagIds == null ? List.of() : tagIds.stream().distinct().toList();
        List<Tag> tags = selected.isEmpty() ? List.of() : tagRepository.findAllByTagIdInAndUserId(selected, user.getUserId());
        if (tags.size() != selected.size()) throw new InvalidTagSelectionException();
        taskTagRepository.deleteAllByIdTaskId(task.getTaskId());
        for (Tag tag : tags) taskTagRepository.save(new TaskTag(task.getTaskId(), tag.getTagId()));
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

    public static class TaskNotFoundException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    public static class InvalidTagSelectionException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
