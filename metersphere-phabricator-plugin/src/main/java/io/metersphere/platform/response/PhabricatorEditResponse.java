package io.metersphere.platform.response;

import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.Map;

/**
 * Response from maniphest.edit or similar edit operations
 */
@Getter
@Setter
public class PhabricatorEditResponse {
    
    private String objectPHID;
    
    private Map<String, Object> transactions;
    
    private List<String> errorMessages;
    
    public boolean hasErrors() {
        return errorMessages != null && !errorMessages.isEmpty();
    }
}