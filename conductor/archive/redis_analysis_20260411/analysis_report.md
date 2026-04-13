# Redis 설정 분석 보고서 (stockwellness)

## 1. 개요
현재 `stockwellness` 프로젝트에서 사용 중인 Redis 설정을 전수 조사하고, 아키텍처적 장점과 단점, 그리고 개선 방향을 분석합니다.

---

## 2. 주요 Redis 활용 사례
1. **세션 및 JWT 관리**: `RefreshTokenRedisAdapter`를 통해 리프레시 토큰의 생명주기 관리.
2. **API 응답 캐싱**: `@Cacheable` 및 `CacheManager`를 활용하여 EOD 시세 등 자주 조회되는 데이터 캐싱.
3. **실시간 데이터 관리**: 
    - **최근 검색어**: `ZSet`을 활용하여 사용자별 최근 10개의 검색어 저장.
    - **인기 검색어**: `ZSet`의 score를 활용하여 일자별 검색 빈도 집계.

---

## 3. 설정 분석 (Configuration Analysis)

### 3.1 직렬화 전략 (`RedisSerializerConfig`)
- **방식**: `GenericJackson2JsonRedisSerializer` 사용.
- **특징**: 
    - `PolymorphicTypeValidator`를 통한 보안 강화 (허용된 패키지 외 역직렬화 차단).
    - Java 21 Record 및 Java 8 Date/Time 지원.
    - `@class` 필드를 통한 타입 정보 포함.
    - `SecurityJackson2Modules`를 지원하는 별도의 Security 전용 직렬화기 제공.

### 3.2 캐시 관리 (`ApiRedisConfig`)
- **방식**: `Enum(CacheType)` 기반의 동적 `CacheManager` 설정.
- **특징**: 캐시 이름별로 서로 다른 TTL과 직렬화기(Domain vs Security)를 유연하게 적용.

---

## 4. 장점 (Pros)

1. **유연한 캐시 정책**: `CacheType` Enum을 통해 캐시별로 정밀한 TTL 제어가 가능하며, 코드 가독성이 높음.
2. **보안성 (Security-First)**: 
    - `PolymorphicTypeValidator`를 적용하여 원격 코드 실행(RCE) 취약점을 사전에 방어.
    - 인증 객체와 일반 도메인 객체의 직렬화기를 분리하여 보안 모듈과의 호환성 확보.
3. **가독성 및 디버깅 용이성**: JSON 포맷으로 저장되어 `redis-cli`를 통한 실시간 모니터링 및 디버깅이 매우 쉬움.
4. **효율적인 데이터 구조 활용**: `ZSet`을 사용하여 최근/인기 검색어의 정렬 및 개수 제한 로직을 Redis 레벨에서 효율적으로 처리.
5. **Virtual Threads 최적화**: 가상 스레드 환경에서 I/O 병목을 최소화할 수 있는 비동기 기반의 Lettuce 클라이언트 사용.

---

## 5. 단점 및 개선 필요 사항 (Cons)

1. **설정 중복 및 불일치 (Consistency Issue)**:
    - `CoreRedisConfig`와 `ApiRedisConfig`에 `RedisTemplate` 설정이 중복 존재함.
    - 특히 `CoreRedisConfig`는 기본 직렬화기를 사용하는 반면, `ApiRedisConfig`는 고도로 커스터마이징된 직렬화기를 사용함. 모듈 간 데이터 공유 시 역직렬화 오류 발생 위험이 있음.
2. **수동 직렬화 로직 존재**:
    - `RefreshTokenRedisAdapter` 등 일부 코드에서 `::` 구분자를 사용한 문자열 결합 방식으로 데이터를 저장함. 이는 정규화된 직렬화 전략에서 벗어나며 유지보수성을 저하시킴.
3. **StringRedisTemplate과 RedisTemplate의 혼용**:
    - `RedisTemplate<String, Object>`를 빈으로 등록했음에도, 여러 어댑터에서 기본 `StringRedisTemplate`을 주입받아 사용함. 일관된 타입 정책이 필요함.
4. **커넥션 풀 설정 부재**:
    - `application.yaml`에 Lettuce 커넥션 풀(Max Active, Wait Time 등) 설정이 누락됨. 고부하 환경에서 성능 저하의 원인이 될 수 있음.
5. **장애 내성(Fault Tolerance) 부족**:
    - Redis 장애 시 로컬 캐시(Caffeine 등)로 폴백(Fallback)하거나 DB로 직접 조회하는 Circuit Breaker 로직이 명시적으로 보이지 않음.

---

## 6. 제언 (Recommendations)
1. **설정 통합**: `RedisSerializerConfig`를 활용하여 `CoreRedisConfig`와 `ApiRedisConfig`의 직렬화 전략을 하나로 통합.
2. **수동 직렬화 제거**: `RefreshToken` 저장 시에도 전용 DTO와 `RedisTemplate<String, Object>`를 사용하여 일관된 JSON 직렬화 적용.
3. **인프라 튜닝**: 운영 환경(`application-prod.yaml`)에 적절한 Lettuce 커넥션 풀 및 타임아웃 설정 추가.
4. **로컬 캐시 계층 추가**: 성능 최적화가 극도로 필요한 영역은 Redis 호출 전 로컬 캐시를 먼저 확인하는 L1-L2 캐싱 구조 고려.
