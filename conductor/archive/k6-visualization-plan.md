# k6 Result Visualization Plan (Python Chart Generator)

## Objective
k6 부하 테스트의 개선 전후(Before & After) 성과를 이력서, PR, 기술 문서 등에 직관적으로 보여주기 위해, k6 JSON 결과 파일들을 기반으로 `matplotlib` 막대 그래프(PNG)를 자동 생성하는 Python 파이프라인을 구축합니다.

## Key Files & Context
- `k6/generate_charts.py` (신규): JSON 결과 파싱 및 차트 생성 스크립트.
- `k6/requirements.txt` (신규): Python 의존성 관리 (최소 `matplotlib` 포함).
- `k6/README.md`: 차트 생성 스크립트 실행 가이드 추가.

## Implementation Steps

### 1. 의존성 및 환경 설정
- `k6/requirements.txt` 파일을 생성하고 `matplotlib` 라이브러리를 추가합니다.

### 2. Python 스크립트 작성 (`generate_charts.py`)
- **데이터 스캔 및 파싱:** 
  - `k6/results/` 디렉토리를 스캔하여 `[시나리오명]-before-[번호].json` 및 `[시나리오명]-after-[번호].json` 패턴의 파일을 찾아 파싱합니다.
- **데이터 집계 (Aggregation):** 
  - 동일한 시나리오의 여러 회차 결과(예: before 1~3회)를 읽어 평균값을 산출합니다.
  - 핵심 지표: 
    - `p95 Latency` (단위: ms, 낮을수록 좋음)
    - `Throughput` 또는 `Request Count` (높을수록 좋음)
- **차트 렌더링 (Matplotlib):**
  - 시나리오별로 Before/After를 비교하는 그룹형 막대 그래프(Bar chart)를 생성합니다.
  - 막대 상단에 정확한 수치 텍스트를 표기하여 가독성을 높입니다.
  - 명확한 색상 대비(Before: 무채색/붉은색 톤, After: 강조색/푸른색 톤)를 적용합니다.
- **이미지 저장:** 
  - `k6/results/charts/` 폴더를 자동 생성하고, `[시나리오명]-comparison.png` 형태로 결과 이미지를 저장합니다.

### 3. 가이드 문서 업데이트 (`README.md`)
- Python 스크립트 구동을 위한 사전 준비(`pip install -r requirements.txt`) 안내를 추가합니다.
- 스크립트 실행 명령어(`python generate_charts.py`)와 예상되는 산출물 위치에 대한 설명을 `README.md`에 반영합니다.

## Verification
- 테스트용 `before`/`after` JSON Mock 데이터를 준비한 후 스크립트를 실행해 봅니다.
- `k6/results/charts/` 디렉토리에 PNG 차트가 생성되는지 확인합니다.
- 차트 이미지의 레이아웃(라벨, 범례, 수치 표기)이 깨짐 없이 잘 표현되는지 육안으로 검증합니다.