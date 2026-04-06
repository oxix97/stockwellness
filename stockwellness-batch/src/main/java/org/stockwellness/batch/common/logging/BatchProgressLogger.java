package org.stockwellness.batch.common.logging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchProgressLogger {

    private final BatchLogFormatter formatter;

    public void logProgress(Map<String, Object> fields) {
        log.info(formatter.format(BatchLoggingConstants.STAGE_PROGRESS, fields));
    }
}
