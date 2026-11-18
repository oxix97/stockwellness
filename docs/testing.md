# Testing

## 커버리지 목표

신규 코드 기준 **80% 이상** 유지.

## 테스트 계층

### 컨트롤러 테스트 (REST Docs 기반)

- `RestDocsSupport` 상속
- 요청/응답 스펙 문서화가 목적 → 모든 Controller는 REST Docs 테스트 필수
- 위치: `adapter/in/web/`

```java
class PortfolioControllerTest extends RestDocsSupport {
    // MockMvc + REST Docs 스니펫 생성
}
```

### 서비스 단위 테스트

- 외부 의존성은 Mock 처리
- 성공·실패 케이스 모두 작성
- 위치: `application/service/`

### 통합 테스트

- H2 기반 (`testFixtures/` 공유 픽스처 활용)
- DB 트랜잭션·이벤트 흐름 검증
- 위치: `integration/`

### 배치 테스트

- `BatchIntegrationTestSupport` 활용
- 위치: `stockwellness-batch/.../batch/`

## RestDocsSupport 사용법

### 클래스 구조

```java
// ✅ 모든 컨트롤러 테스트는 RestDocsSupport 상속
@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@ActiveProfiles("test")
@ExtendWith(RestDocumentationExtension.class)
public abstract class RestDocsSupport {

    // AI/LLM 포트는 기반 클래스에서 일괄 Mock 처리
    @MockitoBean protected LoadPortfolioAiPort loadPortfolioAiPort;
    @MockitoBean protected AiAdviceProviderPort aiAdviceProviderPort;
    @MockitoBean protected LoadSectorAiPort     loadSectorAiPort;
    @MockitoBean protected LlmClientPort        llmClientPort;

    @Autowired protected MockMvc     mockMvc;
    @Autowired protected ObjectMapper objectMapper;

    @BeforeEach
    void setUp(WebApplicationContext ctx, RestDocumentationContextProvider restDocs) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(ctx)
                .apply(documentationConfiguration(restDocs)
                        .operationPreprocessors()
                        .withRequestDefaults(prettyPrint())
                        .withResponseDefaults(prettyPrint()))
                .build();
    }

    // 공통 응답 필드 헬퍼
    protected List<FieldDescriptor> commonResponseFields() { ... }
    protected List<FieldDescriptor> commonResponseFieldsWithNoData() { ... }
}
```

### 테스트 클래스 기본 구조

```java
@Transactional                                         // DB 롤백
@DisplayName("Portfolio 통합 테스트 (RestDocs)")
class PortfolioControllerTest extends RestDocsSupport {

    @MockitoBean                                       // 서비스 계층 Mock
    private PortfolioFacade portfolioFacade;

    @Test
    @MockMember(id = 1L)                               // 인증 컨텍스트 주입
    @DisplayName("포트폴리오를 생성한다")
    void create_portfolio() throws Exception {
        // given
        given(portfolioFacade.createPortfolio(any())).willReturn(100L);

        // when & then
        mockMvc.perform(post("/api/v1/portfolios")
                        .header("Authorization", "Bearer {ACCESS_TOKEN}")
                        .with(csrf())
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andDo(document("portfolio-create",
                        resource(ResourceSnippetParameters.builder()
                                .tag("Portfolio")
                                .summary("포트폴리오 생성")
                                .requestFields(
                                        fieldWithPath("name").description("포트폴리오 이름"),
                                        fieldWithPath("items[].symbol").description("종목 심볼")
                                )
                                .responseFields(new ArrayList<>(commonResponseFields()) {{
                                    add(fieldWithPath("data").description("생성된 포트폴리오 ID"));
                                }})
                                .build())));
    }
}
```

---

## @MockMember 사용법

### 어노테이션 정의

```java
// ✅ Spring Security WithSecurityContext 활용
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = MockMemberSecurityContextFactory.class)
public @interface MockMember {
    long   id()       default 1L;
    String email()    default "test@example.com";
    String nickname() default "tester";
    String role()     default "USER";
}
```

내부적으로 `MemberPrincipal`을 생성해 `SecurityContext`에 주입한다.
기본 `LoginType`은 `KAKAO`로 고정.

### 사용 패턴

```java
// ✅ 기본 사용 — id=1, role=USER
@Test
@MockMember(id = 1L)
void getMyPortfolios() throws Exception { ... }

// ✅ 커스텀 속성
@Test
@MockMember(id = 99L, email = "admin@test.com", role = "ADMIN")
void adminOnly() throws Exception { ... }

// ✅ 인증 불필요 엔드포인트 — 어노테이션 생략
@Test
void publicSearch() throws Exception { ... }
```

---

## REST Docs 문서화 패턴

### 기본 문서화 (간단한 엔드포인트)

```java
.andDo(document("operation-id"));   // 스니펫만 생성
```

### 풀 문서화 (epages-restdocs-apispec)

```java
.andDo(document("auth-login",
        resource(ResourceSnippetParameters.builder()
                .tag("Auth")                              // OpenAPI 태그
                .summary("로그인")
                .description("소셜 로그인으로 토큰 발급")
                .requestSchema(Schema.schema("LoginRequest"))
                .responseSchema(Schema.schema("LoginResponse"))
                .requestFields(
                        fieldWithPath("email").description("이메일"),
                        fieldWithPath("loginType").description("GOOGLE | KAKAO | NAVER")
                )
                .responseFields(new ArrayList<>(commonResponseFields()) {{
                    add(fieldWithPath("data.accessToken").description("액세스 토큰"));
                    add(fieldWithPath("data.refreshToken").description("리프레시 토큰"));
                }})
                .build())
));
```

### 자주 쓰는 필드 기술자

```java
// 경로 파라미터
.pathParameters(parameterWithName("portfolioId").description("포트폴리오 ID"))

// 요청 헤더
.requestHeaders(headerWithName("Authorization").description("Bearer Access Token"))

// 배열 필드
fieldWithPath("data[].id").description("그룹 ID")

// 복잡한 중첩 객체 (세부 필드 생략 가능)
subsectionWithPath("data.items").description("포트폴리오 구성 종목 목록")
subsectionWithPath("data.categories").description("카테고리별 점수 Map")

// 선택 필드
fieldWithPath("traceId").description("에러 추적 ID").optional()

// 데이터 없는 응답
.responseFields(commonResponseFieldsWithNoData())   // data: null
```

---

## 테스트 픽스처 패턴

```java
// ✅ Fixture 클래스: 도메인 객체 생성 팩토리
public class PortfolioFixture {
    public static final Long   MEMBER_ID    = 1L;
    public static final Long   PORTFOLIO_ID = 100L;
    public static final String NAME         = "내 연금 포트폴리오";

    public static Portfolio createEntity() { ... }
    public static Portfolio createEntity(Long id) { ... }     // ID 지정 버전
    public static CreatePortfolioCommand createCreateCommand() { ... }
}

// ✅ 상속 구조로 공통 데이터 재사용
public class AuthFixture extends MemberFixture {
    public static LoginCommand  createLoginCommand()  { ... }
    public static ReissueRequest createReissueRequest() { ... }
}
```

---

## 동작 검증

- 동작과 위험에 맞는 의미 있는 테스트를 선택하고 실행 결과를 확인한다.
- API 계약이 바뀌면 REST Docs 테스트 및 OpenAPI와 소비자 호환성을 검증한다.

## 지원 클래스

| 클래스 | 용도 |
|---|---|
| `RestDocsSupport` | MockMvc + REST Docs 기반 테스트 |
| `@MockMember` | 인증된 사용자 컨텍스트 주입 |
| `fixture/` | 도메인 객체 픽스처 팩토리 |
| `testFixtures/` | core 모듈 공유 H2 픽스처 |
| `BatchIntegrationTestSupport` | 배치 통합 테스트 기반 클래스 |
