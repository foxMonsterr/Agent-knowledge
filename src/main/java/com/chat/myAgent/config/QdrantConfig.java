package com.chat.myAgent.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QdrantConfig {

    @Value("${spring.data.qdrant.host:localhost}")
    private String host;

    @Value("${spring.data.qdrant.port:6334}")
    private int port;

    @Value("${spring.data.qdrant.collection-name:smart-agent}")
    private String collectionName;

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getCollectionName() {
        return collectionName;
    }
}
