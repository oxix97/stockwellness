package org.stockwellness.adapter.out.external.kis.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KisMarketDataResponseCaptureInterceptorTest {

    @TempDir
    Path captureDirectory;

    @Test
    void capturesOnlyAllowlistedMarketDataResponseWithoutAuthorizationHeader() throws Exception {
        byte[] body = "{\"rt_cd\":\"0\",\"output2\":[{\"stck_bsop_date\":\"20261113\"}]}"
                .getBytes(StandardCharsets.UTF_8);
        var interceptor = new KisMarketDataResponseCaptureInterceptor(captureDirectory.toString(), new ObjectMapper());
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://openapi.koreainvestment.com:9443")
                .requestInterceptor(interceptor);
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).bufferContent().build();
        server.expect(requestTo(containsString("investor-trade-by-stock-daily")))
                .andRespond(withSuccess(new String(body, StandardCharsets.UTF_8), org.springframework.http.MediaType.APPLICATION_JSON));
        String parsedBody = builder.build().get()
                .uri("/uapi/domestic-stock/v1/quotations/investor-trade-by-stock-daily"
                        + "?FID_INPUT_ISCD=005930&FID_INPUT_DATE_1=20261113")
                .header("tr_id", "FHPTJ04160001")
                .header("Authorization", "Bearer must-not-be-captured")
                .retrieve()
                .body(String.class);

        Path bodyFile;
        try (var files = Files.list(captureDirectory)) {
            bodyFile = files.filter(path -> path.toString().endsWith(".body")).findFirst().orElseThrow();
        }
        assertThat(Files.readAllBytes(bodyFile)).containsExactly(body);
        String metadata = Files.readString(captureDirectory.resolve(bodyFile.getFileName().toString().replace(".body", ".json")));
        assertThat(metadata).contains("FHPTJ04160001", "investor-trade-by-stock-daily", "20261113");
        assertThat(metadata).doesNotContain("must-not-be-captured", "Authorization", "Bearer");
        assertThat(parsedBody).isEqualTo(new String(body, StandardCharsets.UTF_8));
        server.verify();
    }

    @Test
    void doesNotCaptureNonMarketDataResponses() throws Exception {
        var interceptor = new KisMarketDataResponseCaptureInterceptor(captureDirectory.toString(), new ObjectMapper());
        var request = request("/uapi/domestic-stock/v1/trading/inquire-balance");
        ClientHttpRequestExecution execution = (ignored, ignoredBody) ->
                new MockClientHttpResponse("{}".getBytes(StandardCharsets.UTF_8), org.springframework.http.HttpStatus.OK);

        interceptor.intercept(request, new byte[0], execution);

        try (var files = Files.list(captureDirectory)) {
            assertThat(files).isEmpty();
        }
    }

    @Test
    void closesResponseAndFailsTheRequestWhenCaptureCannotBeWritten() throws Exception {
        Path capturePath = captureDirectory.resolve("not-a-directory");
        Files.writeString(capturePath, "file");
        var interceptor = new KisMarketDataResponseCaptureInterceptor(capturePath.toString(), new ObjectMapper());
        ClientHttpResponse response = mock(ClientHttpResponse.class);
        when(response.getBody()).thenReturn(new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)));
        when(response.getStatusCode()).thenReturn(HttpStatus.OK);
        ClientHttpRequestExecution execution = (ignored, ignoredBody) -> response;

        assertThatThrownBy(() -> interceptor.intercept(
                request("/uapi/domestic-stock/v1/quotations/investor-trade-by-stock-daily"), new byte[0], execution))
                .isInstanceOf(IOException.class);

        verify(response).close();
    }

    @Test
    void captureIsDisabledWhenDirectoryIsUnset() throws Exception {
        var interceptor = new KisMarketDataResponseCaptureInterceptor("", new ObjectMapper());
        var request = request("/uapi/domestic-stock/v1/quotations/investor-trade-by-stock-daily");
        ClientHttpRequestExecution execution = (ignored, ignoredBody) ->
                new MockClientHttpResponse("{}".getBytes(StandardCharsets.UTF_8), org.springframework.http.HttpStatus.OK);

        interceptor.intercept(request, new byte[0], execution);

        try (var files = Files.list(captureDirectory)) {
            assertThat(files.toList()).isEmpty();
        }
    }

    private static org.springframework.http.HttpRequest request(String uri) {
        URI requestUri = URI.create("https://openapi.koreainvestment.com:9443" + uri);
        HttpHeaders headers = new HttpHeaders();
        return new org.springframework.http.HttpRequest() {
            @Override public HttpMethod getMethod() { return HttpMethod.GET; }
            @Override public URI getURI() { return requestUri; }
            @Override public HttpHeaders getHeaders() { return headers; }
            @Override public Map<String, Object> getAttributes() { return Map.of(); }
        };
    }
}
