package io.metersphere.platform.enums;

/**
 * Types of Phabricator objects
 */
public enum PhabricatorObjectType {
    
    TASK("TASK", "Maniphest Task"),
    PROJECT("PROJ", "Project"),
    USER("USER", "User"),
    FILE("FILE", "File"),
    DIFFERENTIAL("DREV", "Differential Revision"),
    COMMIT("CMIT", "Commit"),
    MOCK("MOCK", "Pholio Mock");
    
    private final String typeCode;
    private final String description;
    
    PhabricatorObjectType(String typeCode, String description) {
        this.typeCode = typeCode;
        this.description = description;
    }
    
    public String getTypeCode() {
        return typeCode;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static PhabricatorObjectType fromPHID(String phid) {
        if (phid == null || !phid.startsWith("PHID-")) {
            return null;
        }
        String typePart = phid.split("-")[1];
        for (PhabricatorObjectType type : values()) {
            if (type.typeCode.equals(typePart)) {
                return type;
            }
        }
        return null;
    }
}