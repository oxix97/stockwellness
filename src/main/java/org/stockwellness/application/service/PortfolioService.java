package org.stockwellness.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.adapter.in.web.portfolio.dto.PortfolioResponse;
import org.stockwellness.application.port.in.portfolio.PortfolioUseCase;
import org.stockwellness.application.port.in.portfolio.command.CreatePortfolioCommand;
import org.stockwellness.application.port.in.portfolio.command.UpdatePortfolioCommand;
import org.stockwellness.application.port.out.portfolio.LoadPortfolioPort;
import org.stockwellness.application.port.out.portfolio.SavePortfolioPort;
import org.stockwellness.domain.portfolio.AssetType;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.PortfolioItem;
import org.stockwellness.global.error.ErrorCode;
import org.stockwellness.global.error.exception.BusinessException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PortfolioService implements PortfolioUseCase {

    private final LoadPortfolioPort loadPortfolioPort;
    private final SavePortfolioPort savePortfolioPort;

    @Override
    @Transactional
    public Long createPortfolio(CreatePortfolioCommand command) {
        if (loadPortfolioPort.existsPortfolioName(command.memberId(), command.name())) {
            throw new BusinessException(ErrorCode.DUPLICATE_PORTFOLIO_NAME);
        }

        Portfolio portfolio = Portfolio.create(command.memberId(), command.name(), command.description());
        
        List<PortfolioItem> items = command.items().stream()
                .map(item -> mapToEntity(item.stockCode(), item.pieceCount(), item.assetType()))
                .toList();
        
        portfolio.updateItems(items);

        return savePortfolioPort.savePortfolio(portfolio).getId();
    }

    @Override
    public PortfolioResponse getPortfolio(Long memberId, Long portfolioId) {
        Portfolio portfolio = loadPortfolioPort.loadPortfolio(portfolioId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PORTFOLIO_NOT_FOUND));

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
                .orElseThrow(() -> new BusinessException(ErrorCode.PORTFOLIO_NOT_FOUND));

        List<PortfolioItem> newItems = command.items().stream()
                .map(item -> mapToEntity(item.stockCode(), item.pieceCount(), item.assetType()))
                .toList();

        portfolio.updateItems(newItems);
    }

    private PortfolioItem mapToEntity(String stockCode, int pieceCount, AssetType assetType) {
        if (assetType == AssetType.CASH) {
            return PortfolioItem.createCash(pieceCount);
        } else {
            return PortfolioItem.createStock(stockCode, pieceCount);
        }
    }
}