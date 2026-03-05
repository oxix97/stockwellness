package org.stockwellness.batch.common;

import org.springframework.batch.core.BatchStatus;

/**
 * 일관된 배치 로그 메시지를 생성하기 위한 템플릿 유틸리티
 */
public class BatchLogTemplate {

    private static final String STEP_PREFIX = ">>> [STEP: %s] ";
    private static final String JOB_PREFIX = "===== [JOB: %s] ";

    public static String stepStarted(String stepName) {
        return String.format(STEP_PREFIX + "Started", stepName);
    }

    public static String stepFinished(String stepName, BatchStatus status, long duration) {
        return String.format(STEP_PREFIX + "Finished | Status: %s | Duration: %dms", stepName, status, duration);
    }

    public static String progress(int processed, String detail) {
        return String.format(">>> [PROGRESS] %d items processed successfully (%s)", processed, detail);
    }

    public static String error(String message) {
        return String.format("!!! [ERROR] %s", message);
    }

    public static String jobStarted(String jobName) {
        return String.format(JOB_PREFIX + "START =====", jobName);
    }

    public static String jobFinished(String jobName, BatchStatus status, long duration) {
        return String.format(JOB_PREFIX + "FINISHED | Status: %s | Duration: %dms =====", jobName, status, duration);
    }
}
