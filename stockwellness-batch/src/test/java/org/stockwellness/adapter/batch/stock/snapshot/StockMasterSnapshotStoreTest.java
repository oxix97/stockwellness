package org.stockwellness.adapter.batch.stock.snapshot;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.stockwellness.adapter.batch.stock.config.StockMasterSnapshotProperties;
import org.stockwellness.domain.stock.MarketType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockMasterSnapshotStoreTest {

    @TempDir
    Path stagingDirectory;

    private StockMasterSnapshotStore snapshotStore;

    @BeforeEach
    void setUp() {
        snapshotStore = new StockMasterSnapshotStore(
                new StockMasterSnapshotProperties(stagingDirectory, Duration.ofDays(7)));
    }

    @Test
    void stageAndReadVerified_preservesBytesAndMetadata() throws Exception {
        byte[] archive = {0, 1, 2, 3, 4, 5};

        StockMasterSnapshot snapshot = snapshotStore.stage(
                42L, MarketType.KOSPI, archive, 3, "STRUCTURALLY_VALID");

        assertThat(snapshot.marketType()).isEqualTo(MarketType.KOSPI);
        assertThat(snapshot.byteCount()).isEqualTo(archive.length);
        assertThat(snapshot.recordCount()).isEqualTo(3);
        assertThat(snapshot.completenessStatus()).isEqualTo("STRUCTURALLY_VALID");
        assertThat(snapshot.sha256()).hasSize(64);
        assertThat(snapshotStore.readVerified(snapshot)).containsExactly(archive);
    }

    @Test
    void readVerified_rejectsMissingArtifact() throws Exception {
        StockMasterSnapshot snapshot = snapshotStore.stage(
                42L, MarketType.KOSDAQ, new byte[]{1, 2}, 1, "STRUCTURALLY_VALID");

        Files.delete(Path.of(snapshot.artifactPath()));

        assertThatThrownBy(() -> snapshotStore.readVerified(snapshot))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void readVerified_rejectsChangedArtifact() throws Exception {
        StockMasterSnapshot snapshot = snapshotStore.stage(
                42L, MarketType.KOSDAQ, new byte[]{1, 2}, 1, "STRUCTURALLY_VALID");
        Files.write(Path.of(snapshot.artifactPath()), new byte[]{1, 3});

        assertThatThrownBy(() -> snapshotStore.readVerified(snapshot))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cleanupExpired_removesExpiredDirectoriesAndKeepsActiveOnes() throws Exception {
        StockMasterSnapshot expired = snapshotStore.stage(
                41L, MarketType.KOSPI, new byte[]{1}, 1, "STRUCTURALLY_VALID");
        StockMasterSnapshot active = snapshotStore.stage(
                42L, MarketType.KOSDAQ, new byte[]{2}, 1, "STRUCTURALLY_VALID");
        Instant cutoff = Instant.now();
        Files.setLastModifiedTime(Path.of(expired.artifactPath()).getParent(),
                FileTime.from(cutoff.minusSeconds(60)));

        snapshotStore.cleanupExpired(cutoff, Set.of(42L));

        assertThat(Files.exists(Path.of(expired.artifactPath()).getParent())).isFalse();
        assertThat(Files.exists(Path.of(active.artifactPath()))).isTrue();
    }
}
