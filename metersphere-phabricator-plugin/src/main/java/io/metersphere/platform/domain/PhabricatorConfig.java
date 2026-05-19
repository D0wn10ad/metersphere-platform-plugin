package io.metersphere.platform.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PhabricatorConfig {
    private String url;
    private String apiToken;
    private String user;
    private boolean debugMode = true;
    private String msUrl;
    private boolean syncPriority = true;
    private boolean syncEnvironment = true;

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
        return debugMode;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    public String getMsUrl() {
        return msUrl;
    }

    public void setMsUrl(String msUrl) {
        this.msUrl = msUrl;
    }

    public boolean isSyncPriority() {
        return syncPriority;
    }

    public void setSyncPriority(boolean syncPriority) {
        this.syncPriority = syncPriority;
    }

    public boolean isSyncEnvironment() {
        return syncEnvironment;
    }

    public void setSyncEnvironment(boolean syncEnvironment) {
        this.syncEnvironment = syncEnvironment;
    }
}
