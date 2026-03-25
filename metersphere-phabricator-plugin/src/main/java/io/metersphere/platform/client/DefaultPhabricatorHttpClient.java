package io.metersphere.platform.client;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;

import java.nio.charset.StandardCharsets;

/**
 * Default HTTP client implementation for Phabricator API.
 */
public class DefaultPhabricatorHttpClient implements PhabricatorHttpClient {
    
    private CloseableHttpClient httpClient;
    private RequestConfig requestConfig;
    
    public DefaultPhabricatorHttpClient() {
        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
            .setMaxConnTotal(20)
            .setMaxConnPerRoute(10)
            .build();

        this.requestConfig = RequestConfig.custom()
            .setConnectTimeout(Timeout.ofSeconds(30))
            .setResponseTimeout(Timeout.ofSeconds(120))
            .setConnectionRequestTimeout(Timeout.ofSeconds(30))
            .build();

        this.httpClient = HttpClients.custom()
            .setDefaultRequestConfig(requestConfig)
            .setConnectionManager(connectionManager)
            .build();
    }
    
    @Override
    public String executePost(String url, String formBody) {
        try {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
            httpPost.setEntity(new StringEntity(formBody, 
                ContentType.create("application/x-www-form-urlencoded", StandardCharsets.UTF_8)));

            try (var response = httpClient.execute(httpPost)) {
                return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            throw new RuntimeException("HTTP request failed: " + e.getMessage(), e);
        }
    }
    
    public void close() {
        try {
            httpClient.close();
        } catch (Exception e) {
            // Ignore
        }
    }
}