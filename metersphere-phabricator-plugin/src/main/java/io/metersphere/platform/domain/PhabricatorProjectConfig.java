package io.metersphere.platform.domain;

import io.metersphere.platform.enums.ProjectMappingStrategy;
import lombok.Getter;
import lombok.Setter;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * Project-level configuration for Phabricator mapping
 */
@Getter
@Setter
public class PhabricatorProjectConfig {
    
    @NotNull(message = "Primary strategy is required")
    private ProjectMappingStrategy primaryStrategy;
    
    @NotBlank(message = "Default project ID is required")
    private String defaultProjectId;
    
    // Status configuration
    private List<String> closureStatuses = List.of(
        "resolved", "wontfix", "invalid", "spite", "duplicate"
    );
    
    private boolean syncClosedIssues = true;
    
    private Map<String, String> statusMapping;
    
    // Subtype configuration
    private List<String> allowedSubtypes = List.of("task", "story", "epic");
    
    private String defaultSubtype = "task";
    
    private Map<String, String> subtypeMapping;
    
    // Tag configuration (mandatory)
    private String tagFormat = "[{type}] {name}";
    
    private boolean includeRootTag = true;
    
    private boolean includeSubprojectTag = true;
    
    private boolean includeMilestoneTag = true;
    
    private boolean includeRelatedTag = true;
}