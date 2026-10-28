package com.example.task_manager.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SlackConversationsOpenResponse {
    private boolean ok;
    private String error;
    private Channel channel;

    public boolean isOk() { return ok; }
    public void setOk(boolean ok) { this.ok = ok; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public Channel getChannel() { return channel; }
    public void setChannel(Channel channel) { this.channel = channel; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Channel {
        private String id;
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
    }
}
