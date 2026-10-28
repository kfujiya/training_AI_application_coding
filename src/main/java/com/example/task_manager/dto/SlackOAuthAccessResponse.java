package com.example.task_manager.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SlackOAuthAccessResponse {
    private boolean ok;
    private String error;
    @JsonProperty("access_token") private String accessToken;
    @JsonProperty("bot_user_id") private String botUserId;
    private String scope;
    private Team team;
    @JsonProperty("authed_user") private AuthedUser authedUser;

    public boolean isOk() { return ok; }
    public void setOk(boolean ok) { this.ok = ok; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getBotUserId() { return botUserId; }
    public void setBotUserId(String botUserId) { this.botUserId = botUserId; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }
    public AuthedUser getAuthedUser() { return authedUser; }
    public void setAuthedUser(AuthedUser authedUser) { this.authedUser = authedUser; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Team {
        private String id;
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AuthedUser {
        private String id;
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
    }
}
