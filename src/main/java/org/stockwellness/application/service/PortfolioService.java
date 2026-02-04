package org.stockwellness.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.adapter.in.web.portfolio.dto.PortfolioResponse;
import org.stockwellness.application.port.in.portfolio.PortfolioUseCase;
import org.stockwellness.application.port.in.portfolio.command.CreatePortfolioCommand;
import org.stockwellness.application.port.in.portfolio.command.UpdatePortfolioCommand;
import org.stockwellness.application.port.out.portfolio.DeletePortfolioPort;
import org.stockwellness.application.port.out.portfolio.LoadPortfolioPort;
import org.stockwellness.application.port.out.portfolio.SavePortfolioPort;
import org.stockwellness.application.port.out.stock.LoadStockPort;
import org.stockwellness.domain.portfolio.AssetType;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.PortfolioItem;
import org.stockwellness.domain.portfolio.exception.DuplicatePortfolioNameException;
import org.stockwellness.domain.portfolio.exception.PortfolioAccessDeniedException;
import org.stockwellness.domain.portfolio.exception.PortfolioNotFoundException;
import org.stockwellness.domain.stock.exception.InvalidStockCodeException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioService implements PortfolioUseCase {

    private final LoadPortfolioPort loadPortfolioPort;
    private final SavePortfolioPort savePortfolioPort;
    private final DeletePortfolioPort deletePortfolioPort;
    private final LoadStockPort loadStockPort;

    @Override
    @Transactional
    public Long createPortfolio(CreatePortfolioCommand command) {
        if (loadPortfolioPort.existsPortfolioName(command.memberId(), command.name())) {
            throw new DuplicatePortfolioNameException();
        }

        Portfolio portfolio = Portfolio.create(command.memberId(), command.name(), command.description());
        
        List<PortfolioItem> items = command.items().stream()
                .map(item -> {
                    validateStockCode(item.stockCode(), item.assetType());
                    return mapToEntity(item.stockCode(), item.pieceCount(), item.assetType());
                })
                .toList();
        
        portfolio.updateItems(items);

        return savePortfolioPort.savePortfolio(portfolio).getId();
    }

    @Override
    public PortfolioResponse getPortfolio(Long memberId, Long portfolioId) {
        Portfolio portfolio = loadPortfolioPort.loadPortfolio(portfolioId, memberId)
                .orElseThrow(() -> {
                    if (loadPortfolioPort.findById(portfolioId).isPresent()) {
                        throw new PortfolioAccessDeniedException();
                    }
                    return new PortfolioNotFoundException();
                });

        return PortfolioResponse.from(portfolio);
    }

    @Override
    public List<PortfolioResponse> getMyPortfolios(Long memberId) {
        return loadPortfolioPort.loadAllPortfolios(memberId).stream()
                .map(PortfolioResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public void updatePortfolio(UpdatePortfolioCommand command) {
        Portfolio portfolio = loadPortfolioPort.loadPortfolio(command.portfolioId(), command.memberId())
                .orElseThrow(() -> {
                    if (loadPortfolioPort.findById(command.portfolioId()).isPresent()) {
                        throw new PortfolioAccessDeniedException();
                    }
                    return new PortfolioNotFoundException();
                });

        // 1. 기본 정보 수정 (이름 변경 시 중복 체크)
        if (!portfolio.getName().equals(command.name())) {
            if (loadPortfolioPort.existsPortfolioName(command.memberId(), command.name())) {
                throw new DuplicatePortfolioNameException();
            }
        }
        portfolio.updateBasicInfo(command.name(), command.description());

        // 2. 구성 종목 수정
        List<PortfolioItem> newItems = command.items().stream()
                .map(item -> {
                    validateStockCode(item.stockCode(), item.assetType());
                    return mapToEntity(item.stockCode(), item.pieceCount(), item.assetType());
                })
                .toList();

        portfolio.updateItems(newItems);
    }

    @Override
    @Transactional
    public void deletePortfolio(Long memberId, Long portfolioId) {
        // 소유권 확인 (updatePortfolio와 동일한 로직)
        loadPortfolioPort.loadPortfolio(portfolioId, memberId)
                .orElseThrow(() -> {
                    if (loadPortfolioPort.findById(portfolioId).isPresent()) {
                        throw new PortfolioAccessDeniedException();
                    }
                    return new PortfolioNotFoundException();
                });

        deletePortfolioPort.deletePortfolio(portfolioId);
    }

    private void validateStockCode(String stockCode, AssetType assetType) {
        if (assetType == AssetType.STOCK && !loadStockPort.existsByIsinCode(stockCode)) {
            throw new InvalidStockCodeException("Stock not found with code: " + stockCode);
        }
    }

    private PortfolioItem mapToEntity(String stockCode, int pieceCount, AssetType assetType) {
        if (assetType == AssetType.CASH) {
            return PortfolioItem.createCash(pieceCount);
        } else {
            return PortfolioItem.createStock(stockCode, pieceCount);
        }
    }
}