package org.stockwellness.adapter.out.external.kis.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipInputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class KisMasterClient {

    private final RestClient kisAuthClient; // 인증용 클라이언트 재사용 (BaseURL만 덮어씀)

    // KIS 마스터 파일 다운로드 URL (상수)
    private static final String KOSPI_MASTER_URL = "https://new.real.download.dws.co.kr/common/master/kospi_code.mst.zip";
    private static final String KOSDAQ_MASTER_URL = "https://new.real.download.dws.co.kr/common/master/kosdaq_code.mst.zip";

    public List<String> downloadKospiMaster() {
        return downloadAndParse(KOSPI_MASTER_URL);
    }

    public List<String> downloadKosdaqMaster() {
        return downloadAndParse(KOSDAQ_MASTER_URL);
    }

    private List<String> downloadAndParse(String url) {
        log.info("Start downloading master file from: {}", url);

        byte[] zipBytes = kisAuthClient.get()
                .uri(url)
                .retrieve()
                .body(byte[].class);

        List<String> lines = new ArrayList<>();

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes));
             BufferedReader br = new BufferedReader(new InputStreamReader(zis, Charset.forName("Cp949")))) { // 인코딩 주의 (MS949/Cp949)

            // 압축 파일 내 첫 번째 엔트리(파일)로 이동
            if (zis.getNextEntry() != null) {
                String line;
                while ((line = br.readLine()) != null) {
                    System.out.println("line str : " + line);
                    lines.add(line);
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse master file", e);
            throw new RuntimeException("Master file download failed", e);
        }

        log.info("Downloaded {} lines.", lines.size());
        return lines;
    }
}