package org.stockwellness.adapter.out.external.ai.dto;

import java.util.List;

public record OpenAiResponse(
    String id,
    List<Choice> choices,
    Usage usage
) {
    public record Choice(int index, Message message, String finish_reason) {}
    public record Message(String role, String content) {}
    public record Usage(int prompt_tokens, int completion_tokens, int total_tokens) {}
}