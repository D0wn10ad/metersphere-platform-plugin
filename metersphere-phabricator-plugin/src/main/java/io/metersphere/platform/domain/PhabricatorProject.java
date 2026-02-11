package io.metersphere.platform.domain;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

/**
 * Represents a Phabricator project
 */
@Getter
@Setter
public class PhabricatorProject {
    
    private String phid;
    
    private String id;
    
    private String name;
    
    private String slug;
    
    private String type;
    
    private String subtype;
    
    private String parentPHID;
    
    private List<String> childPHIDs;
    
    private int depth;
    
    private String milestone;
    
    private String icon;
    
    private String color;
    
    // Helper methods
    public boolean isRootProject() {
        return parentPHID == null || parentPHID.isEmpty();
    }
    
    public boolean isSubproject() {
        return parentPHID != null && !parentPHID.isEmpty() && milestone == null;
    }
    
    public boolean isMilestone() {
        return milestone != null;
    }
    
    public String getProjectType() {
        if (isMilestone()) return "milestone";
        if (isSubproject()) return "subproject";
        return "project";
    }
}