package com.example.task_manager.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TaskForm {
    @NotBlank(message = "タイトルは必須です")
    @Size(max = 200, message = "タイトルは200文字以内で入力してください")
    private String title;

    @Size(max = 2000, message = "詳細は2000文字以内で入力してください")
    private String description;

    @Pattern(regexp = "TODO|IN_PROGRESS|DONE", message = "ステータスの値が不正です")
    private String status = "TODO";

    @Pattern(regexp = "LOW|MEDIUM|HIGH", message = "優先度の値が不正です")
    private String priority = "MEDIUM";

    private LocalDate dueDate;

    private List<Long> tagIds = new ArrayList<>();

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public List<Long> getTagIds() { return tagIds; }
    public void setTagIds(List<Long> tagIds) { this.tagIds = tagIds == null ? new ArrayList<>() : tagIds; }
}
