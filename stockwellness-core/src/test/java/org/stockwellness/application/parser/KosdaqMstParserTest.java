package org.stockwellness.application.parser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.stockwellness.domain.stock.KosdaqItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KosdaqMstParserTest {

    private static final Charset CP949 = Charset.forName("CP949");

    @Test
    void parseArchive_readsExactWidthKisRows() throws IOException {
        byte[] archive = archive("kosdaq_code.mst", row("005930", "KR7005930003", "삼성전자"));

        List<KosdaqItem> items = KosdaqMstParser.parseArchive(archive);

        assertThat(items).hasSize(1);
        assertThat(items.getFirst().shortCode()).isEqualTo("005930");
        assertThat(items.getFirst().isinCode()).isEqualTo("KR7005930003");
    }

    @Test
    void parseArchive_readsTheTrackedKisMasterFile() throws IOException {
        Path path = Path.of("../kis/kosdaq_code.mst");
        List<String> lines = Files.readAllLines(path, CP949);
        List<byte[]> rows = lines.stream().map(line -> line.getBytes(CP949)).toList();
        List<KosdaqItem> expected = KosdaqMstParser.parse(path);

        assertThat(expected).isNotEmpty().anyMatch(item -> item.shortCode().equals("478340"));
        assertThat(KosdaqMstParser.parseArchive(archive("kosdaq_code.mst", rows.toArray(byte[][]::new))))
                .containsExactlyElementsOf(expected);
    }

    @Test
    void parseArchive_rejectsEmptyArchive() throws IOException {
        assertThatThrownBy(() -> KosdaqMstParser.parseArchive(archive()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseArchive_rejectsUnexpectedEntry() throws IOException {
        byte[] archive = archive("unexpected.mst", row("005930", "KR7005930003", "삼성전자"));

        assertThatThrownBy(() -> KosdaqMstParser.parseArchive(archive))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseArchive_rejectsCorruptZip() {
        assertThatThrownBy(() -> KosdaqMstParser.parseArchive(new byte[]{1, 2, 3, 4}))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseArchive_rejectsTruncatedFixedWidthRow() throws IOException {
        byte[] row = row("005930", "KR7005930003", "삼성전자");
        byte[] truncated = java.util.Arrays.copyOf(row, 280);
        byte[] archive = archive("kosdaq_code.mst", truncated);

        assertThatThrownBy(() -> KosdaqMstParser.parseArchive(archive))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseArchive_rejectsBlankTicker() throws IOException {
        byte[] archive = archive("kosdaq_code.mst", row("", "KR7005930003", "삼성전자"));

        assertThatThrownBy(() -> KosdaqMstParser.parseArchive(archive))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseArchive_rejectsBlankIsin() throws IOException {
        byte[] archive = archive("kosdaq_code.mst", row("005930", "", "삼성전자"));

        assertThatThrownBy(() -> KosdaqMstParser.parseArchive(archive))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseArchive_rejectsDuplicateTickerWithinMarket() throws IOException {
        byte[] row = row("005930", "KR7005930003", "삼성전자");
        byte[] archive = archive("kosdaq_code.mst", row, row);

        assertThatThrownBy(() -> KosdaqMstParser.parseArchive(archive))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static byte[] row(String ticker, String isin, String name) {
        ByteArrayOutputStream row = new ByteArrayOutputStream(282);
        writePadded(row, ticker, 9);
        writePadded(row, isin, 12);
        writePadded(row, name, 40);
        row.writeBytes(" ".repeat(221).getBytes(CP949));
        byte[] bytes = row.toByteArray();
        if (bytes.length != 282) {
            throw new IllegalArgumentException("KOSDAQ fixture row must be 282 bytes");
        }
        return bytes;
    }

    private static void writePadded(ByteArrayOutputStream target, String value, int width) {
        byte[] bytes = value.getBytes(CP949);
        if (bytes.length > width) {
            throw new IllegalArgumentException("Fixture value exceeds fixed width");
        }
        target.writeBytes(bytes);
        target.writeBytes(" ".repeat(width - bytes.length).getBytes(CP949));
    }

    private static byte[] archive() throws IOException {
        return archive("kosdaq_code.mst");
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
