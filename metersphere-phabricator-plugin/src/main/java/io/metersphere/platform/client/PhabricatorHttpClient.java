package io.metersphere.platform.client;

import java.util.Map;

/**
 * Interface for HTTP operations to Phabricator API.
 * Extracted for testability - allows mocking in tests.
 */
public interface PhabricatorHttpClient {
    
    /**
     * Execute a POST request to the Phabricator API
     * @param url The full URL to call
     * @param formBody The form body content
     * @return The response body as string
     */
    String executePost(String url, String formBody);
    
    /**
     * Close the HTTP client and release resources
     */
    void close();
}
