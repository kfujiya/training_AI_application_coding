package com.example.task_manager.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TagForm {
    @NotBlank(message = "タグ名は必須です")
    @Size(max = 50, message = "タグ名は50文字以内で入力してください")
    private String name;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
