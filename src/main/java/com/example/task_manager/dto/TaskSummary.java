package com.example.task_manager.dto;

public class TaskSummary {
    private final int totalCount;
    private final int incompleteCount;
    private final int overdueCount;
    private final int dueTodayCount;
    private final int completedCount;
    private final int completionRate;

    public TaskSummary(int totalCount, int incompleteCount, int overdueCount,
                       int dueTodayCount, int completedCount, int completionRate) {
        this.totalCount = totalCount;
        this.incompleteCount = incompleteCount;
        this.overdueCount = overdueCount;
        this.dueTodayCount = dueTodayCount;
        this.completedCount = completedCount;
        this.completionRate = completionRate;
    }

    public int getTotalCount() { return totalCount; }
    public int getIncompleteCount() { return incompleteCount; }
    public int getOverdueCount() { return overdueCount; }
    public int getDueTodayCount() { return dueTodayCount; }
    public int getCompletedCount() { return completedCount; }
    public int getCompletionRate() { return completionRate; }
}
