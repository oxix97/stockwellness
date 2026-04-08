package org.stockwellness.batch.support.logging;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class BatchProviderResolver {

    private final List<BatchStartSummaryProvider> startSummaryProviders;
    private final List<BatchProgressSnapshotProvider> progressSnapshotProviders;
    private final List<BatchFailureSummaryProvider> failureSummaryProviders;

    public BatchProviderResolver(
            List<BatchStartSummaryProvider> startSummaryProviders,
            List<BatchProgressSnapshotProvider> progressSnapshotProviders,
            List<BatchFailureSummaryProvider> failureSummaryProviders
    ) {
        this.startSummaryProviders = startSummaryProviders;
        this.progressSnapshotProviders = progressSnapshotProviders;
        this.failureSummaryProviders = failureSummaryProviders;
    }

    public Optional<BatchStartSummaryProvider> findStartSummaryProvider(String jobName) {
        return startSummaryProviders.stream()
                .filter(provider -> provider.jobName().equals(jobName))
                .findFirst();
    }

    public Optional<BatchProgressSnapshotProvider> findProgressSnapshotProvider(String jobName) {
        return progressSnapshotProviders.stream()
                .filter(provider -> provider.jobName().equals(jobName))
                .findFirst();
    }

    public Optional<BatchFailureSummaryProvider> findFailureSummaryProvider(String jobName) {
        return failureSummaryProviders.stream()
                .filter(provider -> provider.jobName().equals(jobName))
                .findFirst();
    }
}
