package io.metersphere.platform.dto;

import io.metersphere.platform.domain.PhabricatorProject;
import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.Map;

/**
 * Complete project context for a task
 */
@Getter
@Setter
public class PhabricatorProjectContext {
    
    private String rootProjectPHID;
    
    private String rootProjectName;
    
    private String currentProjectPHID;
    
    private String currentProjectName;
    
    private String currentProjectType;
    
    private List<String> allProjectPHIDs;
    
    private List<String> allProjectNames;
    
    private Map<String, PhabricatorProject> projectDetails;
    
    private int hierarchyDepth;
    
    public PhabricatorProject getRootProject() {
        return projectDetails != null ? 
               projectDetails.get(rootProjectPHID) : null;
    }
    
    public PhabricatorProject getCurrentProject() {
        return projectDetails != null ? 
               projectDetails.get(currentProjectPHID) : null;
    }
}