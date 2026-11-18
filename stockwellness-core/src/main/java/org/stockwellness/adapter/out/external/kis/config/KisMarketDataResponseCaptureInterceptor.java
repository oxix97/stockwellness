package org.stockwellness.adapter.out.external.kis.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

final class KisMarketDataResponseCaptureInterceptor implements ClientHttpRequestInterceptor {
    private static final Set<String> CAPTURED_PATHS = Set.of(
            "/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice",
            "/uapi/domestic-stock/v1/quotations/inquire-daily-indexchartprice",
            "/uapi/domestic-stock/v1/quotations/investor-trade-by-stock-daily",
            "/uapi/overseas-price/v1/quotations/inquire-daily-chartprice");

    private final Path captureDirectory;
    private final ObjectMapper objectMapper;

    KisMarketDataResponseCaptureInterceptor(String captureDirectory, ObjectMapper objectMapper) {
        this.captureDirectory = captureDirectory == null || captureDirectory.isBlank()
                ? null
                : Path.of(captureDirectory);
        this.objectMapper = objectMapper;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        ClientHttpResponse response = execution.execute(request, body);
        if (captureDirectory == null || !CAPTURED_PATHS.contains(request.getURI().getRawPath())) return response;

        try {
            byte[] responseBody = StreamUtils.copyToByteArray(response.getBody());
            save(request, response, responseBody);
            return response;
        } catch (IOException | RuntimeException exception) {
            response.close();
            throw exception;
        }
    }

    private void save(HttpRequest request, ClientHttpResponse response, byte[] responseBody) throws IOException {
        Files.createDirectories(captureDirectory);
        String fileStem = "kis-" + UUID.randomUUID();
        Path bodyFile = captureDirectory.resolve(fileStem + ".body");
        Path metadataFile = captureDirectory.resolve(fileStem + ".json");
        CaptureMetadata metadata = new CaptureMetadata(
                Instant.now().toString(),
                request.getMethod().name(),
                request.getURI().getRawPath(),
                request.getURI().getRawQuery(),
                request.getHeaders().getFirst("tr_id"),
                response.getStatusCode().value(),
                sha256(responseBody));

        writeAtomically(bodyFile, responseBody);
        try {
            writeAtomically(metadataFile, objectMapper.writeValueAsBytes(metadata));
        } catch (IOException | RuntimeException exception) {
            Files.deleteIfExists(bodyFile);
            throw exception;
        }
    }

    private void writeAtomically(Path target, byte[] value) throws IOException {
        Path temporary = Files.createTempFile(captureDirectory, ".kis-", ".tmp");
        try {
            Files.write(temporary, value, StandardOpenOption.TRUNCATE_EXISTING);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, target);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private record CaptureMetadata(
            String capturedAt,
            String method,
            String path,
            String query,
            String trId,
            int status,
            String responseSha256) {}
}
