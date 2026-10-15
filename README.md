# 산수 알람 (Math Alarm)

간단한 산수 문제를 풀어야만 꺼지는 안드로이드 알람 앱입니다.

## 기능
- 알람 추가 / 수정 / 삭제 / 켜기·끄기
- 반복 요일 설정 (매일, 주중, 주말, 원하는 요일)
- 알람마다 **끄기 위해 풀어야 할 문제 수** 설정 (1~10개, 기본 3개)
- 문제 종류: 두 자리 덧셈, 뺄셈, 한 자리 × 십몇 곱셈
- 틀리면 새 문제가 나오고, 뒤로 가기로는 끌 수 없음
- 화면이 꺼져 있거나 잠겨 있어도 잠금화면 위에 문제 화면 표시
- 재부팅·시간 변경 후에도 알람 자동 재등록

## 빌드 방법
1. Android Studio(최신 버전)에서 이 폴더를 엽니다.
2. Gradle 동기화가 끝나면 기기나 에뮬레이터를 선택하고 ▶ 실행합니다.

명령줄로 빌드할 때:
```bash
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest    # 단위 테스트
```

- 최소 지원: Android 8.0 (API 26), 타깃: Android 15 (API 35)
- Kotlin 2.0, Jetpack Compose, Material 3

## 권한
| 권한 | 용도 |
|---|---|
| `USE_EXACT_ALARM` / `SCHEDULE_EXACT_ALARM` | 정확한 시간에 알람 울리기 |
| `POST_NOTIFICATIONS` | 알람 알림 표시 (Android 13+) |
| `USE_FULL_SCREEN_INTENT` | 잠금화면 위에 알람 화면 띄우기 (Android 14+는 설정에서 허용 필요) |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | 알람 소리 재생 |
| `RECEIVE_BOOT_COMPLETED` | 재부팅 후 알람 다시 등록 |
| `VIBRATE`, `WAKE_LOCK` | 진동, 화면 꺼짐 상태에서 재생 유지 |

빠진 권한이 있으면 메인 화면 상단에 안내 카드가 나타납니다.

## 구조
```
app/src/main/java/com/jojojack/mathalarm/
├── MainActivity.kt            알람 목록 화면 진입점
├── data/
│   ├── Alarm.kt               알람 모델, 다음 울릴 시각 계산
│   ├── AlarmFormat.kt         화면 표시용 문자열
│   └── AlarmRepository.kt     SharedPreferences(JSON) 저장소
├── alarm/
│   ├── AlarmScheduler.kt      AlarmManager.setAlarmClock 등록/취소
│   ├── AlarmReceiver.kt       알람 시각에 서비스 시작
│   ├── AlarmService.kt        소리·진동 재생, 전체 화면 알림
│   └── BootReceiver.kt        재부팅/시간 변경 시 재등록
├── math/MathProblem.kt        산수 문제 생성
├── ring/
│   ├── AlarmRingActivity.kt   잠금화면 위 알람 화면
│   └── MathChallengeScreen.kt 문제 풀이 UI (숫자 키패드)
└── ui/                        알람 목록, 편집 다이얼로그, 테마
```

## 동작 흐름
1. 알람 저장 → `AlarmScheduler`가 `setAlarmClock`으로 등록
2. 시각 도달 → `AlarmReceiver` → (반복 알람이면 다음 회차 등록) → `AlarmService` 시작
3. `AlarmService`가 소리·진동을 재생하고 전체 화면 알림으로 `AlarmRingActivity` 표시
4. 문제를 모두 맞히면 서비스 종료 → 알람 꺼짐
