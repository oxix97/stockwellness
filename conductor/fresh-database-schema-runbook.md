# 새 PostgreSQL 데이터베이스 스키마 준비

API와 Batch는 애플리케이션 시작 시 Flyway migration을 실행하지 않는다. 신규 DB는 배포 후보와 동일한 백엔드 리비전의 통합 V1 스냅샷으로 사전 준비한다.

## 적용 절차

1. 전용 PostgreSQL 15 DB의 접속 대상과 소유자를 확인한다. 대상 DB가 비어 있고 연결 정보가 검증된 경우에만 진행한다. 운영 DB, 기존 사용자 데이터가 있는 DB, 공유 개발 DB에는 이 초기화 절차를 적용하지 않는다.
2. 배포 후보와 같은 Git 리비전에서 `stockwellness-core/src/main/resources/db/migration/V1__init_schema.sql` 파일을 선택한다.
3. 해당 DB에 연결해 다음 SQL로 현재 스키마가 비어 있는지 확인한다. 조회 대상이 예상과 다르거나 public 이외에 준비할 스키마가 있으면 적용을 중단하고 범위를 확인한다.

   ```sql
   SELECT schemaname, tablename
   FROM pg_tables
   WHERE schemaname = 'public';
   ```

4. 결과가 비어 있을 때만 검증한 대상 DB에 아래 명령으로 V1을 한 번 적용한다. 접속 비밀번호는 명령행 인자나 기록에 넣지 않는다.

   ```bash
   psql -X -v ON_ERROR_STOP=1 --single-transaction "$DATABASE_URL" \
     -f stockwellness-core/src/main/resources/db/migration/V1__init_schema.sql
   ```

5. 테이블 목록에 `actual_account`, Batch 메타데이터 테이블이 있는지, `market` 기준 데이터가 5건인지, `batch_job_seq`, `batch_job_execution_seq`, `batch_step_execution_seq` 시퀀스가 있는지 확인한다. `flyway_schema_history`가 없어야 한다.
6. 스키마가 준비된 경우에만 API와 Batch를 `prod` 프로필로 시작한다. Hibernate `validate`가 실패하거나 Batch metadata 검증에 실패하면 서비스를 트래픽에 연결하지 않는다.

SQL 적용이나 검증이 실패하면 서비스를 시작하지 않는다. 부분 적용 여부를 확인하고, 버린 뒤 다시 만든 별도 빈 DB에서 절차를 처음부터 재실행한다. 기존 DB의 데이터를 삭제하거나 재생성하지 않는다.
