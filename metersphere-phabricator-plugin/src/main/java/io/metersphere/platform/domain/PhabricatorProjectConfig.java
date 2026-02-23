package io.metersphere.platform.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PhabricatorProjectConfig {
    private String projectPHID;

    public String getProjectPHID() {
        return projectPHID;
    }

    public void setProjectPHID(String projectPHID) {
        this.projectPHID = projectPHID;
    }
}
