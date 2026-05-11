package org.stockwellness.batch.support.logging;

import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchProgressLogger {

    private final BatchLogFormatter formatter;

    public void logProgress(Map<String, Object> fields) {
        log.info(formatter.format(BatchLoggingConstants.STAGE_PROGRESS, fields));
    }
}
