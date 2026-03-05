package org.stockwellness.batch.dto;

/**
 * 배치 실행 API 요청에 대한 표준 응답 DTO
 */
public record BatchExecutionResponse(
        Long executionId,
        String jobName,
        String statusUrl,
        String message
) {
    public static BatchExecutionResponse of(Long executionId, String jobName, String statusUrl) {
        return new BatchExecutionResponse(
                executionId,
                jobName,
                statusUrl,
                String.format("배치 잡 [%s]이 시작되었습니다. (ExecutionId: %d)", jobName, executionId)
        );
    }
}
