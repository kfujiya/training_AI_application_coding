package com.example.task_manager.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegistrationForm {
    @NotBlank(message = "ユーザー名は必須です") @Size(max = 50, message = "ユーザー名は50文字以内で入力してください")
    private String username;
    @NotBlank(message = "メールアドレスは必須です") @Email(message = "メールアドレスの形式が正しくありません") @Size(max = 255, message = "メールアドレスは255文字以内で入力してください")
    private String email;
    @NotBlank(message = "パスワードは必須です") @Size(min = 8, max = 255, message = "パスワードは8文字以上255文字以内で入力してください")
    private String password;
    @NotBlank(message = "表示名は必須です") @Size(max = 100, message = "表示名は100文字以内で入力してください")
    private String displayName;
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
}
