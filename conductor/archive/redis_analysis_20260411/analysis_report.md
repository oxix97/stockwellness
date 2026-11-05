# Redis 설정 분석 보고서

- 기준일: 2026-10-02
- 범위: 현재 저장소의 Redis 설정·직렬화·사용 어댑터를 정적 검토했다. 운영 Redis 인스턴스에는 접속하지 않았으며, 성능 부하는 실행하지 않았다.

## 현재 구성

- 연결 설정은 `stockwellness-core/src/main/resources/application-core.yaml`에서 `${REDIS_HOST:localhost}:6379`와 `${REDIS_PASSWORD}`를 사용한다. API·배치 운영 설정은 공통 core 설정을 import한다. `application-core-prod.yaml`에는 Redis 연결을 덮어쓰는 설정이 없으므로 운영 호스트·비밀번호는 외부 환경 주입 상태를 확인해야 한다.
- `compose.yaml`은 개발용 Redis에 `bitnami/redis:latest`, 빈 비밀번호 허용, 6379 포트와 데이터 볼륨을 설정한다. 테스트 설정은 localhost:6379를 가리키며, 테스트 지원 코드에는 `redis:7.2-alpine` Testcontainers 구성이 있다.
- API의 `ApiRedisConfig`와 배치의 `BatchRedisConfig`는 각각 `RedisTemplate`과 `CacheManager`를 만든다. 둘 다 `CacheType`별 TTL·JSON 직렬화 정책을 구성하고 기본 TTL은 1시간이다. 캐시 종류별 TTL은 `CacheType`에 선언돼 있다.
- API와 배치에는 별도 JSON serializer 팩토리가 있다. 두 쪽 모두 허용 패키지 기반의 polymorphic type validator, Java Time 모듈, `@class` 타입 정보를 설정한다. API 쪽은 Spring Security 모듈을 등록하는 별도 security serializer를 제공하며 `MEMBER` 캐시에 사용한다. 배치 쪽에는 security serializer 분기가 없다.
- Redis 사용은 문자열 기반 토큰·인증 코드·검색 자료와 JSON 기반 캐시가 혼재한다. `RefreshTokenRedisAdapter`는 토큰과 만료 시각을 `::`로 연결하고, OAuth 교환 코드는 JSON 문자열로 저장한다. 검색 이력과 인기 검색어는 `StringRedisTemplate`의 ZSet을 사용한다. KIS 토큰 어댑터도 문자열 템플릿을 사용한다.

## 장점

- `CacheType`의 이름·TTL 정의로 캐시별 만료 정책을 한 곳에서 검토할 수 있다.
- Redis ZSet으로 검색 이력과 인기 검색어의 정렬·범위 조회를 처리하고, 검색 이력은 최대 10개를 유지한다.
- JSON 캐시는 저장값을 점검하기 쉽고, type validator와 Java Time 설정이 역직렬화 범위를 제한하면서 날짜 타입을 지원한다.
- API의 회원 캐시는 Spring Security 타입용 serializer를 별도로 사용해 일반 도메인 직렬화와 구분한다.

## 관찰 사항

1. `CoreRedisConfig`와 공통 `RedisSerializerConfig`는 현재 존재하지 않는다. 이전 보고서의 이 클래스명은 현행 코드와 맞지 않는다. 실제 중복은 API·배치 모듈 각각에 있는 설정 및 serializer 구현이다.
2. API·배치의 `CacheManager`와 domain serializer 구현이 비슷하게 중복돼 있다. 보안 타입 지원과 모듈 의존성 차이가 있으므로 통합 전 두 모듈의 직렬화 호환성 테스트가 필요하다.
3. `RefreshTokenRedisAdapter`는 구분자 문자열 직렬화를 유지한다. 현재 토큰 값과 날짜 문자열에 구분자가 들어가지 않는다는 전제를 둔다. 구조화된 값으로 변경하려면 기존 Redis 데이터 호환 또는 만료 시점까지의 전환 방식을 정해야 한다.
4. Compose의 `latest` 태그와 무인증 개발 Redis는 개발 편의 설정이다. 이를 운영 보안 상태의 증거로 간주하면 안 된다. 운영 비밀값과 실제 Redis 배포 설정은 이 저장소만으로 검증할 수 없다.

## 권고 사항

- API·배치 cache 설정을 합칠 때는 캐시 이름, TTL, serializer, `MEMBER` 보안 캐시의 역직렬화 회귀 테스트를 먼저 확정한다.
- Refresh token 저장 형식을 바꾸려면 키·값 포맷 버전 또는 만료를 이용한 이행 정책을 정하고 인증 경로 회귀 테스트를 추가한다.
- 운영 Redis의 인증, 네트워크 접근, 버전, TLS·백업 정책은 배포 설정 소유자가 별도로 확인한다. 본 검토는 배포 환경을 확인하지 않았다.

## 검증 한계

이 보고서는 저장소 파일의 정적 검토 결과다. 실제 운영 설정, Redis 장애 시 동작, 부하 시 지연·메모리 사용, 캐시 데이터 호환성은 검증하지 않았다. 이 항목을 출시 인수 증거로 사용하지 않는다.
