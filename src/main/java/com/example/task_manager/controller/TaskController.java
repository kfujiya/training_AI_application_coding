package com.example.task_manager.controller;

import com.example.task_manager.entity.Task;
import com.example.task_manager.form.TaskForm;
import com.example.task_manager.service.TaskService;
import com.example.task_manager.service.TaskService.TaskNotFoundException;
import com.example.task_manager.service.TaskService.InvalidTagSelectionException;
import com.example.task_manager.service.TagService;
import com.example.task_manager.dto.TaskDisplayLabels;
import com.example.task_manager.form.TaskFilterForm;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/tasks")
public class TaskController {
    private final TaskService taskService;
    private final TagService tagService;

    public TaskController(TaskService taskService, TagService tagService) {
        this.taskService = taskService;
        this.tagService = tagService;
    }

    @GetMapping
    public String list(@ModelAttribute("filter") TaskFilterForm filter, Authentication authentication, Model model) {
        model.addAttribute("dashboard", taskService.findDashboard(authentication.getName(), filter));
        model.addAttribute("tags", tagService.findAllOwnedBy(authentication.getName()));
        return "tasks/list";
    }

    @GetMapping("/new")
    public String newForm(Authentication authentication, Model model) {
        model.addAttribute("taskForm", new TaskForm());
        model.addAttribute("formAction", "/tasks");
        model.addAttribute("tags", tagService.findAllOwnedBy(authentication.getName()));
        return "tasks/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("taskForm") TaskForm form, BindingResult result,
                         Authentication authentication, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("formAction", "/tasks");
            model.addAttribute("tags", tagService.findAllOwnedBy(authentication.getName()));
            return "tasks/form";
        }
        try {
            Task task = taskService.create(form, authentication.getName());
            return "redirect:/tasks/" + task.getTaskId();
        } catch (InvalidTagSelectionException e) {
            model.addAttribute("tagError", "選択したタグを使用できません。");
            model.addAttribute("formAction", "/tasks");
            model.addAttribute("tags", tagService.findAllOwnedBy(authentication.getName()));
            return "tasks/form";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Authentication authentication, Model model) {
        try {
            Task task = taskService.findOwnedById(id, authentication.getName());
            model.addAttribute("task", task);
            model.addAttribute("statusLabel", TaskDisplayLabels.status(task.getStatus()));
            model.addAttribute("priorityLabel", TaskDisplayLabels.priority(task.getPriority()));
            model.addAttribute("statusBadgeClass", TaskDisplayLabels.statusBadgeClass(task.getStatus()));
            model.addAttribute("priorityBadgeClass", TaskDisplayLabels.priorityBadgeClass(task.getPriority()));
            model.addAttribute("tagNames", taskService.findOwnedTagNames(id, authentication.getName()));
            return "tasks/detail";
        } catch (TaskNotFoundException e) {
            return "redirect:/tasks?notFound";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Authentication authentication, Model model) {
        try {
            Task task = taskService.findOwnedById(id, authentication.getName());
            model.addAttribute("taskForm", toForm(task, taskService.findOwnedTagIds(id, authentication.getName())));
            model.addAttribute("formAction", "/tasks/" + id);
            model.addAttribute("editing", true);
            model.addAttribute("tags", tagService.findAllOwnedBy(authentication.getName()));
            return "tasks/form";
        } catch (TaskNotFoundException e) {
            return "redirect:/tasks?notFound";
        }
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("taskForm") TaskForm form,
                         BindingResult result, Authentication authentication, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("formAction", "/tasks/" + id);
            model.addAttribute("editing", true);
            model.addAttribute("tags", tagService.findAllOwnedBy(authentication.getName()));
            return "tasks/form";
        }
        try {
            taskService.update(id, form, authentication.getName());
            return "redirect:/tasks/" + id;
        } catch (TaskNotFoundException e) {
            return "redirect:/tasks?notFound";
        } catch (InvalidTagSelectionException e) {
            model.addAttribute("tagError", "選択したタグを使用できません。");
            model.addAttribute("formAction", "/tasks/" + id);
            model.addAttribute("editing", true);
            model.addAttribute("tags", tagService.findAllOwnedBy(authentication.getName()));
            return "tasks/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Authentication authentication) {
        try {
            taskService.delete(id, authentication.getName());
        } catch (TaskNotFoundException ignored) {
            // Do not reveal whether another user's task exists.
        }
        return "redirect:/tasks";
    }

    private TaskForm toForm(Task task, java.util.List<Long> tagIds) {
        TaskForm form = new TaskForm();
        form.setTitle(task.getTitle());
        form.setDescription(task.getDescription());
        form.setStatus(task.getStatus());
        form.setPriority(task.getPriority());
        form.setDueDate(task.getDueDate());
        form.setTagIds(tagIds);
        return form;
    }
}
