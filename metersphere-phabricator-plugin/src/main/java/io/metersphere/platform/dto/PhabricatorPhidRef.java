package io.metersphere.platform.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Reference to a Phabricator object by PHID
 */
@Getter
@Setter
public class PhabricatorPhidRef {
    
    private String phid;
    
    private String objectType;
    
    private String originalRef;
    
    private String resolvedName;
    
    private String resolvedUrl;
}