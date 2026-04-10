package org.stockwellness.adapter.out.external.kis.exception;

import java.util.Locale;

public class KisApiException extends RuntimeException {

    private final String rtCd;
    private final String msgCd;
    private final String msg1;

    public KisApiException(String rtCd, String msgCd, String msg1) {
        super(buildMessage(rtCd, msgCd, msg1));
        this.rtCd = rtCd;
        this.msgCd = msgCd;
        this.msg1 = msg1;
    }

    public String rtCd() {
        return rtCd;
    }

    public String msgCd() {
        return msgCd;
    }

    public String msg1() {
        return msg1;
    }

    public boolean isTokenExpired() {
        return isTokenExpired(msgCd, msg1);
    }

    public boolean isRateLimitExceeded() {
        return isRateLimitExceeded(msgCd, msg1);
    }

    public static KisApiException from(String rtCd, String msgCd, String msg1) {
        if (isTokenExpired(msgCd, msg1)) {
            return new KisAuthenticationException(rtCd, msgCd, msg1);
        }
        return new KisApiException(rtCd, msgCd, msg1);
    }

    public static boolean isTokenExpired(String msgCd, String msg1) {
        String normalizedMessage = normalize(msg1);
        String normalizedCode = normalize(msgCd);

        return normalizedMessage.contains("기간이 만료된 token")
                || (normalizedMessage.contains("token") && (normalizedMessage.contains("expire") || normalizedMessage.contains("만료")))
                || (normalizedCode.contains("token") && (normalizedCode.contains("expire") || normalizedCode.contains("auth")));
    }

    public static boolean isRateLimitExceeded(String msgCd, String msg1) {
        String normalizedMessage = normalize(msg1);
        String normalizedCode = normalize(msgCd);

        return normalizedMessage.contains("초당 거래건수를 초과")
                || normalizedMessage.contains("호출 유량 초과")
                || normalizedMessage.contains("rate limit")
                || normalizedCode.contains("rate")
                || normalizedCode.contains("limit");
    }

    private static String buildMessage(String rtCd, String msgCd, String msg1) {
        return "KIS API 호출 실패 [rtCd=%s, msgCd=%s, msg1=%s]".formatted(rtCd, msgCd, msg1);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
