package com.chat.myAgent.learn.service.websearch;

import java.util.List;

public interface WebSearchProvider {
    List<WebHit> search(String query, int topK);
}
