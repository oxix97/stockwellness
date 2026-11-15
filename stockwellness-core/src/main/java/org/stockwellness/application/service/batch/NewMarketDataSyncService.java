package org.stockwellness.application.service.batch;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.stockwellness.adapter.out.external.kis.dto.KisDailyPriceDetail;
import org.stockwellness.adapter.out.external.kis.dto.KisDailySectorDetail;
import org.stockwellness.adapter.out.external.kis.dto.KisHolidayEntry;
import org.stockwellness.adapter.out.external.kis.dto.KisInvestorTradeByStockDaily;
import org.stockwellness.application.port.in.batch.NewMarketDataSyncUseCase;
import org.stockwellness.application.port.out.external.kis.KisDailyPricePort;
import org.stockwellness.application.port.out.external.kis.KisIndexPricePort;
import org.stockwellness.application.port.out.external.kis.KisHolidayPort;
import org.stockwellness.application.port.out.stock.EodStockInvestorTradePort;
import org.stockwellness.application.port.out.stock.EodStockPricePort;
import org.stockwellness.application.port.out.stock.IndexDailyPort;
import org.stockwellness.application.port.out.stock.TradingCalendarPort;
import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.StockStatus;
import org.stockwellness.domain.stock.TradingCalendar;
import org.stockwellness.domain.stock.insight.BenchmarkDataStatus;
import org.stockwellness.domain.stock.insight.IndexDaily;
import org.stockwellness.domain.stock.insight.IndexSeries;
import org.stockwellness.domain.stock.price.EodStockInvestorTradeDaily;
import org.stockwellness.domain.stock.price.EodStockPrice;
import org.stockwellness.domain.stock.price.InvestorSourceType;
import org.stockwellness.domain.stock.price.StockPriceAdjustment;

@Service
@RequiredArgsConstructor
public class NewMarketDataSyncService implements NewMarketDataSyncUseCase {
    private static final String STOCK_API = "FHKST03010100";
    private static final String INDEX_API = "FHKUP03500100";
    private static final int STOCK_MAX_ROWS = 100;
    private static final int INDEX_MAX_ROWS = 50;
    private static final int PRICE_PRECISION = 19;
    private static final int AMOUNT_PRECISION = 25;
    private static final String INVESTOR_API = "FHPTJ04160001";
    // The existing KIS *_tr_pbmn contract is 백만원; store canonical KRW after this single conversion.
    private static final BigDecimal KIS_INVESTOR_AMOUNT_UNIT = new BigDecimal("1000000");
    private static final List<InvestorSourceType> INVESTOR_TYPES = List.of(InvestorSourceType.values());

    private final KisDailyPricePort kisDailyPricePort;
    private final KisIndexPricePort kisIndexPricePort;
    private final KisHolidayPort kisHolidayPort;
    private final EodStockPricePort eodStockPricePort;
    private final EodStockInvestorTradePort eodStockInvestorTradePort;
    private final IndexDailyPort indexDailyPort;
    private final TradingCalendarPort tradingCalendarPort;

    @Override
    @Transactional
    public void syncTradingCalendar(LocalDate baseDate) {
        if (baseDate == null) throw new IllegalArgumentException("KIS holiday base date is required");
        List<KisHolidayEntry> response = kisHolidayPort.fetchHolidays(baseDate);
        if (response == null || response.isEmpty()) throw new IllegalStateException("KIS returned no calendar rows");
        Set<LocalDate> dates = new HashSet<>();
        List<TradingCalendar> records = new ArrayList<>();
        for (KisHolidayEntry row : response) {
            LocalDate date = row == null ? null : parseDate(row.baseDate());
            if (date == null || !dates.add(date)
                    || !("Y".equals(row.marketOpen()) || "N".equals(row.marketOpen()))) {
                throw new IllegalArgumentException("KIS calendar response contains an invalid or duplicate row");
            }
            boolean open = "Y".equals(row.marketOpen());
            records.add(TradingCalendar.of("KOSPI", date, open, null, null, "KIS:CTCA0903R"));
            records.add(TradingCalendar.of("KOSDAQ", date, open, null, null, "KIS:CTCA0903R"));
        }
        if (!dates.contains(baseDate)) throw new IllegalArgumentException("KIS calendar response omitted the requested base date");
        tradingCalendarPort.upsert(records);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void syncStockPrices(Stock stock, LocalDate startDate, LocalDate endDate) {
        validateRange(stock, startDate, endDate);
        if (stock.getStatus() == StockStatus.DELISTED) throw new IllegalArgumentException("Delisted stock cannot be collected");
        List<LocalDate> openDates = verifiedOpenDates(stock.getMarketCode(), startDate, endDate);
        for (List<LocalDate> chunk : calendarWindows(openDates, startDate, endDate, STOCK_MAX_ROWS)) {
            LocalDate from = chunk.getFirst();
            LocalDate to = chunk.getLast();
            Set<LocalDate> requestedDates = Set.copyOf(chunk);
            List<KisDailyPriceDetail> rows = kisDailyPricePort.fetchDailyPrices(stock, from, to);
            if (rows == null || rows.size() > STOCK_MAX_ROWS) throw new IllegalArgumentException("Invalid KIS price response size");
            List<EodStockPrice> records = new ArrayList<>();
            Set<LocalDate> seen = new HashSet<>();
            for (KisDailyPriceDetail row : rows) {
                LocalDate date = row.baseDate();
                if (date == null || !requestedDates.contains(date) || !seen.add(date)) {
                    throw new IllegalArgumentException("KIS price response contains an invalid or duplicate date");
                }
                boolean complete = present(row.openPrice()) && present(row.highPrice()) && present(row.lowPrice())
                        && present(row.closePrice()) && row.volume() != null && present(row.transactionAmt())
                        && ("Y".equals(row.modYn()) || "N".equals(row.modYn()));
                var status = complete ? "PROVISIONAL" : "INCOMPLETE";
                records.add(EodStockPrice.of(stock, date, "KRX", price(row.openPrice()), price(row.highPrice()),
                        price(row.lowPrice()), price(row.closePrice()), row.volume(), amount(row.transactionAmt()),
                        "KIS:" + STOCK_API + ":RAW", status));
            }
            for (LocalDate missing : chunk) {
                if (seen.add(missing)) records.add(EodStockPrice.of(stock, missing, "KRX", null, null, null,
                        null, null, null, "KIS:" + STOCK_API + ":RAW", "INCOMPLETE"));
            }
            List<StockPriceAdjustment> adjustments = adjustedPrices(stock, from, to, requestedDates,
                    LocalDate.now(ZoneId.of("Asia/Seoul")));
            eodStockPricePort.upsertObservations(records, adjustments);
        }
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void syncStockInvestorTrades(Stock stock, LocalDate tradeDate) {
        if (stock == null || tradeDate == null) throw new IllegalArgumentException("Stock and trade date are required");
        if (!("KOSPI".equals(stock.getMarketCode()) || "KOSDAQ".equals(stock.getMarketCode()))
                || stock.getStatus() == StockStatus.DELISTED) {
            throw new IllegalArgumentException("Only non-delisted KRX stocks are supported for investor collection");
        }
        if (verifiedOpenDates(stock.getMarketCode(), tradeDate, tradeDate).size() != 1) {
            throw new IllegalArgumentException("Investor trade date is not an open KRX session");
        }

        List<KisInvestorTradeByStockDaily> response = kisDailyPricePort.fetchInvestorTradeByStockDaily(stock, tradeDate);
        if (response == null || response.size() > 1 || response.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("KIS investor response must contain at most one daily observation");
        }
        KisInvestorTradeByStockDaily observation = response.isEmpty() ? null : response.getFirst();
        if (observation != null && !tradeDate.equals(parseDate(observation.baseDate()))) {
            throw new IllegalArgumentException("KIS investor response date does not match the requested trade date");
        }

        Instant collectedAt = Instant.now();
        Map<InvestorSourceType, InvestorFlows> flows = investorFlows(observation);
        List<EodStockInvestorTradeDaily> records = new ArrayList<>(INVESTOR_TYPES.size());
        for (InvestorSourceType type : INVESTOR_TYPES) {
            InvestorFlows flow = flows.get(type);
            BigDecimal quantity = flow == null ? null : investorQuantity(flow.quantity());
            BigDecimal netAmount = flow == null ? null : investorAmount(flow.amount());
            String status = quantity != null && netAmount != null ? "PROVISIONAL" : "INCOMPLETE";
            records.add(new EodStockInvestorTradeDaily(stock.getId(), tradeDate, "KRX", type, quantity,
                    netAmount, status, "KIS:" + INVESTOR_API + ":J", collectedAt));
        }
        eodStockInvestorTradePort.upsert(records);
    }

    private List<StockPriceAdjustment> adjustedPrices(Stock stock, LocalDate from, LocalDate to,
            Set<LocalDate> requestedDates, LocalDate adjustmentAsOf) {
        List<KisDailyPriceDetail> rows = kisDailyPricePort.fetchAdjustedDailyPrices(stock, from, to);
        if (rows == null || rows.size() > STOCK_MAX_ROWS) {
            throw new IllegalArgumentException("Invalid KIS adjusted price response size");
        }
        Set<LocalDate> seen = new HashSet<>();
        List<StockPriceAdjustment> adjustments = new ArrayList<>();
        for (KisDailyPriceDetail row : rows) {
            LocalDate date = row.baseDate();
            if (date == null || !requestedDates.contains(date) || !seen.add(date)) {
                throw new IllegalArgumentException("KIS adjusted price response contains an invalid or duplicate date");
            }
            if (row.closePrice() == null) continue;
            adjustments.add(StockPriceAdjustment.of(stock, date, "KRX", adjustmentAsOf,
                    price(row.openPrice()), price(row.highPrice()), price(row.lowPrice()), price(row.closePrice()),
                    "KIS:" + STOCK_API + ":ADJUSTED"));
        }
        return adjustments;
    }

    @Override
    @Transactional
    public void syncIndexPrices(IndexSeries series, LocalDate startDate, LocalDate endDate) {
        validateRange(series, startDate, endDate);
        if (!"KIS".equals(series.getProvider()) || !series.isActive() || !series.isCanonical()
                || !"PRICE".equals(series.getReturnBasis()) || !"NONE".equals(series.getDividendBasis())
                || !"KRW".equals(series.getCurrency())) {
            throw new IllegalArgumentException("Only active canonical KIS KRW price index series are supported");
        }
        if (!("KOSPI".equals(series.getCalendarMarketCode()) || "KOSDAQ".equals(series.getCalendarMarketCode()))) {
            throw new IllegalArgumentException("Only KOSPI and KOSDAQ market indices are supported");
        }
        List<LocalDate> openDates = verifiedOpenDates(series.getCalendarMarketCode(), startDate, endDate);
        List<IndexDaily> records = new ArrayList<>();
        for (List<LocalDate> chunk : calendarWindows(openDates, startDate, endDate, INDEX_MAX_ROWS)) {
            LocalDate from = chunk.getFirst();
            LocalDate to = chunk.getLast();
            Set<LocalDate> requestedDates = Set.copyOf(chunk);
            List<KisDailySectorDetail> rows = kisIndexPricePort.fetchRawIndexDailyPrices(series.getProviderSeriesCode(), from, to);
            if (rows == null || rows.size() > INDEX_MAX_ROWS) throw new IllegalArgumentException("Invalid KIS index response size");
            Set<LocalDate> seen = new HashSet<>();
            for (KisDailySectorDetail row : rows) {
                LocalDate date = parseDate(row.stckBsopDate());
                if (date == null || !requestedDates.contains(date) || !seen.add(date)) {
                    throw new IllegalArgumentException("KIS index response contains an invalid or duplicate date");
                }
                BigDecimal open = decimal(row.sectorIndexOpenPrice());
                BigDecimal high = decimal(row.sectorIndexHighPrice());
                BigDecimal low = decimal(row.sectorIndexLowPrice());
                BigDecimal close = decimal(row.sectorIndexPrice());
                Long volume = integer(row.accumulatedVolume());
                BigDecimal tradingAmount = amount(decimal(row.accumulatedTradingValue()));
                BenchmarkDataStatus status = open != null && high != null && low != null && close != null
                        && volume != null && tradingAmount != null
                        ? BenchmarkDataStatus.PROVISIONAL : BenchmarkDataStatus.INCOMPLETE;
                records.add(IndexDaily.of(series, date, price(open), price(high), price(low), price(close), volume,
                        tradingAmount, null, null, null, null, null, null, null, INDEX_API, null, status, null));
            }
            for (LocalDate missing : chunk) {
                if (seen.add(missing)) records.add(IndexDaily.of(series, missing, null, null, null, null,
                        null, null, null, null, null, null, null, null, null, INDEX_API, null,
                        BenchmarkDataStatus.INCOMPLETE, null));
            }
        }
        indexDailyPort.upsert(records);
    }

    private List<LocalDate> verifiedOpenDates(String marketCode, LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, TradingCalendar> calendar = tradingCalendarPort.findAll(marketCode, startDate, endDate).stream()
                .collect(Collectors.toMap(TradingCalendar::getTradeDate, Function.identity(),
                        (left, right) -> { throw new IllegalStateException("Duplicate calendar date"); }));
        List<LocalDate> openDates = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            TradingCalendar day = calendar.get(date);
            if (day == null) throw new IllegalStateException("Trading calendar is unknown for " + marketCode + " " + date);
            if (day.isTradingDay()) openDates.add(date);
        }
        return openDates;
    }

    private static List<List<LocalDate>> calendarWindows(List<LocalDate> openDates, LocalDate startDate,
            LocalDate endDate, int maxCalendarDays) {
        List<List<LocalDate>> windows = new ArrayList<>();
        int cursor = 0;
        for (LocalDate windowStart = startDate; !windowStart.isAfter(endDate); windowStart = windowStart.plusDays(maxCalendarDays)) {
            LocalDate windowEnd = windowStart.plusDays(maxCalendarDays - 1L);
            if (windowEnd.isAfter(endDate)) windowEnd = endDate;
            int first = cursor;
            while (cursor < openDates.size() && !openDates.get(cursor).isAfter(windowEnd)) cursor++;
            if (cursor > first) windows.add(List.copyOf(openDates.subList(first, cursor)));
        }
        return windows;
    }

    private static void validateRange(Object target, LocalDate start, LocalDate end) {
        if (target == null || start == null || end == null || start.isAfter(end)) {
            throw new IllegalArgumentException("A valid source and date range are required");
        }
    }

    private static LocalDate parseDate(String value) {
        if (value == null || !value.matches("\\d{8}")) return null;
        try { return LocalDate.parse(value, java.time.format.DateTimeFormatter.BASIC_ISO_DATE); }
        catch (java.time.DateTimeException ex) { return null; }
    }

    private static boolean present(BigDecimal value) { return value != null; }
    private static BigDecimal decimal(String value) {
        if (value == null || value.isBlank()) return null;
        try { return new BigDecimal(value.trim()); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException("Invalid numeric value in KIS response", ex); }
    }
    private static Long integer(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Long.valueOf(value.trim()); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException("Invalid integer in KIS response", ex); }
    }
    private static BigDecimal price(BigDecimal value) { return scaled(value, PRICE_PRECISION, 4); }
    private static BigDecimal amount(BigDecimal value) { return scaled(value, AMOUNT_PRECISION, 2); }

    private static Map<InvestorSourceType, InvestorFlows> investorFlows(KisInvestorTradeByStockDaily row) {
        Map<InvestorSourceType, InvestorFlows> values = new EnumMap<>(InvestorSourceType.class);
        if (row == null) return values;
        values.put(InvestorSourceType.FOREIGN, new InvestorFlows(row.foreignNetQuantity(), row.foreignNetAmount()));
        values.put(InvestorSourceType.INSTITUTION_TOTAL,
                new InvestorFlows(row.institutionNetQuantity(), row.institutionNetAmount()));
        values.put(InvestorSourceType.INDIVIDUAL, new InvestorFlows(row.individualNetQuantity(), row.individualNetAmount()));
        values.put(InvestorSourceType.FINANCIAL_INVESTMENT,
                new InvestorFlows(row.financialInvestmentNetQuantity(), row.financialInvestmentNetAmount()));
        values.put(InvestorSourceType.INSURANCE, new InvestorFlows(row.insuranceNetQuantity(), row.insuranceNetAmount()));
        values.put(InvestorSourceType.TRUST, new InvestorFlows(row.trustNetQuantity(), row.trustNetAmount()));
        values.put(InvestorSourceType.PRIVATE_FUND, new InvestorFlows(row.privateFundNetQuantity(), row.privateFundNetAmount()));
        values.put(InvestorSourceType.BANK, new InvestorFlows(row.bankNetQuantity(), row.bankNetAmount()));
        values.put(InvestorSourceType.PENSION, new InvestorFlows(row.pensionNetQuantity(), row.pensionNetAmount()));
        values.put(InvestorSourceType.MERCHANT_BANK,
                new InvestorFlows(row.merchantBankNetQuantity(), row.merchantBankNetAmount()));
        values.put(InvestorSourceType.OTHER_ORGANIZATION,
                new InvestorFlows(row.otherOrganizationNetQuantity(), row.otherOrganizationNetAmount()));
        values.put(InvestorSourceType.OTHER_CORPORATE,
                new InvestorFlows(row.otherCorporateNetQuantity(), row.otherCorporateNetAmount()));
        values.put(InvestorSourceType.OTHER, new InvestorFlows(row.otherNetQuantity(), row.otherNetAmount()));
        return values;
    }

    private static BigDecimal investorQuantity(String raw) {
        return scaledSigned(signedDecimal(raw), 19, 6);
    }

    private static BigDecimal investorAmount(String raw) {
        BigDecimal value = signedDecimal(raw);
        return value == null ? null : scaledSigned(value.multiply(KIS_INVESTOR_AMOUNT_UNIT), AMOUNT_PRECISION, 2);
    }

    private static BigDecimal signedDecimal(String value) {
        if (value == null || value.isBlank()) return null;
        try { return new BigDecimal(value.replace(",", "").trim()); }
        catch (NumberFormatException ex) { throw new IllegalArgumentException("Invalid investor value in KIS response", ex); }
    }

    private record InvestorFlows(String quantity, String amount) {}

    private static BigDecimal scaled(BigDecimal value, int precision, int scale) {
        if (value == null) return null;
        BigDecimal result;
        try { result = value.setScale(scale, RoundingMode.UNNECESSARY); }
        catch (ArithmeticException ex) { throw new IllegalArgumentException("KIS numeric precision exceeds storage scale", ex); }
        if (result.signum() < 0 || result.precision() > precision) throw new IllegalArgumentException("KIS numeric value is outside storage range");
        return result;
    }

    private static BigDecimal scaledSigned(BigDecimal value, int precision, int scale) {
        if (value == null) return null;
        BigDecimal result;
        try { result = value.setScale(scale, RoundingMode.UNNECESSARY); }
        catch (ArithmeticException ex) { throw new IllegalArgumentException("KIS investor value exceeds storage scale", ex); }
        if (result.precision() > precision) throw new IllegalArgumentException("KIS investor value is outside storage range");
        return result;
    }
}
