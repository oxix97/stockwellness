package org.stockwellness.application.parser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.stockwellness.domain.stock.KospiItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KospiMstParserTest {

    private static final Charset CP949 = Charset.forName("CP949");

    @Test
    void parseArchive_readsExactWidthKisRows() throws IOException {
        byte[] archive = archive("kospi_code.mst", row("005930", "KR7005930003", "삼성전자"));

        List<KospiItem> items = KospiMstParser.parseArchive(archive);

        assertThat(items).hasSize(1);
        assertThat(items.getFirst().shortCode()).isEqualTo("005930");
        assertThat(items.getFirst().isinCode()).isEqualTo("KR7005930003");
    }

    @Test
    void parseArchive_rejectsEmptyArchive() throws IOException {
        assertThatThrownBy(() -> KospiMstParser.parseArchive(archive()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseArchive_rejectsUnexpectedEntry() throws IOException {
        byte[] archive = archive("unexpected.mst", row("005930", "KR7005930003", "삼성전자"));

        assertThatThrownBy(() -> KospiMstParser.parseArchive(archive))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseArchive_rejectsAdditionalZipEntry() throws IOException {
        byte[] output;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("kospi_code.mst"));
            zip.write(row("005930", "KR7005930003", "삼성전자"));
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry("extra.mst"));
            zip.write(row("000660", "KR7000660001", "SK하이닉스"));
            zip.closeEntry();
        }
        output = bytes.toByteArray();

        assertThatThrownBy(() -> KospiMstParser.parseArchive(output))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseArchive_rejectsCorruptZip() {
        assertThatThrownBy(() -> KospiMstParser.parseArchive(new byte[]{1, 2, 3, 4}))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseArchive_rejectsTruncatedFixedWidthRow() throws IOException {
        byte[] archive = archive("kospi_code.mst", Arrays.copyOf(
                row("005930", "KR7005930003", "삼성전자"), 287));

        assertThatThrownBy(() -> KospiMstParser.parseArchive(archive))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseArchive_rejectsBlankTicker() throws IOException {
        byte[] archive = archive("kospi_code.mst", row("", "KR7005930003", "삼성전자"));

        assertThatThrownBy(() -> KospiMstParser.parseArchive(archive))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseArchive_rejectsBlankIsin() throws IOException {
        byte[] archive = archive("kospi_code.mst", row("005930", "", "삼성전자"));

        assertThatThrownBy(() -> KospiMstParser.parseArchive(archive))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseArchive_rejectsDuplicateTickerWithinMarket() throws IOException {
        byte[] first = row("005930", "KR7005930003", "삼성전자");
        byte[] second = row("005930", "KR7005930003", "삼성전자");
        byte[] archive = archive("kospi_code.mst", first, second);

        assertThatThrownBy(() -> KospiMstParser.parseArchive(archive))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sameTickerCanExistInKOSPIAndKOSDAQArchives() throws IOException {
        List<KospiItem> kospi = KospiMstParser.parseArchive(
                archive("kospi_code.mst", row("005930", "KR7005930003", "삼성전자")));
        List<org.stockwellness.domain.stock.KosdaqItem> kosdaq = KosdaqMstParser.parseArchive(
                archive("kosdaq_code.mst", kosdaqRow("005930", "KR7005930003", "삼성전자")));

        assertThat(kospi.getFirst().shortCode()).isEqualTo("005930");
        assertThat(kosdaq.getFirst().shortCode()).isEqualTo("005930");
    }

    private static byte[] row(String ticker, String isin, String name) {
        ByteArrayOutputStream row = new ByteArrayOutputStream(288);
        writePadded(row, ticker, 9);
        writePadded(row, isin, 12);
        writePadded(row, name, 40);
        row.writeBytes(" ".repeat(227).getBytes(CP949));
        byte[] bytes = row.toByteArray();
        if (bytes.length != 288) {
            throw new IllegalArgumentException("KOSPI fixture row must be 288 bytes");
        }
        return bytes;
    }

    private static byte[] kosdaqRow(String ticker, String isin, String name) {
        ByteArrayOutputStream row = new ByteArrayOutputStream(281);
        writePadded(row, ticker, 9);
        writePadded(row, isin, 12);
        writePadded(row, name, 39);
        row.writeBytes(" ".repeat(221).getBytes(CP949));
        return row.toByteArray();
    }

    private static void writePadded(ByteArrayOutputStream target, String value, int width) {
        byte[] bytes = value.getBytes(CP949);
        if (bytes.length > width) {
            throw new IllegalArgumentException("Fixture value exceeds fixed width");
        }
        target.writeBytes(bytes);
        target.writeBytes(" ".repeat(width - bytes.length).getBytes(CP949));
    }

    private static byte[] archive(byte[]... rows) throws IOException {
        return archive("kospi_code.mst", rows);
    }

    private static byte[] archive(String entryName, byte[]... rows) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry(entryName));
            for (byte[] row : rows) {
                zip.write(row);
                zip.write('\n');
            }
            zip.closeEntry();
        }
        return output.toByteArray();
    }
}
