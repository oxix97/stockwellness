package org.stockwellness.adapter.in.web.portfolio;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.stockwellness.adapter.in.web.portfolio.dto.DiagnosisResponse;
import org.stockwellness.application.port.in.portfolio.dto.PortfolioCreateRequest;
import org.stockwellness.application.port.in.portfolio.dto.PortfolioResponse;
import org.stockwellness.application.port.in.portfolio.dto.PortfolioUpdateRequest;
import org.stockwellness.application.port.in.portfolio.command.CreatePortfolioCommand;
import org.stockwellness.application.port.in.portfolio.command.UpdatePortfolioCommand;
import org.stockwellness.application.port.in.portfolio.result.PortfolioHealthResult;
import org.stockwellness.application.service.portfolio.PortfolioFacade;
import org.stockwellness.global.security.MemberPrincipal;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/portfolios")
@RequiredArgsConstructor
public class PortfolioController {
    
    private final PortfolioFacade portfolioFacade;

    /**
     * 포트폴리오 생성
     */
    @PostMapping
    public ResponseEntity<Void> createPortfolio(
            @AuthenticationPrincipal MemberPrincipal memberPrincipal,
            @RequestBody @Valid PortfolioCreateRequest request) {

        CreatePortfolioCommand command = new CreatePortfolioCommand(
            memberPrincipal.id(),
            request.name(),
            request.description(),
            request.items().stream()
                .map(item -> new CreatePortfolioCommand.PortfolioItemCommand(
                    item.symbol(),
                    item.quantity(),
                    item.purchasePrice(),
                    item.currency(),
                    item.assetType(),
                    item.targetWeight()
                ))
                .toList()
        );

        Long portfolioId = portfolioFacade.createPortfolio(command);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(portfolioId)
                .toUri();
        return ResponseEntity.created(location).build();
    }

    /**
     * 내 포트폴리오 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<PortfolioResponse>> getMyPortfolios(
            @AuthenticationPrincipal MemberPrincipal memberPrincipal) {

        List<PortfolioResponse> responses = portfolioFacade.getMyPortfolios(memberPrincipal.id());
        return ResponseEntity.ok(responses);
    }

    /**
     * 포트폴리오 상세 조회
     */
    @GetMapping("/{portfolioId}")
    public ResponseEntity<PortfolioResponse> getPortfolio(
            @AuthenticationPrincipal MemberPrincipal memberPrincipal,
            @PathVariable Long portfolioId) {

        PortfolioResponse response = portfolioFacade.getPortfolio(memberPrincipal.id(), portfolioId);
        return ResponseEntity.ok(response);
    }

    /**
     * 포트폴리오 수정 (구성 종목 변경)
     */
    @PutMapping("/{portfolioId}")
    public ResponseEntity<Void> updatePortfolio(
            @AuthenticationPrincipal MemberPrincipal memberPrincipal,
            @PathVariable Long portfolioId,
            @RequestBody @Valid PortfolioUpdateRequest request) {

        UpdatePortfolioCommand command = new UpdatePortfolioCommand(
            memberPrincipal.id(),
            portfolioId,
            request.name(),
            request.description(),
            request.items().stream()
                .map(item -> new UpdatePortfolioCommand.PortfolioItemCommand(
                    item.symbol(),
                    item.quantity(),
                    item.purchasePrice(),
                    item.currency(),
                    item.assetType(),
                    item.targetWeight()
                ))
                .toList()
        );

        portfolioFacade.updatePortfolio(command);
        return ResponseEntity.noContent().build();
    }

    /**
     * 포트폴리오 삭제
     */
    @DeleteMapping("/{portfolioId}")
    public ResponseEntity<Void> deletePortfolio(
            @AuthenticationPrincipal MemberPrincipal memberPrincipal,
            @PathVariable Long portfolioId) {

        portfolioFacade.deletePortfolio(memberPrincipal.id(), portfolioId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 포트폴리오 건강 진단
     */
    @GetMapping("/{portfolioId}/health")
    public ResponseEntity<DiagnosisResponse> diagnosePortfolio(
            @AuthenticationPrincipal MemberPrincipal memberPrincipal,
            @PathVariable Long portfolioId) {

        PortfolioHealthResult result = portfolioFacade.diagnosePortfolio(memberPrincipal.id(), portfolioId);
        return ResponseEntity.ok(DiagnosisResponse.from(result));
    }
}
