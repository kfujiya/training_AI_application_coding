package com.example.task_manager.service;

import java.io.IOException;

public interface SlackWebApiClient {
    String openDirectMessage(String botToken, String slackUserId) throws IOException, InterruptedException;
    void postMessage(String botToken, String channelId, String text) throws IOException, InterruptedException;
}
