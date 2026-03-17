# Docker Compose 로컬 이미지 빌드 및 실행 구현 계획

> **에이전트 작업자 가이드:** superpowers:executing-plans를 사용하여 이 계획을 실행하십시오. 각 단계는 체크박스(`- [ ]`) 구문을 사용하여 추적합니다.

**목표:** `compose.yaml`을 수정하여 `stockwellness-api`와 `stockwellness-batch` 애플리케이션 서비스를 추가하고, 로컬 환경에서 직접 이미지를 빌드하여 실행할 수 있도록 구성합니다.

**아키텍처:** Docker Compose의 `build` 기능을 활용하여 `Dockerfile.api` 및 `Dockerfile.batch`를 기반으로 로컬 이미지를 생성합니다. 각 애플리케이션 서비스는 동일한 도커 네트워크 내의 PostgreSQL, Redis, Kafka 서비스와 통신합니다.

**기술 스택:** Docker, Docker Compose, Java 21, Spring Boot, PostgreSQL, Redis, Kafka

---

### Task 1: compose.yaml 수정

**파일:**
- 수정: `compose.yaml`

- [ ] **단계 1: 애플리케이션 서비스 추가**

`compose.yaml` 파일에 `stockwellness-api`와 `stockwellness-batch` 서비스를 추가합니다.

```yaml
services:
  # ... 기존 인프라 서비스 유지 ...

  stockwellness-api:
    build:
      context: .
      dockerfile: Dockerfile.api
    container_name: stockwellness-api
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_URL=jdbc:postgresql://postgres:5432/mydatabase
      - DB_USERNAME=myuser
      - DB_PASSWORD=secret
      - REDIS_HOST=redis
      - KAFKA_BOOTSTRAP_SERVERS=kafka:9092
      - KAKAO_CLIENT_ID=${KAKAO_CLIENT_ID}
      - KAKAO_CLIENT_SECRET=${KAKAO_CLIENT_SECRET}
      - GOOGLE_CLIENT_ID=${GOOGLE_CLIENT_ID}
      - GOOGLE_CLIENT_SECRET=${GOOGLE_CLIENT_SECRET}
      - OPENAI_API_KEY=${OPENAI_API_KEY}
      - JWT_SECRET=${JWT_SECRET}
      - KIS_APP_KEY=${KIS_APP_KEY}
      - KIS_APP_SECRET=${KIS_APP_SECRET}
    depends_on:
      - postgres
      - redis
      - kafka
    networks:
      - stockwellness-net

  stockwellness-batch:
    build:
      context: .
      dockerfile: Dockerfile.batch
    container_name: stockwellness-batch
    ports:
      - "8081:8081"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_URL=jdbc:postgresql://postgres:5432/mydatabase
      - DB_USERNAME=myuser
      - DB_PASSWORD=secret
      - REDIS_HOST=redis
      - KAFKA_BOOTSTRAP_SERVERS=kafka:9092
      - OPENAI_API_KEY=${OPENAI_API_KEY}
      - KIS_APP_KEY=${KIS_APP_KEY}
      - KIS_APP_SECRET=${KIS_APP_SECRET}
    depends_on:
      - postgres
      - redis
      - kafka
    networks:
      - stockwellness-net

networks:
  stockwellness-net:
    driver: bridge
```

- [ ] **단계 2: 기존 서비스에 네트워크 추가**

기존 `postgres`, `redis`, `zookeeper`, `kafka` 서비스들이 `stockwellness-net` 네트워크를 사용하도록 수정합니다.

- [ ] **단계 3: 변경 사항 저장**

수정된 내용을 `compose.yaml` 파일에 반영합니다.

### Task 2: 검증 및 실행

- [ ] **단계 1: Docker Compose 빌드 확인**

명령어: `docker compose build`
예상 결과: `stockwellness-api` 및 `stockwellness-batch` 이미지가 성공적으로 빌드됨.

- [ ] **단계 2: 전체 서비스 실행**

명령어: `docker compose up -d`
예상 결과: 모든 서비스(인프라 + 애플리케이션)가 정상적으로 실행됨.

- [ ] **단계 3: 헬스체크 확인**

명령어: `docker ps`
예상 결과: `stockwellness-api`와 `stockwellness-batch` 서비스의 상태가 `healthy`로 표시됨.

---
