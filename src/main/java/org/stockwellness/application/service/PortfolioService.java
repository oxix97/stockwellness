package org.stockwellness.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.adapter.in.web.portfolio.dto.PortfolioCreateRequest;
import org.stockwellness.adapter.in.web.portfolio.dto.PortfolioItemRequest;
import org.stockwellness.adapter.in.web.portfolio.dto.PortfolioResponse;
import org.stockwellness.adapter.in.web.portfolio.dto.PortfolioUpdateRequest;
import org.stockwellness.adapter.out.persistence.portfolio.PortfolioJpaRepository;
import org.stockwellness.domain.portfolio.AssetType;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.PortfolioItem;
import org.stockwellness.global.error.ErrorCode;
import org.stockwellness.global.error.exception.BusinessException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본적으로 읽기 전용
public class PortfolioService {

    private final PortfolioJpaRepository portfolioRepository;

    /**
     * 포트폴리오 생성
     */
    @Transactional // 쓰기 트랜잭션
    public Long createPortfolio(Long memberId, PortfolioCreateRequest request) {
        // 1. 이름 중복 검사
        if (portfolioRepository.existsByMemberIdAndName(memberId, request.name())) {
            throw new BusinessException(ErrorCode.DUPLICATE_PORTFOLIO_NAME);
        }

        // 2. 포트폴리오 엔티티 생성
        Portfolio portfolio = Portfolio.create(memberId, request.name(), request.description());

        // 3. 아이템 변환 및 주입 (도메인 로직 검증 발생)
        List<PortfolioItem> items = mapToEntities(request.items());
        portfolio.updateItems(items);

        // 4. 저장
        return portfolioRepository.save(portfolio).getId();
    }

    /**
     * 포트폴리오 상세 조회
     */
    public PortfolioResponse getPortfolio(Long memberId, Long portfolioId) {
        // Fetch Join을 사용하여 아이템까지 한 번에 조회 (성능 최적화)
        Portfolio portfolio = portfolioRepository.findWithItems(portfolioId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PORTFOLIO_NOT_FOUND));

        return PortfolioResponse.from(portfolio);
    }

    /**
     * 포트폴리오 목록 조회
     */
    public List<PortfolioResponse> getMyPortfolios(Long memberId) {
        return portfolioRepository.findAllByMemberId(memberId).stream()
                .map(PortfolioResponse::from)
                .toList();
    }

    /**
     * 포트폴리오 구성 수정
     */
    @Transactional
    public void updatePortfolio(Long memberId, Long portfolioId, PortfolioUpdateRequest request) {
        // 1. 조회 및 소유권 확인
        Portfolio portfolio = portfolioRepository.findByIdAndMemberId(portfolioId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PORTFOLIO_NOT_FOUND));

        // 2. 새로운 아이템 리스트 생성
        List<PortfolioItem> newItems = mapToEntities(request.items());

        // 3. 도메인 메서드 호출 (Dirty Checking에 의해 자동 update/delete/insert 발생)
        portfolio.updateItems(newItems);
    }

    // --- Helper Methods ---

    // DTO List -> Entity List 변환기
    private List<PortfolioItem> mapToEntities(List<PortfolioItemRequest> itemRequests) {
        return itemRequests.stream()
                .map(this::mapToEntity)
                .toList();
    }

    // 개별 아이템 변환 (Factory Method 활용)
    private PortfolioItem mapToEntity(PortfolioItemRequest req) {
        if (req.assetType() == AssetType.CASH) {
            return PortfolioItem.createCash(req.pieceCount());
        } else {
            return PortfolioItem.createStock(req.stockCode(), req.pieceCount());
        }
    }
}