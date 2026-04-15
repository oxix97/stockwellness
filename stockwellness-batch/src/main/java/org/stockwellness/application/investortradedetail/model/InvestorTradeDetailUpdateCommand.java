package org.stockwellness.application.investortradedetail.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvestorTradeDetailUpdateCommand(
        Long stockId,
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
}
