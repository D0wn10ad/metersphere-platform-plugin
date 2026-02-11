package io.metersphere.platform.response;

import lombok.Getter;
import lombok.Setter;
import java.util.Map;

/**
 * Generic search response from Phabricator
 */
@Getter
@Setter
public class PhabricatorSearchResponse<T> {
    
    private Map<String, T> data;
    
    private Map<String, Object> maps;
    
    private Map<String, Object> query;
    
    private PhabricatorCursor cursor;
    
    @Getter
    @Setter
    public static class PhabricatorCursor {
        private int limit;
        private String after;
        private String before;
    }
    
    public boolean hasNextPage() {
        return cursor != null && cursor.getAfter() != null;
    }
    
    public boolean hasPreviousPage() {
        return cursor != null && cursor.getBefore() != null;
    }
}