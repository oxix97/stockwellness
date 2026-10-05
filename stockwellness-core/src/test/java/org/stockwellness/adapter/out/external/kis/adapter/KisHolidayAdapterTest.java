package org.stockwellness.adapter.out.external.kis.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.containsString;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.ratelimiter.RateLimiter;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class KisHolidayAdapterTest {
    private KisHolidayAdapter adapter;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new KisHolidayAdapter(builder.baseUrl("https://openapi.koreainvestment.com:9443").build(),
                new ObjectMapper(), RateLimiter.ofDefaults("kisHolidayTest"));
    }

    @Test
    void sends_one_calendar_request_and_maps_only_returned_dates() {
        server.expect(requestTo(containsString("/uapi/domestic-stock/v1/quotations/chk-holiday")))
                .andExpect(requestTo(containsString("BASS_DT=20261005")))
                .andExpect(requestTo(containsString("CTX_AREA_NK=")))
                .andExpect(header("tr_id", "CTCA0903R"))
                .andRespond(withSuccess("""
                        {"rt_cd":"0","msg_cd":"MCA00000","msg1":"OK","output":[
                          {"bass_dt":"20261005","opnd_yn":"Y"},
                          {"bass_dt":"20261006","opnd_yn":"N"}]}
                        """, MediaType.APPLICATION_JSON));

        var rows = adapter.fetchHolidays(LocalDate.of(2026, 10, 5));

        assertThat(rows).extracting(row -> row.baseDate()).containsExactly("20261005", "20261006");
        assertThat(rows).extracting(row -> row.marketOpen()).containsExactly("Y", "N");
        server.verify();
    }

    @Test
    void accepts_single_object_output() {
        server.expect(requestTo(containsString("chk-holiday")))
                .andRespond(withSuccess("{\"rt_cd\":\"0\",\"output\":{\"bass_dt\":\"20261005\",\"opnd_yn\":\"Y\"}}",
                        MediaType.APPLICATION_JSON));

        assertThat(adapter.fetchHolidays(LocalDate.of(2026, 10, 5))).hasSize(1);
        server.verify();
    }
}
