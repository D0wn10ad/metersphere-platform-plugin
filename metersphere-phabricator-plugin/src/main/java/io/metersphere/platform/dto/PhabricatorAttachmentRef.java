package io.metersphere.platform.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Reference to a Phabricator attachment/file
 */
@Getter
@Setter
public class PhabricatorAttachmentRef {
    
    private String filePHID;
    
    private String fileName;
    
    private String originalRef;  // e.g., "{F123}"
    
    private String mimeType;
    
    private long fileSize;
    
    private String proxyUrl;
    
    public String generateProxyLink(String baseUrl) {
        return String.format("[%s](%s/file/data/%s/)", 
            fileName, baseUrl, filePHID);
    }
}