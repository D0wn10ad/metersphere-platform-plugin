package io.metersphere.platform.utils;

/**
 * Utility methods for PHID handling
 */
public class PhabricatorPhidUtils {
    
    public static boolean isValidPHID(String phid) {
        return phid != null && 
               phid.startsWith("PHID-") && 
               phid.length() > 10;
    }
    
    public static String extractType(String phid) {
        if (!isValidPHID(phid)) {
            return null;
        }
        return phid.split("-")[1];
    }
    
    public static String extractId(String phid) {
        if (!isValidPHID(phid)) {
            return null;
        }
        String[] parts = phid.split("-");
        return parts.length > 2 ? parts[2] : null;
    }
}