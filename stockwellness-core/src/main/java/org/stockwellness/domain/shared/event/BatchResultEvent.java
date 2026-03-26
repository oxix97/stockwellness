package org.stockwellness.domain.shared.event;

import java.util.List;

/**
 * 배치 작업 결과를 나타내는 공통 이벤트 모델
 */
public record BatchResultEvent(
    String batchName,
    boolean isSuccess,
    long processedCount,
    long successCount,
    long failedCount,
    List<String> failedIdList,
    long executionTime,
    String errorMessage
) {
    public static BatchResultEvent success(String batchName, long processedCount, long executionTime) {
        return new BatchResultEvent(batchName, true, processedCount, processedCount, 0, List.of(), executionTime, null);
    }

    public static BatchResultEvent failure(String batchName, long processedCount, long successCount, List<String> failedIdList, long executionTime, String errorMessage) {
        return new BatchResultEvent(batchName, false, processedCount, successCount, failedIdList.size(), failedIdList, executionTime, errorMessage);
    }
}
