package com.chat.myAgent.learn.service.websearch.impl;

import com.chat.myAgent.learn.service.websearch.WebHit;
import com.chat.myAgent.learn.service.websearch.WebSearchProvider;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MockWebSearchProvider implements WebSearchProvider {

    @Override
    public List<WebHit> search(String query, int topK) {
        return List.of(
                WebHit.builder()
                        .title("Mock Search Result: " + query)
                        .url("https://example.com/result-1")
                        .snippet("This is a mock search result for query: " + query + ". Configure a real provider for production use.")
                        .score(0.95)
                        .build(),
                WebHit.builder()
                        .title("Mock Reference: Related Topic")
                        .url("https://example.com/result-2")
                        .snippet("Another mock result to demonstrate the web search capability. Replace with Tavily or Bing API.")
                        .score(0.82)
                        .build()
        );
    }
}
