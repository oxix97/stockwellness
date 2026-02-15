# Stockwellness (멀티 모듈 프로젝트)

본 프로젝트는 주식 데이터 분석 및 포트폴리오 관리 서비스인 Stockwellness의 백엔드 서버입니다. 
확장성과 유지보수성을 위해 멀티 모듈 구조로 구성되어 있습니다.

## 모듈 구조

- **stockwellness-core**: 도메인 엔티티, 비즈니스 로직(UseCase), 영속성 계층(JPA, Redis) 및 공통 인프라 설정을 포함하는 공유 라이브러리 모듈입니다.
- **stockwellness-api**: REST API를 제공하는 웹 어댑터 모듈입니다. OAuth2 인증 및 회원/포트폴리오 관련 API를 담당합니다.
- **stockwellness-batch**: 데이터 전처리 및 대량 데이터 수집을 담당하는 Spring Batch 모듈입니다.

## 빌드 및 실행

### 전체 빌드
```bash
./gradlew clean build
```

### API 서버 실행
```bash
./gradlew :stockwellness-api:bootRun
```

### 배치 서버 실행
```bash
./gradlew :stockwellness-batch:bootRun
```

## 기술 스택
- Java 21
- Spring Boot 3.4.1
- Spring Data JPA, QueryDSL
- Spring Security, OAuth2 (Kakao)
- Spring Batch
- PostgreSQL, Redis
- Spring AI (OpenAI)
