package org.stockwellness.adapter.out.external.ai;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.stockwellness.adapter.out.external.ai.dto.OpenAiRequest;
import org.stockwellness.adapter.out.external.ai.dto.OpenAiResponse;
import org.stockwellness.application.port.out.LlmClientPort;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiAdapter implements LlmClientPort {

    private final RestClient aiRestClient;

    @Value("${ai.model}")
    private String model;

    @Override
    public String generateInsight(String systemInstruction, String userContext) {
        log.info("[AI Request] Model: {}, Context Length: {}", model, userContext.length());

        // 1. 요청 객체 생성 (DTO)
        OpenAiRequest request = OpenAiRequest.of(model, systemInstruction, userContext);

        // 2. RestClient 호출 (Virtual Thread 환경에서 Non-blocking 처럼 동작)
        OpenAiResponse response = aiRestClient.post()
                .uri("/chat/completions")
                .body(request)
                .retrieve()
                // 3. 에러 핸들링 (간단 예시)
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new RuntimeException("AI API Client Error: " + res.getStatusCode());
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                    throw new RuntimeException("AI API Server Error: " + res.getStatusCode());
                })
                .body(OpenAiResponse.class);

        // 4. 응답 파싱 및 반환
        return extractContent(response);
    }

    private String extractContent(OpenAiResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            log.error("AI Response is empty or null");
            throw new RuntimeException("AI 응답을 받아오지 못했습니다.");
        }

        // 토큰 사용량 로깅 (비용 추적용)
        log.info("[AI Response] Tokens Used - Prompt: {}, Completion: {}, Total: {}", 
            response.usage().prompt_tokens(),
            response.usage().completion_tokens(),
            response.usage().total_tokens()
        );

        return response.choices().getFirst().message().content();
    }
}