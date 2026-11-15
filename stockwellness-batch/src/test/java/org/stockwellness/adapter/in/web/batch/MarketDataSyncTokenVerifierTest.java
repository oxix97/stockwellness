package org.stockwellness.adapter.in.web.batch;

import org.junit.jupiter.api.Test;
import org.stockwellness.global.error.ErrorCode;
import org.stockwellness.global.error.exception.GlobalException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MarketDataSyncTokenVerifierTest {

    @Test
    void rejectsRequestsWhenNoTokenIsConfigured() {
        var verifier = new MarketDataSyncTokenVerifier("");

        GlobalException exception = assertThrows(GlobalException.class, () -> verifier.verify("any-value"));

        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
    }

    @Test
    void rejectsMissingOrMismatchedTokens() {
        var verifier = new MarketDataSyncTokenVerifier("configured-secret");

        assertThrows(GlobalException.class, () -> verifier.verify(null));
        assertThrows(GlobalException.class, () -> verifier.verify("wrong-secret"));
    }

    @Test
    void acceptsOnlyTheConfiguredToken() {
        var verifier = new MarketDataSyncTokenVerifier("configured-secret");

        assertDoesNotThrow(() -> verifier.verify("configured-secret"));
    }
}
