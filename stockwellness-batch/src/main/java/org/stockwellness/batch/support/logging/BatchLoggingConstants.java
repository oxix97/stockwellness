package org.stockwellness.batch.support.logging;

public final class BatchLoggingConstants {

    public static final String STAGE_START = "START";
    public static final String STAGE_PROGRESS = "PROGRESS";
    public static final String STAGE_STEP_END = "STEP-END";
    public static final String STAGE_END = "END";

    public static final String CTX_START_DATE = "batch.startDate";
    public static final String CTX_END_DATE = "batch.endDate";
    public static final String CTX_TOTAL_COUNT = "batch.totalCount";
    public static final String CTX_PROCESSED_COUNT = "batch.processedCount";
    public static final String CTX_CURRENT_ITEM_ID = "batch.currentItemId";
    public static final String CTX_CURRENT_ITEM_KEY = "batch.currentItemKey";
    public static final String CTX_LAST_UPDATED_AT = "batch.lastUpdatedAt";
    public static final String CTX_ESTIMATED_REMAINING_MS = "batch.estimatedRemainingMs";
    public static final String CTX_LAST_LOGGED_AT = "batch.lastLoggedAt";
    public static final String CTX_LAST_LOGGED_PROCESSED_COUNT = "batch.lastLoggedProcessedCount";
    public static final String CTX_FAILED_ITEM_ID = "batch.failedItemId";
    public static final String CTX_FAILED_ITEM_KEY = "batch.failedItemKey";

    public static final long PROGRESS_LOG_INTERVAL_COUNT = 100L;
    public static final long PROGRESS_LOG_INTERVAL_MS = 60_000L;

    private BatchLoggingConstants() {
    }
}
