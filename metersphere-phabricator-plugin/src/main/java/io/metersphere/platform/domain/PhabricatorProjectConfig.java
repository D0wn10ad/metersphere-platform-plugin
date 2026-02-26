package io.metersphere.platform.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PhabricatorProjectConfig {
    private String projectPHID;
    private String primaryStrategy;
    private String defaultProjectId;
    private List<String> closureStatuses;
    private boolean syncClosedIssues = true;
    private List<String> allowedSubtypes;
    private String defaultSubtype = "bug";

    public String getProjectPHID() {
        return projectPHID;
    }

    public void setProjectPHID(String projectPHID) {
        this.projectPHID = projectPHID;
    }

    public String getPrimaryStrategy() {
        return primaryStrategy;
    }

    public void setPrimaryStrategy(String primaryStrategy) {
        this.primaryStrategy = primaryStrategy;
    }

    public String getDefaultProjectId() {
        return defaultProjectId;
    }

    public void setDefaultProjectId(String defaultProjectId) {
        this.defaultProjectId = defaultProjectId;
    }

    public List<String> getClosureStatuses() {
        return closureStatuses;
    }

    public void setClosureStatuses(List<String> closureStatuses) {
        this.closureStatuses = closureStatuses;
    }

    public boolean isSyncClosedIssues() {
        return syncClosedIssues;
    }

    public void setSyncClosedIssues(boolean syncClosedIssues) {
        this.syncClosedIssues = syncClosedIssues;
    }

    public List<String> getAllowedSubtypes() {
        return allowedSubtypes;
    }

    public void setAllowedSubtypes(List<String> allowedSubtypes) {
        this.allowedSubtypes = allowedSubtypes;
    }

    public String getDefaultSubtype() {
        return defaultSubtype;
    }

    public void setDefaultSubtype(String defaultSubtype) {
        this.defaultSubtype = defaultSubtype;
    }
}
