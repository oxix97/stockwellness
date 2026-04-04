package org.stockwellness.adapter.out.external.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.stockwellness.application.port.in.portfolio.result.PortfolioAiResult;
import org.stockwellness.application.port.out.portfolio.AiAdviceProviderPort;
import org.stockwellness.application.port.out.portfolio.AiAdviceProviderPort.AdvisorAiResult;
import org.stockwellness.application.port.out.portfolio.AdvisorAiContext;
import org.stockwellness.application.port.out.portfolio.LoadPortfolioAiPort;
import org.stockwellness.application.port.out.portfolio.PortfolioAiContext;
import org.stockwellness.application.port.out.sector.LoadSectorAiPort;
import org.stockwellness.application.port.out.sector.SectorAiContext;
import org.stockwellness.application.port.out.stock.LlmClientPort;
import org.stockwellness.domain.stock.analysis.AiAnalysisContext;
import org.stockwellness.domain.stock.analysis.AiReport;

@Slf4j
@Component
public class OpenAiAdapter implements LlmClientPort, LoadPortfolioAiPort, LoadSectorAiPort, AiAdviceProviderPort {

    private final ChatClient chatClient;
    private final PromptTemplateMapper promptTemplateMapper;

    public OpenAiAdapter(ChatClient.Builder builder, PromptTemplateMapper promptTemplateMapper) {
        this.chatClient = builder.build();
        this.promptTemplateMapper = promptTemplateMapper;
    }

    @Override
    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2.0)
    )
    public AiReport generateSectorOpinion(SectorAiContext context) {
        String systemInstruction = promptTemplateMapper.getSectorSystemInstruction();
        String userContext = promptTemplateMapper.toSectorPromptString(context);

        return executeAiCall(systemInstruction, userContext, AiReport.class, "Sector AI Analysis");
    }

    @Recover
    public AiReport recoverSectorOpinion(Exception e, SectorAiContext context) {
        log.error("❌ Sector AI Analysis Failed after retries: {}", e.getMessage());
        return AiReport.fallback();
    }

    @Override
    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2.0)
    )
    public PortfolioAiResult generatePortfolioInsight(PortfolioAiContext context) {
        String systemInstruction = promptTemplateMapper.getPortfolioSystemInstruction();
        String userContext = promptTemplateMapper.toPortfolioPromptString(context);

        return executeAiCall(systemInstruction, userContext, PortfolioAiResult.class, "Portfolio AI Diagnosis");
    }

    @Recover
    public PortfolioAiResult recoverPortfolioInsight(Exception e, PortfolioAiContext context) {
        log.error("❌ Portfolio AI Diagnosis Failed after retries: {}", e.getMessage());
        return PortfolioAiResult.fallback();
    }

    @Override
    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2.0)
    )
    public AiReport generateInsight(String systemInstruction, AiAnalysisContext context) {
        String userContext = promptTemplateMapper.toPromptString(context);

        return executeAiCall(systemInstruction, userContext, AiReport.class, "Stock AI Analysis");
    }

    @Recover
    public AiReport recoverGenerateInsight(Exception e, String systemInstruction, AiAnalysisContext context) {
        log.error("❌ Stock AI Analysis Failed after retries: {}", e.getMessage());
        return AiReport.fallback();
    }

    @Override
    @Retryable(
            retryFor = {Exception.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2.0)
    )
    public AdvisorAiResult getRebalancingAdvice(AdvisorAiContext context) {
        String systemInstruction = promptTemplateMapper.getAdvisorSystemInstruction();
        String userContext = promptTemplateMapper.toAdvisorPromptString(context);

        return executeAiCall(systemInstruction, userContext, AdvisorAiResult.class, "Portfolio Rebalancing Advice");
    }

    @Recover
    public AdvisorAiResult recoverRebalancingAdvice(Exception e, AdvisorAiContext context) {
        log.error("❌ Portfolio Rebalancing Advice Failed after retries: {}", e.getMessage());
        return AdvisorAiResult.fallback();
    }

    /**
     * 공통 AI 호출 로직 (Generic)
     */
    private <T> T executeAiCall(String system, String user, Class<T> clazz, String taskName) {
        log.info("📡 Requesting {}... (Prompt Length: {})", taskName, user.length());
        var outputConverter = new BeanOutputConverter<>(clazz);

        return chatClient.prompt()
                .system(system)
                .user(u -> u.text(user + "\n\n반드시 아래 JSON 포맷을 준수하여 응답해:\n{format}")
                        .param("format", outputConverter.getFormat()))
                .call()
                .entity(outputConverter);
    }
}
