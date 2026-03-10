package org.stockwellness.application.service.portfolio;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.application.port.in.portfolio.ManagePortfolioUseCase;
import org.stockwellness.application.port.in.portfolio.command.CreatePortfolioCommand;
import org.stockwellness.application.port.in.portfolio.command.UpdatePortfolioCommand;
import org.stockwellness.application.port.out.portfolio.PortfolioPort;
import org.stockwellness.application.port.out.stock.StockPort;
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
@Transactional
public class PortfolioCommandService implements ManagePortfolioUseCase {

    private final PortfolioPort portfolioPort;
    private final StockPort stockPort;

    @Override
    public Long createPortfolio(CreatePortfolioCommand command) {
        if (portfolioPort.existsPortfolioName(command.memberId(), command.name())) {
            throw new DuplicatePortfolioNameException();
        }

        Portfolio portfolio = Portfolio.create(command.memberId(), command.name(), command.description());

        List<PortfolioItem> items = command.items().stream()
                .map(this::mapToEntity)
                .toList();

        portfolio.updateItems(items);

        return portfolioPort.savePortfolio(portfolio).getId();
    }

    @Override
    public void updatePortfolio(UpdatePortfolioCommand command) {
        Portfolio portfolio = loadOwnedPortfolio(command.portfolioId(), command.memberId());

        // 1. 기본 정보 수정 (이름 변경 시 중복 체크)
        if (!portfolio.getName().equals(command.name())) {
            if (portfolioPort.existsPortfolioName(command.memberId(), command.name())) {
                throw new DuplicatePortfolioNameException();
            }
        }
        portfolio.updateBasicInfo(command.name(), command.description());

        // 2. 구성 종목 수정
        List<PortfolioItem> newItems = command.items().stream()
                .map(this::mapToEntity)
                .toList();

        portfolio.updateItems(newItems);
    }

    @Override
    public void deletePortfolio(Long memberId, Long portfolioId) {
        loadOwnedPortfolio(portfolioId, memberId);
        portfolioPort.deletePortfolio(portfolioId);
    }

    private Portfolio loadOwnedPortfolio(Long portfolioId, Long memberId) {
        return portfolioPort.loadPortfolio(portfolioId, memberId)
                .orElseThrow(() -> {
                    if (portfolioPort.findById(portfolioId).isPresent()) {
                        throw new PortfolioAccessDeniedException();
                    }
                    return new PortfolioNotFoundException();
                });
    }

    private PortfolioItem mapToEntity(CreatePortfolioCommand.PortfolioItemCommand item) {
        if (item.assetType() == AssetType.STOCK) {
            if (!stockPort.existsByTicker(item.symbol())) {
                throw new InvalidStockCodeException("Stock not found with symbol: " + item.symbol());
            }
            return PortfolioItem.createStock(item.symbol(), item.quantity(), item.purchasePrice(), item.currency());
        } else {
            return PortfolioItem.createCash(item.quantity(), item.currency());
        }
    }

    private PortfolioItem mapToEntity(UpdatePortfolioCommand.PortfolioItemCommand item) {
        if (item.assetType() == AssetType.STOCK) {
            if (!stockPort.existsByTicker(item.symbol())) {
                throw new InvalidStockCodeException("Stock not found with symbol: " + item.symbol());
            }
            return PortfolioItem.createStock(item.symbol(), item.quantity(), item.purchasePrice(), item.currency());
        } else {
            return PortfolioItem.createCash(item.quantity(), item.currency());
        }
    }
}
