package org.stockwellness.adapter.batch.stock.snapshot;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.stockwellness.adapter.batch.stock.config.StockMasterSnapshotProperties;
import org.stockwellness.domain.stock.MarketType;

@Component
@RequiredArgsConstructor
public class StockMasterSnapshotStore {

    private final StockMasterSnapshotProperties properties;

    public StockMasterSnapshot stage(
            long executionId,
            MarketType marketType,
            byte[] archiveBytes,
            int recordCount,
            String completenessStatus
    ) {
        if (executionId <= 0 || marketType == null || archiveBytes == null || archiveBytes.length == 0
                || recordCount <= 0 || completenessStatus == null || completenessStatus.isBlank()) {
            throw new IllegalArgumentException("Invalid stock master snapshot input");
        }

        Path executionDirectory = executionDirectory(executionId);
        Path temporaryFile = null;
        Path artifact = executionDirectory.resolve(fileName(marketType));
        try {
            Files.createDirectories(executionDirectory);
            temporaryFile = Files.createTempFile(executionDirectory, fileName(marketType), ".tmp");
            Files.write(temporaryFile, archiveBytes);
            try {
                Files.move(temporaryFile, artifact,
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                throw new IOException("Atomic snapshot publication is not supported", exception);
            }

            return new StockMasterSnapshot(
                    marketType,
                    artifact.toAbsolutePath().normalize().toString(),
                    sha256(archiveBytes),
                    archiveBytes.length,
                    recordCount,
                    completenessStatus
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Could not stage stock master snapshot", exception);
        } finally {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException ignored) {
                    // Best-effort cleanup for a temporary file that was not atomically moved.
                }
            }
        }
    }

    public byte[] readVerified(StockMasterSnapshot snapshot) {
        Path artifact = verifiedArtifactPath(snapshot);
        try {
            byte[] bytes = Files.readAllBytes(artifact);
            if (bytes.length != snapshot.byteCount() || !sha256(bytes).equals(snapshot.sha256())) {
                throw new IllegalStateException("Stock master snapshot does not match its descriptor");
            }
            return bytes;
        } catch (IOException exception) {
            throw new IllegalStateException("Stock master snapshot is missing or unreadable", exception);
        }
    }

    public void verifyBoth(StockMasterSnapshot kospi, StockMasterSnapshot kosdaq) {
        if (kospi == null || kospi.marketType() != MarketType.KOSPI
                || kosdaq == null || kosdaq.marketType() != MarketType.KOSDAQ) {
            throw new IllegalStateException("Both market snapshots are required");
        }
        verify(kospi);
        verify(kosdaq);
    }

    public void cleanupExpired(Instant cutoff, Set<Long> activeExecutionIds) {
        if (cutoff == null || activeExecutionIds == null) {
            throw new IllegalArgumentException("Cleanup cutoff and active execution IDs are required");
        }

        Path stagingDirectory = properties.stagingDirectory();
        if (!Files.isDirectory(stagingDirectory)) {
            return;
        }

        try (var children = Files.list(stagingDirectory)) {
            for (Path child : children.toList()) {
                if (!Files.isDirectory(child) || Files.isSymbolicLink(child)) {
                    continue;
                }
                long executionId = parseExecutionId(child);
                if (executionId <= 0 || activeExecutionIds.contains(executionId)) {
                    continue;
                }
                Instant modifiedAt = Files.getLastModifiedTime(child).toInstant();
                if (modifiedAt.isBefore(cutoff)) {
                    deleteRecursively(child);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not clean expired stock master snapshots", exception);
        }
    }

    public void deleteExecutionArtifacts(long executionId) {
        if (executionId <= 0) {
            throw new IllegalArgumentException("Execution ID must be positive");
        }
        Path directory = executionDirectory(executionId);
        if (Files.exists(directory)) {
            deleteRecursively(directory);
        }
    }

    private void verify(StockMasterSnapshot snapshot) {
        Path artifact = verifiedArtifactPath(snapshot);
        try {
            if (!Files.isRegularFile(artifact) || Files.size(artifact) != snapshot.byteCount()
                    || !sha256(Files.readAllBytes(artifact)).equals(snapshot.sha256())) {
                throw new IllegalStateException("Stock master snapshot does not match its descriptor");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Stock master snapshot is missing or unreadable", exception);
        }
    }

    private Path verifiedArtifactPath(StockMasterSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalStateException("Stock master snapshot descriptor is missing");
        }
        Path artifact = Path.of(snapshot.artifactPath()).toAbsolutePath().normalize();
        if (!artifact.startsWith(properties.stagingDirectory()) || Files.isSymbolicLink(artifact)) {
            throw new IllegalStateException("Stock master snapshot path is outside its staging directory");
        }
        return artifact;
    }

    private Path executionDirectory(long executionId) {
        return properties.stagingDirectory().resolve(Long.toString(executionId)).normalize();
    }

    private static String fileName(MarketType marketType) {
        return marketType.name().toLowerCase(java.util.Locale.ROOT) + ".mst.zip";
    }

    private static long parseExecutionId(Path directory) {
        try {
            return Long.parseLong(directory.getFileName().toString());
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private static void deleteRecursively(Path directory) {
        try {
            Files.walkFileTree(directory, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                    Files.deleteIfExists(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exception) throws IOException {
                    if (exception != null) {
                        throw exception;
                    }
                    Files.deleteIfExists(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException exception) {
            throw new IllegalStateException("Could not delete stock master snapshot directory", exception);
        }
    }
}
