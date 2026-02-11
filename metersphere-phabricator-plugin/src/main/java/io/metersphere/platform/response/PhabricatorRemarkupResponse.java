package io.metersphere.platform.response;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

/**
 * Response from remarkup.process
 */
@Getter
@Setter
public class PhabricatorRemarkupResponse {
    
    private List<String> content;
    
    public String getFirstContent() {
        return content != null && !content.isEmpty() ? 
               content.get(0) : null;
    }
}