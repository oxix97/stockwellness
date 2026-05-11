package org.stockwellness.adapter.out.external.ai;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.stockwellness.application.port.out.external.SearchApiPort;

@Slf4j
@Component
@RequiredArgsConstructor
public class TavilyAdapter implements SearchApiPort {

    private final RestClient.Builder restClientBuilder;

    @Value("${app.ai.tavily.api-key:}")
    private String apiKey;

    private static final String TAVILY_API_URL = "https://api.tavily.com/search";

    @Override
    public String searchFinancialNews(String query) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("⚠️ Tavily API Key is missing. Skipping news search.");
            return "";
        }

        try {
            log.info("📡 Searching Tavily for: {}", query);
            Map<String, Object> requestBody = Map.of(
                "api_key", apiKey,
                "query", query,
                "search_depth", "basic",
                "include_answer", false,
                "max_results", 5
            );

            Map response = restClientBuilder.build()
                .post()
                .uri(TAVILY_API_URL)
                .body(requestBody)
                .retrieve()
                .body(Map.class);

            if (response == null || !response.containsKey("results")) {
                return "";
            }

            List<Map> results = (List<Map>) response.get("results");
            StringBuilder sb = new StringBuilder();
            for (Map result : results) {
                sb.append("- ").append(result.get("title")).append(": ").append(result.get("content")).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("❌ Tavily Search Failed: {}", e.getMessage());
            return "";
        }
    }
}
