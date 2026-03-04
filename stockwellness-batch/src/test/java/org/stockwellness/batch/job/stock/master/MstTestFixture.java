package org.stockwellness.batch.job.stock.master;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * KIS 마스터 파일({@code .mst}) 테스트를 위한 공통 Mock 데이터 생성 픽스처
 */
public class MstTestFixture {

    public static final Charset CP949 = Charset.forName("EUC-KR");

    /**
     * 지정된 길이의 빈 버퍼(공백으로 채워짐)를 생성합니다.
     */
    public static ByteBuffer createBaseBuffer(int totalLength) {
        ByteBuffer buffer = ByteBuffer.allocate(totalLength);
        // 초기화 (공백 ' ' 바이트로 채움)
        for (int i = 0; i < totalLength; i++) {
            buffer.put((byte) ' ');
        }
        buffer.flip();
        return buffer;
    }

    /**
     * KOSPI/KOSDAQ 공통 Part 1 영역(60바이트)을 채웁니다.
     * <pre>
     * 00~08: 단축코드(9)
     * 09~20: 표준코드(12)
     * 21~59: 한글종목명(39)
     * </pre>
     */
    public static void putPart1(ByteBuffer buffer, String shortCode, String isin, String name) {
        putPadded(buffer, 0, 9, shortCode, false);
        putPadded(buffer, 9, 12, isin, false);
        putPadded(buffer, 21, 39, name, false);
    }

    /**
     * 지정된 오프셋에 패딩을 고려하여 문자열을 삽입합니다.
     *
     * @param offset  시작 바이트 위치
     * @param len     필드 너비
     * @param val     삽입할 값
     * @param leftPad true이면 우측 정렬(왼쪽 패딩), false이면 좌측 정렬(오른쪽 패딩)
     */
    public static void putPadded(ByteBuffer buffer, int offset, int len, String val, boolean leftPad) {
        if (val == null) return;
        byte[] valBytes = val.getBytes(CP949);
        int writeLen = Math.min(valBytes.length, len);

        int startPos = leftPad ? (offset + len - writeLen) : offset;
        buffer.position(startPos);
        buffer.put(valBytes, 0, writeLen);
    }
}
