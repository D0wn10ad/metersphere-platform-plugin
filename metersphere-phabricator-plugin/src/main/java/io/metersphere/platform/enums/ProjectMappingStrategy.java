package io.metersphere.platform.enums;

/**
 * Strategy for mapping Phabricator projects to MeterSphere
 */
public enum ProjectMappingStrategy {
    
    /**
     * Always map to root project, ignoring subprojects/milestones
     */
    ROOT_ONLY("Always map to root project"),
    
    /**
     * Map to current project context (may be subproject/milestone)
     */
    CURRENT_CONTEXT("Use current project context"),
    
    /**
     * Maintain full hierarchy path
     */
    HIERARCHICAL("Maintain full hierarchy"),
    
    /**
     * Automatic context detection
     */
    SMART_RESOLUTION("Automatic smart resolution");
    
    private final String description;
    
    ProjectMappingStrategy(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}