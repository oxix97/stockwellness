package org.stockwellness.batch.job.stockmaster.support;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class StockMasterArchiveFixture {

    private static final Charset CP949 = Charset.forName("CP949");

    private StockMasterArchiveFixture() {
    }

    public static byte[] kospi(List<Row> rows) {
        return archive("kospi_code.mst", rows, 288, 40);
    }

    public static byte[] kosdaq(List<Row> rows) {
        return archive("kosdaq_code.mst", rows, 281, 39);
    }

    private static byte[] archive(String entryName, List<Row> rows, int rowWidth, int nameWidth) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(output)) {
                zip.putNextEntry(new ZipEntry(entryName));
                for (Row row : rows) {
                    writePadded(zip, row.ticker(), 9);
                    writePadded(zip, row.isin(), 12);
                    writePadded(zip, row.name(), nameWidth);
                    zip.write(" ".repeat(rowWidth - 9 - 12 - nameWidth).getBytes(CP949));
                    zip.write('\n');
                }
                zip.closeEntry();
            }
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not create synthetic KIS master archive", exception);
        }
    }

    private static void writePadded(ZipOutputStream zip, String value, int width) throws Exception {
        byte[] bytes = value.getBytes(CP949);
        if (bytes.length > width) {
            throw new IllegalArgumentException("Fixture field exceeds its fixed width");
        }
        zip.write(bytes);
        zip.write(" ".repeat(width - bytes.length).getBytes(CP949));
    }

    public record Row(String ticker, String isin, String name) {
    }
}
