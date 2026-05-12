package org.stockwellness.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Testcontainers를 활용한 공통 인프라(PostgreSQL, Redis, Kafka) 통합 테스트 지원 클래스.
 * 싱글톤 컨테이너 패턴을 사용하여 테스트 전체에서 컨테이너를 한 번만 기동합니다.
 */
@ActiveProfiles("test")
@SpringBootTest
public abstract class InfrastructureTestSupport {

    /*
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.2-alpine"))
            .withExposedPorts(6379);

    static {
        postgres.start();
        kafka.start();
        redis.start();
    }

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        
        // Kafka 속성 설정
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }
    */
}
