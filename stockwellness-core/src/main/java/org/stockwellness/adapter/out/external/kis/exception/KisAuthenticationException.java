package org.stockwellness.adapter.out.external.kis.exception;

public class KisAuthenticationException extends KisApiException {

    public KisAuthenticationException(String rtCd, String msgCd, String msg1) {
        super(rtCd, msgCd, msg1);
    }
}
