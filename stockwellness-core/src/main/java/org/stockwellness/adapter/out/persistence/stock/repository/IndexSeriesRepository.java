package org.stockwellness.adapter.out.persistence.stock.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.stockwellness.domain.stock.insight.IndexSeries;

public interface IndexSeriesRepository extends JpaRepository<IndexSeries, Long> {

    List<IndexSeries> findByMarketIndex_PublicBenchmarkCodeAndReturnBasisAndDividendBasisAndCurrencyAndActiveTrueAndCanonicalTrue(
            String publicBenchmarkCode, String returnBasis, String dividendBasis, String currency);

    default List<IndexSeries> findCanonical(String publicCode, String returnBasis, String dividendBasis, String currency) {
        return findByMarketIndex_PublicBenchmarkCodeAndReturnBasisAndDividendBasisAndCurrencyAndActiveTrueAndCanonicalTrue(
                publicCode, returnBasis, dividendBasis, currency);
    }

    List<IndexSeries> findAllByProviderAndProviderNamespace(String provider, String providerNamespace);

    Optional<IndexSeries> findByProviderAndProviderNamespaceAndProviderSeriesCodeAndReturnBasisAndDividendBasisAndCurrency(
            String provider, String providerNamespace, String providerSeriesCode,
            String returnBasis, String dividendBasis, String currency);
}
