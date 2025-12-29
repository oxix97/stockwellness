package org.stockwellness.adapter.in.web.portfolio;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.stockwellness.adapter.in.web.portfolio.dto.PortfolioCreateRequest;
import org.stockwellness.adapter.in.web.portfolio.dto.PortfolioResponse;
import org.stockwellness.adapter.in.web.portfolio.dto.PortfolioUpdateRequest;
import org.stockwellness.application.service.PortfolioService;
import org.stockwellness.global.security.CurrentMemberId;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/portfolios")
@RequiredArgsConstructor
public class PortfolioController {
    private final PortfolioService portfolioService;

    /**
     * 포트폴리오 생성
     * POST /api/portfolios
     */
    @PostMapping
    public ResponseEntity<Void> createPortfolio(
            @CurrentMemberId Long memberId,
            @RequestBody @Valid PortfolioCreateRequest request) { // DTO에 @NotNull 등 추가 권장

        Long portfolioId = portfolioService.createPortfolio(memberId, request);

        // RESTful 원칙: 생성된 리소스의 위치(URI)를 Header에 담아 201 Created 반환
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

        List<PortfolioResponse> responses = portfolioService.getMyPortfolios(memberId);
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

        PortfolioResponse response = portfolioService.getPortfolio(memberId, portfolioId);
        return ResponseEntity.ok(response);
    }

    /**
     * 포트폴리오 수정 (구성 종목 변경)
     * PUT /api/portfolios/{portfolioId}
     * (전체 구성을 갈아끼우므로 PUT 사용, 부분 변경이라면 PATCH 권장)
     */
    @PutMapping("/{portfolioId}")
    public ResponseEntity<Void> updatePortfolio(
            @CurrentMemberId Long memberId,
            @PathVariable Long portfolioId,
            @RequestBody @Valid PortfolioUpdateRequest request) {

        portfolioService.updatePortfolio(memberId, portfolioId, request);
        return ResponseEntity.noContent().build();
    }
}
