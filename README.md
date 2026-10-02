# 김포 상권 개폐업 대시보드

지방행정 인허가 데이터로 **김포시 음식점의 개업·폐업 추이**를 읍·면·동, 업종별로 보여주는 Spring Boot 웹 서비스입니다.
"우리 동네 카페는 순증가, 치킨집은 순감소" 같은 흐름과, 업종별로 보통 몇 년 버티는지를 한눈에 볼 수 있어요.

![대시보드 (샘플 데이터)](docs/dashboard-sample.png)

> ⚠️ 위 화면과 `sample-data/` 는 **개발·시연용 가짜 데이터**입니다. 실제 김포시 통계가 아닙니다.
> 모든 지표는 공공데이터에 기록된 값 기준의 **추정치**이며, 폐업 신고 누락·지연이 있을 수 있습니다.

## 빠른 시작

필요한 것: **JDK 21 이상** (21, 25에서 테스트 확인). Gradle은 `./gradlew` 가 알아서 받고, DB 설치도 필요 없어요.

```bash
# 1. 샘플(가짜) 데이터로 먼저 화면 확인  (macOS / Linux / Git Bash)
./gradlew bootRun --args="--app.import.path=sample-data/"
# 2. http://localhost:8080 접속
```

Windows 에서는 `./gradlew` 대신 `gradlew`(cmd) 또는 `.\gradlew`(PowerShell) 를 쓰고, 인수는 **큰따옴표**로 감쌉니다.

```bat
gradlew bootRun --args="--app.import.path=sample-data/"
```

**IntelliJ 에서 실행하기**

1. Settings → Build, Execution, Deployment → Build Tools → Gradle → **Gradle JVM** 을 JDK 21 이상으로 고르고, Gradle 창에서 *Reload All Gradle Projects* 를 누릅니다.
2. `GimpoBizDashboardApplication` 실행 구성의 **프로그램 인수**에 `--app.import.path=sample-data/` 를 넣고, 작업 디렉터리는 프로젝트 루트로 둡니다. (한 번 적재하면 다음부터는 인수 없이 실행해도 돼요.)

적재한 데이터는 `./data/` 의 H2 파일 DB에 남아서, 다음부터는 인수 없이 실행해도 됩니다.

### 실제 김포 데이터로 돌리기

1. 지방행정 인허가 데이터(공공데이터포털 또는 지방행정 인허가 데이터개방)에서 **김포시 일반음식점 / 휴게음식점 CSV** 를 받아 `import/` 폴더에 넣습니다.
   (제공 방식과 컬럼은 바뀔 수 있으니 받은 파일의 헤더를 직접 열어 확인하세요.)
2. `./gradlew bootRun --args="--app.import.path=import/"` (Windows cmd 는 `gradlew ...`)
   - 폴더를 주면 안의 `*.csv` 를 전부 적재합니다. 같은 파일을 다시 적재해도 **중복 없이 갱신**돼요.
   - CP949(EUC-KR)·UTF-8(BOM 포함) 인코딩을 자동으로 구분합니다.
   - 전국 파일을 받아도 주소에 `김포시` 가 없는 행은 걸러냅니다 (`app.import.region-keyword`).
3. 컬럼명이 달라 읽지 못하면 어떤 컬럼을 못 찾았는지와 실제 헤더를 에러로 알려줍니다. 별칭은 `importer/CsvRowParser.java` 상단에서 추가하세요.
4. `GET /api/meta` 의 `categories` 를 보고 `importer/CategoryNormalizer.java` 의 업종 묶음(예: 커피숍·까페 → 카페)을 데이터에 맞게 조정하세요.

`import/` 는 `.gitignore` 에 들어 있어 원본 CSV가 저장소에 올라가지 않습니다.

## 문제 해결

| 증상 | 해결 |
|---|---|
| Gradle 동기화가 "호환되지 않는 Java" 로 실패 | Gradle JVM 을 JDK 21 이상으로 지정 (위 IntelliJ 설정 참고). Gradle 래퍼는 9.8.0 이라 JDK 25 까지 지원합니다. |
| IntelliJ 로 실행하면 `NoClassDefFoundError: com/fasterxml/classmate/TypeResolver` | 최신 코드를 받고 *Reload All Gradle Projects* 후 다시 실행하세요. 그래도 나면 터미널에서 `gradlew bootRun --args="..."` 로 실행하세요. (Gradle 이 직접 클래스패스를 만들어서 영향이 없어요.) |
| 위 오류가 계속될 때 확인 | File → Project Structure → Libraries 에 `classmate` 가 있는지, 빨갛게 깨져 있지는 않은지 봅니다. 깨져 있으면 `~/.gradle/caches/modules-2/files-2.1/com.fasterxml/classmate` 폴더를 지우고 `gradlew build --refresh-dependencies` 후 다시 Reload 하세요. |
| 8080 포트가 이미 사용 중 | `--server.port=8081` 을 프로그램 인수에 추가 |

## 기능

| 카드 | 내용 |
|---|---|
| 요약 | 최근 12개월 개업·폐업·순증감 |
| 연도별 개업·폐업 | 최근 15년 막대 그래프. 동네·업종 필터 적용 |
| 업종별 영업 기간 | 폐업한 곳의 인허가일→폐업일 중앙값(평균은 툴팁). 표본 10곳 미만 업종 제외 |
| 최근 12개월 TOP 업종 | 개업·폐업 상위 10개 업종 |
| 읍·면·동별 비교 | 신도시와 구도심의 개업·폐업·순증감 |

모든 차트에는 **표로 보기** 가 있고, 라이트/다크 모드와 모바일 폭을 지원합니다.

## API

| 엔드포인트 | 설명 |
|---|---|
| `GET /api/meta` | 적재 건수, 기준일, 읍면동·업종 목록(필터용) |
| `GET /api/trend?district=&category=&years=15` | 연도별 개업·폐업·순증감 |
| `GET /api/survival?district=&minSample=10&limit=12` | 업종별 영업 기간 (평균·중앙값) |
| `GET /api/ranking?district=&months=12&limit=10` | 최근 N개월 개업·폐업 TOP 업종과 합계 |
| `GET /api/districts?category=&months=12` | 읍·면·동별 최근 N개월 개업·폐업 |

`district`, `category` 를 비우면 전체입니다.

## 설계 메모

- **기준일은 오늘이 아니라 데이터의 마지막 날짜(`asOf`)** 입니다. 내려받은 CSV가 한두 달 묵었어도 "최근 12개월"이 비지 않게 하려는 선택이에요. 미래 날짜 오타는 오늘로 잘라냅니다.
- **영업 기간 지표에는 편향이 있어요.** 폐업한 곳만 계산하므로 아직 영업 중인 오래된 가게가 빠져 실제 수명보다 짧게 나옵니다. 업종끼리 비교하는 상대 지표로 읽도록 화면에도 적어 두었습니다. 다음 단계로 "개업 후 3년 생존율"처럼 아직 영업 중인 곳까지 반영하는 지표를 추가할 수 있어요.
- **적재는 멱등합니다.** `개방서비스명 + 관리번호` 를 키로 upsert 하며, 관리번호가 없는 파일은 `사업장명 + 주소 + 인허가일` 로 대신합니다.
- **읍·면·동은 지번 주소에서 추출**합니다. 도로명 주소만 있는 행은 `미상` 으로 분류해 표에 그대로 드러냅니다.
- 집계는 `GROUP BY 연도 / 업종 / 읍면동` JPQL 쿼리이고 (`domain/BusinessRepository.java`), 선택 필터는 `null` 대신 빈 문자열 센티널로 받아 DB에 상관없이 동작하게 했습니다.
- 집계 테스트는 `src/test/resources/test-businesses.csv` 의 **손으로 계산한 정답**과 비교합니다.
- Chart.js 는 CDN 없이 `static/vendor/` 에 포함해 오프라인에서도 동작합니다. 화면의 동적 문자열은 전부 `textContent` 로만 넣습니다.

## 프로젝트 구조

```
src/main/java/com/gimpo/bizdash
├── domain/    Business 엔티티, BusinessRepository(집계 쿼리)
├── importer/  CSV 적재: 인코딩 판별, 헤더 별칭, 주소·날짜·업종 정제, upsert
├── stats/     StatsService: 추이·영업 기간·랭킹·읍면동 비교
└── web/       StatsController (REST)
src/main/resources/static/   index.html · app.css · app.js · vendor/chart.umd.js
sample-data/                 가짜 샘플 CSV  (scripts/generate_sample_data.py 로 재생성)
```

## 테스트·빌드

```bash
./gradlew test       # 23개: 주소/날짜 파서, 적재(CP949·BOM·멱등), 집계 API
./gradlew bootJar    # build/libs/ 에 실행 가능한 jar
```

## 다음 단계 아이디어

- 휴게음식점·미용·학원 등 업종 확장 (파일만 추가하면 적재됨)
- 아직 영업 중인 곳까지 반영한 N년 생존율, 폐업률 높은 업종 랭킹
- 좌표(X/Y)로 지도 히트맵, "폐업률 높은 골목"
- H2 → MySQL/PostgreSQL 전환, Flyway 마이그레이션, 배포
