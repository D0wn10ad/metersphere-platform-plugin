package io.metersphere.platform.domain;

import lombok.Getter;
import lombok.Setter;
import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.Map;

/**
 * Main configuration for Phabricator integration
 */
@Getter
@Setter
public class PhabricatorConfig {
    
    @NotBlank(message = "Phabricator URL is required")
    private String url;
    
    @NotBlank(message = "API Token is required")
    private String apiToken;
    
    private boolean verifySSL = true;
    
    private int timeout = 30000;
    
    private String proxyHost;
    
    private Integer proxyPort;
}