package io.metersphere.platform.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PhabricatorConfig {
    private String url;
    private String apiToken;
    private String user;
    private String debugMode = "true";
    private String msUrl;
    private String syncPriority = "true";
    private String syncEnvironment = "true";

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public boolean isDebugMode() {
        return "true".equalsIgnoreCase(debugMode);
    }

    public String getDebugMode() {
        return debugMode;
    }

    public void setDebugMode(String debugMode) {
        this.debugMode = debugMode;
    }

    public String getMsUrl() {
        return msUrl;
    }

    public void setMsUrl(String msUrl) {
        this.msUrl = msUrl;
    }

    public boolean isSyncPriority() {
        return "true".equalsIgnoreCase(syncPriority);
    }

    public String getSyncPriority() {
        return syncPriority;
    }

    public void setSyncPriority(String syncPriority) {
        this.syncPriority = syncPriority;
    }

    public boolean isSyncEnvironment() {
        return "true".equalsIgnoreCase(syncEnvironment);
    }

    public String getSyncEnvironment() {
        return syncEnvironment;
    }

    public void setSyncEnvironment(String syncEnvironment) {
        this.syncEnvironment = syncEnvironment;
    }
}
