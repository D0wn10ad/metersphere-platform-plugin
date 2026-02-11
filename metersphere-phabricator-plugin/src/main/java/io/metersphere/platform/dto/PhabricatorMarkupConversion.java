package io.metersphere.platform.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

/**
 * Result of remarkup to markdown conversion
 */
@Getter
@Setter
public class PhabricatorMarkupConversion {
    
    private String originalRemarkup;
    
    private String convertedMarkdown;
    
    private List<PhabricatorAttachmentRef> extractedAttachments;
    
    private List<PhabricatorPhidRef> extractedPhidRefs;
    
    private boolean conversionSuccessful;
    
    private String errorMessage;
}