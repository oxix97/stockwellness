package org.stockwellness.adapter.batch.benchmarkprice.model;

import org.stockwellness.adapter.out.external.kis.dto.BenchmarkPriceData;
import org.stockwellness.domain.stock.BenchmarkType;

/**
 * Reader와 Processor 사이에서 지수 타입과 개별 시세 데이터를 함께 전달하기 위한 래퍼
 */
public record BenchmarkPriceDataWrapper(BenchmarkType type, BenchmarkPriceData data) {
}
