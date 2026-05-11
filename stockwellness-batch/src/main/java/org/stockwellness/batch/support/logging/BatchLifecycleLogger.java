package org.stockwellness.batch.support.logging;

import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchLifecycleLogger {

    private final BatchLogFormatter formatter;

    public void logStart(Map<String, Object> fields) {
        log.info(formatter.format(BatchLoggingConstants.STAGE_START, fields));
    }

    public void logStepEnd(Map<String, Object> fields) {
        log.info(formatter.format(BatchLoggingConstants.STAGE_STEP_END, fields));
    }

    public void logEnd(Map<String, Object> fields) {
        log.info(formatter.format(BatchLoggingConstants.STAGE_END, fields));
    }
}
