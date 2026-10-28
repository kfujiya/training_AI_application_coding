package com.example.task_manager.dto;

public final class TaskDisplayLabels {
    private TaskDisplayLabels() { }

    public static String status(String value) {
        if (value == null) return "未設定";
        return switch (value) {
            case "TODO" -> "未着手";
            case "IN_PROGRESS" -> "進行中";
            case "DONE" -> "完了";
            default -> value;
        };
    }

    public static String priority(String value) {
        if (value == null) return "未設定";
        return switch (value) {
            case "LOW" -> "低";
            case "MEDIUM" -> "中";
            case "HIGH" -> "高";
            default -> value;
        };
    }

    public static String statusBadgeClass(String value) {
        if (value == null) return "text-bg-secondary";
        return switch (value) {
            case "DONE" -> "text-bg-success";
            case "IN_PROGRESS" -> "text-bg-primary";
            default -> "text-bg-secondary";
        };
    }

    public static String priorityBadgeClass(String value) {
        if (value == null) return "text-bg-secondary";
        return switch (value) {
            case "HIGH" -> "text-bg-danger";
            case "MEDIUM" -> "text-bg-warning";
            default -> "text-bg-secondary";
        };
    }
}
