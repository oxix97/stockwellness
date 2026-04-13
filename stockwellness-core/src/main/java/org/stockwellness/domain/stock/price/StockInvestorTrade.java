package org.stockwellness.domain.stock.price;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.stockwellness.domain.stock.Stock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static jakarta.persistence.FetchType.LAZY;

@Getter
@Entity
@Table(name = "stock_investor_trade")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class StockInvestorTrade {

    @EmbeddedId
    private StockInvestorTradeId id;

    @MapsId("stockId")
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(name = "name")
    private String name;

    @Column(name = "ticker")
    private String ticker;

    @Column(name = "frgn_ntby_qty")
    private Long frgnNtbyQty;

    @Column(name = "orgn_ntby_qty")
    private Long orgnNtbyQty;

    @Column(name = "ivtr_ntby_qty")
    private Long ivtrNtbyQty;

    @Column(name = "bank_ntby_qty")
    private Long bankNtbyQty;

    @Column(name = "insu_ntby_qty")
    private Long insuNtbyQty;

    @Column(name = "mrbn_ntby_qty")
    private Long mrbnNtbyQty;

    @Column(name = "fund_ntby_qty")
    private Long fundNtbyQty;

    @Column(name = "etc_orgt_ntby_vol")
    private Long etcOrgtNtbyVol;

    @Column(name = "etc_corp_ntby_vol")
    private Long etcCorpNtbyVol;

    @Column(name = "frgn_ntby_tr_pbmn", precision = 25, scale = 2)
    private BigDecimal frgnNtbyTrPbmn;

    @Column(name = "orgn_ntby_tr_pbmn", precision = 25, scale = 2)
    private BigDecimal orgnNtbyTrPbmn;

    @Column(name = "ivtr_ntby_tr_pbmn", precision = 25, scale = 2)
    private BigDecimal ivtrNtbyTrPbmn;

    @Column(name = "bank_ntby_tr_pbmn", precision = 25, scale = 2)
    private BigDecimal bankNtbyTrPbmn;

    @Column(name = "insu_ntby_tr_pbmn", precision = 25, scale = 2)
    private BigDecimal insuNtbyTrPbmn;

    @Column(name = "mrbn_ntby_tr_pbmn", precision = 25, scale = 2)
    private BigDecimal mrbnNtbyTrPbmn;

    @Column(name = "fund_ntby_tr_pbmn", precision = 25, scale = 2)
    private BigDecimal fundNtbyTrPbmn;

    @Column(name = "etc_orgt_ntby_tr_pbmn", precision = 25, scale = 2)
    private BigDecimal etcOrgtNtbyTrPbmn;

    @Column(name = "etc_corp_ntby_tr_pbmn", precision = 25, scale = 2)
    private BigDecimal etcCorpNtbyTrPbmn;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static StockInvestorTrade of(
            Stock stock,
            LocalDate baseDate,
            String name,
            String ticker,
            Long frgnNtbyQty,
            Long orgnNtbyQty,
            Long ivtrNtbyQty,
            Long bankNtbyQty,
            Long insuNtbyQty,
            Long mrbnNtbyQty,
            Long fundNtbyQty,
            Long etcOrgtNtbyVol,
            Long etcCorpNtbyVol,
            BigDecimal frgnNtbyTrPbmn,
            BigDecimal orgnNtbyTrPbmn,
            BigDecimal ivtrNtbyTrPbmn,
            BigDecimal bankNtbyTrPbmn,
            BigDecimal insuNtbyTrPbmn,
            BigDecimal mrbnNtbyTrPbmn,
            BigDecimal fundNtbyTrPbmn,
            BigDecimal etcOrgtNtbyTrPbmn,
            BigDecimal etcCorpNtbyTrPbmn
    ) {
        StockInvestorTrade entity = new StockInvestorTrade();
        entity.id = new StockInvestorTradeId(stock.getId(), baseDate);
        entity.stock = stock;
        entity.name = name;
        entity.ticker = ticker;
        entity.frgnNtbyQty = frgnNtbyQty;
        entity.orgnNtbyQty = orgnNtbyQty;
        entity.ivtrNtbyQty = ivtrNtbyQty;
        entity.bankNtbyQty = bankNtbyQty;
        entity.insuNtbyQty = insuNtbyQty;
        entity.mrbnNtbyQty = mrbnNtbyQty;
        entity.fundNtbyQty = fundNtbyQty;
        entity.etcOrgtNtbyVol = etcOrgtNtbyVol;
        entity.etcCorpNtbyVol = etcCorpNtbyVol;
        entity.frgnNtbyTrPbmn = frgnNtbyTrPbmn;
        entity.orgnNtbyTrPbmn = orgnNtbyTrPbmn;
        entity.ivtrNtbyTrPbmn = ivtrNtbyTrPbmn;
        entity.bankNtbyTrPbmn = bankNtbyTrPbmn;
        entity.insuNtbyTrPbmn = insuNtbyTrPbmn;
        entity.mrbnNtbyTrPbmn = mrbnNtbyTrPbmn;
        entity.fundNtbyTrPbmn = fundNtbyTrPbmn;
        entity.etcOrgtNtbyTrPbmn = etcOrgtNtbyTrPbmn;
        entity.etcCorpNtbyTrPbmn = etcCorpNtbyTrPbmn;
        return entity;
    }
}
