package org.stockwellness.fixture;

import org.springframework.test.util.ReflectionTestUtils;
import org.stockwellness.application.port.in.portfolio.dto.PortfolioCreateRequest;
import org.stockwellness.application.port.in.portfolio.dto.PortfolioItemRequest;
import org.stockwellness.application.port.in.portfolio.dto.PortfolioUpdateRequest;
import org.stockwellness.application.port.in.portfolio.command.CreatePortfolioCommand;
import org.stockwellness.application.port.in.portfolio.command.UpdatePortfolioCommand;
import org.stockwellness.domain.portfolio.AssetType;
import org.stockwellness.domain.portfolio.Portfolio;
import org.stockwellness.domain.portfolio.PortfolioItem;

import java.util.List;

public class PortfolioFixture {

    public static final Long MEMBER_ID = 1L;
    public static final Long PORTFOLIO_ID = 100L;
    public static final String NAME = "내 연금 포트폴리오";
    public static final String DESCRIPTION = "은퇴 자금 마련용";

    /**
     * 기본 포트폴리오 엔티티 생성
     */
    public static Portfolio createEntity() {
        return Portfolio.create(MEMBER_ID, NAME, DESCRIPTION);
    }

    /**
     * ID가 부여된 포트폴리오 엔티티 생성
     */
    public static Portfolio createEntity(Long id) {
        Portfolio portfolio = createEntity();
        ReflectionTestUtils.setField(portfolio, "id", id);
        return portfolio;
    }

    /**
     * 구성 종목이 포함된 엔티티 생성
     */
    public static Portfolio createEntityWithItems(Long id, List<PortfolioItem> items) {
        Portfolio portfolio = createEntity(id);
        portfolio.updateItems(items);
        return portfolio;
    }

    /**
     * 생성 커맨드 생성
     */
    public static CreatePortfolioCommand createCreateCommand(List<CreatePortfolioCommand.PortfolioItemCommand> items) {
        return new CreatePortfolioCommand(MEMBER_ID, NAME, DESCRIPTION, items);
    }

    /**
     * 수정 커맨드 생성
     */
    public static UpdatePortfolioCommand createUpdateCommand(Long portfolioId, String name, List<UpdatePortfolioCommand.PortfolioItemCommand> items) {
        return new UpdatePortfolioCommand(MEMBER_ID, portfolioId, name, DESCRIPTION, items);
    }

    /**
     * 생성 요청 DTO 생성
     */
    public static PortfolioCreateRequest createCreateRequest(List<PortfolioItemRequest> items) {
        return new PortfolioCreateRequest(NAME, DESCRIPTION, items);
    }

    /**
     * 수정 요청 DTO 생성
     */
    public static PortfolioUpdateRequest createUpdateRequest(String name, List<PortfolioItemRequest> items) {
        return new PortfolioUpdateRequest(name, DESCRIPTION, items);
    }

    /**
     * 아이템 요청 DTO 헬퍼
     */
    public static PortfolioItemRequest createItemRequest(String code, int count, AssetType type) {
        return new PortfolioItemRequest(code, count, type);
    }

    /**
     * 주식 항목 커맨드 헬퍼 (Create용)
     */
    public static CreatePortfolioCommand.PortfolioItemCommand createStockItem(String code, int count) {
        return new CreatePortfolioCommand.PortfolioItemCommand(code, count, AssetType.STOCK);
    }

    /**
     * 현금 항목 커맨드 헬퍼 (Create용)
     */
    public static CreatePortfolioCommand.PortfolioItemCommand createCashItem(int count) {
        return new CreatePortfolioCommand.PortfolioItemCommand("KRW", count, AssetType.CASH);
    }

    /**
     * 주식 항목 커맨드 헬퍼 (Update용)
     */
    public static UpdatePortfolioCommand.PortfolioItemCommand updateStockItem(String code, int count) {
        return new UpdatePortfolioCommand.PortfolioItemCommand(code, count, AssetType.STOCK);
    }
}