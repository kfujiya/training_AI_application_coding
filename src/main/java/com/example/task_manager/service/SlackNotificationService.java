package com.example.task_manager.service;

import com.example.task_manager.dto.TaskDisplayLabels;
import com.example.task_manager.dto.TaskNotificationData;
import com.example.task_manager.entity.SlackConnection;
import com.example.task_manager.entity.SlackInstallation;
import com.example.task_manager.entity.Task;
import com.example.task_manager.repository.SlackConnectionRepository;
import com.example.task_manager.repository.SlackInstallationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class SlackNotificationService {
    private static final Logger logger = LoggerFactory.getLogger(SlackNotificationService.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final SlackConnectionRepository connectionRepository;
    private final SlackInstallationRepository installationRepository;
    private final SlackTokenEncryptionService tokenEncryptionService;
    private final SlackWebApiClient webApiClient;

    public SlackNotificationService(SlackConnectionRepository connectionRepository,
                                    SlackInstallationRepository installationRepository,
                                    SlackTokenEncryptionService tokenEncryptionService,
                                    SlackWebApiClient webApiClient) {
        this.connectionRepository = connectionRepository;
        this.installationRepository = installationRepository;
        this.tokenEncryptionService = tokenEncryptionService;
        this.webApiClient = webApiClient;
    }

    public void notifyTaskCreated(TaskNotificationData data) {
        send(data == null ? null : data.userId(), formatCreated(data));
    }

    public void notifyTaskUpdated(TaskNotificationData before, TaskNotificationData after) {
        if (before == null || after == null) return;
        List<String> changes = changes(before, after);
        if (changes.isEmpty()) return;
        send(after.userId(), "【タスク更新】" + display(after.title()) + "\n\n変更内容\n" + String.join("\n", changes));
    }

    public void notifyTaskCompleted(TaskNotificationData before, TaskNotificationData after) {
        if (before == null || after == null || "DONE".equals(before.status()) || !"DONE".equals(after.status())) return;
        send(after.userId(), "【タスク完了！】" + display(after.title())
                + "\nステータス: 完了"
                + "\n優先度: " + TaskDisplayLabels.priority(after.priority())
                + "\n期限: " + dueDate(after)
                + "\nタグ: " + tags(after));
    }

    // Kept for callers outside the task flow; task updates use the snapshot overloads above.
    public void notifyTaskCreated(Task task) {
        notifyTaskCreated(TaskNotificationData.from(task, List.of()));
    }

    public void notifyTaskUpdated(Task task) {
        if (task != null) send(task.getUserId(), formatCreated(TaskNotificationData.from(task, List.of())));
    }

    public void notifyTaskCompleted(Task task) {
        if (task != null) send(task.getUserId(), "【タスク完了！】" + display(task.getTitle()) + "\nステータス: 完了");
    }

    private void send(Long userId, String message) {
        if (userId == null || message == null) return;
        SlackConnection connection = connectionRepository.findByUserId(userId).orElse(null);
        if (connection == null || !SlackConnection.ACTIVE.equals(connection.getStatus())) return;
        SlackInstallation installation = installationRepository.findById(connection.getSlackInstallationId()).orElse(null);
        if (installation == null) return;

        final String token;
        try {
            token = tokenEncryptionService.decrypt(installation.getBotAccessTokenEncrypted());
        } catch (RuntimeException e) {
            logger.warn("Slack通知をスキップしました（Tokenの復号に失敗）");
            return;
        }

        try {
            String channelId = connection.getDmChannelId();
            if (channelId == null || channelId.isBlank()) channelId = openAndStore(connection, token);
            try {
                webApiClient.postMessage(token, channelId, message);
            } catch (HttpSlackWebApiClient.SlackWebApiException e) {
                if (!isInvalidChannel(e.getErrorCode())) throw e;
                connection.setDmChannelId(null);
                connectionRepository.save(connection);
                webApiClient.postMessage(token, openAndStore(connection, token), message);
            }
        } catch (HttpSlackWebApiClient.SlackWebApiException e) {
            handleApiFailure(connection, e.getErrorCode());
        } catch (IOException e) {
            logger.warn("Slack通知に失敗しました（通信エラー）");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Slack通知を中断しました");
        } catch (RuntimeException e) {
            logger.warn("Slack通知に失敗しました（予期しないエラー）");
        }
    }

    private String openAndStore(SlackConnection connection, String token) throws IOException, InterruptedException {
        String channelId = webApiClient.openDirectMessage(token, connection.getSlackUserId());
        connection.setDmChannelId(channelId);
        connectionRepository.save(connection);
        return channelId;
    }

    private void handleApiFailure(SlackConnection connection, String errorCode) {
        if ("invalid_auth".equals(errorCode) || "token_revoked".equals(errorCode)) {
            connection.setStatus(SlackConnection.REVOKED);
            connectionRepository.save(connection);
            logger.warn("Slack通知をスキップしました（連携Tokenが無効）");
        } else {
            logger.warn("Slack通知に失敗しました（APIエラー: {}）", errorCode);
        }
    }

    private boolean isInvalidChannel(String errorCode) {
        return "channel_not_found".equals(errorCode) || "invalid_channel".equals(errorCode);
    }

    private String formatCreated(TaskNotificationData data) {
        if (data == null) return null;
        return "【タスク登録】" + display(data.title())
                + "\nステータス: " + TaskDisplayLabels.status(data.status())
                + "\n優先度: " + TaskDisplayLabels.priority(data.priority())
                + "\n期限: " + dueDate(data)
                + "\nタグ: " + tags(data);
    }

    private List<String> changes(TaskNotificationData before, TaskNotificationData after) {
        List<String> changes = new ArrayList<>();
        addChange(changes, "タスク名", before.title(), after.title());
        addChange(changes, "説明", before.description(), after.description());
        addChange(changes, "ステータス", labelStatus(before.status()), labelStatus(after.status()));
        addChange(changes, "優先度", labelPriority(before.priority()), labelPriority(after.priority()));
        addChange(changes, "期限", dueDate(before), dueDate(after));
        addChange(changes, "タグ", tags(before), tags(after));
        return changes;
    }

    private void addChange(List<String> changes, String label, String before, String after) {
        if (!Objects.equals(before, after)) changes.add("・" + label + ": " + display(before) + " → " + display(after));
    }

    private String labelStatus(String value) { return TaskDisplayLabels.status(value); }
    private String labelPriority(String value) { return TaskDisplayLabels.priority(value); }
    private String dueDate(TaskNotificationData data) { return data.dueDate() == null ? "未設定" : DATE_FORMAT.format(data.dueDate()); }
    private String dueDate(TaskNotificationData data, boolean ignored) { return dueDate(data); }
    private String tags(TaskNotificationData data) { return data.tagNames().isEmpty() ? "なし" : String.join("、", data.tagNames()); }
    private String display(String value) { return value == null || value.isBlank() ? "未設定" : value; }
}
