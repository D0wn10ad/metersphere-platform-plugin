package io.metersphere.platform.domain;

import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.Map;

/**
 * Represents a Maniphest task from Phabricator
 */
@Getter
@Setter
public class PhabricatorTask {
    
    private String phid;
    
    private String id;
    
    private String subtype;
    
    private String title;
    
    private String description;
    
    private String authorPHID;
    
    private String ownerPHID;
    
    private String status;
    
    private int priority;
    
    private List<String> projectPHIDs;
    
    private List<String> subscriberPHIDs;
    
    private long dateCreated;
    
    private long dateModified;
    
    private Map<String, Object> customFields;
    
    // Helper methods
    public boolean isOpen() {
        return "open".equals(status);
    }
    
    public boolean hasOwner() {
        return ownerPHID != null && !ownerPHID.isEmpty();
    }
    
    public boolean isInProject(String projectPHID) {
        return projectPHIDs != null && projectPHIDs.contains(projectPHID);
    }
    
    // Alias for title (compatibility with Platform interface)
    public String getName() {
        return title;
    }
    
    public void setName(String name) {
        this.title = name;
    }
}