# 새 PostgreSQL 데이터베이스 스키마 준비

API와 Batch는 시작 시 스키마를 생성하거나 변경하지 않는다. 이 runbook의 목표 스키마 적용은 T10 애플리케이션 매핑 및 T13 인수 검증이 완료된 뒤 새 전용 DB에 한해 수행한다.

## 적용 절차

1. 전용 PostgreSQL 15 대상, DBA가 미리 만든 최소 권한 role, SQL 파일 checksum을 확인한다. 이 절차는 기존 데이터가 있는 DB에 적용하지 않는다.
2. `stockwellness-core/src/main/resources/db/manual/2026-11-11-redesign/01-create-database.sql`을 자동으로 실행하지 않는다. 예시 DB 이름과 role을 승인된 대상에 맞춰 DBA가 검토·수정한 뒤 `postgres` maintenance DB에서 트랜잭션 밖으로 실행한다.
3. 대상 DB에 접속해 `00-preflight.sql`을 실행한다. 이 스크립트는 `postgres` maintenance DB와 public schema가 비어 있지 않은 대상을 거부한다.

   ```bash
   psql -X -v ON_ERROR_STOP=1 "$DATABASE_URL" -f stockwellness-core/src/main/resources/db/manual/2026-11-11-redesign/00-preflight.sql
   ```

4. 새 대상에서 아래 순서로 SQL을 적용한다. `01-create-database.sql`은 이 목록과 별도로 반드시 transaction 밖에서 실행한다. 비밀번호를 명령행 인자나 기록에 넣지 않는다.

   ```bash
   psql -X -v ON_ERROR_STOP=1 "$DATABASE_URL" -f stockwellness-core/src/main/resources/db/manual/2026-11-11-redesign/02-schema.sql
   psql -X -v ON_ERROR_STOP=1 "$DATABASE_URL" -f stockwellness-core/src/main/resources/db/manual/2026-11-11-redesign/03-batch-metadata.sql
   psql -X -v ON_ERROR_STOP=1 "$DATABASE_URL" -f stockwellness-core/src/main/resources/db/manual/2026-11-11-redesign/04-reference-data.sql
   psql -X -v ON_ERROR_STOP=1 "$DATABASE_URL" -f stockwellness-core/src/main/resources/db/manual/2026-11-11-redesign/05-verify.sql
   ```

5. 05 검증 결과가 정확히 37개 논리 base table, 기준 시장 5건 및 Batch metadata와 세 sequence를 확인하는지 점검한다. Flyway schema history는 만들지 않는다.
6. T13 증거와 승인된 새 DB 접속 대상을 확인한 뒤 API와 Batch를 시작한다. Hibernate `validate` 또는 Batch metadata 검증이 실패하면 트래픽에 연결하지 않는다.

SQL 적용이나 검증이 실패하면 서비스를 시작하지 않는다. 부분 적용 여부를 확인하고 별도의 새 빈 DB에서 절차를 다시 실행한다. 현재 DDL이 준비됐다는 사실만으로 DB 전환이나 기존 DB 삭제가 승인된 것은 아니다.
