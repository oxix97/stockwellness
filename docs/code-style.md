# Code Style

## 기본 원칙

- **가독성**: 영리한 트릭보다 명확한 코드 우선
- **일관성**: 기존 코드베이스의 패턴·네이밍·구조를 따른다
- **단순성**: 복잡한 문제는 작은 단위로 분리
- **유지보수성**: 의존성·결합도 최소화
- **문서화**: *what*이 아닌 *why*를 주석으로 기록, 코드 변경 시 함께 갱신

## 네이밍 컨벤션

| 구분 | 규칙 | 예시 |
|---|---|---|
| UseCase | `동사 + 대상 + UseCase` | `LoadPortfolioUseCase` |
| Port (out) | `동사 + 대상 + Port` | `SaveAdvisorPort` |
| Service | `대상 + Service` | `PortfolioAnalysisService` |
| Facade | `대상 + Facade` | `PortfolioFacade` |
| Adapter | `대상 + Adapter` | `PortfolioAdapter` |
| Command | `동사 + Command` | `LoginCommand` |
| Result/DTO | `대상 + Result` / `대상 + Request` | `LoginResult` |

## 예외 처리

```java
// ❌ 금지
throw new RuntimeException("error");

// ✅ 올바른 방식 - 도메인별 커스텀 예외 사용
throw new PortfolioNotFoundException(ErrorCode.PORTFOLIO_NOT_FOUND);
```

- 모든 예외는 `GlobalException` 또는 `BusinessException`을 상속
- ErrorCode는 `global/error/ErrorCode.java`에 정의
- `GlobalExceptionHandler`가 표준 `ApiResponse` 형태로 변환

## 데이터 접근

```java
// ❌ N+1 유발 패턴 금지
portfolioRepository.findAll(); // 이후 연관 엔티티 개별 조회

// ✅ Fetch Join / Entity Graph 명시
@EntityGraph(attributePaths = {"items"})
Optional<Portfolio> findWithItemsById(Long id);

// ✅ 복잡한 동적 쿼리는 QueryDSL
public List<Portfolio> findByCondition(PortfolioSearchCondition cond) { ... }
```

## Controller 구현 패턴

### 클래스 구조

```java
@RestController
@RequestMapping("/api/v1/portfolios")
@RequiredArgsConstructor          // 생성자 주입 — @Autowired 금지
@LogExecution                     // AOP 로깅 (선택, PortfolioController에 적용)
public class PortfolioController {

    private final PortfolioFacade portfolioFacade;  // UseCase 또는 Facade만 주입
    // ❌ Service 직접 주입 금지
}
```

### 인증 사용자 주입

```java
// ✅ @AuthenticationPrincipal로 MemberPrincipal 주입
@GetMapping("/{portfolioId}")
public ApiResponse<PortfolioResponse> getPortfolio(
        @AuthenticationPrincipal MemberPrincipal memberPrincipal,
        @PathVariable Long portfolioId) {

    return ApiResponse.success(
        portfolioFacade.getPortfolio(memberPrincipal.id(), portfolioId)
    );
}

// ✅ 인증 선택적 엔드포인트 — null 체크
@GetMapping("/search")
public ApiResponse<Slice<StockSearchResult>> search(
        @AuthenticationPrincipal MemberPrincipal member,  // 비인증 시 null
        @RequestParam(required = false) String keyword) {

    if (member != null && keyword != null) {
        stockSearchUseCase.saveSearchHistory(member.id(), keyword);
    }
    ...
}
```

### 응답 래핑 (`ApiResponse<T>`)

```java
// ✅ 200 OK — 데이터 있음
return ApiResponse.success(data);

// ✅ 201 Created — 생성 리소스 ID 반환
return ApiResponse.success(SuccessCode.CREATED, portfolioId);

// ✅ 200 OK — 데이터 없음 (삭제, 로그아웃 등)
return ApiResponse.success();
```

### Command / Result 변환

```java
// ✅ Controller는 Request → Command 변환 후 UseCase 호출
@PostMapping
public ApiResponse<Long> create(
        @AuthenticationPrincipal MemberPrincipal memberPrincipal,
        @RequestBody @Valid PortfolioCreateRequest request) {

    CreatePortfolioCommand command = new CreatePortfolioCommand(
        memberPrincipal.id(),
        request.name(),
        request.description(),
        request.items().stream()
            .map(item -> new CreatePortfolioCommand.PortfolioItemCommand(
                item.symbol(), item.quantity(), item.purchasePrice(),
                item.currency(), item.assetType(), item.targetWeight()))
            .toList()
    );

    return ApiResponse.success(SuccessCode.CREATED, portfolioFacade.createPortfolio(command));
}
```

### 요청 파라미터 패턴

```java
// @PathVariable — 리소스 식별자
@DeleteMapping("/{portfolioId}")
public ApiResponse<Void> delete(@PathVariable Long portfolioId) { ... }

// @RequestParam — 선택적 필터, 기본값 명시
@GetMapping("/search")
public ApiResponse<Slice<StockSearchResult>> search(
        @RequestParam(required = false) MarketType marketType,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size) { ... }

// @Valid + @RequestBody — 입력 검증 (Jakarta Validation)
@PostMapping
public ApiResponse<Long> create(@RequestBody @Valid PortfolioCreateRequest request) { ... }

// @DateTimeFormat — 날짜 쿼리 파라미터
@GetMapping("/sectors")
public ApiResponse<SectorResponse> get(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) { ... }
```

### Facade vs UseCase 주입 기준

| 주입 타입 | 사용 시점 |
|---|---|
| `XxxUseCase` (인터페이스) | 단일 유스케이스 호출로 충분한 경우 |
| `XxxFacade` (클래스) | 여러 유스케이스를 조합하는 복잡한 흐름 (예: `PortfolioFacade`) |

---

## 커밋 메시지

메시지는 **한글**로 작성. 커밋 전 반드시 메시지 보고 후 승인 받고 진행.

```
<type>(<scope>): <한글 설명>
```

| type | 용도 |
|---|---|
| `feat` | 새 기능 |
| `fix` | 버그 수정 |
| `refactor` | 동작 변경 없는 코드 개선 |
| `test` | 테스트 추가·수정 |
| `docs` | 문서 변경 |
| `chore` | 빌드·설정 등 유지보수 |

```bash
# 예시
git commit -m "feat(portfolio): AI 리밸런싱 어드바이저 엔드포인트 추가"
git commit -m "fix(batch): KIS API 타임아웃 처리 추가"
```
