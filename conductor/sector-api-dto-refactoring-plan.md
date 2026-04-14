# SectorApiDto 모듈 의존성 리팩토링 계획

## 1. Objective (목표)

`stockwellness-batch` 모듈 빌드 시 발생하는 컴파일 에러의 임시방편 해결책(`sourceSets` 의존성 추가)을 제거하고, 헥사고날 아키텍처 원칙에 부합하도록 `SectorApiDto`의 위치를 재조정하여 근본적인 의존성 문제를 해결합니다.

## 2. Problem (문제 상황)

현재 `stockwellness-batch` 모듈의 `SectorEodStepConfig`가 `stockwellness-core` 모듈의 `org.stockwellness.application.port.out.stock.SectorApiDto`를 직접 참조하면서 빌드 오류가 발생했습니다.

- **아키텍처 규칙 위반**: `core` 모듈의 `application/port/out` 패키지는 외부 어댑터가 구현해야 할 **"인터페이스(Port)"** 를 정의하는 영역입니다. `SectorApiDto`와 같은 구체적인 데이터 전송 객체(DTO)가 이 위치에 있는 것은 역할에 맞지 않습니다.
- **임시 해결책의 한계**: `stockwellness-batch/build.gradle.kts`에 `implementation(project(":stockwellness-core").sourceSets.main.get().output)` 코드를 추가하여 문제를 해결했으나, 이는 모듈 간의 경계를 허무는 임시방편으로 장기적으로 유지보수성을 저해합니다.

## 3. Solution (해결 방안)

`SectorApiDto`를 역할에 맞는 패키지로 이동시키고, `build.gradle.kts` 설정을 원상 복구합니다.

### Step 1: `SectorApiDto.java` 파일 이동

`SectorApiDto`는 KIS API 어댑터에서 사용하는 DTO이므로, `adapter` 패키지 하위로 이동합니다.

- **AS-IS (현재 위치)**:
  `stockwellness-core/src/main/java/org/stockwellness/application/port/out/stock/SectorApiDto.java`
- **TO-BE (이동할 위치)**:
  `stockwellness-core/src/main/java/org/stockwellness/adapter/out/external/kis/dto/SectorApiDto.java`

*실행 명령어:*
```bash
# dto 디렉토리가 없을 경우 생성
mkdir -p stockwellness-core/src/main/java/org/stockwellness/adapter/out/external/kis/dto

# 파일 이동
mv stockwellness-core/src/main/java/org/stockwellness/application/port/out/stock/SectorApiDto.java stockwellness-core/src/main/java/org/stockwellness/adapter/out/external/kis/dto/SectorApiDto.java
```

### Step 2: `import` 경로 수정

`SectorApiDto`를 참조하는 모든 파일의 `import` 구문을 새로운 경로로 업데이트합니다.

- **대상 파일**: `SectorEodStepConfig.java`, `SectorApiItemReader.java` 등 `SectorApiDto`를 사용하는 모든 클래스
- **변경 전**: `import org.stockwellness.application.port.out.stock.SectorApiDto;`
- **변경 후**: `import org.stockwellness.adapter.out.external.kis.dto.SectorApiDto;`

### Step 3: `build.gradle.kts` 설정 원복

임시로 추가했던 `sourceSets` 의존성을 제거하여 `batch` 모듈의 빌드 스크립트를 원래대로 되돌립니다.

- **파일**: `stockwellness-batch/build.gradle.kts`
- **제거 대상 코드**:
  ```kotlin
  implementation(project(":stockwellness-core").sourceSets.main.get().output)
  ```

## 4. Verification (검증)

1.  모든 수정이 완료된 후, 프로젝트 루트에서 전체 빌드를 실행합니다.
    ```bash
    ./gradlew clean build
    ```
2.  빌드가 성공적으로 완료되고 모든 테스트가 통과하는지 확인합니다.
