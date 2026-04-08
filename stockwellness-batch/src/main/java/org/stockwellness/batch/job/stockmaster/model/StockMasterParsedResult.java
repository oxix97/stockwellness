package org.stockwellness.batch.job.stockmaster.model;

import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.insight.IndexConstituent;

import java.util.List;

public record StockMasterParsedResult(Stock stock, List<IndexConstituent> constituents) {}
