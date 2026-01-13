package org.stockwellness.adapter.out.external.ai.dto;

import java.util.List;

public record OpenAiRequest(
    String model,
    List<Message> messages,
    double temperature,
    int max_tokens
) {
    public record Message(String role, String content) {}

    public static OpenAiRequest of(String model, String system, String user) {
        return new OpenAiRequest(
            model,
            List.of(
                new Message("system", system),
                new Message("user", user)
            ),
            0.7, // 창의성 조절 (금융 분석이므로 너무 높지 않게)
            1000 // 토큰 제한
        );
    }
}