package io.metersphere.platform.client;

import io.metersphere.platform.domain.PhabricatorConfig;
import org.springframework.web.client.RestTemplate;

public class PhabricatorClient {
    private PhabricatorConfig config;
    private RestTemplate restTemplate;

    public void setConfig(PhabricatorConfig config) {
        this.config = config;
    }

    public void auth() {
        // Stub - to be implemented in future phase
    }

    public boolean testConnection() {
        // Stub - to be implemented in future phase
        return true;
    }
}
