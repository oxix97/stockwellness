package org.stockwellness.adapter.batch.stock.model;

import java.util.List;

import org.stockwellness.domain.stock.Stock;
import org.stockwellness.domain.stock.insight.IndexConstituent;

public record StockMasterParsedResult(Stock stock, List<IndexConstituent> constituents) {}
