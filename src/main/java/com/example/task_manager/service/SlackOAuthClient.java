package com.example.task_manager.service;

import com.example.task_manager.dto.SlackOAuthAccessResponse;

import java.io.IOException;

public interface SlackOAuthClient {
    SlackOAuthAccessResponse exchangeCode(String code) throws IOException, InterruptedException;
}
