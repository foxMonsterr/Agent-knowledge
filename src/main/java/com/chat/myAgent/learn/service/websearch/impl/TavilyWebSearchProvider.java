package com.chat.myAgent.learn.service.websearch.impl;

import com.chat.myAgent.learn.service.websearch.WebHit;
import com.chat.myAgent.learn.service.websearch.WebSearchProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@ConditionalOnProperty(name = "smart-agent.web.tavily.enabled", havingValue = "true")
public class TavilyWebSearchProvider implements WebSearchProvider {

    private static final String TAVILY_API_URL = "https://api.tavily.com/search";

    @Override
    public List<WebHit> search(String query, int topK) {
        log.info("Tavily WebSearch invoked: query={}, topK={}", query, topK);
        // TODO: Implement Tavily API HTTP call when API key is configured
        // For now, return empty results — the real implementation calls Tavily REST API
        return List.of();
    }
}
