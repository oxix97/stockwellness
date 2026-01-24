package org.stockwellness.adapter.in.web.portfolio;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.stockwellness.adapter.in.web.portfolio.dto.PortfolioCreateRequest;
import org.stockwellness.adapter.in.web.portfolio.dto.PortfolioResponse;
import org.stockwellness.adapter.in.web.portfolio.dto.PortfolioUpdateRequest;
import org.stockwellness.application.port.in.portfolio.PortfolioUseCase;
import org.stockwellness.application.port.in.portfolio.command.CreatePortfolioCommand;
import org.stockwellness.application.port.in.portfolio.command.UpdatePortfolioCommand;
import org.stockwellness.global.security.CurrentMemberId;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/portfolios")
@RequiredArgsConstructor
public class PortfolioController {
    private final PortfolioUseCase portfolioUseCase;

    /**
     * 포트폴리오 생성
     * POST /api/portfolios
     */
    @PostMapping
    public ResponseEntity<Void> createPortfolio(
            @CurrentMemberId Long memberId,
            @RequestBody @Valid PortfolioCreateRequest request) {

        CreatePortfolioCommand command = new CreatePortfolioCommand(
            memberId,
            request.name(),
            request.description(),
            request.items().stream()
                .map(item -> new CreatePortfolioCommand.PortfolioItemCommand(
                    item.stockCode(),
                    item.pieceCount(),
                    item.assetType()
                ))
                .toList()
        );

        Long portfolioId = portfolioUseCase.createPortfolio(command);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(portfolioId)
                .toUri();
        return ResponseEntity.created(location).build();
    }

    /**
     * 내 포트폴리오 목록 조회
     * GET /api/portfolios
     */
    @GetMapping
    public ResponseEntity<List<PortfolioResponse>> getMyPortfolios(
            @CurrentMemberId Long memberId) {

        List<PortfolioResponse> responses = portfolioUseCase.getMyPortfolios(memberId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 포트폴리오 상세 조회
     * GET /api/portfolios/{portfolioId}
     */
    @GetMapping("/{portfolioId}")
    public ResponseEntity<PortfolioResponse> getPortfolio(
            @CurrentMemberId Long memberId,
            @PathVariable Long portfolioId) {

        PortfolioResponse response = portfolioUseCase.getPortfolio(memberId, portfolioId);
        return ResponseEntity.ok(response);
    }

    /**
     * 포트폴리오 수정 (구성 종목 변경)
     * PUT /api/portfolios/{portfolioId}
     */
    @PutMapping("/{portfolioId}")
    public ResponseEntity<Void> updatePortfolio(
            @CurrentMemberId Long memberId,
            @PathVariable Long portfolioId,
            @RequestBody @Valid PortfolioUpdateRequest request) {

        UpdatePortfolioCommand command = new UpdatePortfolioCommand(
            memberId,
            portfolioId,
            request.items().stream()
                .map(item -> new UpdatePortfolioCommand.PortfolioItemCommand(
                    item.stockCode(),
                    item.pieceCount(),
                    item.assetType()
                ))
                .toList()
        );

        portfolioUseCase.updatePortfolio(command);
        return ResponseEntity.noContent().build();
    }
}
