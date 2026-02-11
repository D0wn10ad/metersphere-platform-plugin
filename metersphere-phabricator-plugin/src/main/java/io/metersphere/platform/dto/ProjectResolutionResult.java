package io.metersphere.platform.dto;

import io.metersphere.platform.domain.PlatformCustomFieldItemDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

/**
 * Result of project resolution operation
 */
@Getter
@Setter
@AllArgsConstructor
public class ProjectResolutionResult {
    
    private String primaryProjectId;
    
    private String finalProjectId;
    
    private List<PlatformCustomFieldItemDTO> projectTags;
    
    // Helper method
    public boolean usedFallback() {
        return primaryProjectId == null || 
               !primaryProjectId.equals(finalProjectId);
    }
}