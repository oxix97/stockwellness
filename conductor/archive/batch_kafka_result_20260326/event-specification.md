# Kafka Event Specification: Price/Indicator Batch Result

## 1. Overview
`Price/Indicator Calculation` 배치 작업 종료 시 발행되는 이벤트 명세입니다.

## 2. Topic Information
- **Topic Name**: `price-indicator-batch-result`
- **Producer**: `stockwellness-batch`
- **Consumer**: Monitoring System, Auto-Recovery Worker (TBD)

## 3. Payload (JSON)
| Field | Type | Description | Example |
| :--- | :--- | :--- | :--- |
| `batchName` | String | 배치 작업 명칭 | `stockPriceBatchJob` |
| `isSuccess` | Boolean | 작업 성공 여부 (COMPLETED 시 true) | `true` / `false` |
| `processedCount` | Long | 총 읽어온 아이템 수 | `2500` |
| `successCount` | Long | 성공적으로 쓰기 완료된 수 | `2498` |
| `failedCount` | Long | 처리/쓰기 중 실패(Skip)된 수 | `2` |
| `failedIdList` | List<String> | 실패한 종목 ID(Ticker) 목록 | `["005930", "000660"]` |
| `executionTime` | Long | 총 소요 시간 (ms) | `12500` |
| `errorMessage` | String | 실패 시 에러 메시지 (성공 시 null) | `Connection timeout` |

## 4. Monitoring & Alerting
- **Slack Alert**: 배치 작업이 `FAILED` 상태로 종료될 경우, 즉시 `#alert-batch` 채널로 상세 내용이 전송됩니다.
- **Retry Policy**: Kafka 발행 실패 시 비동기 콜백을 통해 에러 로그를 남기며, Producer 설정에 의해 최대 3회 자동 재시도합니다.

## 5. Troubleshooting
- **배치 실패 알림 수신 시**: `failedIdList`를 확인하여 특정 종목의 API 호출 오류인지, 혹은 DB 제약 조건 위반인지 로그를 통해 파악합니다.
- **Kafka 발행 실패 로그 발생 시**: Kafka 클러스터 가용 상태를 점검하고, 부트스트랩 서버 설정을 확인합니다.
