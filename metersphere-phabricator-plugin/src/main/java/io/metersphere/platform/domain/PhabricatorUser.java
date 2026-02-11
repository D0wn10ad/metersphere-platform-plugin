package io.metersphere.platform.domain;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

/**
 * Represents a Phabricator user
 */
@Getter
@Setter
public class PhabricatorUser {
    
    private String phid;
    
    private String id;
    
    private String username;
    
    private String realName;
    
    private List<String> roles;
    
    private String imageUrl;
    
    private String uri;
}