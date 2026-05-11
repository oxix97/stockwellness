package org.stockwellness.domain.portfolio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.stockwellness.domain.portfolio.advisor.AdvisorReport;
import org.stockwellness.domain.portfolio.exception.InvalidPortfolioException;
import org.stockwellness.domain.shared.AbstractEntity;
import org.stockwellness.global.util.FinanceCalculationUtil;
import static jakarta.persistence.CascadeType.ALL;
import static jakarta.persistence.FetchType.LAZY;
import static lombok.AccessLevel.PROTECTED;

/**
 * 사용자의 포트폴리오를 관리하는 도메인 모델입니다.
 * 포트폴리오 이름, 설명, 구성 종목(items) 및 AI 조언 리포트 정보를 포함합니다.
 */
@Getter
@Entity
@ToString
@NoArgsConstructor(access = PROTECTED)
public class Portfolio extends AbstractEntity {

    /**
     * 포트폴리오 소유 사용자의 ID
     */
    @Column(nullable = false)
    private Long memberId;

    /**
     * 포트폴리오 이름
     */
    @Column(nullable = false)
    private String name;

    /**
     * 포트폴리오 설명
     */
    private String description;

    /**
     * 포트폴리오를 구성하는 개별 종목 리스트
     */
    @ToString.Exclude
    @OneToMany(fetch = LAZY, mappedBy = "portfolio", cascade = ALL, orphanRemoval = true)
    private List<PortfolioItem> items = new ArrayList<>();

    /**
     * AI 어드바이저가 생성한 조언 리포트 이력
     */
    @ToString.Exclude
    @OneToMany(fetch = LAZY, mappedBy = "portfolio", cascade = ALL, orphanRemoval = true)
    private List<AdvisorReport> advisorReports = new ArrayList<>();

    /**
     * 새로운 포트폴리오 인스턴스를 생성합니다.
     *
     * @param memberId 소유자 ID
     * @param name 포트폴리오 이름
     * @param description 포트폴리오 설명
     * @return 생성된 포트폴리오 객체
     */
    public static Portfolio create(Long memberId, String name, String description) {
        Portfolio portfolio = new Portfolio();
        portfolio.memberId = memberId;
        portfolio.name = name;
        portfolio.description = description;
        return portfolio;
    }

    /**
     * 포트폴리오의 기본 정보(이름, 설명)를 수정합니다.
     *
     * @param name 새로운 이름
     * @param description 새로운 설명
     */
    public void updateBasicInfo(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /**
     * AI 어드바이저의 리포트를 포트폴리오 이력에 추가합니다.
     *
     * @param report 추가할 조언 리포트
     */
    public void addAdvisorReport(AdvisorReport report) {
        this.advisorReports.add(report);
    }

    /**
     * 포트폴리오의 구성 종목 리스트를 새로운 리스트로 교체합니다.
     * 교체 시 모든 종목의 목표 비중 합계가 100%인지 검증합니다.
     *
     * @param newItems 새로운 구성 종목 리스트
     */
    public void updateItems(List<PortfolioItem> newItems) {
        validateTargetWeightSum(newItems);
        this.items.clear();
        this.items.addAll(newItems);
        newItems.forEach(item -> item.assignPortfolio(this));
    }

    /**
     * 구성 종목들의 목표 비중(Target Weight) 합계가 유효한지 검사합니다.
     * 비중 설정이 시작된 경우 합계는 반드시 100%여야 합니다.
     *
     * @param items 검증할 종목 리스트
     * @throws InvalidPortfolioException 비중 합계가 100%가 아닐 때 발생
     */
    private void validateTargetWeightSum(List<PortfolioItem> items) {
        if (items.isEmpty()) return;

        BigDecimal sum = items.stream()
                .map(PortfolioItem::getTargetWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 모든 비중이 0인 경우는 초기 상태로 간주하고 허용
        if (sum.compareTo(BigDecimal.ZERO) == 0) return;

        // 비중 설정이 시작되었다면 정확히 100%여야 함 (소수점 4자리 정밀도까지 확인)
        BigDecimal targetSum = BigDecimal.valueOf(100).setScale(4, RoundingMode.HALF_UP);
        if (sum.setScale(4, RoundingMode.HALF_UP).compareTo(targetSum) != 0) {
            throw new InvalidPortfolioException();
        }
    }

    /**
     * 모든 구성 종목의 총 매수 금액 합계를 계산합니다.
     *
     * @return 총 매수 금액
     */
    public BigDecimal calculateTotalPurchaseAmount() {
        return items.stream()
                .map(PortfolioItem::calculatePurchaseAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 현재 시세 맵을 기반으로 포트폴리오 전체의 평가 가치를 계산합니다.
     *
     * @param currentPrices 종목별 현재가 맵
     * @return 총 평가 가치
     */
    public BigDecimal calculateTotalCurrentValue(Map<String, BigDecimal> currentPrices) {
        BigDecimal total = BigDecimal.ZERO;
        for (PortfolioItem item : items) {
            BigDecimal price = (item.getAssetType() == AssetType.CASH) ? BigDecimal.ONE : currentPrices.get(item.getSymbol());
            if (price == null) price = item.getPurchasePrice(); // 가격 정보 없으면 매수가 기준 (0 방지)
            
            BigDecimal value = (item.getAssetType() == AssetType.CASH) ? item.getQuantity() : item.getQuantity().multiply(price);
            total = total.add(value);
        }
        return total;
    }

    /**
     * 현재 시세 기반 전체 손익 금액을 계산합니다.
     *
     * @param currentPrices 종목별 현재가 맵
     * @return 총 손익 금액 (현재가치 - 총 매수금액)
     */
    public BigDecimal calculateTotalProfitLoss(Map<String, BigDecimal> currentPrices) {
        return calculateTotalCurrentValue(currentPrices).subtract(calculateTotalPurchaseAmount());
    }

    /**
     * 현재 시세 기반 전체 수익률(%)을 계산합니다.
     *
     * @param currentPrices 종목별 현재가 맵
     * @return 전체 수익률 (%)
     */
    public BigDecimal calculateTotalReturnRate(Map<String, BigDecimal> currentPrices) {
        BigDecimal totalPurchase = calculateTotalPurchaseAmount();
        if (totalPurchase.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        
        return FinanceCalculationUtil.calculateRate(calculateTotalProfitLoss(currentPrices), totalPurchase);
    }

    /**
     * 포트폴리오의 최초 매수일(Inception Date)을 조회합니다.
     *
     * @return 최초 매수일
     */
    public LocalDate getInceptionDate() {
        return items.stream()
                .map(PortfolioItem::getPurchaseDate)
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now());
    }
}
