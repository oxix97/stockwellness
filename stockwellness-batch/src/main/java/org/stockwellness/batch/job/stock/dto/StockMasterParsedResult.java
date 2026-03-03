package org.stockwellness.batch.job.stock.dto;

import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.insight.IndexConstituent;

import java.util.List;

public record StockMasterParsedResult(Stock stock, List<IndexConstituent> constituents) {}