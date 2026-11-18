package org.stockwellness.application.service.stock;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.stockwellness.adapter.out.persistence.stock.repository.IndexSeriesRepository;
import org.stockwellness.domain.stock.exception.UnsupportedBenchmarkSeriesException;
import org.stockwellness.domain.stock.insight.IndexSeries;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class BenchmarkSeriesResolverTest {
    @Mock
    private IndexSeriesRepository repository;

    @InjectMocks
    private BenchmarkSeriesResolver resolver;

    @Test
    void resolves_only_the_requested_public_code_basis_and_currency() {
        IndexSeries canonical = org.mockito.Mockito.mock(IndexSeries.class);
        given(repository.findCanonical("0001", "PRICE", "NONE", "KRW")).willReturn(List.of(canonical));

        assertThat(resolver.resolve("0001", "PRICE", "NONE", "KRW")).isSameAs(canonical);
    }

    @Test
    void unsupported_total_return_series_does_not_fall_back_to_price_return() {
        given(repository.findCanonical("0001", "TOTAL_RETURN", "GROSS", "KRW")).willReturn(List.of());

        assertThatThrownBy(() -> resolver.resolve("0001", "TOTAL_RETURN", "GROSS", "KRW"))
                .isInstanceOf(UnsupportedBenchmarkSeriesException.class);
    }

    @Test
    void missing_canonical_series_fails_instead_of_choosing_a_noncanonical_provider() {
        given(repository.findCanonical("0001", "PRICE", "NONE", "KRW")).willReturn(List.of());

        assertThatThrownBy(() -> resolver.resolve("0001", "PRICE", "NONE", "KRW"))
                .isInstanceOf(UnsupportedBenchmarkSeriesException.class);
    }

    @Test
    void inactive_canonical_provider_is_not_resolvable() {
        given(repository.findCanonical("0001", "PRICE", "NONE", "KRW")).willReturn(List.of());

        assertThatThrownBy(() -> resolver.resolve("0001", "PRICE", "NONE", "KRW"))
                .isInstanceOf(UnsupportedBenchmarkSeriesException.class);
    }

    @Test
    void ambiguous_canonical_series_fails_closed() {
        IndexSeries first = org.mockito.Mockito.mock(IndexSeries.class);
        IndexSeries second = org.mockito.Mockito.mock(IndexSeries.class);
        given(repository.findCanonical("0001", "PRICE", "NONE", "KRW")).willReturn(List.of(first, second));

        assertThatThrownBy(() -> resolver.resolve("0001", "PRICE", "NONE", "KRW"))
                .isInstanceOf(UnsupportedBenchmarkSeriesException.class);
    }
}
